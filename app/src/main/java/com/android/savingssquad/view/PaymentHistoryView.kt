package com.android.savingssquad.view

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.android.savingssquad.model.Member
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.SquadStrings
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

    var selectedUser by remember { mutableStateOf(SquadStrings.all) }
    var selectedMemberId by remember { mutableStateOf<String?>(null) }

    var selectedMember by remember { mutableStateOf<Member?>(null) }

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

                SSTextField(
                    icon = Icons.Default.Person,
                    placeholder = selectedMember?.localizedMemberName ?: SquadStrings.selectSquadMember,
                    textState = remember { mutableStateOf(selectedMember?.localizedMemberName ?: "") },
                    keyboardType = KeyboardType.Text,
                    showDropdown = true,
                    error = "",
                    onDropdownTap = { squadViewModel.setShowMembersListPopup(true) },
                    disabled = true
                )
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
                            SquadStrings.noPaymentsYet
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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 10.dp)
                )
                {

                    items(
                        items = payments.value,
                        key = { it.id ?: "" }
                    )
                    { payment ->

                        PaymentRow(
                            payment = payment
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


        val isShowMemberList = squadViewModel.showMembersListPopup.collectAsStateWithLifecycle()
        if (isShowMemberList.value) {
            OverlayBackgroundView(
                showPopup = isShowMemberList,
                onDismiss = { squadViewModel.setShowMembersListPopup(false) }
            ) {

                MemberNameSelectionPopup(

                    showPopup = true,

                    squadId = squadViewModel.squad.value?.squadID ?: "",

                    squadViewModel = squadViewModel,

                    onDismiss = {

                        squadViewModel.setShowMembersListPopup(false)

                    },
                    showSelectAll = true,
                    onSelectAll = {

                        selectedMemberId = null
                        selectedUser = SquadStrings.all
                        selectedMember = null

                        squadViewModel.resetPaymentsPagination()

                        squadViewModel.fetchPayments(
                            showLoader = true,
                            memberId = selectedMemberId
                        ) { _, error ->
                            if (error != null) {
                                println("❌ $error")
                            }
                        }

                    },
                    onSelect = { member ->

                        selectedMemberId = member.id
                        selectedUser = member.localizedMemberName
                        selectedMember = member

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

                )
            }
        }

        }
}

