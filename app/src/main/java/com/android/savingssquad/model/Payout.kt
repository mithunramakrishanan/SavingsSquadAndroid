package com.android.savingssquad.model
import androidx.compose.ui.graphics.Color


import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName

// --------------------------
// MARK: - Payout Status Result
// --------------------------
@Keep
data class PayoutStatusResult(

    @get:PropertyName("status") @set:PropertyName("status")
    var status: String? = null,

    @get:PropertyName("updatedOn") @set:PropertyName("updatedOn")
    var updatedOn: String? = null,

    @get:PropertyName("transferId") @set:PropertyName("transferId")
    var transferId: String? = null,

    @get:PropertyName("statusDescription") @set:PropertyName("statusDescription")
    var statusDescription: String? = null
) {
    constructor() : this(null, null, null, null)
}

// --------------------------
// MARK: - Basic Beneficiary Details
// --------------------------
@Keep
data class BeneficiaryBasic(

    @get:PropertyName("beneficiaryId") @set:PropertyName("beneficiaryId")
    var beneficiaryId: String? = null
) {
    constructor() : this(null)
}

// --------------------------
// MARK: - Full Beneficiary Details
// --------------------------
@Keep
data class BeneficiaryFull(

    @get:PropertyName("beneficiaryId") @set:PropertyName("beneficiaryId")
    var beneficiaryId: String? = null,

    @get:PropertyName("beneficiaryName") @set:PropertyName("beneficiaryName")
    var beneficiaryName: String? = null,

    @get:PropertyName("beneficiaryInstrumentDetails") @set:PropertyName("beneficiaryInstrumentDetails")
    var beneficiaryInstrumentDetails: BeneficiaryInstrumentDetails? = BeneficiaryInstrumentDetails(),

    @get:PropertyName("beneficiaryContactDetails") @set:PropertyName("beneficiaryContactDetails")
    var beneficiaryContactDetails: BeneficiaryContactDetails? = BeneficiaryContactDetails(),

    @get:PropertyName("beneficiaryStatus") @set:PropertyName("beneficiaryStatus")
    var beneficiaryStatus: String? = null,

    @get:PropertyName("addedOn") @set:PropertyName("addedOn")
    var addedOn: String? = null
) {
    constructor() : this(null, null, BeneficiaryInstrumentDetails(), BeneficiaryContactDetails(), null, null)
}

// --------------------------
// MARK: - Instrument Details (Bank / UPI)
// --------------------------
@Keep
data class BeneficiaryInstrumentDetails(

    @get:PropertyName("bankAccountNumber") @set:PropertyName("bankAccountNumber")
    var bankAccountNumber: String? = null,

    @get:PropertyName("bankIfsc") @set:PropertyName("bankIfsc")
    var bankIfsc: String? = null,

    @get:PropertyName("vpa") @set:PropertyName("vpa")
    var vpa: String? = null
) {
    constructor() : this(null, null, null)
}

// --------------------------
// MARK: - Contact Details
// --------------------------
@Keep
data class BeneficiaryContactDetails(

    @get:PropertyName("beneficiaryEmail") @set:PropertyName("beneficiaryEmail")
    var beneficiaryEmail: String? = null,

    @get:PropertyName("beneficiaryPhone") @set:PropertyName("beneficiaryPhone")
    var beneficiaryPhone: String? = null,

    @get:PropertyName("beneficiaryCountryCode") @set:PropertyName("beneficiaryCountryCode")
    var beneficiaryCountryCode: String? = null,

    @get:PropertyName("beneficiaryAddress") @set:PropertyName("beneficiaryAddress")
    var beneficiaryAddress: String? = null,

    @get:PropertyName("beneficiaryCity") @set:PropertyName("beneficiaryCity")
    var beneficiaryCity: String? = null,

    @get:PropertyName("beneficiaryState") @set:PropertyName("beneficiaryState")
    var beneficiaryState: String? = null,

    @get:PropertyName("beneficiaryPostalCode") @set:PropertyName("beneficiaryPostalCode")
    var beneficiaryPostalCode: String? = null
) {
    constructor() : this(null, null, null, null, null, null, null)
}

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