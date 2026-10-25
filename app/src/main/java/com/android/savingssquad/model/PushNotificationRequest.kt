package com.android.savingssquad.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class NotificationRecipientType {
    @SerialName("all")
    ALL,

    @SerialName("selected")
    SELECTED
}

@Serializable
data class PushNotificationRequest(
    val squadId: String,
    val memberIds: List<String> = emptyList(),
    val title: String,
    val message: String,
    val data: Map<String, String> = emptyMap(),
    val recipientType: NotificationRecipientType = NotificationRecipientType.SELECTED
)

data class PushNotificationResponse(
    val success: Boolean? = null,
    val sentTo: Int? = null
)