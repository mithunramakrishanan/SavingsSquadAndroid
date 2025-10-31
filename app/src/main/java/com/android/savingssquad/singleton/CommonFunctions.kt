package com.yourapp.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.android.savingssquad.model.EMIConfiguration
import com.android.savingssquad.model.Installment
import com.android.savingssquad.model.MemberLoan
import com.android.savingssquad.singleton.EMIStatus
import com.google.firebase.Timestamp
import com.android.savingssquad.viewmodel.SquadViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import android.net.Network
import android.net.NetworkRequest
import android.util.Patterns
import com.android.savingssquad.model.GroupFundActivity
import com.android.savingssquad.model.Member
import com.android.savingssquad.singleton.CashfreeBeneficiaryType
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.singleton.asTimestamp
import kotlin.random.Random

object CommonFunctions {

    private var isConnected: Boolean = true

    // Call this once during app launch
    fun startMonitoringInternet(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = cm.activeNetwork
        val capabilities = cm.getNetworkCapabilities(network)
        isConnected = capabilities != null
    }

    fun isInternetAvailable(): Boolean {
        return isConnected
    }

    // MARK: - GroupFund Activity
    fun createGroupFundActivity(
        squadViewModel: SquadViewModel,
        groupFundActivity: GroupFundActivity,
        completion: (Boolean, String?) -> Unit
    ) {
        val login = UserDefaultsManager.getLogin()
        if (login?.groupFundID == null) {
            println("❌ No groupFund found!")
            completion(false, "GroupFund not found")
            return
        }

        squadViewModel.addGroupFundActivity(true, groupFundActivity) { success, error ->
            if (success) {
                println("✅ Activity added successfully!")
                completion(true, null)
            } else {
                println("❌ Failed to add activity: ${error ?: "Unknown error"}")
                completion(false, error)
            }
        }
    }

    // MARK: - Date Formatting
    fun dateToString(date: Date, format: String = "MMM dd yyyy hh:mm a"): String {
        val formatter = SimpleDateFormat(format, Locale.US)
        formatter.timeZone = TimeZone.getDefault()
        return formatter.format(date)
    }

    fun stringToDate(dateString: String, format: String = "MMM dd yyyy hh:mm a"): Date? {
        val formatter = SimpleDateFormat(format, Locale.US)
        formatter.timeZone = TimeZone.getDefault()
        return formatter.parse(dateString)
    }

    fun getFutureMonthYearDate(from: Date, monthsToAdd: Int): Date? {
        val cal = Calendar.getInstance()
        cal.time = from
        cal.add(Calendar.MONTH, monthsToAdd)
        return cal.time
    }

    fun getEndOfMonthFromDate(from: Date): Date? {
        val cal = Calendar.getInstance()
        cal.time = from
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        return cal.time
    }

