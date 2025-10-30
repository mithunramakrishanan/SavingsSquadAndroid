package com.android.savingssquad.model

import com.android.savingssquad.singleton.PaidStatus
import com.android.savingssquad.singleton.PaymentEntryType
import com.android.savingssquad.singleton.RecordStatus
import com.google.firebase.Timestamp
import com.yourapp.utils.CommonFunctions
import java.text.SimpleDateFormat
import java.util.*

data class ContributionDetail(
    var id: String? = null,                     // Unique identifier
    var orderId: String,
    var memberID: String,                       // Firebase UID or internal member ID
    var memberName: String,                     // Snapshot of name at the time of payment
    var monthYear: String,                      // Format: "MMM yyyy", e.g., "Mar 2025"
    var amount: Int,                            // Amount due or paid
    var paidOn: Timestamp? = null,              // Timestamp of payment, if any
    var paidStatus: PaidStatus,                 // Enum: PAID / NOT_PAID
    var paymentEntryType: PaymentEntryType,     // Enum: CASH / ONLINE / UPI / etc.
    var recordStatus: RecordStatus = RecordStatus.ACTIVE,
    var recordDate: Date = Date(),
    var dueDate: Timestamp? = null              // When this contribution was due
) {
    init {
        // Auto-generate ID if missing
        if (id == null) {
            id = CommonFunctions.generateContributionID(memberID, monthYear)
        }
    }
}

fun List<ContributionDetail>.unpaidMonths(): List<String> {
    val formatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    return this
        .filter { it.paidStatus == PaidStatus.NOT_PAID }
        .sortedBy { formatter.parse(it.monthYear) }
        .map { it.monthYear }
}

fun List<ContributionDetail>.unpaidContributions(): List<ContributionDetail> {
    val formatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    return this
        .filter { it.paidStatus == PaidStatus.NOT_PAID }
        .sortedBy { formatter.parse(it.monthYear) }
}

fun List<ContributionDetail>.currentAndOverdueUnpaid(): List<ContributionDetail> {
    val formatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    val current = Date()
    return this
        .filter { it.paidStatus == PaidStatus.NOT_PAID }
        .filter {
            val monthDate = formatter.parse(it.monthYear)
            monthDate != null && monthDate <= current
        }
        .sortedBy { formatter.parse(it.monthYear) }
}