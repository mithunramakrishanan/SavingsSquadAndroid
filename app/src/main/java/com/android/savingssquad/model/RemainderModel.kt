package com.android.savingssquad.model


import com.android.savingssquad.singleton.RemainderType
import com.android.savingssquad.singleton.RecordStatus
import com.google.firebase.Timestamp
import java.util.UUID

data class RemainderModel(
    val id: String = UUID.randomUUID().toString(),
    val remainderTitle: String,
    val remainderSubTitle: String,
    val remainderType: RemainderType,
    val remainderAmount: Int,
    val remainderID: String,
    val remainderDueDate: Timestamp? = null,
    val recordStatus: RecordStatus = RecordStatus.ACTIVE,
    val recordDate: Timestamp = Timestamp.now()
)

fun List<RemainderModel>.sortedByDueDateRemainder(): List<RemainderModel> {
    return this.sortedBy {
        it.remainderDueDate?.toDate() ?: java.util.Date() // fallback to current date
    }
}