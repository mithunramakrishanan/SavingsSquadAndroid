package com.android.savingssquad.view

import android.app.Activity
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.android.savingssquad.viewmodel.SquadViewModel
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.savingssquad.singleton.LoaderManager
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import java.util.Date
import com.android.savingssquad.model.Squad
import com.android.savingssquad.model.Installment
import com.android.savingssquad.model.InterestType
import com.android.savingssquad.model.LoanPaidType
import com.android.savingssquad.model.Member
import com.android.savingssquad.model.MemberOtherPayments
import com.android.savingssquad.model.PaymentsDetails
import com.android.savingssquad.model.unpaidMonths
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.MemberPaymentSubType
import com.android.savingssquad.singleton.PaidStatus
import com.android.savingssquad.singleton.PaymentApproveStatus
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.PaymentEntryType
import com.android.savingssquad.singleton.PaymentStatus
import com.android.savingssquad.singleton.PaymentSubType
import com.android.savingssquad.singleton.PaymentType
import com.android.savingssquad.singleton.RemainderType
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.android.savingssquad.viewmodel.AppDestination
import com.google.firebase.Timestamp
import com.yourapp.utils.CommonFunctions
import com.android.savingssquad.viewmodel.ToastManager
import com.android.savingssquad.viewmodel.ToastType
import com.yourapp.utils.IDGenerator

