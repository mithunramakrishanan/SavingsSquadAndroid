package com.android.savingssquad.model

import androidx.annotation.Keep
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.RecordStatus
import com.android.savingssquad.singleton.SquadLanguages
import com.android.savingssquad.singleton.SquadStrings
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import java.util.Date
import kotlin.text.ifEmpty

@Keep
data class Login(

    @get:PropertyName("id") @set:PropertyName("id")
    var id: String? = null,

    @get:PropertyName("squadID") @set:PropertyName("squadID")
    var squadID: String = "",

    @get:PropertyName("squadName") @set:PropertyName("squadName")
    var squadName: String = "",

    @get:PropertyName("squadNameHindi") @set:PropertyName("squadNameHindi")
    var squadNameHindi: String = "",

    @get:PropertyName("squadNameTamil") @set:PropertyName("squadNameTamil")
    var squadNameTamil: String = "",

    @get:PropertyName("squadNameEnglish") @set:PropertyName("squadNameEnglish")
    var squadNameEnglish: String = "",

    @get:PropertyName("memberName") @set:PropertyName("memberName")
    var memberName: String = "", // User who performed the activity

    @get:PropertyName("memberNameHindi") @set:PropertyName("memberNameHindi")
    var memberNameHindi: String,

    @get:PropertyName("memberNameTamil") @set:PropertyName("memberNameTamil")
    var memberNameTamil: String,

    @get:PropertyName("memberNameEnglish") @set:PropertyName("memberNameEnglish")
    var memberNameEnglish: String,

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
        squadNameHindi = "",
        squadNameTamil = "",
        squadNameEnglish = "",
        memberName = "",
        memberNameHindi = "",
        memberNameTamil = "",
        memberNameEnglish = "",
        squadUserId = "",
        phoneNumber = "",
        role = SquadUserType.SQUAD_MANAGER, // MUST HAVE DEFAULT
        squadCreatedDate = null,
        userCreatedDate = null,
        recordStatus = RecordStatus.ACTIVE,
        recordDate = Date()
    )

    val localizedMemberName: String

        get() = when (SquadStrings.currentLanguage) {

            SquadLanguages.TAMIL ->

                memberNameTamil.ifEmpty { memberName }

            SquadLanguages.HINDI ->

                memberNameHindi.ifEmpty { memberName }

            SquadLanguages.ENGLISH ->

                memberNameEnglish

        }

    val localizedSquadName: String

        get() = when (SquadStrings.currentLanguage) {

            SquadLanguages.TAMIL ->

                squadNameTamil.ifEmpty { squadName }

            SquadLanguages.HINDI ->

                squadNameHindi.ifEmpty { squadName }

            SquadLanguages.ENGLISH ->

                squadNameEnglish

        }
}