package com.android.savingssquad.model

import com.android.savingssquad.singleton.GroupFundActivityType
import com.android.savingssquad.singleton.RecordStatus

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import java.util.Date

@Keep
data class GroupFundActivity(

    @get:PropertyName("id") @set:PropertyName("id")
    var id: String? = null, // Firestore document ID

    @get:PropertyName("groupFundID") @set:PropertyName("groupFundID")
    var groupFundID: String = "", // Associated groupFund ID

    @get:PropertyName("groupFundName") @set:PropertyName("groupFundName")
    var groupFundName: String = "", // GroupFund name for context

    @get:PropertyName("date") @set:PropertyName("date")
    var date: Timestamp? = null, // Activity date

    @get:PropertyName("activityType") @set:PropertyName("activityType")
    var activityType: GroupFundActivityType = GroupFundActivityType.AMOUNT_CREDIT, // Default type

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
        groupFundID = "",
        groupFundName = "",
        date = null,
        activityType = GroupFundActivityType.AMOUNT_CREDIT,
        userName = "",
        amount = 0,
        description = "",
        recordStatus = RecordStatus.ACTIVE,
        recordDate = Date()
    )
}
