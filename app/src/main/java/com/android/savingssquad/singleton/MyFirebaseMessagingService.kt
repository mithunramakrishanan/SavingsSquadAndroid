package com.android.savingssquad.singleton

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.android.savingssquad.R
import com.android.savingssquad.view.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "default_channel"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        createNotificationChannel()

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Notification"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: ""

        val navigate = remoteMessage.data["navigate"] ?: ""
        val notificationType = remoteMessage.data["notificationType"] ?: ""
        val payment = remoteMessage.data["payment"] ?: ""

        showNotification(title, body, navigate, notificationType, payment)
    }

    private fun showNotification(
        title: String,
        body: String,
        navigate: String,
        notificationType: String,
        payment: String
    ) {

        val notificationId = System.currentTimeMillis().toInt()

        // ✅ FIXED INTENT
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

            putExtra("navigate", navigate)
            putExtra("notificationType", notificationType)
            putExtra("payment_data", payment)
        }

        // ✅ FIXED PENDING INTENT (MUST BE getActivity)
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent) // 🔥 IMPORTANT FIX

        val manager = NotificationManagerCompat.from(this)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            manager.notify(notificationId, builder.build())
        }
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val manager = getSystemService(NotificationManager::class.java)

            manager.deleteNotificationChannel(CHANNEL_ID)

            val soundUri = Uri.parse(
                ContentResolver.SCHEME_ANDROID_RESOURCE +
                        "://$packageName/${R.raw.notification_sound}"
            )

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Default Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Savings Squad Notifications"
                setSound(soundUri, audioAttributes)
                enableVibration(true)
            }

            manager.createNotificationChannel(channel)
        }
    }
}