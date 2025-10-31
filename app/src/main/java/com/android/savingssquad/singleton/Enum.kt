package com.android.savingssquad.singleton

import androidx.compose.ui.graphics.Color
import com.android.savingssquad.model.PaymentsDetails
import kotlinx.serialization.Serializable


import com.google.firebase.firestore.PropertyName

// ------------------------------
// MARK: - Payment Type
// ------------------------------
enum class PaymentType(@get:PropertyName("value") val value: String) {
    @PropertyName("AMOUNT DEBIT") PAYMENT_DEBIT("AMOUNT DEBIT"),
    @PropertyName("AMOUNT CREDIT") PAYMENT_CREDIT("AMOUNT CREDIT");

    companion object {
        fun fromValue(value: String?): PaymentType =
            entries.find { it.value == value } ?: PAYMENT_DEBIT
    }
}

// ------------------------------
// MARK: - Payment SubType
// ------------------------------
enum class PaymentSubType(@get:PropertyName("value") val value: String) {
    @PropertyName("INTEREST AMOUNT") INTEREST_AMOUNT("INTEREST AMOUNT"),
    @PropertyName("EMI AMOUNT") EMI_AMOUNT("EMI AMOUNT"),
    @PropertyName("CONTRIBUTION AMOUNT") CONTRIBUTION_AMOUNT("CONTRIBUTION AMOUNT"),
    @PropertyName("LOAN AMOUNT") LOAN_AMOUNT("LOAN AMOUNT"),
    @PropertyName("OTHERS AMOUNT") OTHERS_AMOUNT("OTHERS AMOUNT");

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
    @PropertyName("INPROGRESS") IN_PROGRESS("INPROGRESS"),
    @PropertyName("SUCCESS") SUCCESS("SUCCESS"),
    @PropertyName("FAILED") FAILED("FAILED"),
    @PropertyName("USER_DROPPED") USER_DROPPED("USER_DROPPED"),
    @PropertyName("CANCELLED") CANCELLED("CANCELLED"),
    @PropertyName("REFUNDED") REFUNDED("REFUNDED"),
    @PropertyName("VOID") VOID("VOID");

    companion object {
        fun fromValue(value: String?): PaymentStatus =
            entries.find { it.value == value } ?: PENDING
    }
}

// ------------------------------
// MARK: - Payment Entry Type
// ------------------------------
enum class PaymentEntryType(@get:PropertyName("value") val value: String) {
    @PropertyName("MANUAL ENTRY") MANUAL_ENTRY("MANUAL ENTRY"),
    @PropertyName("AUTOMATIC ENTRY") AUTOMATIC_ENTRY("AUTOMATIC ENTRY");

    companion object {
        fun fromValue(value: String?): PaymentEntryType =
            entries.find { it.value == value } ?: MANUAL_ENTRY
    }
}

// ------------------------------
// MARK: - Paid Status
// ------------------------------
enum class PaidStatus(@get:PropertyName("value") val value: String) {
    @PropertyName("PAID") PAID("PAID"),
    @PropertyName("NOT PAID") NOT_PAID("NOT PAID");

    companion object {
        fun fromValue(value: String?): PaidStatus =
            entries.find { it.value == value } ?: NOT_PAID
    }
}

// ------------------------------
// MARK: - EMI Status
// ------------------------------
enum class EMIStatus(@get:PropertyName("value") val value: String) {
    @PropertyName("PENDING") PENDING("PENDING"),
    @PropertyName("PAID") PAID("PAID"),
    @PropertyName("OVERDUE") OVERDUE("OVERDUE"),
    @PropertyName("FAILED") FAILED("FAILED");

    companion object {
        fun fromValue(value: String?): EMIStatus =
            entries.find { it.value == value } ?: PENDING
    }
}

// ------------------------------
// MARK: - Payout Status
// ------------------------------
enum class PayoutStatus(@get:PropertyName("value") val value: String) {
    @PropertyName("PENDING") PENDING("PENDING"),
    @PropertyName("RECEIVED") INITIATED("RECEIVED"),
    @PropertyName("PAYOUT_INPROGRESS") IN_PROGRESS("PAYOUT_INPROGRESS"),
    @PropertyName("PAYOUT_SUCCESS") SUCCESS("PAYOUT_SUCCESS"),
    @PropertyName("PAYOUT_FAILED") FAILED("PAYOUT_FAILED"),
    @PropertyName("PAYOUT_CANCELLED") CANCELLED("PAYOUT_CANCELLED"),
    @PropertyName("PAYOUT_REVERSED") REVERSED("PAYOUT_REVERSED");

