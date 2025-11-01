package com.android.savingssquad.model
import androidx.compose.ui.graphics.Color


import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName
import com.google.gson.annotations.SerializedName

// --------------------------
// MARK: - Payout Status Result
// --------------------------
@Keep
data class PayoutStatusResult(

    @get:PropertyName("status") @set:PropertyName("status")
    var status: String? = null,

    @get:PropertyName("updated_on") @set:PropertyName("updated_on")
    var updatedOn: String? = null,

    @get:PropertyName("transfer_id") @set:PropertyName("transfer_id")
    var transferId: String? = null,

    @get:PropertyName("status_description") @set:PropertyName("status_description")
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

@Keep
enum class PayoutStatus(@get:PropertyName("value") val value: String) {

    @PropertyName("PENDING")
    PENDING("PENDING"),

    @PropertyName("RECEIVED")
    INITIATED("RECEIVED"),

    @PropertyName("PAYOUT_INPROGRESS")
    IN_PROGRESS("PAYOUT_INPROGRESS"),

    @PropertyName("PAYOUT_SUCCESS")
    SUCCESS("PAYOUT_SUCCESS"),

    @PropertyName("PAYOUT_FAILED")
    FAILED("PAYOUT_FAILED"),

    @PropertyName("PAYOUT_CANCELLED")
    CANCELLED("PAYOUT_CANCELLED"),

    @PropertyName("PAYOUT_REVERSED")
    REVERSED("PAYOUT_REVERSED");

    val displayText: String
        get() = when (this) {
            PENDING -> "Waiting for Payout"
            INITIATED -> "Payout Initiated"
            IN_PROGRESS -> "Payout In Progress"
            SUCCESS -> "Payout Successful"
            FAILED -> "Payout Failed"
            CANCELLED -> "Payout Cancelled"
            REVERSED -> "Payout Reversed"
        }

    companion object {
        fun fromValue(value: String?): PayoutStatus {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: PENDING
        }
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
                PayoutStatus.SUCCESS -> "check_circle"
                PayoutStatus.FAILED -> "cancel"
                PayoutStatus.CANCELLED -> "block"
                PayoutStatus.INITIATED, PayoutStatus.IN_PROGRESS, PayoutStatus.PENDING -> "schedule"
                PayoutStatus.REVERSED -> "autorenew"
            }

        override val color: Color
            get() = when (this@toStatusUI) {
                PayoutStatus.SUCCESS -> Color(0xFF4CAF50) // Green
                PayoutStatus.FAILED, PayoutStatus.CANCELLED -> Color(0xFFF44336) // Red
                PayoutStatus.INITIATED, PayoutStatus.IN_PROGRESS, PayoutStatus.PENDING -> Color(0xFFFF9800) // Orange
                PayoutStatus.REVERSED -> Color(0xFF9C27B0) // Purple
            }

        override val backgroundColor: Color
            get() = color.copy(alpha = 0.05f)
    }
}