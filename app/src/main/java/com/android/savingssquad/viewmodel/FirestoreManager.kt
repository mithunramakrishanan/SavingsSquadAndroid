package com.android.savingssquad.viewmodel

import android.util.Log
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
import com.android.savingssquad.model.PayoutStatus
import com.android.savingssquad.singleton.PaidStatus
import com.android.savingssquad.singleton.PaymentEntryType
import com.android.savingssquad.singleton.asTimestamp
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.firestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CountDownLatch

class FirestoreManager private constructor() {

    companion object {
        val shared: FirestoreManager by lazy { FirestoreManager() } // 🔥 Singleton
    }

    private val db: FirebaseFirestore = Firebase.firestore // Firestore reference

    // MARK: - 🔹 Save User Login Details
    fun addUserLogin(login: Login, completion: (Boolean, String?) -> Unit) {
        val userRef = db.collection("users").document(login.phoneNumber.toString())
        val loginRef = userRef.collection("logins").document() // Create new document

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
    fun fetchUserLogins(phoneNumber: String, completion: (List<Login>?, String?) -> Unit) {
        val userRef = db.collection("users").document(phoneNumber)

        userRef.collection("logins")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot == null || snapshot.isEmpty) {
                    completion(null, "No login records found for this user.")
                    return@addOnSuccessListener
                }

                try {
                    val logins = snapshot.documents.mapNotNull { doc ->
                        val login = doc.toObject(Login::class.java)
                        login?.copy(id = doc.id)
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
                if (snapshot == null || snapshot.isEmpty) {
                    completion(null, "No squads found.")
                    return@addOnSuccessListener
                }
                try {
                    val squads = snapshot.documents.mapNotNull { doc ->
                        val gf = doc.toObject(Squad::class.java)
                        gf?.copy(id = doc.id)
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
            completion(false, "❌ Payment ID is missing.")
            return
        }

        val ref = db.collection("squads").document(squadID)
            .collection("payments").document(paymentID)

        try {
            ref.set(payment.toMap(), SetOptions.merge())
                .addOnSuccessListener { completion(true, null) }
                .addOnFailureListener { e -> completion(false, "❌ Failed to save payment: ${e.localizedMessage}") }
        } catch (e: Exception) {
            completion(false, "❌ Encoding error: ${e.localizedMessage}")
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
            .addOnFailureListener { e -> completion(false, "❌ Failed to update payment: ${e.localizedMessage}") }
    }

    // MARK: - 🔹 Update Member UPI BeneId
    fun updateMemberUPIBeneId(squadID: String, memberID: String, upiID: String, completion: (Boolean, String?) -> Unit) {
        val ref = db.collection("squads").document(squadID)
            .collection("members").document(memberID)

        val updateData = mapOf("upiBeneId" to upiID)

        ref.update(updateData)
            .addOnSuccessListener { completion(true, null) }
            .addOnFailureListener { e -> completion(false, "❌ Failed to updateMemberUPIBeneId: ${e.localizedMessage}") }
    }

    // MARK: - 🔹 Update Member Bank BeneID
    fun updateMemberBankBeneID(squadID: String, memberID: String, bankID: String, completion: (Boolean, String?) -> Unit) {
        val ref = db.collection("squads").document(squadID)
            .collection("members").document(memberID)

        val updateData = mapOf("bankBeneId" to bankID)

        ref.update(updateData)
            .addOnSuccessListener { completion(true, null) }
            .addOnFailureListener { e -> completion(false, "❌ Failed to updateMemberBankBeneID: ${e.localizedMessage}") }
    }

    // MARK: - 🔹 Update Member Mobile Number
    fun updateMemberMobileNumber(squadID: String, memberID: String, mobileNumber: String, completion: (Boolean, String?) -> Unit) {
        val ref = db.collection("squads").document(squadID)
            .collection("members").document(memberID)

        val updateData = mapOf("phoneNumber" to mobileNumber)

        ref.update(updateData)
            .addOnSuccessListener { completion(true, null) }
            .addOnFailureListener { e -> completion(false, "❌ Failed to updateMemberMobileNumber: ${e.localizedMessage}") }
    }

    // MARK: - 🔹 Update Contribution Status
    fun updateContributionStatus(
        squadID: String,
        memberID: String,
        contributionID: String,
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
            "paidOn" to FieldValue.serverTimestamp(),
            "paidStatus" to newStatus
        )

        contributionRef.update(updateData)
            .addOnSuccessListener {
                completion(true, "✅ Contribution status updated to $newStatus")
            }
            .addOnFailureListener { e ->
                completion(false, "❌ Failed to update status: ${e.localizedMessage}")
            }
    }

    // MARK: - 🔹 Update Loan Status
    fun updateLoanStatus(
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

        val updateData = mapOf(
            "duePaidDate" to FieldValue.serverTimestamp(),
            "loanStatus" to status
        )

        loanRef.update(updateData)
            .addOnSuccessListener {
                completion(true, "✅ Loan status updated successfully")
            }
            .addOnFailureListener { e ->
                completion(false, "❌ Failed to update loan status: ${e.localizedMessage}")
            }
    }

    // MARK: - 🔹 Update Installment Status
    fun updateInstallmentStatus(
        squadID: String,
        memberID: String,
        loanID: String,
        installmentID: String,
        status: String,
        completion: (Boolean, String?) -> Unit
    ) {
        val installmentRef = db.collection("squads")
            .document(squadID)
            .collection("members")
            .document(memberID)
            .collection("loans")
            .document(loanID)
            .collection("installments")
            .document(installmentID)

        val updateData = mapOf(
            "amountReceivedDate" to FieldValue.serverTimestamp(),
            "loanStatus" to status
        )

        installmentRef.update(updateData)
            .addOnSuccessListener {
                completion(true, "✅ Installment status updated successfully")
            }
            .addOnFailureListener { e ->
                completion(false, "❌ Failed to update installment status: ${e.localizedMessage}")
            }
    }

    // MARK: - 🔹 Save Multiple Payments (Batch)
    fun savePayments(
        squadID: String,
        payments: List<PaymentsDetails>,
        completion: (Boolean, String?) -> Unit
    ) {
        val batch = db.batch()

        for (payment in payments) {
            val paymentID = payment.id
            if (paymentID == null) {
                completion(false, "❌ One or more payments are missing an ID.")
                return
            }

            val docRef = db.collection("squads")
                .document(squadID)
                .collection("payments")
                .document(paymentID)

            try {
                batch.set(docRef, payment.toMap(), SetOptions.merge())
            } catch (e: Exception) {
                completion(false, "❌ Error encoding payment: ${e.localizedMessage}")
                return
            }
        }

        batch.commit()
            .addOnSuccessListener {
                completion(true, null)
            }
            .addOnFailureListener { e ->
                completion(false, "❌ Error saving batch payments: ${e.localizedMessage}")
            }
    }

    // MARK: - 🔹 Fetch Payments
    fun fetchPayments(
        squadID: String,
        completion: (List<PaymentsDetails>?, String?) -> Unit
    ) {
        val paymentsRef = db.collection("squads")
            .document(squadID)
            .collection("payments")
            .orderBy("recordDate", Query.Direction.DESCENDING)

        paymentsRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot == null || snapshot.isEmpty) {
                    completion(null, "❌  records found.")
                    return@addOnSuccessListener
                }

                val payments = snapshot.documents.mapNotNull { doc ->
                    try {
                        // Try normal Firestore decoding
                        val payment = doc.toObject(PaymentsDetails::class.java)

                        // Extra safety: ensure payoutStatus is valid (handles cases like PAYOUT_SUCCESS)
                        payment?.payoutStatus = PayoutStatus.fromValue(payment?.payoutStatus?.value)

                        payment
                    } catch (e: Exception) {
                        println("❌ Decoding error for ${doc.id}: ${e.localizedMessage}")

                        // Try fallback manual mapping if enum decoding fails
                        try {
                            val rawMap = doc.data ?: return@mapNotNull null
                            val payoutStatusStr = rawMap["payoutStatus"] as? String
                            val payment = doc.toObject(PaymentsDetails::class.java)
                            payment?.payoutStatus = PayoutStatus.fromValue(payoutStatusStr)
                            payment
                        } catch (inner: Exception) {
                            println("⚠️ Fallback mapping failed for ${doc.id}: ${inner.localizedMessage}")
                            null
                        }
                    }
                }

                completion(payments, null)
            }
            .addOnFailureListener { e ->
                completion(null, "❌ Error fetching payments: ${e.localizedMessage}")
            }
    }
    // MARK: - 🔹 Observe Payments (Realtime)
    fun observePayments(squadID: String, completion: (List<PaymentsDetails>?, String?) -> Unit) {
        val paymentsRef = db.collection("squads")
            .document(squadID)
            .collection("payments")

        paymentsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                completion(null, "Error observing payments: ${error.localizedMessage}")
                return@addSnapshotListener
            }

            if (snapshot == null || snapshot.isEmpty) {
                completion(null, "No payments found")
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

        val memberID = member.id
        if (memberID == null) {
            completion(false, "Error adding member: Member ID is null")
            return
        }

        try {
            membersRef.document(memberID).set(member)
                .addOnSuccessListener {
                    completion(true, null)
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
                            Log.d("Firestore", "✅ Member fetched: ${member.name}")
                        } else {
                            completion(null, "❌ Failed to decode member object.")
                        }
                    } catch (e: Exception) {
                        Log.e("Firestore", "❌ Decoding error", e)
                        completion(null, "❌ Decoding error: ${e.localizedMessage}")
                    }
                } else {
                    Log.w("Firestore", "⚠️ Member not found in squad: $squadID")
                    completion(null, "❌ Member not found.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "❌ Error fetching member", e)
                completion(null, "❌ Error fetching member: ${e.localizedMessage}")
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
                if (snapshot == null || snapshot.isEmpty) {
                    completion(null, "No members found.")
                    return@addOnSuccessListener
                }

                try {
                    val rawData = snapshot.documents.map { it.data }
                    println("📌 Firestore raw data: $rawData")

                    val members = snapshot.documents.mapNotNull { doc ->
                        try {
                            val member = doc.toObject(Member::class.java)
                            member?.id = doc.id
                            member
                        } catch (e: Exception) {
                            println("❌ Error decoding member: ${e.localizedMessage}")
                            null
                        }
                    }

                    completion(members, null)
                } catch (e: Exception) {
                    println("❌ Error decoding member: ${e.localizedMessage}")
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
        val updateErrors = mutableListOf<String>()
        val latch = CountDownLatch(members.size)

        for (member in members) {
            val memberID = member.id
            if (memberID == null) {
                updateErrors.add("❌ Member ID missing for ${member.name}")
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
                        updateErrors.add("❌ Error updating ${member.name}: ${e.localizedMessage}")
                        latch.countDown()
                    }
                    .addOnSuccessListener {
                        latch.countDown()
                    }
            } catch (e: Exception) {
                updateErrors.add("❌ Error encoding ${member.name}: ${e.localizedMessage}")
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
            .addOnSuccessListener {
                completion(true, null)
            }
            .addOnFailureListener { e ->
                completion(false, "Error deleting member: ${e.localizedMessage}")
            }
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
            val contributionID =
                CommonFunctions.generateContributionID(memberID, monthYear)

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
                    completion(true, "✅ Contributions created successfully")
                }
                .addOnFailureListener { e ->
                    completion(false, "❌ Batch write failed: ${e.localizedMessage}")
                }
        } catch (e: Exception) {
            completion(false, "❌ Encoding error: ${e.localizedMessage}")
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
                    completion(false, "❌ No members found.")
                    return@addOnSuccessListener
                }

                var processedCount = 0
                var encounteredError: String? = null

                for (memberDoc in memberDocs) {
                    val memberID = memberDoc.id
                    val member = memberDoc.toObject(Member::class.java)
                    if (member == null) {
                        processedCount++
                        if (processedCount == memberDocs.size) {
                            completion(false, "❌ Failed to decode member: $memberID")
                        }
                        continue
                    }

                    val contributionsRef = membersRef.document(memberID).collection("contributions")
                    contributionsRef.get()
                        .addOnSuccessListener { contribSnapshot ->
                            val contribDocs = contribSnapshot.documents
                            val existingMonths =
                                contribDocs.mapNotNull { it.getString("monthYear") }.toSet()

                            val toDelete = contribDocs.filter {
                                val month = it.getString("monthYear") ?: return@filter false
                                !validMonths.contains(month)
                            }

                            val missingMonths = validMonths.subtract(existingMonths)
                            val batch = db.batch()

                            // Delete outdated
                            toDelete.forEach { batch.delete(it.reference) }

                            // Add missing
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
                                val newDoc =
                                    contributionsRef.document(newContribution.id ?: UUID.randomUUID().toString())
                                batch.set(newDoc, newContribution)
                            }

                            batch.commit()
                                .addOnSuccessListener {
                                    processedCount++
                                    if (processedCount == memberDocs.size) {
                                        if (encounteredError != null)
                                            completion(false, encounteredError)
                                        else
                                            completion(
                                                true,
                                                "✅ Successfully updated all member contributions."
                                            )
                                    }
                                }
                                .addOnFailureListener { e ->
                                    encounteredError =
                                        "❌ Failed to update $memberID: ${e.localizedMessage}"
                                    processedCount++
                                    if (processedCount == memberDocs.size) {
                                        completion(false, encounteredError)
                                    }
                                }
                        }
                        .addOnFailureListener { e ->
                            encounteredError =
                                "❌ Failed to fetch contributions for $memberID: ${e.localizedMessage}"
                            processedCount++
                            if (processedCount == memberDocs.size) {
                                completion(false, encounteredError)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                completion(false, "❌ Failed to fetch members: ${e.localizedMessage}")
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
                completion(true, "✅ Contribution added successfully")
            }
            .addOnFailureListener { e ->
                completion(false, "❌ Error adding contribution: ${e.localizedMessage}")
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
                    completion(null, "❌ Decoding error: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener { e ->
                completion(null, "❌ Error fetching contributions: ${e.localizedMessage}")
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

                var totalAmount = 0
                var processedCount = 0
                var errorMessage: String? = null

                for (doc in memberDocs) {
                    val memberID = doc.id
                    val contributionsRef =
                        membersRef.document(memberID).collection("contributions")

                    contributionsRef.get()
                        .addOnSuccessListener { contribSnapshot ->
                            val sum = contribSnapshot.documents.sumOf {
                                (it.getLong("amount") ?: 0L).toInt()
                            }
                            totalAmount += sum
                            processedCount++
                            if (processedCount == memberDocs.size) {
                                if (errorMessage != null) completion(null, errorMessage)
                                else completion(totalAmount, null)
                            }
                        }
                        .addOnFailureListener { e ->
                            errorMessage =
                                "❌ Error fetching contributions for $memberID: ${e.localizedMessage}"
                            processedCount++
                            if (processedCount == memberDocs.size) {
                                completion(null, errorMessage)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                completion(null, "❌ Error fetching members: ${e.localizedMessage}")
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
                completion(true, "✅ Contribution updated successfully")
            }
            .addOnFailureListener { e ->
                completion(false, "❌ Failed to update contribution: ${e.localizedMessage}")
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
            .addOnSuccessListener {
                completion(true, null)
            }
            .addOnFailureListener { e ->
                completion(false, "Error adding EMI configuration: ${e.localizedMessage}")
            }
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
            .addOnSuccessListener {
                completion(true, null)
            }
            .addOnFailureListener { e ->
                completion(false, "Error adding or updating EMI configuration: ${e.localizedMessage}")
            }
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
                    completion(null, "❌ Decoding error: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener { e ->
                completion(null, "❌ Error fetching EMI configurations: ${e.localizedMessage}")
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
                    completion(null, "❌ EMI configuration not found.")
                    return@addOnSuccessListener
                }

                try {
                    val emiConfig = document.toObject(EMIConfiguration::class.java)
                        ?.copy(id = document.id)
                    completion(emiConfig, null)
                } catch (e: Exception) {
                    completion(null, "❌ Decoding error: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener { e ->
                completion(null, "❌ Error fetching EMI configuration: ${e.localizedMessage}")
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
            .addOnSuccessListener {
                completion(true, null)
            }
            .addOnFailureListener { e ->
                completion(false, "Error updating EMI configuration: ${e.localizedMessage}")
            }
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
            .addOnSuccessListener {
                completion(true, null)
            }
            .addOnFailureListener { e ->
                completion(false, "Error deleting EMI configuration: ${e.localizedMessage}")
            }
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
            .addOnSuccessListener {
                completion(true, null)
            }
            .addOnFailureListener { e ->
                completion(false, "Error adding Squad Activity: ${e.localizedMessage}")
            }
    }

    // MARK: - Fetch All Squad Activities
    fun fetchSquadActivities(
        squadID: String,
        completion: (List<SquadActivity>?, String?) -> Unit
    ) {
        val activitiesRef = db.collection("squads")
            .document(squadID)
            .collection("activities")
            .orderBy("recordDate", Query.Direction.DESCENDING)

        activitiesRef.get()
            .addOnSuccessListener { snapshot ->
                val documents = snapshot.documents
                if (documents.isEmpty()) {
                    completion(emptyList(), null)
                    return@addOnSuccessListener
                }

                try {
                    val activities = documents.mapNotNull { doc ->
                        val activity = doc.toObject(SquadActivity::class.java)
                        activity?.copy(id = doc.id)
                    }
                    completion(activities, null)
                } catch (e: Exception) {
                    completion(null, "❌ Decoding error: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener { e ->
                completion(null, "❌ Error fetching activities: ${e.localizedMessage}")
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
                    completion(null, "❌ Activity not found.")
                    return@addOnSuccessListener
                }

                try {
                    val activity = document.toObject(SquadActivity::class.java)
                        ?.copy(id = document.id)
                    completion(activity, null)
                } catch (e: Exception) {
                    completion(null, "❌ Decoding error: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener { e ->
                completion(null, "❌ Error fetching activity: ${e.localizedMessage}")
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
            .addOnSuccessListener {
                completion(true, null)
            }
            .addOnFailureListener { e ->
                completion(false, "❌ Error updating activity: ${e.localizedMessage}")
            }
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
            .addOnSuccessListener {
                completion(true, null)
            }
            .addOnFailureListener { e ->
                completion(false, "Error deleting activity: ${e.localizedMessage}")
            }
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
            .addOnSuccessListener {
                completion(true, null)
            }
            .addOnFailureListener { e ->
                completion(false, "Error adding Squad Rule: ${e.localizedMessage}")
            }
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
                    completion(null, "❌ Decoding error: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener { e ->
                completion(null, "❌ Error fetching rules: ${e.localizedMessage}")
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
            .addOnSuccessListener {
                completion(true, null)
            }
            .addOnFailureListener { e ->
                completion(false, "❌ Error deleting rule: ${e.localizedMessage}")
            }
    }

    // MARK: - Update Squad Rule
    fun updateSquadRule(
        squadID: String,
        rule: SquadRule,
        completion: (Boolean, String?) -> Unit
    ) {
        val ruleRef = db.collection("squads")
            .document(squadID)
            .collection("rules")
            .document(rule.id ?: "")

        ruleRef.set(rule)
            .addOnSuccessListener {
                completion(true, null)
            }
            .addOnFailureListener { e ->
                completion(false, "❌ Error updating rule: ${e.localizedMessage}")
            }
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
                completion(null, "❌ Error fetching EMIs: ${e.localizedMessage}")
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
            // 🔹 Update existing loan
            val loanRef = loansCollection.document(loan.id!!)
            val updatedLoan = loan.copy(id = loanRef.id)

            loanRef.set(updatedLoan)
                .addOnSuccessListener { completion(true, null) }
                .addOnFailureListener { e ->
                    completion(false, "❌ Error updating loan: ${e.localizedMessage}")
                }

        } else {
            // 🔹 Add new loan
            val newLoanRef = loansCollection.document()
            val newLoan = loan.copy(id = newLoanRef.id)

            newLoanRef.set(newLoan)
                .addOnSuccessListener { completion(true, null) }
                .addOnFailureListener { e ->
                    completion(false, "❌ Error adding loan: ${e.localizedMessage}")
                }
        }
    }

    // MARK: - 🔹 Fetch All Loans in Squad
    fun fetchAllLoansInSquad(
        squadID: String,
        completion: (List<MemberLoan>?, String?) -> Unit
    ) {
        val membersRef = db.collection("squads")
            .document(squadID)
            .collection("members")

        val allLoans = mutableListOf<MemberLoan>()
        val errors = mutableListOf<String>()
        val dispatchGroup = java.util.concurrent.CountDownLatch(1)

        membersRef.get()
            .addOnSuccessListener { memberSnapshot ->
                val members = memberSnapshot.documents
                if (members.isEmpty()) {
                    completion(emptyList(), "❌ No members found")
                    return@addOnSuccessListener
                }

                val latch = java.util.concurrent.CountDownLatch(members.size)

                for (memberDoc in members) {
                    val memberID = memberDoc.id
                    val loansRef = membersRef.document(memberID).collection("loans")

                    loansRef.get()
                        .addOnSuccessListener { loanSnapshot ->
                            for (loanDoc in loanSnapshot.documents) {
                                loanDoc.toObject(MemberLoan::class.java)?.let {
                                    allLoans.add(it)
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            errors.add("❌ Failed for $memberID: ${e.localizedMessage}")
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
                completion(null, "❌ Failed to fetch members: ${e.localizedMessage}")
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
            .addOnFailureListener { e ->
                completion(false, "Error deleting EMI: ${e.localizedMessage}")
            }
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
                completion(false, "❌ EMI not found")
                return@fetchMemberLoans
            }

            var updatedLoan = selectedLoan.copy()
            var updatedInstallment = installment.copy()

            // 🔹 Ensure installment ID exists
            if (updatedInstallment.id.isNullOrEmpty()) {
                updatedInstallment.id = CommonFunctions.generateInstallmentID()
            }

            val index = updatedLoan.installments.indexOfFirst { it.id == updatedInstallment.id }

            if (index != -1) {
                // 🔥 Update existing installment
                val updatedList = updatedLoan.installments.toMutableList()
                updatedList[index] = updatedInstallment
                updatedLoan = updatedLoan.copy(installments = updatedList)
            } else {
                // 🔥 Append new installment
                updatedLoan = updatedLoan.copy(
                    installments = updatedLoan.installments + updatedInstallment
                )
            }

            // 🔹 Save updated EMI to Firestore
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

        val installmentMap = installment.toMap()

        loanRef.update("installments", FieldValue.arrayRemove(installmentMap))
            .addOnSuccessListener { completion(true, null) }
            .addOnFailureListener { e ->
                completion(false, "Error removing installment: ${e.localizedMessage}")
            }
    }
}