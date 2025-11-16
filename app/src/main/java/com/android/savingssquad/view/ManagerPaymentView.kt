package com.android.savingssquad.view

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.android.savingssquad.viewmodel.SquadViewModel
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
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.CashfreeBeneficiaryType
import com.android.savingssquad.singleton.CashfreePaymentAction
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.GroupFundUserType
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
import com.android.savingssquad.viewmodel.FirebaseFunctionsManager
import com.google.firebase.Timestamp
import com.yourapp.utils.CommonFunctions
import kotlinx.coroutines.flow.forEach
import java.util.Calendar

@Composable
fun ManagerPaymentView(
    navController: NavController,
    squadViewModel: SquadViewModel,
) {
    val coroutineScope = rememberCoroutineScope()

    var selectedSegment by remember { mutableStateOf(SquadStrings.loanPayments) }
    var loanSelectedMember by remember { mutableStateOf<Member?>(null) }
    var emiSelectedType by remember { mutableStateOf<EMIConfiguration?>(null) }
    var isShowLoanMemberList by remember { mutableStateOf(false) }
    var loanSelectedMemberNameError by remember { mutableStateOf("") }
    var showAllEMIs by remember { mutableStateOf(false) }

    val isPendingLoanAvailable by remember { derivedStateOf { squadViewModel.isPendingLoanAvailable } }

    Box(modifier = Modifier.fillMaxSize()) {

        AppBackgroundGradient()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp),
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Navigation Bar
            item {
                SSNavigationBar(title = "Payment", showBackButton = false, navController = navController)
            }

            // Segmented Picker
            item {
                ModernSegmentedPickerView(
                    segments = listOf(SquadStrings.loanPayments, SquadStrings.otherPayments),
                    selectedSegment = selectedSegment,
                    onSegmentSelected = { selectedSegment = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (selectedSegment == SquadStrings.loanPayments) {
                // Member Selection
                item {
                    SectionView(title = "Select Member") {
                        SSTextField(
                            icon = Icons.Default.Person,
                            placeholder = loanSelectedMember?.name ?: "Select Group Fund Member",
                            textState = remember { mutableStateOf(loanSelectedMember?.name ?: "") },
                            keyboardType = KeyboardType.Text,
                            showDropdown = true,
                            error = loanSelectedMemberNameError,
                            onDropdownTap = { isShowLoanMemberList = true },
                            disabled = true
                        )
                    }
                }

                // EMI list
                items(
                    if (showAllEMIs) squadViewModel.emiConfigurations.value
                    else squadViewModel.emiConfigurations.value.take(2),
                    key = { it.id!! }
                ) { emi ->
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
                    val buttonEnabled = (loanSelectedMember?.upiBeneId?.isNotEmpty() == true) && emiSelectedType != null
                    SSButton(
                        title = if (emiSelectedType != null && loanSelectedMember != null)
                            "Pay ₹${emiSelectedType!!.loanAmount} to ${loanSelectedMember!!.name}'s UPI"
                        else "Pay",
                        isDisabled = !buttonEnabled,
                        action = { /* handle payment */ }
                    )
                }

            } else {
                // Other Payments
                item {
                    SectionView(title = "Other Payments") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val amountState = remember { mutableStateOf("") }
                            SSTextField(
                                icon = Icons.Default.CreditCard,
                                placeholder = "Enter Amount",
                                textState = amountState,
                                keyboardType = KeyboardType.Number,
                                error = ""
                            )

                            val notesState = remember { mutableStateOf("") }
                            SSTextView(
                                placeholder = "Add a note",
                                text = notesState.value,
                                onTextChange = { notesState.value = it },
                                error = "",
                                maxCharacters = 200
                            )

                            SSButton(title = "Make Payment") {
                                /* handle other payment */
                            }
                        }
                    }
                }
            }
        }


        // Member Selection Popup
        if (isShowLoanMemberList) {
            OverlayBackgroundView(
                showPopup = remember { mutableStateOf(true) },
                onDismiss = { isShowLoanMemberList = false }
            ) {
                SingleSelectionPopupView(
                    showPopup = remember { mutableStateOf(true) },
                    listValues = squadViewModel.groupFundMemberNames.collectAsState().value,
                    title = "Members",
                    onItemSelected = { selectedValue ->
                        isShowLoanMemberList = false
                        val member = CommonFunctions.getMember(
                            by = selectedValue,
                            from = squadViewModel.groupFundMembers.value
                        )
                        loanSelectedMember = member
                        loanSelectedMemberNameError = ""
                        squadViewModel.fetchMemberLoans(
                            showLoader = true,
                            memberID = member?.id ?: ""
                        ) { _, _ -> }
                    }
                )
            }
        }
    }
}

