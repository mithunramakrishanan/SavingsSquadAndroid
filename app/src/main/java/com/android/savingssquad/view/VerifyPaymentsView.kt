package com.android.savingssquad.view

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
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import com.android.savingssquad.model.Squad
import com.android.savingssquad.model.Installment
import com.android.savingssquad.model.MemberLoan
import com.android.savingssquad.model.PaymentsDetails
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.PaymentType
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.singleton.appShadow
import com.android.savingssquad.singleton.asTimestamp
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.android.savingssquad.singleton.displayText
import com.android.savingssquad.viewmodel.AlertManager
import com.yourapp.utils.CommonFunctions
import kotlinx.coroutines.flow.map
import java.util.Calendar

@Composable
fun VerifyPaymentsView(
    navController: NavController,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager = LoaderManager.shared
) {

    val dismiss = { navController.popBackStack() }

    var selectedUser by remember { mutableStateOf("All") }
    var selectedPayment by remember { mutableStateOf<PaymentsDetails?>(null) }
    var showPayoutOptions by remember { mutableStateOf(false) }

    val userList = remember {
        listOf("All") + squadViewModel.squadMembers.value
            .map { it.name }
            .distinct()
    }

    val screenType =
        if (UserDefaultsManager.getSquadManagerLogged())
            SquadUserType.SQUAD_MANAGER
        else
            SquadUserType.SQUAD_MEMBER

    // 🔹 Pending Transfers Logic
    val pendingAccountTransfers = remember(selectedUser, squadViewModel.squadPayments) {
        if (screenType != SquadUserType.SQUAD_MEMBER) {

            val base = squadViewModel.squadPayments.value.filter {
                it.paymentSuccess &&
                        !it.payoutSuccess &&
                        it.paymentType == PaymentType.PAYMENT_CREDIT
            }

            if (selectedUser == "All") base
            else base.filter { it.memberName == selectedUser }

        } else {

            squadViewModel.squadPayments.value.filter {
                it.paymentSuccess &&
                        !it.payoutSuccess &&
                        it.paymentType == PaymentType.PAYMENT_DEBIT &&
                        it.memberId == squadViewModel.currentMember.value!!.id
            }
        }
    }

    // 🔹 Fetch on appear
    LaunchedEffect(true) {
        squadViewModel.fetchPayments(showLoader = true) { _, _ -> }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackgroundGradient()

        Column(modifier = Modifier.fillMaxSize()) {

            // NAV BAR
            SSNavigationBar("Verify Payments", navController)

            Spacer(modifier = Modifier.height(16.dp))

            // FILTER SECTION
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (screenType != SquadUserType.SQUAD_MEMBER) {
                    DropdownMenuPicker(
                        label = "",
                        selected = selectedUser,
                        items = userList,
                        modifier = Modifier.weight(1f)
                    ) { selectedUser = it }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 🔹 Empty State
            if (pendingAccountTransfers.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = AppColors.successAccent.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "No pending account transfers",
                        style = AppFont.ibmPlexSans(15, FontWeight.Medium),
                        color = AppColors.secondaryText,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
            }

            // 🔹 List
            else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(pendingAccountTransfers, key = { it.id!! }) { payment ->

                        PaymentRow(
                            payment = payment,
                            showPaymentStatusRow = true,
                            showPayoutStatusRow = true,
                            squadViewModel = squadViewModel
                        )
                    }
                }
            }
        }

        SSAlert()
        SSLoaderView()
    }


}
