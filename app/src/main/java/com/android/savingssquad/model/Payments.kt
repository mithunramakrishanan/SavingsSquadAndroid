package com.android.savingssquad.model

import com.android.savingssquad.singleton.GroupFundUserType
import com.android.savingssquad.singleton.PaymentEntryType
import com.android.savingssquad.singleton.PaymentStatus
import com.android.savingssquad.singleton.PaymentSubType
import com.android.savingssquad.singleton.PaymentType
import com.android.savingssquad.singleton.RecordStatus
import com.google.firebase.Timestamp
import java.util.Date
import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName

// ----------------------
// ✅ BeneficiaryDetails
// ----------------------
@Keep
data class BeneficiaryDetails(

    @get:PropertyName("beneficiaryId") @set:PropertyName("beneficiaryId")
    var beneficiaryId: String = "",

    @get:PropertyName("beneficiaryName") @set:PropertyName("beneficiaryName")
    var beneficiaryName: String = "",

    @get:PropertyName("instrumentDetails") @set:PropertyName("instrumentDetails")
    var instrumentDetails: InstrumentDetails = InstrumentDetails(),

    @get:PropertyName("contactDetails") @set:PropertyName("contactDetails")
    var contactDetails: ContactDetails = ContactDetails(),

    @get:PropertyName("status") @set:PropertyName("status")
    var status: String? = null,

    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: String? = null,

    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: String? = null
) {
    constructor() : this(
        beneficiaryId = "",
        beneficiaryName = "",
        instrumentDetails = InstrumentDetails(),
        contactDetails = ContactDetails(),
        status = null,
        createdAt = null,
        updatedAt = null
    )

    @Keep
    data class InstrumentDetails(
        @get:PropertyName("bankAccountNumber") @set:PropertyName("bankAccountNumber")
        var bankAccountNumber: String? = null,

        @get:PropertyName("bankIfsc") @set:PropertyName("bankIfsc")
        var bankIfsc: String? = null,

        @get:PropertyName("vpa") @set:PropertyName("vpa")
        var vpa: String? = null
    ) {
        constructor() : this(bankAccountNumber = null, bankIfsc = null, vpa = null)
    }

    @Keep
    data class ContactDetails(
        @get:PropertyName("email") @set:PropertyName("email")
        var email: String? = null,

        @get:PropertyName("phone") @set:PropertyName("phone")
        var phone: String? = null,

        @get:PropertyName("countryCode") @set:PropertyName("countryCode")
        var countryCode: String? = null,

        @get:PropertyName("address") @set:PropertyName("address")
        var address: String? = null,

        @get:PropertyName("city") @set:PropertyName("city")
        var city: String? = null,

        @get:PropertyName("state") @set:PropertyName("state")
        var state: String? = null,

        @get:PropertyName("postalCode") @set:PropertyName("postalCode")
        var postalCode: String? = null
    ) {
        constructor() : this(
            email = null,
            phone = null,
            countryCode = null,
            address = null,
            city = null,
            state = null,
            postalCode = null
        )
    }
}

