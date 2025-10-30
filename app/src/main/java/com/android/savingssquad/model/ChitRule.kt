package com.android.savingssquad.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

import java.util.Date

data class GroupFundRule(
    var id: String? = null,            // Firestore document ID
    var ruleText: String = "",         // Description or text of the rule
    var createdDate: Timestamp? = null, // Date the rule was created
    var recordDate: Date = Date()      // For internal tracking (e.g., sorting or filtering)
)