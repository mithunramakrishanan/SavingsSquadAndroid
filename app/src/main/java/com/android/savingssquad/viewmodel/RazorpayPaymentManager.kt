package com.android.savingssquad.viewmodel

import android.app.Activity
import android.util.Log
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject

object RazorpayPaymentManager : PaymentResultWithDataListener {

    private const val TAG = "RazorpayPaymentManager"

    // ---------------------------------------------------------
    // PAYMENT EVENTS
    // ---------------------------------------------------------

    sealed interface PaymentEvent {

        data class VerificationRequested(
            val orderID: String,
            val paymentID: String
        ) : PaymentEvent

        data class PaymentFailed(
            val orderID: String,
            val message: String,
            val code: Int
        ) : PaymentEvent
    }

    private val _paymentEvents =
        MutableSharedFlow<PaymentEvent>(
            extraBufferCapacity = 10
        )

    val paymentEvents = _paymentEvents.asSharedFlow()

    // ---------------------------------------------------------
    // PAYMENT STATE
    // ---------------------------------------------------------

    private var paymentCompletion: ((Boolean) -> Unit)? = null

    private var currentOrderID: String? = null

    private var currentPaymentID: String? = null

    private var isVerificationPending = false

    // ---------------------------------------------------------
    // START RAZORPAY CHECKOUT
    // ---------------------------------------------------------

    fun startCheckout(
        activity: Activity,
        keyID: String,
        orderID: String,
        amountInPaise: Int,
        currency: String = "INR",
        completion: (Boolean) -> Unit
    ) {

        if (paymentCompletion != null) {
            Log.w(TAG, "A Razorpay payment is already in progress.")
            return
        }

        if (
            keyID.isBlank() ||
            !orderID.startsWith("order_") ||
            amountInPaise <= 0 ||
            currency.isBlank()
        ) {

            Log.e(TAG, "Invalid Razorpay payment details.")

            _paymentEvents.tryEmit(
                PaymentEvent.PaymentFailed(
                    orderID = orderID,
                    message = "Invalid payment details. Please try again.",
                    code = -1
                )
            )

            completion(false)
            return
        }

        if (activity.isFinishing || activity.isDestroyed) {
            Log.e(TAG, "Cannot launch Razorpay from an inactive Activity.")

            _paymentEvents.tryEmit(
                PaymentEvent.PaymentFailed(
                    orderID = orderID,
                    message = "Unable to open payment screen. Please try again.",
                    code = -1
                )
            )

            completion(false)
            return
        }

        paymentCompletion = completion
        currentOrderID = orderID
        currentPaymentID = null
        isVerificationPending = false

        try {

            val checkout = Checkout()

            checkout.setKeyID(keyID)

            val options = JSONObject().apply {

                put("name", "Savings Squad")

                put(
                    "description",
                    "Savings Squad Subscription"
                )

                // Use the actual Razorpay order ID returned by Firebase.
                put("order_id", orderID)

                // Amount must be in paise, not rupees.
                put("amount", amountInPaise)

                put("currency", currency)

                put(
                    "theme",
                    JSONObject().apply {
                        put("color", "#4F46E5")
                    }
                )

                put(
                    "retry",
                    JSONObject().apply {
                        put("enabled", true)
                        put("max_count", 3)
                    }
                )
            }

            Log.d(TAG, "Launching Razorpay Checkout")
            Log.d(TAG, "Order ID: $orderID")
            Log.d(TAG, "Currency: $currency")
            Log.d(TAG, "Amount in paise: $amountInPaise")

            checkout.open(activity, options)

        } catch (exception: Exception) {

            Log.e(
                TAG,
                "Failed to launch Razorpay Checkout",
                exception
            )

            emitPaymentFailure(
                orderID = orderID,
                message = exception.localizedMessage
                    ?: "Unable to start payment. Please try again.",
                code = -1
            )

            finishPayment(false)
        }
    }

    // ---------------------------------------------------------
    // RAZORPAY SUCCESS CALLBACK
    // Invoked by MainActivity
    // ---------------------------------------------------------

    override fun onPaymentSuccess(
        paymentId: String,
        paymentData: PaymentData
    ) {

        val orderID = paymentData.orderId ?: currentOrderID

        Log.d(TAG, "Razorpay success callback received")
        Log.d(TAG, "Payment ID: $paymentId")
        Log.d(TAG, "Order ID: $orderID")

        if (orderID.isNullOrBlank() || paymentId.isBlank()) {

            emitPaymentFailure(
                orderID = orderID.orEmpty(),
                message = "Payment details are incomplete. Please contact support.",
                code = -1
            )

            finishPayment(false)
            return
        }

        if (isVerificationPending) {
            Log.w(TAG, "Payment verification is already pending.")
            return
        }

        currentOrderID = orderID
        currentPaymentID = paymentId
        isVerificationPending = true

        val emitted = _paymentEvents.tryEmit(
            PaymentEvent.VerificationRequested(
                orderID = orderID,
                paymentID = paymentId
            )
        )

        if (!emitted) {
            Log.e(TAG, "Unable to enqueue payment verification event.")

            emitPaymentFailure(
                orderID = orderID,
                message = "Unable to verify payment right now. Please check your subscription status.",
                code = -1
            )

            finishPayment(false)
        }
    }

    // ---------------------------------------------------------
    // RAZORPAY FAILURE CALLBACK
    // Invoked by MainActivity
    // ---------------------------------------------------------

    override fun onPaymentError(
        code: Int,
        message: String,
        paymentData: PaymentData
    ) {

        val orderID = paymentData.orderId ?: currentOrderID.orEmpty()

        Log.e(TAG, "Razorpay payment failed")
        Log.e(TAG, "Error code: $code")
        Log.e(TAG, "Error message: $message")
        Log.e(TAG, "Order ID: $orderID")

        emitPaymentFailure(
            orderID = orderID,
            message = message.ifBlank {
                "Payment failed. Please try again."
            },
            code = code
        )

        finishPayment(false)
    }

    // ---------------------------------------------------------
    // COMPLETE BACKEND / SUBSCRIPTION VERIFICATION
    // Called from UpgradePlanScreen
    // ---------------------------------------------------------

    fun completePaymentVerification(success: Boolean) {

        Log.d(
            TAG,
            "Payment verification completed. Verified: $success"
        )

        if (!isVerificationPending) {
            Log.w(TAG, "No payment verification is currently pending.")
            return
        }

        if (success) {

            Log.d(
                TAG,
                "Subscription verification successful. Payment flow completed."
            )

            finishPayment(true)

        } else {

            Log.w(
                TAG,
                "Subscription verification not confirmed."
            )

            finishPayment(false)
        }
    }

    // ---------------------------------------------------------
    // PAYMENT FAILURE EVENT
    // ---------------------------------------------------------

    private fun emitPaymentFailure(
        orderID: String,
        message: String,
        code: Int
    ) {

        _paymentEvents.tryEmit(
            PaymentEvent.PaymentFailed(
                orderID = orderID,
                message = message,
                code = code
            )
        )
    }

    // ---------------------------------------------------------
    // RESET PAYMENT STATE
    // ---------------------------------------------------------

    private fun finishPayment(success: Boolean) {

        val completion = paymentCompletion

        paymentCompletion = null
        currentOrderID = null
        currentPaymentID = null
        isVerificationPending = false

        completion?.invoke(success)
    }
}