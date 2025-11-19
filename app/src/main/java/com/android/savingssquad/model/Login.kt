package com.android.savingssquad.model

import androidx.annotation.Keep
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.RecordStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import java.util.Date

@Keep
data class Login(

    @get:PropertyName("id") @set:PropertyName("id")
    var id: String? = null,

    @get:PropertyName("squadID") @set:PropertyName("squadID")
    var squadID: String = "",

    @get:PropertyName("squadName") @set:PropertyName("squadName")
    var squadName: String = "",

    @get:PropertyName("squadUsername") @set:PropertyName("squadUsername")
    var squadUsername: String = "",

    @get:PropertyName("squadUserId") @set:PropertyName("squadUserId")
    var squadUserId: String = "",

    @get:PropertyName("phoneNumber") @set:PropertyName("phoneNumber")
    var phoneNumber: String = "",

    @get:PropertyName("role") @set:PropertyName("role")
    var role: SquadUserType = SquadUserType.SQUAD_MANAGER,

    @get:PropertyName("squadCreatedDate") @set:PropertyName("squadCreatedDate")
    var squadCreatedDate: Timestamp? = null,

    @get:PropertyName("userCreatedDate") @set:PropertyName("userCreatedDate")
    var userCreatedDate: Timestamp? = null,

    @get:PropertyName("recordStatus") @set:PropertyName("recordStatus")
    var recordStatus: RecordStatus = RecordStatus.ACTIVE,

    @get:PropertyName("recordDate") @set:PropertyName("recordDate")
    var recordDate: Date = Date()
) {
    // REQUIRED empty constructor for Firestore
    constructor() : this(
        id = null,
        squadID = "",
        squadName = "",
        squadUsername = "",
        squadUserId = "",
        phoneNumber = "",
        role = SquadUserType.SQUAD_MANAGER, // MUST HAVE DEFAULT
        squadCreatedDate = null,
        userCreatedDate = null,
        recordStatus = RecordStatus.ACTIVE,
        recordDate = Date()
    )
}