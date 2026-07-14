package com.android.savingssquad.model

import androidx.annotation.Keep
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.RecordStatus
import com.android.savingssquad.singleton.orNow

import kotlinx.serialization.Serializable
import java.util.Date
import java.util.concurrent.TimeUnit

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import kotlin.math.pow
import kotlin.math.roundToInt

//import com.google.firebase.firestore.FirebaseFirestore


@Keep
data class MemberLoan(

    @get:PropertyName("id") @set:PropertyName("id")
    var id: String? = null,

    @get:PropertyName("orderId") @set:PropertyName("orderId")
    var orderId: String = "",

    @get:PropertyName("memberID") @set:PropertyName("memberID")
    var memberID: String = "",

    @get:PropertyName("memberName") @set:PropertyName("memberName")
    var memberName: String = "",

    @get:PropertyName("loanNumber") @set:PropertyName("loanNumber")
    var loanNumber: String = "",

    @get:PropertyName("loanAmount") @set:PropertyName("loanAmount")
    var loanAmount: Int = 0,

    @get:PropertyName("loanMonth") @set:PropertyName("loanMonth")
    var loanMonth: Int = 0,

    @get:PropertyName("interest") @set:PropertyName("interest")
    var interest: Double = 0.0,

    @get:PropertyName("forceClosedAmount") @set:PropertyName("forceClosedAmount")
    var forceClosedAmount: Int = 0,

    @get:PropertyName("forceClosedDate") @set:PropertyName("forceClosedDate")
    var forceClosedDate: Timestamp? = null,

    @get:PropertyName("amountSentDate") @set:PropertyName("amountSentDate")
    var amountSentDate: Timestamp? = null,

    @get:PropertyName("loanStatus") @set:PropertyName("loanStatus")
    var loanStatus: EMIStatus = EMIStatus.FAILED,

    @get:PropertyName("loanClosedDate") @set:PropertyName("loanClosedDate")
    var loanClosedDate: Timestamp? = null,

    @get:PropertyName("installments") @set:PropertyName("installments")
    var installments: List<Installment> = emptyList(),

    @get:PropertyName("emiConfiguration") @set:PropertyName("emiConfiguration")
    var emiConfiguration: EMIConfiguration? = null,

    @get:PropertyName("recordStatus") @set:PropertyName("recordStatus")
    var recordStatus: RecordStatus = RecordStatus.ACTIVE,

    @get:PropertyName("recordDate") @set:PropertyName("recordDate")
    var recordDate: Timestamp? = Timestamp.now(),

    @get:PropertyName("paidType") @set:PropertyName("paidType")
    var paidType: LoanPaidType = LoanPaidType.REGULAR,

    @get:PropertyName("isForceClosed") @set:PropertyName("isForceClosed")
    var isForceClosed: Boolean = false
) {
    constructor() : this(
        id = null,
        orderId = "",
        memberID = "",
        memberName = "",
        loanNumber = "",
        loanAmount = 0,
        loanMonth = 0,
        interest = 0.0,
        forceClosedAmount = 0,
        forceClosedDate = null,
        amountSentDate = null,
        loanStatus = EMIStatus.FAILED,
        loanClosedDate = null,
        installments = emptyList(),
        emiConfiguration = null,
        recordStatus = RecordStatus.ACTIVE,
        recordDate = Timestamp.now(),
        paidType = LoanPaidType.REGULAR,
        isForceClosed = false
    )
}

data class ForceCloseSummary(
    var outstandingPrincipal: Int = 0,
    var daysElapsed: Int = 0,
    var recalculatedInterest: Int = 0,
    var totalPayable: Int = 0,
    var asOfDate: Date = Date()
)

fun List<Installment>.pending(): List<Installment> = filter { it.status == EMIStatus.PENDING }
fun List<Installment>.paid(): List<Installment> = filter { it.status == EMIStatus.PAID }

fun MemberLoan.forceCloseSummary(
    interestType: InterestType,
    rate: Double,
    asOf: Date = Date()
): ForceCloseSummary {
    val pendingInstallments = installments.pending()
    val outstandingPrincipal = pendingInstallments.sumOf { it.installmentAmount }

    // Interest accrues from the day after the last paid installment (or loan start) to today.
    // 🔹 duePaidDate / amountSentDate are Timestamp?, convert to Date before time math
    val lastPaidDate: Date? = installments.paid()
        .mapNotNull { it.duePaidDate?.toDate() }
        .maxOrNull()

    val startDate: Date = lastPaidDate ?: amountSentDate?.toDate() ?: asOf

    val days = TimeUnit.MILLISECONDS.toDays(asOf.time - startDate.time).coerceAtLeast(0)
    val dailyRate = interestType.dailyRate(rate)
    val recalculatedInterest = outstandingPrincipal * dailyRate * days
    val total = outstandingPrincipal + recalculatedInterest.roundToInt()

    return ForceCloseSummary(
        outstandingPrincipal = outstandingPrincipal,
        daysElapsed = days.toInt(),
        recalculatedInterest = recalculatedInterest.roundToInt(),
        totalPayable = total,
        asOfDate = asOf
    )
}

