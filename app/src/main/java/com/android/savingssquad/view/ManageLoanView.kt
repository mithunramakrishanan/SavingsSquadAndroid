package com.android.savingssquad.view

import android.util.Log
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.android.savingssquad.R
import com.android.savingssquad.model.BeneficiaryResult
import com.android.savingssquad.model.ContributionDetail
import com.android.savingssquad.model.EMIConfiguration
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.viewmodel.LoaderManager
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import kotlinx.coroutines.launch
import java.util.Date
import com.android.savingssquad.model.Squad
import com.android.savingssquad.model.Installment
import com.android.savingssquad.model.Member
import com.android.savingssquad.model.MemberLoan
import com.android.savingssquad.model.PaymentsDetails
import com.android.savingssquad.model.unpaidMonths
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.SquadActivityType
import com.android.savingssquad.singleton.SquadUserType
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
import com.android.savingssquad.viewmodel.FirebaseFunctionsManager
import com.yourapp.utils.CommonFunctions
import kotlinx.coroutines.Dispatchers
import java.util.Calendar
import java.util.UUID

@Composable
fun ManageLoanView(
    navController: NavController,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager = LoaderManager.shared
) {
    var selectedEMI by remember { mutableStateOf<EMIConfiguration?>(null) }
    var oldEMIConfig by remember { mutableStateOf<EMIConfiguration?>(null) }

    val emiList = squadViewModel.emiConfigurations.collectAsState().value

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        AppBackgroundGradient()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
        ) {

            SSNavigationBar(
                title = SquadStrings.manageLoanDetails,
                navController = navController,
                showBackButton = true,
                rightButtonDrawable = if (emiList.size < 3) {
                    R.drawable.add_loan_icon
                } else {
                    null     // hide button
                },
                rightButtonAction = {
                    if (emiList.size < 3) {
                        selectedEMI = null
                        squadViewModel.setShowEMIConfigPopup(true)
                    }
                }
            )

            SectionView(
                title = "Loan(s)",
                subtitle = "Below is your current EMI configuration - you can add or edit configurations"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    emiList.forEach { emi ->
                        EMIListRow(
                            emi = emi,
                            onEdit = {
                                selectedEMI = emi
                                oldEMIConfig = emi
                               squadViewModel.setShowEMIConfigPopup(true)
                            },
                            onDelete = {

                                selectedEMI = emi
                                oldEMIConfig = emi
                                AlertManager.shared.showAlert(
                                    title = SquadStrings.appName,
                                    message = "Are you sure you want to delete this EMI configuration?",
                                    primaryButtonTitle = "DELETE",
                                    primaryAction = {
                                        squadViewModel.deleteEMIConfiguration(
                                            showLoader = true,
                                            emiID = emi.id!!
                                        ) { success, error ->
                                            if (success) {
                                                val amount = selectedEMI?.loanAmount ?: 0
                                                val rate = selectedEMI?.emiInterestRate ?: 0.0

                                                squadViewModel.createSquadActivity(
                                                    activityType = SquadActivityType.OTHER_ACTIVITY,
                                                    userName = "SQUAD MANAGER",
                                                    amount = 0,
                                                    description = "Deleted EMI Config - Loan Amount $amount with interest $rate"
                                                )
                                            } else {
                                                Log.e("ManageLoan", error ?: "Failed delete")
                                            }
                                        }
                                    },
                                    secondaryButtonTitle = "NO",
                                    secondaryAction = {}
                                )
                            }
                        )
                    }
                }
            }
        }

        SSAlert()
        SSLoaderView()

        val showEMIPopup = squadViewModel.showEMIConfigPopup.collectAsStateWithLifecycle()

        if (showEMIPopup.value) {
            OverlayBackgroundView(
                showPopup = remember { mutableStateOf(true) },
                onDismiss = {  squadViewModel.setShowEMIConfigPopup(false)}
            ) {

                AddEMIPopup(
                    emi = selectedEMI,
                    onSave = { amount, months, interest ->
                        squadViewModel.setShowEMIConfigPopup(false)

                        handleAddEditEMI(
                            amount = amount,
                            months = months,
                            interest = interest,
                            squadViewModel = squadViewModel,
                            loaderManager = loaderManager,
                            selectedEMI = selectedEMI,
                            oldEMIConfig = oldEMIConfig,
                            onCompleted = {


                             }
                            )
                        selectedEMI = null
                    },
                    onCancel = {squadViewModel.setShowEMIConfigPopup(false)}
                )
            }


        }
    }
}

