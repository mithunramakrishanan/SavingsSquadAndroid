package com.android.savingssquad.SquadSubscription

import com.android.savingssquad.singleton.Plan
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
    fun createDefaultConfig(
        squadID: String,
        completion: (Boolean, String?) -> Unit
    ) {

        val data = hashMapOf(
            "trialDays" to 30,

            "free_maxMembers" to 10,
            "free_contribution" to true,
            "free_loan" to true,

            "basic_maxMembers" to 50,
            "basic_contribution" to true,
            "basic_loan" to true,

            "biz_maxMembers" to 200,
            "biz_contribution" to true,
            "biz_loan" to true,

            "free_price" to "₹0",
            "basic_price" to "119/month",
            "biz_price" to "₹299/month",

            "addon_loan_price" to "₹49/month",

            "updatedAt" to Timestamp.now()
        )

        db.collection("squads")
            .document(squadID)
            .collection("config")
            .document("subscriptionSettings")
            .set(data)
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
        completion: (RemoteConfig) -> Unit
    ) {

        db.collection("squads")
            .document(squadID)
            .collection("config")
            .document("subscriptionSettings")
            .get()
            .addOnSuccessListener { snapshot ->

                val config = RemoteConfig()

                val data = snapshot.data ?: run {
                    completion(config)
                    return@addOnSuccessListener
                }

                config.trialDays = (data["trialDays"] as? Long)?.toInt() ?: 30

                config.free_maxMembers = (data["free_maxMembers"] as? Long)?.toInt() ?: 10
                config.basic_maxMembers = (data["basic_maxMembers"] as? Long)?.toInt() ?: 50
                config.biz_maxMembers = (data["biz_maxMembers"] as? Long)?.toInt() ?: 200

                completion(config)
            }
            .addOnFailureListener {
                completion(RemoteConfig())
            }
    }

    // MARK: - CREATE DEFAULT SUBSCRIPTION
    fun createDefaultSubscription(
        squadID: String,
        subDefault: SubscriptionModel,
        completion: (Boolean, String?) -> Unit
    ) {

        fetchRemoteConfig(squadID) { config ->

            val (start, end) = createTrialDates(config.trialDays)

            val data = hashMapOf(
                "plan" to Plan.FREE.value,
                "loanAddon" to false,
                "isTrialActive" to true,
                "trialStartDate" to start,
                "trialEndDate" to end,
                "trialDays" to config.trialDays,
                "createdAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            )

            db.collection("squads")
                .document(squadID)
                .collection("subscription")
                .document("current")
                .set(data)
                .addOnSuccessListener {
                    completion(true, null)
                }
                .addOnFailureListener {
                    completion(false, it.localizedMessage)
                }
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

                val data = snapshot.data

                if (data == null) {
                    completion(null, "No subscription found")
                    return@addOnSuccessListener
                }

                completion(mapSubscription(data), null)
            }
            .addOnFailureListener {
                completion(null, it.localizedMessage)
            }
    }

    // MARK: - UPDATE SUBSCRIPTION
    fun updateSubscription(
        squadID: String,
        plan: Plan,
        loanAddon: Boolean,
        completion: (Boolean, String?) -> Unit
    ) {

        val effectiveLoan = if (plan == Plan.BUSINESS) true else loanAddon

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

    // MARK: - MAPPING
    private fun mapSubscription(data: Map<String, Any>): SubscriptionModel {

        return SubscriptionModel(
            plan = Plan.from(data["plan"] as? String),

            loanAddon = data["loanAddon"] as? Boolean ?: false,
            isTrialActive = data["isTrialActive"] as? Boolean ?: true,

            trialStartDate = data["trialStartDate"] as? Timestamp ?: Timestamp.now(),
            trialEndDate = data["trialEndDate"] as? Timestamp ?: Timestamp.now(),
            trialDays = (data["trialDays"] as? Long)?.toInt() ?: 30,

            createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
            updatedAt = data["updatedAt"] as? Timestamp ?: Timestamp.now()
        )
    }
}