    companion object {
        fun fromValue(value: String?): PayoutStatus =
            entries.find { it.value == value } ?: PENDING
    }
}

// ------------------------------
// MARK: - Group Fund User Type
// ------------------------------
enum class GroupFundUserType(@get:PropertyName("value") val value: String) {
    @PropertyName("AS MANAGER") GROUP_FUND_MANAGER("AS MANAGER"),
    @PropertyName("AS MEMBER") GROUP_FUND_MEMBER("AS MEMBER");

    val roleDescription: String get() = value

    companion object {
        fun fromValue(value: String?): GroupFundUserType =
            entries.find { it.value == value } ?: GROUP_FUND_MEMBER
    }
}

// ------------------------------
// MARK: - Group Fund Activity Type
// ------------------------------
enum class GroupFundActivityType(@get:PropertyName("value") val value: String) {
    @PropertyName("AMOUNT DEBIT") AMOUNT_DEBIT("AMOUNT DEBIT"),
    @PropertyName("AMOUNT CREDIT") AMOUNT_CREDIT("AMOUNT CREDIT"),
    @PropertyName("OTHER ACTIVITY") OTHER_ACTIVITY("OTHER ACTIVITY");

    companion object {
        fun fromValue(value: String?): GroupFundActivityType =
            entries.find { it.value == value } ?: OTHER_ACTIVITY
    }
}

// ------------------------------
// MARK: - Reminder Type
// ------------------------------
enum class RemainderType(@get:PropertyName("value") val value: String) {
    @PropertyName("Contribution") CONTRIBUTION("Contribution"),
    @PropertyName("EMI") EMI("EMI"),
    @PropertyName("Other Remainder") OTHER_REMAINDER("Other Remainder");

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
    @PropertyName("banktransfer") BANK("banktransfer"),
    @PropertyName("upi") UPI("upi"),
    @PropertyName("card") CARD("card"),
    @PropertyName("paypal") PAYPAL("paypal");

    companion object {
        fun fromValue(value: String?): CashfreeBeneficiaryType =
            entries.find { it.value == value } ?: BANK
    }
}

// ------------------------------
// MARK: - EMIStatus Extensions
// ------------------------------
val EMIStatus.displayText: String
    get() = when (this) {
        EMIStatus.PENDING -> "PENDING"
        EMIStatus.PAID -> "PAID"
        EMIStatus.OVERDUE -> "OVERDUE"
        EMIStatus.FAILED -> "FAILED"
    }

val EMIStatus.color: Color
    get() = when (this) {
        EMIStatus.PENDING -> Color(0xFFFFA500) // Orange
        EMIStatus.PAID -> Color(0xFF4CAF50)    // Green
        EMIStatus.OVERDUE -> Color(0xFFE53935) // Red
        EMIStatus.FAILED -> Color(0xFF9E9E9E)  // Gray
    }

// ------------------------------
// MARK: - PaymentStatus Extensions
// ------------------------------
val PaymentStatus.displayText: String
    get() = when (this) {
        PaymentStatus.PENDING -> "Waiting for Payment"
        PaymentStatus.IN_PROGRESS -> "Payment In Progress"
        PaymentStatus.SUCCESS -> "Payment Successful"
        PaymentStatus.FAILED -> "Payment Failed"
        PaymentStatus.USER_DROPPED -> "Payment Dropped"
        PaymentStatus.CANCELLED -> "Payment Cancelled"
        PaymentStatus.REFUNDED -> "Refunded"
        PaymentStatus.VOID -> "Payment Voided"
    }

// ------------------------------
// MARK: - Cashfree Payment Action
// ------------------------------
sealed class CashfreePaymentAction {
    data class New(val payment: PaymentsDetails) : CashfreePaymentAction()
    data class Retry(val failedOrderId: String) : CashfreePaymentAction()
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