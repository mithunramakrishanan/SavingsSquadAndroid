package com.android.savingssquad.singleton

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object SquadStringsEnglishDesc {

    const val startedSquadWithAmountOf =
        "Started a squad with an amount of"

    fun forceClosedDesc(loanNumber: String): String =
        "Force Closed #$loanNumber"

    fun contributionFor(monthYear: String): String =
        "Contribution for $monthYear."

    fun emiAndInterest(
        installmentNumber: String,
        loanNumber: String,
        total: String
    ): String =
        "EMI and Interest - $installmentNumber for #$loanNumber $total"

    fun amountDebited(
        amount: String,
        notes: String
    ): String =
        "Amount $amount debited for $notes"

    fun squadAmountUpdated(
        oldAmount: String,
        newAmount: String,
        reason: String
    ): String =
        "Squad manager updated squad amount from $oldAmount to $newAmount for $reason"

    fun changedSquadDurationAndAmount(
        oldDuration: String,
        newDuration: String,
        oldAmount: String,
        newAmount: String
    ): String =
        "Changed squad duration from $oldDuration to $newDuration and squad amount from $oldAmount to $newAmount"

    fun changedSquadAmount(
        oldAmount: String,
        newAmount: String
    ): String =
        "Changed squad amount from $oldAmount to $newAmount"

    fun changedSquadDuration(
        oldDuration: String,
        newDuration: String
    ): String =
        "Changed squad duration from $oldDuration to $newDuration"

    fun recordedPayment(
        amount: String,
        note: String
    ): String =
        "Recorded payment of $amount. Note: $note."

    fun loanForceClosedByManager(
        loanNumber: String,
        memberName: String
    ): String =
        "Loan #$loanNumber for $memberName was force closed by the squad manager."

    fun forceClosedLoanSettlement(
        loanNumber: String,
        memberName: String,
        amount: String
    ): String =
        "Force closed Loan #$loanNumber for $memberName. Total settlement: $amount."

    fun contributionPaymentUpdatedByManager(
        memberName: String,
        monthYear: String
    ): String =
        "Contribution payment for $memberName ($monthYear) updated by the squad manager."

    fun updatedContribution(
        memberName: String,
        monthYear: String,
        amount: String
    ): String =
        "Updated contribution for $memberName ($monthYear) — Amount: $amount."

    fun emiPaymentUpdatedByManager(
        memberName: String,
        installmentNumber: String,
        loanNumber: String
    ): String =
        "EMI payment for $memberName - $installmentNumber of Loan #$loanNumber updated by the squad manager."

    fun updatedEMIPayment(
        memberName: String,
        installmentNumber: String,
        loanNumber: String,
        amount: String
    ): String =
        "Updated EMI payment for $memberName — $installmentNumber for Loan #$loanNumber. Amount: $amount."

    fun deletedEMIConfig(
        loanAmount: String,
        interestRate: String
    ): String =
        "Deleted EMI Config - Loan Amount $loanAmount with interest of $interestRate"

    fun emiConfigurationCreated(
        loanAmount: String,
        interestRate: String,
        interestType: String
    ): String =
        "EMI configuration created (Loan ₹$loanAmount, Interest $interestRate% - $interestType)"

    fun emiConfigurationUpdated(
        oldLoanAmount: String,
        newLoanAmount: String,
        oldInterestRate: String,
        oldInterestType: String,
        newInterestRate: String,
        newInterestType: String
    ): String =
        "EMI configuration updated: Loan ₹$oldLoanAmount → ₹$newLoanAmount, " +
                "Interest $oldInterestRate% ($oldInterestType) → " +
                "$newInterestRate% ($newInterestType)"

    fun addedNewMember(memberName: String): String =
        "Added a new member $memberName to the squad"

    fun managerUpdatedMemberContribution(
        oldAmount: String,
        newAmount: String
    ): String =
        "Manager updated member contribution amount $oldAmount to $newAmount"

    fun managerUpdatedMemberLoanBorrowed(
        oldAmount: String,
        newAmount: String
    ): String =
        "Manager updated member loan borrowed amount $oldAmount to $newAmount"

    fun managerUpdatedMemberLoanPaid(
        oldAmount: String,
        newAmount: String
    ): String =
        "Manager updated member loan paid amount $oldAmount to $newAmount"

    fun managerUpdatedMemberInterestPaid(
        oldAmount: String,
        newAmount: String
    ): String =
        "Manager updated member interest paid amount $oldAmount to $newAmount"
}