private fun handleAddEditEMI(
    amount: Int,
    months: Int,
    interest: Double,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager,
    selectedEMI: EMIConfiguration?,
    oldEMIConfig: EMIConfiguration?,
    onCompleted: () -> Unit
) {
    loaderManager.showLoader()

    val squadId = squadViewModel.squad.value?.squadID ?: ""

    // Create new EMI object
    val endOfMonth = CommonFunctions.getEndOfMonthFromDate(Date())

    val newEmi = EMIConfiguration(
        id = selectedEMI?.id ?: UUID.randomUUID().toString(),
        loanAmount = amount,
        emiMonths = months,
        emiInterestRate = interest,
        emiAmount = 0,
        interestAmount = 0,
        emiDate = endOfMonth?.asTimestamp,
        emiCreatedDate = Date().asTimestamp
    )

    // Calculate EMI + Interest
    val (emiAmount, interestAmount) = newEmi.calculateEMIAndInterest()
    newEmi.emiAmount = emiAmount
    newEmi.interestAmount = interestAmount

    // Save to Firestore
    squadViewModel.addOrUpdateEMIConfiguration(
        showLoader = false,
        squadID = squadId,
        emi = newEmi
    ) { success, error ->
        loaderManager.hideLoader()

        if (success) {
            val desc = if (selectedEMI == null) {
                "Created EMI Config - Loan Amount ${newEmi.loanAmount} with interest of ${newEmi.emiInterestRate}"
            } else {
                "Edited EMI Config - From Loan Amount ${oldEMIConfig?.loanAmount ?: 0} " +
                        "with interest of ${oldEMIConfig?.emiInterestRate ?: 0} " +
                        "to Loan Amount ${newEmi.loanAmount} with interest of ${newEmi.emiInterestRate}"
            }

            // Create Activity Log
            squadViewModel.createSquadActivity(
                activityType = SquadActivityType.OTHER_ACTIVITY,
                userName = "SQUAD MANAGER",
                amount = 0,
                description = desc
            ) {
                Log.d("ManageLoan", "EMI Config updated successfully")
                onCompleted()
            }
        } else {
            Log.e("ManageLoan", "❌ Failed to add/edit EMI: $error")
        }
    }
}

