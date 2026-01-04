package com.android.savingssquad.view


import android.app.Activity
import android.content.Intent
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import org.json.JSONObject

//Razor pay
/*class PaymentViewController : ComponentActivity(), PaymentResultListener {

    private val squadViewModel: SquadViewModel by viewModels()

    private var orderId: String = ""
    private var payment_session_id: String = ""   // Razorpay amount
    private var squadId: String = ""

    private var onPaymentSuccess: ((String) -> Unit)? = null
    private var onPaymentFailure: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        orderId = intent.getStringExtra("orderId") ?: ""
        payment_session_id = intent.getStringExtra("payment_session_id") ?: ""
        squadId = intent.getStringExtra("squadId") ?: ""

        setContent {
            PaymentScreen(
                isLoading = true,
                message = "Processing payment..."
            )
        }

        val db = LocalDatabase.getInstance(this)
        val order = BulkOrder(orderId = orderId, squadId = squadId)

        Thread {
            try {
                db.insertOrders(listOf(order))
                Log.d("PaymentActivity", "✅ Order saved locally: $orderId")
            } catch (e: Exception) {
                Log.e("PaymentActivity", "❌ Failed to save order: ${e.localizedMessage}")
            }
        }.start()

        startPayment()
    }

    // ✅ Razorpay Payment Start
    private fun startPayment() {
        try {
            val checkout = Checkout()
            checkout.setKeyID("rzp_test_RpTo57d9qKVhOm")  // replace in prod

            val options = JSONObject()
            options.put("name", "Savings Squad")
            options.put("description", "Payment for Order $orderId")
            options.put("order_id", orderId)
            options.put("currency", "INR")

            val amountInPaise =
                (payment_session_id.toDoubleOrNull()?.times(100))?.toInt() ?: 0
            options.put("amount", amountInPaise)

            val prefill = JSONObject()
            prefill.put("email", "support@savingssquad.com")
            prefill.put("contact", "9999999999")
            options.put("prefill", prefill)

            checkout.open(this, options)

        } catch (e: Exception) {
            Log.e("Razorpay", "❌ Error: ${e.localizedMessage}")
            triggerFailure("Payment initialization failed: ${e.localizedMessage}")
        }
    }

    // ✅ Razorpay Success Callback
    override fun onPaymentSuccess(paymentId: String?) {
        Log.d("Razorpay", "Payment success: $paymentId")
        verifyPayment(orderId)
    }

    // ✅ Razorpay Error Callback
    override fun onPaymentError(code: Int, message: String?) {
        Log.e("Razorpay", "Payment failed: $message")
        triggerFailure("Payment failed: ${message ?: "Unknown error"}")
    }

    // 🔥 SAME LOGIC — Calls Firebase Function for verification
    private fun verifyPayment(orderId: String) {
        Log.d("Payment flow", "Verifying payment…")

        setContent {
            PaymentScreen(
                isLoading = true,
                message = "Verifying payment..."
            )
        }

        FirebaseFunctions.getInstance()
            .getHttpsCallable("verifyCashFreePaymentStatus")
            .call(
                mapOf(
                    "orderId" to orderId,
                    "squadId" to squadId
                )
            )
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *> ?: return@addOnSuccessListener

                val success = data["success"] as? Boolean ?: false
                val message = data["paymentResponseMessage"] as? String ?: "Unknown"
                val amount = (data["amount"] as? Number)?.toInt() ?: 0
                val recipientName = data["recipientName"] as? String ?: "Recipient"

                if (success) {
                    try {
                        val db = LocalDatabase.getInstance(this)
                        db.deleteOrder(orderId)
                    } catch (_: Exception) {}

                    triggerSuccess(orderId, amount, message, recipientName)
                } else {
                    triggerFailure("Payment failed: $message")
                }
            }
            .addOnFailureListener { e ->
                triggerFailure("Verification failed: ${e.localizedMessage}")
            }
    }

    private fun triggerSuccess(
        orderId: String,
        amount: Int,
        message: String,
        recipientName: String
    ) {
        setContent {
            PaymentResultScreen(
                success = true,
                message = message,
                recipientName = recipientName,
                totalAmount = "₹$amount",
                onDone = {
                    onPaymentSuccess?.invoke(orderId)
                    finish()
                },
                recipientNumber = orderId
            )
        }
    }

    private fun triggerFailure(message: String) {
        setContent {
            PaymentResultScreen(
                success = false,
                message = message,
                recipientName = "",
                totalAmount = "",
                onDone = {
                    onPaymentFailure?.invoke(message)
                    finish()
                },
                recipientNumber = orderId
            )
        }
    }
} */

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.android.savingssquad.singleton.BulkOrder
import com.android.savingssquad.singleton.LocalDatabase
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.viewmodel.SquadViewModel
import com.cashfree.pg.api.CFPaymentGatewayService
//import com.cashfree.pg.core.api.CFPayment
import com.cashfree.pg.core.api.CFSession
import com.cashfree.pg.core.api.callback.CFCheckoutResponseCallback
import com.cashfree.pg.core.api.exception.CFException
//import com.cashfree.pg.core.api.response.CFErrorResponse
import com.google.firebase.functions.FirebaseFunctions
import com.cashfree.pg.core.api.*
import com.cashfree.pg.core.api.base.CFPayment
import com.cashfree.pg.core.api.utils.CFErrorResponse
import com.cashfree.pg.core.api.webcheckout.CFWebCheckoutPayment
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener

object RazorpayHolder {
    var checkout: Checkout? = null
}

class PaymentViewController : ComponentActivity(), PaymentResultListener {

    private val squadViewModel: SquadViewModel by viewModels()

    private var orderId = ""
    private var squadId = ""

    private var hasOpened = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        orderId = intent.getStringExtra("orderId") ?: ""
        squadId = intent.getStringExtra("squadId") ?: ""

        setLoading("Opening payment gateway...")

        // Save order locally
        Thread {
            try {
                LocalDatabase.getInstance(this)
                    .insertOrders(listOf(BulkOrder(orderId, squadId)))
                Log.d("PaymentActivity", "✅ Order saved locally")
            } catch (e: Exception) {
                Log.e("PaymentActivity", "❌ Local save failed", e)
            }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        if (!hasOpened) {
            hasOpened = true
            Handler(Looper.getMainLooper()).postDelayed({
                startPayment()
            }, 300)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        RazorpayHolder.checkout = null
        Log.d("PaymentActivity", "🧹 Razorpay cleared")
    }

    /* ───────────────────────────────────────────── */
    /* 🚀 START PAYMENT                              */
    /* ───────────────────────────────────────────── */
    private fun startPayment() {
        try {
            val checkout = Checkout()
            checkout.setKeyID(SquadStrings.RAZORPAY_KEY_ID)

            // 🔐 STRONG reference
            RazorpayHolder.checkout = checkout

            val options = JSONObject().apply {
                put("name", "Savings Squad")
                put("order_id", orderId)
                put("currency", "INR")
                put("theme.color", "#0A6C3D")

                put(
                    "prefill", JSONObject().apply {
                        put("contact", "")
                        put("email", "")
                    }
                )
            }

            checkout.open(this, options)

        } catch (e: Exception) {
            triggerFailure("Payment init failed: ${e.localizedMessage}")
        }
    }

    /* ───────────────────────────────────────────── */
    /* ✅ CALLBACK: SUCCESS                          */
    /* ───────────────────────────────────────────── */
    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        Log.d("Razorpay", "✅ Success: $razorpayPaymentId")
        verifyPayment()
    }

    /* ───────────────────────────────────────────── */
    /* ❌ CALLBACK: FAILURE                          */
    /* ───────────────────────────────────────────── */
    override fun onPaymentError(code: Int, response: String?) {
        Log.e("Razorpay", "❌ Error: $code → $response")
        verifyPayment() // 🔁 ALWAYS VERIFY
    }

    /* ───────────────────────────────────────────── */
    /* 🔐 VERIFY WITH BACKEND                        */
    /* ───────────────────────────────────────────── */
    private fun verifyPayment() {
        setLoading("Verifying payment...")

        FirebaseFunctions.getInstance()
            .getHttpsCallable("verifyRazorpayPaymentStatus")
            .call(
                mapOf(
                    "orderId" to orderId,
                    "squadId" to squadId
                )
            )
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *> ?: return@addOnSuccessListener
                val success = data["success"] as? Boolean ?: false

                if (success) {
                    try {
                        LocalDatabase.getInstance(this).deleteOrder(orderId)
                    } catch (_: Exception) {}

                    showResult(
                        success = true,
                        message = data["paymentResponseMessage"] as? String
                            ?: "Payment Successful",
                        amount = "${data["amount"] ?: 0}"
                    )
                } else {
                    showResult(
                        success = false,
                        message = data["paymentResponseMessage"] as? String
                            ?: "Payment Failed",
                        amount = "0"
                    )
                }
            }
            .addOnFailureListener {
                triggerFailure(it.localizedMessage ?: "Verification failed")
            }
    }

    /* ───────────────────────────────────────────── */
    /* 🧠 UI HELPERS                                 */
    /* ───────────────────────────────────────────── */
    private fun setLoading(message: String) {
        setContent {
            PaymentScreen(isLoading = true, message = message)
        }
    }

    private fun showResult(success: Boolean, message: String, amount: String) {
        setContent {
            PaymentResultScreen(
                success = success,
                message = message,
                recipientName = "",
                orderId = orderId,
                amount = amount,
                onDone = { finish() }
            )
        }
    }

    private fun triggerFailure(message: String) {
        showResult(false, message, "0")
    }
}