object SquadStringsTamilDesc {

    const val startedSquadWithAmountOf =
        "ஒரு குழுவை ஒரு தொகையுடன் தொடங்கினார்"

    fun forceClosedDesc(loanNumber: String): String =
        "கட்டாயமாக முடிக்கப்பட்டது #$loanNumber"

    fun contributionFor(monthYear: String): String =
        "$monthYear மாதத்திற்கான பங்களிப்பு."

    fun emiAndInterest(
        installmentNumber: String,
        loanNumber: String,
        total: String
    ): String =
        "EMI மற்றும் வட்டி - #$loanNumber க்கான தவணை $installmentNumber $total"

    fun amountDebited(
        amount: String,
        notes: String
    ): String =
        "$notes க்காக $amount தொகை பற்று வைக்கப்பட்டது"

    fun squadAmountUpdated(
        oldAmount: String,
        newAmount: String,
        reason: String
    ): String =
        "குழு மேலாளர், குழுவின் தொகையை $oldAmount இலிருந்து $newAmount ஆக $reason காரணமாக மாற்றினார்"

    fun changedSquadDurationAndAmount(
        oldDuration: String,
        newDuration: String,
        oldAmount: String,
        newAmount: String
    ): String =
        "குழுவின் கால அளவை $oldDuration இலிருந்து $newDuration ஆகவும், " +
                "குழுவின் தொகையை $oldAmount இலிருந்து $newAmount ஆகவும் மாற்றினார்"

    fun changedSquadAmount(
        oldAmount: String,
        newAmount: String
    ): String =
        "குழுவின் தொகையை $oldAmount இலிருந்து $newAmount ஆக மாற்றினார்"

    fun changedSquadDuration(
        oldDuration: String,
        newDuration: String
    ): String =
        "குழுவின் கால அளவை $oldDuration இலிருந்து $newDuration ஆக மாற்றினார்"

    fun recordedPayment(
        amount: String,
        note: String
    ): String =
        "$amount தொகைக்கான பணம் பதிவு செய்யப்பட்டது. குறிப்பு: $note."

    fun loanForceClosedByManager(
        loanNumber: String,
        memberName: String
    ): String =
        "$memberName அவர்களின் #$loanNumber கடன், குழு மேலாளரால் முன்கூட்டியே முடிக்கப்பட்டது."

    fun forceClosedLoanSettlement(
        loanNumber: String,
        memberName: String,
        amount: String
    ): String =
        "$memberName அவர்களின் #$loanNumber கடன் முன்கூட்டியே முடிக்கப்பட்டது. மொத்த தீர்வுத் தொகை: $amount."

    fun contributionPaymentUpdatedByManager(
        memberName: String,
        monthYear: String
    ): String =
        "$memberName ($monthYear) மாதத்திற்கான பங்களிப்பு பணம் குழு மேலாளரால் புதுப்பிக்கப்பட்டது."

    fun updatedContribution(
        memberName: String,
        monthYear: String,
        amount: String
    ): String =
        "$memberName ($monthYear) மாதத்திற்கான பங்களிப்பு புதுப்பிக்கப்பட்டது — தொகை: $amount."

    fun emiPaymentUpdatedByManager(
        memberName: String,
        installmentNumber: String,
        loanNumber: String
    ): String =
        "$memberName அவர்களின் #$loanNumber கடனுக்கான $installmentNumber தவணை EMI பணம் குழு மேலாளரால் புதுப்பிக்கப்பட்டது."

    fun updatedEMIPayment(
        memberName: String,
        installmentNumber: String,
        loanNumber: String,
        amount: String
    ): String =
        "$memberName அவர்களின் #$loanNumber கடனுக்கான $installmentNumber EMI பணம் புதுப்பிக்கப்பட்டது. தொகை: $amount."

