package com.android.savingssquad.view

import android.annotation.SuppressLint
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Verified
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
import com.android.savingssquad.model.GroupFundActivity
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
import com.yourapp.utils.CommonFunctions
import java.util.Calendar
import androidx.compose.foundation.lazy.items
import com.android.savingssquad.singleton.GroupFundActivityType

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun PaymentHistoryView(
    navController: NavController,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager = LoaderManager.shared
) {
    val screenType =
        if (UserDefaultsManager.getGroupFundManagerLogged())
            GroupFundUserType.GROUP_FUND_MANAGER
        else
            GroupFundUserType.GROUP_FUND_MEMBER

    var selectedUser by remember { mutableStateOf("All") }

    val payments = squadViewModel.groupFundPayments.collectAsStateWithLifecycle()

    val groupFundMembers = squadViewModel.groupFundMembers.collectAsStateWithLifecycle()

    // User Dropdown List
    val userList = remember(groupFundMembers.value) {
        listOf("All") + groupFundMembers.value.map { it.name }.distinct()
    }

    // Filter Logic
    val filteredPayments = remember(selectedUser, payments.value) {
        if (selectedUser == "All") payments.value
        else payments.value.filter { it.memberName == selectedUser }
    }

    // Load Data on Appear
    LaunchedEffect(Unit) {

        // First update the user selection
        selectedUser = if (screenType == GroupFundUserType.GROUP_FUND_MEMBER) {
            squadViewModel.currentMember.value?.name ?: "All"
        } else {
            "All"
        }

        // Then fetch payments
        squadViewModel.fetchPayments(showLoader = true) { success, error ->
            if (!success) println("❌ Error: ${error ?: "Unknown error"}")
        }
    }

    AppBackgroundGradient()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        // NAV BAR
        SSNavigationBar("Payment History", navController)

        Spacer(modifier = Modifier.height(16.dp))

        // USER PICKER (only for Managers)
        if (screenType != GroupFundUserType.GROUP_FUND_MEMBER) {
            DropdownMenuPicker(
                label = "Select User",
                selected = selectedUser,
                items = userList,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) { selected ->
                selectedUser = selected
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // EMPTY STATE
        if (filteredPayments.isEmpty()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = Color.Gray.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (screenType == GroupFundUserType.GROUP_FUND_MEMBER)
                        "No payments yet"
                    else
                        "No transactions yet",
                    style = AppFont.ibmPlexSans(15, FontWeight.Medium),
                    color = AppColors.secondaryText
                )
            }

        } else {

            // PAYMENT LIST
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                items(
                    items = filteredPayments,
                    key = { it.id!! }
                ) { payment ->

                    PaymentRow(
                        payment = payment,
                        showPaymentStatusRow = true,
                        showPayoutStatusRow = true,
                        squadViewModel
                    )
                }
            }
        }
    }
}