@Composable
fun AddEMIPopup(
    emi: EMIConfiguration?,                   // null → add, non-null → edit
    onSave: (Int, Int, Double) -> Unit,
    onCancel: () -> Unit
) {
    // ------------------------------
    // TEXT STATES
    // ------------------------------
    val amountState = remember { mutableStateOf("") }
    val monthsState = remember { mutableStateOf("") }
    val rateState = remember { mutableStateOf("") }

    // ------------------------------
    // PREFILL FOR EDIT MODE
    // ------------------------------
    LaunchedEffect(emi) {
        emi?.let {
            amountState.value = it.loanAmount.toString()
            monthsState.value = it.emiMonths.toString()
            rateState.value = String.format("%.2f", it.emiInterestRate)
        }
    }

    // ------------------------------
    // VALIDATION
    // ------------------------------
    val isValid = amountState.value.isNotEmpty()
            && monthsState.value.isNotEmpty()
            && rateState.value.isNotEmpty()

    // ------------------------------
    // LIVE EMI CALCULATION
    // ------------------------------
    val calculatedEMI = remember(
        amountState.value,
        monthsState.value,
        rateState.value
    ) {
        try {
            val amount = amountState.value.toDouble()
            val m = monthsState.value.toInt()
            val r = rateState.value.toDouble()

            val monthlyRate = (r / 100) / 12
            val numerator = amount * monthlyRate * Math.pow(1 + monthlyRate, m.toDouble())
            val denominator = Math.pow(1 + monthlyRate, m.toDouble()) - 1

            if (denominator != 0.0) {
                val emiValue = (numerator / denominator)
                val totalPayment = emiValue * m
                val totalInterest = totalPayment - amount
                emiValue to totalInterest
            } else null
        } catch (e: Exception) {
            null
        }
    }

    // ------------------------------
    // POPUP BACKGROUND
    // ------------------------------
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = Color.Black.copy(alpha = 0.15f),
                    spotColor = Color.Black.copy(alpha = 0.15f)
                )
                .background(AppColors.surface, RoundedCornerShape(18.dp))
                .padding(vertical = 20.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ------------------------------
            // TITLE
            Text(
                text = if (emi == null) "Add EMI Configuration" else "Edit EMI Configuration",
                style = AppFont.ibmPlexSans(18, FontWeight.SemiBold).copy(
                    color = AppColors.headerText
                )
            )

            // ------------------------------
            // INPUT FIELDS
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                SSTextField(
                    icon = Icons.Default.CurrencyRupee,
                    placeholder = "Loan Amount",
                    textState = amountState,
                    keyboardType = KeyboardType.Number
                )

                SSTextField(
                    icon = Icons.Default.CalendarMonth,
                    placeholder = "Months",
                    textState = monthsState,
                    keyboardType = KeyboardType.Number
                )

                SSTextField(
                    icon = Icons.Default.Percent,
                    placeholder = "Interest Rate (%)",
                    textState = rateState,
                    keyboardType = KeyboardType.Decimal
                )
            }

            // ------------------------------
            // EMI PREVIEW (Live Update)
            calculatedEMI?.let { (emiValue, interestValue) ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Estimated EMI: ₹${String.format("%.2f", emiValue)} per month",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = AppFont.ibmPlexSans(14, FontWeight.Medium)
                            .copy(color = AppColors.secondaryText)
                    )

                    Text(
                        text = "Total Interest: ₹${String.format("%.2f", interestValue)}",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = AppFont.ibmPlexSans(14, FontWeight.Medium)
                            .copy(color = AppColors.secondaryText)
                    )
                }
            }

            // ------------------------------
            // ACTION BUTTONS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Cancel
                SSCancelButton(
                    title = "Cancel",
                    modifier = Modifier.weight(1f),
                    action = { onCancel() }
                )

                // Save
                SSButton(
                    title = "Save",
                    isDisabled = !isValid,
                    modifier = Modifier.weight(1f),
                    action = {
                        onSave(
                            amountState.value.toInt(),
                            monthsState.value.toInt(),
                            rateState.value.toDouble()
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun EMIListRow(
    emi: EMIConfiguration,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)   // ← Leading + Trailing padding added
            .appShadow(AppShadows.card)
            .background(
                color = AppColors.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

            Text(
                "Loan Amount: ${emi.loanAmount.currencyFormattedWithCommas()}",
                style = AppFont.ibmPlexSans(14, FontWeight.SemiBold),
                color = AppColors.headerText
            )

            Text(
                "Tenure: ${emi.emiMonths} months @ ${emi.emiInterestRate}% interest",
                style = AppFont.ibmPlexSans(13, FontWeight.Normal),
                color = AppColors.secondaryText
            )

            Text(
                "EMI: ${emi.emiAmount.currencyFormattedWithCommas()} / month",
                style = AppFont.ibmPlexSans(13, FontWeight.Medium),
                color = AppColors.infoAccent
            )

            Text(
                "Total Interest: ${emi.interestAmount.currencyFormattedWithCommas()}",
                style = AppFont.ibmPlexSans(13, FontWeight.SemiBold),
                color = AppColors.successAccent
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit EMI",
                    tint = AppColors.primaryButton,
                    modifier = Modifier
                        .size(32.dp)
                        .background(AppColors.primaryButton.copy(alpha = 0.15f), CircleShape)
                        .padding(8.dp)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete EMI",
                    tint = AppColors.errorAccent,
                    modifier = Modifier
                        .size(32.dp)
                        .background(AppColors.errorAccent.copy(alpha = 0.15f), CircleShape)
                        .padding(8.dp)
                )
            }
        }
    }
}