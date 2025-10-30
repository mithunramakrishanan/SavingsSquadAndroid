package com.android.savingssquad.model

import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.RecordStatus
import com.android.savingssquad.singleton.orNow

import kotlinx.serialization.Serializable
import java.util.Date
import java.util.concurrent.TimeUnit

import com.google.firebase.Timestamp
//import com.google.firebase.firestore.FirebaseFirestore

data class MemberLoan(
    var id: String? = null,
    var orderId: String = "",
    var memberID: String = "",
    var memberName: String = "",
    var loadNumber: String = "",
    var loanAmount: Int = 0,
    var loanMonth: Int = 0,
    var interest: Double = 0.0,
    var amountSentDate: Timestamp? = null,
    var loanStatus: EMIStatus = EMIStatus.FAILED,
    var loanClosedDate: Timestamp? = null,
    var installments: List<Installment> = emptyList(),
    var recordStatus: RecordStatus = RecordStatus.ACTIVE,
    var recordDate: Timestamp? = Timestamp.now()
)

// ✅ Installment Model
data class Installment(
    var id: String? = null,
    var orderId: String = "",
    var memberID: String = "",
    var memberName: String = "",
    var installmentNumber: String = "",
    var installmentAmount: Int = 0,
    var interestAmount: Int = 0,
    var dueDate: com.google.firebase.Timestamp? = null,
    var duePaidDate: com.google.firebase.Timestamp? = null,
    var status: EMIStatus = EMIStatus.PENDING,
    var loanNumber: String = "",
    var recordStatus: RecordStatus = RecordStatus.ACTIVE,
    var recordDate: com.google.firebase.Timestamp? = com.google.firebase.Timestamp.now()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
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
}


// ✅ EMI Configuration Model
data class EMIConfiguration(
    var id: String? = null,
    var loadAmount: Int = 0,
    var emiMonths: Int = 0,
    var emiInterestRate: Double = 0.0,
    var emiAmount: Int = 0,
    var interestAmount: Int = 0,
    var emiDate: Timestamp? = null,
    var emiCreatedDate: Timestamp? = null,
    var recordStatus: RecordStatus = RecordStatus.ACTIVE,
    var recordDate: Timestamp? = Timestamp.now()
) {
    fun calculateEMIAndInterest(): Pair<Int, Int> {
        val monthlyRate = (emiInterestRate / 100) / 12
        val numerator = loadAmount * monthlyRate * Math.pow(1 + monthlyRate, emiMonths.toDouble())
        val denominator = Math.pow(1 + monthlyRate, emiMonths.toDouble()) - 1

        if (denominator == 0.0) {
            val simpleEMI = loadAmount.toDouble() / emiMonths
            return Pair(simpleEMI.toInt(), 0)
        }

        val emi = numerator / denominator
        val totalPayment = emi * emiMonths
        val totalInterest = totalPayment - loadAmount
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

