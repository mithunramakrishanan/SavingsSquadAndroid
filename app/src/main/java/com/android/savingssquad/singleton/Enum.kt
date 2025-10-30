package com.android.savingssquad.singleton

import androidx.compose.ui.graphics.Color
import com.android.savingssquad.model.PaymentsDetails
import kotlinx.serialization.Serializable

// MARK: - Payment Related
enum class PaymentType(val value: String) {
    PAYMENT_DEBIT("AMOUNT DEBIT"),    // Money going out (to member)
    PAYMENT_CREDIT("AMOUNT CREDIT")   // Money coming in (from member)
}

enum class PaymentSubType(val value: String) {
    INTEREST_AMOUNT("INTEREST AMOUNT"),
    EMI_AMOUNT("EMI AMOUNT"),
    CONTRIBUTION_AMOUNT("CONTRIBUTION AMOUNT"),
    LOAN_AMOUNT("LOAN AMOUNT"),
    OTHERS_AMOUNT("OTHERS AMOUNT")
}

enum class PaymentEntryType(val value: String) {
    MANUAL_ENTRY("MANUAL ENTRY"),
    AUTOMATIC_ENTRY("AUTOMATIC ENTRY")
}

enum class PaidStatus(val value: String) {
    PAID("PAID"),
    NOT_PAID("NOT PAID")
}

// MARK: - EMI
enum class EMIStatus(val value: String) {
    PENDING("PENDING"),
    PAID("PAID"),
    OVERDUE("OVERDUE"),
    FAILED("FAILED");

    val displayText: String
        get() = when (this) {
            PENDING -> "PENDING"
            PAID -> "PAID"
            OVERDUE -> "OVERDUE"
            FAILED -> "FAILED"
        }

    val color: Color
        get() = when (this) {
            PENDING -> Color(0xFFFFA500) // Orange
            PAID -> Color(0xFF4CAF50)    // Green
            OVERDUE -> Color(0xFFF44336) // Red
            FAILED -> Color(0xFF9E9E9E)  // Gray
        }
}

// MARK: - Payout
enum class PayoutStatus(val value: String) {
    PENDING("PENDING"),
    INITIATED("RECEIVED"),
    IN_PROGRESS("PAYOUT_INPROGRESS"),
    SUCCESS("PAYOUT_SUCCESS"),
    FAILED("PAYOUT_FAILED"),
    CANCELLED("PAYOUT_CANCELLED"),
    REVERSED("PAYOUT_REVERSED")
}

// MARK: - Group Fund
enum class GroupFundUserType(val value: String) {
    GROUP_FUND_MANAGER("AS MANAGER"),
    GROUP_FUND_MEMBER("AS MEMBER");

    val roleDescription: String
        get() = value
}

enum class GroupFundActivityType(val value: String) {
    AMOUNT_DEBIT("AMOUNT DEBIT"),
    AMOUNT_CREDIT("AMOUNT CREDIT"),
    OTHER_ACTIVITY("OTHER ACTIVITY")
}

// MARK: - Reminder
enum class ReminderType(val value: String) {
    CONTRIBUTION("Contribution"),
    EMI("EMI"),
    OTHER_REMAINDER("Other Remainder")
}

// MARK: - DB
sealed class DatabaseError(val description: String) {
    object OpenDatabaseFailed : DatabaseError("Unable to open database")
    class PrepareStatementFailed(val sql: String) : DatabaseError("Failed to prepare statement: $sql")
    class ExecutionFailed(val msg: String) : DatabaseError("Execution failed: $msg")
    object StepFailed : DatabaseError("Step execution failed")
    object Unknown : DatabaseError("Unknown database error")
}

// MARK: - Record Status
enum class RecordStatus {
    ACTIVE,
    INACTIVE,
    DELETED;

    val color: Color
        get() = when (this) {
            ACTIVE -> Color.Green
            INACTIVE -> Color.Gray
            DELETED -> Color.Red
        }
}

// MARK: - Cashfree
sealed class CashfreePaymentAction {
    data class New(val payment: PaymentsDetails) : CashfreePaymentAction()
    data class Retry(val failedOrderId: String) : CashfreePaymentAction()
}

enum class CashfreeBeneficiaryType(val value: String) {
    BANK("banktransfer"),
    UPI("upi"),
    CARD("card"),
    PAYPAL("paypal")
}

// MARK: - UI / Alerts
enum class AlertType {
    SUCCESS,
    ERROR,
    INFO
}

enum class PaymentStatus : StatusUI {

    PENDING,
    IN_PROGRESS,
    SUCCESS,
    FAILED,
    USER_DROPPED,
    CANCELLED,
    REFUNDED,
    VOID;

    override val iconName: String
        get() = when (this) {
            SUCCESS -> "check_circle"
            FAILED -> "cancel"
            PENDING, IN_PROGRESS -> "schedule"
            REFUNDED -> "undo"
            VOID, USER_DROPPED, CANCELLED -> "block"
        }

    override val color: Color
        get() = when (this) {
            SUCCESS -> Color(0xFF4CAF50)   // Green
            FAILED, CANCELLED, USER_DROPPED, VOID -> Color(0xFFF44336)  // Red
            PENDING, IN_PROGRESS -> Color(0xFFFF9800) // Orange
            REFUNDED -> Color(0xFF2196F3)  // Blue
        }

    override val backgroundColor: Color
        get() = color.copy(alpha = 0.05f)
}

// Extension property to get display text
val PaymentStatus.displayText: String
    get() = when (this) {
        PaymentStatus.PENDING      -> "Waiting for Payment"
        PaymentStatus.IN_PROGRESS  -> "Payment In Progress"
        PaymentStatus.SUCCESS      -> "Payment Successful"
        PaymentStatus.FAILED       -> "Payment Failed"
        PaymentStatus.USER_DROPPED -> "Payment Dropped"
        PaymentStatus.CANCELLED    -> "Payment Cancelled"
        PaymentStatus.REFUNDED     -> "Refunded"
        PaymentStatus.VOID         -> "Payment Voided"
    }


enum class RemainderType(val type: String) {
    CONTRIBUTION("Contribution"),
    EMI("EMI"),
    OTHER_REMAINDER("Other Remainder");

    companion object {
        fun fromString(value: String): RemainderType {
            return entries.firstOrNull { it.type.equals(value, ignoreCase = true) }
                ?: OTHER_REMAINDER
        }
    }
}

enum class RootType { LOGIN, MANAGER, MEMBER }
