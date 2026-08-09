package com.android.savingssquad.singleton

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import com.android.savingssquad.model.PaymentsDetails


import com.google.firebase.firestore.PropertyName

// ------------------------------
// MARK: - Payment Type
// ------------------------------
enum class PaymentType {
    PAYMENT_DEBIT,
    PAYMENT_CREDIT;


    val localizedName: String

        get() = when (this) {

            PAYMENT_DEBIT -> SquadStrings.paymentDebit

            PAYMENT_CREDIT -> SquadStrings.paymentCredit

        }
}

// ------------------------------
// MARK: - Payment SubType
// ------------------------------
enum class PaymentSubType {
    INTEREST_AMOUNT,
    EMI_AMOUNT,
    CONTRIBUTION_AMOUNT,
    LOAN_AMOUNT,
    OTHERS_AMOUNT,
    RE_PAYMENT,
    SETTLEMENT;

    val localizedName: String

        get() = when (this) {

            INTEREST_AMOUNT -> SquadStrings.interest

            EMI_AMOUNT -> SquadStrings.emiAmount

            CONTRIBUTION_AMOUNT -> SquadStrings.contributionAmount

            LOAN_AMOUNT -> SquadStrings.loanAmount

            OTHERS_AMOUNT -> SquadStrings.others

            RE_PAYMENT -> SquadStrings.repayment

            SETTLEMENT -> SquadStrings.settlement

        }
}

// ------------------------------
// MARK: - Payment Status
// ------------------------------
enum class PaymentStatus {
    PENDING,
    INPROGRESS,
    SUCCESS,
    FAILED,
    USER_DROPPED,
    CANCELLED,
    REFUNDED,
    VOID,
    INVERIFICATION;

    val localizedName: String

        get() = when (this) {

            PENDING -> SquadStrings.pending

            INPROGRESS -> SquadStrings.inProgress

            SUCCESS -> SquadStrings.success

            FAILED -> SquadStrings.failed

            USER_DROPPED -> SquadStrings.userDropped

            CANCELLED -> SquadStrings.cancelled

            REFUNDED -> SquadStrings.refunded

            VOID -> SquadStrings.voidStatus

            INVERIFICATION -> SquadStrings.inVerification

        }
}

// ------------------------------
// MARK: - Payment Entry Type
// ------------------------------
enum class PaymentEntryType {
    MANUAL_ENTRY,
    AUTOMATIC_ENTRY;

    val localizedName: String

        get() = when (this) {

            MANUAL_ENTRY -> SquadStrings.manualEntry

            AUTOMATIC_ENTRY -> SquadStrings.automaticEntry

        }
}

// ------------------------------
// MARK: - Paid Status
// ------------------------------
enum class PaidStatus {

    PAID,
    NOT_PAID,
    INVERIFICATION;

    val localizedName: String
        get() = when (this) {
            PAID -> SquadStrings.paid
            NOT_PAID -> SquadStrings.unpaid
            INVERIFICATION -> SquadStrings.inVerification
        }
}

enum class EMIStatus {

    CREATED,
    PENDING,
    PAID,
    OVERDUE,
    FAILED,
    INVERIFICATION;

    val localizedName: String
        get() = when (this) {
            CREATED -> SquadStrings.created
            PENDING -> SquadStrings.pending
            PAID -> SquadStrings.paid
            OVERDUE -> SquadStrings.overdue
            FAILED -> SquadStrings.failed
            INVERIFICATION -> SquadStrings.inVerification
        }
}

val PaymentStatus.displayText: String
    get() = when (this) {
        PaymentStatus.PENDING ->
            SquadStrings.paymentWaitingForPayment

        PaymentStatus.INPROGRESS ->
            SquadStrings.paymentInProgress

        PaymentStatus.SUCCESS ->
            SquadStrings.paymentSuccessful

        PaymentStatus.FAILED ->
            SquadStrings.paymentFailed

        PaymentStatus.USER_DROPPED ->
            SquadStrings.paymentDropped

        PaymentStatus.CANCELLED ->
            SquadStrings.paymentCancelled

        PaymentStatus.REFUNDED ->
            SquadStrings.refunded

        PaymentStatus.VOID ->
            SquadStrings.paymentVoided

        PaymentStatus.INVERIFICATION ->
            SquadStrings.paymentInVerification
    }

enum class PayoutStatus {

