package com.android.savingssquad.singleton

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import com.android.savingssquad.model.PaymentsDetails


import com.google.firebase.firestore.PropertyName

// ------------------------------
// MARK: - Payment Type
// ------------------------------
enum class PaymentType(@get:PropertyName("value") val value: String) {
    @PropertyName("PAYMENT_DEBIT") PAYMENT_DEBIT("PAYMENT_DEBIT"),
    @PropertyName("PAYMENT_CREDIT") PAYMENT_CREDIT("PAYMENT_CREDIT");

    companion object {
        fun fromValue(value: String?): PaymentType =
            entries.find { it.value == value } ?: PAYMENT_DEBIT
    }
}

// ------------------------------
// MARK: - Payment SubType
// ------------------------------
enum class PaymentSubType(@get:PropertyName("value") val value: String) {
    @PropertyName("INTEREST_AMOUNT") INTEREST_AMOUNT("INTEREST_AMOUNT"),
    @PropertyName("EMI_AMOUNT") EMI_AMOUNT("EMI_AMOUNT"),
    @PropertyName("CONTRIBUTION_AMOUNT") CONTRIBUTION_AMOUNT("CONTRIBUTION_AMOUNT"),
    @PropertyName("LOAN_AMOUNT") LOAN_AMOUNT("LOAN_AMOUNT"),
    @PropertyName("OTHERS_AMOUNT") OTHERS_AMOUNT("OTHERS_AMOUNT");

    companion object {
        fun fromValue(value: String?): PaymentSubType =
            entries.find { it.value == value } ?: OTHERS_AMOUNT
    }
}

// ------------------------------
// MARK: - Payment Status
// ------------------------------
enum class PaymentStatus(@get:PropertyName("value") val value: String) {
    @PropertyName("PENDING") PENDING("PENDING"),
    @PropertyName("INPROGRESS") INPROGRESS("INPROGRESS"),
    @PropertyName("SUCCESS") SUCCESS("SUCCESS"),
    @PropertyName("FAILED") FAILED("FAILED"),
    @PropertyName("USER_DROPPED") USER_DROPPED("USER_DROPPED"),
    @PropertyName("CANCELLED") CANCELLED("CANCELLED"),
    @PropertyName("REFUNDED") REFUNDED("REFUNDED"),
    @PropertyName("VOID") VOID("VOID"),

    @PropertyName("INVERIFICATION") INVERIFICATION("INVERIFICATION");

    companion object {
        fun fromValue(value: String?): PaymentStatus =
            entries.find { it.value == value } ?: PENDING
    }
}

// ------------------------------
// MARK: - Payment Entry Type
// ------------------------------
enum class PaymentEntryType(@get:PropertyName("value") val value: String) {
    @PropertyName("MANUAL_ENTRY") MANUAL_ENTRY("MANUAL_ENTRY"),
    @PropertyName("AUTOMATIC_ENTRY") AUTOMATIC_ENTRY("AUTOMATIC_ENTRY");

    companion object {
        fun fromValue(value: String?): PaymentEntryType =
            entries.find { it.value == value } ?: MANUAL_ENTRY
    }
}

// ------------------------------
// MARK: - Paid Status
// ------------------------------
enum class PaidStatus(

    @get:PropertyName("value") val value: String

) {

    @PropertyName("PAID")

    PAID("PAID"),

    @PropertyName("NOT_PAID")

    NOT_PAID("NOT_PAID"),

    @PropertyName("INVERIFICATION")

    INVERIFICATION("INVERIFICATION");

    companion object {

        fun fromValue(value: String?): PaidStatus =

            entries.find { it.value == value } ?: NOT_PAID

    }

}

// ------------------------------
// MARK: - EMI Status
// ------------------------------
enum class EMIStatus(@get:PropertyName("value") val value: String) {
    @PropertyName("CREATED") CREATED("CREATED"),
    @PropertyName("PENDING") PENDING("PENDING"),
    @PropertyName("PAID") PAID("PAID"),
    @PropertyName("OVERDUE") OVERDUE("OVERDUE"),
    @PropertyName("FAILED") FAILED("FAILED"),

    @PropertyName("INVERIFICATION") INVERIFICATION("INVERIFICATION");

    companion object {
        fun fromValue(value: String?): EMIStatus =
            entries.find { it.value == value } ?: PENDING
    }
}

