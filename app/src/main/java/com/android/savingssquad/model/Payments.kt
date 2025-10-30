package com.android.savingssquad.model

import com.android.savingssquad.singleton.GroupFundUserType
import com.android.savingssquad.singleton.PaymentEntryType
import com.android.savingssquad.singleton.PaymentStatus
import com.android.savingssquad.singleton.PaymentSubType
import com.android.savingssquad.singleton.PaymentType
import com.android.savingssquad.singleton.RecordStatus
import com.google.firebase.Timestamp
import java.util.Date

// ----------------------
// ✅ BeneficiaryDetails
// ----------------------
data class BeneficiaryDetails(
    val beneficiaryId: String,
    val beneficiaryName: String,
    val instrumentDetails: InstrumentDetails,
    val contactDetails: ContactDetails,
    val status: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    data class InstrumentDetails(
        val bankAccountNumber: String? = null,
        val bankIfsc: String? = null,
        val vpa: String? = null
    )

    data class ContactDetails(
        val email: String? = null,
        val phone: String? = null,
        val countryCode: String? = null,
        val address: String? = null,
        val city: String? = null,
        val state: String? = null,
        val postalCode: String? = null
    )
}

// ----------------------
// ✅ PaymentsDetails
// ----------------------
data class PaymentsDetails(
    var id: String? = null,
    var paymentUpdatedDate: com.google.firebase.Timestamp? = null,
    var payoutUpdatedDate: com.google.firebase.Timestamp? = null,
    var memberId: String,
    var memberName: String,
    var paymentPhone: String,
    var paymentEmail: String,
    var userType: GroupFundUserType,
    var amount: Int,
    var intrestAmount: Int,
    var paymentEntryType: PaymentEntryType,
    var paymentType: PaymentType,
    var paymentSubType: PaymentSubType,
    var paymentStatus: PaymentStatus = PaymentStatus.IN_PROGRESS,
    var payoutStatus: PayoutStatus = PayoutStatus.InProgress,
    var description: String,
    var groupFundId: String,
    var paymentSessionId: String = "",
    var orderId: String = "",
    var contributionId: String,
    var loanId: String,
    var installmentId: String,
    var transferMode: String = "",
    var beneId: String = "",
    var paymentSuccess: Boolean = false,
    var paymentResponseMessage: String = "",
    var payoutSuccess: Boolean = false,
    var payoutResponseMessage: String = "",
    var transferReferenceId: String,
    var recordStatus: RecordStatus = RecordStatus.ACTIVE,
    var recordDate: com.google.firebase.Timestamp = com.google.firebase.Timestamp(Date())
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
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

