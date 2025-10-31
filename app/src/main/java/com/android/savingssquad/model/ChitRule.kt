package com.android.savingssquad.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PropertyName

import java.util.Date


@Keep
data class GroupFundRule(

    @get:PropertyName("id") @set:PropertyName("id")
    var id: String? = null, // Firestore document ID

    @get:PropertyName("ruleText") @set:PropertyName("ruleText")
    var ruleText: String = "", // Description or text of the rule

    @get:PropertyName("createdDate") @set:PropertyName("createdDate")
    var createdDate: Timestamp? = null, // Date the rule was created

    @get:PropertyName("recordDate") @set:PropertyName("recordDate")
    var recordDate: Date = Date() // Internal tracking (e.g., sorting or filtering)
) {
    // Required empty constructor for Firestore deserialization
    constructor() : this(
        id = null,
        ruleText = "",
        createdDate = null,
        recordDate = Date()
    )
}