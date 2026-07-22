package com.android.savingssquad.view

import android.app.Activity
import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.android.savingssquad.SquadSubscription.SubscriptionManager
import com.android.savingssquad.model.EMIConfiguration
import com.android.savingssquad.model.Member
import com.android.savingssquad.model.PaymentsDetails
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.LoaderManager
import com.android.savingssquad.singleton.MemberPaymentSubType
import com.android.savingssquad.singleton.PaymentApproveStatus
import com.android.savingssquad.singleton.PaymentEntryType
import com.android.savingssquad.singleton.PaymentStatus
import com.android.savingssquad.singleton.PaymentSubType
import com.android.savingssquad.singleton.PaymentType
import com.android.savingssquad.singleton.PayoutStatus
import com.android.savingssquad.singleton.RecordStatus
import com.android.savingssquad.singleton.SquadActivityType
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.appShadow
import com.android.savingssquad.singleton.asTimestamp
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.viewmodel.ToastManager
import com.android.savingssquad.viewmodel.ToastType
import com.google.firebase.Timestamp
import com.yourapp.utils.CommonFunctions
import java.util.Date
import kotlin.text.toIntOrNull
import kotlin.text.trim

@Composable
fun ManagerPaymentView(
    navController: NavController,
    squadViewModel: SquadViewModel,
) {
    val coroutineScope = rememberCoroutineScope()

    var selectedSegment by remember { mutableStateOf(SquadStrings.toMemberPayment) }
    var loanSelectedMember by remember { mutableStateOf<Member?>(null) }
    var emiSelectedType by remember { mutableStateOf<EMIConfiguration?>(null) }
    var loanSelectedMemberNameError by remember { mutableStateOf("") }
    var showAllEMIs by remember { mutableStateOf(false) }

    val paymentAmount = remember { mutableStateOf("") }

    var paymentAmountError by remember { mutableStateOf("") }

    var paymentNotes by remember { mutableStateOf("") }
    var paymentNotesError by remember { mutableStateOf("") }


    var selectedMemberPaymentType by remember { mutableStateOf(SquadStrings.memberPaymentTypeLoan) }
    var selectedMemberPaymentSubType by remember { mutableStateOf(SquadStrings.MemberPaymentSubTypeRepayment) }

    val memberPaymentAmount = remember { mutableStateOf("") }
    var memberPaymentAmountError by remember { mutableStateOf("") }
    var memberPaymentNotes by remember { mutableStateOf("") }
    var memberPaymentNotesError by remember { mutableStateOf("") }

    val activity = LocalContext.current as Activity
    val appContext = LocalContext.current.applicationContext

    fun validateFields(): Boolean {
        paymentAmountError = if (paymentAmount.value.isEmpty()) {
            "Amount is required"
        } else ""

        paymentNotesError = if (paymentNotes.isEmpty()) {
            "Note is required"
        } else ""

        return paymentAmountError.isEmpty() &&
                paymentNotesError.isEmpty()
    }

    fun validateMemberOtherPaymentFields(): Boolean {
        memberPaymentAmountError = if (memberPaymentAmount.value.isEmpty()) {
            "Amount is required"
        } else ""

        memberPaymentNotesError = if (memberPaymentNotes.isEmpty()) {
            "Note is required"
        } else ""

        return memberPaymentAmountError.isEmpty() &&
                memberPaymentNotesError.isEmpty()
    }

    Box(
        modifier = Modifier

            .fillMaxSize()

            .windowInsetsPadding(WindowInsets.safeDrawing)
    )
    {
        AppBackgroundGradient()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp),
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        )
        {

            // Navigation Bar
            item {
                SSNavigationBar(title = SquadStrings.payment, showBackButton = false, navController = navController)
            }

            // Segmented Picker
            item {
                ModernSegmentedPickerView(
                    segments = listOf(SquadStrings.toMemberPayment, SquadStrings.otherPayments),
                    selectedSegment = selectedSegment,
                    onSegmentSelected = { selectedSegment = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (selectedSegment == SquadStrings.toMemberPayment) {
                // Member Selection
                item {
                    SectionView(title = "Select Member") {
                        SSTextField(
                            icon = Icons.Default.Person,
                            placeholder = loanSelectedMember?.name ?: "Select Squad Member",
                            textState = remember { mutableStateOf(loanSelectedMember?.name ?: "") },
                            keyboardType = KeyboardType.Text,
                            showDropdown = true,
                            error = loanSelectedMemberNameError,
                            onDropdownTap = { squadViewModel.setShowLoanMembersPopupPopup(true) },
                            disabled = true
                        )
                    }

                    DropdownMenuPicker(
                        selected = selectedMemberPaymentType,
                        items = listOf(SquadStrings.memberPaymentTypeLoan,SquadStrings.memberPaymentTypeOthers),
                        icon = Icons.Default.Tune,
                    ) { selectedMemberPaymentType = it }
                }

                if (selectedMemberPaymentType == SquadStrings.memberPaymentTypeLoan) {

                    if (loanSelectedMember?.currentLoanApproveStatus == EMIStatus.PENDING) {

                        item {

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {

                                Icon(
                                    imageVector = Icons.Default.Pending,
                                    contentDescription = null,
                                    modifier = Modifier.size(50.dp),
                                    tint = AppColors.errorAccent
                                )

                                Text(
                                    text = "Pending loan exists",
                                    style = AppFont.ibmPlexSans(16, FontWeight.SemiBold),
                                    color = AppColors.errorAccent,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "${loanSelectedMember?.name ?: "This member"} already has a pending loan. Please complete or close the existing loan before creating a new one.",
                                    style = AppFont.ibmPlexSans(14),
                                    color = AppColors.secondaryText,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                            }
                        }
                    }
                    else if (loanSelectedMember?.currentLoanApproveStatus == EMIStatus.INVERIFICATION) {

                        item {

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {

                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    modifier = Modifier.size(50.dp),
                                    tint = AppColors.infoAccent
                                )

                                Text(
                                    text = "Loan verification is pending",
                                    style = AppFont.ibmPlexSans(16, FontWeight.SemiBold),
                                    color = AppColors.infoAccent,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "A loan verification request is already pending for ${loanSelectedMember?.name ?: "this member"}. Please wait until it is approved or rejected before creating another loan.",
                                    style = AppFont.ibmPlexSans(14),
                                    color = AppColors.secondaryText,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                            }
                        }
                    }
                    else {

                        // EMI list
                        items(
                            if (showAllEMIs) squadViewModel.emiConfigurations.value
                            else squadViewModel.emiConfigurations.value.take(2),
                            key = { it.id!! }
                        )
                        { emi ->
                            PaymentEMIListRow(
                                emi = emi,
                                isSelected = emiSelectedType?.id == emi.id,
                                onClick = { emiSelectedType = if (emiSelectedType?.id == emi.id) null else emi }
                            )
                        }

                        // Show More / Show Less
                        if (squadViewModel.emiConfigurations.value.size > 2) {
                            item {
                                TextButton(onClick = { showAllEMIs = !showAllEMIs }) {
                                    Text(
                                        text = if (showAllEMIs) "Show Less" else "Show More",
                                        style = AppFont.ibmPlexSans(14, FontWeight.SemiBold),
                                        color = AppColors.infoAccent
                                    )
                                }
                            }
                        }

                        // Payment Button
                        item {
                            val buttonEnabled = (loanSelectedMember?.upiID?.isNotEmpty() == true) && emiSelectedType != null
                            SSButton(
                                title = if (emiSelectedType != null && loanSelectedMember != null)
                                    "Pay ₹${emiSelectedType!!.loanAmount} to ${loanSelectedMember!!.name}'s UPI"
                                else "Pay",
                                isDisabled = !buttonEnabled,
                                action = {
                                    val member = loanSelectedMember
                                    val emi = emiSelectedType

                                    if (member != null && emi != null) {

                                        if (SubscriptionManager.shared.canUseLoan()) {
                                            makeLoanPayment(
                                                squadViewModel = squadViewModel,
                                                selectedMember = member,
                                                selectedLoan = emi,
                                                context = appContext,
                                                activity = activity,
                                                handler = {
                                                    loanSelectedMember = null
                                                    emiSelectedType = null
                                                }
                                            )

                                        }
                                        else {
                                            squadViewModel.setShowUpgradePlan(true)
                                        }
                                    }
                                }
                            )
                        }

                        item {

                            if (loanSelectedMember != null) {

                                if (loanSelectedMember!!.upiID.trim().isEmpty()) {

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                    ) {

                                        Text(
                                            text = "UPI ID not available",
                                            style = AppFont.ibmPlexSans(13, FontWeight.Medium),
                                            color = AppColors.errorAccent
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = "Please ask the member to add their UPI ID before proceeding with the payment.",
                                            style = AppFont.ibmPlexSans(12, FontWeight.Normal),
                                            color = AppColors.secondaryText,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                } else {

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                    ) {

                                        Text(
                                            text = "Payment will be sent to",
                                            style = AppFont.ibmPlexSans(12, FontWeight.Normal),
                                            color = AppColors.secondaryText
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = loanSelectedMember!!.upiID,
                                            style = AppFont.ibmPlexSans(14, FontWeight.Medium),
                                            color = AppColors.successAccent,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = "Please verify the UPI ID before completing the transfer.",
                                            style = AppFont.ibmPlexSans(11, FontWeight.Normal),
                                            color = AppColors.secondaryText,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                }
                else {
                    item {

                        DropdownMenuPicker(
                            selected = selectedMemberPaymentSubType,
                            items = listOf(SquadStrings.MemberPaymentSubTypeRepayment,SquadStrings.MemberPaymentSubTypeSettlement),
                            icon = Icons.Default.Tune,
                        ) { selectedMemberPaymentSubType = it }


                        val amount = remember { mutableStateOf("") }

                        LaunchedEffect(amount.value) {
                            memberPaymentAmount.value = amount.value ?: ""
                        }
                        SSTextField(
                            icon = Icons.Default.CreditCard,
                            placeholder = "Enter Amount",
                            textState = amount,
                            keyboardType = KeyboardType.Number,
                            error = memberPaymentAmountError
                        )


                        SSTextView(
                            placeholder = "Add a note",
                            text = memberPaymentNotes,
                            onTextChange = { memberPaymentNotes = it },
                            error = memberPaymentNotesError,
                            maxCharacters = 200
                        )

                        val buttonEnabled = if(loanSelectedMember?.upiID?.isNotEmpty() == true){false}else {true}
                        SSButton(
                            title = if (loanSelectedMember != null)
                                "Pay ₹${memberPaymentAmount} to ${loanSelectedMember!!.name}'s UPI"
                            else "Pay",
                            isDisabled = !buttonEnabled,
                            action = {
                                val selectedMember = loanSelectedMember

                                if (memberPaymentNotes.trim().isEmpty()) {
                                    paymentNotesError = "Note is required"
                                    return@SSButton
                                } else {
                                    paymentNotesError = ""
                                }

                                if (memberPaymentAmount.value.trim().isEmpty()) {
                                    memberPaymentAmountError = "Amount is required"
                                    return@SSButton
                                } else {
                                    memberPaymentAmountError = ""
                                }

                                val squad = squadViewModel.squad ?: return@SSButton

                                val paymentId = CommonFunctions.generatePaymentID(squad.value?.squadID
                                    ?: "")

                                val amount = memberPaymentAmount.value.toIntOrNull() ?: 0

                                val payment = PaymentsDetails(
                                    id = paymentId,
                                    paymentUpdatedDate = Timestamp.now(),

                                    memberId = selectedMember?.id ?: "",
                                    memberName = selectedMember?.name ?: "",
                                    paymentPhone = selectedMember?.phoneNumber ?: "",
                                    paymentEmail = selectedMember?.mailID ?: "",

                                    userType = SquadUserType.SQUAD_MANAGER,

                                    amount = amount,
                                    intrestAmount = 0,

                                    paymentEntryType = PaymentEntryType.AUTOMATIC_ENTRY,
                                    paymentType = PaymentType.PAYMENT_DEBIT,

                                    paymentSubType =
                                        if (selectedMemberPaymentSubType == SquadStrings.MemberPaymentSubTypeRepayment)
                                            PaymentSubType.RE_PAYMENT
                                        else
                                            PaymentSubType.SETTLEMENT,

                                    paymentStatus = PaymentStatus.INVERIFICATION,
                                    paymentApproveStatus = PaymentApproveStatus.REQUESTED,

                                    description = memberPaymentNotes,

                                    squadId = squad.value?.squadID ?: "",

                                    order_id = paymentId,
                                    contributionId = "",
                                    loanId = "",
                                    installmentId = "",

                                    paymentResponseMessage = "Pending member verification.",

                                    transferReferenceId =
                                        if (selectedMemberPaymentSubType == SquadStrings.MemberPaymentSubTypeRepayment)
                                            "$memberPaymentNotes - ${selectedMember?.name}"
                                        else
                                            "Settlement to ${selectedMember?.name}",

                                    upiID = selectedMember?.upiID ?: "",
                                    cashRequestId = ""
                                )

                                squadViewModel.savePayments(
                                    activity = activity,
                                    context = appContext,
                                    showLoader = false,
                                    squadID = squadViewModel.squad.value?.squadID ?: "",
                                    payment = listOf(payment)
                                ) { success, error ->
                                    if (success) {
                                        println("✅ Payment added successfully!")

                                        squadViewModel.createSquadActivity(
                                            activityType = SquadActivityType.AMOUNT_DEBIT,
                                            userName = selectedMember?.name ?: "",
                                            memberId = selectedMember?.id ?: "",
                                            amount = memberPaymentAmount.value.toInt(),
                                            description = "Amount ${memberPaymentAmount.value} debited for $memberPaymentNotes"
                                        )
                                        LoaderManager.shared.hideLoader()
                                        ToastManager.show(title = SquadStrings.appName, message = "Payment updated", type = ToastType.SUCCESS)
                                    } else {
                                        println("❌ Error adding payment: $error")
                                    }
                                }

                            }
                        )

                    }




                }







            } else {
                // Other Payments
                item {
                    SectionView(title = "Other Payments") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                            val amount = remember { mutableStateOf("") }

                            LaunchedEffect(amount.value) {
                                paymentAmount.value = amount.value ?: ""
                            }
                            SSTextField(
                                icon = Icons.Default.CreditCard,
                                placeholder = "Enter Amount",
                                textState = amount,
                                keyboardType = KeyboardType.Number,
                                error = paymentAmountError
                            )


                            SSTextView(
                                placeholder = "Add a note",
                                text = paymentNotes,
                                onTextChange = { paymentNotes = it },
                                error = paymentNotesError,
                                maxCharacters = 200
                            )

                            SSButton(title = "Make Payment") {
                                /* handle other payment */
                                
                                if (validateFields()) {
                                    handleOtherPayment(squadViewModel = squadViewModel, amountStr = paymentAmount.value, notes = paymentNotes, context = appContext, activity = activity)

                                }

                            }
                        }
                    }
                }
            }
        }

        val isShowLoanMemberList = squadViewModel.showLoanMembersPopup.collectAsStateWithLifecycle()

        if (isShowLoanMemberList.value) {
            OverlayBackgroundView(
                showPopup = isShowLoanMemberList,
                onDismiss = { squadViewModel.setShowLoanMembersPopupPopup(false) }
            ) {

                SingleSelectionPopupView(
                    listValues = squadViewModel.squadMemberNames.collectAsState().value,
                    title = "Members",
                    onItemSelected = { selectedValue ->
                        squadViewModel.setShowLoanMembersPopupPopup(false)
                        val member = CommonFunctions.getMember(
                            by = selectedValue,
                            from = squadViewModel.squadMembers.value
                        )
                        loanSelectedMember = member
                        loanSelectedMemberNameError = ""
                        squadViewModel.fetchMemberLoans(
                            showLoader = true,
                            memberID = member?.id ?: ""
                        ) { _, _ -> }
                    },
                    onCancelClick = {
                        squadViewModel.setShowLoanMembersPopupPopup(false)

                    }
                )
            }
        }

    }
}

private fun makeLoanPayment(
    activity : Activity,
    context : Context,
    squadViewModel: SquadViewModel,
    selectedMember: Member,
    selectedLoan: EMIConfiguration,
    handler : () -> Unit
) {

    squadViewModel.makeLoanPayment(
        activity = activity,
        context = context,
        selectedMember = selectedMember,
        selectedLoan = selectedLoan,
        ""
    ) { success, error ->

        if (success) {

            if (error == "UPI_OPENED") {
                handler()
            }

        } else {

            println("❌ Error adding payment: $error")
        }
    }
}

private fun handleOtherPayment(squadViewModel: SquadViewModel, amountStr: String, notes: String, context: Context, activity: Activity) {
    val availableAmount = squadViewModel.squad.value?.currentAvailableAmount ?: 0
    val amountInt = amountStr.toIntOrNull() ?: 0
    if (availableAmount < amountInt) {

        ToastManager.show(title = SquadStrings.appName, message = "Fund not available", type = ToastType.ERROR)
        return
    }
    LoaderManager.shared.showLoader()
    val otherID = CommonFunctions.generatePaymentID(squadViewModel.squad.value?.squadID ?: "")
    val newPayment = PaymentsDetails(
        id = otherID,
        paymentUpdatedDate = Date().asTimestamp,
        payoutUpdatedDate = null,

        memberId = "",
        memberName = "SQUAD MANAGER",
        paymentPhone = "",
        paymentEmail = "",

        userType = SquadUserType.SQUAD_MANAGER,

        amount = amountInt,
        intrestAmount = 0,

        paymentEntryType = PaymentEntryType.MANUAL_ENTRY,
        paymentType = PaymentType.PAYMENT_DEBIT,
        paymentSubType = PaymentSubType.OTHERS_AMOUNT,
        paymentStatus = PaymentStatus.INVERIFICATION,
        payoutStatus = PayoutStatus.PAYOUT_SUCCESS,
        paymentApproveStatus = PaymentApproveStatus.REQUESTED,
        description = notes,
        squadId = squadViewModel.squad.value?.squadID ?: "",
        contributionId = "",
        loanId = "",
        installmentId = "",
        order_id = otherID,
        transferMode = "",
        beneId = "",

        paymentSuccess = true,
        paymentResponseMessage = "",
        payoutSuccess = true,
        payoutResponseMessage = "",
        transferReferenceId = notes,

        recordStatus = RecordStatus.ACTIVE,
        recordDate = Date().asTimestamp
    )

    // 🔹 Save payment
    squadViewModel.savePayments(
        activity = activity,
        context = context,
        showLoader = false,
        squadID = squadViewModel.squad.value?.squadID ?: "",
        payment = listOf(newPayment)
    ) { success, error ->
        if (success) {
            println("✅ Payment added successfully!")

            squadViewModel.createSquadActivity(
                activityType = SquadActivityType.AMOUNT_DEBIT,
                userName = "CHIT MEMBER",
                memberId = newPayment.memberId,
                amount = amountInt,
                description = "Amount $amountStr debited for $notes"
            )
            LoaderManager.shared.hideLoader()

            ToastManager.show(title = SquadStrings.appName, message = "Payment updated", type = ToastType.SUCCESS)
        } else {
            println("❌ Error adding payment: $error")
        }
    }

    println("Processing Other Payment: $amountStr - Notes: $notes")
}

@Composable
fun PaymentEMIListRow(
    emi: EMIConfiguration,
    isSelected: Boolean,
    onClick: () -> Unit
) {

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.99f,
        animationSpec = tween(300),
        label = "cardScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .appShadow(AppShadows.card)
            .background(
                color = if (isSelected) AppColors.primaryButton.copy(alpha = 0.06f) else AppColors.surface,
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = if (isSelected) 1.6.dp else 1.dp,
                color = if (isSelected) AppColors.primaryButton else AppColors.border.copy(alpha = 0.4f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // MARK: Loan Amount + Tenure/Interest

            Column {

                Text(
                    "Loan Amount",
                    style = AppFont.ibmPlexSans(10, FontWeight.Medium),
                    color = AppColors.secondaryText
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    emi.loanAmount.currencyFormattedWithCommas(),
                    style = AppFont.ibmPlexSans(19, FontWeight.Bold),
                    color = AppColors.headerText
                )

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Text(
                        "${emi.emiMonths} Months",
                        style = AppFont.ibmPlexSans(10, FontWeight.Medium),
                        color = if (isSelected) AppColors.primaryButton else AppColors.primaryBrand
                    )

                    Spacer(Modifier.width(6.dp))

                    Box(
                        Modifier
                            .size(3.dp)
                            .background(AppColors.border, CircleShape)
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        "${"%.2f".format(emi.emiInterestRate)}% ${emi.interestType}",
                        style = AppFont.ibmPlexSans(10, FontWeight.Medium),
                        color = AppColors.secondaryText
                    )
                }
            }

            HorizontalDivider(color = AppColors.secondaryText.copy(alpha = 0.12f))

            // MARK: EMI / Interest / Total

            Row(modifier = Modifier.fillMaxWidth()) {

                InfoView(
                    modifier = Modifier.weight(1f),
                    title = "Monthly EMI",
                    value = emi.emiAmount.currencyFormattedWithCommas(),
                    valueColor = AppColors.infoAccent
                )

                InfoView(
                    modifier = Modifier.weight(1f),
                    title = "Total Interest",
                    value = emi.interestAmount.currencyFormattedWithCommas(),
                    valueColor = AppColors.errorAccent
                )

                InfoView(
                    modifier = Modifier.weight(1f),
                    title = "Total Payable",
                    value = (emi.loanAmount + emi.interestAmount).currencyFormattedWithCommas(),
                    valueColor = AppColors.headerText
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Icon(
            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Circle,
            contentDescription = null,
            tint = if (isSelected) AppColors.primaryButton else AppColors.border,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun InfoView(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    valueColor: Color
) {
    Column(modifier = modifier) {
        Text(
            title,
            style = AppFont.ibmPlexSans(10, FontWeight.Medium),
            color = AppColors.secondaryText
        )
        Spacer(Modifier.height(3.dp))
        Text(
            value,
            style = AppFont.ibmPlexSans(12, FontWeight.SemiBold),
            color = valueColor
        )
    }
}