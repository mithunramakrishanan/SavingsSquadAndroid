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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.viewmodel.LoaderManager
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import kotlinx.coroutines.launch
import java.util.Date
import com.android.savingssquad.model.GroupFund
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.asTimestamp
import com.android.savingssquad.viewmodel.AlertManager
import com.yourapp.utils.CommonFunctions
import java.util.Calendar

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ManageGroupFundView(
    navController: NavController,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager = LoaderManager.shared,
    onDismiss: (() -> Unit)? = null // optional callback, like SwiftUI dismiss
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // collect groupFund from viewmodel (if present)
    val groupFundState by squadViewModel.groupFund.collectAsStateWithLifecycle()

    // Local editable state
    val groupFundName = remember { mutableStateOf("") }
    val groupFundPhoneNumber = remember { mutableStateOf("") }

    // errors
    var phoneError by remember { mutableStateOf("") }
    var groupFundMonthError by remember { mutableStateOf("") }
    var groupFundAmountError by remember { mutableStateOf("") }
    var groupFundDurationErrorMessage by remember { mutableStateOf("") }

    val groupFundDuration = remember { mutableStateOf(0) }
    val groupFundAmount = remember { mutableStateOf(0) }
    val originalGroupFundDuration = remember { mutableStateOf(0) }
    val originalGroupFundAmount = remember { mutableStateOf(0) }

    // focus requesters for keyboard navigation
    val durationFocusRequester = remember { FocusRequester() }
    val amountFocusRequester = remember { FocusRequester() }

    LaunchedEffect(groupFundState) {
        groupFundState?.let { gf ->
            groupFundName.value = gf.groupFundName
            groupFundPhoneNumber.value = gf.phoneNumber

            groupFundDuration.value = gf.totalDuration
            groupFundAmount.value = gf.monthlyContribution

            originalGroupFundDuration.value = gf.totalDuration
            originalGroupFundAmount.value = gf.monthlyContribution
        }
    }

    // hasChanges equivalent
    val hasChanges by remember {
        derivedStateOf {
            groupFundDuration.value != originalGroupFundDuration.value ||
                    groupFundAmount.value != originalGroupFundAmount.value
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
            SSNavigationBar(title = "Manage Group Fund",navController)

            Spacer(modifier = Modifier.height(8.dp))

            // Section: Group Fund Details
            SectionView(title = "Group Fund Details") {

                Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {

                    // Group Fund Name
                    SSTextField(
                        icon = Icons.Default.Business,
                        placeholder = "Group Fund Name",
                        textState = groupFundName,
                        disabled = true
                    )

                    // Phone Number
                    SSTextField(
                        icon = Icons.Default.Phone,
                        placeholder = "Phone Number",
                        textState = groupFundPhoneNumber,
                        disabled = true,
                        error = phoneError
                    )

                    // editable text
                    val durationText = remember { mutableStateOf("") }

                    LaunchedEffect(durationText.value) {
                        groupFundDuration.value = durationText.value.toIntOrNull() ?: 0
                    }

                    LaunchedEffect(groupFundState) {
                        groupFundState?.let { gf ->
                            groupFundDuration.value = gf.totalDuration
                            durationText.value = gf.totalDuration.toString()
                        }
                    }

                    SSTextField(
                        icon = Icons.Default.CalendarToday,
                        placeholder = "Group Fund Duration",
                        textState = durationText,
                        keyboardType = KeyboardType.Number,
                        error = groupFundMonthError
                    )


                    // Converted duration display
                    Text(
                        text = CommonFunctions.convertMonthsToYearsAndMonths(groupFundDuration.value),
                        style = AppFont.ibmPlexSans(12, FontWeight.Bold),
                        color = AppColors.headerText,
                        modifier = Modifier.padding(start = 30.dp)
                    )

                    val amountState = remember { mutableStateOf("") }

                    LaunchedEffect(amountState.value) {
                        groupFundAmount.value = amountState.value.toIntOrNull() ?: 0
                    }

                    LaunchedEffect(groupFundState) {
                        groupFundState?.let { gf ->
                            groupFundAmount.value = gf.monthlyContribution
                            amountState.value = gf.monthlyContribution.toString()
                        }
                    }

                    SSTextField(
                        icon = Icons.Default.CreditCard,
                        placeholder = "Group Fund Amount",
                        textState = amountState,
                        keyboardType = KeyboardType.Number,
                        error = groupFundAmountError
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Update Button
            SSButton(
                title = "Update Group Fund Details",
                isButtonLoading = false,
                isDisabled = !hasChanges,
                action = {
                    focusManager.clearFocus()
                    keyboardController?.hide()

                    coroutineScope.launch {

                        validateGroupFundDuration(
                            squadViewModel = squadViewModel,
                            currentDuration = groupFundDuration.value,
                            onInvalid = { requiredMonths, resetValue ->

                                val message = "Group Fund duration is too short! Needs to be at least $requiredMonths months."

                                AlertManager.shared.showAlert(
                                    title = SquadStrings.appName,
                                    message = message,
                                    primaryButtonTitle = "OK"
                                ) {
                                    groupFundDuration.value = resetValue
                                }
                            },
                            onValid = {
                                groupFundMonthError = ""

                                saveChanges(
                                    squadViewModel = squadViewModel,
                                    loaderManager = loaderManager,
                                    currentDuration = groupFundDuration.value,
                                    currentAmount = groupFundAmount.value,
                                    phone = groupFundPhoneNumber.value,
                                    originalDuration = originalGroupFundDuration.value,
                                    originalAmount = originalGroupFundAmount.value,
                                    onSuccess = {
                                        originalGroupFundDuration.value = groupFundDuration.value
                                        originalGroupFundAmount.value = groupFundAmount.value
                                        onDismiss?.invoke() ?: navController.popBackStack()
                                    },
                                    setErrors = { phoneErr, monthErr, amountErr ->
                                        phoneError = phoneErr
                                        groupFundMonthError = monthErr
                                        groupFundAmountError = amountErr
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
 * - Prepares updated GroupFund and calls viewmodel.updateGroupFund(...)
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
    val monthErr = if (currentDuration == 0) "Group Fund Month is required" else ""
    val amountErr = if (currentAmount <= 0) "Group Fund Amount is required" else ""

    setErrors(phoneErr, monthErr, amountErr)
    if (phoneErr.isNotEmpty() || monthErr.isNotEmpty() || amountErr.isNotEmpty()) return

    val gf = squadViewModel.groupFund.value ?: return

    val groupFundDurationEdited = gf.totalDuration != currentDuration
    val groupFundAmountEdited = gf.monthlyContribution != currentAmount
    if (!groupFundDurationEdited && !groupFundAmountEdited) return

    loaderManager.showLoader()

    val endDate: Date? = CommonFunctions.getFutureMonthYearDate(
        from = gf.groupFundStartDate?.toDate() ?: Date(),
        monthsToAdd = currentDuration
    )

    val updatedGroupFund = gf.copy().apply {
        phoneNumber = phone
        totalDuration = currentDuration
        groupFundEndDate = endDate?.asTimestamp
        monthlyContribution = currentAmount
    }

    squadViewModel.updateGroupFund(true, updatedGroupFund) { success, updatedGF, error ->
        if (success && updatedGF != null) {
            squadViewModel.contibutionEditWhenMonthsChanged(
                showLoader = true,
                groupFund = updatedGF,
                groupFundEndDate = endDate ?: Date(),
                amount = updatedGF.monthlyContribution.toString()
            ) { contSuccess, message ->
                loaderManager.hideLoader()
                if (contSuccess) {
                    val description = createActivityDescription(
                        oldDuration = originalDuration,
                        newDuration = currentDuration,
                        oldGroupFundAmount = originalAmount,
                        newGroupFundAmount = currentAmount,
                        groupFundAmountEdited = groupFundAmountEdited,
                        groupFundDurationEdited = groupFundDurationEdited
                    )
                    squadViewModel.createGroupFundActivity(
                        activityType = com.android.savingssquad.singleton.GroupFundActivityType.OTHER_ACTIVITY,
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
    oldGroupFundAmount: Int,
    newGroupFundAmount: Int,
    groupFundAmountEdited: Boolean,
    groupFundDurationEdited: Boolean
): String {
    return when {
        groupFundAmountEdited && groupFundDurationEdited -> {
            "Changed groupFund duration from ${CommonFunctions.convertMonthsToYearsAndMonths(oldDuration)} to ${CommonFunctions.convertMonthsToYearsAndMonths(newDuration)} and groupFund amount from $oldGroupFundAmount to $newGroupFundAmount"
        }
        groupFundAmountEdited -> "Changed groupFund amount from $oldGroupFundAmount to $newGroupFundAmount"
        groupFundDurationEdited -> "Changed groupFund duration from ${CommonFunctions.convertMonthsToYearsAndMonths(oldDuration)} to ${CommonFunctions.convertMonthsToYearsAndMonths(newDuration)}"
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

private fun validateGroupFundDuration(
    squadViewModel: SquadViewModel,
    currentDuration: Int,
    onInvalid: (requiredMonths: Int, resetValue: Int) -> Unit,
    onValid: () -> Unit
) {
    val gf = squadViewModel.groupFund.value ?: return

    val startDate = gf.groupFundStartDate?.toDate() ?: Date()
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
    val adjustedGroupFundDuration = currentDuration - 1

    if (adjustedGroupFundDuration < totalMonthsDifference) {
        val required = totalMonthsDifference + 1

        onInvalid(required, gf.totalDuration)   // Pass info to reset UI or show popup
    } else {
        onValid()
    }
}