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
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.viewmodel.LoaderManager
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import kotlinx.coroutines.launch
import java.util.Date
import com.android.savingssquad.model.GroupFund
import com.android.savingssquad.model.Installment
import com.android.savingssquad.model.MemberLoan
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.GroupFundUserType
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.singleton.appShadow
import com.android.savingssquad.singleton.asTimestamp
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.android.savingssquad.singleton.displayText
import com.android.savingssquad.viewmodel.AlertManager
import com.android.savingssquad.viewmodel.AppDestination
import com.yourapp.utils.CommonFunctions
import java.util.Calendar

@Composable
fun ManagerSettingsView(
    navController: NavController,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager = LoaderManager.shared
) {

    var navigateToManageGroupFund by remember { mutableStateOf(false) }
    var navigateToManageMember by remember { mutableStateOf(false) }
    var navigateToManualEntry by remember { mutableStateOf(false) }
    var navigateToBankDetails by remember { mutableStateOf(false) }
    var navigateToLoanDetails by remember { mutableStateOf(false) }



    LaunchedEffect(navigateToManageGroupFund) {
        if (navigateToManageGroupFund) {
            navController.navigate(AppDestination.MANAGE_GROUP_FUND.route)
            navigateToManageGroupFund = false
        }
    }

    LaunchedEffect(navigateToManualEntry) {
        if (navigateToManualEntry) {
            navController.navigate(AppDestination.OPEN_MANUAL_ENTRY.route)
            navigateToManualEntry = false
        }
    }

    LaunchedEffect(navigateToBankDetails) {
        if (navigateToBankDetails) {
            navController.navigate(AppDestination.OPEN_BANK_DETAILS.route)
            navigateToBankDetails = false
        }
    }

    LaunchedEffect(navigateToLoanDetails) {
        if (navigateToLoanDetails) {
            navController.navigate(AppDestination.OPEN_LOAD_DETAILS.route)
            navigateToLoanDetails = false
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 📌 Title
            SSNavigationBar(
                title = SquadStrings.manageAccount,
                navController = navController,
                showBackButton = false
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 🚀 Quick Action Buttons
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {

                ActionButton(
                    title = SquadStrings.manageGroupFund,
                    caption = "Edit groupFund details and update settings"
                ) {
                    navigateToManageGroupFund = true
                }

                ActionButton(
                    title = SquadStrings.manageLoanDetails,
                    caption = "Edit loan details and update settings"
                ) {
                    navigateToLoanDetails = true
                }

                ActionButton(
                    title = SquadStrings.manualEntry,
                    caption = "Record cash transactions manually for accurate tracking"
                ) {
                    navigateToManualEntry = true
                }

                ActionButton(
                    title = SquadStrings.manageBankDetails,
                    caption = "Update UPI for seamless transactions"
                ) {
                    navigateToBankDetails = true
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // LOGOUT BUTTON
            LogoutButton {
                AlertManager.shared.showAlert(
                    title = SquadStrings.appName,
                    message = "Are you sure you want to logout?",
                    primaryButtonTitle = "LOGOUT",
                    primaryAction = {
                        logoutUser(navController)
                    },
                    secondaryButtonTitle = "NO"
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun LogoutButton(action: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.surface)
            .appShadow(AppShadows.elevated)
            .clickable { action() }
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.ExitToApp,
            contentDescription = null,
            tint = AppColors.errorAccent
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Logout",
            color = AppColors.errorAccent,
            style = AppFont.ibmPlexSans(14, FontWeight.SemiBold)
        )
    }
}

private fun logoutUser(navController: NavController) {
    UserDefaultsManager.clearAll()

    navController.navigate(AppDestination.SIGN_IN.route) {
        // Remove the entire back stack
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}