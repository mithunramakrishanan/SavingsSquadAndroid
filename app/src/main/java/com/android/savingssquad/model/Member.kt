package com.android.savingssquad.model
import com.google.firebase.firestore.PropertyName
import androidx.annotation.Keep

import com.android.savingssquad.singleton.GroupFundUserType
import com.android.savingssquad.singleton.RecordStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.gson.annotations.SerializedName
import java.util.Date

data class UpiVerificationResult(
    val status: String,
    val data: UpiVerificationData?,
    val type: String?,
    val code: String?,
    val message: String?
)

data class UpiVerificationData(
    val upi_id: String?,
    val name_at_bank: String?,
    val verified: Boolean?
)

data class BeneficiaryResult(
    val beneId: String,
    val upi: String
)

@Keep
data class Member(

    @get:PropertyName("id") @set:PropertyName("id")
    var id: String? = null,

    @get:PropertyName("name") @set:PropertyName("name")
    var name: String = "",

    @get:PropertyName("profileImage") @set:PropertyName("profileImage")
    var profileImage: String = "",

    @get:PropertyName("mailID") @set:PropertyName("mailID")
    var mailID: String? = null,

    @get:PropertyName("phoneNumber") @set:PropertyName("phoneNumber")
    var phoneNumber: String = "",

    @get:PropertyName("password") @set:PropertyName("password")
    var password: String = "",

    @get:PropertyName("groupFundID") @set:PropertyName("groupFundID")
    var groupFundID: String = "",

    @get:PropertyName("role") @set:PropertyName("role")
    var role: GroupFundUserType = GroupFundUserType.GROUP_FUND_MEMBER,

    @get:PropertyName("memberCreatedDate") @set:PropertyName("memberCreatedDate")
    var memberCreatedDate: Timestamp? = null,

    @get:PropertyName("totalContributionPaid") @set:PropertyName("totalContributionPaid")
    var totalContributionPaid: Int = 0,

    @get:PropertyName("totalLoanBorrowed") @set:PropertyName("totalLoanBorrowed")
    var totalLoanBorrowed: Int = 0,

    @get:PropertyName("totalLoanPaid") @set:PropertyName("totalLoanPaid")
    var totalLoanPaid: Int = 0,

    @get:PropertyName("totalInterestPaid") @set:PropertyName("totalInterestPaid")
    var totalInterestPaid: Int = 0,

    @get:PropertyName("recordStatus") @set:PropertyName("recordStatus")
    var recordStatus: RecordStatus = RecordStatus.ACTIVE,

    @get:PropertyName("recordDate") @set:PropertyName("recordDate")
    var recordDate: Date = Date(),

    @get:PropertyName("upiBeneId") @set:PropertyName("upiBeneId")
    var upiBeneId: String = "",

    @get:PropertyName("bankBeneId") @set:PropertyName("bankBeneId")
    var bankBeneId: String = "",

    @get:PropertyName("upiID") @set:PropertyName("upiID")
    var upiID: String = ""
) {
    // 🔹 Firestore needs a no-arg constructor
    constructor() : this(
        id = null,
        name = "",
        profileImage = "",
        mailID = null,
        phoneNumber = "",
        password = "",
        groupFundID = "",
        role = GroupFundUserType.GROUP_FUND_MEMBER,
        memberCreatedDate = null,
        totalContributionPaid = 0,
        totalLoanBorrowed = 0,
        totalLoanPaid = 0,
        totalInterestPaid = 0,
        recordStatus = RecordStatus.ACTIVE,
        recordDate = Date(),
        upiBeneId = "",
        bankBeneId = "",
        upiID = ""
    )
}