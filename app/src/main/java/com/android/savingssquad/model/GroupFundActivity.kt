package com.android.savingssquad.model

import com.android.savingssquad.singleton.SquadActivityType
import com.android.savingssquad.singleton.RecordStatus

import androidx.annotation.Keep
import com.android.savingssquad.singleton.SquadLanguages
import com.android.savingssquad.singleton.SquadStrings
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

    @get:PropertyName("memberId") @set:PropertyName("memberId")
    var memberId: String?,

    @get:PropertyName("date") @set:PropertyName("date")
    var date: Timestamp? = null, // Activity date

    @get:PropertyName("activityType") @set:PropertyName("activityType")
    var activityType: SquadActivityType = SquadActivityType.AMOUNT_CREDIT, // Default type

    @get:PropertyName("memberName") @set:PropertyName("memberName")
    var memberName: String = "", // User who performed the activity

    @get:PropertyName("memberNameHindi") @set:PropertyName("memberNameHindi")
    var memberNameHindi: String,

    @get:PropertyName("memberNameTamil") @set:PropertyName("memberNameTamil")
    var memberNameTamil: String,

    @get:PropertyName("memberNameEnglish") @set:PropertyName("memberNameEnglish")
    var memberNameEnglish: String,

    @get:PropertyName("amount") @set:PropertyName("amount")
    var amount: Int = 0, // Amount involved

    @get:PropertyName("description") @set:PropertyName("description")
    var description: String = "", // Optional description

    @get:PropertyName("recordStatus") @set:PropertyName("recordStatus")
    var recordStatus: RecordStatus = RecordStatus.ACTIVE, // Record state

    @get:PropertyName("recordDate") @set:PropertyName("recordDate")
    var recordDate: Date = Date(),

    @get:PropertyName("descriptionTamil") @set:PropertyName("descriptionTamil")
    var descriptionTamil: String = "",

    @get:PropertyName("descriptionHindi") @set:PropertyName("descriptionHindi")
    var descriptionHindi: String = ""
) {

    val localizedDescription: String

        get() = when (SquadStrings.currentLanguage) {

            SquadLanguages.TAMIL -> descriptionTamil.ifEmpty { description }

            SquadLanguages.HINDI -> descriptionHindi.ifEmpty { description }

            SquadLanguages.ENGLISH -> description

        }

    constructor() : this(
        id = null,
        squadID = "",
        squadName = "",
        memberId = null,
        date = null,
        activityType = SquadActivityType.AMOUNT_CREDIT,
        memberName = "",
        memberNameHindi = "",
        memberNameTamil = "",
        memberNameEnglish = "",
        amount = 0,
        description = "",
        recordStatus = RecordStatus.ACTIVE,
        recordDate = Date(),
        descriptionTamil = "",
        descriptionHindi = ""
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
}
