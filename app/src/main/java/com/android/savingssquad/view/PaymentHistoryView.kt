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
import com.android.savingssquad.model.Squad
import com.android.savingssquad.model.SquadActivity
import com.android.savingssquad.model.Installment
import com.android.savingssquad.model.MemberLoan
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.SquadUserType
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
import com.android.savingssquad.singleton.SquadActivityType

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun PaymentHistoryView(
    navController: NavController,
    squadViewModel: SquadViewModel
) {

    val screenType =
        if (UserDefaultsManager.getSquadManagerLogged())
            SquadUserType.SQUAD_MANAGER
        else
            SquadUserType.SQUAD_MEMBER

    val payments = squadViewModel.squadPayments.collectAsStateWithLifecycle()
    val members = squadViewModel.squadMembers.collectAsStateWithLifecycle()

    var selectedUser by remember { mutableStateOf("All") }
    var selectedMemberId by remember { mutableStateOf<String?>(null) }

    // MARK: - User List (iOS style)
    val userList = remember(members.value) {
        listOf("All") + members.value.map { it.name }.distinct()
    }

    // MARK: - INIT LOAD GUARD (iOS equivalent)
    var hasLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {

        if (hasLoaded) return@LaunchedEffect
        hasLoaded = true

        selectedMemberId =
            if (screenType == SquadUserType.SQUAD_MEMBER)
                squadViewModel.currentMember.value?.id
            else null

        squadViewModel.fetchPayments(
            showLoader = true,
            memberId = selectedMemberId
        ) { _, error ->
            if (error != null) {
                println("❌ $error")
            }
        }
    }

    AppBackgroundGradient()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        // NAV BAR (same as iOS SSNavigationBar)
        SSNavigationBar(
            title = "Payment History",
            navController = navController
        )

        Spacer(modifier = Modifier.height(12.dp))

        // MARK: - USER PICKER (Manager only)
        if (screenType != SquadUserType.SQUAD_MEMBER) {

            DropdownMenuPicker(
                label = "",
                selected = selectedUser,
                items = userList,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) { selected ->

                selectedUser = selected

                selectedMemberId =
                    members.value.firstOrNull {
                        it.name == selected
                    }?.id

                squadViewModel.resetPaymentsPagination()

                squadViewModel.fetchPayments(
                    showLoader = true,
                    memberId = selectedMemberId
                ) { _, error ->
                    if (error != null) {
                        println("❌ $error")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // MARK: - EMPTY STATE (iOS style)
        if (payments.value.isEmpty()) {

            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
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
                    text = if (screenType == SquadUserType.SQUAD_MEMBER)
                        "No payments yet"
                    else
                        "No transactions yet",
                    style = AppFont.ibmPlexSans(
                        15,
                        FontWeight.Medium
                    ),
                    color = AppColors.secondaryText
                )
            }
        }

        // MARK: - LIST
        else {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {

                items(
                    items = payments.value,
                    key = { it.id ?: "" }
                ) { payment ->

                    PaymentRow(
                        payment = payment,
                        showPaymentStatusRow = true,
                        showPayoutStatusRow = false,
                        squadViewModel = squadViewModel
                    )

                    // MARK: - Pagination trigger (iOS onAppear equivalent)
                    LaunchedEffect(payment.id) {

                        squadViewModel.loadMorePaymentsIfNeeded(
                            currentPayment = payment,
                            memberId = selectedMemberId
                        )
                    }
                }

                // MARK: - Loader (pagination)
                if (squadViewModel.paymentsIsLoadingMore) {

                    item {
                        ShimmerLoader()
                    }
                }
            }
        }
    }
}

