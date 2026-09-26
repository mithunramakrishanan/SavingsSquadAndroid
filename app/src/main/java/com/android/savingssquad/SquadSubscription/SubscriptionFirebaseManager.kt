package com.android.savingssquad.SquadSubscription

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class SubscriptionFirebaseManager private constructor() {

    companion object {
        val shared: SubscriptionFirebaseManager by lazy {
            SubscriptionFirebaseManager()
        }
    }

    private val db = FirebaseFirestore.getInstance()

    // MARK: - CREATE DEFAULT CONFIG
    fun createDefaultSubscriptionData(
        squadID: String,
        completion: (Boolean, String?) -> Unit
    ) {

        val globalConfigRef = db
            .collection("subscriptionSettings")
            .document("current")

        val squadSubscriptionRef = db
            .collection("squads")
            .document(squadID)
            .collection("subscription")
            .document("current")

        // =========================================================
        // 1. Fetch global subscription configuration
        // =========================================================

        globalConfigRef
            .get()
            .addOnSuccessListener { snapshot ->

                // =================================================
                // 2. Global config exists
                // =================================================

                if (snapshot.exists()) {

                    try {

                        val config =
                            snapshot.toObject(RemoteConfig::class.java)

                        if (config == null) {
                            completion(
                                false,
                                "Failed to decode RemoteConfig."
                            )
                            return@addOnSuccessListener
                        }

                        createSquadSubscription(
                            squadID = squadID,
                            config = config,
                            subscriptionRef = squadSubscriptionRef,
                            completion = completion
                        )

                    } catch (e: Exception) {

                        completion(
                            false,
                            e.localizedMessage
                                ?: "Failed to decode RemoteConfig."
                        )
                    }

                    return@addOnSuccessListener
                }

                // =================================================
                // 3. Global config DOES NOT exist
                //
                // Create it using RemoteConfig() defaults.
                // =================================================

                val config = RemoteConfig()

                globalConfigRef
                    .set(config)
                    .addOnSuccessListener {

                        createSquadSubscription(
                            squadID = squadID,
                            config = config,
                            subscriptionRef = squadSubscriptionRef,
                            completion = completion
                        )
                    }
                    .addOnFailureListener { error ->

                        completion(
                            false,
                            error.localizedMessage
                                ?: "Failed to create subscription configuration."
                        )
                    }
            }
            .addOnFailureListener { error ->

                completion(
                    false,
                    error.localizedMessage
                        ?: "Failed to fetch subscription configuration."
                )
            }
    }

    private fun createSquadSubscription(
        squadID: String,
        config: RemoteConfig,
        subscriptionRef: DocumentReference,
        completion: (Boolean, String?) -> Unit
    ) {

        val (start, end) =
            createTrialDates(config.trialDays)

        val subscription = SubscriptionModel(
            plan = SubscriptionModel.Plan.FREE,

            billingPeriod =
                SubscriptionModel.BillingPeriod.MONTHLY,

            loanAddon = false,

            isTrialActive = true,

            trialStartDate = start,
            trialEndDate = end,

            trialDays = config.trialDays,

            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )

        // =========================================================
        // IMPORTANT:
        //
        // Only create:
        //
        // squads/{squadID}/subscription/current
        //
        // We DO NOT create:
        //
        // squads/{squadID}/config/subscriptionSettings
        // =========================================================

        subscriptionRef
            .set(subscription)
            .addOnSuccessListener {

                completion(true, null)
            }
            .addOnFailureListener { error ->

                completion(
                    false,
                    error.localizedMessage
                        ?: "Failed to create subscription."
                )
            }
    }

    // MARK: - FETCH REMOTE CONFIG
    fun fetchRemoteConfig(
        squadID: String,
        completion: (RemoteConfig?, String?) -> Unit
    ) {

        // =========================================================
        // Subscription configuration is now GLOBAL.
        //
        // squadID is kept in the function signature so existing
        // callers don't need to change.
        //
        // It is intentionally not used here.
        // =========================================================

        db.collection("subscriptionSettings")
            .document("current")
            .get()
            .addOnSuccessListener { snapshot ->

                if (!snapshot.exists()) {

                    completion(
                        null,
                        "RemoteConfig not found."
                    )

                    return@addOnSuccessListener
                }

                try {

                    val config =
                        snapshot.toObject(
                            RemoteConfig::class.java
                        )

                    if (config != null) {

                        completion(
                            config,
                            null
                        )

                    } else {

                        completion(
                            null,
                            "Failed to decode RemoteConfig."
                        )
                    }

                } catch (e: Exception) {

                    completion(
                        null,
                        e.localizedMessage
                            ?: "Decoding error"
                    )
                }
            }
            .addOnFailureListener { error ->

                completion(
                    null,
                    error.localizedMessage
                        ?: "Unknown error"
                )
            }
    }


    // MARK: - FETCH SUBSCRIPTION
    fun fetchSubscription(
        squadID: String,
        completion: (SubscriptionModel?, String?) -> Unit
    ) {

        db.collection("squads")
            .document(squadID)
            .collection("subscription")
            .document("current")
            .get()
            .addOnSuccessListener { snapshot ->

                if (!snapshot.exists()) {
                    completion(null, "Subscription not found.")
                    return@addOnSuccessListener
                }

                val subscription = snapshot.toObject(SubscriptionModel::class.java)

                if (subscription != null) {
                    completion(subscription, null)
                } else {
                    completion(null, "Failed to decode subscription.")
                }
            }
            .addOnFailureListener {
                completion(null, it.localizedMessage)
            }
    }

    // MARK: - UPDATE SUBSCRIPTION
    fun updateSubscription(
        squadID: String,
        plan: SubscriptionModel.Plan,
        billingPeriod: SubscriptionModel.BillingPeriod,   // ⭐ NEW
        loanAddon: Boolean,
        completion: (Boolean, String?) -> Unit
    ) {

        val effectiveLoan = if (plan == SubscriptionModel.Plan.BUSINESS) true else loanAddon

        val data = hashMapOf(
            "plan" to plan,
            "billingPeriod" to billingPeriod,   // ⭐ NEW
            "loanAddon" to effectiveLoan,
            "updatedAt" to Timestamp.now()
        )

        db.collection("squads")
            .document(squadID)
            .collection("subscription")
            .document("current")
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                completion(true, null)
            }
            .addOnFailureListener {
                completion(false, it.localizedMessage)
            }
    }

    // MARK: - DEACTIVATE TRIAL
    fun deactivateTrial(
        squadID: String,
        completion: (Boolean, String?) -> Unit
    ) {

        val data = hashMapOf(
            "isTrialActive" to false,
            "updatedAt" to Timestamp.now()
        )

        db.collection("squads")
            .document(squadID)
            .collection("subscription")
            .document("current")
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                completion(true, null)
            }
            .addOnFailureListener {
                completion(false, it.localizedMessage)
            }
    }

    // MARK: - HELPERS
    private fun createTrialDates(days: Int): Pair<Timestamp, Timestamp> {
        val start = Timestamp.now()

        val calendar = java.util.Calendar.getInstance()
        calendar.time = java.util.Date()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, days)

        val end = Timestamp(calendar.time)

        return Pair(start, end)
    }

}