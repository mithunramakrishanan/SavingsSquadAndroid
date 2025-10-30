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
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.count

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MemberHomeView(
    memberSelectedTab: Int,
    squadViewModel: SquadViewModel = SquadViewModel(),
    loaderManager: LoaderManager = LoaderManager.shared,
    navController: NavController = rememberNavController()
) {
    val context = LocalContext.current
    val groupFund by squadViewModel.groupFund.collectAsState()
    val showPopup by squadViewModel.showPopup.collectAsState()
    val currentMember by squadViewModel.currentMember.collectAsState(null)

    // Navigation-like flags
    var openPaymentHistoryView by remember { mutableStateOf(false) }
    var openNotificationView by remember { mutableStateOf(false) }
    var openContributionDetails by remember { mutableStateOf(false) }
    var openLoanDetailsView by remember { mutableStateOf(false) }
    var openGroupFundRulesView by remember { mutableStateOf(false) }
    var openProfile by remember { mutableStateOf(false) }
    var openDuesScreen by remember { mutableStateOf(false) }
    var openVerifyPayment by remember { mutableStateOf(false) }
    var openAccountSummary by remember { mutableStateOf(false) }
    var openMembersList by remember { mutableStateOf(false) }

    var remainders by remember { mutableStateOf(listOf<RemainderModel>()) }
    var currentOrOverDueContribution by remember { mutableStateOf(listOf<ContributionDetail>()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {
        if (groupFund != null) {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 🔹 Navigation Bar
                SSNavigationBar(
                    title = "Vault Captain",
                    navController = navController,
                    showBackButton = false,
                    rightButtonIcon = Icons.Default.SwipeRight
                ) {
                    squadViewModel.fetchUserLogins(
                        showLoader = true,
                        phoneNumber = squadViewModel.loginMember?.phoneNumber ?: ""
                    ) { success, error ->
                        Log.d("MemberHomeView", if (success) "✅ User logins fetched" else "❌ $error")
                    }
                }

                // 🔹 Scrollable Content
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // --- Progress & Dashboard Cards
                    item {
                        val remainingMonths = CommonFunctions.getRemainingMonths(
                            startDate = Date(),
                            endDate = groupFund!!.groupFundEndDate?.toDate() ?: Date()
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ProgressCircleView(
                                completedMonths = groupFund!!.totalDuration - remainingMonths,
                                totalMonths = groupFund!!.totalDuration,
                                monthlyContribution = groupFund!!.monthlyContribution.currencyFormattedWithCommas()
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                MemberDashBoardCard(
                                    title = SquadStrings.totalMembers,
                                    value = squadViewModel.groupFundMembersCount.toString(),
                                    subDetails = emptyList()
                                )

                                MemberDashBoardCard(
                                    title = "Current Available Fund",
                                    value = groupFund!!.currentAvailableAmount.currencyFormattedWithCommas(),
                                    subDetails = listOf("banknote" to "As of Sep 2025")
                                )
                            }
                        }
                    }

                    // --- Header
                    item {
                        MemberHeaderView(
                            groupFund = groupFund!!,
                            squadViewModel = squadViewModel,
                            nameClicked = { openProfile = true },
                            amountClicked = { openContributionDetails = true }
                        )
                    }

                    // --- Reminders / Dues
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
                                            //memberSelectedTab = 1
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

                    // --- Buttons
                    item {
                        MemberTwoButtons(
                            requestCashAction = { /* TODO */ },
                            approveCashAction = { openVerifyPayment = true }
                        )
                    }

                    // --- Recent Transactions
                    item {
                        SectionView(title = "Recent Transactions") {
                            val groupFundPayments by squadViewModel.groupFundPayments.collectAsState(emptyList())

                            val lastFivePayments = groupFundPayments
                                .filter { it.memberName == currentMember?.name }
                                .sortedByDescending { it.paymentUpdatedDate?.toDate() ?: Date(0) }
                                .take(5)

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                lastFivePayments.forEach { payment ->
                                    PaymentRow(
                                        payment = payment,
                                        showPaymentStatusRow = true,
                                        showPayoutStatusRow = false
                                    )
                                }

                                if (groupFundPayments.count { it.memberName == currentMember?.name } > 5) {
                                    ViewAllButton(
                                        title = "View All",
                                        icon = "arrow.right"
                                    ) {
                                        openPaymentHistoryView = true
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 🔹 Floating Menu
            FloatingGroupFundButton(
                onGroupFundActivity = { openNotificationView = true },
                onPaymentHistory = { openPaymentHistoryView = true },
                onGroupFundRules = { openGroupFundRulesView = true }
            )
        }

        // 🔹 Popup

        if (squadViewModel.showPopup.collectAsState().value) {
            OverlayBackgroundView()
            LoginListPopup(
                showPopup = remember { mutableStateOf(squadViewModel.showPopup.value) },
                selectedUser = remember { mutableStateOf(squadViewModel.selectedUser.value) },
                users = squadViewModel.users.collectAsState().value,
                squadViewModel = squadViewModel
            )
        }

        // 🔹 Loader
        if (loaderManager.isLoading) {
            SSLoaderView(true)
        }

        SSAlert()
    }

    // 🔹 On Load
    LaunchedEffect(Unit) {
        val member = UserDefaultsManager.getLogin() ?: return@LaunchedEffect
        loaderManager.showLoader()

        squadViewModel.fetchMember(true, member.groupFundID, member.groupFundUserId) { success, fetchedMember, error ->
            if (success && fetchedMember != null) {
                remainders = emptyList()

                squadViewModel.fetchContributionsForMember(true, fetchedMember.groupFundID, fetchedMember.id ?: "") { contributions, _ ->
                    contributions?.let {
                        val currentContributions = it.currentAndOverdueUnpaid()
                        currentOrOverDueContribution = currentContributions

                        val mapped = currentContributions.map { contrib ->
                            RemainderModel(
                                remainderTitle = "CONTRIBUTION",
                                remainderSubTitle = contrib.monthYear,
                                remainderType = RemainderType.CONTRIBUTION,
                                remainderAmount = contrib.amount,
                                remainderID = contrib.id ?: "",
                                remainderDueDate = contrib.dueDate ?: Timestamp.now()
                            )
                        }
                        remainders = mapped
                    }
                }

                squadViewModel.fetchMemberLoans(true, fetchedMember.id ?: "") { _, _ ->
                    val loanRemainders = squadViewModel.memberPendingLoans.value
                        ?.firstOrNull()
                        ?.installments
                        ?.currentAndOverdueUnpaid()
                        ?.map {
                            RemainderModel(
                                remainderTitle = "EMI",
                                remainderSubTitle = it.installmentNumber,
                                remainderType = RemainderType.EMI,
                                remainderAmount = it.installmentAmount + it.interestAmount,
                                remainderID = it.id ?: "",
                                remainderDueDate = it.dueDate ?: Timestamp.now()
                            )
                        } ?: emptyList()

                    remainders = (remainders + loanRemainders).sortedBy { it.remainderDueDate.orNow }
                    loaderManager.hideLoader()
                }
            } else loaderManager.hideLoader()
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
            action = requestCashAction
        )

        TwoButtonGradient(
            icon = Icons.Default.SwipeRight,
            title = "Approve Payment",
            gradientColors = listOf(AppColors.secondaryAccent, AppColors.warningAccent),
            action = approveCashAction
        )
    }
}

@Composable
fun TwoButtonGradient(
    icon: ImageVector,
    title: String,
    gradientColors: List<Color>,
    action: () -> Unit
) {
    Button(
        onClick = action,
        modifier = Modifier
            .height(56.dp)
            .appShadow(AppShadows.card),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(colors = gradientColors),
                    shape = RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = AppFont.ibmPlexSans(16, FontWeight.SemiBold),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun MemberDashBoardCard(
    title: String,
    value: String,
    subDetails: List<Pair<String, String>>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.surface, RoundedCornerShape(16.dp))
            .appShadow(AppShadows.card)
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = AppFont.ibmPlexSans(14, FontWeight.Normal),
            color = AppColors.secondaryText
        )

        Text(
            text = value,
            style = AppFont.ibmPlexSans(22, FontWeight.SemiBold),
            color = AppColors.headerText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

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