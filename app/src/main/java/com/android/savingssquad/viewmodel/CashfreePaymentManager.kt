package com.android.savingssquad.viewmodel

import android.app.Activity
import android.util.Log

import com.cashfree.pg.api.CFPaymentGatewayService
import com.cashfree.pg.core.api.CFSession
import com.cashfree.pg.core.api.callback.CFCheckoutResponseCallback
import com.cashfree.pg.core.api.exception.CFException
import com.cashfree.pg.core.api.utils.CFErrorResponse
import com.cashfree.pg.core.api.webcheckout.CFWebCheckoutPayment

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object CashfreePaymentManager : CFCheckoutResponseCallback {

    private const val TAG = "CashfreePaymentManager"

    // MARK: - Payment Events

    sealed interface PaymentEvent {

        data class VerificationRequested(
            val orderID: String
        ) : PaymentEvent

        data class PaymentFailed(
            val orderID: String,
            val message: String
        ) : PaymentEvent
    }

    private val _paymentEvents =
        MutableSharedFlow<PaymentEvent>(
            extraBufferCapacity = 10
        )

    val paymentEvents = _paymentEvents.asSharedFlow()

    // MARK: - Completion Callback

    private var paymentCompletion: ((Boolean) -> Unit)? = null

    // MARK: - Start Checkout

    fun startCheckout(
        activity: Activity,
        orderID: String,
        paymentSessionID: String,
        completion: (Boolean) -> Unit
    ) {

        paymentCompletion = completion

        try {

            val session = CFSession.CFSessionBuilder()
                .setEnvironment(
                    CFSession.Environment.SANDBOX
                )
                .setPaymentSessionID(paymentSessionID)
                .setOrderId(orderID)
                .build()

            val checkout =
                CFWebCheckoutPayment
                    .CFWebCheckoutPaymentBuilder()
                    .setSession(session)
                    .build()

            val gateway =
                CFPaymentGatewayService.getInstance()

            gateway.setCheckoutCallback(this)

            gateway.doPayment(
                activity,
                checkout
            )

            Log.d(
                TAG,
                "Cashfree checkout started: $orderID"
            )

        } catch (e: CFException) {

            Log.e(
                TAG,
                "Unable to start Cashfree checkout",
                e
            )

            paymentCompletion = null

            completion(false)

            _paymentEvents.tryEmit(
                PaymentEvent.PaymentFailed(
                    orderID = orderID,
                    message = e.message
                        ?: "Unable to open the payment screen."
                )
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Unexpected checkout error",
                e
            )

            paymentCompletion = null

            completion(false)

            _paymentEvents.tryEmit(
                PaymentEvent.PaymentFailed(
                    orderID = orderID,
                    message = e.message
                        ?: "An unexpected payment error occurred."
                )
            )
        }
    }

    // MARK: - Cashfree Callbacks

    override fun onPaymentVerify(
        orderID: String
    ) {

        Log.d(
            TAG,
            "Cashfree requested payment verification: $orderID"
        )

        _paymentEvents.tryEmit(
            PaymentEvent.VerificationRequested(
                orderID = orderID
            )
        )

        // Do not mark payment successful here.
        // Verify the payment through your trusted backend.
    }

    override fun onPaymentFailure(
        cfErrorResponse: CFErrorResponse,
        orderID: String
    ) {

        val message = cfErrorResponse.message
            ?: "Payment failed. Please try again."

        Log.e(
            TAG,
            "Cashfree payment failed: $orderID, $message"
        )

        val completion = paymentCompletion
        paymentCompletion = null

        activityCallback {
            completion?.invoke(false)

            _paymentEvents.tryEmit(
                PaymentEvent.PaymentFailed(
                    orderID = orderID,
                    message = message
                )
            )
        }
    }

    // MARK: - Complete After Backend Verification

    fun completePaymentVerification(
        success: Boolean
    ) {

        val completion = paymentCompletion
        paymentCompletion = null

        completion?.invoke(success)
    }

    /**
     * Use this callback on the main thread.
     * Cashfree callbacks should be checked against the
     * threading requirements of your installed SDK version.
     */
    private fun activityCallback(
        action: () -> Unit
    ) {
        android.os.Handler(
            android.os.Looper.getMainLooper()
        ).post(action)
    }
}