package com.android.savingssquad.view

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.viewmodel.SSToast

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
    Box(
        modifier = Modifier

            .fillMaxSize()

            .windowInsetsPadding(WindowInsets.safeDrawing)
    )
    {
        AppBackgroundGradient()
        Column(
            modifier = Modifier.fillMaxSize()
        )
        {

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
}

