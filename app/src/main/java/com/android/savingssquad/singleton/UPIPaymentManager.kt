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

// MARK: - UPI Payment Status
enum class UPIPaymentStatus {
    SUCCESS, FAILED, PENDING, CANCELLED
}

class UPIPaymentManager private constructor() {

    // MARK: - Singleton
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

    // MARK: - Private Properties
    private var onReturn: ((UPIPaymentStatus) -> Unit)? = null
    private var transactionRef: String? = null
    private var launcher: ActivityResultLauncher<Intent>? = null

    // MARK: - Register (call in MainActivity.onCreate)
    fun register(activity: ComponentActivity) {  // ← ComponentActivity not AppCompatActivity
        launcher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val data = result.data
            val status = data?.getStringExtra("Status") ?: ""
            val txnId  = data?.getStringExtra("txnId")  ?: ""
            val txnRef = data?.getStringExtra("txnRef") ?: ""

            println("🔍 UPI Result — status: $status txnId: $txnId txnRef: $txnRef")

            val paymentStatus = when (status.lowercase()) {
                "success"   -> UPIPaymentStatus.SUCCESS
                "failure"   -> UPIPaymentStatus.FAILED
                "submitted" -> UPIPaymentStatus.PENDING
                ""          -> UPIPaymentStatus.CANCELLED
                else        -> UPIPaymentStatus.PENDING
            }

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
        this.transactionRef = transactionRef
        this.onReturn = onReturn

        val installedApps = upiApps.filter { (_, packageName, _) ->
            isAppInstalled(context, packageName)
        }

        if (installedApps.isEmpty()) {
            AlertManager.shared.showAlert(
                title = "No UPI App Found",
                message = "Please install GPay, PhonePe, or Paytm to make payments."
            )
            completion?.invoke(false)
            onReturn?.invoke(UPIPaymentStatus.CANCELLED)
            return
        }

        AlertDialog.Builder(activity)
            .setTitle("Pay with UPI")
            .setItems(installedApps.map { it.first }.toTypedArray()) { _, index ->
                val (_, packageName, deepLinkBase) = installedApps[index]
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
                openDeepLink(context, deepLink, upiID, name, amount, note)
                completion?.invoke(true)
            }
            .setNegativeButton("Cancel") { _, _ ->
                completion?.invoke(false)
                onReturn?.invoke(UPIPaymentStatus.CANCELLED)
                this.onReturn = null
                this.transactionRef = null
            }
            .create()
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
            .appendQueryParameter("am", String.format("%.2f", amount))
            .appendQueryParameter("cu", "INR")

        if (merchantCode.isNotEmpty()) {
            queryBuilder.appendQueryParameter("mc", merchantCode)
        }

        val query = queryBuilder.build().encodedQuery ?: ""

        return if (base == "intent") {
            // ✅ GPay — Android Intent URI format
            "intent://pay?$query#Intent;scheme=upi;package=$packageName;end"
        } else {
            // Other apps — standard deep link
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
        val intent = if (deepLink.startsWith("intent://")) {
            // ✅ Parse Android Intent URI for GPay
            Intent.parseUri(deepLink, Intent.URI_INTENT_SCHEME).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        if (launcher != null) {
            launcher!!.launch(intent)
        } else if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Fallback to generic upi://
            val fallbackUri = Uri.parse("upi://pay").buildUpon()
                .appendQueryParameter("pa", upiID)
                .appendQueryParameter("pn", name)
                .appendQueryParameter("am", String.format("%.2f", amount))
                .appendQueryParameter("cu", "INR")
                .appendQueryParameter("tn", note)
                .build()

            val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            launcher?.launch(fallbackIntent) ?: context.startActivity(
                Intent.createChooser(fallbackIntent, "Pay with UPI").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    // MARK: - Check App Installed
    private fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}