    fun deletedEMIConfig(
        loanAmount: String,
        interestRate: String
    ): String =
        "EMI அமைப்பு நீக்கப்பட்டது - $loanAmount கடன் தொகை, $interestRate வட்டி"

    fun emiConfigurationCreated(
        loanAmount: String,
        interestRate: String,
        interestType: String
    ): String =
        "EMI அமைப்பு உருவாக்கப்பட்டது (கடன் ₹$loanAmount, வட்டி $interestRate% - $interestType)"

    fun emiConfigurationUpdated(
        oldLoanAmount: String,
        newLoanAmount: String,
        oldInterestRate: String,
        oldInterestType: String,
        newInterestRate: String,
        newInterestType: String
    ): String =
        "EMI அமைப்பு புதுப்பிக்கப்பட்டது: கடன் ₹$oldLoanAmount → ₹$newLoanAmount, " +
                "வட்டி $oldInterestRate% ($oldInterestType) → " +
                "$newInterestRate% ($newInterestType)"

    fun addedNewMember(memberName: String): String =
        "$memberName என்ற புதிய உறுப்பினர் குழுவில் சேர்க்கப்பட்டார்"

    fun managerUpdatedMemberContribution(
        oldAmount: String,
        newAmount: String
    ): String =
        "மேலாளர் உறுப்பினரின் பங்களிப்பு தொகையை $oldAmount இலிருந்து $newAmount ஆக மாற்றினார்"

    fun managerUpdatedMemberLoanBorrowed(
        oldAmount: String,
        newAmount: String
    ): String =
        "மேலாளர் உறுப்பினர் பெற்ற கடன் தொகையை $oldAmount இலிருந்து $newAmount ஆக மாற்றினார்"

    fun managerUpdatedMemberLoanPaid(
        oldAmount: String,
        newAmount: String
    ): String =
        "மேலாளர் உறுப்பினர் செலுத்திய கடன் தொகையை $oldAmount இலிருந்து $newAmount ஆக மாற்றினார்"

    fun managerUpdatedMemberInterestPaid(
        oldAmount: String,
        newAmount: String
    ): String =
        "மேலாளர் உறுப்பினர் செலுத்திய வட்டி தொகையை $oldAmount இலிருந்து $newAmount ஆக மாற்றினார்"
}

object SquadStringsHindiDesc {

    const val startedSquadWithAmountOf =
        "एक समूह को इस राशि के साथ शुरू किया"

    fun forceClosedDesc(loanNumber: String): String =
        "बलपूर्वक बंद किया गया #$loanNumber"

    fun contributionFor(monthYear: String): String =
        "$monthYear के लिए योगदान।"

    fun emiAndInterest(
        installmentNumber: String,
        loanNumber: String,
        total: String
    ): String =
        "EMI और ब्याज - #$loanNumber की किस्त $installmentNumber $total"

    fun amountDebited(
        amount: String,
        notes: String
    ): String =
        "$notes के लिए $amount की राशि डेबिट की गई"

    fun squadAmountUpdated(
        oldAmount: String,
        newAmount: String,
        reason: String
    ): String =
        "स्क्वाड मैनेजर ने स्क्वाड की राशि $oldAmount से $newAmount कर दी, कारण: $reason"

    fun changedSquadDurationAndAmount(
        oldDuration: String,
        newDuration: String,
        oldAmount: String,
        newAmount: String
    ): String =
        "स्क्वाड की अवधि $oldDuration से $newDuration और स्क्वाड की राशि $oldAmount से $newAmount कर दी गई"

    fun changedSquadAmount(
        oldAmount: String,
        newAmount: String
    ): String =
        "स्क्वाड की राशि $oldAmount से $newAmount कर दी गई"

    fun changedSquadDuration(
        oldDuration: String,
        newDuration: String
    ): String =
        "स्क्वाड की अवधि $oldDuration से $newDuration कर दी गई"

    fun recordedPayment(
        amount: String,
        note: String
    ): String =
        "$amount का भुगतान दर्ज किया गया। नोट: $note।"

    fun loanForceClosedByManager(
        loanNumber: String,
        memberName: String
    ): String =
        "$memberName के लिए ऋण #$loanNumber को स्क्वाड मैनेजर द्वारा बलपूर्वक बंद किया गया।"

