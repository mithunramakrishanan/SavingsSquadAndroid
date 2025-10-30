package com.android.savingssquad.model

import com.android.savingssquad.singleton.GroupFundUserType
import com.android.savingssquad.singleton.RecordStatus
import com.google.firebase.Timestamp
import java.util.Date

data class UpiVerificationResult(
    val status: String,
    val data: UpiVerificationData?,
    val type: String?,
    val code: String?,
    val message: String?
)

data class UpiVerificationData(
    val upi_id: String?,
    val name_at_bank: String?,
    val verified: Boolean?
)

data class BeneficiaryResult(
    val beneId: String,
    val upi: String
)

data class Member(
    var id: String?,                     // Firestore Document ID
    var name: String,                    // Member's full name
    var profileImage: String,            // Profile image URL or path
    var mailID: String?,                 // Optional email ID
    var phoneNumber: String,             // Phone number (used for login)
    var password: String,                // Login password (store hashed in production)
    var groupFundID: String,             // Associated groupFund ID
    var role: GroupFundUserType,         // Member role
    var memberCreatedDate: Timestamp?,   // Creation date

    // Contribution & Loan Tracking
    var totalContributionPaid: Int = 0,
    var totalLoanBorrowed: Int = 0,
    var totalLoanPaid: Int = 0,
    var totalInterestPaid: Int = 0,

    // Metadata
    var recordStatus: RecordStatus,
    var recordDate: Date = Date(),

    // UPI / Bank Details
    var upiBeneId: String= "",
    var bankBeneId: String = "",
    var upiID: String = ""
)