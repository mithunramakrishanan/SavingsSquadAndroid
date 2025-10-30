package com.android.savingssquad.model
import androidx.compose.ui.graphics.Color

// --------------------------
// MARK: - Payout Status Result
// --------------------------
data class PayoutStatusResult(
    val status: String? = null,
    val updatedOn: String? = null,
    val transferId: String? = null,
    val statusDescription: String? = null
)

// --------------------------
// MARK: - Basic Beneficiary Details
// --------------------------
data class BeneficiaryBasic(
    val beneficiaryId: String? = null
)

// --------------------------
// MARK: - Full Beneficiary Details
// --------------------------
data class BeneficiaryFull(
    val beneficiaryId: String? = null,
    val beneficiaryName: String? = null,
    val beneficiaryInstrumentDetails: BeneficiaryInstrumentDetails? = null,
    val beneficiaryContactDetails: BeneficiaryContactDetails? = null,
    val beneficiaryStatus: String? = null,
    val addedOn: String? = null
)

// --------------------------
// MARK: - Instrument Details (Bank / UPI)
// --------------------------
data class BeneficiaryInstrumentDetails(
    val bankAccountNumber: String? = null,
    val bankIfsc: String? = null,
    val vpa: String? = null
)

// --------------------------
// MARK: - Contact Details
// --------------------------
data class BeneficiaryContactDetails(
    val beneficiaryEmail: String? = null,
    val beneficiaryPhone: String? = null,
    val beneficiaryCountryCode: String? = null,
    val beneficiaryAddress: String? = null,
    val beneficiaryCity: String? = null,
    val beneficiaryState: String? = null,
    val beneficiaryPostalCode: String? = null
)

// --------------------------
// MARK: - Payout Status Enum
// --------------------------
enum class PayoutStatus {
    Pending,
    Initiated,
    InProgress,
    Success,
    Failed,
    Cancelled,
    Reversed;

    val displayText: String
        get() = when (this) {
            Pending -> "Waiting for Payout"
            Initiated -> "Payout Initiated"
            InProgress -> "Payout In Progress"
            Success -> "Payout Successful"
            Failed -> "Payout Failed"
            Cancelled -> "Payout Cancelled"
            Reversed -> "Payout Reversed"
        }
}

// --------------------------
// MARK: - StatusUI (equivalent of Swift protocol)
// --------------------------
interface StatusUI {
    val iconName: String
    val color: Color
    val backgroundColor: Color
}

// --------------------------
// MARK: - PayoutStatus Extension implementing StatusUI
// --------------------------
fun PayoutStatus.toStatusUI(): StatusUI {
    return object : StatusUI {
        override val iconName: String
            get() = when (this@toStatusUI) {
                PayoutStatus.Success -> "check_circle"
                PayoutStatus.Failed -> "cancel"
                PayoutStatus.Cancelled -> "block"
                PayoutStatus.Initiated, PayoutStatus.InProgress, PayoutStatus.Pending -> "schedule"
                PayoutStatus.Reversed -> "autorenew"
            }

        override val color: Color
            get() = when (this@toStatusUI) {
                PayoutStatus.Success -> Color(0xFF4CAF50) // Green
                PayoutStatus.Failed, PayoutStatus.Cancelled -> Color(0xFFF44336) // Red
                PayoutStatus.Initiated, PayoutStatus.InProgress, PayoutStatus.Pending -> Color(0xFFFF9800) // Orange
                PayoutStatus.Reversed -> Color(0xFF9C27B0) // Purple
            }

        override val backgroundColor: Color
            get() = color.copy(alpha = 0.05f)
    }
}