@Keep
data class Installment(

    @get:PropertyName("id") @set:PropertyName("id")
    var id: String? = null,

    @get:PropertyName("orderId") @set:PropertyName("orderId")
    var orderId: String = "",

    @get:PropertyName("memberID") @set:PropertyName("memberID")
    var memberID: String = "",

    @get:PropertyName("memberName") @set:PropertyName("memberName")
    var memberName: String = "",

    @get:PropertyName("installmentNumber") @set:PropertyName("installmentNumber")
    var installmentNumber: String = "",

    @get:PropertyName("installmentAmount") @set:PropertyName("installmentAmount")
    var installmentAmount: Int = 0,

    @get:PropertyName("interestAmount") @set:PropertyName("interestAmount")
    var interestAmount: Int = 0,

    @get:PropertyName("dueDate") @set:PropertyName("dueDate")
    var dueDate: Timestamp? = null,

    @get:PropertyName("duePaidDate") @set:PropertyName("duePaidDate")
    var duePaidDate: Timestamp? = null,

    @get:PropertyName("status") @set:PropertyName("status")
    var status: EMIStatus = EMIStatus.PENDING,

    @get:PropertyName("loanNumber") @set:PropertyName("loanNumber")
    var loanNumber: String = "",

    @get:PropertyName("recordStatus") @set:PropertyName("recordStatus")
    var recordStatus: RecordStatus = RecordStatus.ACTIVE,

    @get:PropertyName("recordDate") @set:PropertyName("recordDate")
    var recordDate: Timestamp? = Timestamp.now()
) {
    constructor() : this(
        id = null,
        orderId = "",
        memberID = "",
        memberName = "",
        installmentNumber = "",
        installmentAmount = 0,
        interestAmount = 0,
        dueDate = null,
        duePaidDate = null,
        status = EMIStatus.PENDING,
        loanNumber = "",
        recordStatus = RecordStatus.ACTIVE,
        recordDate = Timestamp.now()
    )

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "orderId" to orderId,
        "memberID" to memberID,
        "memberName" to memberName,
        "installmentNumber" to installmentNumber,
        "installmentAmount" to installmentAmount,
        "interestAmount" to interestAmount,
        "dueDate" to dueDate,
        "duePaidDate" to duePaidDate,
        "status" to status.name,
        "loanNumber" to loanNumber,
        "recordStatus" to recordStatus.name,
        "recordDate" to recordDate
    )
}

@Keep
enum class InterestType(val label: String) {
    @PropertyName("DAILY") DAILY("DAILY"),
    @PropertyName("MONTHLY") MONTHLY("MONTHLY"),
    @PropertyName("YEARLY") YEARLY("YEARLY");

    /** Converts the entered rate into an effective monthly rate for EMI math. */
    fun monthlyRate(rate: Double): Double = when (this) {
        YEARLY -> (rate / 100) / 12
        MONTHLY -> rate / 100
        DAILY -> (rate / 100) * 30 // simple approximation: daily rate * 30 days
    }

    fun dailyRate(rate: Double): Double = when (this) {
        DAILY -> rate / 100
        MONTHLY -> (rate / 100) / 30
        YEARLY -> (rate / 100) / 365
    }

    fun displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }

    companion object {
        fun fromLabelOrDefault(value: String?): InterestType =
            entries.firstOrNull { it.name == value } ?: YEARLY
    }
}

enum class LoanPaidType {
    REGULAR, FORCECLOSED
}

