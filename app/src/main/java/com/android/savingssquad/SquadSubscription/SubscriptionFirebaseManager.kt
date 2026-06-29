package com.android.savingssquad.SquadSubscription

import com.google.firebase.Timestamp
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

        val config = RemoteConfig()

        db.collection("squads")
            .document(squadID)
            .collection("config")
            .document("subscriptionSettings")
            .set(config)
            .continueWithTask {

                val (start, end) = createTrialDates(config.trialDays)

                val subscription = SubscriptionModel(
                    plan = SubscriptionModel.Plan.FREE,
                    loanAddon = false,
                    isTrialActive = true,
                    trialStartDate = start,
                    trialEndDate = end,
                    trialDays = config.trialDays,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )

                db.collection("squads")
                    .document(squadID)
                    .collection("subscription")
                    .document("current")
                    .set(subscription)
            }
            .addOnSuccessListener {
                completion(true, null)
            }
            .addOnFailureListener {
                completion(false, it.localizedMessage)
            }
    }

    // MARK: - FETCH REMOTE CONFIG
    fun fetchRemoteConfig(
        squadID: String,
        completion: (RemoteConfig?, String?) -> Unit
    ) {

        db.collection("squads")
            .document(squadID)
            .collection("config")
            .document("subscriptionSettings")
            .get()
            .addOnSuccessListener { snapshot ->

                if (!snapshot.exists()) {
                    completion(null, "RemoteConfig not found.")
                    return@addOnSuccessListener
                }

                try {
                    val config = snapshot.toObject(RemoteConfig::class.java)

                    if (config != null) {
                        completion(config, null)
                    } else {
                        completion(null, "Failed to decode RemoteConfig.")
                    }

                } catch (e: Exception) {
                    completion(null, e.localizedMessage ?: "Decoding error")
                }
            }
            .addOnFailureListener {
                completion(null, it.localizedMessage ?: "Unknown error")
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
        loanAddon: Boolean,
        completion: (Boolean, String?) -> Unit
    ) {

        val effectiveLoan = if (plan == SubscriptionModel.Plan.BUSINESS) true else loanAddon

        val data = hashMapOf(
            "plan" to plan.value,
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