    fun forceClosedLoanSettlement(
        loanNumber: String,
        memberName: String,
        amount: String
    ): String =
        "$memberName का ऋण #$loanNumber बलपूर्वक बंद किया गया। कुल निपटान राशि: $amount।"

    fun contributionPaymentUpdatedByManager(
        memberName: String,
        monthYear: String
    ): String =
        "$memberName के $monthYear के योगदान भुगतान को स्क्वाड मैनेजर द्वारा अपडेट किया गया।"

    fun updatedContribution(
        memberName: String,
        monthYear: String,
        amount: String
    ): String =
        "$memberName के $monthYear के योगदान को अपडेट किया गया — राशि: $amount।"

    fun emiPaymentUpdatedByManager(
        memberName: String,
        installmentNumber: String,
        loanNumber: String
    ): String =
        "$memberName के ऋण #$loanNumber की किस्त $installmentNumber का EMI भुगतान स्क्वाड मैनेजर द्वारा अपडेट किया गया।"

    fun updatedEMIPayment(
        memberName: String,
        installmentNumber: String,
        loanNumber: String,
        amount: String
    ): String =
        "$memberName के ऋण #$loanNumber की $installmentNumber EMI भुगतान को अपडेट किया गया। राशि: $amount।"

    fun deletedEMIConfig(
        loanAmount: String,
        interestRate: String
    ): String =
        "EMI कॉन्फ़िगरेशन हटाया गया - ऋण राशि $loanAmount, ब्याज $interestRate"

    fun emiConfigurationCreated(
        loanAmount: String,
        interestRate: String,
        interestType: String
    ): String =
        "EMI कॉन्फ़िगरेशन बनाया गया (ऋण ₹$loanAmount, ब्याज $interestRate% - $interestType)"

    fun emiConfigurationUpdated(
        oldLoanAmount: String,
        newLoanAmount: String,
        oldInterestRate: String,
        oldInterestType: String,
        newInterestRate: String,
        newInterestType: String
    ): String =
        "EMI कॉन्फ़िगरेशन अपडेट किया गया: ऋण ₹$oldLoanAmount → ₹$newLoanAmount, " +
                "ब्याज $oldInterestRate% ($oldInterestType) → " +
                "$newInterestRate% ($newInterestType)"

    fun addedNewMember(memberName: String): String =
        "$memberName नाम के नए सदस्य को स्क्वाड में जोड़ा गया"

    fun managerUpdatedMemberContribution(
        oldAmount: String,
        newAmount: String
    ): String =
        "मैनेजर ने सदस्य के योगदान की राशि $oldAmount से $newAmount कर दी"

    fun managerUpdatedMemberLoanBorrowed(
        oldAmount: String,
        newAmount: String
    ): String =
        "मैनेजर ने सदस्य द्वारा लिए गए ऋण की राशि $oldAmount से $newAmount कर दी"

    fun managerUpdatedMemberLoanPaid(
        oldAmount: String,
        newAmount: String
    ): String =
        "मैनेजर ने सदस्य द्वारा चुकाई गई ऋण राशि $oldAmount से $newAmount कर दी"

    fun managerUpdatedMemberInterestPaid(
        oldAmount: String,
        newAmount: String
    ): String =
        "मैनेजर ने सदस्य द्वारा चुकाई गई ब्याज राशि $oldAmount से $newAmount कर दी"
}


object SquadStringsDesc {

    var currentLanguage by mutableStateOf(

        UserDefaultsManager.getLanguage()

    )

        private set

    fun setLanguage(language: SquadLanguages) {

        currentLanguage = language

        UserDefaultsManager.saveLanguage(language)

    }

    val isTamil: Boolean

        get() = currentLanguage == SquadLanguages.TAMIL

    val startedSquadWithAmountOf: String
        get() = if (isTamil)
            SquadStringsTamilDesc.startedSquadWithAmountOf
        else
            SquadStringsEnglishDesc.startedSquadWithAmountOf

    fun forceClosed(loanNumber: String): String =
        if (isTamil)
            SquadStringsTamilDesc.forceClosedDesc(loanNumber)
        else
            SquadStringsEnglishDesc.forceClosedDesc(loanNumber)

