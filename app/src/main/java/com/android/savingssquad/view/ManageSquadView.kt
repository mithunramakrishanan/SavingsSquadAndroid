package com.android.savingssquad.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.viewmodel.LoaderManager
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
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
                                    primaryButtonTitle = "OK"
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
                        amount = 0,
                        description = description
                    ) {
                        onSuccess()
                    }
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