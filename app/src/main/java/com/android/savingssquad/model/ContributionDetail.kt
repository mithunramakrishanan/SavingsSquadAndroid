package com.android.savingssquad.model

import android.os.Build
import com.android.savingssquad.singleton.PaidStatus
import com.android.savingssquad.singleton.PaymentEntryType
import com.android.savingssquad.singleton.RecordStatus
import com.google.firebase.Timestamp
import com.yourapp.utils.CommonFunctions
import java.text.SimpleDateFormat
import java.util.*

import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import com.google.firebase.firestore.PropertyName
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Date

@Keep
data class ContributionDetail(

    @get:PropertyName("id") @set:PropertyName("id")
    var id: String? = null, // Unique identifier

    @get:PropertyName("orderId") @set:PropertyName("orderId")
    var orderId: String = "",

    @get:PropertyName("memberID") @set:PropertyName("memberID")
    var memberID: String = "", // Firebase UID or internal member ID

    @get:PropertyName("memberName") @set:PropertyName("memberName")
    var memberName: String = "", // Snapshot of name at the time of payment

    @get:PropertyName("monthYear") @set:PropertyName("monthYear")
    var monthYear: String = "", // Format: "MMM yyyy", e.g., "Mar 2025"

    @get:PropertyName("amount") @set:PropertyName("amount")
    var amount: Int = 0, // Amount due or paid

    @get:PropertyName("paidOn") @set:PropertyName("paidOn")
    var paidOn: Timestamp? = null, // Timestamp of payment, if any

    @get:PropertyName("paidStatus") @set:PropertyName("paidStatus")
    var paidStatus: PaidStatus = PaidStatus.NOT_PAID, // Enum: PAID / NOT_PAID

    @get:PropertyName("paymentEntryType") @set:PropertyName("paymentEntryType")
    var paymentEntryType: PaymentEntryType = PaymentEntryType.AUTOMATIC_ENTRY, // Enum: CASH / ONLINE / UPI / etc.

    @get:PropertyName("recordStatus") @set:PropertyName("recordStatus")
    var recordStatus: RecordStatus = RecordStatus.ACTIVE,

    @get:PropertyName("recordDate") @set:PropertyName("recordDate")
    var recordDate: Date = Date(),

    @get:PropertyName("dueDate") @set:PropertyName("dueDate")
    var dueDate: Timestamp? = null // When this contribution was due
) {
    // 🔹 Required empty constructor for Firestore
    constructor() : this(
        id = null,
        orderId = "",
        memberID = "",
        memberName = "",
        monthYear = "",
        amount = 0,
        paidOn = null,
        paidStatus = PaidStatus.NOT_PAID,
        paymentEntryType = PaymentEntryType.AUTOMATIC_ENTRY,
        recordStatus = RecordStatus.ACTIVE,
        recordDate = Date(),
        dueDate = null
    )

    // 🔹 Custom initializer logic
    init {
        if (id == null && memberID.isNotEmpty() && monthYear.isNotEmpty()) {
            id = CommonFunctions.generateContributionID(memberID, monthYear)
        }
    }
}

fun List<ContributionDetail>.unpaidMonths(): List<String> {
    val formatter = SimpleDateFormat("MMM yyyy", Locale.ENGLISH)   // FIXED
    return this
        .filter { it.paidStatus == PaidStatus.NOT_PAID }
        .sortedBy { formatter.parse(it.monthYear) }
        .map { it.monthYear }
}

fun List<ContributionDetail>.unpaidContributions(): List<ContributionDetail> {
    val formatter = SimpleDateFormat("MMM yyyy", Locale.ENGLISH)   // FIXED
    return this
        .filter { it.paidStatus == PaidStatus.NOT_PAID }
        .sortedBy { formatter.parse(it.monthYear) }
}

@RequiresApi(Build.VERSION_CODES.O)
fun List<ContributionDetail>.currentAndOverdueUnpaid(): List<ContributionDetail> {
    val formatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)
    val current = YearMonth.now()

    return this
        .filter { it.paidStatus == PaidStatus.NOT_PAID }
        .filter {
            val month = YearMonth.parse(it.monthYear, formatter)
            month <= current
        }
        .sortedBy { YearMonth.parse(it.monthYear, formatter) }
}