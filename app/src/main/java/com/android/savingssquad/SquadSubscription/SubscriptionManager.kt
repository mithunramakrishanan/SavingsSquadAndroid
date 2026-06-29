package com.android.savingssquad.SquadSubscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resumeWithException

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
        val sub = _subscription.value ?: return false
        val config = remoteConfig

        // If still loading, be safe → block access
        if (!isSubscriptionLoaded.value) return false

        // Trial override
        if (isTrialActive()) return true

        return sub.features.loan || sub.loanAddon
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
                    completion(false, error)
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

                // auto-trial deactivate
                val now = System.currentTimeMillis()
                val end = updated.trialEndDate.toDate().time

                if (updated.isTrialActive && now >= end) {
                    SubscriptionFirebaseManager.shared.deactivateTrial(squadID) { _, _ -> }
                }

                _subscription.value = updated
                trialDaysTotal = updated.trialDays
                _isSubscriptionLoaded.value = true

                completion(true, null)
            }
        }
    }

    // MARK: - REFRESH (Store-style sync equivalent)
    fun refreshFromServer(
        squadID: String,
        completion: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {

            try {

                val activePlan = withContext(Dispatchers.IO) {
                    BillingHelper.getCurrentPlan()
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
                    loanAddon
                ) { success, error ->

                    if (!success) {
                        completion(false, error ?: "Update failed")
                        return@updateSubscription
                    }

                    loadSubscription(squadID) { ok, err ->
                        completion(ok, err)
                    }
                }

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
        val config = _remoteConfig.value

        if (isTrialActive()) return false
        if (sub.plan != SubscriptionModel.Plan.FREE) return false

        val maxMembers = config.maxMembers(sub.plan)

        return memberCount > maxMembers
    }
}