package com.android.savingssquad.model

import com.android.savingssquad.singleton.GroupFundUserType
import com.android.savingssquad.singleton.RecordStatus
import com.google.firebase.Timestamp
import java.util.Date

data class Login(
    var id: String? = null,                   // Firestore Document ID
    var groupFundID: String = "",             // Associated groupFund group ID
    var groupFundName: String = "",           // GroupFund group name
    var groupFundUsername: String = "",       // User's name within the groupFund
    var groupFundUserId: String = "",         // User's ID within the groupFund
    var phoneNumber: String = "",             // User's registered phone number
    var role: GroupFundUserType = GroupFundUserType.GROUP_FUND_MANAGER, // Role
    var groupFundCreatedDate: Timestamp? = null, // When groupFund was created
    var userCreatedDate: Timestamp? = null,   // When user joined groupFund
    var recordStatus: RecordStatus = RecordStatus.ACTIVE,
    var recordDate: Date = Date()             // Timestamp of record creation/update
)