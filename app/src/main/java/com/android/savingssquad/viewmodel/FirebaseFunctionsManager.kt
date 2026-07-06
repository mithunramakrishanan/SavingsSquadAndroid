package com.android.savingssquad.viewmodel

import android.content.Context
import android.util.Log
import androidx.activity.compose.setContent
import com.android.savingssquad.model.BeneficiaryDetails
import com.android.savingssquad.model.BeneficiaryResult
import com.android.savingssquad.model.Member
import com.android.savingssquad.model.PaymentsDetails
import com.android.savingssquad.model.PayoutStatusResult
import com.android.savingssquad.singleton.BulkOrder
import com.android.savingssquad.singleton.CashfreePaymentAction
import com.android.savingssquad.singleton.JsonUtil
import com.android.savingssquad.singleton.LocalDatabase
import com.android.savingssquad.singleton.RazorpayPaymentAction
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.view.PaymentScreen
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.HttpsCallableResult
import com.yourapp.utils.CommonFunctions

import org.json.JSONObject

class FirebaseFunctionsManager private constructor() {

    companion object {
        val shared = FirebaseFunctionsManager()
    }

    private var appContext: Context? = null

    fun init(context: Context) {
        // store a safe non-null application context
        appContext = context.applicationContext
    }

    private fun requireContext(): Context {
        return appContext ?: throw IllegalStateException("❌ FirebaseFunctionsManager not initialized. Call init(context) first.")
    }

    private val functions = FirebaseFunctions.getInstance()

    fun processRazorPayPayment(
        squadId: String,
        action: RazorpayPaymentAction,
        completion: (String?, String?, Exception?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = "📴 No Internet Connection.",
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            completion(null, null, Exception("No Internet Connection"))
            return
        }

        val data = mutableMapOf<String, Any>("squadId" to squadId)
        var functionName = ""

        when (action) {

            /* ───────────── NEW PAYMENT ───────────── */
            is RazorpayPaymentAction.New -> {
                functionName = "makeRazorpayPayment"

                data.putAll(
                    mapOf(
                        "memberId" to action.payment.memberId,
                        "name" to action.payment.memberName,
                        "email" to action.payment.paymentEmail,
                        "phone" to action.payment.paymentPhone,
                        "amount" to action.payment.amount,
                        "intrestAmount" to action.payment.intrestAmount,
                        "paymentEntryType" to action.payment.paymentEntryType.value,
                        "paymentType" to action.payment.paymentType.value,
                        "paymentSubType" to action.payment.paymentSubType.value,
                        "description" to action.payment.description,
                        "contributionId" to action.payment.contributionId,
                        "loanId" to action.payment.loanId,
                        "installmentId" to action.payment.installmentId,
                        "upiID" to action.payment.upiID
                    )
                )
            }

            /* ───────────── RETRY PAYMENT ───────────── */
            is RazorpayPaymentAction.Retry -> {
                functionName = "retryRazorpayPayment"
                data["failedOrderId"] = action.failedOrderId
            }
        }

        Log.d("Razorpay Payload", data.toString())
//        functions.useEmulator("10.0.2.2", 9099)
        functions
            .getHttpsCallable(functionName)
            .call(data)
            .addOnCompleteListener { task ->

                LoaderManager.shared.hideLoader()

                if (!task.isSuccessful) {
                    val e = task.exception
                    val firebaseError = e as? FirebaseFunctionsException

                    val code = firebaseError?.code
                    val details = firebaseError?.details
                    val serverMsg = firebaseError?.message

                    Log.e(
                        "🔥 Razorpay Function Error",
                        "code=$code | message=$serverMsg | details=$details",
                        e
                    )

                    AlertManager.shared.showAlert(
                        title = SquadStrings.appName,
                        message = "⚠️ Payment initiation failed.\n$serverMsg",
                        primaryButtonTitle = SquadStrings.ok,
                        primaryAction = {}
                    )

                    completion(null, null, e)
                    return@addOnCompleteListener
                }

                val resultData = task.result?.data as? Map<*, *> ?: emptyMap<String, Any>()

                val orderId = resultData["orderId"] as? String

                if (orderId.isNullOrEmpty()) {
                    Log.e("❌ Razorpay", "Invalid server response: $resultData")

                    AlertManager.shared.showAlert(
                        title = SquadStrings.appName,
                        message = "⚠️ Invalid response from server. Please try again.",
                        primaryButtonTitle = SquadStrings.ok,
                        primaryAction = {}
                    )

                    completion(null, null, Exception("Invalid Razorpay response"))
                    return@addOnCompleteListener
                }

                Log.d(
                    "✅ Razorpay Order Created",
                    "orderId=$orderId | key="
                )

                /*
                 * SUCCESS:
                 * return orderId + key
                 * Client should now open Razorpay Checkout
                 */
                completion("", orderId, null)
            }
    }

