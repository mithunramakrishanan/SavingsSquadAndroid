package com.android.savingssquad.view

import android.os.Build
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
import androidx.annotation.RequiresApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.savingssquad.R
import com.android.savingssquad.viewmodel.AppDestination

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MemberHomeView(
    selectedTab: Int,
    onChangeTab: (Int) -> Unit,
    navController: NavController,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager = LoaderManager.shared
) {
    // Observe state from ViewModel
    val squad by squadViewModel.squad.collectAsStateWithLifecycle()
    val currentMember by squadViewModel.currentMember.collectAsStateWithLifecycle()
    val squadPayments by squadViewModel.squadPayments.collectAsStateWithLifecycle()
    val users by squadViewModel.users.collectAsStateWithLifecycle()
    val showPopup by squadViewModel.showPopup.collectAsStateWithLifecycle()
    val selectedUser by squadViewModel.selectedUser.collectAsStateWithLifecycle()
    var remainders by remember { mutableStateOf(listOf<RemainderModel>()) }
    var currentOrOverDueContribution by remember { mutableStateOf(listOf<ContributionDetail>()) }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {
        // --------------------------
        // ✅ Main Content
        // --------------------------
        if (squad != null) {
            AppBackgroundGradient()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                // 🔹 Top Navigation Bar
                item {
                    SSNavigationBar(
                        title = "Savings Squad",
                        navController = navController,
                        showBackButton = false,
                        rightButtonDrawable = if (UserDefaultsManager.getIsMultipleAccount())
                            R.drawable.switch_account
                        else null
                    ) {
                        squadViewModel.fetchUserLogins(
                            showLoader = true,
                            phoneNumber = squadViewModel.loginMember?.phoneNumber ?: ""
                        ) { success,loginList, error ->
                            if (loginList != null) {
                                squadViewModel.setShowPopup(UserDefaultsManager.getIsMultipleAccount())
                                Log.d("ManagerHomeView", if (success) "✅ Logins fetched: ${loginList.size}" else "❌ $error")
                            }
                        }
                    }
                }

                // 🔹 Progress Circle + Summary Cards
                item {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val remainingMonths = squadViewModel.remainingMonths.collectAsState().value

                        squad?.let { fund ->
                            val completed = fund.totalDuration - remainingMonths
                            val total = fund.totalDuration
                            val monthlyContribution = fund.monthlyContribution.currencyFormattedWithCommas()

                            ProgressCircleView(
                                completedMonths = completed,
                                totalMonths = total,
                                monthlyContribution = monthlyContribution,
                                onClick = {  }
                            )
                        } ?: CircularProgressIndicator()

                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            MemberDashBoardCard(
                                title = "Total Members",
                                value = squadViewModel.squadMembersCount.collectAsStateWithLifecycle().value.toString(),
                                subDetails = emptyList(),
                                onClick = { navController.navigate(AppDestination.OPEN_MEMBERS_LIST.route) }
                            )

                            MemberDashBoardCard(
                                title = "Current Available Fund",
                                value = squad!!.currentAvailableAmount.currencyFormattedWithCommas(),
                                subDetails = listOf(
                                    "banknote" to "As of ${CommonFunctions.dateToString(Date(), "MMM yyyy")}"
                                ),
                                onClick = { navController.navigate(AppDestination.ACCOUNT_SUMMARY.route) }
                            )
                        }
                    }
                }

                // 🔹 Member Header
                item {
                    MemberHeaderView(
                        selectedUser = squadViewModel.selectedUser.collectAsState().value,
                        currentMember = squadViewModel.currentMember.collectAsState().value,
                        nameClicked = { navController.navigate(AppDestination.OPEN_MEMBER_PROFILE.route) },
                        amountClicked = { navController.navigate(AppDestination.OPEN_CONTRUBUTION_DETAILS.route) }
                    )
                }

                // 🔹 Reminder Section
                if (remainders.isNotEmpty()) {
                    item {
                        SectionView(title = "Reminders") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(remainders) { reminder ->
                                    RemainderCardView(
                                        title = reminder.remainderTitle,
                                        subtitle = reminder.remainderSubTitle,
                                        amount = reminder.remainderAmount.toString(),
                                        dueDate = reminder.remainderDueDate.orNow
                                    ) {
                                        UserDefaultsManager.saveRemainder(reminder)
                                        onChangeTab(1)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            DuesCardView(
                                title = "All Due’s Paid",
                                subtitle = "Squad all caught up!",
                                icon = Icons.Default.CheckCircle,
                                iconColor = Color(0xFF4CAF50),
                                gradientColors = listOf(
                                    Color(0xFF4CAF50).copy(alpha = 0.08f),
                                    Color(0xFF4CAF50).copy(alpha = 0.15f)
                                ),
                                showChevron = false
                            )
                        }
                    }
                }

                // 🔹 Action Buttons (Request / Approve)
                item {
                    MemberTwoButtons(
                        requestCashAction = { /* TODO: handle request cash */ },
                        approveCashAction = { navController.navigate(AppDestination.OPEN_VERIFY_PAYMENTS.route) }
                    )
                }

                // 🔹 Transaction Section
                item {
                    SectionView(title = "Recent Transactions") {
                        val lastFivePayments = squadPayments
                            .filter { it.memberName == currentMember?.name }
                            .sortedByDescending { it.paymentUpdatedDate?.toDate() ?: Date(0) }
                            .take(5)

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            lastFivePayments.forEach { payment ->
                                PaymentRow(
                                    payment = payment,
                                    showPaymentStatusRow = true,
                                    showPayoutStatusRow = false,
                                    squadViewModel = squadViewModel
                                )
                            }

                            if (squadPayments.count { it.memberName == currentMember?.name } > 5) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    ViewAllButton(
                                        title = "View All",
                                        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    ) {
                                        navController.navigate(AppDestination.OPEN_PAYMENT_HISTORY.route)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 🔹 Floating Buttons
            FloatingSquadButton(
                onSquadActivity = { navController.navigate(AppDestination.OPEN_ACTIITY.route) },
                onPaymentHistory = { navController.navigate(AppDestination.OPEN_PAYMENT_HISTORY.route) },
                onSquadRules = { navController.navigate(AppDestination.OPEN_GROUP_RULES.route) }
            )
        }

        // ------------------------------
        // 🔹 Overlay Popup
        // ------------------------------
        if (showPopup) {
            OverlayBackgroundView(
                showPopup = remember { mutableStateOf(showPopup) },
                onDismiss = { squadViewModel.setShowPopup(false) }
            ) {
                LoginListPopup(
                    navController = navController,
                    isVisible = showPopup,
                    onDismiss = { squadViewModel.setShowPopup(false) },
                    selectedUser = selectedUser,
                    onUserSelected = { user -> squadViewModel.setSelectedUser(user) },
                    users = users
                )
            }
        }
    }

    // ------------------------------
    // 🔹 Initial Data Fetch
    // ------------------------------
    LaunchedEffect(Unit) {
        val member = UserDefaultsManager.getLogin() ?: return@LaunchedEffect

        squadViewModel.fetchMember(
            showLoader = false,
            squadID = member.squadID,
            memberID = member.squadUserId
        ) { success, fetchedMember, error ->
            if (success && fetchedMember != null) {
                remainders = emptyList()

                // Fetch Contributions
                squadViewModel.fetchContributionsForMember(
                    showLoader = false,
                    squadID = fetchedMember.squadID,
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

                // Fetch Loans
                squadViewModel.fetchMemberLoans(
                    showLoader = false,
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
fun RemainderCardView(
    title: String,
    subtitle: String,
    amount: String,
    dueDate: Date,
    onTap: () -> Unit
) {
    val isOverdue = dueDate.before(Date())
    val formattedDate =
        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(dueDate)

    // 🔹 Dynamic width logic
    val cardWidth = when (title.uppercase()) {
        "CONTRIBUTION" -> 200.dp
        "EMI" -> 150.dp
        else -> 200.dp   // fallback
    }

    Column(
        modifier = Modifier
            .width(cardWidth)            // 👈 Dynamic width applied here
            .padding(horizontal = 5.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                clip = false
            )
            .background(AppColors.surface, RoundedCornerShape(16.dp))
            .border(1.dp, AppColors.border, RoundedCornerShape(16.dp))
            .clickable { onTap() }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        // 🔹 Title + Badge Row
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
                    .clip(RoundedCornerShape(50))
                    .background(if (isOverdue) AppColors.errorAccent else AppColors.warningAccent)
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

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = AppColors.border.copy(alpha = 0.4f)
        )

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
    selectedUser: Login?,
    currentMember: Member?,
    nameClicked: () -> Unit,
    amountClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp), // ⬅️ Added left & right padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Hi, ${selectedUser?.squadUsername ?: ""}",
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

        Text(
            text = currentMember?.totalContributionPaid
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp) // 🔹 Left–right margin
    ) {
        TwoButtonGradient(
            icon = Icons.Filled.AddCircle,
            title = "Request Cash",
            gradientColors = listOf(AppColors.primaryButton, AppColors.successAccent),
            onClick = requestCashAction,
            modifier = Modifier.weight(1f)
        )

        TwoButtonGradient(
            icon = Icons.Filled.CreditCard,
            title = "Approve Payment",
            gradientColors = listOf(AppColors.secondaryAccent, AppColors.warningAccent),
            onClick = approveCashAction,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TwoButtonGradient(
    icon: ImageVector,
    title: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .height(56.dp)
            .appShadow(AppShadows.card, RoundedCornerShape(18.dp))
            .background(
                brush = Brush.horizontalGradient(gradientColors),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.primaryButtonText,
                modifier = Modifier
                    .size(25.dp)
                    .padding(end = 6.dp)
            )

            Text(
                text = title,
                style = AppFont.ibmPlexSans(13, FontWeight.SemiBold),
                color = AppColors.primaryButtonText,
                maxLines = 1, // ✅ Single line
                overflow = TextOverflow.Ellipsis // ✅ Truncate if text too long
            )
        }
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
            .appShadow(AppShadows.card)
            .background(AppColors.surface, RoundedCornerShape(16.dp))
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
    icon: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.primaryBackground)
            .border(
                width = 1.dp,
                color = AppColors.primaryButton,
                shape = RoundedCornerShape(12.dp)
            )
            .appShadow(AppShadows.card, RoundedCornerShape(12.dp)) // same soft shadow
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
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

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.primaryButton,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}