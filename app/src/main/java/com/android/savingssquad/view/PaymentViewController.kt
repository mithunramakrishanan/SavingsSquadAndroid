package com.android.savingssquad.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
    private var paymentSessionId: String = ""
    private var squadId: String = ""

    private lateinit var functions: FirebaseFunctions

    private var onPaymentSuccess: ((String) -> Unit)? = null
    private var onPaymentFailure: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        orderId = intent.getStringExtra("orderId") ?: ""
        paymentSessionId = intent.getStringExtra("paymentSessionId") ?: ""
        squadId = intent.getStringExtra("squadId") ?: ""

        functions = FirebaseFunctions.getInstance("asia-south1") // ✅ region

        setContent {
            PaymentScreen(
                isLoading = true,
                message = "Processing payment..."
            )
        }

        startPayment()
    }

    // ✅ Start Payment (Cashfree SDK v2)
    private fun startPayment() {
        try {
            val cfSession = CFSession.CFSessionBuilder()
                .setEnvironment(CFSession.Environment.SANDBOX).setPaymentSessionID(paymentSessionId)
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
        verifyPayment(orderID)
    }

    override fun onPaymentFailure(errorResponse: CFErrorResponse, orderID: String) {
        val msg = errorResponse.message ?: "Payment failed"
        triggerFailure(msg)
    }

    // ✅ Verify payment via Firebase Cloud Function
    private fun verifyPayment(orderId: String) {
        setContent {
            PaymentScreen(
                isLoading = true,
                message = "Verifying payment..."
            )
        }

        functions
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
                    triggerSuccess(orderId, amount, message, recipientName)
                } else {
                    triggerFailure("Payment failed: $message")
                }
            }
            .addOnFailureListener { e ->
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
                amount = "₹$amount",
                onDone = {
                    onPaymentSuccess?.invoke(orderId)
                    finish()
                }
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
                amount = "",
                onDone = {
                    onPaymentFailure?.invoke(message)
                    finish()
                }
            )
        }
    }
}