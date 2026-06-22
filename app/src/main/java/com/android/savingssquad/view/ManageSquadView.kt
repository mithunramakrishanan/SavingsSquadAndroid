package com.android.savingssquad.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.viewmodel.LoaderManager
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.SquadActivityType
import kotlinx.coroutines.launch
import java.util.Date
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.asTimestamp
import com.android.savingssquad.viewmodel.AlertManager
import com.yourapp.utils.CommonFunctions
import java.util.Calendar

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ManageSquadView(
    navController: NavController,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager = LoaderManager.shared,
    onDismiss: (() -> Unit)? = null // optional callback, like SwiftUI dismiss
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var totalSquadAmount by remember { mutableStateOf(0) }
    // collect squad from viewmodel (if present)
    val squadState by squadViewModel.squad.collectAsStateWithLifecycle()

    // Local editable state
    val squadName = remember { mutableStateOf("") }
    val squadPhoneNumber = remember { mutableStateOf("") }

    // errors
    var phoneError by remember { mutableStateOf("") }
    var squadMonthError by remember { mutableStateOf("") }
    var squadAmountError by remember { mutableStateOf("") }
    var squadDurationErrorMessage by remember { mutableStateOf("") }

    val squadDuration = remember { mutableStateOf(0) }
    val squadAmount = remember { mutableStateOf(0) }
    val originalSquadDuration = remember { mutableStateOf(0) }
    val originalSquadAmount = remember { mutableStateOf(0) }

    // focus requesters for keyboard navigation
    val durationFocusRequester = remember { FocusRequester() }
    val amountFocusRequester = remember { FocusRequester() }

    LaunchedEffect(squadState) {
        squadState?.let { gf ->
            squadName.value = gf.squadName
            squadPhoneNumber.value = gf.phoneNumber

            squadDuration.value = gf.totalDuration
            squadAmount.value = gf.monthlyContribution

            originalSquadDuration.value = gf.totalDuration
            originalSquadAmount.value = gf.monthlyContribution
            totalSquadAmount = gf.currentAvailableAmount
        }
    }

    // hasChanges equivalent
    val hasChanges by remember {
        derivedStateOf {
            squadDuration.value != originalSquadDuration.value ||
                    squadAmount.value != originalSquadAmount.value
        }
    }

    BackHandler {
        onDismiss?.invoke() ?: navController.popBackStack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackgroundGradient()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
        ) {
            // Navigation Bar (use your SSNavigationBar)
            SSNavigationBar(title = "Manage Squad",navController)

            Spacer(modifier = Modifier.height(8.dp))

            // Section: Squad Details
            SectionView(title = "Squad Details") {

                Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {

                    // Squad Name
                    SSTextField(
                        icon = Icons.Default.Business,
                        placeholder = "Squad Name",
                        textState = squadName,
                        disabled = true
                    )

                    // Phone Number
                    SSTextField(
                        icon = Icons.Default.Phone,
                        placeholder = "Phone Number",
                        textState = squadPhoneNumber,
                        disabled = true,
                        error = phoneError
                    )

                    // editable text
                    val durationText = remember { mutableStateOf("") }

                    LaunchedEffect(durationText.value) {
                        squadDuration.value = durationText.value.toIntOrNull() ?: 0
                    }

                    LaunchedEffect(squadState) {
                        squadState?.let { gf ->
                            squadDuration.value = gf.totalDuration
                            durationText.value = gf.totalDuration.toString()
                        }
                    }

                    SSTextField(
                        icon = Icons.Default.CalendarToday,
                        placeholder = "Squad Duration",
                        textState = durationText,
                        keyboardType = KeyboardType.Number,
                        error = squadMonthError
                    )


                    // Converted duration display
                    Text(
                        text = CommonFunctions.convertMonthsToYearsAndMonths(squadDuration.value),
                        style = AppFont.ibmPlexSans(12, FontWeight.Bold),
                        color = AppColors.headerText,
                        modifier = Modifier.padding(start = 30.dp)
                    )

                    val amountState = remember { mutableStateOf("") }

                    LaunchedEffect(amountState.value) {
                        squadAmount.value = amountState.value.toIntOrNull() ?: 0
                    }

                    LaunchedEffect(squadState) {
                        squadState?.let { gf ->
                            squadAmount.value = gf.monthlyContribution
                            amountState.value = gf.monthlyContribution.toString()
                        }
                    }

                    SSTextField(
                        icon = Icons.Default.CreditCard,
                        placeholder = "Squad Amount",
                        textState = amountState,
                        keyboardType = KeyboardType.Number,
                        error = squadAmountError
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Update Button
            SSButton(
                title = "Update Squad Details",
                isButtonLoading = false,
                isDisabled = !hasChanges,
                action = {
                    focusManager.clearFocus()
                    keyboardController?.hide()

                    coroutineScope.launch {

                        validateSquadDuration(
                            squadViewModel = squadViewModel,
                            currentDuration = squadDuration.value,
                            onInvalid = { requiredMonths, resetValue ->

                                val message = "Squad duration is too short! Needs to be at least $requiredMonths months."

                                AlertManager.shared.showAlert(
                                    title = SquadStrings.appName,
                                    message = message,
                                    primaryButtonTitle = SquadStrings.ok
                                ) {
                                    squadDuration.value = resetValue
                                }
                            },
                            onValid = {
                                squadMonthError = ""

                                saveChanges(
                                    squadViewModel = squadViewModel,
                                    loaderManager = loaderManager,
                                    currentDuration = squadDuration.value,
                                    currentAmount = squadAmount.value,
                                    phone = squadPhoneNumber.value,
                                    originalDuration = originalSquadDuration.value,
                                    originalAmount = originalSquadAmount.value,
                                    onSuccess = {
                                        originalSquadDuration.value = squadDuration.value
                                        originalSquadAmount.value = squadAmount.value
                                        onDismiss?.invoke() ?: navController.popBackStack()
                                    },
                                    setErrors = { phoneErr, monthErr, amountErr ->
                                        phoneError = phoneErr
                                        squadMonthError = monthErr
                                        squadAmountError = amountErr
                                    }
                                )
                            }
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            SquadAmountQuickEditView(
                squadViewModel = squadViewModel,
                onSave = { newValue, reason ->
                    println("Amount: $newValue")
                    println("Reason: $reason")

                    println("Updated amount: $newValue")

                    // 🔹 Show loader
                    LoaderManager.shared.showLoader()

                    val squadId = squadViewModel.squad.value?.squadID ?: ""

                    squadViewModel.updateSquadTotalAmount(
                        squadId = squadId,
                        amount = newValue
                    ) { success, error ->

                        if (success) {

                            squadViewModel.createSquadActivity(
                                activityType = SquadActivityType.OTHER_ACTIVITY,
                                userName = "SQUAD MANAGER",
                                memberId = "",
                                amount = newValue,
                                description = "Squad manager updated squad amount from $totalSquadAmount to $newValue for $reason"
                            ) { _, _ ->

                                totalSquadAmount = newValue

                                LoaderManager.shared.hideLoader()

                                // 🔹 Show Alert
                                AlertManager.shared.showAlert(
                                    title = SquadStrings.appName,
                                    message = "Updated squad amount: $totalSquadAmount → $newValue",
                                    primaryButtonTitle = "OK",
                                    primaryAction = { }
                                )
                            }

                        } else {

                            LoaderManager.shared.hideLoader()

                            println("Error updating squad: $error")
                        }
                    }
                }
            )
        }

        SSAlert()
        SSLoaderView()
    }
}

/**
 * Save logic copied & adapted from Swift code.
 * - Validates fields
 * - Prepares updated Squad and calls viewmodel.updateSquad(...)
 * - Calls contibutionEditWhenMonthsChanged(...) if update succeeded and duration changed
 */
private fun saveChanges(
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager,
    currentDuration: Int,
    currentAmount: Int,
    phone: String,
    originalDuration: Int,
    originalAmount: Int,
    onSuccess: () -> Unit,
    setErrors: (phoneError: String, monthError: String, amountError: String) -> Unit
) {
    val phoneErr = if (Regex("^[0-9]{10}$").matches(phone)) "" else "Enter a valid 10-digit phone number"
    val monthErr = if (currentDuration == 0) "Squad Month is required" else ""
    val amountErr = if (currentAmount <= 0) "Squad Amount is required" else ""

    setErrors(phoneErr, monthErr, amountErr)
    if (phoneErr.isNotEmpty() || monthErr.isNotEmpty() || amountErr.isNotEmpty()) return

    val gf = squadViewModel.squad.value ?: return

    val squadDurationEdited = gf.totalDuration != currentDuration
    val squadAmountEdited = gf.monthlyContribution != currentAmount
    if (!squadDurationEdited && !squadAmountEdited) return

    loaderManager.showLoader()

    val endDate: Date? = CommonFunctions.getFutureMonthYearDate(
        from = gf.squadStartDate?.toDate() ?: Date(),
        monthsToAdd = currentDuration
    )

    val updatedSquad = gf.copy().apply {
        phoneNumber = phone
        totalDuration = currentDuration
        squadEndDate = endDate?.asTimestamp
        monthlyContribution = currentAmount
    }

    squadViewModel.updateSquad(true, updatedSquad) { success, updatedGF, error ->
        if (success && updatedGF != null) {
            squadViewModel.contibutionEditWhenMonthsChanged(
                showLoader = true,
                squad = updatedGF,
                squadEndDate = endDate ?: Date(),
                amount = updatedGF.monthlyContribution.toString()
            ) { contSuccess, message ->
                loaderManager.hideLoader()
                if (contSuccess) {
                    val description = createActivityDescription(
                        oldDuration = originalDuration,
                        newDuration = currentDuration,
                        oldSquadAmount = originalAmount,
                        newSquadAmount = currentAmount,
                        squadAmountEdited = squadAmountEdited,
                        squadDurationEdited = squadDurationEdited
                    )
                    squadViewModel.createSquadActivity(
                        activityType = com.android.savingssquad.singleton.SquadActivityType.OTHER_ACTIVITY,
                        userName = "SQUAD MANAGER",
                        memberId = "",
                        amount = 0,
                        description = description
                    )
                } else {
                    println("❌ Error updating contributions: $message")
                }
            }
        } else {
            loaderManager.hideLoader()
            println("❌ Error: ${error ?: "Unknown error"}")
        }
    }
}

private fun createActivityDescription(
    oldDuration: Int,
    newDuration: Int,
    oldSquadAmount: Int,
    newSquadAmount: Int,
    squadAmountEdited: Boolean,
    squadDurationEdited: Boolean
): String {
    return when {
        squadAmountEdited && squadDurationEdited -> {
            "Changed squad duration from ${CommonFunctions.convertMonthsToYearsAndMonths(oldDuration)} to ${CommonFunctions.convertMonthsToYearsAndMonths(newDuration)} and squad amount from $oldSquadAmount to $newSquadAmount"
        }
        squadAmountEdited -> "Changed squad amount from $oldSquadAmount to $newSquadAmount"
        squadDurationEdited -> "Changed squad duration from ${CommonFunctions.convertMonthsToYearsAndMonths(oldDuration)} to ${CommonFunctions.convertMonthsToYearsAndMonths(newDuration)}"
        else -> ""
    }
}


/**
 * Small helper composable to mimic SwiftUI caption text used for converted duration display.
 * You can remove/replace with your actual caption composable.
 */
@Composable
fun SimpleCaption(text: String) {
    androidx.compose.material3.Text(
        text = text,
        style = AppFont.ibmPlexSans(12, androidx.compose.ui.text.font.FontWeight.Bold),
        color = AppColors.headerText,
        modifier = Modifier.padding(start = 8.dp)
    )
}

private fun validateSquadDuration(
    squadViewModel: SquadViewModel,
    currentDuration: Int,
    onInvalid: (requiredMonths: Int, resetValue: Int) -> Unit,
    onValid: () -> Unit
) {
    val gf = squadViewModel.squad.value ?: return

    val startDate = gf.squadStartDate?.toDate() ?: Date()
    val currentDate = Date()

    val calendar = Calendar.getInstance()
    val components = calendar.run {
        val start = Calendar.getInstance().apply { time = startDate }
        val end = Calendar.getInstance().apply { time = currentDate }
        val years = end.get(Calendar.YEAR) - start.get(Calendar.YEAR)
        val months = end.get(Calendar.MONTH) - start.get(Calendar.MONTH)
        years to months
    }

    val totalMonthsDifference = (components.first * 12) + components.second
    val adjustedSquadDuration = currentDuration - 1

    if (adjustedSquadDuration < totalMonthsDifference) {
        val required = totalMonthsDifference + 1

        onInvalid(required, gf.totalDuration)   // Pass info to reset UI or show popup
    } else {
        onValid()
    }
}

@Composable
fun SquadAmountQuickEditView(
    squadViewModel: SquadViewModel,
    onSave: ((Int, String) -> Unit)? = null
) {

    var isEditing by remember { mutableStateOf(false) }
    var tempAmount by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var reasonError by remember { mutableStateOf("") }

    val squad = squadViewModel.squad.collectAsState().value

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = Color(0x22000000),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(18.dp)
    ) {

        // 🔹 HEADER
        Row(verticalAlignment = Alignment.CenterVertically) {

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = "Squad",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0D1117)
                )

                Text(
                    text = "Current available balance",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }

            Icon(
                imageVector = Icons.Default.CreditCard,
                contentDescription = null,
                tint = Color(0xFF1D9E75)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 🔹 FIELD + BUTTON
        Row(verticalAlignment = Alignment.CenterVertically) {

            TextField(
                value = if (isEditing)
                    tempAmount
                else
                    (squad?.currentAvailableAmount?.toString() ?: "0"),

                onValueChange = { tempAmount = it },
                enabled = isEditing,
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (isEditing)
                            Color(0x141D9E75)
                        else
                            Color(0xFFF5F6F8),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(2.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = {

                    if (isEditing) {

                        // 🔴 VALIDATION
                        reasonError = if (reason.trim().isEmpty()) {
                            "Reason is required"
                        } else ""

                        if (reasonError.isNotEmpty()) return@Button

                        val newValue = tempAmount.toIntOrNull()
                            ?: (squad?.currentAvailableAmount ?: 0)

                        squadViewModel.squad.value?.currentAvailableAmount = newValue

                        onSave?.invoke(newValue, reason)

                        reason = ""
                    } else {
                        tempAmount = squad?.currentAvailableAmount?.toString() ?: "0"
                    }

                    isEditing = !isEditing
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEditing) Color(0xFF1D9E75) else Color(0xFF0D1117)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isEditing) "Save" else "Edit",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 🔹 REASON FIELD (ONLY IN EDIT MODE)
        if (isEditing) {

            Spacer(modifier = Modifier.height(10.dp))

            TextField(
                value = reason,
                onValueChange = {
                    reason = it
                    reasonError = ""
                },
                placeholder = { Text("Enter reason for update") },
                isError = reasonError.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F6F8), RoundedCornerShape(12.dp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Red
                )
            )

            if (reasonError.isNotEmpty()) {
                Text(
                    text = reasonError,
                    color = Color.Red,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 🔹 INFO TEXT
        Text(
            text = "Any update is securely recorded and visible to all squad members for transparency.",
            fontSize = 11.sp,
            color = Color(0xFF9CA3AF)
        )
    }
}