    PENDING,
    RECEIVED,
    PAYOUT_INPROGRESS,
    PAYOUT_SUCCESS,
    PAYOUT_FAILED,
    PAYOUT_CANCELLED,
    PAYOUT_REVERSED;

    val localizedName: String
        get() = when (this) {
            PENDING -> SquadStrings.pending
            RECEIVED -> SquadStrings.received
            PAYOUT_INPROGRESS -> SquadStrings.inProgress
            PAYOUT_SUCCESS -> SquadStrings.success
            PAYOUT_FAILED -> SquadStrings.failed
            PAYOUT_CANCELLED -> SquadStrings.cancelled
            PAYOUT_REVERSED -> SquadStrings.reversed
        }
}

enum class SquadUserType {

    SQUAD_MANAGER,
    SQUAD_MEMBER;

    val localizedName: String
        get() = when (this) {
            SQUAD_MANAGER -> SquadStrings.roleAsManager
            SQUAD_MEMBER -> SquadStrings.roleAsMember
        }
}

enum class SquadActivityType {

    AMOUNT_DEBIT,
    AMOUNT_CREDIT,
    AMOUNT_EDIT,
    OTHER_ACTIVITY;

    val localizedName: String
        get() = when (this) {
            AMOUNT_DEBIT -> SquadStrings.amountDebit
            AMOUNT_CREDIT -> SquadStrings.amountCredit
            AMOUNT_EDIT -> SquadStrings.amountEdit
            OTHER_ACTIVITY -> SquadStrings.otherActivity
        }
}

enum class RemainderType {

    CONTRIBUTION,
    EMI,
    OTHER_REMAINDER;

    val localizedName: String
        get() = when (this) {
            CONTRIBUTION -> SquadStrings.contribution
            EMI -> SquadStrings.emi
            OTHER_REMAINDER -> SquadStrings.otherReminder
        }
}

enum class RecordStatus {

    ACTIVE,
    INACTIVE,
    DELETED,
    COMPLETED;

    val localizedName: String
        get() = when (this) {
            ACTIVE -> SquadStrings.active
            INACTIVE -> SquadStrings.inactive
            DELETED -> SquadStrings.deleted
            COMPLETED -> SquadStrings.completed
        }

    val color: Color
        get() = when (this) {
            ACTIVE -> Color(0xFF4CAF50)
            INACTIVE -> Color(0xFF9E9E9E)
            DELETED -> Color(0xFFE53935)
            COMPLETED -> Color(0xFF4CAF50)
        }

    fun tintColor(): Color {
        return when (this) {
            ACTIVE -> Color(0xFF2ECC71)
            INACTIVE -> Color(0xFFE74C3C)
            DELETED -> Color(0xFFE74C3C)
            COMPLETED -> Color(0xFF2ECC71)
        }
    }

    companion object {
        val toggleCases = listOf(ACTIVE, INACTIVE)
    }
}
enum class CashfreeBeneficiaryType {
    BANKTRANSFER,
    UPI,
    CARD,
    PAYPAL
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

enum class PaymentApproveStatus {

    NOT_REQUESTED,
    REQUESTED,
    ACCEPTED,
    REJECTED;

    val localizedName: String
        get() = when (this) {
            NOT_REQUESTED -> SquadStrings.notRequested
            REQUESTED -> SquadStrings.requested
            ACCEPTED -> SquadStrings.accepted
            REJECTED -> SquadStrings.rejected
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


enum class PaymentFilter {
    ALL,
    CREDIT,
    DEBIT;
    val localizedName: String

        get() = when (this) {

            ALL -> SquadStrings.all

            CREDIT -> SquadStrings.paymentCredit

            DEBIT -> SquadStrings.paymentDebit

        }
}

enum class MemberPaymentType(
    val value: String
) {
    Loan("LOAN"),
    Others("OTHERS");

    val localizedName: String
        get() = when (this) {
            Loan -> SquadStrings.loan
            Others -> SquadStrings.others
        }
}

enum class MemberPaymentSubType(
    val value: String
) {
    RE_PAYMENT("RE_PAYMENT"),
    SETTLEMENT("SETTLEMENT");

    val localizedName: String
        get() = when (this) {
            RE_PAYMENT -> SquadStrings.repayment
            SETTLEMENT -> SquadStrings.settlement
        }
}

enum class SquadLanguages(val value: String) {
    ENGLISH("English"),
    TAMIL("தமிழ்")
}