    fun contributionFor(monthYear: String): String =
        if (isTamil)
            SquadStringsTamilDesc.contributionFor(monthYear)
        else
            SquadStringsEnglishDesc.contributionFor(monthYear)

    fun emiAndInterest(
        installmentNumber: String,
        loanNumber: String,
        total: String
    ): String =
        if (isTamil)
            SquadStringsTamilDesc.emiAndInterest(
                installmentNumber = installmentNumber,
                loanNumber = loanNumber,
                total = total
            )
        else
            SquadStringsEnglishDesc.emiAndInterest(
                installmentNumber = installmentNumber,
                loanNumber = loanNumber,
                total = total
            )

    fun amountDebited(
        amount: String,
        notes: String
    ): String =
        if (isTamil)
            SquadStringsTamilDesc.amountDebited(amount, notes)
        else
            SquadStringsEnglishDesc.amountDebited(amount, notes)

    fun squadAmountUpdated(
        oldAmount: String,
        newAmount: String,
        reason: String
    ): String =
        if (isTamil)
            SquadStringsTamilDesc.squadAmountUpdated(
                oldAmount,
                newAmount,
                reason
            )
        else
            SquadStringsEnglishDesc.squadAmountUpdated(
                oldAmount,
                newAmount,
                reason
            )

    fun changedSquadDurationAndAmount(
        oldDuration: String,
        newDuration: String,
        oldAmount: String,
        newAmount: String
    ): String =
        if (isTamil)
            SquadStringsTamilDesc.changedSquadDurationAndAmount(
                oldDuration,
                newDuration,
                oldAmount,
                newAmount
            )
        else
            SquadStringsEnglishDesc.changedSquadDurationAndAmount(
                oldDuration,
                newDuration,
                oldAmount,
                newAmount
            )

    fun changedSquadAmount(
        oldAmount: String,
        newAmount: String
    ): String =
        if (isTamil)
            SquadStringsTamilDesc.changedSquadAmount(
                oldAmount,
                newAmount
            )
        else
            SquadStringsEnglishDesc.changedSquadAmount(
                oldAmount,
                newAmount
            )

    fun changedSquadDuration(
        oldDuration: String,
        newDuration: String
    ): String =
        if (isTamil)
            SquadStringsTamilDesc.changedSquadDuration(
                oldDuration,
                newDuration
            )
        else
            SquadStringsEnglishDesc.changedSquadDuration(
                oldDuration,
                newDuration
            )

    fun recordedPayment(
        amount: String,
        note: String
    ): String =
        if (isTamil)
            SquadStringsTamilDesc.recordedPayment(amount, note)
        else
            SquadStringsEnglishDesc.recordedPayment(amount, note)

    fun loanForceClosedByManager(
        loanNumber: String,
        memberName: String
    ): String =
        if (isTamil)
            SquadStringsTamilDesc.loanForceClosedByManager(
                loanNumber,
                memberName
            )
        else
            SquadStringsEnglishDesc.loanForceClosedByManager(
                loanNumber,
                memberName
            )

    fun forceClosedLoanSettlement(
        loanNumber: String,
        memberName: String,
        amount: String
    ): String =
        if (isTamil)
            SquadStringsTamilDesc.forceClosedLoanSettlement(
                loanNumber,
                memberName,
                amount
            )
        else
            SquadStringsEnglishDesc.forceClosedLoanSettlement(
                loanNumber,
                memberName,
                amount
            )

    fun contributionPaymentUpdatedByManager(
        memberName: String,
        monthYear: String
    ): String =
        if (isTamil)
            SquadStringsTamilDesc.contributionPaymentUpdatedByManager(
                memberName,
                monthYear
            )
        else
            SquadStringsEnglishDesc.contributionPaymentUpdatedByManager(
                memberName,
                monthYear
            )

    fun updatedContribution(
        memberName: String,
        monthYear: String,
        amount: String
    ): String =
        if (isTamil)
            SquadStringsTamilDesc.updatedContribution(
                memberName,
                monthYear,
                amount
            )
        else
            SquadStringsEnglishDesc.updatedContribution(
                memberName,
                monthYear,
                amount
            )

