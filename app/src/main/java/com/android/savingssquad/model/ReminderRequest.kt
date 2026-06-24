package com.android.savingssquad.model

data class ReminderRequest(
    val squadId: String,
    val memberIds: List<String>,
    val title: String,
    val message: String,
    val data: Map<String, String>? = null
)

data class ReminderResponse(
    val success: Boolean? = null,
    val sentTo: Int? = null
)