@Keep
data class PaymentsDetails(

    @get:PropertyName("id") @set:PropertyName("id")
    var id: String? = null,

    @get:PropertyName("paymentUpdatedDate") @set:PropertyName("paymentUpdatedDate")
    var paymentUpdatedDate: Timestamp? = null,

    @get:PropertyName("payoutUpdatedDate") @set:PropertyName("payoutUpdatedDate")
    var payoutUpdatedDate: Timestamp? = null,

    @get:PropertyName("memberId") @set:PropertyName("memberId")
    var memberId: String = "",

    @get:PropertyName("memberName") @set:PropertyName("memberName")
    var memberName: String = "",

    @get:PropertyName("paymentPhone") @set:PropertyName("paymentPhone")
    var paymentPhone: String = "",

    @get:PropertyName("paymentEmail") @set:PropertyName("paymentEmail")
    var paymentEmail: String = "",

    @get:PropertyName("userType") @set:PropertyName("userType")
    var userType: GroupFundUserType = GroupFundUserType.GROUP_FUND_MEMBER,

    @get:PropertyName("amount") @set:PropertyName("amount")
    var amount: Int = 0,

    @get:PropertyName("intrestAmount") @set:PropertyName("intrestAmount")
    var intrestAmount: Int = 0,

    @get:PropertyName("paymentEntryType") @set:PropertyName("paymentEntryType")
    var paymentEntryType: PaymentEntryType = PaymentEntryType.AUTOMATIC_ENTRY,

    @get:PropertyName("paymentType") @set:PropertyName("paymentType")
    var paymentType: PaymentType = PaymentType.PAYMENT_CREDIT,

    @get:PropertyName("paymentSubType") @set:PropertyName("paymentSubType")
    var paymentSubType: PaymentSubType = PaymentSubType.CONTRIBUTION_AMOUNT,

    @get:PropertyName("paymentStatus") @set:PropertyName("paymentStatus")
    var paymentStatus: PaymentStatus = PaymentStatus.IN_PROGRESS,

    @get:PropertyName("payoutStatus") @set:PropertyName("payoutStatus")
    var payoutStatus: PayoutStatus = PayoutStatus.IN_PROGRESS,

    @get:PropertyName("description") @set:PropertyName("description")
    var description: String = "",

    @get:PropertyName("groupFundId") @set:PropertyName("groupFundId")
    var groupFundId: String = "",

    @get:PropertyName("paymentSessionId") @set:PropertyName("paymentSessionId")
    var paymentSessionId: String = "",

    @get:PropertyName("orderId") @set:PropertyName("orderId")
    var orderId: String = "",

    @get:PropertyName("contributionId") @set:PropertyName("contributionId")
    var contributionId: String = "",

    @get:PropertyName("loanId") @set:PropertyName("loanId")
    var loanId: String = "",

    @get:PropertyName("installmentId") @set:PropertyName("installmentId")
    var installmentId: String = "",

    @get:PropertyName("transferMode") @set:PropertyName("transferMode")
    var transferMode: String = "",

    @get:PropertyName("beneId") @set:PropertyName("beneId")
    var beneId: String = "",

    @get:PropertyName("paymentSuccess") @set:PropertyName("paymentSuccess")
    var paymentSuccess: Boolean = false,

    @get:PropertyName("paymentResponseMessage") @set:PropertyName("paymentResponseMessage")
    var paymentResponseMessage: String = "",

    @get:PropertyName("payoutSuccess") @set:PropertyName("payoutSuccess")
    var payoutSuccess: Boolean = false,

    @get:PropertyName("payoutResponseMessage") @set:PropertyName("payoutResponseMessage")
    var payoutResponseMessage: String = "",

    @get:PropertyName("transferReferenceId") @set:PropertyName("transferReferenceId")
    var transferReferenceId: String = "",

    @get:PropertyName("recordStatus") @set:PropertyName("recordStatus")
    var recordStatus: RecordStatus = RecordStatus.ACTIVE,

    @get:PropertyName("recordDate") @set:PropertyName("recordDate")
    var recordDate: Timestamp = Timestamp(Date())
) {
    constructor() : this(
        id = null,
        paymentUpdatedDate = null,
        payoutUpdatedDate = null,
        memberId = "",
        memberName = "",
        paymentPhone = "",
        paymentEmail = "",
        userType = GroupFundUserType.GROUP_FUND_MEMBER,
        amount = 0,
        intrestAmount = 0,
        paymentEntryType = PaymentEntryType.AUTOMATIC_ENTRY,
        paymentType = PaymentType.PAYMENT_CREDIT,
        paymentSubType = PaymentSubType.CONTRIBUTION_AMOUNT,
        paymentStatus = PaymentStatus.IN_PROGRESS,
        payoutStatus = PayoutStatus.IN_PROGRESS,
        description = "",
        groupFundId = "",
        paymentSessionId = "",
        orderId = "",
        contributionId = "",
        loanId = "",
        installmentId = "",
        transferMode = "",
        beneId = "",
        paymentSuccess = false,
        paymentResponseMessage = "",
        payoutSuccess = false,
        payoutResponseMessage = "",
        transferReferenceId = "",
        recordStatus = RecordStatus.ACTIVE,
        recordDate = Timestamp(Date())
    )

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "paymentUpdatedDate" to paymentUpdatedDate,
        "payoutUpdatedDate" to payoutUpdatedDate,
        "memberId" to memberId,
        "memberName" to memberName,
        "paymentPhone" to paymentPhone,
        "paymentEmail" to paymentEmail,
        "userType" to userType.name,
        "amount" to amount,
        "intrestAmount" to intrestAmount,
        "paymentEntryType" to paymentEntryType.name,
        "paymentType" to paymentType.name,
        "paymentSubType" to paymentSubType.name,
        "paymentStatus" to paymentStatus.name,
        "payoutStatus" to payoutStatus.name,
        "description" to description,
        "groupFundId" to groupFundId,
        "paymentSessionId" to paymentSessionId,
        "orderId" to orderId,
        "contributionId" to contributionId,
        "loanId" to loanId,
        "installmentId" to installmentId,
        "transferMode" to transferMode,
        "beneId" to beneId,
        "paymentSuccess" to paymentSuccess,
        "paymentResponseMessage" to paymentResponseMessage,
        "payoutSuccess" to payoutSuccess,
        "payoutResponseMessage" to payoutResponseMessage,
        "transferReferenceId" to transferReferenceId,
        "recordStatus" to recordStatus.name,
        "recordDate" to recordDate
    )
}

// ----------------------
// ✅ Array (List) Extensions
// ----------------------
val List<PaymentsDetails>.debitPayments: List<PaymentsDetails>
    get() = this.filter { it.paymentType == PaymentType.PAYMENT_DEBIT }

val List<PaymentsDetails>.creditPayments: List<PaymentsDetails>
    get() = this.filter { it.paymentType == PaymentType.PAYMENT_CREDIT }

val List<PaymentsDetails>.totalDebit: Int
    get() = debitPayments.sumOf { it.amount }

val List<PaymentsDetails>.totalCredit: Int
    get() = creditPayments.sumOf { it.amount }

val List<PaymentsDetails>.totalBySubType: Map<PaymentSubType, Int>
    get() = this.groupBy { it.paymentSubType }
        .mapValues { (_, payments) -> payments.sumOf { it.amount } }

fun List<PaymentsDetails>.filterBySubType(subType: PaymentSubType): List<PaymentsDetails> =
    this.filter { it.paymentSubType == subType }