    fun emiPaymentUpdatedByManager(
        memberName: String,
        installmentNumber: String,
        loanNumber: String
    ): String =
        if (isTamil)
            SquadStringsTamilDesc.emiPaymentUpdatedByManager(
                memberName,
                installmentNumber,
                loanNumber
            )
        else
            SquadStringsEnglishDesc.emiPaymentUpdatedByManager(
                memberName,
                installmentNumber,
                loanNumber
            )

    fun updatedEMIPayment(
        memberName: String,
        installmentNumber: String,
        loanNumber: String,
        amount: String
    ): String =
        if (isTamil)
            SquadStringsTamilDesc.updatedEMIPayment(
                memberName,
                installmentNumber,
                loanNumber,
                amount
            )
        else
            SquadStringsEnglishDesc.updatedEMIPayment(
                memberName,
                installmentNumber,
                loanNumber,
                amount
            )

    fun deletedEMIConfig(
        loanAmount: String,
        interestRate: String
    ): String =
        if (isTamil)
            SquadStringsTamilDesc.deletedEMIConfig(
                loanAmount,
                interestRate
            )
        else
            SquadStringsEnglishDesc.deletedEMIConfig(
                loanAmount,
                interestRate
            )

    fun emiConfigurationCreated(
        loanAmount: String,
        interestRate: String,
        interestType: String
    ): String =
        if (isTamil)
            SquadStringsTamilDesc.emiConfigurationCreated(
                loanAmount,
                interestRate,
                interestType
            )
        else
            SquadStringsEnglishDesc.emiConfigurationCreated(
                loanAmount,
                interestRate,
                interestType
            )

    fun emiConfigurationUpdated(
        oldLoanAmount: String,
        newLoanAmount: String,
        oldInterestRate: String,
        oldInterestType: String,
        newInterestRate: String,
        newInterestType: String
    ): String =
        if (isTamil)
            SquadStringsTamilDesc.emiConfigurationUpdated(
                oldLoanAmount,
                newLoanAmount,
                oldInterestRate,
                oldInterestType,
                newInterestRate,
                newInterestType
            )
        else
            SquadStringsEnglishDesc.emiConfigurationUpdated(
                oldLoanAmount,
                newLoanAmount,
                oldInterestRate,
                oldInterestType,
                newInterestRate,
                newInterestType
            )

    fun addedNewMember(memberName: String): String =
        if (isTamil)
            SquadStringsTamilDesc.addedNewMember(memberName)
        else
            SquadStringsEnglishDesc.addedNewMember(memberName)

    fun managerUpdatedMemberContribution(
        oldAmount: String,
        newAmount: String
    ): String =
        if (isTamil)
            SquadStringsTamilDesc.managerUpdatedMemberContribution(
                oldAmount,
                newAmount
            )
        else
            SquadStringsEnglishDesc.managerUpdatedMemberContribution(
                oldAmount,
                newAmount
            )

    fun managerUpdatedMemberLoanBorrowed(
        oldAmount: String,
        newAmount: String
    ): String =
        if (isTamil)
            SquadStringsTamilDesc.managerUpdatedMemberLoanBorrowed(
                oldAmount,
                newAmount
            )
        else
            SquadStringsEnglishDesc.managerUpdatedMemberLoanBorrowed(
                oldAmount,
                newAmount
            )

    fun managerUpdatedMemberLoanPaid(
        oldAmount: String,
        newAmount: String
    ): String =
        if (isTamil)
            SquadStringsTamilDesc.managerUpdatedMemberLoanPaid(
                oldAmount,
                newAmount
            )
        else
            SquadStringsEnglishDesc.managerUpdatedMemberLoanPaid(
                oldAmount,
                newAmount
            )

    fun managerUpdatedMemberInterestPaid(
        oldAmount: String,
        newAmount: String
    ): String =
        if (isTamil)
            SquadStringsTamilDesc.managerUpdatedMemberInterestPaid(
                oldAmount,
                newAmount
            )
        else
            SquadStringsEnglishDesc.managerUpdatedMemberInterestPaid(
                oldAmount,
                newAmount
            )
}