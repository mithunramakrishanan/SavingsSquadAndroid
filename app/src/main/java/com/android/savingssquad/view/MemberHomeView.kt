package com.android.savingssquad.view

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.savingssquad.model.*
import com.android.savingssquad.singleton.*
import com.yourapp.utils.CommonFunctions
import com.android.savingssquad.viewmodel.LoaderManager
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.model.Installment
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import kotlinx.coroutines.flow.count
import androidx.activity.compose.setContent
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.savingssquad.R


@Composable
fun MembersListView(groupFundID: String) {

}

@Composable
fun AccountSummaryView() {
    TODO("Not yet implemented")
}

@Composable
fun VerifyPaymentsView(screenType: GroupFundUserType) {

}

@Composable
fun DuesScreenView(screenType: GroupFundUserType) {

}

@Composable
fun LoanDetailsView(screenType: Any, memberID: String) {

}

@Composable
fun ContributionDetailsView(member: Member, screenType: GroupFundUserType) {

}

@Composable
fun MemberProfileView(member: Member, screenType: Any) {

}

@Composable
fun GroupFundRulesView(screenType: Any) {

}

@Composable
fun GroupFundActivityView(screenType: GroupFundUserType) {

}

@Composable
fun PaymentHistoryView(screenType: Any) {

}

