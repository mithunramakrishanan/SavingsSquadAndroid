package com.android.savingssquad.view

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.MemberPaymentSubType
import com.android.savingssquad.singleton.PaidStatus
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.viewmodel.SquadViewModel

// MARK: - Enums (mirrors iOS)

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun MemberOtherPaymentsView(
    navController: NavController,
    squadViewModel: SquadViewModel
) {


    val activity = LocalContext.current as Activity
    val appContext = LocalContext.current.applicationContext

    val screenType =
        if (UserDefaultsManager.getSquadManagerLogged())
            SquadUserType.SQUAD_MANAGER
        else
            SquadUserType.SQUAD_MEMBER

    val payments = squadViewModel.memberOtherPayments.collectAsStateWithLifecycle()
    val members = squadViewModel.squadMembers.collectAsStateWithLifecycle()
    val currentMember = squadViewModel.currentMember.collectAsStateWithLifecycle()

    var selectedSubType by remember { mutableStateOf(MemberPaymentSubType.RE_PAYMENT.value) }
    var selectedPaidStatus by remember { mutableStateOf<String>(SquadStrings.all) }
    var selectedUser by remember { mutableStateOf(SquadStrings.all) }
    var selectedMemberId by remember { mutableStateOf<String?>(null) }
    var hasLoaded by remember { mutableStateOf(false) }

    val userList = remember(members.value) {
        listOf(SquadStrings.all) + members.value.map { it.name }.distinct()
    }

    // Member the request should be scoped to.
    // Squad member -> always themselves. Manager -> whichever user is picked, null = everyone.
    val effectiveMemberId: String? =
        if (screenType == SquadUserType.SQUAD_MEMBER)
            currentMember.value?.id
        else
            selectedMemberId

    fun reloadCashRequests(showLoader: Boolean = true) {

        squadViewModel.resetOtherPaymentsPagination()

        val paidStatus = when (selectedPaidStatus) {
            PaidStatus.PAID.value -> PaidStatus.PAID
            PaidStatus.NOT_PAID.value -> PaidStatus.NOT_PAID
            else -> PaidStatus.INVERIFICATION
        }

        if (selectedPaidStatus == SquadStrings.all) {

            squadViewModel.fetchMemberOtherPayments(
                showLoader = showLoader,
                memberID = effectiveMemberId,
                paidStatus = null,
                type = if (selectedSubType == MemberPaymentSubType.RE_PAYMENT.value) MemberPaymentSubType.RE_PAYMENT else MemberPaymentSubType.SETTLEMENT
            ) { success, error ->
                if (success) {
                    println("✅ Payments fetched successfully")
                } else {
                    println("❌ Error: ${error ?: "Unknown error"}")
                }
            }
        }
        else {

            squadViewModel.fetchMemberOtherPayments(
                showLoader = showLoader,
                memberID = effectiveMemberId,
                paidStatus = if (selectedSubType == MemberPaymentSubType.RE_PAYMENT.value) paidStatus else null,
                type = if (selectedSubType == MemberPaymentSubType.RE_PAYMENT.value) MemberPaymentSubType.RE_PAYMENT else MemberPaymentSubType.SETTLEMENT
            ) { success, error ->
                if (success) {
                    println("✅ Payments fetched successfully")
                } else {
                    println("❌ Error: ${error ?: "Unknown error"}")
                }
            }
        }


    }

    LaunchedEffect(Unit) {

        if (hasLoaded) return@LaunchedEffect
        hasLoaded = true

        if (screenType == SquadUserType.SQUAD_MEMBER) {
            selectedMemberId = currentMember.value?.id
            selectedUser = currentMember.value?.name ?: ""
        } else {
            selectedUser = SquadStrings.all
            selectedMemberId = null
        }

        reloadCashRequests()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {

        AppBackgroundGradient()

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            // MARK: - Nav Bar

            SSNavigationBar(
                title = "Other Payments",
                navController = navController
            )

            Spacer(modifier = Modifier.height(12.dp))

            // MARK: - Segment: Repayment / Settlement

            ModernSegmentedPickerView(
                segments = listOf(
                    MemberPaymentSubType.RE_PAYMENT.value,
                    MemberPaymentSubType.SETTLEMENT.value
                ),
                selectedSegment = selectedSubType
            )
            { newSegment ->

                selectedSubType =
                    if (newSegment == MemberPaymentSubType.RE_PAYMENT.value)
                        MemberPaymentSubType.RE_PAYMENT.value
                    else
                        MemberPaymentSubType.SETTLEMENT.value

//                if (newSegment != MemberPaymentSubType.RE_PAYMENT.value) {
//                    selectedPaidStatus = ""
//                }

                reloadCashRequests()
            }

            Spacer(modifier = Modifier.height(8.dp))

            // MARK: - Paid status filter (Repayment only)

            if (selectedSubType == MemberPaymentSubType.RE_PAYMENT.value) {

                DropdownMenuPicker(
                    selected = selectedPaidStatus,
                    items = listOf(SquadStrings.all) + listOf(
                        PaidStatus.PAID.value,
                        PaidStatus.NOT_PAID.value,
                        PaidStatus.INVERIFICATION.value
                    ),
                    icon = Icons.Default.Payment,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                ) { selected ->

                    selectedPaidStatus = when (selected) {
                        PaidStatus.PAID.value -> PaidStatus.PAID.value
                        PaidStatus.NOT_PAID.value -> PaidStatus.NOT_PAID.value
                        PaidStatus.INVERIFICATION.value -> PaidStatus.INVERIFICATION.value
                        else -> SquadStrings.all
                    }

                    reloadCashRequests()
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // MARK: - User Filter (manager only)

            if (screenType != SquadUserType.SQUAD_MEMBER) {

                DropdownMenuPicker(
                    selected = selectedUser,
                    items = userList,
                    icon = Icons.Default.People,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                ) { selected ->

                    selectedUser = selected

                    selectedMemberId =
                        if (selected == SquadStrings.all) null
                        else members.value.firstOrNull { it.name == selected }?.id

                    reloadCashRequests()
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // MARK: - Empty State

            if (payments.value.isEmpty() && !squadViewModel.otherPaymentsIsLoadingMore) {

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = Color.Gray.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (selectedSubType == MemberPaymentSubType.RE_PAYMENT.value)
                            "No repayments yet"
                        else
                            "No settlements yet",
                        style = AppFont.ibmPlexSans(15, FontWeight.Medium),
                        color = AppColors.secondaryText
                    )
                }
            }

            // MARK: - List

            else {

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 10.dp)
                ) {

                    items(
                        items = payments.value,
                        key = { it.id ?: "" }
                    ) { payment ->

                        MemberOtherPaymentRow(payment = payment, onPay = {

                                val member = currentMember.value

                                if (member != null) {

                                    squadViewModel.makeMemberRepay(
                                        member = member,
                                        payment = payment,
                                        activity = activity,
                                        context = appContext
                                    ) { success, error ->

                                        if (success) {
                                            reloadCashRequests(showLoader = false)
                                        } else {
                                            println("❌ Repay error: ${error ?: "Unknown error"}")
                                        }
                                    }
                                }
                            }
                        )

                        // MARK: - Pagination trigger

                        LaunchedEffect(payment.id) {




                            val paidStatus = when (selectedPaidStatus) {
                                PaidStatus.PAID.value -> PaidStatus.PAID
                                PaidStatus.NOT_PAID.value -> PaidStatus.NOT_PAID
                                else -> PaidStatus.INVERIFICATION
                            }

                            if (selectedPaidStatus == SquadStrings.all) {

                                squadViewModel.loadMoreOtherPaymentsIfNeeded(
                                    currentPayment = payment,
                                    filterType = if (selectedSubType == MemberPaymentSubType.RE_PAYMENT.value) MemberPaymentSubType.RE_PAYMENT else MemberPaymentSubType.SETTLEMENT,
                                    paidStatus = null,
                                    memberId = effectiveMemberId ?: ""
                                )
                            }
                            else {

                                squadViewModel.loadMoreOtherPaymentsIfNeeded(
                                    currentPayment = payment,
                                    filterType = if (selectedSubType == MemberPaymentSubType.RE_PAYMENT.value) MemberPaymentSubType.RE_PAYMENT else MemberPaymentSubType.SETTLEMENT,
                                    paidStatus = if (selectedSubType == MemberPaymentSubType.RE_PAYMENT.value) paidStatus else null,
                                    memberId = effectiveMemberId ?: ""
                                )
                            }








                        }
                    }

                    // MARK: - Pagination Loader

                    if (squadViewModel.otherPaymentsIsLoadingMore) {

                        item {
                            ShimmerLoader()
                        }
                    }
                }
            }
        }
    }
}