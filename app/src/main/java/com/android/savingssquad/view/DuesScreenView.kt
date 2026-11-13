package com.android.savingssquad.view

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.android.savingssquad.model.Member
import com.android.savingssquad.model.PaymentsDetails
import com.android.savingssquad.model.PushNotificationRequest
import com.android.savingssquad.model.NotificationRecipientType
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.LoaderManager
import com.android.savingssquad.singleton.NotificationService
import com.android.savingssquad.singleton.PaymentApproveStatus
import com.android.savingssquad.singleton.PaymentStatus
import com.android.savingssquad.singleton.PaymentSubType
import com.android.savingssquad.singleton.PaymentType
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.viewmodel.ToastManager
import com.android.savingssquad.viewmodel.ToastType
import com.yourapp.utils.CommonFunctions
import java.time.LocalDate
import java.time.ZoneId

@SuppressLint("StateFlowValueCalledInComposition")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DuesScreenView(navController: NavController, squadViewModel: SquadViewModel) {
    val screenType = if (UserDefaultsManager.getSquadManagerLogged()) {
        SquadUserType.SQUAD_MANAGER
    } else {
        SquadUserType.SQUAD_MEMBER
    }

    val contributionMembers by squadViewModel.unpaidContributionMembers.collectAsStateWithLifecycle()
    val loanMembers by squadViewModel.unpaidLoanMembers.collectAsStateWithLifecycle()
    val paymentsState by squadViewModel.squadPayments.collectAsStateWithLifecycle()
    val currentMember by squadViewModel.currentMember.collectAsStateWithLifecycle()

    var payments by remember { mutableStateOf(emptyList<PaymentsDetails>()) }
    var selectedSegment by remember { mutableStateOf(SquadStrings.contribution) }
    var isLoadingDues by remember { mutableStateOf(true) }
    val members = if (selectedSegment == SquadStrings.contribution) contributionMembers else loanMembers

    LaunchedEffect(Unit) {
        squadViewModel.fetchPayments(showLoader = true) { _, _ ->
            val currentMonth = getCurrentMonthPayments(squadViewModel.squadPayments.value)
                .filter { it.paymentStatus == PaymentStatus.SUCCESS }
                .filter { it.paymentApproveStatus == PaymentApproveStatus.ACCEPTED }
            payments = if (screenType == SquadUserType.SQUAD_MEMBER) {
                currentMonth.filter { it.memberId == currentMember?.id }
            } else currentMonth
        }

        if (screenType != SquadUserType.SQUAD_MANAGER) {
            isLoadingDues = false
        }
    }

    LaunchedEffect(selectedSegment) {
        if (screenType == SquadUserType.SQUAD_MANAGER) {
            isLoadingDues = true
            loadDuesPage(squadViewModel, selectedSegment) { isLoadingDues = false }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(vertical = 16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AppBackgroundGradient()
            Column(modifier = Modifier.fillMaxSize()) {
                SSNavigationBar(
                    title = SquadStrings.currentOverDues,
                    navController = navController,
                    showBackButton = true
                )
                Spacer(Modifier.height(12.dp))

                if (screenType != SquadUserType.SQUAD_MANAGER) {
                    AllCaughUPView(
                        title = "All Due’s Paid",
                        subtitle = SquadStrings.youAreAllCaughtUp,
                        icon = Icons.Default.CheckCircle,
                        iconColor = Color(0xFF4CAF50),
                        showChevron = false
                    )
                    RecentPayments(payments)
                } else {
                    ModernSegmentedPickerView(
                        segments = listOf(SquadStrings.contribution, SquadStrings.emi),
                        selectedSegment = selectedSegment,
                        onSegmentSelected = { selectedSegment = it }
                    )
                    Spacer(Modifier.height(8.dp))

                    when {
                        isLoadingDues && members.isEmpty() -> {
                            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                ShimmerLoader()
                            }
                        }
                        members.isEmpty() -> {
                            AllCaughUPView(
                                title = if (selectedSegment == SquadStrings.contribution) "All Contributions Paid" else "All EMI’s Paid",
                                subtitle = SquadStrings.squadAllCaughtUp,
                                icon = Icons.Default.CheckCircle,
                                iconColor = Color(0xFF4CAF50),
                                showChevron = false
                            )
                            RecentPayments(payments)
                        }
                        else -> {
                            SectionView(title = "$selectedSegment ${SquadStrings.dues}") {
                                Column {
                                    // Bulk reminder behavior is intentionally deferred.
                                    RemindAllButton(members.size) {
                                        // Bulk reminders are not enabled yet.
                                        sendMemberReminder(
                                            squadViewModel,
                                            members = members,
                                            contribution = selectedSegment == SquadStrings.contribution
                                        )
                                    }
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(16.dp)
                                    ) {
                                        items(members, key = { it.id ?: it.hashCode().toString() }) { member ->
                                            MemberDueRow(member = member) {
                                                sendMemberReminder(
                                                    squadViewModel,
                                                    members = listOf(member),
                                                    contribution = selectedSegment == SquadStrings.contribution
                                                )
                                            }
                                            LaunchedEffect(member.id, selectedSegment) {
                                                if (selectedSegment == SquadStrings.contribution) {
                                                    squadViewModel.loadMoreUnpaidContributionMembersIfNeeded(member)
                                                } else {
                                                    squadViewModel.loadMoreUnpaidLoanMembersIfNeeded(member)
                                                }
                                            }
                                        }
                                        val loadingMore = if (selectedSegment == SquadStrings.contribution) {
                                            squadViewModel.unpaidContributionMembersIsLoadingMore
                                        } else squadViewModel.unpaidLoanMembersIsLoadingMore
                                        if (loadingMore) item { ShimmerLoader() }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentPayments(payments: List<PaymentsDetails>) {
    if (payments.isNotEmpty()) {
        SectionView(title = SquadStrings.recentPayments) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 10.dp)
            ) {
                items(payments, key = { it.id ?: it.hashCode().toString() }) { payment ->
                    PaymentRow(payment = payment)
                }
            }
        }
    }
}

@Composable
private fun MemberDueRow(member: Member, onRemind: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .background(AppColors.surface, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            tint = AppColors.primaryButton,
            modifier = Modifier.size(30.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(member.localizedMemberName, style = AppFont.ibmPlexSans(16, FontWeight.SemiBold), color = AppColors.headerText)
            Text(SquadStrings.pending, style = AppFont.ibmPlexSans(12, FontWeight.Normal), color = Color.Red)
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(AppColors.primaryButton)
                .clickable(onClick = onRemind)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(SquadStrings.remind, style = AppFont.ibmPlexSans(11, FontWeight.SemiBold), color = Color.White)
        }
    }
}

private fun loadDuesPage(viewModel: SquadViewModel, segment: String, onFinished: () -> Unit) {
    if (segment == SquadStrings.contribution) {
        viewModel.resetUnpaidContributionMembersPagination()
        viewModel.fetchUnpaidContributionMembers(showLoader = true) { _, _ -> onFinished() }
    } else {
        viewModel.resetUnpaidLoanMembersPagination()
        viewModel.fetchUnpaidLoanMembers(showLoader = true) { _, _ -> onFinished() }
    }
}

private fun sendMemberReminder(
    squadViewModel: SquadViewModel,
    members: List<Member>,
    contribution: Boolean
) {

    val memberIds = members
        .mapNotNull { member ->
            member.id?.takeIf { it.isNotBlank() }
        }
        .distinct()

    if (memberIds.isEmpty()) return

    val title = if (contribution) {
        "Contribution Reminder"
    } else {
        "EMI Reminder"
    }

    val notificationType = if (contribution) {
        "CONTRIBUTION_REMINDER"
    } else {
        "EMI_REMINDER"
    }

    val message = if (contribution) {
        "Please complete your pending contribution."
    } else {
        "Please complete your pending EMI."
    }

    LoaderManager.shared.showLoader()

    NotificationService.shared.sendMemberReminder(
        request = PushNotificationRequest(
            squadId = squadViewModel.squad.value?.squadID ?: "",
            memberIds = memberIds,
            title = title,
            message = message,
            data = mapOf(
                "navigate" to "PAYMENT",
                "notificationType" to notificationType
            ),
            recipientType = NotificationRecipientType.SELECTED
        ),
        onSuccess = {
            LoaderManager.shared.hideLoader()

            ToastManager.show(
                title = SquadStrings.reminderSent,
                message = if (memberIds.size == 1) {
                    SquadStrings.notificationSentToMember(
                        members.first { it.id == memberIds.first() }.localizedMemberName
                    )
                } else {
                    "Reminder sent to ${memberIds.size} members."
                },
                type = ToastType.SUCCESS
            )
        },
        onError = { error ->
            LoaderManager.shared.hideLoader()

            ToastManager.show(
                title = "Failed",
                message = error.localizedMessage
                    ?: "Unable to send reminder.",
                type = ToastType.ERROR
            )
        }
    )
}

@RequiresApi(Build.VERSION_CODES.O)
fun getCurrentMonthPayments(allPayments: List<PaymentsDetails>): List<PaymentsDetails> {
    val now = LocalDate.now()
    val startOfMonth = now.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val startOfNextMonth = now.plusMonths(1).withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val allowedSubTypes = setOf(PaymentSubType.INTEREST_AMOUNT, PaymentSubType.EMI_AMOUNT, PaymentSubType.CONTRIBUTION_AMOUNT)
    return allPayments.filter { payment ->
        val recordMs = payment.recordDate.toDate().time
        payment.paymentType == PaymentType.PAYMENT_CREDIT && payment.paymentSubType in allowedSubTypes && recordMs in startOfMonth until startOfNextMonth
    }
}

