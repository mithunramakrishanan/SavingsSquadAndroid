package com.android.savingssquad.SquadSubscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.savingssquad.singleton.UserDefaultsManager
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SubscriptionManager private constructor() : ViewModel() {

    companion object {
        val shared: SubscriptionManager by lazy { SubscriptionManager() }
    }

    private val _subscription = MutableStateFlow<SubscriptionModel?>(null)
    val subscription = _subscription.asStateFlow()

    private val _isSubscriptionLoaded = MutableStateFlow(false)
    val isSubscriptionLoaded = _isSubscriptionLoaded.asStateFlow()

    private val _remoteConfig = MutableStateFlow(RemoteConfig())
    val remoteConfig = _remoteConfig.asStateFlow()

    var trialDaysTotal: Int = 45
        private set

    // MARK: - TRIAL
    fun isTrialActive(): Boolean {
        val sub = _subscription.value ?: return false

        val now = System.currentTimeMillis()
        val end = sub.trialEndDate.toDate().time

        return sub.isTrialActive && now < end
    }

    fun trialDaysRemaining(): Int {
        val sub = _subscription.value ?: return 0

        if (!sub.isTrialActive) return 0

        val diff = sub.trialEndDate.toDate().time - System.currentTimeMillis()

        return (diff / (1000 * 60 * 60 * 24))
            .toInt()
            .coerceAtLeast(0)
    }

    fun canAddMember(currentCount: Int): Boolean {

        val sub = _subscription.value ?: return false
        val config = _remoteConfig.value

        if (isTrialActive()) {
            return currentCount < config.biz_maxMembers
        }

        return currentCount < config.maxMembers(sub.plan)
    }


    fun canUseLoan(): Boolean {

        if (UserDefaultsManager.getSquadManagerLogged()) {

            val sub = _subscription.value ?: return false
            val config = remoteConfig

            // If still loading, be safe → block access
            if (!isSubscriptionLoaded.value) return false

            // Trial override
            if (isTrialActive()) return true

            return sub.features.loan || sub.loanAddon
        }
        else {
            return  true
        }

    }


    // MARK: - LOAD SUBSCRIPTION
    fun loadSubscription(
        squadID: String,
        completion: (Boolean, String?) -> Unit
    ) {
        _isSubscriptionLoaded.value = false

        SubscriptionFirebaseManager.shared.fetchRemoteConfig(squadID) { config, error ->

            if (config == null) {
                completion(false, error ?: "Config missing")
                return@fetchRemoteConfig
            }

            _remoteConfig.value = config

            SubscriptionFirebaseManager.shared.fetchSubscription(squadID) { model, error ->

                if (model == null) {
                    completion(false, error ?: "Subscription missing")
                    return@fetchSubscription
                }

                val features = config.features(model.plan)

                val updated = model.copy(
                    maxMembers = config.maxMembers(model.plan),
                    features = SubscriptionModel.Features(
                        contribution = features.contribution,
                        loan = features.loan
                    )
                )

                val now = System.currentTimeMillis()

                // MARK: - Check Trial Expiry

                var finalSubscription = updated

                val trialEndTime = updated.trialEndDate.toDate().time

                if (updated.isTrialActive && now >= trialEndTime) {
                    finalSubscription = finalSubscription.copy(
                        isTrialActive = false
                    )

                    SubscriptionFirebaseManager.shared.deactivateTrial(squadID) { success, error ->
                        if (!success) {
                            println(
                                "Failed to deactivate trial: ${error ?: "Unknown error"}"
                            )
                        }
                    }
                }

                // MARK: - Check Paid Subscription Expiry

                val subscriptionEndTime =
                    updated.subscriptionEndDate?.toDate()?.time

                if (
                    updated.plan != SubscriptionModel.Plan.FREE &&
                    updated.subscriptionStatus == SubscriptionModel.SubscriptionStatus.ACTIVE &&
                    subscriptionEndTime != null &&
                    now >= subscriptionEndTime
                ) {
                    finalSubscription = finalSubscription.copy(
                        subscriptionStatus = SubscriptionModel.SubscriptionStatus.EXPIRED
                    )

                    SubscriptionFirebaseManager.shared.expireSubscription(squadID) { success, error ->
                        if (!success) {
                            println(
                                "Failed to expire subscription: ${error ?: "Unknown error"}"
                            )
                        }
                    }
                }

                // MARK: - Update Local State

                _subscription.value = finalSubscription
                trialDaysTotal = finalSubscription.trialDays
                _isSubscriptionLoaded.value = true

                completion(true, null)
            }
        }
    }

    // MARK: - REFRESH (Store-style sync equivalent) ⭐ CHANGED — now plan + period
    fun refreshFromServer(
        squadID: String,
        completion: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {

            try {

                loadSubscription(squadID) { ok, err ->
                    completion(ok, err)
                }

               /* val (activePlan, activePeriod) = withContext(Dispatchers.IO) {
                    BillingHelper.getCurrentPlanAndPeriod()
                }

                val loanAddon =
                    if (activePlan == SubscriptionModel.Plan.BUSINESS) {
                        false
                    } else {
                        subscription.value?.loanAddon ?: false
                    }

                SubscriptionFirebaseManager.shared.updateSubscription(
                    squadID,
                    activePlan,
                    activePeriod,   // ⭐ NEW
                    loanAddon
                ) { success, error ->

                    if (!success) {
                        completion(false, error ?: "Update failed")
                        return@updateSubscription
                    }

                    loadSubscription(squadID) { ok, err ->
                        completion(ok, err)
                    }
                } */

            } catch (e: Exception) {
                completion(false, e.localizedMessage ?: "Unexpected error")
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
                    completion = completion
                )

            } catch (e: Exception) {
                completion(false, e.localizedMessage)
            }
        }
    }

    fun shouldForceUpgrade(memberCount: Int): Boolean {

        val sub = _subscription.value ?: return false

        val currentDate = Timestamp.now()

        // MARK: - 1. Active trial → No forced upgrade
        if (isTrialActive()) {
            return false
        }

        // MARK: - 2. FREE plan with expired trial → Force upgrade
        if (sub.plan == SubscriptionModel.Plan.FREE) {
            return true
        }

        // MARK: - 3. Check paid subscription status
        if (sub.subscriptionStatus != SubscriptionModel.SubscriptionStatus.ACTIVE) {
            return true
        }

        // MARK: - 4. Check subscription expiry
        val subscriptionEndDate = sub.subscriptionEndDate
            ?: return true

        if (currentDate.seconds >= subscriptionEndDate.seconds) {
            return true
        }

        // MARK: - 5. Check member limit
        val maxMembers = sub.maxMembers

        return memberCount > maxMembers
    }
}