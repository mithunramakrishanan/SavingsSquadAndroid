package com.android.savingssquad.SquadSubscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.savingssquad.singleton.Plan
import kotlinx.coroutines.launch
import com.google.firebase.Timestamp

class SubscriptionManager private constructor() : ViewModel() {

    companion object {
        val shared: SubscriptionManager by lazy { SubscriptionManager() }
    }

    var subscription: SubscriptionModel = SubscriptionModel()
        private set

    var remoteConfig: RemoteConfig = RemoteConfig()
        private set

    var trialDaysTotal: Int = 30
        private set

    // MARK: - TRIAL
    fun isTrialActive(): Boolean {
        val now = System.currentTimeMillis()
        val end = subscription.trialEndDate.toDate().time
        return subscription.isTrialActive && now < end
    }

    fun trialDaysRemaining(): Int {
        if (!isTrialActive()) return 0

        val diff = subscription.trialEndDate.toDate().time - System.currentTimeMillis()
        return (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
    }

    // MARK: - FEATURES
    fun isFeatureBlocked(feature: String): Boolean {
        if (isTrialActive()) return false

        return when (feature) {
            "loan" -> !(subscription.features.loan || subscription.loanAddon)
            else -> false
        }
    }

    fun canAddMember(currentCount: Int): Boolean {
        if (isTrialActive()) {
            return currentCount < remoteConfig.biz_maxMembers
        }
        return currentCount < remoteConfig.maxMembers(subscription.plan)
    }

    fun canUseContribution(): Boolean {
        return isTrialActive() || subscription.features.contribution
    }

    fun canUseLoan(): Boolean {
        return isTrialActive() || subscription.features.loan || subscription.loanAddon
    }

    fun isPlan(plan: Plan): Boolean {
        return subscription.plan == plan
    }

    // MARK: - LOAD SUBSCRIPTION
    fun loadSubscription(
        squadID: String,
        completion: (Boolean, String?) -> Unit
    ) {

        SubscriptionFirebaseManager.shared.fetchRemoteConfig(squadID) { config ->

            remoteConfig = config

            SubscriptionFirebaseManager.shared.fetchSubscription(
                squadID
            ) { model, error ->

                if (model == null) {
                    completion(false, error)
                    return@fetchSubscription
                }

                var updated = model

                val features = config.features(updated.plan)

                updated = updated.copy(
                    maxMembers = config.maxMembers(updated.plan),
                    features = SubscriptionModel.Features(
                        contribution = features.contribution,
                        loan = features.loan
                    )
                )

                // auto deactivate trial
                if (updated.isTrialActive &&
                    System.currentTimeMillis() >= updated.trialEndDate.toDate().time
                ) {
                    updated = updated.copy(isTrialActive = false)

                    SubscriptionFirebaseManager.shared.deactivateTrial(squadID) { _, _ -> }
                }

                subscription = updated
                trialDaysTotal = updated.trialDays

                completion(true, null)
            }
        }
    }

    // MARK: - REFRESH (Store-style sync equivalent)
    fun refreshFromServer(
        squadID: String,
        getActivePlan: suspend () -> Plan,
        completion: (Boolean, String?) -> Unit
    ) {

        viewModelScope.launch {
            try {

                val activePlan = getActivePlan()

                val loanAddon = if (activePlan == Plan.BUSINESS) {
                    false
                } else {
                    subscription.loanAddon
                }

                SubscriptionFirebaseManager.shared.updateSubscription(
                    squadID,
                    activePlan,
                    loanAddon
                ) { success, error ->
                    if (success) {
                        loadSubscription(squadID, completion)
                    } else {
                        completion(false, error)
                    }
                }

            } catch (e: Exception) {
                completion(false, e.localizedMessage)
            }
        }
    }

    // MARK: - RESTORE PURCHASES (Play Billing sync)
    fun restorePurchases(
        squadID: String,
        completion: (Boolean, String?) -> Unit
    ) {

        viewModelScope.launch {
            try {
                // Play Billing equivalent of AppStore.sync()
                // BillingClient.queryPurchasesAsync()

                refreshFromServer(
                    squadID,
                    getActivePlan = {
                        BillingHelper.getCurrentPlan()
                    },
                    completion = completion
                )

            } catch (e: Exception) {
                completion(false, e.localizedMessage)
            }
        }
    }
}