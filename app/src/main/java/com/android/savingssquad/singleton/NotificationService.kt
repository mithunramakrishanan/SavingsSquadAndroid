package com.android.savingssquad.singleton

import com.android.savingssquad.model.ReminderRequest
import com.android.savingssquad.model.ReminderResponse
import com.google.firebase.functions.FirebaseFunctions

class NotificationService private constructor() {

    private val functions = FirebaseFunctions.getInstance()

    companion object {
        val shared = NotificationService()
    }

    fun sendMemberReminder(
        request: ReminderRequest,
        onSuccess: (ReminderResponse) -> Unit,
        onError: (Exception) -> Unit
    ) {

        val payload = hashMapOf(
            "squadId" to request.squadId,
            "memberIds" to request.memberIds,
            "title" to request.title,
            "message" to request.message,
            "data" to (request.data ?: emptyMap())
        )

        functions
            .getHttpsCallable("sendMemberReminderPush")
            .call(payload)
            .addOnSuccessListener { result ->

                try {
                    val data = result.data as? Map<*, *>

                    val response = ReminderResponse(
                        success = data?.get("success") as? Boolean,
                        sentTo = (data?.get("sentTo") as? Number)?.toInt()
                    )

                    onSuccess(response)

                } catch (e: Exception) {
                    onError(e)
                }
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }
}