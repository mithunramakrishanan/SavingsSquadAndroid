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

    @get:PropertyName("amountSentDate") @set:PropertyName("amountSentDate")
    var amountSentDate: Timestamp? = null,

    @get:PropertyName("loanStatus") @set:PropertyName("loanStatus")
    var loanStatus: EMIStatus = EMIStatus.FAILED,

    @get:PropertyName("loanClosedDate") @set:PropertyName("loanClosedDate")
    var loanClosedDate: Timestamp? = null,

    @get:PropertyName("installments") @set:PropertyName("installments")
    var installments: List<Installment> = emptyList(),

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
        loanNumber = "",
        loanAmount = 0,
        loanMonth = 0,
        interest = 0.0,
        amountSentDate = null,
        loanStatus = EMIStatus.FAILED,
        loanClosedDate = null,
        installments = emptyList(),
        recordStatus = RecordStatus.ACTIVE,
        recordDate = Timestamp.now()
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
data class EMIConfiguration(

    @get:PropertyName("id") @set:PropertyName("id")
    var id: String? = null,

    @get:PropertyName("loanAmount") @set:PropertyName("loanAmount")
    var loanAmount: Int = 0,

    @get:PropertyName("emiMonths") @set:PropertyName("emiMonths")
    var emiMonths: Int = 0,

    @get:PropertyName("emiInterestRate") @set:PropertyName("emiInterestRate")
    var emiInterestRate: Double = 0.0,

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
) {
    constructor() : this(
        id = null,
        loanAmount = 0,
        emiMonths = 0,
        emiInterestRate = 0.0,
        emiAmount = 0,
        interestAmount = 0,
        emiDate = null,
        emiCreatedDate = null,
        recordStatus = RecordStatus.ACTIVE,
        recordDate = Timestamp.now()
    )

    fun calculateEMIAndInterest(): Pair<Int, Int> {
        val monthlyRate = (emiInterestRate / 100) / 12
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