    fun getContributionDue(monthYear: String): Date {
        val formatter = SimpleDateFormat("MMM yyyy", Locale.US)
        val date = formatter.parse(monthYear) ?: Date()
        val cal = Calendar.getInstance()
        cal.time = date
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.SECOND, -1)
        return cal.time
    }

    fun getRemainingMonths(startDate: Date, endDate: Date): Int {
        val cal = Calendar.getInstance()
        val start = cal.apply { time = startDate }
        val end = cal.apply { time = endDate }
        val yearDiff = end.get(Calendar.YEAR) - start.get(Calendar.YEAR)
        val monthDiff = end.get(Calendar.MONTH) - start.get(Calendar.MONTH)
        return yearDiff * 12 + monthDiff
    }

    fun convertMonthsToYearsAndMonths(months: Int): String {
        val years = months / 12
        val remaining = months % 12
        val parts = mutableListOf<String>()
        if (years > 0) parts.add("$years Year${if (years > 1) "s" else ""}")
        if (remaining > 0) parts.add("$remaining Month${if (remaining > 1) "s" else ""}")
        return parts.joinToString(" ")
    }

    // MARK: - Validators
    fun isValidPhoneNumber(phone: String): Boolean {
        return phone.matches(Regex("^[0-9]{10}$"))
    }

    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun validatePassword(password: String): String? {
        if (password.length < 6) return "Password must be at least 6 characters."
        if (!password.any { it.isUpperCase() }) return "Password must contain at least one uppercase letter."
        if (!password.any { it.isDigit() }) return "Password must contain at least one number."
        return null
    }

    fun validateConfirmPassword(password: String, confirmPassword: String): String? {
        return when {
            confirmPassword.isEmpty() -> "Confirm password cannot be empty."
            password != confirmPassword -> "Passwords do not match."
            else -> null
        }
    }

    fun generateAccountPrefix(groupFundID: String): String {
        var prefix = groupFundID.takeLast(4)
        val randomLetter = ('A'..'Z').random()
        prefix += randomLetter
        if (prefix.length > 6) prefix = prefix.take(6)
        return prefix.uppercase()
    }

    fun generatePayoutId(memberId: String): String {
        val formatter = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
        val timestamp = formatter.format(Date())
        return "PAYOUT-$memberId-$timestamp"
    }

    // MARK: - Generators
    fun generateLoanNumber(): String {
        val dateStr = SimpleDateFormat("MMddyyyyHHmmss", Locale.US).format(Date())
        return "LN$dateStr"
    }

    fun generateMemberID(): String {
        return "MB${Random.nextInt(1000, 9999)}"
    }

    fun generateBankAccountID(): String {
        return "BNK${Random.nextInt(1000, 9999)}"
    }

    fun generateRuleID(): String {
        return "RULE${Random.nextInt(100, 999)}"
    }

    fun generateInstallmentID(): String {
        return "INST${Random.nextInt(1000, 9999)}"
    }

    fun generateContributionID(memberId: String, monthYear: String): String {
        return "$memberId-$monthYear"
    }

    fun generatePaymentID(groupFundId: String): String {
        val timestamp = System.currentTimeMillis()
        return "pg_${groupFundId}_$timestamp"
    }

    // MARK: - Member Loan
    fun generateMemberLoan(
        emiConfig: EMIConfiguration,
        memberID: String,
        memberName: String
    ): MemberLoan {
        val cal = Calendar.getInstance()
        val today = Date()
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val loanNumber = generateLoanNumber()

        val interestSplits = splitAmountEvenly(emiConfig.interestAmount, emiConfig.emiMonths)
        val principalSplits = splitAmountEvenly(emiConfig.loanAmount, emiConfig.emiMonths)

        val installments = mutableListOf<Installment>()

        cal.time = today
        cal.add(Calendar.MONTH, 1)
        cal.set(Calendar.DAY_OF_MONTH, currentDay)
        val firstDueDate = cal.time

        for (i in 0 until emiConfig.emiMonths) {
            cal.time = firstDueDate
            cal.add(Calendar.MONTH, i)
            val dueDate = cal.time
            val suffix = ordinalSuffix(i + 1)
            installments.add(
                Installment(
                    id = generateInstallmentID(),
                    orderId = "",
                    memberID = memberID,
                    memberName = memberName,
                    installmentNumber = "$suffix Installment",
                    installmentAmount = principalSplits[i],
                    interestAmount = interestSplits[i],
                    dueDate = dueDate.asTimestamp,
                    duePaidDate = null,
                    status = EMIStatus.PENDING,
                    loanNumber = loanNumber
                )
            )
        }

        return MemberLoan(
            id = loanNumber,
            orderId = "",
            memberID = memberID,
            memberName = memberName,
            loanNumber = loanNumber, // ✅ fixed
            loanAmount = emiConfig.loanAmount,
            loanMonth = emiConfig.emiMonths,
            interest = emiConfig.emiInterestRate,
            amountSentDate = today.asTimestamp,
            loanStatus = EMIStatus.PENDING,
            loanClosedDate = null,
            installments = installments
        )
    }

    fun generateCashfreeBeneId(memberId: String, type: CashfreeBeneficiaryType): String {
        val cleanMemberId = memberId.lowercase(Locale.US).replace("[^a-z0-9]".toRegex(), "")
        return "bene$cleanMemberId"
    }

    fun splitAmountEvenly(amount: Int, months: Int): List<Int> {
        if (months <= 0) return emptyList()
        val base = amount / months
        val remainder = amount % months
        val list = MutableList(months) { base }
        for (i in 0 until remainder) list[i]++
        return list
    }

    fun getMemberByName(name: String, members: List<Member>): Member? {
        return members.firstOrNull { it.name == name }
    }

    fun getMember(by: String, from: List<Member>): Member? {
        return from.firstOrNull { it.name == by }
    }

    fun cleanUpName(name: String): String {
        return name.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.joinToString(" ")
    }

    fun cleanUpPhoneNumber(phone: String): String {
        return phone.filter { it.isDigit() }.takeLast(10)
    }

    fun ordinalSuffix(number: Int): String {
        val ones = number % 10
        val tens = (number / 10) % 10
        return if (tens == 1) {
            "${number}th"
        } else {
            when (ones) {
                1 -> "${number}st"
                2 -> "${number}nd"
                3 -> "${number}rd"
                else -> "${number}th"
            }
        }
    }

    fun replaceRootView(context: Context, target: Class<out Activity>) {
        val intent = Intent(context, target)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }
}