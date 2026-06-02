package com.android.savingssquad.model

import com.android.savingssquad.singleton.SquadActivityType
import com.android.savingssquad.singleton.RecordStatus

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import java.util.Date

@Keep
data class SquadActivity(

    @get:PropertyName("id") @set:PropertyName("id")
    var id: String? = null, // Firestore document ID

    @get:PropertyName("squadID") @set:PropertyName("squadID")
    var squadID: String = "", // Associated squad ID

    @get:PropertyName("squadName") @set:PropertyName("squadName")
    var squadName: String = "", // Squad name for context

    @get:PropertyName("date") @set:PropertyName("date")
    var date: Timestamp? = null, // Activity date

    @get:PropertyName("activityType") @set:PropertyName("activityType")
    var activityType: SquadActivityType = SquadActivityType.AMOUNT_CREDIT, // Default type

    @get:PropertyName("userName") @set:PropertyName("userName")
    var userName: String = "", // User who performed the activity

    @get:PropertyName("amount") @set:PropertyName("amount")
    var amount: Int = 0, // Amount involved

    @get:PropertyName("description") @set:PropertyName("description")
    var description: String = "", // Optional description

    @get:PropertyName("recordStatus") @set:PropertyName("recordStatus")
    var recordStatus: RecordStatus = RecordStatus.ACTIVE, // Record state

    @get:PropertyName("recordDate") @set:PropertyName("recordDate")
    var recordDate: Date = Date() // Timestamp for sorting or audit
) {
    // 🔹 Required empty constructor for Firestore deserialization
    constructor() : this(
        id = null,
        squadID = "",
        squadName = "",
        date = null,
        activityType = SquadActivityType.AMOUNT_CREDIT,
        userName = "",
        amount = 0,
        description = "",
        recordStatus = RecordStatus.ACTIVE,
        recordDate = Date()
    )
}