    fun verifyBulkOrders(savedOrders: List<BulkOrder>, context: Context) {
        if (savedOrders.isEmpty()) {
            println("No orders to verify.")
            return
        }

        val ordersData = savedOrders.map {
            mapOf("orderId" to it.orderId, "squadId" to it.squadId)
        }

        functions
            .getHttpsCallable("verifyRazorpayPaymentStatusBulk")
            .call(mapOf("orders" to ordersData))
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *> ?: return@addOnSuccessListener
                val results = data["results"] as? List<Map<String, Any>> ?: emptyList()

                results.forEach { res ->
                    val orderId = res["orderId"] as? String ?: "Unknown"
                    val status = res["status"] as? String ?: "UNKNOWN"
                    val success = res["success"] as? Boolean ?: false
                    val errorMessage = res["error"] as? String

                    if (success) {
                        println("✅ Order $orderId verified successfully. Status: $status")
                    } else {
                        println("⚠️ Order $orderId failed verification. Status: $status, Error: ${errorMessage ?: "Unknown"}")
                    }
                }

                val completedIds = data["completedOrderIds"] as? List<String> ?: emptyList()
                if (completedIds.isNotEmpty()) {
                    completedIds.forEach { completedId ->
                        try {
                            Thread {
                                try {
                                    val db = LocalDatabase.getInstance(context)
                                    db.deleteOrder(completedId)
                                    Log.d("LocalDatabase", "✅ Deleted order: $completedId")
                                } catch (e: Exception) {
                                    Log.e("LocalDatabase", "❌ Failed to delete order: ${e.localizedMessage}")
                                }
                            }.start()

                        } catch (e: Exception) {
                            println("❌ Failed to delete order: ${e.localizedMessage}")
                        }
                    }
                } else {
                    println("No completed orders to delete.")
                }
            }
            .addOnFailureListener { error ->
                println("❌ Error verifying bulk orders: ${error.localizedMessage}")
            }
    }

    // MARK: - Get Beneficiary Details
    fun getBeneficiaryDetails(
        beneId: String,
        completion: (Result<BeneficiaryDetails>) -> Unit
    ) {
        // 1️⃣ Internet check
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = "📴 No Internet Connection.",
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            completion(Result.failure(Exception("No Internet Connection")))
            return
        }

        val data = mapOf("beneficiaryId" to beneId)

        // 2️⃣ Firebase callable
        functions
            .getHttpsCallable("getBeneficiaryDetails")
            .call(data)
            .addOnCompleteListener { task ->
                LoaderManager.shared.hideLoader()

                if (!task.isSuccessful) {
                    val error = task.exception
                    if (error != null) {
                        println("❌ Firebase function error details: ${error.message}")
                        completion(Result.failure(error))
                    } else {
                        completion(Result.failure(Exception("Unknown Firebase error")))
                    }
                    return@addOnCompleteListener
                }

                val response = task.result?.data as? Map<*, *>
                    ?: run {
                        completion(Result.failure(Exception("Empty or invalid response from server")))
                        return@addOnCompleteListener
                    }

                val success = response["success"] as? Boolean ?: false
                if (!success) {
                    val message = response["message"] as? String ?: "Unknown backend error"
                    completion(Result.failure(Exception(message)))
                    return@addOnCompleteListener
                }

                val detailsData = response["beneficiaryDetails"]
                if (detailsData != null) {
                    try {
                        val jsonData = JSONObject(detailsData as Map<*, *>).toString()
                        val beneficiary =
                            JsonUtil.fromJson(jsonData, BeneficiaryDetails::class.java)
                        completion(Result.success(beneficiary))
                    } catch (e: Exception) {
                        completion(Result.failure(Exception("Failed to parse beneficiary details: ${e.localizedMessage}")))
                    }
                } else {
                    completion(Result.failure(Exception("Beneficiary details missing in response")))
                }
            }
    }

    // MARK: - Manager → Member Payout (Cashfree Payouts)
    fun makeCashFreePayout(
        squadId: String,
        paymentId: String?,
        beneId: String,
        amount: Double,
        transferType: String,
        description: String?,
        memberId: String?,
        memberName: String?,
        userType: String?,
        paymentEntryType: String?,
        paymentType: String?,
        paymentSubType: String?,
        contributionId: String?,
        loanId: String?,
        installmentId: String?,
        transferMode: String?,
        completion: (Result<PayoutStatusResult>) -> Unit
    ) {
        // 1️⃣ Internet check
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = "📴 No Internet Connection.",
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            completion(Result.failure(Exception("No Internet Connection")))
            return
        }

        // 2️⃣ Prepare data dictionary
        val payoutData = mutableMapOf<String, Any>(
            "squadId" to squadId,
            "beneId" to beneId,
            "amount" to amount,
            "transferType" to transferType
        )

        Log.d("Payout started", payoutData.toString())

        // Attach optional values
        paymentId?.let { payoutData["paymentId"] = it }
        description?.let { payoutData["description"] = it }
        memberId?.let { payoutData["memberId"] = it }
        memberName?.let { payoutData["memberName"] = it }
        userType?.let { payoutData["userType"] = it }
        paymentEntryType?.let { payoutData["paymentEntryType"] = it }
        paymentType?.let { payoutData["paymentType"] = it }
        paymentSubType?.let { payoutData["paymentSubType"] = it }
        contributionId?.let { payoutData["contributionId"] = it }
        loanId?.let { payoutData["loanId"] = it }
        installmentId?.let { payoutData["installmentId"] = it }
        transferMode?.let { payoutData["transferMode"] = it }

        // 3️⃣ Call Firebase Function
        functions
            .getHttpsCallable("makeCashFreePayout")
            .call(payoutData)
            .addOnCompleteListener { task ->
                LoaderManager.shared.hideLoader()

                if (!task.isSuccessful) {
                    val error = task.exception
                    completion(Result.failure(error ?: Exception("Firebase error")))
                    return@addOnCompleteListener
                }

                val data = task.result?.data as? Map<*, *>
                    ?: run {
                        completion(Result.failure(Exception("Empty or invalid response from server")))
                        return@addOnCompleteListener
                    }

                val success = data["success"] as? Boolean ?: false
                if (!success) {
                    val message = data["message"] as? String ?: "Payout failed"
                    completion(Result.failure(Exception(message)))
                    return@addOnCompleteListener
                }

                val innerData = data["data"] as? Map<*, *>
                    ?: run {
                        completion(Result.failure(Exception("Invalid inner response from server")))
                        return@addOnCompleteListener
                    }

                try {
                    val jsonData = JSONObject(innerData).toString()
                    val decoded = JsonUtil.fromJson(jsonData, PayoutStatusResult::class.java)
                    println("📦 Payout status: ${decoded.status ?: "N/A"}")
                    println("📅 Updated on: ${decoded.updatedOn ?: "N/A"}")
                    completion(Result.success(decoded))
                } catch (e: Exception) {
                    completion(Result.failure(Exception("Failed to parse payout status: ${e.localizedMessage}")))
                }
            }
    }

    // MARK: - Check payout status
    fun verifyCashFreePayoutStatus(
        squadId: String,
        paymentId: String,
        transferId: String,
        completion: (Result<PayoutStatusResult>) -> Unit
    ) {
        // 1️⃣ Check Internet
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = "📴 No Internet Connection.",
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            completion(Result.failure(Exception("No Internet Connection")))
            return
        }

        val payoutData = mapOf(
            "squadId" to squadId,
            "paymentId" to paymentId,
            "transferId" to transferId
        )

        functions
            .getHttpsCallable("verifyCashFreePayoutStatus")
            .call(payoutData)
            .addOnCompleteListener { task ->
                LoaderManager.shared.hideLoader()

                if (!task.isSuccessful) {
                    val error = task.exception
                    completion(Result.failure(error ?: Exception("Firebase error")))
                    return@addOnCompleteListener
                }

                val data = task.result?.data as? Map<*, *>
                val innerData = data?.get("data") as? Map<*, *>
                if (innerData == null) {
                    completion(Result.failure(Exception("Empty or invalid response from server")))
                    return@addOnCompleteListener
                }

                try {
                    val jsonData = JSONObject(innerData).toString()
                    val decoded = JsonUtil.fromJson(jsonData, PayoutStatusResult::class.java)
                    println("📦 Payout status: ${decoded.status ?: "N/A"}")
                    println("📅 Updated on: ${decoded.updatedOn ?: "N/A"}")
                    completion(Result.success(decoded))
                } catch (e: Exception) {
                    completion(Result.failure(Exception("Failed to parse payout status: ${e.localizedMessage}")))
                }
            }
    }

    fun verifyAndSaveUPIBeneficiary(
        squadId: String,
        memberId: String? = null,
        name: String,
        vpa: String,
        email: String? = null,
        phone: String? = null,
        address: String? = null,
        city: String? = null,
        countryCode: String? = "+91",
        postalCode: String? = null,
        completion: (Result<BeneficiaryResult>) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = "📴 No Internet Connection.",
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            completion(Result.failure(Exception("No Internet Connection")))
            return
        }

        val payload = mapOf(
            "squadId" to squadId,
            "memberId" to memberId,
            "name" to name,
            "vpa" to vpa,
            "email" to email,
            "phone" to phone,
            "address" to address,
            "city" to city,
            "countryCode" to countryCode,
            "postalCode" to postalCode
        )

        functions
            .getHttpsCallable("verifyAndSaveUPIBeneficiary")
            .call(payload)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    val error = task.exception
                    completion(Result.failure(error ?: Exception("Firebase error")))
                    return@addOnCompleteListener
                }

                val data = task.result?.data as? Map<*, *> ?: run {
                    completion(Result.failure(Exception("Invalid response from server")))
                    return@addOnCompleteListener
                }

                val status = data["status"] as? String ?: ""
                if (status != "success") {
                    val msg = data["message"] as? String ?: "UPI verification failed"
                    completion(Result.failure(Exception(msg)))
                    return@addOnCompleteListener
                }

                val beneId = data["beneId"] as? String
                val upi = data["upi"] as? String

                if (beneId == null || upi == null) {
                    completion(Result.failure(Exception("Missing beneId/upi/verification")))
                    return@addOnCompleteListener
                }

                val result = BeneficiaryResult(beneId, upi)
                completion(Result.success(result))
            }
    }


    fun createUPIPaymentIntent(
        squadId: String,
        payment: PaymentsDetails,
        completion: (String?, String?, Exception?) -> Unit
    ) {
        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = "📴 No Internet Connection.",
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            completion(null, null, Exception("No Internet Connection"))
            return
        }

        val data = mutableMapOf<String, Any>("squadId" to squadId)
        val functionName = "createUPIPaymentIntent"

        data.putAll(
            mapOf(
                "memberId" to payment.memberId,
                "name" to payment.memberName,
                "email" to payment.paymentEmail,
                "phone" to payment.paymentPhone,
                "amount" to payment.amount,
                "intrestAmount" to payment.intrestAmount,
                "paymentEntryType" to payment.paymentEntryType.value,
                "paymentType" to payment.paymentType.value,
                "paymentSubType" to payment.paymentSubType.value,
                "description" to payment.description,
                "contributionId" to payment.contributionId,
                "loanId" to payment.loanId,
                "installmentId" to payment.installmentId
            )
        )

        Log.d("Payment Payload", "$data")

        functions
            .getHttpsCallable(functionName)
            .call(data)
            .addOnCompleteListener { task ->
                LoaderManager.shared.hideLoader()

                if (!task.isSuccessful) {
                    val e = task.exception
                    val firebaseError = e as? FirebaseFunctionsException

                    AlertManager.shared.showAlert(
                        title = SquadStrings.appName,
                        message = "⚠️ $functionName failed.\n${firebaseError?.message}",
                        primaryButtonTitle = SquadStrings.ok,
                        primaryAction = {}
                    )

                    completion(null, null, e)
                    return@addOnCompleteListener
                }

                val resultData = task.result?.data as? Map<*, *> ?: emptyMap<String, Any>()
                val orderId = resultData["orderId"] as? String
                val transferId = resultData["transferId"] as? String

                if (orderId.isNullOrEmpty()) {
                    AlertManager.shared.showAlert(
                        title = SquadStrings.appName,
                        message = "⚠️ Invalid response from server. Please try again.",
                        primaryButtonTitle = SquadStrings.ok,
                        primaryAction = {}
                    )
                    completion(null, null, Exception("Invalid response"))
                    return@addOnCompleteListener
                }

                // ✅ sessionId is NOT applicable for UPI
                completion(transferId, orderId, null)
            }
    }

    fun verifyUPIPaymentIntent(
        context: Context,
        squadId: String,
        orderId: String,
        status: String,
        amount: Int,
        completion: (String?, String?, Exception?) -> Unit
    ) {
        Log.d("Payment flow", "Verifying payment...")

        Log.d("Payment flow orderId", orderId)
        Log.d("Payment flow squadId", squadId)
        Log.d("Payment flow status", status)
        Log.d("Payment flow amount", amount.toString())

        if (!CommonFunctions.isInternetAvailable()) {
            LoaderManager.shared.hideLoader()
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = "📴 No Internet Connection.",
                primaryButtonTitle = SquadStrings.ok,
                primaryAction = {}
            )
            completion(null, null, Exception("No Internet Connection"))
            return
        }

        functions
            .getHttpsCallable("verifyUPIPaymentIntent")
            .call(
                mapOf(
                    "orderId" to orderId,
                    "squadId" to squadId,
                    "status" to status,
                    "amount" to amount
                )
            )
            .addOnSuccessListener { result ->
                Log.d("Payment flow Success", "verifyUPIPaymentIntent Success")
                val data = result.data as? Map<*, *> ?: return@addOnSuccessListener
                val success = data["success"] as? Boolean ?: false
                val message = data["paymentResponseMessage"] as? String ?: "Unknown"
                val responseAmount = (data["amount"] as? Number)?.toInt() ?: 0
                val recipientName = data["recipientName"] as? String ?: "Recipient"

                if (success) {

                    try {
                        val db = LocalDatabase.getInstance(context)
                        db.deleteOrder(orderId)
                        Log.d("LocalDatabase", "✅ Deleted order: $orderId")

                    } catch (e: Exception) {
                        Log.e("LocalDatabase", "❌ Failed to delete order: ${e.localizedMessage}")
                    }

                    completion(orderId, message, null)
                } else {

                    completion(null, null, Exception("Payment failed: $message"))
                }
            }
            .addOnFailureListener { e ->
                e.localizedMessage?.let { Log.d("Payment flow failed", it) }
                completion(null, null, Exception("Verification failed: ${e.localizedMessage}"))

            }
    }


    fun retryUPIPayment(
        squadId: String,
        failedOrderId: String,
        onComplete: (transferId: String?, orderId: String?, error: Exception?) -> Unit
    ) {
        val data = mapOf(
            "squadId" to squadId,
            "failedOrderId" to failedOrderId
        )

        functions
            .getHttpsCallable("retryUPIPaymentIntent")
            .call(data)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    val e = task.exception
                    onComplete(null, null, e)
                    return@addOnCompleteListener
                }

                val resultData = task.result?.data as? Map<*, *> ?: mapOf<String, Any>()
                val orderId = resultData["orderId"] as? String
                val transferId = resultData["transferId"] as? String

                if (orderId.isNullOrEmpty() || transferId.isNullOrEmpty()) {
                    onComplete(null, null, Exception("Invalid response from server"))
                    return@addOnCompleteListener
                }

                onComplete(transferId, orderId, null)
            }
    }
}