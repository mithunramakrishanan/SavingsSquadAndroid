package com.android.savingssquad.singleton

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.android.savingssquad.R
import androidx.core.net.toUri

data class UPIApp(
    val name: String,
    val scheme: String,
    val packageName: String,
    val iconRes: Int
)

object UPIAppDetector {

    private val candidates = listOf(

        UPIApp("Google Pay", "gpay://", "com.google.android.apps.nbu.paisa.user", R.drawable.gpay_icon),

        UPIApp("PhonePe", "phonepe://", "com.phonepe.app", R.drawable.phonepe_icon),

        UPIApp("Paytm", "paytmmp://", "net.one97.paytm", R.drawable.paytm_icon),

        UPIApp("CRED", "credpay://", "com.dreamplug.androidapp", R.drawable.cred_icon),

        UPIApp("BHIM", "bhim://", "in.org.npci.upiapp", R.drawable.bhim_icon)

    )

    fun installedApps(context: Context): List<UPIApp> {

        val packageManager = context.packageManager

        return candidates.filter { app ->

            try {

                val intent = Intent(

                    Intent.ACTION_VIEW,

                    app.scheme.toUri()

                )

                intent.resolveActivity(packageManager) != null

            } catch (_: Exception) {

                false

            }

        }

    }

}