@Keep
data class EMIConfiguration(

    @get:PropertyName("id") @set:PropertyName("id")
    var id: String? = null,

    @get:PropertyName("loanAmount") @set:PropertyName("loanAmount")
    var loanAmount: Int = 0,

    @get:PropertyName("emiMonths") @set:PropertyName("emiMonths")
    var emiMonths: Int = 0,

    @get:PropertyName("emiInterestRate") @set:PropertyName("emiInterestRate")
    var emiInterestRate: Double = 0.0,

    @get:PropertyName("interestType") @set:PropertyName("interestType")
    var interestType: InterestType = InterestType.YEARLY, // 🔹 NEW

    @get:PropertyName("emiAmount") @set:PropertyName("emiAmount")
    var emiAmount: Int = 0,

    @get:PropertyName("interestAmount") @set:PropertyName("interestAmount")
    var interestAmount: Int = 0,

    @get:PropertyName("emiDate") @set:PropertyName("emiDate")
    var emiDate: Timestamp? = null,

    @get:PropertyName("emiCreatedDate") @set:PropertyName("emiCreatedDate")
    var emiCreatedDate: Timestamp? = null,

    @get:PropertyName("recordStatus") @set:PropertyName("recordStatus")
    var recordStatus: RecordStatus = RecordStatus.ACTIVE,

    @get:PropertyName("recordDate") @set:PropertyName("recordDate")
    var recordDate: Timestamp? = Timestamp.now()
)
{
    constructor() : this(
        id = null,
        loanAmount = 0,
        emiMonths = 0,
        emiInterestRate = 0.0,
        interestType = InterestType.YEARLY,
        emiAmount = 0,
        interestAmount = 0,
        emiDate = null,
        emiCreatedDate = null,
        recordStatus = RecordStatus.ACTIVE,
        recordDate = Timestamp.now()
    )

    fun calculateEMIAndInterest(): Pair<Int, Int> {
        val monthlyRate = interestType.monthlyRate(emiInterestRate)

        if (monthlyRate == 0.0) {
            val simpleEMI = loanAmount.toDouble() / emiMonths
            return Pair(simpleEMI.toInt(), 0)
        }

        val numerator = loanAmount * monthlyRate * (1 + monthlyRate).pow(emiMonths.toDouble())
        val denominator = (1 + monthlyRate).pow(emiMonths.toDouble()) - 1

        if (denominator == 0.0) {
            val simpleEMI = loanAmount.toDouble() / emiMonths
            return Pair(simpleEMI.toInt(), 0)
        }

        val emi = numerator / denominator
        val totalPayment = emi * emiMonths
        val totalInterest = totalPayment - loanAmount
        return Pair(emi.toInt(), totalInterest.toInt())
    }
}


// Utility: Get start of today (00:00:00)
fun todayStart(): Date {
    val now = Date()
    val days = TimeUnit.MILLISECONDS.toDays(now.time)
    val startMillis = TimeUnit.DAYS.toMillis(days)
    return Date(startMillis)
}

// --- Installment Extensions ---

fun List<Installment>.pendingInstallments(): List<Installment> =
    filter { it.status == EMIStatus.PENDING }

fun List<Installment>.paidInstallments(): List<Installment> =
    filter { it.status == EMIStatus.PAID }

fun List<Installment>.currentAndOverdueUnpaid(): List<Installment> {
    val today = todayStart()
    return filter {
        it.status == EMIStatus.PENDING && (it.dueDate.orNow ?: Date()) <= today
    }
}

fun List<Installment>.upcomingUnpaid(): List<Installment> {
    val today = todayStart()
    return filter {
        it.status == EMIStatus.PENDING && (it.dueDate.orNow ?: Date()) > today
    }
}

fun List<Installment>.totalOutstandingAmount(): Int =
    sumOf {
        if (it.status == EMIStatus.PENDING)
            it.installmentAmount + it.interestAmount
        else 0
    }

fun List<Installment>.totalPaidAmount(): Int =
    sumOf {
        if (it.status == EMIStatus.PAID)
            it.installmentAmount + it.interestAmount
        else 0
    }

fun List<Installment>.nextDueInstallment(): Installment? =
    currentAndOverdueUnpaid()
        .sortedBy { it.dueDate.orNow ?: Date() }
        .firstOrNull()

fun List<Installment>.sortedByDueDate(): List<Installment> =
    sortedBy { it.dueDate.orNow ?: Date() }


// --- MemberLoan Extensions ---

fun List<MemberLoan>.pendingLoans(): List<MemberLoan> =
    filter { it.loanStatus == EMIStatus.PENDING }

fun List<MemberLoan>.paidLoans(): List<MemberLoan> =
    filter { it.loanStatus == EMIStatus.PAID }

fun List<MemberLoan>.loansWithUnpaidInstallments(): List<MemberLoan> =
    filter { loan ->
        loan.installments.any { it.status == EMIStatus.PENDING }
    }

