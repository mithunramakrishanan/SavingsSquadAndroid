package com.android.savingssquad.singleton

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.android.savingssquad.model.PaymentsDetails
import com.android.savingssquad.viewmodel.AlertManager
import com.google.gson.Gson

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val gson = Gson()
        val paymentJson = intent.getStringExtra("payment_data") ?: return
        val payment = gson.fromJson(paymentJson, PaymentsDetails::class.java)
        val squadViewModel = ViewModelHolder.squadViewModel ?: return
        val manager = NotificationManagerCompat.from(context)

        when (intent.action) {

            "ACTION_CONFIRM" -> {
                Log.d("NOTI", "✔ Confirm clicked: $payment")

                squadViewModel.updatePaymentApproveStatus(
                    squadID = payment.squadId,
                    paymentID = payment.id ?: "",
                    status = PaymentApproveStatus.ACCEPTED
                ) { success, error ->
                }
            }

            "ACTION_REJECT" -> {
                Log.d("NOTI", "❌ Reject clicked: $payment")

                squadViewModel.updatePaymentApproveStatus(
                    squadID = payment.squadId,
                    paymentID = payment.id ?: "",
                    status = PaymentApproveStatus.REJECTED
                ) { success, error ->
                }
            }
        }
    }
}