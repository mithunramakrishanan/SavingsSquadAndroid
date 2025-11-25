package com.android.savingssquad.view

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.android.savingssquad.singleton.BulkOrder
import com.android.savingssquad.singleton.LocalDatabase
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


class PaymentViewController : ComponentActivity(), CFCheckoutResponseCallback {

    private val squadViewModel: SquadViewModel by viewModels()

    private var orderId: String = ""
    private var payment_session_id: String = ""
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

        // Get database instance
        val db = LocalDatabase.getInstance(this)

        // Example: Insert order into DB (run on background thread)
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

    // ✅ Start Payment (Cashfree SDK v2)
    private fun startPayment() {
        try {
            val cfSession = CFSession.CFSessionBuilder()
                .setEnvironment(CFSession.Environment.SANDBOX).setPaymentSessionID(payment_session_id)
                .setOrderId(orderId)
                .build()

            val cfPayment = CFWebCheckoutPayment.CFWebCheckoutPaymentBuilder()
                .setSession(cfSession)
                .build()

            CFPaymentGatewayService.getInstance().doPayment(this, cfPayment)
            CFPaymentGatewayService.getInstance().setCheckoutCallback(this)

        } catch (e: CFException) {
            e.printStackTrace()
            triggerFailure("Payment initialization failed: ${e.localizedMessage}")
        } catch (e: Exception) {
            e.printStackTrace()
            triggerFailure("Unexpected error: ${e.localizedMessage}")
        }
    }

    // ✅ Callbacks from Cashfree SDK
    override fun onPaymentVerify(orderID: String) {
        Log.d("Payment flow Success", orderID)
        verifyPayment(orderID)
    }

    override fun onPaymentFailure(errorResponse: CFErrorResponse, orderID: String) {
        Log.d("Payment flow Failed", errorResponse.message)
        val msg = errorResponse.message ?: "Payment failed"
        triggerFailure(msg)
    }

    // ✅ Verify payment via Firebase Cloud Function
    private fun verifyPayment(orderId: String) {
        Log.d("Payment flow", "Verifying payment...")

        Log.d("Payment flow orderId", orderId)
        Log.d("Payment flow squadId", squadId)
        setContent {
            PaymentScreen(
                isLoading = true,
                message = "Verifying payment..."
            )
        }
        val functions = FirebaseFunctions.getInstance()
        functions
            .getHttpsCallable("verifyCashFreePaymentStatus")
            .call(
                mapOf(
                    "orderId" to orderId,
                    "squadId" to squadId
                )
            )
            .addOnSuccessListener { result ->
                Log.d("Payment flow Success", "verifyCashFreePaymentStatus Success")
                val data = result.data as? Map<*, *> ?: return@addOnSuccessListener
                val success = data["success"] as? Boolean ?: false
                val message = data["paymentResponseMessage"] as? String ?: "Unknown"
                val amount = (data["amount"] as? Number)?.toInt() ?: 0
                val recipientName = data["recipientName"] as? String ?: "Recipient"

                if (success) {

                    try {
                        val db = LocalDatabase.getInstance(this)
                        db.deleteOrder(orderId)
                        Log.d("LocalDatabase", "✅ Deleted order: $orderId")

                    } catch (e: Exception) {
                        Log.e("LocalDatabase", "❌ Failed to delete order: ${e.localizedMessage}")
                    }

                    triggerSuccess(orderId, amount, message, recipientName)
                } else {
                    triggerFailure("Payment failed: $message")
                }
            }
            .addOnFailureListener { e ->
                Log.d("Payment flow failed", e.localizedMessage)
                triggerFailure("Verification failed: ${e.localizedMessage}")
            }
    }

    // ✅ Handle success
    private fun triggerSuccess(orderId: String, amount: Int, message: String, recipientName: String) {
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

    // ✅ Handle failure
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
}