package com.android.savingssquad.viewmodel

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

object SquadNotificationTopicManager {

    private const val TAG = "SquadNotification"
    private fun topicName(squadId: String) = "squad_$squadId"

    fun subscribe(squadId: String) {
        if (squadId.isBlank()) return

        FirebaseMessaging.getInstance()
            .subscribeToTopic(topicName(squadId))
            .addOnSuccessListener {
                Log.d(TAG, "Subscribed to ${topicName(squadId)}")
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Topic subscription failed", error)
            }
    }

    fun unsubscribe(squadId: String, onComplete: (() -> Unit)? = null) {
        if (squadId.isBlank()) {
            onComplete?.invoke()
            return
        }

        FirebaseMessaging.getInstance()
            .unsubscribeFromTopic(topicName(squadId))
            .addOnSuccessListener {
                Log.d(TAG, "Unsubscribed from ${topicName(squadId)}")
                onComplete?.invoke()
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Topic unsubscription failed", error)
                onComplete?.invoke()
            }
    }
}