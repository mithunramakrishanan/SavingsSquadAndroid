package com.android.savingssquad.model

import com.android.savingssquad.singleton.RecordStatus
import com.google.firebase.Timestamp


enum class CashRequestStatus {

    CREATED,

    ACCEPTED,

    REJECTED

}

data class CashRequest(

    var id: String? = null,

    var requestedByName: String = "",
    var requestedByID: String = "",
    var requestedByUPI: String = "",
    var requestedByPhone: String = "",
    var requestedByEmail: String = "",

    var requestedOn: Timestamp? = Timestamp.now(),
    var requestAcceptedOn: Timestamp? = null,

    var requestedEMIConfig: EMIConfiguration? = null,

    var recordStatus: RecordStatus = RecordStatus.ACTIVE,
    var cashRequestStatus: CashRequestStatus = CashRequestStatus.CREATED

) {
    constructor() : this(
        id = null,
        requestedByName = "",
        requestedByID = "",
        requestedByUPI = "",
        requestedByPhone = "",
        requestedByEmail = "",
        requestedOn = Timestamp.now(),
        requestAcceptedOn = null,
        requestedEMIConfig = null,
        recordStatus = RecordStatus.ACTIVE,
        cashRequestStatus = CashRequestStatus.CREATED
    )
}