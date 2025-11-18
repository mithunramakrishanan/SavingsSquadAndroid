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
import androidx.compose.material.icons.filled.QrCode2
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
import com.android.savingssquad.R
import com.android.savingssquad.model.BeneficiaryResult
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
import com.android.savingssquad.viewmodel.FirebaseFunctionsManager
import com.yourapp.utils.CommonFunctions
import kotlinx.coroutines.Dispatchers
import java.util.Calendar

@Composable
fun BankDetailsView(
    navController: NavController?,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager = LoaderManager.shared
) {
    // ---------- State Mapping ----------
    var accountHoldernameState = remember { mutableStateOf("") }
    var accountHoldernameError by remember { mutableStateOf("") }

    val upiState = remember { mutableStateOf("") }
    var upiIDError by remember { mutableStateOf("") }

    val groupFund by squadViewModel.groupFund.collectAsState()
    val currentMember by squadViewModel.currentMember.collectAsState()

    val screenType =
        if (UserDefaultsManager.getGroupFundManagerLogged())
            GroupFundUserType.GROUP_FUND_MANAGER
        else
            GroupFundUserType.GROUP_FUND_MEMBER

    // ---------- Load initial values (SwiftUI .onAppear) ----------
    LaunchedEffect(groupFund, currentMember, screenType) {
        upiState.value = if (screenType == GroupFundUserType.GROUP_FUND_MEMBER) {
            currentMember?.upiID ?: ""
        }
        else {
            groupFund?.upiID ?: ""
        }

        accountHoldernameState.value = if (screenType == GroupFundUserType.GROUP_FUND_MEMBER) {
            currentMember?.name ?: ""
        }
        else {
            groupFund?.groupFundAccountName ?: ""
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AppBackgroundGradient()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.Top
        ) {

            SSNavigationBar(
                title = if (screenType == GroupFundUserType.GROUP_FUND_MANAGER)
                    "Group Fund UPI"
                else
                    "Your UPI",
                navController = navController,
                showBackButton = true
            )

            Spacer(Modifier.height(20.dp))

            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                SSTextField(
                    icon = Icons.Default.Person,              // SYSTEM ICON
                    placeholder = "UPI Holder Name",
                    textState = accountHoldernameState,
                    keyboardType = KeyboardType.Text,
                    error = accountHoldernameError,
                )

                Spacer(Modifier.height(12.dp))

                SSTextField(
                    icon = Icons.Default.QrCode2,
                    placeholder = "UPI",
                    textState = upiState,
                    keyboardType = KeyboardType.Text,
                    error = upiIDError,
                )

                Spacer(Modifier.height(20.dp))

                // ------------ Update Button ------------
                SSButton(
                    title = "Update UPI",
                    action = {
                        val upiID = upiState.value.trim()
                        val accountHoldername = accountHoldernameState.value.trim()

                        if (validateFields(
                                accountHoldername = accountHoldername,
                                upiID = upiID,
                                onErrorName = { accountHoldernameError = it },
                                onErrorUPI = { upiIDError = it }
                            )
                        ) {
                            saveAccountToFirestore(
                                screenType = screenType,
                                squadViewModel = squadViewModel,
                                loaderManager = loaderManager,
                                accountHoldername = accountHoldername,
                                upiID = upiID
                            )
                        }
                    }
                )

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

private fun validateFields(
    accountHoldername: String,
    upiID: String,
    onErrorName: (String) -> Unit,
    onErrorUPI: (String) -> Unit
): Boolean {

    Log.d("BankDetails", "🔍 Starting validation...")
    Log.d("BankDetails", "Input Name: '$accountHoldername'")
    Log.d("BankDetails", "Input UPI: '$upiID'")

    var isValid = true

    // Validate Name
    val nameTrimmed = accountHoldername.trim()
    if (nameTrimmed.isEmpty()) {
        Log.d("BankDetails", "❌ Name validation failed: empty")
        onErrorName("Account Holder Name is required")
        isValid = false
    } else {
        Log.d("BankDetails", "✅ Name validation passed")
        onErrorName("")
    }

    // Validate UPI
    val upiTrimmed = upiID.trim()
    val upiRegex = Regex("^[\\w.-]+@[\\w.-]+$")

    if (upiTrimmed.isNotEmpty() && !upiRegex.matches(upiTrimmed)) {
        Log.d("BankDetails", "❌ UPI validation failed: wrong format → '$upiTrimmed'")
        onErrorUPI("Invalid UPI ID")
        isValid = false
    } else {
        Log.d("BankDetails", "✅ UPI validation passed")
        onErrorUPI("")
    }

    Log.d("BankDetails", "🔚 Validation Completed → isValid = $isValid")

    return isValid
}

private fun saveAccountToFirestore(
    screenType: GroupFundUserType?,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager,
    accountHoldername: String,
    upiID: String
) {
    val groupFundID = squadViewModel.groupFund.value?.groupFundID ?: return

    fun handleResult(result: Result<BeneficiaryResult>) {
        loaderManager.hideLoader()

        if (result.isSuccess) {
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = "UPI Updated",
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
        } else {
            val error = result.exceptionOrNull()?.localizedMessage ?: "Error"
            AlertManager.shared.showAlert(
                title = SquadStrings.appName,
                message = error,
                primaryButtonTitle = "OK",
                primaryAction = {}
            )
        }
    }

    when (screenType) {

        GroupFundUserType.GROUP_FUND_MANAGER -> {
            loaderManager.showLoader()

            FirebaseFunctionsManager.shared.verifyAndSaveUPIBeneficiary(
                groupFundId = groupFundID,
                memberId = "",
                name = accountHoldername,
                vpa = upiID,
                email = "",
                phone = "",
                address = "",
                city = "",
                countryCode = "+91",
                postalCode = ""
            ) { result ->
                handleResult(result)
            }
        }

        else -> {
            val member = squadViewModel.currentMember.value ?: return

            loaderManager.showLoader()

            FirebaseFunctionsManager.shared.verifyAndSaveUPIBeneficiary(
                groupFundId = groupFundID,
                memberId = member.id!!,
                name = member.name,
                vpa = upiID,
                email = "",
                phone = member.phoneNumber,
                address = "",
                city = "",
                countryCode = "+91",
                postalCode = ""
            ) { result ->
                handleResult(result)
            }
        }
    }
}