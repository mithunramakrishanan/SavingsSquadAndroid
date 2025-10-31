package com.android.savingssquad.model
import androidx.annotation.Keep
import com.android.savingssquad.singleton.RecordStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import java.util.Date


@Keep
data class GroupFund(

    @get:PropertyName("id") @set:PropertyName("id")
    var id: String? = java.util.UUID.randomUUID().toString(),

    @get:PropertyName("groupFundID") @set:PropertyName("groupFundID")
    var groupFundID: String = "",

    @get:PropertyName("groupFundName") @set:PropertyName("groupFundName")
    var groupFundName: String = "",

    // 🔹 Manager / Creator Details
    @get:PropertyName("mailID") @set:PropertyName("mailID")
    var mailID: String = "",

    @get:PropertyName("countryCode") @set:PropertyName("countryCode")
    var countryCode: String = "",

    @get:PropertyName("phoneNumber") @set:PropertyName("phoneNumber")
    var phoneNumber: String = "",

    // 🔹 Virtual Account Details
    @get:PropertyName("virtualAccountNumber") @set:PropertyName("virtualAccountNumber")
    var virtualAccountNumber: String = "",

    @get:PropertyName("paymentInstrumentId") @set:PropertyName("paymentInstrumentId")
    var paymentInstrumentId: String = "",

    @get:PropertyName("virtualUPI") @set:PropertyName("virtualUPI")
    var virtualUPI: String = "",

    // 🔹 Account Details
    @get:PropertyName("groupFundAccountName") @set:PropertyName("groupFundAccountName")
    var groupFundAccountName: String = "",

    @get:PropertyName("groupFundAccountNumber") @set:PropertyName("groupFundAccountNumber")
    var groupFundAccountNumber: String = "",

    @get:PropertyName("groupFundIFSCCode") @set:PropertyName("groupFundIFSCCode")
    var groupFundIFSCCode: String = "",

    @get:PropertyName("upiBeneId") @set:PropertyName("upiBeneId")
    var upiBeneId: String = "",

    @get:PropertyName("bankBeneId") @set:PropertyName("bankBeneId")
    var bankBeneId: String = "",

    @get:PropertyName("upiID") @set:PropertyName("upiID")
    var upiID: String = "",

    // 🔹 GroupFund Lifecycle
    @get:PropertyName("groupFundStartDate") @set:PropertyName("groupFundStartDate")
    var groupFundStartDate: Timestamp? = null,

    @get:PropertyName("groupFundEndDate") @set:PropertyName("groupFundEndDate")
    var groupFundEndDate: Timestamp? = null,

    @get:PropertyName("groupFundCreatedDate") @set:PropertyName("groupFundCreatedDate")
    var groupFundCreatedDate: Timestamp? = null,

    @get:PropertyName("groupFundDueDate") @set:PropertyName("groupFundDueDate")
    var groupFundDueDate: Timestamp? = null,

    @get:PropertyName("totalDuration") @set:PropertyName("totalDuration")
    var totalDuration: Int = 0,

    @get:PropertyName("remainingDuration") @set:PropertyName("remainingDuration")
    var remainingDuration: Int = 0,

    // 🔹 Members & Contributions
    @get:PropertyName("totalMembers") @set:PropertyName("totalMembers")
    var totalMembers: Int = 0,

    @get:PropertyName("monthlyContribution") @set:PropertyName("monthlyContribution")
    var monthlyContribution: Int = 0,

    @get:PropertyName("groupFundStartAmount") @set:PropertyName("groupFundStartAmount")
    var groupFundStartAmount: Int = 0,

    @get:PropertyName("totalAmount") @set:PropertyName("totalAmount")
    var totalAmount: Int = 0,

    // 🔹 Payment Summaries
    @get:PropertyName("totalContributionAmountReceived") @set:PropertyName("totalContributionAmountReceived")
    var totalContributionAmountReceived: Int = 0,

    @get:PropertyName("totalLoanAmountReceived") @set:PropertyName("totalLoanAmountReceived")
    var totalLoanAmountReceived: Int = 0,

    @get:PropertyName("totalLoanAmountSent") @set:PropertyName("totalLoanAmountSent")
    var totalLoanAmountSent: Int = 0,

    @get:PropertyName("totalInterestAmountReceived") @set:PropertyName("totalInterestAmountReceived")
    var totalInterestAmountReceived: Int = 0,

    @get:PropertyName("currentAvailableAmount") @set:PropertyName("currentAvailableAmount")
    var currentAvailableAmount: Int = 0,

    // 🔹 Configurations
    @get:PropertyName("emiConfiguration") @set:PropertyName("emiConfiguration")
    var emiConfiguration: List<EMIConfiguration> = emptyList(),

    @get:PropertyName("recordStatus") @set:PropertyName("recordStatus")
    var recordStatus: RecordStatus = RecordStatus.ACTIVE,

    @get:PropertyName("recordDate") @set:PropertyName("recordDate")
    var recordDate: Date = Date(),

    // 🔹 Optional runtime-only field (not persisted)
    @get:PropertyName("password") @set:PropertyName("password")
    var password: String? = null
) {
    // Required empty constructor for Firestore deserialization
    constructor() : this(
        id = java.util.UUID.randomUUID().toString(),
        groupFundID = "",
        groupFundName = "",
        mailID = "",
        countryCode = "",
        phoneNumber = "",
        virtualAccountNumber = "",
        paymentInstrumentId = "",
        virtualUPI = "",
        groupFundAccountName = "",
        groupFundAccountNumber = "",
        groupFundIFSCCode = "",
        upiBeneId = "",
        bankBeneId = "",
        upiID = "",
        groupFundStartDate = null,
        groupFundEndDate = null,
        groupFundCreatedDate = null,
        groupFundDueDate = null,
        totalDuration = 0,
        remainingDuration = 0,
        totalMembers = 0,
        monthlyContribution = 0,
        groupFundStartAmount = 0,
        totalAmount = 0,
        totalContributionAmountReceived = 0,
        totalLoanAmountReceived = 0,
        totalLoanAmountSent = 0,
        totalInterestAmountReceived = 0,
        currentAvailableAmount = 0,
        emiConfiguration = emptyList(),
        recordStatus = RecordStatus.ACTIVE,
        recordDate = Date(),
        password = null
    )
}