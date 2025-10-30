package com.android.savingssquad.model
import com.android.savingssquad.singleton.RecordStatus
import com.google.firebase.Timestamp
import java.util.Date

data class GroupFund(
    var id: String? = java.util.UUID.randomUUID().toString(),
    var groupFundID: String = "",
    var groupFundName: String = "",

    // Manager/creator details
    var mailID: String = "",
    var countryCode: String = "",
    var phoneNumber: String = "",

    // Virtual account details
    var virtualAccountNumber: String = "",
    var paymentInstrumentId: String = "",
    var virtualUPI: String = "",

    // Account details
    var groupFundAccountName: String = "",
    var groupFundAccountNumber: String = "",
    var groupFundIFSCCode: String = "",
    var upiBeneId: String = "",
    var bankBeneId: String = "",
    var upiID: String = "",

    // GroupFund lifecycle
    var groupFundStartDate: Timestamp? = null,
    var groupFundEndDate: Timestamp? = null,
    var groupFundCreatedDate: Timestamp? = null,
    var groupFundDueDate: Timestamp? = null,
    var totalDuration: Int = 0,
    var remainingDuration: Int = 0,

    // Members & contributions
    var totalMembers: Int = 0,
    var monthlyContribution: Int = 0,
    var groupFundStartAmount: Int = 0,
    var totalAmount: Int = 0,

    // Payment summaries
    var totalContributionAmountReceived: Int = 0,
    var totalLoanAmountReceived: Int = 0,
    var totalLoanAmountSent: Int = 0,
    var totalInterestAmountReceived: Int = 0,
    var currentAvailableAmount: Int = 0,

    // Configuration
    var emiConfiguration: List<EMIConfiguration> = emptyList(),
    var recordStatus: RecordStatus = RecordStatus.ACTIVE,
    var recordDate: Date = Date(),

    // Optional runtime-only field (not persisted in Firestore)
    var password: String? = null
)