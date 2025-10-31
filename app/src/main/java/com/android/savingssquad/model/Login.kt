package com.android.savingssquad.model

import com.android.savingssquad.singleton.GroupFundUserType
import com.android.savingssquad.singleton.RecordStatus
import com.google.firebase.Timestamp
import java.util.Date

data class Login(
    var id: String? = null,
    var groupFundID: String = "",
    var groupFundName: String = "",
    var groupFundUsername: String = "",
    var groupFundUserId: String = "",
    var phoneNumber: String = "",
    var role: String = "AS MEMBER", // store as String in Firestore
    var groupFundCreatedDate: Timestamp? = null,
    var userCreatedDate: Timestamp? = null,
    var recordStatus: RecordStatus = RecordStatus.ACTIVE,
    var recordDate: Date = Date()
) {
    fun getRoleEnum(): GroupFundUserType {
        return GroupFundUserType.fromValue(role)
    }
}