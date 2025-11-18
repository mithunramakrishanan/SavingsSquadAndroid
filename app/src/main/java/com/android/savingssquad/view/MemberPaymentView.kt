package com.android.savingssquad.view

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.android.savingssquad.viewmodel.SquadViewModel
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.savingssquad.model.EMIConfiguration
import com.android.savingssquad.viewmodel.LoaderManager
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import kotlinx.coroutines.launch
import java.util.Date
import com.android.savingssquad.model.GroupFund
import com.android.savingssquad.model.Installment
import com.android.savingssquad.model.Member
import com.android.savingssquad.model.MemberLoan
import com.android.savingssquad.model.PaymentsDetails
import com.android.savingssquad.model.unpaidMonths
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.CashfreeBeneficiaryType
import com.android.savingssquad.singleton.CashfreePaymentAction
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.GroupFundUserType
import com.android.savingssquad.singleton.PaymentEntryType
import com.android.savingssquad.singleton.PaymentStatus
import com.android.savingssquad.singleton.PaymentSubType
import com.android.savingssquad.singleton.PaymentType
import com.android.savingssquad.singleton.RemainderType
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.singleton.appShadow
import com.android.savingssquad.singleton.asTimestamp
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.android.savingssquad.singleton.displayText
import com.android.savingssquad.viewmodel.AlertManager
import com.android.savingssquad.viewmodel.FirebaseFunctionsManager
import com.google.firebase.Timestamp
import com.yourapp.utils.CommonFunctions
import kotlinx.coroutines.flow.forEach
import java.util.Calendar