// ------------------------------
// MARK: - Payout Status
// ------------------------------
@Keep
enum class PayoutStatus(@get:PropertyName("value") val value: String) {

    @PropertyName("PENDING")
    PENDING("PENDING"),

    @PropertyName("RECEIVED")
    RECEIVED("RECEIVED"),

    @PropertyName("PAYOUT_INPROGRESS")
    PAYOUT_INPROGRESS("PAYOUT_INPROGRESS"),

    @PropertyName("PAYOUT_SUCCESS")
    PAYOUT_SUCCESS("PAYOUT_SUCCESS"),

    @PropertyName("PAYOUT_FAILED")
    PAYOUT_FAILED("PAYOUT_FAILED"),

    @PropertyName("PAYOUT_CANCELLED")
    PAYOUT_CANCELLED("PAYOUT_CANCELLED"),

    @PropertyName("PAYOUT_REVERSED")
    PAYOUT_REVERSED("PAYOUT_REVERSED");

    val displayText: String
        get() = when (this) {
            PENDING -> "Waiting for Payout"
            RECEIVED -> "Payout Initiated"
            PAYOUT_INPROGRESS -> "Payout In Progress"
            PAYOUT_SUCCESS -> "Payout Successful"
            PAYOUT_FAILED -> "Payout Failed"
            PAYOUT_CANCELLED -> "Payout Cancelled"
            PAYOUT_REVERSED -> "Payout Reversed"
        }

    companion object {
        fun fromValue(value: String?): PayoutStatus {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: PENDING
        }
    }
}
// ------------------------------
// MARK: - Squad User Type
// ------------------------------
enum class SquadUserType(@get:PropertyName("value") val value: String) {
    @PropertyName("SQUAD_MANAGER") SQUAD_MANAGER("SQUAD_MANAGER"),
    @PropertyName("SQUAD_MEMBER") SQUAD_MEMBER("SQUAD_MEMBER");

    val roleDescription: String
        get() = when (this) {
            SQUAD_MANAGER -> "AS MANAGER"
            SQUAD_MEMBER -> "AS MEMBER"
        }

    companion object {
        fun fromValue(value: String?): SquadUserType =
            entries.find { it.value == value } ?: SQUAD_MEMBER
    }
}

// ------------------------------
// MARK: - Squad Activity Type
// ------------------------------
enum class SquadActivityType(@get:PropertyName("value") val value: String) {
    @PropertyName("AMOUNT_DEBIT") AMOUNT_DEBIT("AMOUNT_DEBIT"),
    @PropertyName("AMOUNT_CREDIT") AMOUNT_CREDIT("AMOUNT_CREDIT"),
    @PropertyName("AMOUNT_EDIT") AMOUNT_EDIT("AMOUNT_EDIT"),
    @PropertyName("OTHER_ACTIVITY") OTHER_ACTIVITY("OTHER_ACTIVITY");

    companion object {
        fun fromValue(value: String?): SquadActivityType =
            entries.find { it.value == value } ?: OTHER_ACTIVITY
    }
}

// ------------------------------
// MARK: - Reminder Type
// ------------------------------
enum class RemainderType(@get:PropertyName("value") val value: String) {
    @PropertyName("CONTRIBUTION") CONTRIBUTION("CONTRIBUTION"),
    @PropertyName("EMI") EMI("EMI"),
    @PropertyName("OTHER_REMAINDER") OTHER_REMAINDER("OTHER_REMAINDER");

    companion object {
        fun fromValue(value: String?): RemainderType =
            entries.find { it.value == value } ?: OTHER_REMAINDER
    }
}

// ------------------------------
// MARK: - Record Status
// ------------------------------
enum class RecordStatus(@get:PropertyName("value") val value: String) {
    @PropertyName("ACTIVE") ACTIVE("ACTIVE"),
    @PropertyName("INACTIVE") INACTIVE("INACTIVE"),
    @PropertyName("DELETED") DELETED("DELETED");

    companion object {
        fun fromValue(value: String?): RecordStatus =
            entries.find { it.value == value } ?: ACTIVE
    }

    val color: Color
        get() = when (this) {
            ACTIVE -> Color(0xFF4CAF50)
            INACTIVE -> Color(0xFF9E9E9E)
            DELETED -> Color(0xFFE53935)
        }
}

// ------------------------------
// MARK: - Cashfree Beneficiary Type
// ------------------------------
enum class CashfreeBeneficiaryType(@get:PropertyName("value") val value: String) {
    @PropertyName("banktransfer") banktransfer("banktransfer"),
    @PropertyName("upi") upi("upi"),
    @PropertyName("card") card("card"),
    @PropertyName("paypal") paypal("paypal");

