package com.android.savingssquad.view

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.android.savingssquad.model.ContributionDetail
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.singleton.LoaderManager
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import kotlinx.coroutines.launch
import java.util.Date
import com.android.savingssquad.model.Installment
import com.android.savingssquad.model.InterestType
import com.android.savingssquad.model.LoanPaidType
import com.android.savingssquad.model.Member
import com.android.savingssquad.model.MemberOtherPayments
import com.android.savingssquad.model.PaymentsDetails
import com.android.savingssquad.model.ReminderRequest
import com.android.savingssquad.model.unpaidMonths
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.NotificationService
import com.android.savingssquad.singleton.SquadActivityType
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.PaidStatus
import com.android.savingssquad.singleton.PaymentApproveStatus
import com.android.savingssquad.singleton.PaymentEntryType
import com.android.savingssquad.singleton.PaymentStatus
import com.android.savingssquad.singleton.PaymentSubType
import com.android.savingssquad.singleton.PaymentType
import com.android.savingssquad.singleton.PayoutStatus
import com.android.savingssquad.singleton.RecordStatus
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.appShadow
import com.android.savingssquad.singleton.asTimestamp
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.android.savingssquad.viewmodel.ToastManager
import com.android.savingssquad.viewmodel.ToastType
import com.google.firebase.Timestamp
import com.yourapp.utils.CommonFunctions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import com.android.savingssquad.model.Squad
import com.android.savingssquad.singleton.MemberPaymentSubType
import com.yourapp.utils.IDGenerator


