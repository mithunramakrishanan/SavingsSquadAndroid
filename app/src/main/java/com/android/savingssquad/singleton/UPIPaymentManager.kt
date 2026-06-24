package com.android.savingssquad.singleton

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.android.savingssquad.viewmodel.AlertManager
import androidx.core.net.toUri
import android.util.Log

// MARK: - UPI Payment Status
enum class UPIPaymentStatus {
    SUCCESS, FAILED, PENDING, CANCELLED
}

class UPIPaymentManager private constructor() {

    companion object {
        val shared = UPIPaymentManager()

        private val upiApps = listOf(
            Triple("Google Pay",  "com.google.android.apps.nbu.paisa.user", "intent"),
            Triple("PhonePe",     "com.phonepe.app",                        "phonepe://pay"),
            Triple("Paytm",       "net.one97.paytm",                        "paytmmp://pay"),
            Triple("BHIM",        "in.org.npci.upiapp",                     "bhim://pay"),
            Triple("Amazon Pay",  "in.amazon.mshop.android.shopping",       "amazonpay://pay"),
            Triple("CRED",        "com.dreamplug.androidapp",               "credpay://upi/pay")
        )
    }

    private val TAG = "UPI_PAYMENT"

    private var onReturn: ((UPIPaymentStatus) -> Unit)? = null
    private var transactionRef: String? = null
    private var launcher: ActivityResultLauncher<Intent>? = null

