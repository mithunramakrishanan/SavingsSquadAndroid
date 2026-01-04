package com.android.savingssquad.view

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.savingssquad.R
import java.net.URLEncoder
import androidx.activity.compose.setContent
import com.android.savingssquad.model.PaymentsDetails
import com.android.savingssquad.viewmodel.AlertManager
import com.android.savingssquad.viewmodel.FirebaseFunctionsManager
import com.android.savingssquad.viewmodel.FirestoreManager
import com.google.gson.Gson

class UpiPaymentActivity : ComponentActivity() {

    private lateinit var payment: PaymentsDetails
    private var orderId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ✅ GET PAYMENT DETAILS
        val paymentJson = intent.getStringExtra("payment_details")
        payment = Gson().fromJson(paymentJson, PaymentsDetails::class.java)

        Log.d("payment.upiID", payment.upiID)

        // Loading UI
        setContent {
            PaymentScreen(
                isLoading = true,
                message = "Processing UPI payment..."
            )
        }

        // Create payment in backend
        createUpiPayment()
    }

    // =======================================
    //  CREATE PAYMENT
    // =======================================
    private fun createUpiPayment() {

        FirebaseFunctionsManager.shared.createUPIPaymentIntent(
            squadId = payment.squadId,
            payment = payment
        ) { transferId, createdOrderId, error ->

            if (error != null || createdOrderId.isNullOrEmpty()) {
                showPaymentResult(
                    success = false,
                    message = "Unable to start payment",
                    recipientName = payment.memberName,
                    totalAmount = "₹${payment.amount}",
                    txnId = "NA"
                )
                return@createUPIPaymentIntent
            }

            orderId = createdOrderId

            // START UPI
            if (transferId != null) {
                startUpiPayment(
                    upiId = payment.upiID,
                    payerName = payment.memberName,
                    amount = payment.amount.toString(),
                    note = transferId
                )
            }
        }
    }

    // =======================================
    //  UPI RESULT
    // =======================================
    private val upiPaymentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            val response = result.data?.getStringExtra("response") ?: ""
            val parsed = parseUpiResponse(response)

            val status =
                if (result.resultCode == RESULT_OK || result.resultCode == 11)
                    if (parsed.status.equals("SUCCESS", true)) "SUCCESS" else "FAILED"
                else "FAILED"

            verifyUpiPayment(status, parsed.txnId)
        }

    // =======================================
    //  VERIFY PAYMENT
    // =======================================
    private fun verifyUpiPayment(status: String, txnId: String?) {

       FirebaseFunctionsManager.shared.verifyUPIPaymentIntent(
            context = this,
            squadId = payment.squadId,
            orderId = orderId,
            status = status,
            amount = payment.amount
        ) { _, message, error ->

            val success = error == null

            showPaymentResult(
                success = success,
                message = message ?: if (success) "Payment Successful" else "Payment Failed",
                recipientName = payment.memberName,
                totalAmount = "₹${payment.amount}",
                txnId = txnId ?: orderId
            )
        }
    }

    // =======================================
    //  START UPI
    // =======================================
    private fun startUpiPayment(
        upiId: String,
        payerName: String,
        amount: String,
        note: String,
    ) {
        val finalAmount = amount.toDoubleOrNull() ?: return

        val uri = Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", upiId)               // Payee UPI
            .appendQueryParameter("pn", payerName)           // Payee name
            .appendQueryParameter("tn", note)                // Transaction note
            .appendQueryParameter("am", String.format("%.2f", finalAmount))
            .appendQueryParameter("cu", "INR")
            .appendQueryParameter("tr", orderId)             // Transaction ref
            .build()

        val gpayPackage = "com.google.android.apps.nbu.paisa.user"

        val isGPayInstalled = try {
            packageManager.getPackageInfo(gpayPackage, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

        if (isGPayInstalled) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = uri
                setPackage(gpayPackage) // ✅ FORCE GPay
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            upiPaymentLauncher.launch(intent)
        } else {
            AlertManager.shared.showAlert(
                title = "Google Pay not installed",
                message = "Please install Google Pay to continue payment.",
                primaryButtonTitle = "Install",
                primaryAction = {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=$gpayPackage")
                        )
                    )
                }
            )
        }
    }

    // =======================================
    //  RESULT SCREEN
    // =======================================
    private fun showPaymentResult(
        success: Boolean,
        message: String,
        recipientName: String,
        totalAmount: String,
        txnId: String
    ) {
        setContent {
            PaymentResultScreen(
                success = success,
                message = message,
                recipientName = recipientName,
                orderId = txnId,
                amount = totalAmount,
                onDone = { finish() }
            )
        }
    }

    private fun parseUpiResponse(response: String): UpiResult {
        if (response.isEmpty()) return UpiResult(null, null, null, null)

        val map = response.split("&").mapNotNull {
            val parts = it.split("=")
            if (parts.size == 2) parts[0] to parts[1] else null
        }.toMap()

        return UpiResult(
            status = map["Status"]?.uppercase(),
            txnId = map["txnId"] ?: map["txn_id"],
            approvalRef = map["ApprovalRefNo"] ?: map["txnRef"],
            responseCode = map["responseCode"]
        )
    }
}

data class UpiResult(
    val status: String?,
    val txnId: String?,
    val approvalRef: String?,
    val responseCode: String?
)