    companion object {
        fun fromValue(value: String?): CashfreeBeneficiaryType =
            entries.find { it.value == value } ?: banktransfer
    }
}

// ------------------------------
// MARK: - EMIStatus Extensions
// ------------------------------
val EMIStatus.displayText: String
    get() = when (this) {
        EMIStatus.CREATED -> "CREATED"
        EMIStatus.PENDING -> "PENDING"
        EMIStatus.PAID -> "PAID"
        EMIStatus.OVERDUE -> "OVERDUE"
        EMIStatus.FAILED -> "FAILED"
        EMIStatus.INVERIFICATION -> "INVERIFICATION"
    }

val EMIStatus.color: Color
    get() = when (this) {
        EMIStatus.CREATED -> Color(0xFFFFA500) // Orange
        EMIStatus.PENDING -> Color(0xFFFFA500) // Orange
        EMIStatus.PAID -> Color(0xFF4CAF50)    // Green
        EMIStatus.OVERDUE -> Color(0xFFE53935) // Red
        EMIStatus.FAILED -> Color(0xFF9E9E9E)  // Gray
        EMIStatus.INVERIFICATION -> Color(0xFFFFA500)  // Gray
    }

// ------------------------------
// MARK: - PaymentStatus Extensions
// ------------------------------
val PaymentStatus.displayText: String
    get() = when (this) {
        PaymentStatus.PENDING -> "Waiting for Payment"
        PaymentStatus.INPROGRESS -> "Payment In Progress"
        PaymentStatus.SUCCESS -> "Payment Successful"
        PaymentStatus.FAILED -> "Payment Failed"
        PaymentStatus.USER_DROPPED -> "Payment Dropped"
        PaymentStatus.CANCELLED -> "Payment Cancelled"
        PaymentStatus.REFUNDED -> "Refunded"
        PaymentStatus.VOID -> "Payment Voided"
        PaymentStatus.INVERIFICATION -> "Payment In Verification"
    }

// ------------------------------
// MARK: - Cashfree Payment Action
// ------------------------------
sealed class CashfreePaymentAction {
    data class New(val payment: PaymentsDetails) : CashfreePaymentAction()
    data class Retry(val failedOrderId: String) : CashfreePaymentAction()
}

sealed class RazorpayPaymentAction {
    data class New(val payment: PaymentsDetails) : RazorpayPaymentAction()
    data class Retry(val failedOrderId: String) : RazorpayPaymentAction()
}

// ------------------------------
// MARK: - Database Error
// ------------------------------
enum class DatabaseError(val description: String) {
    OPEN_DATABASE_FAILED("Unable to open database"),
    PREPARE_STATEMENT_FAILED("Failed to prepare statement"),
    EXECUTION_FAILED("Execution failed"),
    STEP_FAILED("Step execution failed"),
    UNKNOWN("Unknown database error");
}

// ------------------------------
// MARK: - Alert Type
// ------------------------------
enum class AlertType {
    SUCCESS,
    ERROR,
    INFO
}

enum class PaymentApproveStatus(@get:PropertyName("value") val value: String) {
    @PropertyName("NOT_REQUESTED") NOT_REQUESTED("NOT_REQUESTED"),
    @PropertyName("REQUESTED") REQUESTED("REQUESTED"),
    @PropertyName("ACCEPTED") ACCEPTED("ACCEPTED"),
    @PropertyName("REJECTED") REJECTED("REJECTED");

    companion object {
        fun fromValue(value: String?): PaymentApproveStatus =
            entries.find { it.value == value } ?: NOT_REQUESTED
    }
}

enum class AmountEditType {
    contribution,
    loanBorrowed,
    paidLoadAmount,
    intrestAmount,
    totalSquadAmount,
    others

}

enum class Plan(val value: String) {
    FREE("FREE"),
    BASIC("BASIC"),
    BUSINESS("BUSINESS");

    companion object {
        fun from(value: String?): Plan {
            return when (value?.uppercase()) {
                "BASIC" -> BASIC
                "BUSINESS" -> BUSINESS
                else -> FREE
            }
        }
    }
}

enum class PaymentFilter(val displayName: String) {
    ALL("All"),
    CREDIT("Credit"),
    DEBIT("Debit")
}