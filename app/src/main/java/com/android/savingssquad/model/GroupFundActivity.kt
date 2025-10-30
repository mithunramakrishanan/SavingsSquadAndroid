package com.android.savingssquad.model

import com.android.savingssquad.singleton.GroupFundActivityType
import com.android.savingssquad.singleton.RecordStatus
import com.google.firebase.Timestamp
import java.util.Date

data class GroupFundActivity(
    var id: String? = null,                         // Firestore document ID
    var groupFundID: String = "",                   // Associated groupFund ID
    var groupFundName: String = "",                 // GroupFund name for context
    var date: Timestamp? = null,                    // Activity date
    var activityType: GroupFundActivityType, // Default activity type
    var userName: String = "",                      // User who performed the activity
    var amount: Int = 0,                            // Amount involved
    var description: String = "",                   // Optional description
    var recordStatus: RecordStatus = RecordStatus.ACTIVE, // Record state
    var recordDate: Date = Date()                   // Timestamp for sorting or audit
)
