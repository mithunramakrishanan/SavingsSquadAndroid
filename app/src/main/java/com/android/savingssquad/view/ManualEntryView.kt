package com.android.savingssquad.view

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.android.savingssquad.model.ContributionDetail
import com.android.savingssquad.viewmodel.SquadViewModel
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
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.GroupFundActivityType
import com.android.savingssquad.singleton.GroupFundUserType
import com.android.savingssquad.singleton.PaidStatus
import com.android.savingssquad.singleton.PaymentEntryType
import com.android.savingssquad.singleton.PaymentSubType
import com.android.savingssquad.singleton.PaymentType
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.singleton.appShadow
import com.android.savingssquad.singleton.asTimestamp
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.android.savingssquad.singleton.displayText
import com.android.savingssquad.viewmodel.AlertManager
import com.android.savingssquad.viewmodel.AppDestination
import com.yourapp.utils.CommonFunctions
import kotlinx.coroutines.Dispatchers
import java.util.Calendar


@Composable
fun ManualEntryView(
    navController: NavController?,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager = LoaderManager.shared
) {
    val coroutineScope = rememberCoroutineScope()

    // ===== state (same names as SwiftUI) =====
    var contributionSelectedMember by remember { mutableStateOf<Member?>(null) }
    var contributionSelectedMemberName by remember { mutableStateOf("") }
    var contributionSelectedMonthYear by remember { mutableStateOf("") }

    var isShowContributionMemberList by remember { mutableStateOf(false) }
    var isShowContributionMonthList by remember { mutableStateOf(false) }
    var availableContributionMonths by remember { mutableStateOf(listOf<String>()) }

    var contributionAmountError by remember { mutableStateOf("") }
    var contributionSelectedMemberNameError by remember { mutableStateOf("") }
    var contributionSelectedMonthYearError by remember { mutableStateOf("") }

    // EMI section
    var emiSelectedMember by remember { mutableStateOf<Member?>(null) }
    var emiSelectedMemberName by remember { mutableStateOf("") }
    var emiSelectedMonthYear by remember { mutableStateOf("") }

    var isShowEMIMemberList by remember { mutableStateOf(false) }
    var isShowEMIMonthList by remember { mutableStateOf(false) }
    var availableEMIMonths by remember { mutableStateOf(listOf<String>()) }

    var emiAmountError by remember { mutableStateOf("") }
    var emiSelectedMemberNameError by remember { mutableStateOf("") }
    var emiSelectedMonthYearError by remember { mutableStateOf("") }
    var selectedInstallment by remember { mutableStateOf<Installment?>(null) }

    var notes by remember { mutableStateOf("") }
    var notesError by remember { mutableStateOf("") }

    var selectedSegment by remember { mutableStateOf(SquadStrings.manualEntryContribution) }

    // ===== viewmodel state (collected safely) =====
    val groupFund by squadViewModel.groupFund.collectAsState() // nullable
    val groupFundMembers by squadViewModel.groupFundMembers.collectAsState(initial = emptyList())
    val groupFundMemberNames by squadViewModel.groupFundMemberNames.collectAsState(initial = emptyList())
    val memberPendingLoans by squadViewModel.memberPendingLoans.collectAsState(initial = null)
    val selectedContributions by squadViewModel.selectedContributions.collectAsState(initial = emptyList())
    val isPendingLoanAvailable by remember { derivedStateOf { squadViewModel.isPendingLoanAvailable } }

    // fetch rules / initial data if needed
    LaunchedEffect(Unit) {
        // no-op or call any required fetches - keep parity with SwiftUI onAppear
    }

    // ===== UI =====
    Box(modifier = Modifier.fillMaxSize()) {
        AppBackgroundGradient()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
        ) {

            SSNavigationBar(
                title = SquadStrings.manualEntry,
                navController = navController,
                showBackButton = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            ModernSegmentedPickerView(
                segments = listOf(SquadStrings.manualEntryContribution, SquadStrings.manualEntryEMI),
                selectedSegment = selectedSegment,
                onSegmentSelected = { newSegment -> selectedSegment = newSegment }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedSegment == SquadStrings.manualEntryContribution) {
                // Contribution Section
                SectionView(title = "Contribution Entry") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        // Member selection field (readonly but dropdown active)
                        SSTextField(
                            icon = Icons.Default.Person,
                            placeholder = if (contributionSelectedMemberName.isEmpty()) "Select GroupFund Member" else contributionSelectedMemberName,
                            textState = remember { mutableStateOf(contributionSelectedMemberName) }, // keep display, but won't edit directly
                            keyboardType = KeyboardType.Text,
                            showDropdown = true,
                            error = contributionSelectedMemberNameError,
                            onDropdownTap = {
                                isShowContributionMemberList = true
                            },
                            disabled = true

                        )

                        // Contribution month selector
                        SSTextField(
                            icon = Icons.Default.CalendarToday,
                            placeholder = if (contributionSelectedMonthYear.isEmpty()) "Select Contribution Date" else contributionSelectedMonthYear,
                            textState = remember { mutableStateOf(contributionSelectedMonthYear) },
                            keyboardType = KeyboardType.Text,
                            showDropdown = true,
                            error = contributionSelectedMonthYearError,
                            onDropdownTap = {
                                if (contributionSelectedMemberName.isEmpty()) {
                                    AlertManager.shared.showAlert(
                                        title = SquadStrings.appName,
                                        message = "Please select a member",
                                        primaryButtonTitle = "OK",
                                        primaryAction = {}
                                    )
                                } else {
                                    if (availableContributionMonths.isEmpty()) {
                                        AlertManager.shared.showAlert(
                                            title = SquadStrings.appName,
                                            message = "No outstanding dues for $contributionSelectedMemberName",
                                            primaryButtonTitle = "OK",
                                            primaryAction = {}
                                        )
                                    } else {
                                        isShowContributionMonthList = true
                                    }
                                }
                            },
                            disabled = true

                        )

                        // Contribution Amount readonly
                        SSTextField(
                            icon = Icons.Default.CheckCircle,
                            placeholder = (groupFund?.monthlyContribution ?: 0).toString(),
                            textState = remember { mutableStateOf((groupFund?.monthlyContribution ?: 0).toString()) },
                            keyboardType = KeyboardType.Number,
                            disabled = true)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SSButton(title = "Add Contribution", isDisabled = false) {
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
                        val groupFundLocal = groupFund
                        if (selectedMember == null || groupFundLocal == null) {
                            // show error
                            return@SSButton
                        }

                        loaderManager.showLoader()
                        coroutineScope.launch(Dispatchers.IO) {
                            val contribution = selectedContributions.find { it.monthYear == contributionSelectedMonthYear }
                            val contributionID = contribution?.id
                            if (contribution == null || contributionID == null) {
                                loaderManager.hideLoader()
                                return@launch
                            }

                            val updatedContribution = ContributionDetail(
                                id = contributionID,
                                orderId = "",
                                memberID = selectedMember.id ?: "",
                                memberName = selectedMember.name,
                                monthYear = contributionSelectedMonthYear,
                                amount = groupFundLocal.monthlyContribution,
                                paidOn = Date().asTimestamp,
                                paidStatus = PaidStatus.PAID, // adjust enum mapping to your model
                                paymentEntryType = PaymentEntryType.MANUAL_ENTRY,
                                dueDate = CommonFunctions.getContributionDue(monthYear = contributionSelectedMonthYear).asTimestamp
                            )

                            squadViewModel.editContribution(
                                showLoader = true,
                                groupFundID = groupFundLocal.groupFundID,
                                memberID = selectedMember.id ?: "",
                                contributionID = contributionID,
                                updatedContribution = updatedContribution
                            ) { success, message ->
                                coroutineScope.launch(Dispatchers.IO) {
                                    if (success) {
                                        // async: create payments and activity like SwiftUI
                                        val newPayment = PaymentsDetails(
                                            id = CommonFunctions.generatePaymentID(groupFundId = groupFundLocal.groupFundID),
                                            paymentUpdatedDate = Date().asTimestamp,
                                            memberId = selectedMember.id ?: "",
                                            memberName = contributionSelectedMemberName,
                                            paymentPhone = selectedMember.phoneNumber,
                                            paymentEmail = selectedMember.mailID ?: "",
                                            userType = GroupFundUserType.GROUP_FUND_MEMBER,
                                            amount = groupFundLocal.monthlyContribution,
                                            intrestAmount = 0,
                                            paymentEntryType = PaymentEntryType.MANUAL_ENTRY,
                                            paymentType = PaymentType.PAYMENT_CREDIT,
                                            paymentSubType = PaymentSubType.CONTRIBUTION_AMOUNT,
                                            description = "Updated $contributionSelectedMemberName contribution for $contributionSelectedMonthYear",
                                            groupFundId = groupFundLocal.groupFundID,
                                            contributionId = contributionID,
                                            loanId = "",
                                            installmentId = "",
                                            transferReferenceId = ""
                                        )

                                        squadViewModel.savePayments(
                                            groupFundID = groupFundLocal.groupFundID,
                                            payment = listOf(newPayment)
                                        ) { pSuccess, pError ->
                                            // no-op logging
                                        }

                                        squadViewModel.createGroupFundActivity(
                                            activityType = GroupFundActivityType.AMOUNT_CREDIT,
                                            userName = "CHIT MEMBER",
                                            amount = groupFundLocal.monthlyContribution,
                                            description = "Updated $contributionSelectedMemberName contribution for $contributionSelectedMonthYear"
                                        ) {
                                            coroutineScope.launch(Dispatchers.Main) {
                                                contributionSelectedMemberName = ""
                                                contributionSelectedMonthYear = ""
                                                loaderManager.hideLoader()
                                            }
                                        }
                                    } else {
                                        coroutineScope.launch(Dispatchers.Main) {
                                            loaderManager.hideLoader()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // EMI Entry
                SectionView(title = "EMI Entry") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        // Member selection
                        SSTextField(
                            icon = Icons.Default.Person,
                            placeholder = if (emiSelectedMemberName.isEmpty()) "Select GroupFund Member" else emiSelectedMemberName,
                            textState = remember { mutableStateOf(emiSelectedMemberName) },
                            keyboardType = KeyboardType.Text,
                            showDropdown = true,
                            error = emiSelectedMemberNameError,
                            onDropdownTap = { isShowEMIMemberList = true },
                            disabled = true)

                        if (isPendingLoanAvailable.collectAsState().value) {
                            SSTextField(
                                icon = Icons.Default.CalendarToday,
                                placeholder = if (emiSelectedMonthYear.isEmpty()) "Select EMI" else emiSelectedMonthYear,
                                textState = remember { mutableStateOf(emiSelectedMonthYear) },
                                keyboardType = KeyboardType.Text,
                                showDropdown = true,
                                error = emiSelectedMonthYearError,
                                onDropdownTap = { isShowEMIMonthList = true },
                                disabled = true)

                            SSTextField(
                                icon = Icons.Default.CheckCircle,
                                placeholder = ((selectedInstallment?.installmentAmount ?: 0) + (selectedInstallment?.interestAmount ?: 0)).toString(),
                                textState = remember { mutableStateOf(((selectedInstallment?.installmentAmount ?: 0) + (selectedInstallment?.interestAmount ?: 0)).toString()) },
                                keyboardType = KeyboardType.Number,
                                disabled = true)
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = AppColors.successAccent,
                                    modifier = Modifier.size(50.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                androidx.compose.material3.Text(
                                    text = "${emiSelectedMemberName.ifEmpty { "Member" }} doesn't have any pending loan.",
                                    style = AppFont.ibmPlexSans(16, FontWeight.Medium),
                                    color = AppColors.successAccent
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SSButton(
                    title = "Pay EMI",
                    isDisabled = !isPendingLoanAvailable.collectAsState().value,
                    action = {
                        if (!isPendingLoanAvailable.value) return@SSButton
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
                                loaderManager.showLoader()
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
                                            val loanPayment = PaymentsDetails(
                                                id = CommonFunctions.generatePaymentID(groupFundId = groupFund?.groupFundID ?: ""),
                                                paymentUpdatedDate = Date().asTimestamp,
                                                memberId = emiSelectedMember?.id ?: "",
                                                memberName = emiSelectedMemberName,
                                                paymentPhone = emiSelectedMember?.phoneNumber ?: "",
                                                paymentEmail = emiSelectedMember?.mailID ?: "",
                                                userType = GroupFundUserType.GROUP_FUND_MANAGER,
                                                amount = selectedInstallment?.installmentAmount ?: 0,
                                                intrestAmount = 0,
                                                paymentEntryType = PaymentEntryType.MANUAL_ENTRY,
                                                paymentType = PaymentType.PAYMENT_CREDIT,
                                                paymentSubType = PaymentSubType.EMI_AMOUNT,
                                                description = "Updated EMI to $emiSelectedMemberName - ${selectedInstallment?.installmentNumber ?: ""} for #$loanNumber",
                                                groupFundId = groupFund?.groupFundID ?: ""
                                            )
                                            val interestPayment = PaymentsDetails(
                                                id = CommonFunctions.generatePaymentID(groupFundId = groupFund?.groupFundID ?: ""),
                                                paymentUpdatedDate = Date().asTimestamp,
                                                memberId = emiSelectedMember?.id ?: "",
                                                memberName = emiSelectedMemberName,
                                                paymentPhone = emiSelectedMember?.phoneNumber ?: "",
                                                paymentEmail = emiSelectedMember?.mailID ?: "",
                                                userType = GroupFundUserType.GROUP_FUND_MEMBER,
                                                amount = selectedInstallment?.interestAmount ?: 0,
                                                intrestAmount = 0,
                                                paymentEntryType = PaymentEntryType.MANUAL_ENTRY,
                                                paymentType = PaymentType.PAYMENT_CREDIT,
                                                paymentSubType = PaymentSubType.INTEREST_AMOUNT,
                                                description = "Updated Interest to $emiSelectedMemberName - ${selectedInstallment?.installmentNumber ?: ""} for #$loanNumber",
                                                groupFundId = groupFund?.groupFundID ?: ""
                                            )

                                            squadViewModel.savePayments(groupFundID = groupFund?.groupFundID ?: "", payment = listOf(loanPayment, interestPayment)) { psuccess, perror ->
                                                // no-op
                                            }

                                            val total = (selectedInstallment?.installmentAmount ?: 0) + (selectedInstallment?.interestAmount ?: 0)
                                            squadViewModel.createGroupFundActivity(
                                                activityType = GroupFundActivityType.AMOUNT_CREDIT,
                                                userName = "SQUAD MANAGER",
                                                amount = total,
                                                description = "Updated EMI and Interest to $emiSelectedMemberName - ${selectedInstallment?.installmentNumber ?: ""} for #$loanNumber ${total.currencyFormattedWithCommas()}"
                                            ) {
                                                coroutineScope.launch(Dispatchers.Main) {
                                                    emiSelectedMemberName = ""
                                                    emiSelectedMonthYear = ""
                                                    loaderManager.hideLoader()
                                                }
                                            }
                                        } else {
                                            loaderManager.hideLoader()
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // ===== Overlays / Popups =====
        if (isShowContributionMemberList) {
            OverlayBackgroundView(
                showPopup = remember { mutableStateOf(true) },
                onDismiss = { isShowContributionMemberList = false }
            ) {
                SingleSelectionPopupView(
                    listValues = groupFundMemberNames,
                    title = "Members",
                    onItemSelected = { selectedValue ->
                        isShowContributionMemberList = false
                        contributionSelectedMemberName = selectedValue
                        contributionSelectedMember = CommonFunctions.getMember(by = selectedValue, from = groupFundMembers)
                        // fetch unpaid months for member
                        val member = contributionSelectedMember
                        val groupId = groupFund?.groupFundID
                        if (member != null && groupId != null) {
                            loaderManager.showLoader()
                            squadViewModel.fetchContributionsForMember(showLoader = true, groupFundID = groupId, memberID = member.id ?: "") { contributions, error ->
                                loaderManager.hideLoader()
                                if (contributions != null) {
                                    availableContributionMonths = contributions.unpaidMonths()
                                } else {
                                    availableContributionMonths = emptyList()
                                }
                            }
                        }
                    },
                    onCancelClick = {}
                )
            }
        }

        if (isShowContributionMonthList) {
            OverlayBackgroundView(
                showPopup = remember { mutableStateOf(true) },
                onDismiss = { isShowContributionMonthList = false }
            ) {
                SingleSelectionPopupView(
                    listValues = availableContributionMonths,
                    title = "Pending Contribution Months",
                    onItemSelected = { selectedValue ->
                        contributionSelectedMonthYear = selectedValue
                        isShowContributionMonthList = false
                    },
                    onCancelClick = {}
                )
            }
        }

        if (isShowEMIMemberList) {
            OverlayBackgroundView(
                showPopup = remember { mutableStateOf(true) },
                onDismiss = { isShowEMIMemberList = false }
            ) {
                SingleSelectionPopupView(
                    listValues = groupFundMemberNames,
                    title = "Members",
                    onItemSelected = { selectedValue ->
                        isShowEMIMemberList = false
                        emiSelectedMemberName = selectedValue
                        emiSelectedMember = CommonFunctions.getMember(by = selectedValue, from = groupFundMembers)
                        if (emiSelectedMember != null) {
                            loaderManager.showLoader()
                            squadViewModel.fetchMemberLoans(showLoader = true, memberID = emiSelectedMember?.id ?: "") { success, error ->
                                loaderManager.hideLoader()
                                // memberPendingLoans will be updated via viewmodel state
                            }
                        }
                    },
                    onCancelClick = {}
                )
            }
        }

        if (isShowEMIMonthList) {
            OverlayBackgroundView(
                showPopup = remember { mutableStateOf(true) },
                onDismiss = { isShowEMIMonthList = false }
            ) {
                InstallmentPopupView(
                    title = memberPendingLoans?.firstOrNull()?.loanNumber ?: "",
                    installments = memberPendingLoans?.firstOrNull()?.installments ?: emptyList(),
                    onSelect = { installment ->
                        selectedInstallment = installment
                        emiSelectedMonthYear = CommonFunctions.dateToString(date = installment.dueDate?.toDate() ?: Date(), format = "MMM yyyy")
                        isShowEMIMonthList = false
                    },
                    isShowing = remember { mutableStateOf(true) }
                )
            }
        }
    }
}

// ===== Helper validation functions (mirrors SwiftUI functions) =====
private fun validateContributionFields(
    memberName: String,
    monthYear: String,
    contributionAmountError: String,
    onSetMemberError: (String) -> Unit,
    onSetMonthError: (String) -> Unit
): Boolean {
    onSetMemberError(if (memberName.trim().isEmpty()) "Member Name is required" else "")
    onSetMonthError(if (monthYear.trim().isEmpty()) "Month-Year is required" else "")
    return memberName.trim().isNotEmpty() && monthYear.trim().isNotEmpty() && contributionAmountError.isEmpty()
}

private fun validateEMIFields(
    memberName: String,
    monthYear: String,
    emiAmountError: String,
    onSetMemberError: (String) -> Unit,
    onSetMonthError: (String) -> Unit
): Boolean {
    onSetMemberError(if (memberName.trim().isEmpty()) "Member Name is required" else "")
    onSetMonthError(if (monthYear.trim().isEmpty()) "Month-Year is required" else "")
    return memberName.trim().isNotEmpty() && monthYear.trim().isNotEmpty() && emiAmountError.isEmpty()
}