// MemberPaymentScreen.kt
@Composable
fun MemberPaymentView(
    navController: NavController?, // if you need nav actions, else pass null
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager = LoaderManager.shared
) {
    val coroutineScope = rememberCoroutineScope()

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

    var memberPaymentSegment by remember { mutableStateOf(SquadStrings.manualEntryContribution) }

    // Collect flows safely
    val currentMember by squadViewModel.currentMember.collectAsStateWithLifecycle()
    val groupFund by squadViewModel.groupFund.collectAsStateWithLifecycle()
    val memberPendingLoans by squadViewModel.memberPendingLoans.collectAsStateWithLifecycle()
    val selectedContributions by squadViewModel.selectedContributions.collectAsStateWithLifecycle()
    val groupFundPayments by squadViewModel.groupFundPayments.collectAsStateWithLifecycle()

    // Payments list for "Recent Payments" similar to SwiftUI logic (current month)
    var payments by remember { mutableStateOf(listOf<PaymentsDetails>()) }

    // On first composition fetch payments and reset UI states
    LaunchedEffect(Unit) {
        // 1️⃣ Fetch payments
        squadViewModel.fetchPayments(showLoader = true) { success, _ ->
            payments = getCurrentMonthPayments(squadViewModel.groupFundPayments.value)
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
                    memberPaymentSegment = SquadStrings.manualEntryContribution
                    contributionSelectedMonthYear = CommonFunctions.dateToString(
                        date = remainder.remainderDueDate?.toDate() ?: Date(),
                        format = "MMM yyyy"
                    )
                }
                RemainderType.EMI -> {
                    memberPaymentSegment = SquadStrings.manualEntryEMI
                    val installment = squadViewModel.memberPendingLoans.value?.firstOrNull()?.installments
                        ?.firstOrNull { it.id == remainder.remainderID }
                    if (installment != null) {
                        selectedInstallment = installment
                        emiSelectedMonthYear = CommonFunctions.dateToString(
                            date = remainder.remainderDueDate?.toDate() ?: Date(),
                            format = "MMM yyyy"
                        )
                    }
                }

                RemainderType.OTHER_REMAINDER -> TODO()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackgroundGradient()

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
            SSNavigationBar(title = "Payment", navController = navController, showBackButton = false)

            Spacer(modifier = Modifier.height(12.dp))

            ModernSegmentedPickerView(
                segments = listOf(SquadStrings.manualEntryContribution, SquadStrings.manualEntryEMI),
                selectedSegment = memberPaymentSegment,
                onSegmentSelected = { memberPaymentSegment = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Content
            Column(modifier = Modifier.fillMaxWidth()) {
                if (memberPaymentSegment == SquadStrings.manualEntryContribution) {
                    // Contribution flow
                    ContributionSection(
                        currentMember = currentMember,
                        contributionSelectedMonthYear = contributionSelectedMonthYear,
                        onOpenMonthList = {
                            // fetch contributions for member and show list
                            loaderManager.showLoader()
                            val gfId = currentMember?.groupFundID ?: ""
                            val memberId = currentMember?.id ?: ""
                            squadViewModel.fetchContributionsForMember(showLoader = true, groupFundID = gfId, memberID = memberId) { contributions, error ->
                                loaderManager.hideLoader()
                                if (contributions != null) {
                                    availableContributionMonths = contributions.unpaidMonths()
                                    squadViewModel.setShowContributionMonthPopup(true)

                                } else {
                                    availableContributionMonths = emptyList()
                                    AlertManager.shared.showAlert(title = SquadStrings.appName, message = "No outstanding dues for ${currentMember?.name ?: ""}", primaryButtonTitle = "OK", primaryAction = {})
                                }
                            }
                        },
                        contributionAmount = groupFund?.monthlyContribution ?: 0,
                        contributionSelectedMonthYearError = contributionSelectedMonthYearError
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ContributionButton(
                        isDisabled = groupFund?.upiBeneId.isNullOrEmpty(),
                        onClick = {
                            if (validateContributionFields(contributionSelectedMonthYear) { contributionSelectedMonthYearError = it }) {
                                // Payment flow
                                val gf = groupFund ?: return@ContributionButton
                                val selectedMember = currentMember ?: return@ContributionButton
                                val contribution = selectedContributions.firstOrNull { it.monthYear == contributionSelectedMonthYear }
                                val contributionID = contribution?.id
                                if (contribution == null || contributionID.isNullOrEmpty()) {
                                    // missing contribution
                                    loaderManager.hideLoader()
                                    return@ContributionButton
                                }

                                loaderManager.showLoader()

                                val newPayment = PaymentsDetails(
                                    id = CommonFunctions.generatePaymentID(groupFundId = gf.groupFundID),
                                    paymentUpdatedDate = Timestamp(date = Date()),
                                    memberId = selectedMember.id ?: "",
                                    memberName = selectedMember.name,
                                    paymentPhone = selectedMember.phoneNumber,
                                    paymentEmail = selectedMember.mailID ?: "",
                                    userType = GroupFundUserType.GROUP_FUND_MEMBER, // adapt enum mapping
                                    amount = gf.monthlyContribution,
                                    intrestAmount = 0,
                                    paymentEntryType = PaymentEntryType.AUTOMATIC_ENTRY,
                                    paymentType = PaymentType.PAYMENT_CREDIT,
                                    paymentSubType = PaymentSubType.CONTRIBUTION_AMOUNT,
                                    description = "Contribution for $contributionSelectedMonthYear.",
                                    groupFundId = gf.groupFundID,
                                    contributionId = contributionID,
                                    loanId = "",
                                    installmentId = "",
                                    transferReferenceId = ""
                                )

                                // create or retry payment
                                if (contribution.orderId.isEmpty()) {
                                    FirebaseFunctionsManager.shared.processCashFreePayment(
                                        groupFundId = gf.groupFundID,
                                        action = CashfreePaymentAction.New(payment = newPayment)
                                    ) { sessionId, orderId, error ->
                                        squadViewModel.handleCashFreeResponse(sessionId, orderId, error)
                                    }
                                } else {
                                    FirebaseFunctionsManager.shared.processCashFreePayment(
                                        groupFundId = gf.groupFundID,
                                        action = CashfreePaymentAction.Retry(contribution.orderId)
                                    ) { sessionId, orderId, error ->
                                        squadViewModel.handleCashFreeResponse(sessionId, orderId, error)
                                    }
                                }
                            }
                        }
                    )
                } else {
                    // EMI flow
                    EMISection(
                        currentMember = currentMember,
                        groupFund = groupFund,
                        isPendingLoanAvailable = squadViewModel.isPendingLoanAvailable.collectAsState().value,
                        emiSelectedMonthYear = emiSelectedMonthYear,
                        emiSelectedMonthYearError = emiSelectedMonthYearError,
                        selectedInstallment = selectedInstallment,
                        onOpenInstallmentList = {
                            // show installment popup
                            squadViewModel.setShowEMIMonthPopup(true)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    EMIButton(
                        isDisabled = !squadViewModel.isPendingLoanAvailable.collectAsState().value,
                        onClick = {
                            if (validateEMIFields(emiSelectedMonthYear) { emiSelectedMonthYearError = it }) {
                                // Manual EMI payment flow (mirrors SwiftUI)
                                selectedInstallment?.status = EMIStatus.PAID
                                selectedInstallment?.duePaidDate = Date().asTimestamp

                                val gf = groupFund ?: return@EMIButton
                                val member = currentMember ?: return@EMIButton
                                val loan = memberPendingLoans?.firstOrNull()
                                val loanId = loan?.id ?: ""
                                val installId = selectedInstallment?.id ?: ""
                                val total = (selectedInstallment?.installmentAmount ?: 0) + (selectedInstallment?.interestAmount ?: 0)

                                val loanPayment = PaymentsDetails(
                                    id = CommonFunctions.generatePaymentID(groupFundId = gf.groupFundID),
                                    paymentUpdatedDate = Timestamp(date = Date()),
                                    memberId = member.id ?: "",
                                    memberName = member.name,
                                    paymentPhone = member.phoneNumber,
                                    paymentEmail = member.mailID ?: "",
                                    userType = GroupFundUserType.GROUP_FUND_MEMBER,
                                    amount = total,
                                    intrestAmount = selectedInstallment?.interestAmount ?: 0,
                                    paymentEntryType = PaymentEntryType.AUTOMATIC_ENTRY,
                                    paymentType = PaymentType.PAYMENT_CREDIT,
                                    paymentSubType = PaymentSubType.EMI_AMOUNT,
                                    description = "EMI and Interest - ${selectedInstallment?.installmentNumber ?: ""} for #${loan?.loanNumber ?: "N/A"} ${total.currencyFormattedWithCommas()}",
                                    groupFundId = gf.groupFundID,
                                    contributionId = "",
                                    loanId = loanId,
                                    installmentId = installId,
                                    transferReferenceId = ""
                                )

                                if (!selectedInstallment?.orderId.isNullOrEmpty()) {
                                    FirebaseFunctionsManager.shared.processCashFreePayment(
                                        groupFundId = gf.groupFundID,
                                        action = CashfreePaymentAction.Retry(selectedInstallment!!.orderId!!)
                                    ) { sessionId, orderId, error ->
                                        squadViewModel.handleCashFreeResponse(sessionId, orderId, error)
                                    }
                                } else {
                                    FirebaseFunctionsManager.shared.processCashFreePayment(
                                        groupFundId = gf.groupFundID,
                                        action = CashfreePaymentAction.New(loanPayment)
                                    ) { sessionId, orderId, error ->
                                        squadViewModel.handleCashFreeResponse(sessionId, orderId, error)
                                    }
                                }
                            }
                        }
                    )
                }
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
                    title = "Pending Contribution Months",
                    onItemSelected = { selectedValue ->
                        contributionSelectedMonthYear = selectedValue
                        squadViewModel.setShowContributionMonthPopup(false)
                    },
                    onCancelClick = {squadViewModel.setShowContributionMonthPopup(false)}
                )
            }
        }


        val isShowEMIMonthList = squadViewModel.showEMIMonthPopup.collectAsStateWithLifecycle()

        if (isShowEMIMonthList.value) {
            OverlayBackgroundView(
                showPopup = remember { mutableStateOf(true) },
                onDismiss = {  squadViewModel.setShowEMIMemberPopup(false)}
            ) {
                InstallmentPopupView(
                    title = memberPendingLoans?.firstOrNull()?.loanNumber ?: "",
                    installments = memberPendingLoans?.firstOrNull()?.installments ?: emptyList(),
                    onSelect = { installment ->
                        selectedInstallment = installment
                        emiSelectedMonthYear = CommonFunctions.dateToString(date = installment.dueDate?.toDate() ?: Date(), format = "MMM yyyy")
                        squadViewModel.setShowEMIMemberPopup(false)
                    },
                    isShowing = remember { mutableStateOf(true) }
                )
            }
        }
        // Alerts & Loader (global)
        SSAlert()
        SSLoaderView()
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
    SectionView(title = "Pay your contribution") {
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
                placeholder = if (contributionSelectedMonthYear.isEmpty()) "Select Contribution Date" else contributionSelectedMonthYear,
                textState = remember { mutableStateOf("") },
                keyboardType = KeyboardType.Text,
                showDropdown = true,
                onDropdownTap = onOpenMonthList,
                error = contributionSelectedMonthYearError
            )

            // Amount
            SSTextField(
                icon = Icons.Default.CreditCard,
                placeholder = "Contribution Amount",
                textState = remember { mutableStateOf(contributionAmount.toString()) },
                keyboardType = KeyboardType.Number,
                disabled = true
            )
        }
    }
}

@Composable
private fun ContributionButton(isDisabled: Boolean, onClick: () -> Unit) {
    Column {
        SSButton(title = "Pay Contribution", isDisabled = isDisabled, action = onClick)
        Spacer(modifier = Modifier.height(8.dp))
        if (isDisabled) {
            Text(
                text = "⚠️ Manager UPI ID is not added for this Group Fund.",
                style = AppFont.ibmPlexSans(12, FontWeight.Normal),
                color = Color.Red,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun EMISection(
    currentMember: Member?,
    groupFund: GroupFund?,
    isPendingLoanAvailable: Boolean,
    emiSelectedMonthYear: String,
    emiSelectedMonthYearError: String,
    selectedInstallment: Installment?,
    onOpenInstallmentList: () -> Unit
) {
    SectionView(title = "Pay Your EMI") {
        Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
            SSTextField(
                icon = Icons.Default.Person,
                placeholder = currentMember?.name ?: "Member",
                textState = remember { mutableStateOf("") },
                keyboardType = KeyboardType.Text,
                disabled = true
            )

            if (groupFund?.upiBeneId.isNullOrEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "⚠️ Manager UPI ID is not added for this Group Fund.",
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
            } else if (isPendingLoanAvailable) {
                SSTextField(
                    icon = Icons.Default.CalendarToday,
                    placeholder = if (emiSelectedMonthYear.isEmpty()) "Select EMI" else emiSelectedMonthYear,
                    textState = remember { mutableStateOf("") },
                    keyboardType = KeyboardType.Text,
                    showDropdown = true,
                    onDropdownTap = onOpenInstallmentList,
                    error = emiSelectedMonthYearError
                )

                SSTextField(
                    icon = Icons.Default.CreditCard,
                    placeholder = "EMI Amount",
                    textState = remember { mutableStateOf(((selectedInstallment?.installmentAmount ?: 0) + (selectedInstallment?.interestAmount
                        ?: 0)).toString()) },
                    keyboardType = KeyboardType.Number,
                    disabled = true
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${currentMember?.name ?: "Member"} doesn’t have any pending loan.",
                        style = AppFont.ibmPlexSans(16, FontWeight.SemiBold),
                        color = Color.Green,
                        textAlign = TextAlign.Center
                    )
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.Green,
                        modifier = Modifier.size(50.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EMIButton(isDisabled: Boolean, onClick: () -> Unit) {
    Column {
        SSButton(title = "Pay EMI", isDisabled = isDisabled, action = onClick)
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