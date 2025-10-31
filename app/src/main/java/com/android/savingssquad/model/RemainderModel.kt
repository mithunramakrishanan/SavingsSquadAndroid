package com.android.savingssquad.model


import androidx.annotation.Keep
import com.android.savingssquad.singleton.RemainderType
import com.android.savingssquad.singleton.RecordStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import java.util.UUID

@Keep
data class RemainderModel(

    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = UUID.randomUUID().toString(),

    @get:PropertyName("remainderTitle") @set:PropertyName("remainderTitle")
    var remainderTitle: String = "",

    @get:PropertyName("remainderSubTitle") @set:PropertyName("remainderSubTitle")
    var remainderSubTitle: String = "",

    @get:PropertyName("remainderType") @set:PropertyName("remainderType")
    var remainderType: RemainderType = RemainderType.CONTRIBUTION,

    @get:PropertyName("remainderAmount") @set:PropertyName("remainderAmount")
    var remainderAmount: Int = 0,

    @get:PropertyName("remainderID") @set:PropertyName("remainderID")
    var remainderID: String = "",

    @get:PropertyName("remainderDueDate") @set:PropertyName("remainderDueDate")
    var remainderDueDate: Timestamp? = null,

    @get:PropertyName("recordStatus") @set:PropertyName("recordStatus")
    var recordStatus: RecordStatus = RecordStatus.ACTIVE,

    @get:PropertyName("recordDate") @set:PropertyName("recordDate")
    var recordDate: Timestamp = Timestamp.now()
) {
    constructor() : this(
        id = UUID.randomUUID().toString(),
        remainderTitle = "",
        remainderSubTitle = "",
        remainderType = RemainderType.CONTRIBUTION,
        remainderAmount = 0,
        remainderID = "",
        remainderDueDate = null,
        recordStatus = RecordStatus.ACTIVE,
        recordDate = Timestamp.now()
    )
}

fun List<RemainderModel>.sortedByDueDateRemainder(): List<RemainderModel> {
    return this.sortedBy {
        it.remainderDueDate?.toDate() ?: java.util.Date() // fallback to current date
    }
}