// MemberPaymentScreen.kt
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MemberPaymentView(
    navController: NavController, // if you need nav actions, else pass null
    squadViewModel: SquadViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    // UI state
    var contributionSelectedMonthYear by remember { mutableStateOf("") }
    var availableContributionMonths by remember { mutableStateOf(listOf<String>()) }
    var contributionAmountError by remember { mutableStateOf("") }
    var contributionSelectedMonthYearError by remember { mutableStateOf("") }

    var emiSelectedMonthYear by remember { mutableStateOf("") }
    var availableEMIMonths by remember { mutableStateOf(listOf<String>()) }
    var emiAmountError by remember { mutableStateOf("") }
    var emiSelectedMonthYearError by remember { mutableStateOf("") }

    var selectedInstallment by remember { mutableStateOf<Installment?>(null) }

    var memberPaymentSegment by remember { mutableStateOf(SquadStrings.contribution) }

    // Collect flows safely
    val currentMember by squadViewModel.currentMember.collectAsStateWithLifecycle()
    val squad by squadViewModel.squad.collectAsStateWithLifecycle()
    val memberPendingLoans by squadViewModel.memberPendingLoans.collectAsStateWithLifecycle()
    val selectedContributions by squadViewModel.selectedContributions.collectAsStateWithLifecycle()
    val squadPayments by squadViewModel.squadPayments.collectAsStateWithLifecycle()

    val memberOtherPayments by squadViewModel.memberOtherPayments.collectAsStateWithLifecycle()


    // Payments list for "Recent Payments" similar to SwiftUI logic (current month)
    var payments by remember { mutableStateOf(listOf<PaymentsDetails>()) }

    val activity = LocalContext.current as Activity
    val appContext = LocalContext.current.applicationContext

    // On first composition fetch payments and reset UI states
    LaunchedEffect(Unit) {
        // 1️⃣ Fetch payments
        squadViewModel.fetchPayments(showLoader = true) { success, _ ->
            payments = getCurrentMonthPayments(squadViewModel.squadPayments.value)
                .filter { it.paymentStatus == PaymentStatus.SUCCESS }
                .let { list ->
                    val memberId = squadViewModel.currentMember.value?.id
                    if (memberId != null) list.filter { it.memberId == memberId } else list
                }
        }

        // 2️⃣ Reset selection state (equivalent to SwiftUI onAppear reset)
        selectedInstallment = null
        emiSelectedMonthYear = ""
        contributionSelectedMonthYear = ""
        availableContributionMonths = emptyList()

        // 3️⃣ Handle "remainder" logic similar to SwiftUI
        val remainder = UserDefaultsManager.getRemainder()
        if (remainder != null) {
            UserDefaultsManager.removeRemainder()
            when (remainder.remainderType) {
                RemainderType.CONTRIBUTION -> {
                    memberPaymentSegment = SquadStrings.contribution
//                    contributionSelectedMonthYear = CommonFunctions.dateToString(
//                        date = remainder.remainderDueDate?.toDate() ?: Date(),
//                        format = "MMM yyyy"
//                    )

                    LoaderManager.shared.showLoader()
                    val gfId = currentMember?.squadID ?: ""
                    val memberId = currentMember?.id ?: ""
                    squadViewModel.fetchContributionsForMember(showLoader = true, squadID = gfId, memberID = memberId) { contributions, error ->
                        LoaderManager.shared.hideLoader()
                        if (contributions != null) {
                            availableContributionMonths = contributions.unpaidMonths()
                            squadViewModel.setShowContributionMonthPopup(true)

                        } else {
                            availableContributionMonths = emptyList()

                            ToastManager.show(title = SquadStrings.savingsSquad, message = SquadStrings.noOutstandingDues(currentMember?.name ?: ""), type = ToastType.SUCCESS)

                        }
                    }

                }
                RemainderType.EMI -> {
                    memberPaymentSegment = SquadStrings.emi

                    squadViewModel.fetchMemberLoans(
                        showLoader = true,
                        memberID = currentMember?.id ?: ""
                    ) { success, _ ->

                        if (success) {
                            squadViewModel.setShowEMIMonthPopup(true)
                        }
                    }

//                    val installment = squadViewModel.memberPendingLoans.value?.firstOrNull()?.installments
//                        ?.firstOrNull { it.id == remainder.remainderID }
//                    if (installment != null) {
//                        selectedInstallment = installment
//                        emiSelectedMonthYear = CommonFunctions.dateToString(
//                            date = remainder.remainderDueDate?.toDate() ?: Date(),
//                            format = "MMM yyyy"
//                        )
//                    }
                }

                RemainderType.OTHER_REMAINDER -> {

                    memberPaymentSegment = SquadStrings.otherPayments

                    squadViewModel.fetchMemberOtherPayments(showLoader = true, memberID = currentMember?.id
                        ?: "", paidStatus = PaidStatus.NOT_PAID, type = MemberPaymentSubType.RE_PAYMENT) { _, _ ->
                        LoaderManager.shared.hideLoader()
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier

            .fillMaxSize()

            .windowInsetsPadding(WindowInsets.safeDrawing)
    )
    {
        AppBackgroundGradient()

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp))
        {
            SSNavigationBar(title = SquadStrings.payment, navController = navController, showBackButton = false)

            Spacer(modifier = Modifier.height(12.dp))

            ModernSegmentedPickerView(
                segments = listOf(SquadStrings.contribution, SquadStrings.emi,
                    SquadStrings.otherPayments),
                selectedSegment = memberPaymentSegment,
                onSegmentSelected = {

                    memberPaymentSegment = it

                    if (memberPaymentSegment == SquadStrings.otherPayments) {

                        squadViewModel.fetchMemberOtherPayments(showLoader = true, memberID = currentMember?.id
                            ?: "", paidStatus = PaidStatus.NOT_PAID, type = MemberPaymentSubType.RE_PAYMENT) { _, _ ->
                            LoaderManager.shared.hideLoader()
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Content
            Column(modifier = Modifier.fillMaxWidth()) {
                if (memberPaymentSegment == SquadStrings.contribution) {
                    // Contribution flow
                    ContributionSection(
                        currentMember = currentMember,
                        contributionSelectedMonthYear = contributionSelectedMonthYear,
                        onOpenMonthList = {
                            // fetch contributions for member and show list
                            LoaderManager.shared.showLoader()
                            val gfId = currentMember?.squadID ?: ""
                            val memberId = currentMember?.id ?: ""
                            squadViewModel.fetchContributionsForMember(showLoader = true, squadID = gfId, memberID = memberId) { contributions, error ->
                                LoaderManager.shared.hideLoader()
                                if (contributions != null) {
                                    availableContributionMonths = contributions.unpaidMonths()
                                    squadViewModel.setShowContributionMonthPopup(true)

                                } else {
                                    availableContributionMonths = emptyList()

                                    ToastManager.show(title = SquadStrings.savingsSquad, message =  SquadStrings.noOutstandingDues(currentMember?.name ?: ""), type = ToastType.SUCCESS)

                                }
                            }
                        },
                        contributionAmount = squad?.monthlyContribution ?: 0,
                        contributionSelectedMonthYearError = contributionSelectedMonthYearError
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ContributionButton(
                        upiID = squad?.upiID ?: "",
                        onClick = {
                            if (validateContributionFields(contributionSelectedMonthYear) { contributionSelectedMonthYearError = it }) {
                                // Payment flow
                                LoaderManager.shared.showLoader()
                                val gf = squad ?: return@ContributionButton
                                val selectedMember = currentMember ?: return@ContributionButton
                                val contribution = selectedContributions.firstOrNull { it.monthYear == contributionSelectedMonthYear }
                                val contributionID = contribution?.id
                                if (contribution == null || contributionID.isNullOrEmpty()) {
                                    // missing contribution
                                    LoaderManager.shared.hideLoader()
                                    return@ContributionButton
                                }



                                val newPayment = PaymentsDetails(
                                    id = IDGenerator.generatePaymentID(squadId = gf.squadID),
                                    paymentUpdatedDate = Timestamp(date = Date()),
                                    memberId = selectedMember.id ?: "",
                                    memberName = selectedMember.name,
                                    paymentPhone = selectedMember.phoneNumber,
                                    paymentEmail = selectedMember.mailID ?: "",
                                    userType = SquadUserType.SQUAD_MEMBER, // adapt enum mapping
                                    amount = gf.monthlyContribution,
                                    intrestAmount = 0,
                                    paymentEntryType = PaymentEntryType.AUTOMATIC_ENTRY,
                                    paymentType = PaymentType.PAYMENT_CREDIT,
                                    paymentSubType = PaymentSubType.CONTRIBUTION_AMOUNT,
                                    paymentStatus = PaymentStatus.INVERIFICATION,
                                    paymentApproveStatus = PaymentApproveStatus.REQUESTED,
                                    description = "Contribution for $contributionSelectedMonthYear.",
                                    squadId = gf.squadID,
                                    order_id = contributionID,
                                    contributionId = contributionID,
                                    loanId = "",
                                    installmentId = "",
                                    paymentResponseMessage = "Pending admin verification.",
                                    transferReferenceId = contributionID.split("-").lastOrNull() ?: "",
                                    upiID = gf.upiID
                                )


                                squadViewModel.savePayments(
                                    activity = activity,
                                    context = appContext,
                                    showLoader = true,
                                    squadID = squad!!.squadID,
                                    payment = listOf(newPayment)
                                ) { success, error ->
                                    if (success) {
                                        println("✅ Payment added successfully!")

                                        if (error == "UPI_OPENED") {
                                            contributionSelectedMonthYear = ""
                                        }
                                    } else {
                                        println("❌ Error adding payment: $error")
                                    }
                                }


                                // create or retry payment
                                /*if (contribution.orderId.isEmpty()) {
                                    Log.d("Cashfree Payment Flow", "New Payment")
                                    FirebaseFunctionsManager.shared.processRazorPayPayment(
                                        squadId = gf.squadID,
                                        action = RazorpayPaymentAction.New(payment = newPayment)
                                    ) { sessionId, orderId, error ->

                                        squadViewModel.handleCashFreeResponse(
                                            sessionId, orderId, error,
                                            completion = {
                                                LoaderManager.shared.hideLoader()
                                                contributionSelectedMonthYear = ""
                                            }
                                        )
                                    }
                                }
                                else {
                                    Log.d("Cashfree Payment Flow", "Retry Payment")

                                    FirebaseFunctionsManager.shared.processRazorPayPayment(
                                        squadId = gf.squadID,
                                        action = RazorpayPaymentAction.Retry(contribution.orderId)
                                    ) { sessionId, orderId, error ->

                                        squadViewModel.handleCashFreeResponse(
                                            sessionId, orderId, error,
                                            completion = {
                                                LoaderManager.shared.hideLoader()
                                                contributionSelectedMonthYear = ""
                                            }
                                        )
                                    }
                                } */
                            }
                        }
                    )
                }
                else if (memberPaymentSegment == SquadStrings.emi) {
                    // EMI flow
                    EMISection(
                        currentMember = currentMember,
                        squad = squad,
                        isPendingLoanAvailable = if (currentMember?.currentLoanApproveStatus == EMIStatus.PENDING) {true}else {false},
                        emiSelectedMonthYear = emiSelectedMonthYear,
                        emiSelectedMonthYearError = emiSelectedMonthYearError,
                        selectedInstallment = selectedInstallment,
                        onOpenInstallmentList = {
                            // show installment popup

                            squadViewModel.fetchMemberLoans(
                                showLoader = true,
                                memberID = currentMember?.id ?: ""
                            ) { success, _ ->

                                if (success) {
                                    squadViewModel.setShowEMIMonthPopup(true)
                                }
                            }


                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    EMIButton(
                        isDisabled = !(currentMember?.currentLoanApproveStatus == EMIStatus.PENDING),
                        onClick = {
                            if (validateEMIFields(emiSelectedMonthYear) { emiSelectedMonthYearError = it }) {
                                // Manual EMI payment flow (mirrors SwiftUI)

                                LoaderManager.shared.showLoader()

//                                selectedInstallment?.status = EMIStatus.PAID
//                                selectedInstallment?.duePaidDate = Date().asTimestamp

                                val gf = squad ?: return@EMIButton
                                val member = currentMember ?: return@EMIButton
                                val loan = memberPendingLoans?.firstOrNull()
                                val loanId = loan?.id ?: ""
                                val installId = selectedInstallment?.id ?: ""
                                val total = (selectedInstallment?.installmentAmount ?: 0) + (selectedInstallment?.interestAmount ?: 0)

                                val loanPayment = PaymentsDetails(
                                    id = IDGenerator.generatePaymentID(squadId = gf.squadID),
                                    paymentUpdatedDate = Timestamp(date = Date()),
                                    memberId = member.id ?: "",
                                    memberName = member.name,
                                    paymentPhone = member.phoneNumber,
                                    paymentEmail = member.mailID ?: "",
                                    userType = SquadUserType.SQUAD_MEMBER,
                                    amount = selectedInstallment?.installmentAmount ?: 0,
                                    intrestAmount = selectedInstallment?.interestAmount ?: 0,
                                    paymentEntryType = PaymentEntryType.AUTOMATIC_ENTRY,
                                    paymentType = PaymentType.PAYMENT_CREDIT,
                                    paymentSubType = PaymentSubType.EMI_AMOUNT,
                                    paymentStatus = PaymentStatus.INVERIFICATION,
                                    paymentApproveStatus = PaymentApproveStatus.REQUESTED,
                                    description = "EMI and Interest - ${selectedInstallment?.installmentNumber ?: ""} for #${loan?.loanNumber ?: "N/A"} ${total.currencyFormattedWithCommas()}",
                                    squadId = gf.squadID,
                                    order_id = "${selectedInstallment?.installmentNumber ?: ""} - ${loan?.loanNumber ?: "N/A"}",
                                    contributionId = "",
                                    loanId = loanId,
                                    installmentId = installId,
                                    paymentResponseMessage = "Pending admin verification.",
                                    transferReferenceId = "${selectedInstallment?.installmentNumber ?: ""} for #${loan?.loanNumber ?: "N/A"}",
                                    upiID = squad!!.upiID
                                )


                                squadViewModel.savePayments(
                                    activity = activity,
                                    context = appContext,
                                    showLoader = true,
                                    squadID = squad!!.squadID,
                                    payment = listOf(loanPayment)
                                ) { success, error ->
                                    if (success) {
                                        println("✅ Payment added successfully!")

                                        if (error == "UPI_OPENED") {
                                            selectedInstallment = null
                                            emiSelectedMonthYear = ""
                                        }
                                    } else {
                                        println("❌ Error adding payment: $error")
                                    }
                                }

//                                if (!selectedInstallment?.orderId.isNullOrEmpty()) {
//
//                                    FirebaseFunctionsManager.shared.processRazorPayPayment(
//                                        squadId = gf.squadID,
//                                        action = RazorpayPaymentAction.Retry(selectedInstallment!!.orderId!!)
//                                    ) { sessionId, orderId, error ->
//
//                                        squadViewModel.handleCashFreeResponse(
//                                            sessionId, orderId, error,
//                                            completion = {
//                                                LoaderManager.shared.hideLoader()
//                                                selectedInstallment = null
//                                                emiSelectedMonthYear = ""
//                                            }
//                                        )
//                                    }
//                                } else {
//
//                                    FirebaseFunctionsManager.shared.processRazorPayPayment(
//                                        squadId = gf.squadID,
//                                        action = RazorpayPaymentAction.New(loanPayment)
//                                    ) { sessionId, orderId, error ->
//                                        squadViewModel.handleCashFreeResponse(
//                                            sessionId, orderId, error,
//                                            completion = {
//                                                LoaderManager.shared.hideLoader()
//                                                selectedInstallment = null
//                                                emiSelectedMonthYear = ""
//
//                                            }
//                                        )
//                                    }
//                                }
                            }
                        }
                    )
                }
                else {

                    if (memberOtherPayments.isEmpty()) {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        )
                        {
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
                                    text = SquadStrings.noPendingPaymentsDescription(currentMember?.name
                                        ?: ""),
                                    style = AppFont.ibmPlexSans(14),
                                    color = AppColors.secondaryText,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    else {

                        LazyColumn {

                            items(

                                items = memberOtherPayments,

                                key = { it.id ?: "" }

                            ) { payment ->

                                MemberOtherPaymentRow(payment = payment, onPay = {

                                    squadViewModel.currentMember.value?.let { member ->

                                        squadViewModel.makeMemberRepay(
                                            member = member,
                                            payment = payment,
                                            activity = activity,
                                            context = appContext
                                        ) { success, error ->

                                        }
                                    }
                                })

                            }

                        }
                    }


                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    )
                    {
                        ViewAllButton(SquadStrings.checkAllPayments) {

                            navController.navigate(AppDestination.MEMBER_OTHER_PAYMENT.route)
                        }

                    }

                }
            }

            Spacer(modifier = Modifier.height(20.dp))
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
                    onCancelClick = {squadViewModel.setShowContributionMonthPopup(false)},  enableOnlyFirstIndex = true
                )
            }
        }


        val isShowEMIMonthList = squadViewModel.showEMIMonthPopup.collectAsStateWithLifecycle()

        if (isShowEMIMonthList.value) {
            val loan = memberPendingLoans?.firstOrNull()

            OverlayBackgroundView(
                showPopup = remember { mutableStateOf(true) },
                onDismiss = { squadViewModel.setShowEMIMonthPopup(false) }
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
                            emiSelectedMonthYear = CommonFunctions.dateToString(
                                date = installment.dueDate?.toDate() ?: Date(),
                                format = "MMM yyyy"
                            )
                            squadViewModel.setShowEMIMonthPopup(false)
                        },
                        onForceClose = { summary ->
                            val updatedLoan = loan.copy(
                                loanStatus = EMIStatus.PAID,
                                paidType = LoanPaidType.FORCECLOSED,
                                loanClosedDate = Timestamp.now()
                            )

                            squadViewModel.makeLoanForceClose(
                                activity = activity,
                                context = appContext,
                                member = squadViewModel.currentMember.value,
                                loan = updatedLoan,
                                forceClosedInterest = summary.recalculatedInterest,
                                paymentEntryType = PaymentEntryType.AUTOMATIC_ENTRY,
                                forceCloseSummary = summary,
                                description = "Force Closed ${loan.loanNumber})"
                            ) { success, error ->

                                selectedInstallment = null
                                emiSelectedMonthYear = ""
                            }
                        },
                        onCancel = { squadViewModel.setShowEMIMonthPopup(false) }
                    )
                }
            }
        }
    }
}

/* ---------- Helper composables for modularity ---------- */

@Composable
private fun ContributionSection(
    currentMember: Member?,
    contributionSelectedMonthYear: String,
    onOpenMonthList: () -> Unit,
    contributionAmount: Int,
    contributionSelectedMonthYearError: String
) {
    SectionView(title = SquadStrings.payYourContribution) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(15.dp)) {
            // Member (disabled)
            SSTextField(
                icon = Icons.Default.Person,
                placeholder = currentMember?.name ?: "",
                textState = remember { mutableStateOf("") }, // blank because it's disabled
                keyboardType = KeyboardType.Text,
                disabled = true
            )

            // Month picker (disabled text + dropdown action)
            SSTextField(
                icon = Icons.Default.CalendarToday,
                placeholder = if (contributionSelectedMonthYear.isEmpty()) SquadStrings.selectContributionDate else contributionSelectedMonthYear,
                textState = remember { mutableStateOf("") },
                keyboardType = KeyboardType.Text,
                disabled = true,
                showDropdown = true,
                onDropdownTap = onOpenMonthList,
                error = contributionSelectedMonthYearError
            )

            // Amount
            SSTextField(
                icon = Icons.Default.CreditCard,
                placeholder = SquadStrings.contributionAmount,
                textState = remember { mutableStateOf(contributionAmount.toString()) },
                keyboardType = KeyboardType.Number,
                disabled = true
            )
        }
    }
}

@Composable
private fun ContributionButton(upiID: String, onClick: () -> Unit) {
    Column {
        SSButton(title = SquadStrings.payContribution, isDisabled = upiID.isEmpty(), action = onClick)
        Spacer(modifier = Modifier.height(8.dp))

        if (upiID.trim().isEmpty()) {

            Text(

                text = "UPI ID not available for this squad yet",

                style = AppFont.ibmPlexSans(12, FontWeight.Normal),

                color = AppColors.errorAccent,

                textAlign = TextAlign.Center,

                modifier = Modifier

                    .fillMaxWidth()

                    .padding(horizontal = 16.dp)

            )

        } else {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                Text(
                    text = SquadStrings.paymentWillBeSentTo,
                    style = AppFont.ibmPlexSans(12, FontWeight.Normal),
                    color = AppColors.secondaryText,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = upiID,
                    style = AppFont.ibmPlexSans(13, FontWeight.Medium),
                    color = AppColors.successAccent,
                    textAlign = TextAlign.Center
                )
            }

        }
    }
}

@Composable
private fun EMISection(
    currentMember: Member?,
    squad: Squad?,
    isPendingLoanAvailable: Boolean,
    emiSelectedMonthYear: String,
    emiSelectedMonthYearError: String,
    selectedInstallment: Installment?,
    onOpenInstallmentList: () -> Unit
) {
    SectionView(title = SquadStrings.payYourEMI) {
        Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
            SSTextField(
                icon = Icons.Default.Person,
                placeholder = currentMember?.name ?: SquadStrings.member,
                textState = remember { mutableStateOf("") },
                keyboardType = KeyboardType.Text,
                disabled = true
            )

            if (squad?.upiID.isNullOrEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = SquadStrings.managerUpiIdError,
                            style = AppFont.ibmPlexSans(14, FontWeight.Normal),
                            color = Color.Red,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.Red.copy(alpha = 0.8f),
                            modifier = Modifier.size(40.dp)
                        )
                    }


                }
            }
            else if (isPendingLoanAvailable) {
                SSTextField(
                    icon = Icons.Default.CalendarToday,
                    placeholder = if (emiSelectedMonthYear.isEmpty()) SquadStrings.selectEMI else emiSelectedMonthYear,
                    textState = remember { mutableStateOf("") },
                    keyboardType = KeyboardType.Text,
                    disabled = true,
                    showDropdown = true,
                    onDropdownTap = onOpenInstallmentList,
                    error = emiSelectedMonthYearError
                )

                SSTextField(
                    icon = Icons.Default.CreditCard,
                    placeholder = SquadStrings.emiAmount,
                    textState = remember(selectedInstallment) {
                        mutableStateOf(
                            ((selectedInstallment?.installmentAmount ?: 0) +
                                    (selectedInstallment?.interestAmount ?: 0)).toString()
                        )
                    },
                    keyboardType = KeyboardType.Number,
                    disabled = true
                )
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
                            text = SquadStrings.noPendingPaymentsDescription(currentMember?.name ?: ""),
                            style = AppFont.ibmPlexSans(14),
                            color = AppColors.secondaryText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EMIButton(isDisabled: Boolean, onClick: () -> Unit) {
    Column {
        SSButton(title = SquadStrings.payEMI, isDisabled = isDisabled, action = onClick)
    }
}

/* ---------- Validation helpers ---------- */

private fun validateContributionFields(
    contributionSelectedMonthYear: String,
    onSetError: (String) -> Unit
): Boolean {
    val trimmed = contributionSelectedMonthYear.trim()
    return if (trimmed.isEmpty()) {
        onSetError("Month-Year is required")
        false
    } else {
        onSetError("")
        true
    }
}

private fun validateEMIFields(
    emiSelectedMonthYear: String,
    onSetError: (String) -> Unit
): Boolean {
    val trimmed = emiSelectedMonthYear.trim()
    return if (trimmed.isEmpty()) {
        onSetError("Month-Year is required")
        false
    } else {
        onSetError("")
        true
    }
}

@Composable
fun MemberOtherPaymentRow(
    payment: MemberOtherPayments,
    onPay: (() -> Unit)? = null
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.surface
        ),
        border = BorderStroke(
            1.dp,
            AppColors.border.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // Description

            Text(
                text = payment.description,
                style = AppFont.ibmPlexSans(10, FontWeight.Medium),
                color = AppColors.secondaryText
            )

            // Amount

            Text(
                text = payment.amount.currencyFormattedWithCommas(),
                style = AppFont.ibmPlexSans(20, FontWeight.Bold),
                color = AppColors.headerText
            )

            // Received Date

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = AppColors.secondaryText,
                    modifier = Modifier.size(14.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    SquadStrings.received,
                    style = AppFont.ibmPlexSans(10, FontWeight.Medium),
                    color = AppColors.secondaryText
                )

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .background(AppColors.border, CircleShape)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    CommonFunctions.dateToString(payment.amountReceivedDate?.toDate() ?: Date()),
                    style = AppFont.ibmPlexSans(10, FontWeight.Medium),
                    color = AppColors.secondaryText
                )
            }

            HorizontalDivider()

            when (payment.memberOtherPaymentType) {

                MemberPaymentSubType.RE_PAYMENT -> {

                    when (payment.paidStatus) {

                        PaidStatus.PAID -> {

                            Row(verticalAlignment = Alignment.CenterVertically) {

                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(15.dp)
                                )

                                Spacer(Modifier.width(4.dp))

                                Text(
                                    SquadStrings.repaid,
                                    style = AppFont.ibmPlexSans(10, FontWeight.SemiBold),
                                    color = Color(0xFF2E7D32)
                                )

                                Spacer(Modifier.width(6.dp))

                                Box(
                                    modifier = Modifier
                                        .size(3.dp)
                                        .background(AppColors.border, CircleShape)
                                )

                                Spacer(Modifier.width(6.dp))

                                Text(
                                    CommonFunctions.dateToString(
                                        payment.amountRepaidDate?.toDate() ?: Date()
                                    ),
                                    style = AppFont.ibmPlexSans(10, FontWeight.Medium),
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }

                        PaidStatus.INVERIFICATION -> {

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Icon(
                                        imageVector = Icons.Default.HourglassTop,
                                        contentDescription = null,
                                        tint = Color(0xFFFF9800),
                                        modifier = Modifier.size(15.dp)
                                    )

                                    Spacer(Modifier.width(4.dp))

                                    Text(
                                        SquadStrings.inVerification,
                                        style = AppFont.ibmPlexSans(10, FontWeight.SemiBold),
                                        color = Color(0xFFFF9800)
                                    )
                                }

                                Button(
                                    onClick = {},
                                    enabled = false,
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.LightGray,
                                        disabledContainerColor = Color.LightGray
                                    )
                                ) {

                                    Text(
                                        SquadStrings.pay,
                                        style = AppFont.ibmPlexSans(13, FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        PaidStatus.NOT_PAID -> {

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = Color(0xFFFF9800),
                                        modifier = Modifier.size(15.dp)
                                    )

                                    Spacer(Modifier.width(4.dp))

                                    Text(
                                        SquadStrings.pendingRepayment,
                                        style = AppFont.ibmPlexSans(10, FontWeight.SemiBold),
                                        color = Color(0xFFFF9800)
                                    )
                                }

                                Button(
                                    onClick = { onPay?.invoke() },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppColors.primaryButton
                                    )
                                ) {

                                    Text(
                                        SquadStrings.pay,
                                        style = AppFont.ibmPlexSans(13, FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                MemberPaymentSubType.SETTLEMENT -> {

                    when (payment.paidStatus) {

                        PaidStatus.NOT_PAID -> {

                            Row(verticalAlignment = Alignment.CenterVertically) {

                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(15.dp)
                                )

                                Spacer(Modifier.width(4.dp))

                                Text(
                                    SquadStrings.settlementPending,
                                    style = AppFont.ibmPlexSans(10, FontWeight.SemiBold),
                                    color = Color(0xFFFF9800)
                                )
                            }
                        }

                        PaidStatus.INVERIFICATION -> {

                            Row(verticalAlignment = Alignment.CenterVertically) {

                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(15.dp)
                                )

                                Spacer(Modifier.width(4.dp))

                                Text(
                                    SquadStrings.settlementInVerification,
                                    style = AppFont.ibmPlexSans(10, FontWeight.SemiBold),
                                    color = Color(0xFFFF9800)
                                )
                            }
                        }

                        PaidStatus.PAID -> {

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = AppColors.primaryBrand,
                                    modifier = Modifier.size(16.dp)
                                )

                                Spacer(Modifier.width(4.dp))

                                Text(
                                    SquadStrings.settlementCompleted,
                                    style = AppFont.ibmPlexSans(10, FontWeight.SemiBold),
                                    color = AppColors.primaryBrand
                                )

                                Spacer(Modifier.width(6.dp))

                                Box(
                                    modifier = Modifier
                                        .size(3.dp)
                                        .background(AppColors.border, CircleShape)
                                )

                                Spacer(Modifier.width(6.dp))

                                Text(
                                    CommonFunctions.dateToString(
                                        payment.amountRepaidDate?.toDate()
                                            ?: payment.amountReceivedDate?.toDate()
                                            ?: Date()
                                    ),
                                    style = AppFont.ibmPlexSans(10, FontWeight.Medium),
                                    color = AppColors.primaryBrand
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}