@Composable
fun MemberHomeView(
    navController: NavController,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager
) {
    val groupFund by squadViewModel.groupFund.collectAsState()
    val currentMember by squadViewModel.currentMember.collectAsState()
    val groupFundPayments by squadViewModel.groupFundPayments.collectAsState()
    val users by squadViewModel.users.collectAsState()

    var remainders by remember { mutableStateOf(listOf<RemainderModel>()) }
    var currentOrOverDueContribution by remember { mutableStateOf(listOf<ContributionDetail>()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {
        if (groupFund != null) {
            AppBackgroundGradient()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // 🔹 Navigation Bar
                item {
                    SSNavigationBar(
                        title = "Vault Captain",
                        navController = navController,
                        showBackButton = false,
                        rightButtonDrawable = R.drawable.switch_account
                    ) {
                        squadViewModel.fetchUserLogins(
                            showLoader = true,
                            phoneNumber = squadViewModel.loginMember?.phoneNumber ?: ""
                        ) { success, error ->
                            Log.d("MemberHomeView", if (success) "✅ User logins fetched" else "❌ $error")
                        }
                    }
                }

                // 🔹 Progress Circle + Cards
                item {
                    val remainingMonths = CommonFunctions.getRemainingMonths(
                        startDate = Date(),
                        endDate = groupFund!!.groupFundEndDate?.toDate() ?: Date()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ProgressCircleView(
                            completedMonths = groupFund!!.totalDuration - remainingMonths,
                            totalMonths = groupFund!!.totalDuration,
                            monthlyContribution = groupFund!!.monthlyContribution.currencyFormattedWithCommas(),
                            onClick = { navController.navigate("account_summary") }
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            MemberDashBoardCard(
                                title = "Total Members",
                                value = squadViewModel.groupFundMembersCount.collectAsState().value.toString(),
                                subDetails = emptyList(),
                                onClick = { navController.navigate("members_list") }
                            )

                            MemberDashBoardCard(
                                title = "Current Available Fund",
                                value = groupFund!!.currentAvailableAmount.currencyFormattedWithCommas(),
                                subDetails = listOf("banknote" to "As of ${CommonFunctions.dateToString(Date(), "MMM yyyy")}"),
                                onClick = { navController.navigate("account_summary") }
                            )
                        }
                    }
                }

                // 🔹 Member Header
                item {
                    MemberHeaderView(
                        groupFund = groupFund!!,
                        squadViewModel = squadViewModel,
                        nameClicked = { navController.navigate("profile") },
                        amountClicked = { navController.navigate("contributions") }
                    )
                }

                // 🔹 Reminders Section
                if (remainders.isNotEmpty()) {
                    item {
                        SectionView(title = "Reminders") {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(remainders) { item ->
                                    RemainderCardView(
                                        title = item.remainderTitle,
                                        subtitle = item.remainderSubTitle,
                                        amount = item.remainderAmount.toString(),
                                        dueDate = item.remainderDueDate.orNow
                                    ) {
                                        UserDefaultsManager.saveRemainder(item)
                                        navController.navigate("dues")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            DuesCardView(
                                title = "All Due's Paid",
                                subtitle = "Group Fund all caught up!",
                                icon = Icons.Default.CheckCircle,
                                iconColor = Color.Green,
                                gradientColors = listOf(
                                    Color.Green.copy(alpha = 0.05f),
                                    Color.Green.copy(alpha = 0.15f)
                                ),
                                showChevron = false
                            )
                        }
                    }
                }

                // 🔹 Buttons
                item {
                    MemberTwoButtons(
                        requestCashAction = { /* TODO */ },
                        approveCashAction = { navController.navigate("verify_payment") }
                    )
                }

                // 🔹 Transactions Section
                item {
                    SectionView(title = "Recent Transactions") {
                        val lastFivePayments = groupFundPayments
                            .filter { it.memberName == currentMember?.name }
                            .sortedByDescending { it.paymentUpdatedDate?.toDate() ?: Date(0) }
                            .take(5)

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            lastFivePayments.forEach { payment ->
                                PaymentRow(payment, showPaymentStatusRow = true, showPayoutStatusRow = false)
                            }

                            if (groupFundPayments.count { it.memberName == currentMember?.name } > 5) {
                                ViewAllButton(
                                    title = "View All",
                                    icon = "arrow.right"
                                ) {
                                    navController.navigate("payment_history")
                                }
                            }
                        }
                    }
                }
            }

            // 🔹 Floating FAB Menu
            FloatingGroupFundButton(
                onGroupFundActivity = { navController.navigate("notifications") },
                onPaymentHistory = { navController.navigate("payment_history") },
                onGroupFundRules = { navController.navigate("rules") }
            )
        }


        val showPopup = squadViewModel.showPopup.collectAsState()
        val selectedUser = squadViewModel.selectedUser.collectAsState()

        if (showPopup.value) {
            OverlayBackgroundView(
                showPopup = showPopup,
                onDismiss = { squadViewModel.setShowPopup(false) } // optional background tap
            ) {
                LoginListPopup(
                    navController = navController,
                    isVisible = showPopup.value,
                    onDismiss = { squadViewModel.setShowPopup(false) },
                    selectedUser = selectedUser.value,
                    onUserSelected = { user -> squadViewModel.setSelectedUser(user) },
                    users = squadViewModel.users.collectAsState().value
                )
            }
        }

    }

    // 🔹 Initial Load
    LaunchedEffect(Unit) {
        val member = UserDefaultsManager.getLogin() ?: return@LaunchedEffect
        loaderManager.showLoader()

        squadViewModel.fetchMember(
            showLoader = true,
            groupFundID = member.groupFundID,
            memberID = member.groupFundUserId
        ) { success, fetchedMember, error ->
            if (success && fetchedMember != null) {
                // ✅ Clear old remainders
                remainders = emptyList()

                // 🔹 Fetch Contributions
                squadViewModel.fetchContributionsForMember(
                    showLoader = true,
                    groupFundID = fetchedMember.groupFundID,
                    memberID = fetchedMember.id ?: ""
                ) { contributions, _ ->
                    contributions?.let { list ->
                        val currentUnpaid = list.currentAndOverdueUnpaid()
                        currentOrOverDueContribution = currentUnpaid

                        val contributionRemainders = currentUnpaid.map { contri ->
                            RemainderModel(
                                remainderTitle = "CONTRIBUTION",
                                remainderSubTitle = contri.monthYear,
                                remainderType = RemainderType.CONTRIBUTION,
                                remainderAmount = contri.amount,
                                remainderID = contri.id ?: "",
                                remainderDueDate = contri.dueDate ?: Timestamp.now()
                            )
                        }
                        remainders = remainders + contributionRemainders
                    } ?: Log.e("MemberHomeView", "❌ Failed to fetch contributions: $error")
                }

                // 🔹 Fetch Loans
                squadViewModel.fetchMemberLoans(
                    showLoader = true,
                    memberID = fetchedMember.id ?: ""
                ) { _, _ ->
                    val pendingUnpaidInstallments =
                        (squadViewModel.memberPendingLoans.value?.firstOrNull()?.installments?.currentAndOverdueUnpaid()
                            ?: emptyList()) +
                                (squadViewModel.memberPendingLoans.value?.firstOrNull()?.installments?.upcomingUnpaid()
                                    ?: emptyList())

                    val loanRemainders = pendingUnpaidInstallments.map { emi ->
                        RemainderModel(
                            remainderTitle = "EMI",
                            remainderSubTitle = emi.installmentNumber,
                            remainderType = RemainderType.EMI,
                            remainderAmount = emi.installmentAmount + emi.interestAmount,
                            remainderID = emi.id ?: "",
                            remainderDueDate = emi.dueDate ?: Timestamp.now()
                        )
                    }

                    // ✅ Merge and sort all remainders
                    remainders = (remainders + loanRemainders)
                        .sortedBy { it.remainderDueDate?.toDate() ?: Date() }

                    loaderManager.hideLoader()
                }

                Log.d("MemberHomeView", "✅ Member fetched: ${fetchedMember.name}")
            } else {
                Log.e("MemberHomeView", "❌ Error: $error")
                loaderManager.hideLoader()
            }
        }
    }
}

@Composable
fun PaymentRow(payment: PaymentsDetails, showPaymentStatusRow: Boolean, showPayoutStatusRow: Boolean) {

}


@Composable
fun RemainderCardView(
    title: String,
    subtitle: String,
    amount: String,
    dueDate: Date,
    onTap: () -> Unit
) {
    val isOverdue = dueDate.before(Date())
    val formattedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(dueDate)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.surface)
            .appShadow(AppShadows.card)
            .border(1.dp, AppColors.border, RoundedCornerShape(16.dp))
            .clickable { onTap() }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 🔹 Title + Badge
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = AppFont.ibmPlexSans(14, FontWeight.SemiBold),
                color = AppColors.headerText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = if (isOverdue) "Overdue" else "Upcoming",
                style = AppFont.ibmPlexSans(10, FontWeight.Bold),
                color = AppColors.primaryButtonText,
                modifier = Modifier
                    .background(
                        color = if (isOverdue) AppColors.errorAccent else AppColors.warningAccent,
                        shape = CircleShape
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        // 🔹 Subtitle
        Text(
            text = subtitle,
            style = AppFont.ibmPlexSans(12, FontWeight.Normal),
            color = AppColors.secondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Divider(modifier = Modifier.padding(vertical = 4.dp))

        // 🔹 Amount
        Text(
            text = "₹$amount",
            style = AppFont.ibmPlexSans(16, FontWeight.Bold),
            color = AppColors.headerText
        )

        // 🔹 Due Date
        Text(
            text = "Due: $formattedDate",
            style = AppFont.ibmPlexSans(11, FontWeight.Medium),
            color = AppColors.secondaryText
        )
    }
}

@Composable
fun MemberHeaderView(
    groupFund: GroupFund,
    squadViewModel: SquadViewModel,
    nameClicked: () -> Unit,
    amountClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 👤 Greeting + Subtitle
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Hi, ${squadViewModel.selectedUser.value?.groupFundUsername ?: ""}",
                style = AppFont.ibmPlexSans(22, FontWeight.SemiBold),
                color = AppColors.headerText,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { nameClicked() }
            )

            Text(
                text = "Your current contribution",
                style = AppFont.ibmPlexSans(18, FontWeight.Normal),
                color = AppColors.secondaryText
            )
        }

        // 💰 Amount
        Text(
            text = squadViewModel.currentMember.value?.totalContributionPaid
                ?.currencyFormattedWithCommas() ?: "₹ 0",
            style = AppFont.ibmPlexSans(26, FontWeight.Bold),
            color = AppColors.headerText,
            modifier = Modifier.clickable { amountClicked() }
        )
    }
}