private suspend fun makeLoanPayment(
    squadViewModel: SquadViewModel,
    selectedMember: Member,
    selectedLoan: EMIConfiguration,
    loaderManager: LoaderManager
) {
    val newLoan = CommonFunctions.generateMemberLoan(
        emiConfig = selectedLoan,
        memberID = selectedMember.id ?: "",
        memberName = selectedMember.name
    )

    squadViewModel.addOrUpdateMemberLoan(
        showLoader = true,
        memberID = selectedMember.id ?: "",
        loan = newLoan
    ) { success, error ->
        if (success) {
            val newPayment = PaymentsDetails(
                id = CommonFunctions.generatePaymentID(groupFundId = squadViewModel.groupFund.value?.groupFundID ?: ""),
                paymentUpdatedDate = Timestamp.now(),
                memberId = selectedMember.id ?: "",
                memberName = selectedMember.name,
                paymentPhone = selectedMember.phoneNumber,
                paymentEmail = selectedMember.mailID ?: "",
                userType = GroupFundUserType.GROUP_FUND_MANAGER,
                amount = selectedLoan.loanAmount,
                intrestAmount = 0,
                paymentEntryType = PaymentEntryType.AUTOMATIC_ENTRY,
                paymentType = PaymentType.PAYMENT_DEBIT,
                paymentSubType = PaymentSubType.LOAN_AMOUNT,
                description = "Loan disbursement",
                groupFundId = squadViewModel.groupFund.value?.groupFundID ?: "",
                contributionId = "",
                loanId = newLoan.id ?: "",
                installmentId = "",
                transferReferenceId = ""
            )

            FirebaseFunctionsManager.shared.processCashFreePayment(
                groupFundId = squadViewModel.groupFund.value?.groupFundID ?: "",
                action = CashfreePaymentAction.New(payment = newPayment)
            ) { sessionId, orderId, error ->
                squadViewModel.handleCashFreeResponse(sessionId, orderId, error)
            }
        } else {
            println("❌ Error: ${error ?: "Unknown error"}")
        }
    }
}

private fun validateOtherPaymentFields(
    amount: String,
    notes: String,
    onSetAmountError: (String) -> Unit,
    onSetNotesError: (String) -> Unit
): Boolean {
    val amountErr = if (amount.isBlank()) "Amount is required" else ""
    val notesErr = if (notes.isBlank()) "Note is required" else ""
    onSetAmountError(amountErr)
    onSetNotesError(notesErr)
    return amountErr.isEmpty() && notesErr.isEmpty()
}

private suspend fun handleOtherPayment(squadViewModel: SquadViewModel, amountStr: String, notes: String) {
    val availableAmount = squadViewModel.groupFund.value?.currentAvailableAmount ?: 0
    val amountInt = amountStr.toIntOrNull() ?: 0
    if (availableAmount < amountInt) {
        AlertManager.shared.showAlert(
            title = SquadStrings.appName,
            message = "❌ Fund not available",
            primaryButtonTitle = "OK",
            primaryAction = {}
        )
        return
    }

    println("Processing Other Payment: $amountStr - Notes: $notes")
}

@Composable
fun PaymentEMIListRow(
    emi: EMIConfiguration,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.surface)
            .border(1.dp, AppColors.border, RoundedCornerShape(14.dp))
            .appShadow(AppShadows.card)
            .padding(14.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = emi.loanAmount.currencyFormattedWithCommas(), style = AppFont.ibmPlexSans(15, FontWeight.SemiBold), color = AppColors.headerText)
            Text(text = "${emi.emiMonths} months @ ${"%.2f".format(emi.emiInterestRate)}% interest", style = AppFont.ibmPlexSans(13, FontWeight.Normal), color = AppColors.secondaryText)
            Text(text = "EMI: ${emi.emiAmount.currencyFormattedWithCommas()} / month", style = AppFont.ibmPlexSans(13, FontWeight.Medium), color = AppColors.infoAccent)
            Text(text = "Total Interest: ${emi.interestAmount.currencyFormattedWithCommas()}", style = AppFont.ibmPlexSans(13, FontWeight.Medium), color = AppColors.errorAccent)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Icon(
            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Circle,
            contentDescription = null,
            tint = if (isSelected) AppColors.primaryButton else AppColors.border,
            modifier = Modifier.size(22.dp)
        )
    }
}