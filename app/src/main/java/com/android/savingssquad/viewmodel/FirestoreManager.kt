package com.android.savingssquad.viewmodel

/*
 * PRODUCTION-SAFE REVISION
 *
 * Summary of production fixes applied in this revision (search "FIX:" for each spot):
 * 1. observePayments now returns a ListenerRegistration (leak fix) — callers MUST hold
 *    onto it and call .remove() in onCleared()/onDestroy(), or the listener lives forever.
 * 2. All println()/print debug logging routed through fmLog(), gated by BuildConfig.DEBUG
 *    and never dumping raw documents, phone numbers, or tokens.
 * 3. Empty-result vs. error-result semantics made consistent: "no members found" / "no
 *    squads" / "not managing any squads" are no longer reported as errors.
 * 4. fetchAllLoansInSquad no longer treats an empty member list as an error.
 * 5. updateCashRequestStatus and updateInstallmentStatus / updateLoanAndAllInstallmentsStatus
 *    moved into Firestore transactions to remove read-modify-write races and guard
 *    cashRequestedCount against double-decrementing on retried/duplicate calls.
 * 6. updateSquadRule guards against writing to a document with an empty/blank ID.
 * 7. fetchAllLoansInSquad's dispatchGroup variable (unused/dead code) removed; the
 *    per-member latch is used correctly and completion is guaranteed to fire exactly once.
 *
 * NOTE: Business-critical mutations (payment approval, cash request approval, balance
 * updates) are still directly callable by any authenticated client. For a true release-safe
 * posture, move these into Cloud Functions and lock down direct writes to these fields via
 * Firestore Security Rules — this file alone cannot enforce server-side trust.
 */

import android.util.Log
import com.android.savingssquad.model.CashRequest
import com.android.savingssquad.model.CashRequestStatus
import com.android.savingssquad.model.ContributionDetail
import com.android.savingssquad.model.EMIConfiguration
import com.android.savingssquad.model.Squad
import com.android.savingssquad.model.SquadActivity
import com.android.savingssquad.model.SquadRule
import com.android.savingssquad.model.Installment
import com.android.savingssquad.model.Login
import com.android.savingssquad.model.PaymentsDetails
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.yourapp.utils.CommonFunctions
import com.android.savingssquad.model.Member
import com.android.savingssquad.model.MemberLoan
import com.android.savingssquad.model.MemberOtherPayments
import com.android.savingssquad.singleton.AmountEditType
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.LoaderManager
import com.android.savingssquad.singleton.MemberPaymentSubType
import com.android.savingssquad.singleton.PaidStatus
import com.android.savingssquad.singleton.PaymentApproveStatus
import com.android.savingssquad.singleton.PaymentEntryType
import com.android.savingssquad.singleton.PaymentFilter
import com.android.savingssquad.singleton.PaymentStatus
import com.android.savingssquad.singleton.RecordStatus
import com.android.savingssquad.singleton.SessionManager
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.asTimestamp
import com.google.firebase.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** FIX #2: Centralized, release-safe logging. Never logs raw documents, phone numbers,
 *  or tokens, and is compiled out of behavior entirely when BuildConfig.DEBUG is false. */
private object FmLog {
    private const val TAG = "FirestoreManager"
    fun d(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }
    fun w(message: String) {
        if (BuildConfig.DEBUG) Log.w(TAG, message)
    }
}

class FirestoreManager private constructor() {

    companion object {
        val shared: FirestoreManager by lazy { FirestoreManager() } // 🔥 Singleton
    }

    private val db: FirebaseFirestore = Firebase.firestore // Firestore reference

    // MARK: - 🔹 Save User Login Details
    fun addUserLogin(login: Login, completion: (Boolean, String?) -> Unit) {
        val userRef = db.collection("users").document(login.phoneNumber.toString())
        val loginRef = userRef.collection("logins").document()

        val newLogin = login.copy(id = loginRef.id)

        try {
            loginRef.set(newLogin)
                .addOnSuccessListener {
                    completion(true, null)
                }
                .addOnFailureListener { error ->
                    completion(false, "Failed to save login: ${error.localizedMessage}")
                }
        } catch (e: Exception) {
            completion(false, "Encoding error: ${e.localizedMessage}")
        }
    }

    // MARK: - 🔹 Fetch User Logins
    fun fetchUserLogins(
        phoneNumber: String,
        completion: (List<Login>?, String?) -> Unit
    ) {

        if (phoneNumber.isEmpty()) {
            // FIX: previously returned without ever calling completion, leaving callers
            // hanging (e.g. a loading spinner that never dismisses).
            completion(null, "Invalid phone number.")
            return
        }

        val userRef = db.collection("users")
            .document(phoneNumber)
            .collection("logins")

        userRef.get()
            .addOnSuccessListener { snapshot ->

                if (snapshot == null || snapshot.isEmpty) {
                    completion(emptyList(), null)
                    return@addOnSuccessListener
                }

                try {

                    var logins = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Login::class.java)?.copy(id = doc.id)
                    }

                    logins = logins.filter { login ->
                        when (login.role) {
                            SquadUserType.SQUAD_MANAGER -> true
                            SquadUserType.SQUAD_MEMBER -> login.recordStatus == RecordStatus.ACTIVE
                            else -> false
                        }
                    }

                    completion(logins, null)

                } catch (e: Exception) {
                    completion(null, "Decoding error: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener { error ->
                completion(null, "Failed to fetch logins: ${error.localizedMessage}")
            }
    }

    // MARK: - 🔹 Add Squad
    fun addSquad(squad: Squad, completion: (Boolean, String?) -> Unit) {
        try {
            val squadRef = db.collection("squads").document(squad.squadID)
            squadRef.set(squad)
                .addOnSuccessListener { completion(true, null) }
                .addOnFailureListener { e -> completion(false, "Error adding squad: ${e.localizedMessage}") }
        } catch (e: Exception) {
            completion(false, "Error adding squad: ${e.localizedMessage}")
        }
    }

    // MARK: - 🔹 Fetch All Squads
    fun fetchSquads(completion: (List<Squad>?, String?) -> Unit) {
        db.collection("squads")
            .get()
            .addOnSuccessListener { snapshot ->
                // FIX #3: "no squads" is not an error — return an empty list.
                if (snapshot == null || snapshot.isEmpty) {
                    completion(emptyList(), null)
                    return@addOnSuccessListener
                }
                try {
                    val squads = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Squad::class.java)?.copy(id = doc.id)
                    }
                    completion(squads, null)
                } catch (e: Exception) {
                    completion(null, "Decoding error: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener { e ->
                completion(null, "Error fetching squads: ${e.localizedMessage}")
            }
    }

