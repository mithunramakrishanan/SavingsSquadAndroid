package com.android.savingssquad.view

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
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
import com.android.savingssquad.singleton.LoaderManager
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import kotlinx.coroutines.launch
import java.util.Date
import com.android.savingssquad.model.Squad
import com.android.savingssquad.model.Installment
import com.android.savingssquad.model.InterestType
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
import com.android.savingssquad.viewmodel.SSToast
import com.yourapp.utils.CommonFunctions
import kotlinx.coroutines.Dispatchers
import java.util.Calendar
import java.util.UUID
import kotlin.math.pow

@Composable
fun ManageLoanView(
    navController: NavController,
    squadViewModel: SquadViewModel) {
    var selectedEMI by remember { mutableStateOf<EMIConfiguration?>(null) }
    var oldEMIConfig by remember { mutableStateOf<EMIConfiguration?>(null) }

    val emiList = squadViewModel.emiConfigurations.collectAsState().value

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
                                                    memberId = "",
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
        val showEMIPopup = squadViewModel.showEMIConfigPopup.collectAsStateWithLifecycle()

        if (showEMIPopup.value) {
            OverlayBackgroundView(
                showPopup = remember { mutableStateOf(true) },
                onDismiss = {  squadViewModel.setShowEMIConfigPopup(false)}
            ) {

                AddEMIPopup(
                    emi = selectedEMI,
                    onSave = { amount, months, interest,type ->
                        squadViewModel.setShowEMIConfigPopup(false)

                        handleAddEditEMI(
                            amount = amount,
                            months = months,
                            interest = interest,
                            type = type,
                            squadViewModel = squadViewModel,
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
    type: InterestType,
    squadViewModel: SquadViewModel,
    selectedEMI: EMIConfiguration?,
    oldEMIConfig: EMIConfiguration?,
    onCompleted: () -> Unit
) {
    LoaderManager.shared.showLoader()

    val squadId = squadViewModel.squad.value?.squadID ?: ""

    // Create new EMI object
    val endOfMonth = CommonFunctions.getEndOfMonthFromDate(Date())

    val newEmi = EMIConfiguration(
        id = selectedEMI?.id ?: UUID.randomUUID().toString(),
        loanAmount = amount,
        emiMonths = months,
        emiInterestRate = interest,
        interestType = type,
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
        LoaderManager.shared.hideLoader()

        if (success) {
            val desc = if (selectedEMI == null) {
                "EMI configuration created (Loan ₹${newEmi.loanAmount}, Interest ${newEmi.emiInterestRate}% - ${newEmi.interestType.name})"
            } else {
                "EMI configuration updated: Loan ₹${oldEMIConfig?.loanAmount ?: 0} → ₹${newEmi.loanAmount}, Interest ${oldEMIConfig?.emiInterestRate ?: 0}% (${oldEMIConfig?.interestType?.name ?: "-"}) → ${newEmi.emiInterestRate}% (${newEmi.interestType.name})"
            }

            // Create Activity Log
            squadViewModel.createSquadActivity(
                activityType = SquadActivityType.OTHER_ACTIVITY,
                userName = "SQUAD MANAGER",
                memberId = "",
                amount = 0,
                description = desc
            ) { success, error ->

                if (success) {

                    println("✅ Activity created")

                    AlertManager.shared.showAlert(
                        title = SquadStrings.appName,
                        message = desc,
                        primaryButtonTitle = "OK",
                        primaryAction = {}
                    )

                    onCompleted()

                } else {

                    println("❌ Error: $error")

                }

            }
        } else {
            Log.e("ManageLoan", "❌ Failed to add/edit EMI: $error")
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun AddEMIPopup(
    emi: EMIConfiguration?,
    onSave: (Int, Int, Double, InterestType) -> Unit,   // 🔹 CHANGED
    onCancel: () -> Unit
) {

    val amountState = remember { mutableStateOf("") }
    val monthsState = remember { mutableStateOf("") }
    val rateState = remember { mutableStateOf("") }
    val interestTypeState = remember { mutableStateOf(InterestType.YEARLY) } // 🔹 NEW
    var dropdownExpanded by remember { mutableStateOf(false) }              // 🔹 NEW

    LaunchedEffect(emi) {
        emi?.let {
            amountState.value = it.loanAmount.toString()
            monthsState.value = it.emiMonths.toString()
            rateState.value = String.format("%.2f", it.emiInterestRate)
            interestTypeState.value = it.interestType   // 🔹 NEW
        }
    }

    val isValid =
        amountState.value.isNotEmpty() &&
                monthsState.value.isNotEmpty() &&
                rateState.value.isNotEmpty()

    val calculatedEMI = remember(
        amountState.value,
        monthsState.value,
        rateState.value,
        interestTypeState.value   // 🔹 NEW — recompute when type changes
    ) {
        runCatching {

            val principal = amountState.value.toDouble()
            val tenure = monthsState.value.toInt()
            val rate = rateState.value.toDouble()

            require(principal > 0 && tenure > 0 && rate >= 0)

            val monthlyRate = interestTypeState.value.monthlyRate(rate)   // 🔹 CHANGED

            if (monthlyRate == 0.0) {
                val emi = principal / tenure
                return@runCatching emi.round(2) to 0.0
            }

            val r = monthlyRate
            val n = tenure.toDouble()

            val factor = (1 + r).pow(n)

            val emi = (principal * r * factor) / (factor - 1)

            val totalPayment = emi * n
            val totalInterest = totalPayment - principal

            emi.round(2) to totalInterest.round(2)

        }.getOrNull()
    }

    // MARK: - Backdrop
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                onCancel()
            },
        contentAlignment = Alignment.Center
    ) {

        // MARK: - Card
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AppColors.surface.copy(alpha = 0.98f),
                            AppColors.surface.copy(alpha = 0.92f)
                        )
                    )
                )

                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            // MARK: Drag handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Gray.copy(alpha = 0.3f))
            )

            // MARK: Title
            Text(
                text = if (emi == null) "Add EMI Plan" else "Edit EMI Plan",
                style = AppFont.ibmPlexSans(20, FontWeight.SemiBold),
                color = AppColors.headerText
            )

            // MARK: Inputs
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                SSTextField(
                    icon = Icons.Default.CurrencyRupee,
                    placeholder = "Loan Amount",
                    textState = amountState,
                    keyboardType = KeyboardType.Number
                )

                SSTextField(
                    icon = Icons.Default.CalendarMonth,
                    placeholder = "Tenure (Months)",
                    textState = monthsState,
                    keyboardType = KeyboardType.Number
                )

                SSTextField(
                    icon = Icons.Default.Percent,
                    placeholder = "Interest Rate (%)",
                    textState = rateState,
                    keyboardType = KeyboardType.Decimal
                )

                // 🔹 NEW — Interest type dropdown
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.6f))
                            .clickable { dropdownExpanded = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Update,
                            contentDescription = null,
                            tint = AppColors.secondaryText
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Interest Type: ${interestTypeState.value.label}",
                            style = AppFont.ibmPlexSans(15, FontWeight.Normal),
                            color = AppColors.headerText,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = AppColors.secondaryText
                        )
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        InterestType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.label) },
                                onClick = {
                                    interestTypeState.value = type
                                    dropdownExpanded = false
                                },
                                trailingIcon = {
                                    if (type == interestTypeState.value) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // MARK: EMI Preview Card
            calculatedEMI?.let { (emiValue, interestValue) ->

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.65f))
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {

                    Text(
                        text = "Estimated EMI",
                        style = AppFont.ibmPlexSans(13, FontWeight.Medium),
                        color = AppColors.secondaryText
                    )

                    Text(
                        text = "₹ ${"%.0f".format(emiValue)} / month",
                        style = AppFont.ibmPlexSans(18, FontWeight.Bold),
                        color = AppColors.headerText
                    )

                    Text(
                        text = "Total Interest: ₹ ${"%.0f".format(interestValue)}",
                        style = AppFont.ibmPlexSans(13, FontWeight.Medium),
                        color = AppColors.secondaryText
                    )
                }
            }

            // MARK: Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                SSCancelButton(
                    title = "Cancel",
                    modifier = Modifier.weight(1f),
                    action = onCancel
                )

                SSButton(
                    title = if (emi == null) "Save EMI" else "Update EMI",
                    isDisabled = !isValid,
                    modifier = Modifier.weight(1f),
                    action = {
                        onSave(
                            amountState.value.toInt(),
                            monthsState.value.toInt(),
                            rateState.value.toDouble(),
                            interestTypeState.value   // 🔹 NEW
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

    var isExpanded by remember { mutableStateOf(false) }

    val memberLoan = remember(emi) {
        CommonFunctions.generateMemberLoan(
            emiConfig = emi,
            memberID = "",
            memberName = ""
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .appShadow(AppShadows.card)
            .background(
                color = AppColors.surface,
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 1.dp,
                color = AppColors.border.copy(alpha = 0.25f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // MARK: Header row — loan amount + edit/delete (manager view, no request button)

        Row(verticalAlignment = Alignment.CenterVertically) {

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    "Loan Amount",
                    style = AppFont.ibmPlexSans(10, FontWeight.Medium),
                    color = AppColors.secondaryText
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    emi.loanAmount.currencyFormattedWithCommas(),
                    style = AppFont.ibmPlexSans(20, FontWeight.Bold),
                    color = AppColors.headerText
                )

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Text(
                        "${emi.emiMonths} Months",
                        style = AppFont.ibmPlexSans(10, FontWeight.Medium),
                        color = AppColors.primaryBrand
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

            Row(horizontalArrangement = Arrangement.spacedBy(30.dp)) {

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(30.dp)
                        .background(AppColors.primaryButton.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit EMI",
                        tint = AppColors.primaryButton,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(30.dp)
                        .background(AppColors.errorAccent.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete EMI",
                        tint = AppColors.errorAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = AppColors.secondaryText.copy(alpha = 0.12f))

        // MARK: Monthly EMI / Interest / Total

        Row(modifier = Modifier.fillMaxWidth()) {
            InfoView(
                modifier = Modifier.weight(1f),
                title = "Monthly EMI",
                value = emi.emiAmount.currencyFormattedWithCommas()
            )
            InfoView(
                modifier = Modifier.weight(1f),
                title = "Interest",
                value = emi.interestAmount.currencyFormattedWithCommas()
            )
            InfoView(
                modifier = Modifier.weight(1f),
                title = "Total",
                value = (emi.loanAmount + emi.interestAmount).currencyFormattedWithCommas()
            )
        }

        HorizontalDivider(color = AppColors.secondaryText.copy(alpha = 0.12f))

        // MARK: Installment Details toggle (manager view — no dates)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ListAlt,
                contentDescription = null,
                tint = AppColors.primaryBrand,
                modifier = Modifier.size(14.dp)
            )

            Spacer(Modifier.width(6.dp))

            Text(
                "Installment Details",
                style = AppFont.ibmPlexSans(12, FontWeight.SemiBold),
                color = AppColors.primaryBrand
            )

            Spacer(Modifier.weight(1f))

            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = AppColors.primaryBrand
            )
        }

        // MARK: Expanded installment list — details only, no due dates

        AnimatedVisibility(visible = isExpanded) {

            Column {

                HorizontalDivider(color = AppColors.secondaryText.copy(alpha = 0.12f))

                Spacer(Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = AppColors.background,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    memberLoan.installments.forEachIndexed { i, installment ->

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(
                                installment.installmentNumber,
                                style = AppFont.ibmPlexSans(12, FontWeight.SemiBold),
                                color = AppColors.headerText,
                                modifier = Modifier.weight(1f)
                            )

                            Column(horizontalAlignment = Alignment.End) {

                                Text(
                                    installment.installmentAmount.currencyFormattedWithCommas(),
                                    style = AppFont.ibmPlexSans(12, FontWeight.Bold),
                                    color = AppColors.headerText
                                )

                                Text(
                                    "Interest ${installment.interestAmount.currencyFormattedWithCommas()}",
                                    style = AppFont.ibmPlexSans(10, FontWeight.Normal),
                                    color = AppColors.secondaryText
                                )
                            }
                        }

                        if (i != memberLoan.installments.lastIndex) {
                            HorizontalDivider(color = AppColors.secondaryText.copy(alpha = 0.12f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoView(
    modifier: Modifier = Modifier,
    title: String,
    value: String
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
            style = AppFont.ibmPlexSans(13, FontWeight.Bold),
            color = AppColors.headerText
        )
    }
}

fun Double.round(decimals: Int): Double {
    val factor = 10.0.pow(decimals)
    return kotlin.math.round(this * factor) / factor
}