@Composable
fun ManualEntryView(
    navController: NavController?,
    squadViewModel: SquadViewModel) {
    val coroutineScope = rememberCoroutineScope()

    // ===== state (same names as SwiftUI) =====
    var contributionSelectedMember by remember { mutableStateOf<Member?>(null) }
    var contributionSelectedMemberName by remember { mutableStateOf("") }
    var contributionSelectedMonthYear by remember { mutableStateOf("") }

    var availableContributionMonths by remember { mutableStateOf(listOf<String>()) }

    var contributionAmountError by remember { mutableStateOf("") }
    var contributionSelectedMemberNameError by remember { mutableStateOf("") }
    var contributionSelectedMonthYearError by remember { mutableStateOf("") }

    // EMI section
    var emiSelectedMember by remember { mutableStateOf<Member?>(null) }
    var emiSelectedMemberName by remember { mutableStateOf("") }
    var emiSelectedMonthYear by remember { mutableStateOf("") }

    var availableEMIMonths by remember { mutableStateOf(listOf<String>()) }

    var emiAmountError by remember { mutableStateOf("") }
    var emiSelectedMemberNameError by remember { mutableStateOf("") }
    var emiSelectedMonthYearError by remember { mutableStateOf("") }
    var selectedInstallment by remember { mutableStateOf<Installment?>(null) }
    var selectedEMIInstallmentAmount by remember { mutableStateOf("") }


    var otherMemberPaymentSelectedMember by remember { mutableStateOf<Member?>(null) }
    var otherMemberPaymentSelectedMemberName by remember { mutableStateOf("") }
    var otherMemberPaymentSelectedMemberNameError by remember { mutableStateOf("") }
    var selectedMemberOtherPayment by remember { mutableStateOf<MemberOtherPayments?>(null) }


    var notes by remember { mutableStateOf("") }
    var notesError by remember { mutableStateOf("") }

    var selectedSegment by remember { mutableStateOf(SquadStrings.toMemberPayment) }

    var memberSubType by remember { mutableStateOf(SquadStrings.contribution) }


    // ===== viewmodel state (collected safely) =====
    val squad by squadViewModel.squad.collectAsState() // nullable
    val squadMembers by squadViewModel.squadMembers.collectAsState(initial = emptyList())
    val squadMemberNames by squadViewModel.squadMemberNames.collectAsState(initial = emptyList())
    val memberPendingLoans by squadViewModel.memberPendingLoans.collectAsState(initial = null)
    val selectedContributions by squadViewModel.selectedContributions.collectAsState(initial = emptyList())

    val paymentAmount = remember { mutableStateOf("") }

    var paymentAmountError by remember { mutableStateOf("") }

    var paymentNotes by remember { mutableStateOf("") }
    var paymentNotesError by remember { mutableStateOf("") }

    val activity = LocalContext.current as Activity
    val appContext = LocalContext.current.applicationContext

    fun validateFields(): Boolean {
        paymentAmountError = if (paymentAmount.value.isEmpty()) {
            SquadStrings.amountIsRequired
        } else ""

        paymentNotesError = if (paymentNotes.isEmpty()) {
            SquadStrings.noteIsRequired
        } else ""

        return paymentAmountError.isEmpty() &&
                paymentNotesError.isEmpty()
    }

    // fetch rules / initial data if needed
    LaunchedEffect(Unit) {
        // no-op or call any required fetches - keep parity with SwiftUI onAppear
    }

    // ===== UI =====
    Box(
        modifier = Modifier

            .fillMaxSize()

            .windowInsetsPadding(WindowInsets.safeDrawing)
    )
    {
        AppBackgroundGradient()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
        )
        {

            SSNavigationBar(
                title = SquadStrings.manualEntry,
                navController = navController,
                showBackButton = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            ModernSegmentedPickerView(
                segments = listOf(SquadStrings.toMemberPayment,SquadStrings.others),
                selectedSegment = selectedSegment,
                onSegmentSelected = { newSegment -> selectedSegment = newSegment }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedSegment == SquadStrings.toMemberPayment) {


                DropdownMenuPicker(
                    selected = memberSubType,
                    items = listOf(SquadStrings.contribution,SquadStrings.emi,SquadStrings.repayment),
                    icon = Icons.Default.Tune,
                ) { memberSubType = it }



                if (memberSubType == SquadStrings.contribution)  {

                    SectionView(title = SquadStrings.contributionEntry) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            // Member selection field (readonly but dropdown active)
                            SSTextField(
                                icon = Icons.Default.Person,
                                placeholder = if (contributionSelectedMemberName.isEmpty()) SquadStrings.selectSquadMember else contributionSelectedMemberName,
                                textState = remember { mutableStateOf(contributionSelectedMemberName) }, // keep display, but won't edit directly
                                keyboardType = KeyboardType.Text,
                                showDropdown = true,
                                error = contributionSelectedMemberNameError,
                                onDropdownTap = {
                                    squadViewModel.setShowContributionMemberPopup(true)
                                },
                                disabled = true

                            )

                            // Contribution month selector
                            SSTextField(
                                icon = Icons.Default.CalendarToday,
                                placeholder = if (contributionSelectedMonthYear.isEmpty()) SquadStrings.selectContributionDate else contributionSelectedMonthYear,
                                textState = remember { mutableStateOf(contributionSelectedMonthYear) },
                                keyboardType = KeyboardType.Text,
                                showDropdown = true,
                                error = contributionSelectedMonthYearError,
                                onDropdownTap = {
                                    if (contributionSelectedMemberName.isEmpty()) {
                                        ToastManager.show(title = SquadStrings.savingsSquad, message =  SquadStrings.pleaseSelectMember, type = ToastType.ERROR)


                                    } else {
                                        if (availableContributionMonths.isEmpty()) {

                                            ToastManager.show(title = SquadStrings.savingsSquad, message = SquadStrings.noOutstandingDues(contributionSelectedMemberName), type = ToastType.SUCCESS)


                                        } else {
                                            squadViewModel.setShowContributionMonthPopup(true)
                                        }
                                    }
                                },
                                disabled = true

                            )

                            // Contribution Amount readonly
                            SSTextField(
                                icon = Icons.Default.CheckCircle,
                                placeholder = (squad?.monthlyContribution ?: 0).toString(),
                                textState = remember { mutableStateOf((squad?.monthlyContribution ?: 0).toString()) },
                                keyboardType = KeyboardType.Number,
                                disabled = true)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SSButton(title = SquadStrings.updateContribution, isDisabled = false) {
                        // handleManualContribution mapping
                        if (validateContributionFields(
                                contributionSelectedMemberName,
                                contributionSelectedMonthYear,
                                contributionAmountError,
                                onSetMemberError = { contributionSelectedMemberNameError = it },
                                onSetMonthError = { contributionSelectedMonthYearError = it }
                            )
                        ) {
                            // performing the same async flow as SwiftUI
                            val selectedMember = contributionSelectedMember
                            val squadLocal = squad
                            if (selectedMember == null || squadLocal == null) {
                                // show error
                                return@SSButton
                            }

                            LoaderManager.shared.showLoader()
                            coroutineScope.launch(Dispatchers.IO) {
                                val contribution = selectedContributions.find { it.monthYear == contributionSelectedMonthYear }
                                val contributionID = contribution?.id
                                if (contribution == null || contributionID == null) {
                                    LoaderManager.shared.hideLoader()
                                    return@launch
                                }

                                val updatedContribution = ContributionDetail(
                                    id = contributionID,
                                    orderId = "",
                                    memberID = selectedMember.id ?: "",
                                    memberName = selectedMember.name,
                                    monthYear = contributionSelectedMonthYear,
                                    amount = squadLocal.monthlyContribution,
                                    paidOn = Date().asTimestamp,
                                    paidStatus = PaidStatus.PAID, // adjust enum mapping to your model
                                    paymentEntryType = PaymentEntryType.MANUAL_ENTRY,
                                    dueDate = CommonFunctions.getContributionDue(monthYear = contributionSelectedMonthYear).asTimestamp
                                )

                                squadViewModel.editContribution(
                                    showLoader = true,
                                    squadID = squadLocal.squadID,
                                    memberID = selectedMember.id ?: "",
                                    contributionID = contributionID,
                                    updatedContribution = updatedContribution
                                ) { success, message ->
                                    coroutineScope.launch(Dispatchers.IO) {
                                        if (success) {
                                            // async: create payments and activity like SwiftUI
                                            val newPayment = PaymentsDetails(
                                                id = IDGenerator.generatePaymentID(squadId = squadLocal.squadID),
                                                paymentUpdatedDate = Date().asTimestamp,
                                                memberId = selectedMember.id ?: "",
                                                memberName = contributionSelectedMemberName,
                                                paymentPhone = selectedMember.phoneNumber,
                                                paymentEmail = selectedMember.mailID ?: "",
                                                userType = SquadUserType.SQUAD_MEMBER,
                                                amount = squadLocal.monthlyContribution,
                                                intrestAmount = 0,
                                                paymentEntryType = PaymentEntryType.MANUAL_ENTRY,
                                                paymentType = PaymentType.PAYMENT_CREDIT,
                                                paymentSubType = PaymentSubType.CONTRIBUTION_AMOUNT,
                                                paymentStatus = PaymentStatus.SUCCESS,
                                                payoutStatus = PayoutStatus.PAYOUT_SUCCESS,
                                                paymentApproveStatus = PaymentApproveStatus.ACCEPTED,
                                                description = "Contribution payment for $contributionSelectedMemberName ($contributionSelectedMonthYear) updated by the squad manager." ,
                                                squadId = squadLocal.squadID,
                                                order_id = contributionID,
                                                contributionId = contributionID,
                                                loanId = "",
                                                installmentId = "",
                                                paymentSuccess = true,
                                                payoutSuccess = true,
                                                transferReferenceId = contributionID.split("-").lastOrNull() ?: ""
                                            )

                                            squadViewModel.savePayments(
                                                activity = activity,
                                                context = appContext,
                                                squadID = squadLocal.squadID,
                                                payment = listOf(newPayment)
                                            ) { pSuccess, pError ->
                                                // no-op logging
                                            }

                                            squadViewModel.createSquadActivity(
                                                activityType = SquadActivityType.AMOUNT_CREDIT,
                                                userName = newPayment.memberName,
                                                memberId = newPayment.memberId,
                                                amount = squadLocal.monthlyContribution,
                                                description = "Updated contribution for $contributionSelectedMemberName ($contributionSelectedMonthYear) — Amount: ${squad?.monthlyContribution?.currencyFormattedWithCommas()}"
                                            ) { success, error ->
                                                coroutineScope.launch(Dispatchers.Main) {
                                                    LoaderManager.shared.hideLoader()



                                                    ToastManager.show(title = SquadStrings.contributionUpdated, message = SquadStrings.contributionRecordedSuccessfully(contributionSelectedMemberName,contributionSelectedMonthYear),
                                                        ToastType.SUCCESS)

                                                    squadViewModel.squad?.let { squad ->

                                                        NotificationService.shared.sendMemberReminder(

                                                            request = ReminderRequest(

                                                                squadId = squad.value?.squadID
                                                                    ?: "",

                                                                memberIds = listOf(newPayment.memberId) ,

                                                                title = "Contribution Updated",

                                                                message = "Your contribution for $contributionSelectedMonthYear has been updated by the squad manager.",

                                                                data = mapOf(

                                                                    "screen" to "PAYMENT"

                                                                )

                                                            ),

                                                            onSuccess = { response ->
                                                            },

                                                            onError = { error ->
                                                            }

                                                        )
                                                    }

                                                    contributionSelectedMemberName = ""
                                                    contributionSelectedMonthYear = ""
                                                }
                                            }
                                        } else {
                                            coroutineScope.launch(Dispatchers.Main) {
                                                LoaderManager.shared.hideLoader()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else if (memberSubType == SquadStrings.emi)  {

                    SectionView(title = SquadStrings.emiEntry) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            // Member selection
                            SSTextField(
                                icon = Icons.Default.Person,
                                placeholder = if (emiSelectedMemberName.isEmpty()) SquadStrings.selectSquadMember else emiSelectedMemberName,
                                textState = remember { mutableStateOf(emiSelectedMemberName) },
                                keyboardType = KeyboardType.Text,
                                showDropdown = true,
                                error = emiSelectedMemberNameError,
                                onDropdownTap = { squadViewModel.setShowEMIMemberPopup(true) },
                                disabled = true)

                            if (emiSelectedMember?.currentLoanApproveStatus == EMIStatus.PENDING) {
                                SSTextField(
                                    icon = Icons.Default.CalendarToday,
                                    placeholder = if (emiSelectedMonthYear.isEmpty()) SquadStrings.selectEMI else emiSelectedMonthYear,
                                    textState = remember { mutableStateOf(emiSelectedMonthYear) },
                                    keyboardType = KeyboardType.Text,
                                    showDropdown = true,
                                    error = emiSelectedMonthYearError,
                                    onDropdownTap = { squadViewModel.setShowEMIMonthPopup(true) },
                                    disabled = true)

                                SSTextField(
                                    icon = Icons.Default.CheckCircle,
                                    placeholder = selectedEMIInstallmentAmount,
                                    textState = remember { mutableStateOf(selectedEMIInstallmentAmount) },
                                    keyboardType = KeyboardType.Number,
                                    disabled = true)
                            }
                            else {

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    ) {

                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = null,
                                            tint = AppColors.primaryBrand,
                                            modifier = Modifier.size(60.dp)
                                        )

                                        Text(
                                            text = SquadStrings.noPendingLoans,
                                            style = AppFont.ibmPlexSans(20, FontWeight.Bold),
                                            color = AppColors.headerText,
                                            textAlign = TextAlign.Center
                                        )

                                        Text(
                                            text = SquadStrings.noPendingPaymentsDescription(emiSelectedMemberName),
                                            style = AppFont.ibmPlexSans(14),
                                            color = AppColors.secondaryText,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SSButton(
                        title = SquadStrings.updateEMI,
                        isDisabled = emiSelectedMember?.currentLoanApproveStatus != EMIStatus.PENDING,
                        action = {
                            if (emiSelectedMember?.currentLoanApproveStatus != EMIStatus.PENDING) return@SSButton
                            // handleManualEMIPayment replicate similar to SwiftUI flow
                            coroutineScope.launch {
                                if (validateEMIFields(
                                        emiSelectedMemberName,
                                        emiSelectedMonthYear,
                                        emiAmountError,
                                        onSetMemberError = { emiSelectedMemberNameError = it },
                                        onSetMonthError = { emiSelectedMonthYearError = it }
                                    )
                                ) {
                                    LoaderManager.shared.showLoader()
                                    selectedInstallment?.let { installment ->
                                        installment.status = EMIStatus.PAID
                                        installment.duePaidDate = Date().asTimestamp
                                        squadViewModel.addOrUpdateInstallment(
                                            showLoader = true,
                                            memberID = emiSelectedMember?.id ?: "",
                                            loanID = memberPendingLoans?.firstOrNull()?.id ?: "",
                                            installment = installment
                                        ) { success, error ->
                                            if (success) {
                                                // create payments and activity similar to SwiftUI
                                                val loanNumber = memberPendingLoans?.firstOrNull()?.loanNumber ?: "N/A"
                                                val loanId = memberPendingLoans?.firstOrNull()?.id ?: "N/A"
                                                val loanPayment = PaymentsDetails(
                                                    id = IDGenerator.generatePaymentID(squadId = squad?.squadID ?: ""),
                                                    paymentUpdatedDate = Date().asTimestamp,
                                                    memberId = emiSelectedMember?.id ?: "",
                                                    memberName = emiSelectedMemberName,
                                                    paymentPhone = emiSelectedMember?.phoneNumber ?: "",
                                                    paymentEmail = emiSelectedMember?.mailID ?: "",
                                                    userType = SquadUserType.SQUAD_MANAGER,
                                                    amount = (selectedInstallment?.installmentAmount ?: 0),
                                                    intrestAmount = (selectedInstallment?.interestAmount ?: 0),
                                                    paymentEntryType = PaymentEntryType.MANUAL_ENTRY,
                                                    paymentType = PaymentType.PAYMENT_CREDIT,
                                                    paymentSubType = PaymentSubType.EMI_AMOUNT,
                                                    paymentStatus = PaymentStatus.SUCCESS,
                                                    payoutStatus = PayoutStatus.PAYOUT_SUCCESS,
                                                    paymentApproveStatus = PaymentApproveStatus.ACCEPTED,
                                                    description = "EMI payment for $emiSelectedMemberName - ${selectedInstallment!!.installmentNumber} of Loan #$loanNumber updated by the squad manager.",
                                                    squadId = squad?.squadID ?: "",
                                                    loanId = loanId,
                                                    installmentId = selectedInstallment?.id ?: "",
                                                    paymentSuccess = true,
                                                    payoutSuccess = true
                                                )
//

                                                squadViewModel.savePayments(activity = activity, context = appContext, squadID = squad?.squadID ?: "", payment = listOf(loanPayment)) { psuccess, perror ->
                                                    // no-op
                                                }

                                                val total = (selectedInstallment?.installmentAmount ?: 0) + (selectedInstallment?.interestAmount ?: 0)

                                                squadViewModel.createSquadActivity(
                                                    activityType = SquadActivityType.AMOUNT_CREDIT,
                                                    userName = emiSelectedMemberName,
                                                    memberId = loanPayment.memberId,
                                                    amount = total,
                                                    description = "Updated EMI payment for $emiSelectedMemberName — ${selectedInstallment!!.installmentNumber} for Loan #$loanNumber. Amount: ${total.currencyFormattedWithCommas()}."
                                                ) { success, error ->
                                                    coroutineScope.launch(Dispatchers.Main) {
                                                        LoaderManager.shared.hideLoader()

                                                        ToastManager.show(title = SquadStrings.emiPaymentUpdated, message = SquadStrings.emiPaymentRecordedSuccessfully(emiSelectedMemberName,selectedInstallment!!.installmentNumber),
                                                            ToastType.SUCCESS)

                                                        squadViewModel.squad?.let { squad ->

                                                            NotificationService.shared.sendMemberReminder(

                                                                request = ReminderRequest(

                                                                    squadId = squad.value?.squadID
                                                                        ?: "",

                                                                    memberIds = listOf(loanPayment.memberId) ,

                                                                    title = "EMI Payment Updated",

                                                                    message = "Your EMI payment for ${selectedInstallment!!.installmentNumber} has been updated by the squad manager.",

                                                                    data = mapOf(

                                                                        "screen" to "PAYMENT"

                                                                    )

                                                                ),

                                                                onSuccess = { response ->
                                                                },

                                                                onError = { error ->
                                                                }

                                                            )
                                                        }

                                                        emiSelectedMemberName = ""
                                                        emiSelectedMonthYear = ""
                                                        emiSelectedMember = null
                                                        emiSelectedMemberNameError = ""
                                                    }
                                                }
                                            } else {
                                                LoaderManager.shared.hideLoader()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
                else {

                    SectionView(title = "") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            // Member selection field (readonly but dropdown active)
                            SSTextField(
                                icon = Icons.Default.Person,
                                placeholder = if (otherMemberPaymentSelectedMemberName.isEmpty()) SquadStrings.selectSquadMember else otherMemberPaymentSelectedMemberName,
                                textState = remember { mutableStateOf(otherMemberPaymentSelectedMemberName) }, // keep display, but won't edit directly
                                keyboardType = KeyboardType.Text,
                                showDropdown = true,
                                error = otherMemberPaymentSelectedMemberNameError,
                                onDropdownTap = {
                                    squadViewModel.setOtherPaymentMemberPopup(true)
                                },
                                disabled = true

                            )

                            val memberOtherPayments by squadViewModel.memberOtherPayments.collectAsState()

                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            )
                            {

                                if (!memberOtherPayments.isNullOrEmpty()) {

                                    items(
                                        items = memberOtherPayments!!,
                                        key = { it.id ?: "" }
                                    ) { payment ->

                                        ManualMemberOtherPaymentRow(
                                            payments = payment,
                                            isSelected = selectedMemberOtherPayment?.id == payment.id,
                                            onClick = {
                                                selectedMemberOtherPayment =
                                                    if (selectedMemberOtherPayment?.id == payment.id) null
                                                    else payment
                                            }
                                        )
                                    }
                                }
                                else {

                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.padding(horizontal = 24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Verified,
                                                    contentDescription = null,
                                                    tint = AppColors.primaryBrand,
                                                    modifier = Modifier.size(60.dp)
                                                )

                                                Text(
                                                    text = SquadStrings.noPendingPayments,
                                                    style = AppFont.ibmPlexSans(20, FontWeight.Bold),
                                                    color = AppColors.headerText,
                                                    textAlign = TextAlign.Center
                                                )

                                                Text(
                                                    text = SquadStrings.noPendingPaymentsDescription(emiSelectedMemberName),
                                                    style = AppFont.ibmPlexSans(14),
                                                    color = AppColors.secondaryText,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            SSButton(title = SquadStrings.updatePayment, isDisabled = selectedMemberOtherPayment == null, action =   {

                                val squad = squadViewModel.squad ?: return@SSButton

                                LoaderManager.shared.showLoader()

                                CoroutineScope(Dispatchers.IO).launch {

                                    val otherID = IDGenerator.generatePaymentID(squad.value?.squadID
                                        ?: "")

                                    val amountInt = selectedMemberOtherPayment?.amount

                                    val payment = PaymentsDetails(
                                        id = otherID,
                                        paymentUpdatedDate = Timestamp.now(),

                                        memberId = selectedMemberOtherPayment?.memberId ?: "",
                                        memberName = selectedMemberOtherPayment?.memberName ?: "",
                                        paymentPhone = "",
                                        paymentEmail = "",

                                        userType = SquadUserType.SQUAD_MANAGER,

                                        amount = selectedMemberOtherPayment?.amount ?: 0,
                                        intrestAmount = 0,

                                        paymentEntryType = PaymentEntryType.MANUAL_ENTRY,
                                        paymentType = PaymentType.PAYMENT_CREDIT,
                                        paymentSubType = PaymentSubType.RE_PAYMENT,

                                        paymentStatus = PaymentStatus.SUCCESS,
                                        payoutStatus = PayoutStatus.PAYOUT_SUCCESS,
                                        paymentApproveStatus = PaymentApproveStatus.ACCEPTED,

                                        description = "${selectedMemberOtherPayment?.description} -  ${selectedMemberOtherPayment?.amount?.currencyFormattedWithCommas()}",

                                        squadId = squad.value?.squadID ?: "",

                                        order_id = otherID,
                                        contributionId = "",
                                        loanId = "",
                                        installmentId = "",

                                        paymentSuccess = true,
                                        payoutSuccess = true,

                                        transferReferenceId = selectedMemberOtherPayment?.description ?: "",
                                        memberOtherPaymentId = selectedMemberOtherPayment?.id ?: ""
                                    )

                                    squadViewModel.savePayments(
                                        activity = activity,
                                        context = appContext,
                                        squadID = squad.value?.squadID ?: "",
                                        payment = listOf(payment)
                                    ) { success, error ->

                                        if (success) {

                                            squadViewModel.createSquadActivity(
                                                activityType = SquadActivityType.AMOUNT_CREDIT,
                                                userName = selectedMemberOtherPayment?.memberName ?: "",
                                                memberId = payment.memberId,
                                                amount = selectedMemberOtherPayment?.amount ?: 0,
                                                description = "Recorded payment of ${amountInt?.currencyFormattedWithCommas()} . Note: $paymentNotes."
                                            ) { _, _ ->

                                                CoroutineScope(Dispatchers.Main).launch {

                                                    LoaderManager.shared.hideLoader()

                                                    ToastManager.show(
                                                        title = SquadStrings.paymentRecorded,
                                                        message = SquadStrings.paymentRecordedSuccessfully(
                                                            amountInt?.currencyFormattedWithCommas()
                                                                ?: "")
                                                    )

                                                    NotificationService.shared.sendMemberReminder(
                                                        request = ReminderRequest(
                                                            squadId = squad.value?.squadID ?: "",
                                                            memberIds = squadViewModel.squadMembers.value.mapNotNull { it.id },
                                                            title = "New Payment Recorded",
                                                            message = "Recorded payment of ${amountInt?.currencyFormattedWithCommas()}. Note: ${selectedMemberOtherPayment?.description}",
                                                            data = mapOf(
                                                                "screen" to "PAYMENT"
                                                            )
                                                        ),
                                                        onSuccess = {
                                                            // Optional: Log.d("Notification", "Reminder sent")
                                                        },
                                                        onError = { error ->

                                                        }
                                                    )

                                                    squadViewModel.fetchMemberOtherPayments(
                                                        showLoader = true,
                                                        memberID = selectedMemberOtherPayment?.memberId ?: "",
                                                        paidStatus = PaidStatus.NOT_PAID,
                                                        type = MemberPaymentSubType.RE_PAYMENT
                                                    ) { _, _ ->

                                                        LoaderManager.shared.hideLoader()
                                                    }

                                                    selectedMemberOtherPayment = null
                                                    otherMemberPaymentSelectedMemberName = ""
                                                    otherMemberPaymentSelectedMember = null
                                                }
                                            }

                                        } else {

                                            CoroutineScope(Dispatchers.Main).launch {
                                                LoaderManager.shared.hideLoader()
                                                Log.e("Payment", error ?: "Unknown Error")
                                            }
                                        }
                                    }
                                }
                            } )

                        }
                    }
                }
            }
            else {

                    SectionView(title = SquadStrings.otherPayments) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                            SSTextField(
                                icon = Icons.Default.CreditCard,
                                placeholder = SquadStrings.enterAmount,
                                textState = paymentAmount,
                                keyboardType = KeyboardType.Number,
                                error = paymentAmountError
                            )


                            SSTextView(
                                placeholder = SquadStrings.addANote,
                                text = paymentNotes,
                                onTextChange = { paymentNotes = it },
                                error = paymentNotesError,
                                maxCharacters = 200
                            )

                            SSButton(title = SquadStrings.updatePayment) {
                                /* handle other payment */

                                if (validateFields()) {
                                    handleOtherPayment(squadViewModel = squadViewModel,  amountStr = paymentAmount.value, notes = paymentNotes , activity = activity, context = appContext,  action = {
                                        LoaderManager.shared.hideLoader()
                                        val total = paymentAmount.value.toInt()
                                        ToastManager.show(title = SquadStrings.paymentRecorded, message = SquadStrings.paymentRecordedSuccessfully(total.currencyFormattedWithCommas()),
                                            ToastType.SUCCESS)


                                        squadViewModel.squad?.let { squad ->

                                            NotificationService.shared.sendMemberReminder(
                                                request = ReminderRequest(
                                                    squadId = squad.value?.squadID ?: "",
                                                    memberIds = squadViewModel.squadMembers.value.mapNotNull { it.id },
                                                    title = "New Payment Recorded",
                                                    message = "A payment of ${total.currencyFormattedWithCommas()} has been recorded by the squad manager. Note: $paymentNotes",
                                                    data = mapOf(
                                                        "screen" to "PAYMENT"
                                                    )
                                                ),
                                                onSuccess = {
                                                    // Optional: Log.d("Notification", "Reminder sent")
                                                },
                                                onError = { error ->

                                                }
                                            )
                                        }

                                        paymentAmount.value = ""
                                        paymentNotes = ""
                                    })

                                }

                            }
                        }
                    }

            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        val isShowContributionMemberList = squadViewModel.showContributionMemberPopup.collectAsStateWithLifecycle()

        if (isShowContributionMemberList.value) {
            OverlayBackgroundView(
                showPopup = remember { mutableStateOf(true) },
                onDismiss = { squadViewModel.setShowContributionMemberPopup(false) }
            ) {
                SingleSelectionPopupView(
                    listValues = squadMemberNames,
                    title = SquadStrings.members,
                    onItemSelected = { selectedValue ->
                        squadViewModel.setShowContributionMemberPopup(false)
                        contributionSelectedMemberName = selectedValue
                        contributionSelectedMember = CommonFunctions.getMember(by = selectedValue, from = squadMembers)
                        // fetch unpaid months for member
                        val member = contributionSelectedMember
                        val groupId = squad?.squadID
                        if (member != null && groupId != null) {
                            LoaderManager.shared.showLoader()
                            squadViewModel.fetchContributionsForMember(showLoader = true, squadID = groupId, memberID = member.id ?: "") { contributions, error ->
                                LoaderManager.shared.hideLoader()
                                if (contributions != null) {
                                    availableContributionMonths = contributions.unpaidMonths()
                                } else {
                                    availableContributionMonths = emptyList()
                                }
                            }
                        }
                    },
                    onCancelClick = {squadViewModel.setShowContributionMemberPopup(false)}
                )
            }
        }

        val isShowContributionMonthList = squadViewModel.showContributionMonthPopup.collectAsStateWithLifecycle()


        if (isShowContributionMonthList.value) {
            OverlayBackgroundView(
                showPopup = remember { mutableStateOf(true) },
                onDismiss = { squadViewModel.setShowContributionMonthPopup(false) }
            ) {
                SingleSelectionPopupView(
                    listValues = availableContributionMonths,
                    title = SquadStrings.pendingContributionMonths,
                    onItemSelected = { selectedValue ->
                        contributionSelectedMonthYear = selectedValue
                        squadViewModel.setShowContributionMonthPopup(false)
                    },
                    onCancelClick = {squadViewModel.setShowContributionMonthPopup(false)}
                )
            }
        }

        val isShowEMIMemberList = squadViewModel.showEMIMemberPopup.collectAsStateWithLifecycle()

        if (isShowEMIMemberList.value) {
            OverlayBackgroundView(
                showPopup = remember { mutableStateOf(true) },
                onDismiss = {  squadViewModel.setShowEMIMemberPopup(false) }
            ) {
                SingleSelectionPopupView(
                    listValues = squadMemberNames,
                    title = SquadStrings.members,
                    onItemSelected = { selectedValue ->
                        squadViewModel.setShowEMIMemberPopup(false)
                        emiSelectedMemberName = selectedValue
                        emiSelectedMember = CommonFunctions.getMember(by = selectedValue, from = squadMembers)
                        if (emiSelectedMemberName.isEmpty()) {

                            ToastManager.show(title = SquadStrings.savingsSquad, message = SquadStrings.pleaseSelectMember, type = ToastType.ERROR)

                        }
                        else {

                            LoaderManager.shared.showLoader()
                            squadViewModel.fetchMemberLoans(showLoader = true, memberID = emiSelectedMember?.id ?: "") { success, error ->
                                LoaderManager.shared.hideLoader()
                                // memberPendingLoans will be updated via viewmodel state
                            }
                        }
                    },
                    onCancelClick = {squadViewModel.setShowEMIMemberPopup(false)}
                )
            }
        }


        val isShowEMIMonthList = squadViewModel.showEMIMonthPopup.collectAsStateWithLifecycle()

        if (isShowEMIMonthList.value) {
            val loan = memberPendingLoans?.firstOrNull()
            OverlayBackgroundView(
                showPopup = remember { mutableStateOf(true) },
                onDismiss = {  squadViewModel.setShowEMIMonthPopup(false)}
            ) {

                if (loan != null) {

                    InstallmentPopupView(
                        title = loan.loanNumber,
                        loan = loan,
                        installments = loan.installments,
                        interestType = loan.emiConfiguration?.interestType ?: InterestType.YEARLY,
                        interestRate = loan.interest,
                        onSelect = { installment ->
                            selectedInstallment = installment
                            emiSelectedMonthYear = CommonFunctions.dateToString(date = installment.dueDate?.toDate() ?: Date(), format = "MMM yyyy")
                            selectedEMIInstallmentAmount = (installment.installmentAmount + installment.interestAmount).toString()
                            squadViewModel.setShowEMIMonthPopup(false)
                        },
                        onForceClose = { summary ->
                            val updatedLoan = loan.copy(
                                loanStatus = EMIStatus.PAID,
                                paidType = LoanPaidType.FORCECLOSED,
                                loanClosedDate = Timestamp.now()
                            )

                            LoaderManager.shared.showLoader()
                            squadViewModel.makeLoanForceClose(
                                activity = activity,
                                context = appContext,
                                member = emiSelectedMember,
                                loan = updatedLoan,
                                forceClosedInterest = summary.recalculatedInterest,
                                paymentEntryType = PaymentEntryType.MANUAL_ENTRY,
                                forceCloseSummary = summary,
                                description = "Loan ${loan.loanNumber} for ${emiSelectedMember?.name} was force closed by the squad manager."
                            )
                            { success, error ->

                                val pending = loan.installments.filter {
                                    it.status == EMIStatus.PENDING
                                }

                                val outstandingPrincipal = pending.sumOf {
                                    it.installmentAmount
                                }

                                val total = outstandingPrincipal + summary.recalculatedInterest
                                squadViewModel.createSquadActivity(
                                    activityType = SquadActivityType.AMOUNT_CREDIT,
                                    userName = emiSelectedMember?.name ?: "",
                                    memberId = loan.memberID,
                                    amount = total,
                                    description = "Force closed Loan #${loan.memberName} for ${loan.loanNumber}. Total settlement: ${total.currencyFormattedWithCommas()}."
                                ) { success, error ->
                                    coroutineScope.launch(Dispatchers.Main) {
                                        LoaderManager.shared.hideLoader()
                                        ToastManager.show(title = SquadStrings.loanClosed, message = SquadStrings.loanForceClosedSuccessfully(loan.memberName,loan.loanNumber),
                                            ToastType.SUCCESS)


                                        squadViewModel.squad?.let { squad ->

                                            NotificationService.shared.sendMemberReminder(

                                                request = ReminderRequest(

                                                    squadId = squad.value?.squadID
                                                        ?: "",

                                                    memberIds = listOf(loan.memberID),

                                                    title = "Loan Closed",

                                                    message = "Your loan #${loan.loanNumber} has been force closed by the squad manager.",

                                                    data = mapOf(

                                                        "screen" to "PAYMENT"

                                                    )

                                                ),

                                                onSuccess = { response ->
                                                },

                                                onError = { error ->
                                                }

                                            )
                                        }
                                    }
                                }

                                emiSelectedMemberName = ""
                                emiSelectedMonthYear = ""
                                emiSelectedMember = null
                                emiSelectedMemberNameError = ""


                            }
                        },
                        onCancel = { squadViewModel.setShowEMIMonthPopup(false) }
                    )

                }
                
            }
        }
    }
}

private fun handleOtherPayment(
    squadViewModel: SquadViewModel,
    amountStr: String,
    notes: String,
    activity: Activity,
    context: android.content.Context,
    action : () -> Unit
) {
    LoaderManager.shared.showLoader()
    val squad = squadViewModel.squad.value ?: return
    val amount = amountStr.toIntOrNull() ?: 0

        // 🔹 Create new payment
    val otherID = IDGenerator.generatePaymentID(squad.squadID)
        val newPayment = PaymentsDetails(
            id = otherID,
            paymentUpdatedDate = Date().asTimestamp,
            payoutUpdatedDate = null,

            memberId = "",
            memberName = "SQUAD MANAGER",
            paymentPhone = "",
            paymentEmail = "",

            userType = SquadUserType.SQUAD_MANAGER,

            amount = amount,
            intrestAmount = 0,

            paymentEntryType = PaymentEntryType.MANUAL_ENTRY,
            paymentType = PaymentType.PAYMENT_CREDIT,
            paymentSubType = PaymentSubType.OTHERS_AMOUNT,
            paymentStatus = PaymentStatus.SUCCESS,
            payoutStatus = PayoutStatus.PAYOUT_SUCCESS,
            paymentApproveStatus = PaymentApproveStatus.ACCEPTED,
            description = "$notes - ${amount.currencyFormattedWithCommas()} ",
            squadId = squad.squadID,
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
            showLoader = true,
            squadID = squad.squadID,
            payment = listOf(newPayment)
        ) { success, error ->
            if (success) {
                println("✅ Payment added successfully!")
                LoaderManager.shared.hideLoader()
                // 🔹 Record activity entry
                squadViewModel.createSquadActivity(
                    activityType = SquadActivityType.AMOUNT_CREDIT,
                    userName = "CHIT MANAGER",
                    memberId = newPayment.memberId,
                    amount = amount,
                    description = "Recorded payment of ${amount.currencyFormattedWithCommas()}. Note: $notes"
                ) { success, error ->
                    action()
                }
            } else {
                println("❌ Error adding payment: $error")
            }
        }


    println("Processing Other Payment: $amountStr - Notes: $notes")
}

// ===== Helper validation functions (mirrors SwiftUI functions) =====
private fun validateContributionFields(
    memberName: String,
    monthYear: String,
    contributionAmountError: String,
    onSetMemberError: (String) -> Unit,
    onSetMonthError: (String) -> Unit
): Boolean {
    onSetMemberError(if (memberName.trim().isEmpty()) SquadStrings.memberNameIsRequired else "")
    onSetMonthError(if (monthYear.trim().isEmpty()) SquadStrings.monthYearIsRequired else "")
    return memberName.trim().isNotEmpty() && monthYear.trim().isNotEmpty() && contributionAmountError.isEmpty()
}

private fun validateEMIFields(
    memberName: String,
    monthYear: String,
    emiAmountError: String,
    onSetMemberError: (String) -> Unit,
    onSetMonthError: (String) -> Unit
): Boolean {
    onSetMemberError(if (memberName.trim().isEmpty()) SquadStrings.memberNameIsRequired else "")
    onSetMonthError(if (monthYear.trim().isEmpty()) SquadStrings.monthYearIsRequired else "")
    return memberName.trim().isNotEmpty() && monthYear.trim().isNotEmpty() && emiAmountError.isEmpty()
}

@Composable
fun ManualMemberOtherPaymentRow(
    payments: MemberOtherPayments,
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
    )
    {

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // MARK: Loan Amount + Tenure/Interest

            Column {

                Text(
                    payments.description,
                    style = AppFont.ibmPlexSans(10, FontWeight.Medium),
                    color = AppColors.secondaryText
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    payments.amount.currencyFormattedWithCommas(),
                    style = AppFont.ibmPlexSans(19, FontWeight.Bold),
                    color = AppColors.headerText
                )

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Text(
                        SquadStrings.paymentSentOn,
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
                        CommonFunctions.dateToString(payments.amountReceivedDate?.toDate() ?: Date()),
                        style = AppFont.ibmPlexSans(10, FontWeight.Medium),
                        color = AppColors.secondaryText
                    )
                }
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