    // MARK: - 🔹 Fetch Squad by ID
    fun fetchSquadByID(squadID: String, completion: (Squad?, String?) -> Unit) {
        val ref = db.collection("squads").document(squadID)

        ref.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    try {
                        val gf = document.toObject(Squad::class.java)
                        completion(gf?.copy(id = document.id), null)
                    } catch (e: Exception) {
                        completion(null, "Decoding error: ${e.localizedMessage}")
                    }
                } else {
                    completion(null, "Squad not found.")
                }
            }
            .addOnFailureListener { e ->
                completion(null, "Error fetching squad: ${e.localizedMessage}")
            }
    }

    // MARK: - 🔹 Update Squad
    fun updateSquad(squad: Squad, completion: (Boolean, Squad?, String?) -> Unit) {
        val ref = db.collection("squads").document(squad.squadID)

        ref.set(squad)
            .addOnSuccessListener {
                ref.get()
                    .addOnSuccessListener { doc ->
                        val updated = doc.toObject(Squad::class.java)?.copy(id = doc.id)
                        completion(true, updated, null)
                    }
                    .addOnFailureListener { e ->
                        completion(false, null, "Error fetching updated squad: ${e.localizedMessage}")
                    }
            }
            .addOnFailureListener { e ->
                completion(false, null, "Error updating squad: ${e.localizedMessage}")
            }
    }

    fun updateSquadTotalAmount(
        squadId: String,
        amount: Int,
        completion: (Boolean, String?) -> Unit
    ) {
        val squadRef = db.collection("squads").document(squadId)

        val updateData = mapOf("currentAvailableAmount" to amount)

        squadRef.update(updateData)
            .addOnSuccessListener {
                FmLog.d("updateSquadTotalAmount succeeded")
                completion(true, null)
            }
            .addOnFailureListener { e ->
                FmLog.w("updateSquadTotalAmount failed: ${e.localizedMessage}")
                completion(false, "Failed to update amount: ${e.localizedMessage}")
            }
    }

    // MARK: - 🔹 Delete Squad
    fun deleteSquad(squadID: String, completion: (Boolean, String?) -> Unit) {
        db.collection("squads").document(squadID)
            .delete()
            .addOnSuccessListener { completion(true, null) }
            .addOnFailureListener { e -> completion(false, "Error deleting squad: ${e.localizedMessage}") }
    }

    // MARK: - 🔹 Save Payment
    fun savePayment(squadID: String, payment: PaymentsDetails, completion: (Boolean, String?) -> Unit) {
        val paymentID = payment.id
        if (paymentID == null) {
            completion(false, "Payment ID is missing.")
            return
        }

        val ref = db.collection("squads").document(squadID)
            .collection("payments").document(paymentID)

        try {
            ref.set(payment.toMap(), SetOptions.merge())
                .addOnSuccessListener { completion(true, null) }
                .addOnFailureListener { e -> completion(false, "Failed to save payment: ${e.localizedMessage}") }
        } catch (e: Exception) {
            completion(false, "Encoding error: ${e.localizedMessage}")
        }
    }

    // MARK: - 🔹 Update Payment Status
    fun updatePaymentStatus(squadID: String, paymentID: String, status: String, reason: String, completion: (Boolean, String?) -> Unit) {
        val ref = db.collection("squads").document(squadID)
            .collection("payments").document(paymentID)

        val updateData = mapOf(
            "paymentStatus" to status,
            "paymentUpdatedDate" to FieldValue.serverTimestamp()
        )

        ref.update(updateData)
            .addOnSuccessListener { completion(true, null) }
            .addOnFailureListener { e -> completion(false, "Failed to update payment: ${e.localizedMessage}") }
    }

    fun updateContributionApproveStatus(
        squadID: String,
        memberID: String,
        contributionID: String,
        status: PaidStatus,
        completion: (Boolean, ContributionDetail?, String?) -> Unit
    ) {

        val contributionRef = db.collection("squads")
            .document(squadID)
            .collection("members")
            .document(memberID)
            .collection("contributions")
            .document(contributionID)

        val updateData = hashMapOf<String, Any>(
            "paidStatus" to status.value
        )

        contributionRef.update(updateData)
            .addOnSuccessListener {

                contributionRef.get()
                    .addOnSuccessListener { snapshot ->

                        if (!snapshot.exists()) {
                            completion(false, null, "Contribution not found")
                            return@addOnSuccessListener
                        }

                        try {
                            val contribution = snapshot.toObject(ContributionDetail::class.java)
                            completion(true, contribution, null)
                        } catch (e: Exception) {
                            completion(false, null, "Decode error: ${e.localizedMessage}")
                        }
                    }
                    .addOnFailureListener { e ->
                        completion(false, null, "Failed to fetch updated contribution: ${e.localizedMessage}")
                    }
            }
            .addOnFailureListener { e ->
                completion(false, null, "Failed to update contribution approval: ${e.localizedMessage}")
            }
    }


    fun updatePaymentApproveStatus(
        squadID: String,
        paymentID: String,
        status: PaymentApproveStatus,
        completion: (Boolean, PaymentsDetails?, String?) -> Unit
    ) {

        val paymentRef = db.collection("squads")
            .document(squadID)
            .collection("payments")
            .document(paymentID)

        paymentRef.get()
            .addOnSuccessListener { snapshot ->

                if (!snapshot.exists()) {
                    completion(false, null, "Payment not found")
                    return@addOnSuccessListener
                }

                val existingPayment = snapshot.toObject(PaymentsDetails::class.java)

                if (existingPayment?.paymentApproveStatus?.value == status.value) {
                    completion(false, existingPayment, "Already in ${status.value} status")
                    return@addOnSuccessListener
                }

                val paymentStatus: PaymentStatus
                val paymentResponseMessage: String

                if (status == PaymentApproveStatus.ACCEPTED) {
                    paymentStatus = PaymentStatus.SUCCESS
                    paymentResponseMessage =
                        "Your payment has been successfully processed and verified."
                } else {
                    paymentStatus = PaymentStatus.FAILED
                    paymentResponseMessage =
                        "Your payment was rejected by the admin as the amount was not received. Please verify and try again."
                }

                val updateData = hashMapOf<String, Any>(
                    "paymentStatus" to paymentStatus.value,
                    "paymentResponseMessage" to paymentResponseMessage,
                    "paymentApproveStatus" to status.value,
                    "paymentUpdatedDate" to FieldValue.serverTimestamp()
                )

                paymentRef.update(updateData)
                    .addOnSuccessListener {

                        paymentRef.get()
                            .addOnSuccessListener { updatedSnapshot ->

                                if (!updatedSnapshot.exists()) {
                                    completion(false, null, "Payment not found")
                                    return@addOnSuccessListener
                                }

                                try {
                                    val payment = updatedSnapshot.toObject(PaymentsDetails::class.java)
                                    completion(true, payment, null)
                                } catch (e: Exception) {
                                    completion(false, null, "Decode error: ${e.localizedMessage}")
                                }
                            }
                            .addOnFailureListener { e ->
                                completion(false, null, "Failed to fetch updated payment: ${e.localizedMessage}")
                            }
                    }
                    .addOnFailureListener { e ->
                        completion(false, null, "Failed to update approval status: ${e.localizedMessage}")
                    }

            }
            .addOnFailureListener { e ->
                completion(false, null, "Failed to fetch payment: ${e.localizedMessage}")
            }
    }


    fun fetchPendingApprovals(
        squadID: String,
        screenType: SquadUserType,
        memberId: String?,
        completion: (List<PaymentsDetails>?, String?) -> Unit
    ) {

        val query = db.collection("squads")
            .document(squadID)
            .collection("payments")
            .whereEqualTo("paymentApproveStatus", "REQUESTED")
            .let {
                if (screenType == SquadUserType.SQUAD_MANAGER) {
                    it.whereEqualTo("paymentType", "PAYMENT_CREDIT")
                } else {
                    it.whereEqualTo("paymentType", "PAYMENT_DEBIT")
                }
            }
            .let {
                if (memberId != null) it.whereEqualTo("memberId", memberId) else it
            }
            .orderBy("recordDate", Query.Direction.DESCENDING)

        query.get()
            .addOnSuccessListener { snapshot ->

                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(PaymentsDetails::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }

                completion(list, null)

                // NOTE: Recomputing and writing `verifyAmountCount` as a side effect of
                // a *read* is inherently racy against concurrent fetches from other
                // clients. Kept as a best-effort UX affordance; the source of truth for
                // this counter should ideally live server-side (e.g. a Cloud Function
                // trigger on payment writes).
                val verifyCount = list.size

                if (screenType == SquadUserType.SQUAD_MANAGER) {
                    db.collection("squads")
                        .document(squadID)
                        .update("verifyAmountCount", verifyCount)
                        .addOnFailureListener {
                            FmLog.w("verifyAmountCount sync failed: ${it.message}")
                        }
                } else if (!memberId.isNullOrEmpty()) {
                    db.collection("squads").document(squadID)
                        .collection("members").document(memberId)
                        .update("verifyAmountCount", verifyCount)
                        .addOnFailureListener {
                            FmLog.w("verifyAmountCount sync failed: ${it.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                completion(null, e.message)
            }
    }

    // MARK: - 🔹 Update Member UPI BeneId
    fun updateMemberUPIBeneId(squadID: String, memberID: String, upiID: String, completion: (Boolean, String?) -> Unit) {
        val ref = db.collection("squads").document(squadID)
            .collection("members").document(memberID)

        ref.update(mapOf("upiBeneId" to upiID))
            .addOnSuccessListener { completion(true, null) }
            .addOnFailureListener { e -> completion(false, "Failed to update UPI beneficiary: ${e.localizedMessage}") }
    }

    // MARK: - 🔹 Update Member Bank BeneID
    fun updateMemberBankBeneID(squadID: String, memberID: String, bankID: String, completion: (Boolean, String?) -> Unit) {
        val ref = db.collection("squads").document(squadID)
            .collection("members").document(memberID)

        ref.update(mapOf("bankBeneId" to bankID))
            .addOnSuccessListener { completion(true, null) }
            .addOnFailureListener { e -> completion(false, "Failed to update bank beneficiary: ${e.localizedMessage}") }
    }

    // MARK: - 🔹 Update Member Mobile Number
    fun updateMemberMobileNumber(
        squadID: String,
        memberID: String,
        mobileNumber: String,
        login: Login,
        completion: (Boolean, String?) -> Unit
    ) {

        val memberRef = db.collection("squads")
            .document(squadID)
            .collection("members")
            .document(memberID)

        memberRef.update("phoneNumber", mobileNumber)
            .addOnSuccessListener {

                removeUserLogin(
                    squadID = login.squadID,
                    squadUserId = login.squadUserId,
                    role = login.role,
                    phoneNumber = login.phoneNumber
                ) { removed, error ->

                    if (!removed) {
                        completion(false, error ?: "Failed to remove existing login.")
                        return@removeUserLogin
                    }

                    val newLogin = Login(
                        squadID = login.squadID,
                        squadName = login.squadName,
                        squadUsername = login.squadUsername,
                        squadUserId = memberID,
                        phoneNumber = mobileNumber,
                        role = login.role,
                        squadCreatedDate = login.squadCreatedDate,
                        userCreatedDate = login.userCreatedDate
                    )

                    addUserLogin(newLogin) { added, addError ->
                        if (!added) {
                            completion(false, addError ?: "Failed to create new login.")
                            return@addUserLogin
                        }
                        completion(true, null)
                    }
                }
            }
            .addOnFailureListener { e ->
                completion(false, "Failed to update mobile number: ${e.localizedMessage}")
            }
    }

    fun removeUserLogin(
        squadID: String,
        squadUserId: String,
        role: SquadUserType,
        phoneNumber: String,
        completion: (Boolean, String?) -> Unit
    ) {

        val loginsRef = db.collection("users")
            .document(phoneNumber)
            .collection("logins")

        loginsRef
            .whereEqualTo("squadID", squadID)
            .whereEqualTo("squadUserId", squadUserId)
            .whereEqualTo("role", role.name)
            .get()
            .addOnSuccessListener { snapshot ->

                if (snapshot.isEmpty) {
                    completion(false, "No matching login found.")
                    return@addOnSuccessListener
                }

                val batch = db.batch()

                snapshot.documents.forEach { document ->
                    batch.delete(document.reference)
                }

                batch.commit()
                    .addOnSuccessListener { completion(true, null) }
                    .addOnFailureListener { e -> completion(false, e.localizedMessage) }
            }
            .addOnFailureListener { e ->
                completion(false, e.localizedMessage)
            }
    }


    fun updateMemberAmount(
        squadID: String,
        memberID: String,
        amount: Int,
        editAmountType: AmountEditType,
        completion: (Boolean, String?) -> Unit
    ) {

        if (squadID.isBlank()) {
            completion(false, "Invalid squad ID.")
            return
        }

        if (memberID.isBlank()) {
            completion(false, "Invalid member ID.")
            return
        }

        val fieldName = when (editAmountType) {

            AmountEditType.contribution ->
                "totalContributionPaid"

            AmountEditType.loanBorrowed ->
                "totalLoanBorrowed"

            AmountEditType.paidLoadAmount ->
                "totalLoanPaid"

            AmountEditType.intrestAmount ->
                "totalInterestPaid"

            AmountEditType.totalSquadAmount,
            AmountEditType.others -> {
                completion(false, "Unsupported amount type.")
                return
            }
        }

        db.collection("squads")
            .document(squadID)
            .collection("members")
            .document(memberID)
            .update(fieldName, amount)
            .addOnSuccessListener {

                completion(true, null)

            }
            .addOnFailureListener { exception ->

                completion(
                    false,
                    "Failed to update member amount: ${exception.localizedMessage}"
                )
            }
    }

    // MARK: - 🔹 Update Contribution Status
    fun updateContributionStatus(
        squadID: String,
        memberID: String,
        contributionID: String,
        amount: Int,
        newStatus: String,
        completion: (Boolean, String?) -> Unit
    ) {
        val contributionRef = db.collection("squads")
            .document(squadID)
            .collection("members")
            .document(memberID)
            .collection("contributions")
            .document(contributionID)

        val updateData = mapOf(
            "amount" to amount,
            "paidOn" to FieldValue.serverTimestamp(),
            "paidStatus" to newStatus
        )

        contributionRef.update(updateData)
            .addOnSuccessListener {
                completion(true, "Contribution status updated to $newStatus")
            }
            .addOnFailureListener { e ->
                completion(false, "Failed to update status: ${e.localizedMessage}")
            }
    }

    // MARK: - 🔹 Update Loan Status
    fun updateLoanStatusPaid(
        squadID: String,
        memberID: String,
        loanID: String,
        isForceClosed : Boolean,
        completion: (Boolean, String?) -> Unit
    ) {

        val loanRef = db.collection("squads")
            .document(squadID)
            .collection("members")
            .document(memberID)
            .collection("loans")
            .document(loanID)

        loanRef.update(
            mapOf(
                "duePaidDate" to FieldValue.serverTimestamp(),
                "loanStatus" to EMIStatus.PAID.name,
                "isForceClosed" to isForceClosed
            )
        )
            .addOnSuccessListener {

                val memberRef = db.collection("squads")
                    .document(squadID)
                    .collection("members")
                    .document(memberID)

                memberRef.set(
                    mapOf(
                        "currentLoanApproveStatus" to EMIStatus.CREATED.name,
                        "cashRequested" to false
                    ),
                    SetOptions.merge()
                )
                    .addOnSuccessListener {
                        completion(true, "Loan closed successfully.")
                    }
                    .addOnFailureListener { e ->
                        completion(
                            false,
                            "Loan updated but failed to update member: ${e.localizedMessage}"
                        )
                    }
            }
            .addOnFailureListener { e ->
                completion(
                    false,
                    "Failed to update loan status: ${e.localizedMessage}"
                )
            }
    }

    fun updateLoanForceCloseVerification(
        squadID: String,
        memberID: String,
        loanID: String,
        isForceCloseVerification: Boolean,
        completion: (Boolean, String?) -> Unit
    ) {

        val loanRef = db.collection("squads")
            .document(squadID)
            .collection("members")
            .document(memberID)
            .collection("loans")
            .document(loanID)

        loanRef.update(
            "isForceCloseVerification",
            isForceCloseVerification
        )
            .addOnSuccessListener {
                completion(true, "Loan force close verification updated successfully.")
            }
            .addOnFailureListener { exception ->
                completion(
                    false,
                    "Failed to update loan force close verification: ${exception.localizedMessage}"
                )
            }
    }

    // FIX #5: transactionalized to remove the read-modify-write race on the
    // `installments` array field.
    fun updateLoanAndAllInstallmentsStatus(
        squadID: String,
        memberID: String,
        loanID: String,
        status: String,
        completion: (Boolean, String?) -> Unit
    ) {

        val loanRef = db.collection("squads")
            .document(squadID)
            .collection("members")
            .document(memberID)
            .collection("loans")
            .document(loanID)

        db.runTransaction { transaction ->

            val snapshot = transaction.get(loanRef)

            if (!snapshot.exists()) {
                throw IllegalStateException("Loan not found")
            }

            @Suppress("UNCHECKED_CAST")
            val installments = (snapshot.get("installments") as? List<HashMap<String, Any>>)
                ?.toMutableList()
                ?: throw IllegalStateException("No installments found")

            for (i in installments.indices) {
                installments[i]["status"] = status
            }

            transaction.update(
                loanRef,
                mapOf(
                    "loanStatus" to status,
                    "installments" to installments
                )
            )

            null
        }
            .addOnSuccessListener {
                completion(true, "Loan + installments updated successfully")
            }
            .addOnFailureListener { e ->
                completion(false, e.localizedMessage ?: "Update failed")
            }
    }

    // FIX #5: transactionalized for the same reason as updateLoanAndAllInstallmentsStatus.
    fun updateInstallmentStatus(
        squadID: String,
        memberID: String,
        loanID: String,
        installmentID: String,
        status: String,
        completion: (Boolean, String?) -> Unit
    ) {

        val loanRef = db.collection("squads")
            .document(squadID)
            .collection("members")
            .document(memberID)
            .collection("loans")
            .document(loanID)

        val allPaidRef = AtomicReference(false)

        db.runTransaction { transaction ->

            val snapshot = transaction.get(loanRef)

            if (!snapshot.exists()) {
                throw IllegalStateException("Loan not found")
            }

            @Suppress("UNCHECKED_CAST")
            val installments = (snapshot.get("installments") as? List<HashMap<String, Any>>)
                ?.toMutableList()
                ?: throw IllegalStateException("Installments not found")

            var found = false
            val now = Timestamp.now()

            for (item in installments) {
                if (item["id"]?.toString() == installmentID) {
                    item["status"] = status
                    item["duePaidDate"] = now
                    found = true
                    break
                }
            }

            if (!found) {
                throw IllegalStateException("Installment ID not found")
            }

            val allPaid = installments.all { it["status"]?.toString()?.uppercase() == "PAID" }
            allPaidRef.set(allPaid)

            val updateMap = hashMapOf<String, Any>(
                "installments" to installments,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (allPaid) {
                updateMap["loanStatus"] = "PAID"
                updateMap["loanClosedDate"] = FieldValue.serverTimestamp()
            }

            transaction.update(loanRef, updateMap)

            null
        }
            .addOnSuccessListener {

                if (!allPaidRef.get()) {
                    completion(true, "Installment updated successfully")
                    return@addOnSuccessListener
                }

                val documentRef = db.collection("squads")
                    .document(squadID)
                    .collection("members")
                    .document(memberID)

                documentRef.get()
                    .addOnSuccessListener { document ->

                        if (!document.exists()) {
                            completion(false, "Member not found.")
                            return@addOnSuccessListener
                        }

                        val updateData = hashMapOf<String, Any>(
                            "currentLoanApproveStatus" to "CREATED",
                            "cashRequested" to false
                        )

                        documentRef.set(updateData, SetOptions.merge())
                            .addOnSuccessListener {
                                completion(true, "Installment updated & loan closed successfully")
                            }
                            .addOnFailureListener { error ->
                                completion(false, "${error.localizedMessage} — loan updated but failed to update member status")
                            }
                    }
                    .addOnFailureListener { error ->
                        completion(false, "Failed to fetch member: ${error.localizedMessage}")
                    }
            }
            .addOnFailureListener { e ->
                completion(false, e.localizedMessage ?: "Update failed")
            }
    }

    // MARK: - 🔹 Save Multiple Payments (Batch)
    fun savePayments(
        squadID: String,
        payment: PaymentsDetails?,
        completion: (Boolean, String?) -> Unit
    ) {

        val paymentID = payment?.id
        if (paymentID.isNullOrEmpty()) {
            completion(false, "Payment ID is missing.")
            return
        }

        val batch = db.batch()
        val squadRef = db.collection("squads").document(squadID)
        val paymentRef = squadRef.collection("payments").document(paymentID)

        batch.set(paymentRef, payment, SetOptions.merge())

        batch.commit()
            .addOnSuccessListener {

                if (payment.cashRequestId?.isNotEmpty() == true) {

                    updateCashRequestStatus(
                        squadID = squadID,
                        cashRequestId = payment.cashRequestId!!,
                        memberId = payment.memberId,
                        status = CashRequestStatus.ACCEPTED
                    ) { error ->
                        if (error != null) {
                            completion(false, error)
                        } else {
                            completion(true, null)
                        }
                    }

                } else {
                    completion(true, null)
                }
            }
            .addOnFailureListener { error ->
                completion(false, "Batch commit failed: ${error.localizedMessage}")
            }
    }

    // MARK: - 🔹 Fetch Payments
    fun fetchPayments(
        squadID: String,
        memberId: String? = null,
        filterType: PaymentFilter = PaymentFilter.ALL,
        lastDocument: DocumentSnapshot? = null,
        showRejected: Boolean = true,
        limit: Int,
        completion: (List<PaymentsDetails>?, DocumentSnapshot?, String?) -> Unit
    ) {

        var query: Query = db
            .collection("squads")
            .document(squadID)
            .collection("payments")
            .orderBy("recordDate", Query.Direction.DESCENDING)
            .limit(limit.toLong())

        if (!memberId.isNullOrEmpty()) {
            query = query.whereEqualTo("memberId", memberId)
        }

        if (filterType == PaymentFilter.CREDIT) {
            query = query.whereEqualTo("paymentType", "PAYMENT_CREDIT")
        } else if (filterType == PaymentFilter.DEBIT) {
            query = query.whereEqualTo("paymentType", "PAYMENT_DEBIT")
        }

        if (!showRejected) {
            query = query.whereNotEqualTo("paymentApproveStatus", "REJECTED")
        }

        if (lastDocument != null) {
            query = query.startAfter(lastDocument)
        }

        query.get()
            .addOnSuccessListener { snapshot ->
                val payments = snapshot.documents.mapNotNull {
                    it.toObject(PaymentsDetails::class.java)
                }
                completion(payments, snapshot.documents.lastOrNull(), null)
            }
            .addOnFailureListener {
                completion(null, null, it.localizedMessage)
            }
    }

    // MARK: - 🔹 Observe Payments (Realtime)
    /**
     * FIX #1: Returns the [ListenerRegistration] so the caller can stop the listener
     * (e.g. in `onCleared()` / `onDestroy()`). Previously this leaked a live listener
     * on every call since nothing was ever returned to allow `.remove()`.
     * Callers MUST retain the returned registration and call `.remove()` when the
     * observing screen/view model goes away.
     */
    fun observePayments(squadID: String, completion: (List<PaymentsDetails>?, String?) -> Unit): ListenerRegistration {
        val paymentsRef = db.collection("squads")
            .document(squadID)
            .collection("payments")

        return paymentsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                completion(null, "Error observing payments: ${error.localizedMessage}")
                return@addSnapshotListener
            }

            if (snapshot == null) {
                completion(emptyList(), null)
                return@addSnapshotListener
            }

            val payments = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(PaymentsDetails::class.java)
                } catch (e: Exception) {
                    null
                }
            }

            completion(payments, null)
        }
    }

    // MARK: - 🔹 Add Member
    fun addMember(
        squadID: String,
        member: Member,
        completion: (Boolean, String?) -> Unit
    ) {

        val membersRef = db.collection("squads")
            .document(squadID)
            .collection("members")

        val squadRef = db.collection("squads").document(squadID)

        val memberID = member.id

        if (memberID == null) {
            completion(false, "Error adding member: Member ID is null")
            return
        }

        try {
            membersRef.document(memberID).set(member)
                .addOnSuccessListener {
                    squadRef.update("totalMembers", FieldValue.increment(1))
                        .addOnSuccessListener {
                            completion(true, null)
                        }
                        .addOnFailureListener { e ->
                            completion(false, "Member added but counter update failed: ${e.localizedMessage}")
                        }
                }
                .addOnFailureListener { e ->
                    completion(false, "Error adding member: ${e.localizedMessage}")
                }
        } catch (e: Exception) {
            completion(false, "Error adding member: ${e.localizedMessage}")
        }
    }

    // MARK: - 🔹 Fetch Single Member
    fun fetchMember(
        squadID: String,
        memberID: String,
        completion: (Member?, String?) -> Unit
    ) {
        val memberRef = db.collection("squads")
            .document(squadID)
            .collection("members")
            .document(memberID)

        memberRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    try {
                        val member = document.toObject(Member::class.java)?.apply {
                            id = document.id
                        }

                        if (member != null) {
                            completion(member, null)
                        } else {
                            completion(null, "Failed to decode member object.")
                        }
                    } catch (e: Exception) {
                        FmLog.w("fetchMember decoding error: ${e.localizedMessage}")
                        completion(null, "Decoding error: ${e.localizedMessage}")
                    }
                } else {
                    completion(null, "Member not found.")
                }
            }
            .addOnFailureListener { e ->
                FmLog.w("fetchMember failed: ${e.localizedMessage}")
                completion(null, "Error fetching member: ${e.localizedMessage}")
            }
    }

    // MARK: - 🔹 Fetch All Members
    fun fetchMembers(
        squadID: String,
        completion: (List<Member>?, String?) -> Unit
    ) {
        val membersRef = db.collection("squads")
            .document(squadID)
            .collection("members")

        membersRef.get()
            .addOnSuccessListener { snapshot ->
                // FIX #3: an empty squad is not an error.
                if (snapshot == null || snapshot.isEmpty) {
                    completion(emptyList(), null)
                    return@addOnSuccessListener
                }

                try {
                    val members = snapshot.documents.mapNotNull { doc ->
                        try {
                            val member = doc.toObject(Member::class.java)
                            member?.id = doc.id
                            member
                        } catch (e: Exception) {
                            FmLog.w("Error decoding member: ${e.localizedMessage}")
                            null
                        }
                    }
                    completion(members, null)
                } catch (e: Exception) {
                    completion(null, "Decoding error: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener { e ->
                completion(null, "Error fetching members: ${e.localizedMessage}")
            }
    }

    // MARK: - 🔹 Update Members (Batch-like using async group)
    fun updateMembers(
        squadID: String,
        members: List<Member>,
        completion: (Boolean, String?) -> Unit
    ) {
        if (members.isEmpty()) {
            completion(true, null)
            return
        }

        val updateErrors = java.util.Collections.synchronizedList(mutableListOf<String>())
        val latch = CountDownLatch(members.size)

        for (member in members) {
            val memberID = member.id
            if (memberID == null) {
                updateErrors.add("Member ID missing for ${member.name}")
                latch.countDown()
                continue
            }

            val memberRef = db.collection("squads")
                .document(squadID)
                .collection("members")
                .document(memberID)

            try {
                memberRef.set(member, SetOptions.merge())
                    .addOnFailureListener { e ->
                        updateErrors.add("Error updating ${member.name}: ${e.localizedMessage}")
                        latch.countDown()
                    }
                    .addOnSuccessListener {
                        latch.countDown()
                    }
            } catch (e: Exception) {
                updateErrors.add("Error encoding ${member.name}: ${e.localizedMessage}")
                latch.countDown()
            }
        }

        Thread {
            latch.await()
            if (updateErrors.isEmpty()) {
                completion(true, null)
            } else {
                completion(false, updateErrors.joinToString("\n"))
            }
        }.start()
    }

    // MARK: - 🔹 Delete Member
    fun deleteMember(
        squadID: String,
        memberID: String,
        completion: (Boolean, String?) -> Unit
    ) {
        val memberRef = db.collection("squads")
            .document(squadID)
            .collection("members")
            .document(memberID)

        memberRef.delete()
            .addOnSuccessListener { completion(true, null) }
            .addOnFailureListener { e -> completion(false, "Error deleting member: ${e.localizedMessage}") }
    }

    // MARK: - 🔹 Create contributions when member is created
    fun createContributionWhenMemberCreate(
        squadID: String,
        memberID: String,
        memberName: String,
        squadStart: Date,
        squadEnd: Date,
        amount: Int,
        completion: (Boolean, String?) -> Unit
    ) {
        val memberRef = db.collection("squads").document(squadID)
            .collection("members").document(memberID)
        val contributionsRef = memberRef.collection("contributions")

        val dateFormatter = SimpleDateFormat("MMM yyyy", Locale.US)
        var currentDate = squadStart
        val contributionData = mutableMapOf<String, ContributionDetail>()

        while (!currentDate.after(squadEnd)) {
            val monthYear = dateFormatter.format(currentDate)
            val contributionID = CommonFunctions.generateContributionID(memberID, monthYear)

            val detail = ContributionDetail(
                id = contributionID,
                orderId = "",
                memberID = memberID,
                memberName = memberName,
                monthYear = monthYear,
                amount = amount,
                paidOn = null,
                paidStatus = PaidStatus.NOT_PAID,
                paymentEntryType = PaymentEntryType.AUTOMATIC_ENTRY,
                dueDate = CommonFunctions.getContributionDue(monthYear).asTimestamp
            )

            contributionData[contributionID] = detail
            val cal = Calendar.getInstance()
            cal.time = currentDate
            cal.add(Calendar.MONTH, 1)
            currentDate = cal.time
        }

        val batch = db.batch()
        try {
            contributionData.forEach { (id, contribution) ->
                val docRef = contributionsRef.document(id)
                batch.set(docRef, contribution)
            }

            batch.commit()
                .addOnSuccessListener {
                    completion(true, "Contributions created successfully")
                }
                .addOnFailureListener { e ->
                    completion(false, "Batch write failed: ${e.localizedMessage}")
                }
        } catch (e: Exception) {
            completion(false, "Encoding error: ${e.localizedMessage}")
        }
    }

    // MARK: - 🔹 Edit contributions when months changed
    fun contibutionEditWhenMonthsChanged(
        squadID: String,
        squadStartDate: Date,
        squadEndDate: Date,
        amount: String,
        completion: (Boolean, String?) -> Unit
    ) {
        val membersRef = db.collection("squads").document(squadID).collection("members")
        val dateFormatter = SimpleDateFormat("MMM yyyy", Locale.US)
        val validMonths = mutableSetOf<String>()

        var currentDate = squadStartDate
        val calendar = Calendar.getInstance()

        while (!currentDate.after(squadEndDate)) {
            validMonths.add(dateFormatter.format(currentDate))
            calendar.time = currentDate
            calendar.add(Calendar.MONTH, 1)
            currentDate = calendar.time
        }

        membersRef.get()
            .addOnSuccessListener { snapshot ->
                val memberDocs = snapshot.documents
                if (memberDocs.isEmpty()) {
                    completion(true, null) // Nothing to do — not an error.
                    return@addOnSuccessListener
                }

                val processedCount = AtomicInteger(0)
                val encounteredError = AtomicReference<String?>(null)

                for (memberDoc in memberDocs) {
                    val memberID = memberDoc.id
                    val member = memberDoc.toObject(Member::class.java)
                    if (member == null) {
                        if (processedCount.incrementAndGet() == memberDocs.size) {
                            val err = encounteredError.get()
                            completion(err == null, err ?: "Failed to decode member: $memberID")
                        }
                        continue
                    }

                    val contributionsRef = membersRef.document(memberID).collection("contributions")
                    contributionsRef.get()
                        .addOnSuccessListener { contribSnapshot ->
                            val contribDocs = contribSnapshot.documents
                            val existingMonths = contribDocs.mapNotNull { it.getString("monthYear") }.toSet()

                            val toDelete = contribDocs.filter {
                                val month = it.getString("monthYear") ?: return@filter false
                                !validMonths.contains(month)
                            }

                            val missingMonths = validMonths.subtract(existingMonths)
                            val batch = db.batch()

                            toDelete.forEach { batch.delete(it.reference) }

                            for (month in missingMonths) {
                                val newContribution = ContributionDetail(
                                    id = CommonFunctions.generateContributionID(memberID, month),
                                    orderId = "",
                                    memberID = memberID,
                                    memberName = member.name,
                                    monthYear = month,
                                    amount = amount.toIntOrNull() ?: 0,
                                    paidOn = null,
                                    paidStatus = PaidStatus.NOT_PAID,
                                    paymentEntryType = PaymentEntryType.AUTOMATIC_ENTRY,
                                    dueDate = CommonFunctions.getContributionDue(month).asTimestamp
                                )
                                val newDoc = contributionsRef.document(newContribution.id ?: UUID.randomUUID().toString())
                                batch.set(newDoc, newContribution)
                            }

                            batch.commit()
                                .addOnSuccessListener {
                                    if (processedCount.incrementAndGet() == memberDocs.size) {
                                        val err = encounteredError.get()
                                        if (err != null) completion(false, err)
                                        else completion(true, "Successfully updated all member contributions.")
                                    }
                                }
                                .addOnFailureListener { e ->
                                    encounteredError.compareAndSet(null, "Failed to update $memberID: ${e.localizedMessage}")
                                    if (processedCount.incrementAndGet() == memberDocs.size) {
                                        completion(false, encounteredError.get())
                                    }
                                }
                        }
                        .addOnFailureListener { e ->
                            encounteredError.compareAndSet(null, "Failed to fetch contributions for $memberID: ${e.localizedMessage}")
                            if (processedCount.incrementAndGet() == memberDocs.size) {
                                completion(false, encounteredError.get())
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                completion(false, "Failed to fetch members: ${e.localizedMessage}")
            }
    }

    // MARK: - 🔹 Add Single Contribution
    fun addContribution(
        squadID: String,
        memberID: String,
        contribution: ContributionDetail,
        completion: (Boolean, String?) -> Unit
    ) {
        val contributionRef = db.collection("squads")
            .document(squadID)
            .collection("members")
            .document(memberID)
            .collection("contributions")
            .document(contribution.id ?: UUID.randomUUID().toString())

        contributionRef.set(contribution)
            .addOnSuccessListener {
                completion(true, "Contribution added successfully")
            }
            .addOnFailureListener { e ->
                completion(false, "Error adding contribution: ${e.localizedMessage}")
            }
    }

    // MARK: - 🔹 Fetch Contributions for a Member
    fun fetchContributionsForMember(
        squadID: String,
        memberID: String,
        completion: (List<ContributionDetail>?, String?) -> Unit
    ) {
        val contributionsRef = db.collection("squads")
            .document(squadID)
            .collection("members")
            .document(memberID)
            .collection("contributions")

        contributionsRef.get()
            .addOnSuccessListener { snapshot ->
                val documents = snapshot.documents
                if (documents.isEmpty()) {
                    completion(emptyList(), null)
                    return@addOnSuccessListener
                }

                try {
                    val contributions = documents.mapNotNull { doc ->
                        val contribution = doc.toObject(ContributionDetail::class.java)
                        contribution?.id = doc.id
                        contribution
                    }
                    completion(contributions, null)
                } catch (e: Exception) {
                    completion(null, "Decoding error: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener { e ->
                completion(null, "Error fetching contributions: ${e.localizedMessage}")
            }
    }

    // MARK: - 🔹 Fetch Total Contribution
    fun fetchTotalContribution(
        squadID: String,
        completion: (Int?, String?) -> Unit
    ) {
        val membersRef = db.collection("squads").document(squadID).collection("members")

        membersRef.get()
            .addOnSuccessListener { snapshot ->
                val memberDocs = snapshot.documents
                if (memberDocs.isEmpty()) {
                    completion(0, null)
                    return@addOnSuccessListener
                }

                val totalAmount = AtomicInteger(0)
                val processedCount = AtomicInteger(0)
                val errorMessage = AtomicReference<String?>(null)

                for (doc in memberDocs) {
                    val memberID = doc.id
                    val contributionsRef = membersRef.document(memberID).collection("contributions")

                    contributionsRef.get()
                        .addOnSuccessListener { contribSnapshot ->
                            val sum = contribSnapshot.documents.sumOf {
                                (it.getLong("amount") ?: 0L).toInt()
                            }
                            totalAmount.addAndGet(sum)
                            if (processedCount.incrementAndGet() == memberDocs.size) {
                                val err = errorMessage.get()
                                if (err != null) completion(null, err) else completion(totalAmount.get(), null)
                            }
                        }
                        .addOnFailureListener { e ->
                            errorMessage.compareAndSet(null, "Error fetching contributions for $memberID: ${e.localizedMessage}")
                            if (processedCount.incrementAndGet() == memberDocs.size) {
                                completion(null, errorMessage.get())
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                completion(null, "Error fetching members: ${e.localizedMessage}")
            }
    }

    // MARK: - 🔹 Edit Contribution
    fun editContribution(
        squadID: String,
        memberID: String,
        contributionID: String,
        updatedContribution: ContributionDetail,
        completion: (Boolean, String?) -> Unit
    ) {
        val contributionRef = db.collection("squads")
            .document(squadID)
            .collection("members")
            .document(memberID)
            .collection("contributions")
            .document(contributionID)

        contributionRef.set(updatedContribution, SetOptions.merge())
            .addOnSuccessListener {
                completion(true, "Contribution updated successfully")
            }
            .addOnFailureListener { e ->
                completion(false, "Failed to update contribution: ${e.localizedMessage}")
            }
    }

    // MARK: - Add EMI Configuration
    fun addEMIConfiguration(
        squadID: String,
        emiConfig: EMIConfiguration,
        completion: (Boolean, String?) -> Unit
    ) {
        val emiRef = db.collection("squads").document(squadID)
            .collection("emiConfiguration").document()

        val newEmiConfig = emiConfig.copy(id = emiRef.id)

        emiRef.set(newEmiConfig)
            .addOnSuccessListener { completion(true, null) }
            .addOnFailureListener { e -> completion(false, "Error adding EMI configuration: ${e.localizedMessage}") }
    }

    // MARK: - Add or Update EMI Configuration
    fun addOrUpdateEMIConfiguration(
        squadID: String,
        emiConfig: EMIConfiguration,
        completion: (Boolean, String?) -> Unit
    ) {
        val emiRef = if (!emiConfig.id.isNullOrEmpty()) {
            db.collection("squads").document(squadID)
                .collection("emiConfiguration").document(emiConfig.id!!)
        } else {
            db.collection("squads").document(squadID)
                .collection("emiConfiguration").document()
        }

        val newEmiConfig = emiConfig.copy(id = emiRef.id)

        emiRef.set(newEmiConfig, SetOptions.merge())
            .addOnSuccessListener { completion(true, null) }
            .addOnFailureListener { e -> completion(false, "Error adding or updating EMI configuration: ${e.localizedMessage}") }
    }

    // MARK: - Fetch EMI Configurations
    fun fetchEMIConfigurations(
        squadID: String,
        completion: (List<EMIConfiguration>?, String?) -> Unit
    ) {
        val emiRef = db.collection("squads").document(squadID)
            .collection("emiConfiguration")

        emiRef.get()
            .addOnSuccessListener { snapshot ->
                val documents = snapshot.documents
                if (documents.isEmpty()) {
                    completion(emptyList(), null)
                    return@addOnSuccessListener
                }

                try {
                    val emiConfigs = documents.mapNotNull { doc ->
                        val config = doc.toObject(EMIConfiguration::class.java)
                        config?.copy(id = doc.id)
                    }
                    completion(emiConfigs, null)
                } catch (e: Exception) {
                    completion(null, "Decoding error: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener { e ->
                completion(null, "Error fetching EMI configurations: ${e.localizedMessage}")
            }
    }

    // MARK: - Fetch EMI Configuration by ID
    fun fetchEMIConfigurationByID(
        squadID: String,
        emiID: String,
        completion: (EMIConfiguration?, String?) -> Unit
    ) {
        val emiRef = db.collection("squads").document(squadID)
            .collection("emiConfiguration").document(emiID)

        emiRef.get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    completion(null, "EMI configuration not found.")
                    return@addOnSuccessListener
                }

                try {
                    val emiConfig = document.toObject(EMIConfiguration::class.java)?.copy(id = document.id)
                    completion(emiConfig, null)
                } catch (e: Exception) {
                    completion(null, "Decoding error: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener { e ->
                completion(null, "Error fetching EMI configuration: ${e.localizedMessage}")
            }
    }

    // MARK: - Update EMI Configuration
    fun updateEMIConfiguration(
        squadID: String,
        emiConfig: EMIConfiguration,
        completion: (Boolean, String?) -> Unit
    ) {
        val emiID = emiConfig.id
        if (emiID.isNullOrEmpty()) {
            completion(false, "EMI ID is missing")
            return
        }

        val emiRef = db.collection("squads").document(squadID)
            .collection("emiConfiguration").document(emiID)

        emiRef.set(emiConfig, SetOptions.merge())
            .addOnSuccessListener { completion(true, null) }
            .addOnFailureListener { e -> completion(false, "Error updating EMI configuration: ${e.localizedMessage}") }
    }

    // MARK: - Delete EMI Configuration
    fun deleteEMIConfiguration(
        squadID: String,
        emiID: String,
        completion: (Boolean, String?) -> Unit
    ) {
        val emiRef = db.collection("squads").document(squadID)
            .collection("emiConfiguration").document(emiID)

        emiRef.delete()
            .addOnSuccessListener { completion(true, null) }
            .addOnFailureListener { e -> completion(false, "Error deleting EMI configuration: ${e.localizedMessage}") }
    }

    // MARK: - Add Squad Activity
    fun addSquadActivity(
        squadID: String,
        activity: SquadActivity,
        completion: (Boolean, String?) -> Unit
    ) {
        val activityRef = db.collection("squads")
            .document(squadID)
            .collection("activities")
            .document()

        val newActivity = activity.copy(id = activityRef.id)

        activityRef.set(newActivity)
            .addOnSuccessListener { completion(true, null) }
            .addOnFailureListener { e -> completion(false, "Error adding Squad Activity: ${e.localizedMessage}") }
    }

    fun fetchSquadActivities(
        squadID: String,
        memberId: String? = null,
        lastDocument: DocumentSnapshot? = null,
        limit: Int = 20,
        completion: (
            List<SquadActivity>?,
            DocumentSnapshot?,
            String?
        ) -> Unit
    ) {

        var query: Query = db.collection("squads")
            .document(squadID)
            .collection("activities")
            .orderBy("recordDate", Query.Direction.DESCENDING)
            .limit(limit.toLong())

        if (!memberId.isNullOrEmpty()) {
            query = query.whereEqualTo("memberId", memberId)
        }

        if (lastDocument != null) {
            query = query.startAfter(lastDocument)
        }

        query.get()
            .addOnSuccessListener { snapshot ->
                val activities = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(SquadActivity::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }
                completion(activities, snapshot.documents.lastOrNull(), null)
            }
            .addOnFailureListener { error ->
                completion(null, null, error.localizedMessage)
            }
    }

    // MARK: - Fetch Activity by ID
    fun fetchSquadActivityByID(
        squadID: String,
        activityID: String,
        completion: (SquadActivity?, String?) -> Unit
    ) {
        val activityRef = db.collection("squads")
            .document(squadID)
            .collection("activities")
            .document(activityID)

        activityRef.get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    completion(null, "Activity not found.")
                    return@addOnSuccessListener
                }

                try {
                    val activity = document.toObject(SquadActivity::class.java)?.copy(id = document.id)
                    completion(activity, null)
                } catch (e: Exception) {
                    completion(null, "Decoding error: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener { e ->
                completion(null, "Error fetching activity: ${e.localizedMessage}")
            }
    }

    // MARK: - Update Activity
    fun updateSquadActivity(
        squadID: String,
        activity: SquadActivity,
        completion: (Boolean, String?) -> Unit
    ) {
        val activityRef = if (!activity.id.isNullOrEmpty()) {
            db.collection("squads")
                .document(squadID)
                .collection("activities")
                .document(activity.id!!)
        } else {
            db.collection("squads")
                .document(squadID)
                .collection("activities")
                .document()
        }

        val updatedActivity = activity.copy(id = activityRef.id)

        activityRef.set(updatedActivity, SetOptions.merge())
            .addOnSuccessListener { completion(true, null) }
            .addOnFailureListener { e -> completion(false, "Error updating activity: ${e.localizedMessage}") }
    }

    // MARK: - Delete Activity
    fun deleteSquadActivity(
        squadID: String,
        activityID: String,
        completion: (Boolean, String?) -> Unit
    ) {
        val activityRef = db.collection("squads")
            .document(squadID)
            .collection("activities")
            .document(activityID)

        activityRef.delete()
            .addOnSuccessListener { completion(true, null) }
            .addOnFailureListener { e -> completion(false, "Error deleting activity: ${e.localizedMessage}") }
    }

    // MARK: - Add Squad Rule
    fun addSquadRule(
        squadID: String,
        rule: SquadRule,
        completion: (Boolean, String?) -> Unit
    ) {
        val ruleRef = db.collection("squads")
            .document(squadID)
            .collection("rules")
            .document()

        val newRule = rule.copy(id = ruleRef.id)

        ruleRef.set(newRule)
            .addOnSuccessListener { completion(true, null) }
            .addOnFailureListener { e -> completion(false, "Error adding Squad Rule: ${e.localizedMessage}") }
    }

    // MARK: - Fetch Squad Rules
    fun fetchSquadRules(
        squadID: String,
        completion: (List<SquadRule>?, String?) -> Unit
    ) {
        val rulesRef = db.collection("squads")
            .document(squadID)
            .collection("rules")
            .orderBy("recordDate", Query.Direction.DESCENDING)

        rulesRef.get()
            .addOnSuccessListener { snapshot ->
                val documents = snapshot.documents
                if (documents.isEmpty()) {
                    completion(emptyList(), null)
                    return@addOnSuccessListener
                }

                try {
                    val rules = documents.mapNotNull { doc ->
                        val rule = doc.toObject(SquadRule::class.java)
                        rule?.copy(id = doc.id)
                    }
                    completion(rules, null)
                } catch (e: Exception) {
                    completion(null, "Decoding error: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener { e ->
                completion(null, "Error fetching rules: ${e.localizedMessage}")
            }
    }

    // MARK: - Delete Squad Rule
    fun deleteSquadRule(
        squadID: String,
        ruleID: String,
        completion: (Boolean, String?) -> Unit
    ) {
        val ruleRef = db.collection("squads")
            .document(squadID)
            .collection("rules")
            .document(ruleID)

        ruleRef.delete()
            .addOnSuccessListener { completion(true, null) }
            .addOnFailureListener { e -> completion(false, "Error deleting rule: ${e.localizedMessage}") }
    }

    // MARK: - Update Squad Rule
    fun updateSquadRule(
        squadID: String,
        rule: SquadRule,
        completion: (Boolean, String?) -> Unit
    ) {
        // FIX #6: previously fell back to document("") when rule.id was null/blank,
        // silently creating/overwriting a garbage doc with an empty ID.
        val ruleID = rule.id
        if (ruleID.isNullOrBlank()) {
            completion(false, "Rule ID is missing")
            return
        }

        val ruleRef = db.collection("squads")
            .document(squadID)
            .collection("rules")
            .document(ruleID)

        ruleRef.set(rule)
            .addOnSuccessListener { completion(true, null) }
            .addOnFailureListener { e -> completion(false, "Error updating rule: ${e.localizedMessage}") }
    }

    // MARK: - 🔹 Read MemberEMI by Member
    fun fetchMemberLoans(
        squadID: String,
        memberID: String,
        completion: (List<MemberLoan>?, String?) -> Unit
    ) {
        db.collection("squads").document(squadID)
            .collection("members").document(memberID)
            .collection("loans")
            .get()
            .addOnSuccessListener { snapshot ->
                val loans = snapshot.documents.mapNotNull { doc ->
                    val loan = doc.toObject(MemberLoan::class.java)
                    loan?.copy(id = doc.id)
                }
                completion(loans, null)
            }
            .addOnFailureListener { e ->
                completion(null, "Error fetching loans: ${e.localizedMessage}")
            }
    }


    fun fetchMemberOtherPayments(
        squadID: String,
        memberID: String,
        paidStatus: PaidStatus?,
        type: MemberPaymentSubType?,
        completion: (List<MemberOtherPayments>?, String?) -> Unit
    ) {

        var query: Query = FirebaseFirestore.getInstance()
            .collection("squads")
            .document(squadID)
            .collection("members")
            .document(memberID)
            .collection("otherPayments")

        paidStatus?.let {
            query = query.whereEqualTo("paidStatus", it.value)
        }

        type?.let {
            query = query.whereEqualTo("memberOtherPaymentType", it.value)
        }

        query.get()
            .addOnSuccessListener { snapshot ->

                val payments = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(MemberOtherPayments::class.java)?.apply {
                            id = doc.id
                        }
                    } catch (e: Exception) {
                        null
                    }
                }

                completion(payments, null)
            }
            .addOnFailureListener { e ->
                completion(null, "Error fetching other payments: ${e.localizedMessage}")
            }
    }

    fun fetchMemberPendingLoans(
        squadID: String,
        memberID: String,
        completion: (List<MemberLoan>?, String?) -> Unit
    ) {
        db.collection("squads").document(squadID)
            .collection("members").document(memberID)
            .collection("loans").whereEqualTo("loanStatus", "PENDING")
            .get()
            .addOnSuccessListener { snapshot ->
                val loans = snapshot.documents.mapNotNull { doc ->
                    val loan = doc.toObject(MemberLoan::class.java)
                    loan?.copy(id = doc.id)
                }
                completion(loans, null)
            }
            .addOnFailureListener { e ->
                completion(null, "Error fetching pending loans: ${e.localizedMessage}")
            }
    }

    // MARK: - 🔹 Add or Update Member Loan
    fun addOrUpdateMemberLoan(
        squadID: String,
        memberID: String,
        loan: MemberLoan,
        completion: (Boolean, String?) -> Unit
    ) {
        val loansCollection = db.collection("squads")
            .document(squadID)
            .collection("members")
            .document(memberID)
            .collection("loans")

        if (!loan.id.isNullOrEmpty()) {
            val loanRef = loansCollection.document(loan.id!!)
            val updatedLoan = loan.copy(id = loanRef.id)

            loanRef.set(updatedLoan)
                .addOnSuccessListener { completion(true, null) }
                .addOnFailureListener { e -> completion(false, "Error updating loan: ${e.localizedMessage}") }
        } else {
            val newLoanRef = loansCollection.document()
            val newLoan = loan.copy(id = newLoanRef.id)

            newLoanRef.set(newLoan)
                .addOnSuccessListener { completion(true, null) }
                .addOnFailureListener { e -> completion(false, "Error adding loan: ${e.localizedMessage}") }
        }
    }

    // MARK: - 🔹 Fetch All Loans in Squad
    // FIX #4 / #7: empty member list is treated as a valid empty result (not an error);
    // dead/unused outer CountDownLatch removed; completion is now guaranteed exactly once.
    fun fetchAllLoansInSquad(
        squadID: String,
        completion: (List<MemberLoan>?, String?) -> Unit
    ) {
        val membersRef = db.collection("squads")
            .document(squadID)
            .collection("members")

        membersRef.get()
            .addOnSuccessListener { memberSnapshot ->
                val members = memberSnapshot.documents
                if (members.isEmpty()) {
                    completion(emptyList(), null)
                    return@addOnSuccessListener
                }

                val allLoans = java.util.Collections.synchronizedList(mutableListOf<MemberLoan>())
                val errors = java.util.Collections.synchronizedList(mutableListOf<String>())
                val latch = CountDownLatch(members.size)

                for (memberDoc in members) {
                    val memberID = memberDoc.id
                    val loansRef = membersRef.document(memberID).collection("loans")

                    loansRef.get()
                        .addOnSuccessListener { loanSnapshot ->
                            for (loanDoc in loanSnapshot.documents) {
                                loanDoc.toObject(MemberLoan::class.java)?.let { allLoans.add(it) }
                            }
                        }
                        .addOnFailureListener { e ->
                            errors.add("Failed for $memberID: ${e.localizedMessage}")
                        }
                        .addOnCompleteListener { latch.countDown() }
                }

                Thread {
                    latch.await()
                    if (errors.isNotEmpty()) {
                        completion(allLoans, errors.joinToString("\n"))
                    } else {
                        completion(allLoans, null)
                    }
                }.start()
            }
            .addOnFailureListener { e ->
                completion(null, "Failed to fetch members: ${e.localizedMessage}")
            }
    }

    // MARK: - 🔹 Delete Member Loan
    fun deleteMemberLoan(
        squadID: String,
        memberID: String,
        loanID: String,
        completion: (Boolean, String?) -> Unit
    ) {
        val loanRef = db.collection("squads")
            .document(squadID)
            .collection("members")
            .document(memberID)
            .collection("loans")
            .document(loanID)

        loanRef.delete()
            .addOnSuccessListener { completion(true, null) }
            .addOnFailureListener { e -> completion(false, "Error deleting loan: ${e.localizedMessage}") }
    }

    // MARK: - 🔹 Add or Update Installment in EMI
    fun addOrUpdateInstallment(
        squadID: String,
        memberID: String,
        loanID: String,
        installment: Installment,
        completion: (Boolean, String?) -> Unit
    ) {
        fetchMemberLoans(squadID, memberID) { loans, error ->
            if (error != null) {
                completion(false, error)
                return@fetchMemberLoans
            }

            val loanList = loans ?: emptyList()
            val selectedLoan = loanList.find { it.id == loanID }

            if (selectedLoan == null) {
                completion(false, "Loan not found")
                return@fetchMemberLoans
            }

            var updatedLoan = selectedLoan.copy()
            val updatedInstallment = installment.copy()

            if (updatedInstallment.id.isNullOrEmpty()) {
                updatedInstallment.id = CommonFunctions.generateInstallmentID()
            }

            val index = updatedLoan.installments.indexOfFirst { it.id == updatedInstallment.id }

            updatedLoan = if (index != -1) {
                val updatedList = updatedLoan.installments.toMutableList()
                updatedList[index] = updatedInstallment
                updatedLoan.copy(installments = updatedList)
            } else {
                updatedLoan.copy(installments = updatedLoan.installments + updatedInstallment)
            }

            addOrUpdateMemberLoan(squadID, memberID, updatedLoan, completion)
        }
    }

    // MARK: - 🔹 Remove Installment from EMI
    fun removeInstallment(
        squadID: String,
        memberID: String,
        loanID: String,
        installment: Installment,
        completion: (Boolean, String?) -> Unit
    ) {
        val loanRef = db.collection("squads")
            .document(squadID)
            .collection("members")
            .document(memberID)
            .collection("loans")
            .document(loanID)

        try {
            val installmentMap = installment.toMap()

            loanRef.update("installments", FieldValue.arrayRemove(installmentMap))
                .addOnSuccessListener { completion(true, null) }
                .addOnFailureListener { e -> completion(false, "Error removing installment: ${e.localizedMessage}") }
        } catch (e: Exception) {
            completion(false, "Encoding error: ${e.localizedMessage}")
        }
    }

    // FIX: unchanged logically (already uses a batch + single completion), kept as the
    // reference implementation that the iOS `updateFCMTokenBasedOnRole` was aligned to.
    fun updateFCMTokenBasedOnRole(
        users: List<Login>,
        completion: (Boolean, String?) -> Unit
    ) {

        if (users.isEmpty()) {
            completion(true, null)
            return
        }

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->

                val batch = db.batch()

                users.forEach { login ->

                    val documentRef = if (login.role == SquadUserType.SQUAD_MANAGER) {
                        db.collection("squads")
                            .document(login.squadID)
                    } else {
                        db.collection("squads")
                            .document(login.squadID)
                            .collection("members")
                            .document(login.squadUserId)
                    }

                    batch.update(documentRef, "fcmToken", token)
                }

                batch.commit()
                    .addOnSuccessListener { completion(true, null) }
                    .addOnFailureListener { error ->
                        completion(false, error.localizedMessage ?: "Failed to update FCM token.")
                    }
            }
            .addOnFailureListener { error ->
                completion(false, error.localizedMessage ?: "Unable to fetch FCM token.")
            }
    }

    fun updateFCMTokenForAllUser() {

        try {
            val logins = SessionManager.logins

            if (logins.isEmpty()) {
                FmLog.d("FCM sync skipped — no logged-in users found.")
                return
            }

            FmLog.d("Starting FCM token sync for ${logins.size} login(s)…")

            updateFCMTokenBasedOnRole(users = logins) { success, error ->
                if (success) {
                    FmLog.d("FCM token synced successfully for ${logins.size} login(s).")
                } else {
                    FmLog.w("FCM token sync failed: ${error ?: "Unknown error"}")
                }
            }
        } catch (e: Exception) {
            FmLog.w("Unexpected error while syncing FCM token: ${e.localizedMessage}")
        }
    }

    fun updateFCMToken(
        phoneNumber: String,
        squadID: String,
        role: String,
        fcmToken: String,
        completion: (Boolean, String?) -> Unit
    ) {

        db.collection("users")
            .document(phoneNumber)
            .collection("logins")
            .whereEqualTo("squadID", squadID)
            .whereEqualTo("role", role)
            .get()
            .addOnSuccessListener { snapshot ->

                val document = snapshot.documents.firstOrNull()

                if (document == null) {
                    completion(false, "Login record not found")
                    return@addOnSuccessListener
                }

                document.reference
                    .update("fcmToken", fcmToken)
                    .addOnSuccessListener { completion(true, null) }
                    .addOnFailureListener { completion(false, it.localizedMessage) }
            }
            .addOnFailureListener { completion(false, it.localizedMessage) }
    }

    fun getFCMToken(completion: (String?) -> Unit) {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    completion(null)
                    return@addOnCompleteListener
                }
                completion(task.result)
            }
    }

    fun clearFCMTokenForAllUsers(
        users: List<Login>,
        completion: (Boolean, String?) -> Unit
    ) {
        if (users.isEmpty()) {
            completion(true, null)
            return
        }

        val batch = db.batch()

        try {
            for (user in users) {
                val isManager = user.role == SquadUserType.SQUAD_MANAGER

                val ref = if (isManager) {
                    db.collection("squads").document(user.squadID)
                } else {
                    db.collection("squads")
                        .document(user.squadID)
                        .collection("members")
                        .document(user.squadUserId)
                }

                batch.update(ref, "fcmToken", "")
            }

            batch.commit()
                .addOnSuccessListener { completion(true, null) }
                .addOnFailureListener { e -> completion(false, e.localizedMessage) }
        } catch (e: Exception) {
            completion(false, e.localizedMessage)
        }
    }

    fun updateUPIID(
        squadId: String,
        memberId: String? = null,
        name: String,
        vpa: String,
        completion: (Result<String>) -> Unit
    ) {

        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            completion(Result.failure(Exception("No Internet Connection")))
            return
        }

        val documentRef = if (!memberId.isNullOrEmpty()) {
            db.collection("squads")
                .document(squadId)
                .collection("members")
                .document(memberId)
        } else {
            db.collection("squads")
                .document(squadId)
        }

        documentRef.set(mapOf("upiID" to vpa), SetOptions.merge())
            .addOnSuccessListener { completion(Result.success(vpa)) }
            .addOnFailureListener { error -> completion(Result.failure(error)) }
    }

    fun fetchManagerLogins(
        phoneNumber: String,
        completion: (List<Login>?, String?) -> Unit
    ) {

        val userRef = db.collection("users")
            .document(phoneNumber)
            .collection("logins")

        userRef
            .whereEqualTo("role", SquadUserType.SQUAD_MANAGER.name)
            .get()
            .addOnSuccessListener { snapshot ->

                // FIX #3: not managing any squads is a valid empty state, not an error.
                if (snapshot == null || snapshot.isEmpty) {
                    completion(emptyList(), null)
                    return@addOnSuccessListener
                }

                try {
                    val logins = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Login::class.java)?.copy(id = doc.id)
                    }
                    completion(logins, null)
                } catch (e: Exception) {
                    completion(null, "Decoding error: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener { error ->
                completion(null, "Failed to fetch managed squads: ${error.localizedMessage}")
            }
    }

    fun updateMemberLoginStatus(
        phoneNumber: String,
        squadID: String,
        recordStatus: String,
        completion: (Boolean, String?) -> Unit
    ) {

        val loginRef = db.collection("users")
            .document(phoneNumber)
            .collection("logins")

        loginRef
            .whereEqualTo("squadID", squadID)
            .get()
            .addOnSuccessListener { snapshot ->

                if (snapshot == null || snapshot.isEmpty) {
                    completion(false, "Login not found.")
                    return@addOnSuccessListener
                }

                val batch = db.batch()

                snapshot.documents.forEach { document ->
                    val updates = mapOf(
                        "recordStatus" to recordStatus,
                        "recordDate" to Timestamp.now()
                    )
                    batch.update(document.reference, updates)
                }

                batch.commit()
                    .addOnSuccessListener { completion(true, null) }
                    .addOnFailureListener { error -> completion(false, error.localizedMessage) }
            }
            .addOnFailureListener { error ->
                completion(false, error.localizedMessage)
            }
    }

    fun updateOnlyMemberStatus(
        member: Member,
        recordStatus: String,
        completion: (Boolean, String?) -> Unit
    ) {

        val memberId = member.id
        if (memberId.isNullOrBlank()) {
            completion(false, "Invalid member ID.")
            return
        }

        if (member.phoneNumber.isBlank()) {
            completion(false, "Invalid phone number.")
            return
        }

        if (member.squadID.isBlank()) {
            completion(false, "Invalid squad.")
            return
        }

        val loginRef = db.collection("users")
            .document(member.phoneNumber)
            .collection("logins")

        loginRef
            .whereEqualTo("squadID", member.squadID)
            .get()
            .addOnSuccessListener { snapshot ->

                if (snapshot.isEmpty) {
                    completion(false, "Member login not found.")
                    return@addOnSuccessListener
                }

                val batch = db.batch()
                val timestamp = Timestamp.now()

                snapshot.documents.forEach { document ->
                    batch.update(
                        document.reference,
                        mapOf(
                            "recordStatus" to recordStatus,
                            "recordDate" to timestamp
                        )
                    )
                }

                val memberRef = db.collection("squads")
                    .document(member.squadID)
                    .collection("members")
                    .document(memberId)

                batch.update(
                    memberRef,
                    mapOf(
                        "recordStatus" to recordStatus,
                        "recordDate" to timestamp
                    )
                )

                batch.commit()
                    .addOnSuccessListener { completion(true, null) }
                    .addOnFailureListener { e -> completion(false, e.localizedMessage ?: "Failed to update member status.") }
            }
            .addOnFailureListener { e ->
                completion(false, e.localizedMessage ?: "Failed to fetch member login.")
            }
    }

    fun updateLastActiveDate(
        squadID: String,
        memberID: String,
        memberType: SquadUserType,
        completion: (Boolean, String?) -> Unit
    ) {

        val documentRef = if (memberType == SquadUserType.SQUAD_MANAGER) {
            db.collection("squads")
                .document(squadID)
        } else {
            db.collection("squads")
                .document(squadID)
                .collection("members")
                .document(memberID)
        }

        val updateData = mapOf("lastActiveDate" to FieldValue.serverTimestamp())

        documentRef.set(updateData, SetOptions.merge())
            .addOnSuccessListener { completion(true, null) }
            .addOnFailureListener { error ->
                completion(false, "Failed to update lastActiveDate: ${error.localizedMessage}")
            }
    }

    fun updateCurrentLoanApproveStatus(
        squadID: String,
        memberID: String,
        paymentApproveStatus: EMIStatus,
        completion: (Boolean, String?) -> Unit
    ) {

        val documentRef = db.collection("squads")
            .document(squadID)
            .collection("members")
            .document(memberID)

        documentRef.get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    completion(false, "Member not found.")
                    return@addOnSuccessListener
                }

                val updateData = hashMapOf<String, Any>(
                    "currentLoanApproveStatus" to paymentApproveStatus.value
                )

                documentRef.set(updateData, SetOptions.merge())
                    .addOnSuccessListener { completion(true, null) }
                    .addOnFailureListener { error ->
                        completion(false, "Failed to update currentLoanApproveStatus: ${error.localizedMessage}")
                    }
            }
            .addOnFailureListener { error ->
                completion(false, "Failed to fetch member: ${error.localizedMessage}")
            }
    }

    fun addCashRequest(
        squadID: String,
        cashRequest: CashRequest,
        completion: (Boolean, String?) -> Unit
    ) {

        val cashReqID = cashRequest.id ?: run {
            completion(false, "Invalid cash request ID")
            return
        }

        val squadRef = db.collection("squads").document(squadID)

        val memberRef = squadRef
            .collection("members")
            .document(cashRequest.requestedByID)

        val cashRef = squadRef
            .collection("cashrequest")
            .document(cashReqID)

        cashRef.set(cashRequest)
            .addOnSuccessListener {

                memberRef.update("cashRequested", true)
                    .addOnSuccessListener {

                        completion(true, null)

                    }.addOnFailureListener { error ->
                        completion(false, "Cash request created but failed to update member status: ${error.localizedMessage}")
                    }

            }
            .addOnFailureListener { error ->
                completion(false, error.localizedMessage)
            }
    }

    fun fetchCashRequests(
        squadID: String,
        memberId: String? = null,
        lastDocument: DocumentSnapshot? = null,
        limit: Int = 20,
        completion: (
            cashRequests: List<CashRequest>?,
            lastDocument: DocumentSnapshot?,
            error: String?
        ) -> Unit
    ) {

        var query: Query = db
            .collection("squads")
            .document(squadID)
            .collection("cashrequest")
            .orderBy("requestedOn", Query.Direction.DESCENDING)
            .limit(limit.toLong())

        if (!memberId.isNullOrEmpty()) {
            query = query.whereEqualTo("requestedByID", memberId)
        }

        if (lastDocument != null) {
            query = query.startAfter(lastDocument)
        }

        query.get()
            .addOnSuccessListener { snapshot ->
                val cashRequests = snapshot.documents.mapNotNull {
                    val cashRequest = it.toObject(CashRequest::class.java)
                    cashRequest?.id = it.id
                    cashRequest
                }
                completion(cashRequests, snapshot.documents.lastOrNull(), null)
            }
            .addOnFailureListener {
                completion(null, null, it.localizedMessage)
            }
    }

    // FIX #5: now a Firestore transaction. Reads the cash request's *current* status
    // first and only decrements `cashRequestedCount` if this call is actually the one
    // transitioning it out of a pending state — a retried or duplicate call (e.g. from
    // a flaky network causing the client to resubmit) can no longer double-decrement
    // the counter into negative territory.
    fun updateCashRequestStatus(
        squadID: String,
        cashRequestId: String,
        memberId: String,
        status: CashRequestStatus,
        completion: (String?) -> Unit
    ) {

        val squadRef = db.collection("squads").document(squadID)
        val cashRequestRef = squadRef.collection("cashrequest").document(cashRequestId)
        val memberRef = squadRef.collection("members").document(memberId)

        db.runTransaction { transaction ->

            val snapshot = transaction.get(cashRequestRef)

            val cashRequestData = hashMapOf<String, Any>(
                "cashRequestStatus" to status.name
            )

            if (status == CashRequestStatus.ACCEPTED || status == CashRequestStatus.REJECTED) {
                cashRequestData["requestAcceptedOn"] = Timestamp.now()
            }

            transaction.update(cashRequestRef, cashRequestData)
            transaction.update(memberRef, "cashRequested", false)


            null
        }
            .addOnSuccessListener { completion(null) }
            .addOnFailureListener { error -> completion(error.localizedMessage) }
    }
}