@Composable
fun MemberTwoButtons(
    requestCashAction: () -> Unit,
    approveCashAction: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        TwoButtonGradient(
            icon = Icons.Default.SwipeRight,
            title = "Request Cash",
            gradientColors = listOf(AppColors.primaryButton, AppColors.successAccent),
            onClick = requestCashAction
        )

        TwoButtonGradient(
            icon = Icons.Default.SwipeRight,
            title = "Approve Payment",
            gradientColors = listOf(AppColors.secondaryAccent, AppColors.warningAccent),
            onClick = approveCashAction
        )
    }
}

@Composable
fun MemberDashBoardCard(
    title: String,
    value: String,
    subDetails: List<Pair<String, String>>,
    onClick: (() -> Unit)? = null // 🔹 Optional click handler
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.surface, RoundedCornerShape(16.dp))
            .appShadow(AppShadows.card)
            // ✅ Safe modern clickable with ripple
            .then(
                if (onClick != null)
                    Modifier
                        .clickable(
                            interactionSource = interactionSource,
                            indication = androidx.compose.material3.ripple()
                        ) { onClick() }
                else Modifier
            )
            .padding(16.dp)
    ) {
        // 🔹 Title
        Text(
            text = title,
            style = AppFont.ibmPlexSans(14, FontWeight.Normal),
            color = AppColors.secondaryText
        )

        // 🔹 Value
        Text(
            text = value,
            style = AppFont.ibmPlexSans(22, FontWeight.SemiBold),
            color = AppColors.headerText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // 🔹 Sub Details
        if (subDetails.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                subDetails.forEach { (icon, text) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppIconView(
                            name = icon,
                            tint = AppColors.secondaryText,
                            size = 12.dp
                        )
                        Text(
                            text = text,
                            style = AppFont.ibmPlexSans(13, FontWeight.Normal),
                            color = AppColors.secondaryText
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ViewAllButton(
    title: String = "View All",
    icon: String = "arrow.right",
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = AppColors.primaryBackground),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f)
            )
            .border(
                width = 1.dp,
                color = AppColors.primaryButton,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = AppFont.ibmPlexSans(13, FontWeight.SemiBold),
                color = AppColors.headerText
            )

            AppIconView(
                name = icon,
                tint = AppColors.primaryButton,
                size = 14.dp
            )
        }
    }
}