    // MARK: - Register
    fun register(activity: ComponentActivity) {
        launcher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            Log.d(TAG, "━━━━━━━━ UPI RESULT RECEIVED ━━━━━━━━")

            val data = result.data
            val status = data?.getStringExtra("Status") ?: ""
            val txnId = data?.getStringExtra("txnId") ?: ""
            val txnRef = data?.getStringExtra("txnRef") ?: ""

            Log.d(TAG, "Status: $status")
            Log.d(TAG, "TxnId: $txnId")
            Log.d(TAG, "TxnRef: $txnRef")

            val paymentStatus = when (status.lowercase()) {
                "success"   -> UPIPaymentStatus.SUCCESS
                "failure"   -> UPIPaymentStatus.FAILED
                "submitted" -> UPIPaymentStatus.PENDING
                ""          -> UPIPaymentStatus.CANCELLED
                else        -> UPIPaymentStatus.PENDING
            }

            Log.d(TAG, "Mapped Status: $paymentStatus")

            onReturn?.invoke(paymentStatus)
            onReturn = null
            transactionRef = null
        }
    }

    // MARK: - Pay
    fun pay(
        activity: Activity,
        context: Context,
        upiID: String,
        name: String,
        amount: Double,
        note: String,
        transactionRef: String = java.util.UUID.randomUUID().toString(),
        merchantCode: String = "",
        completion: ((Boolean) -> Unit)? = null,
        onReturn: ((UPIPaymentStatus) -> Unit)? = null
    ) {

        Log.d(TAG, "━━━━━━━━ START PAYMENT FLOW ━━━━━━━━")
        Log.d(TAG, "UPI ID: $upiID")
        Log.d(TAG, "Name: $name")
        Log.d(TAG, "Amount: ₹$amount")
        Log.d(TAG, "Note: $note")
        Log.d(TAG, "TxnRef: $transactionRef")

        this.transactionRef = transactionRef
        this.onReturn = onReturn

        val installedApps = upiApps.filter { (_, packageName, _) ->
            isAppInstalled(context, packageName)
        }

        Log.d(TAG, "Installed UPI Apps: ${installedApps.map { it.first }}")

        if (installedApps.isEmpty()) {
            Log.e(TAG, "No UPI apps installed")

            AlertManager.shared.showAlert(
                title = "No UPI App Found",
                message = "Please install GPay, PhonePe, or Paytm to make payments.",
                type = AlertType.ERROR
            )

            completion?.invoke(false)
            onReturn?.invoke(UPIPaymentStatus.CANCELLED)
            return
        }

        AlertDialog.Builder(activity)
            .setTitle("Pay with UPI")
            .setItems(installedApps.map { it.first }.toTypedArray()) { _, index ->

                val (appName, packageName, deepLinkBase) = installedApps[index]

                Log.d(TAG, "Selected App: $appName")

                val deepLink = buildDeepLink(
                    base = deepLinkBase,
                    packageName = packageName,
                    upiID = upiID,
                    name = name,
                    amount = amount,
                    note = note,
                    transactionRef = transactionRef,
                    merchantCode = merchantCode
                )

                Log.d(TAG, "Generated DeepLink: $deepLink")

                openDeepLink(context, deepLink, upiID, name, amount, note)

                completion?.invoke(true)
            }
            .setNegativeButton("Cancel") { _, _ ->
                Log.w(TAG, "User cancelled payment")

                completion?.invoke(false)
                onReturn?.invoke(UPIPaymentStatus.CANCELLED)
                this.onReturn = null
                this.transactionRef = null
            }
            .show()
    }

    // MARK: - Build Deep Link
    private fun buildDeepLink(
        base: String,
        packageName: String,
        upiID: String,
        name: String,
        amount: Double,
        note: String,
        transactionRef: String,
        merchantCode: String = ""
    ): String {

        val queryBuilder = Uri.Builder()
            .appendQueryParameter("pa", upiID)
            .appendQueryParameter("pn", name)
            .appendQueryParameter("tr", transactionRef)
            .appendQueryParameter("tn", note)
            .appendQueryParameter("am", amount.toString().replace(".0", ""))
            .appendQueryParameter("cu", "INR")

        if (merchantCode.isNotEmpty()) {
            queryBuilder.appendQueryParameter("mc", merchantCode)
        }

        val query = queryBuilder.build().encodedQuery ?: ""

        return if (base == "intent") {
            "intent://pay?$query#Intent;scheme=upi;package=$packageName;end"
        } else {
            Uri.parse(base).buildUpon()
                .appendQueryParameter("pa", upiID)
                .appendQueryParameter("pn", name)
                .appendQueryParameter("tr", transactionRef)
                .appendQueryParameter("tn", note)
                .appendQueryParameter("am", String.format("%.2f", amount))
                .appendQueryParameter("cu", "INR")
                .apply {
                    if (merchantCode.isNotEmpty()) appendQueryParameter("mc", merchantCode)
                }
                .build()
                .toString()
        }
    }

    // MARK: - Open Deep Link
    private fun openDeepLink(
        context: Context,
        deepLink: String,
        upiID: String,
        name: String,
        amount: Double,
        note: String
    ) {

        Log.d(TAG, "━━━━━━━━ OPEN DEEP LINK ━━━━━━━━")
        Log.d(TAG, "DeepLink: $deepLink")

        try {

            val intent = if (deepLink.startsWith("intent://")) {

                Log.d(TAG, "Using Intent URI Scheme")

                Intent.parseUri(deepLink, Intent.URI_INTENT_SCHEME).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

            } else {

                Log.d(TAG, "Using ACTION_VIEW")

                Intent(Intent.ACTION_VIEW, deepLink.toUri()).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            Log.d(TAG, "Intent Data: ${intent.data}")

            val resolved = intent.resolveActivity(context.packageManager)

            Log.d(TAG, "Resolved App: $resolved")

            if (launcher != null) {

                Log.d(TAG, "Launching via ActivityResultLauncher")
                launcher!!.launch(intent)

            } else if (resolved != null) {

                Log.d(TAG, "Launching via startActivity")
                context.startActivity(intent)

            } else {

                Log.w(TAG, "No app found → fallback triggered")

                val fallbackUri = Uri.parse("upi://pay")
                    .buildUpon()
                    .appendQueryParameter("pa", upiID)
                    .appendQueryParameter("pn", name)
                    .appendQueryParameter("am", String.format("%.2f", amount))
                    .appendQueryParameter("cu", "INR")
                    .appendQueryParameter("tn", note)
                    .build()

                Log.d(TAG, "Fallback URI: $fallbackUri")

                val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                launcher?.launch(fallbackIntent)
                    ?: context.startActivity(
                        Intent.createChooser(fallbackIntent, "Pay with UPI")
                    )

                Log.d(TAG, "Fallback launched")
            }

        } catch (e: Exception) {

            Log.e(TAG, "Payment failed", e)
            Log.e(TAG, "DeepLink: $deepLink")
        }

        Log.d(TAG, "━━━━━━━━ END OPEN DEEP LINK ━━━━━━━━")
    }

    // MARK: - Check App
    private fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}