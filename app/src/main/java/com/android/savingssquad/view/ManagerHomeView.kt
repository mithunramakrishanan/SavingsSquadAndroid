package com.android.savingssquad.view

import android.app.Activity
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.pointerInput
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.ShadowStyle
import com.android.savingssquad.singleton.appShadow

import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.android.savingssquad.model.Installment
import com.android.savingssquad.model.Login
import com.android.savingssquad.singleton.GroupFundUserType
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.viewmodel.SquadViewModel
import com.yourapp.utils.CommonFunctions
import kotlinx.coroutines.delay
import com.android.savingssquad.viewmodel.LoaderManager
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import java.util.Date
import java.util.concurrent.TimeUnit
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.savingssquad.R
import com.android.savingssquad.model.GroupFund
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.currencyFormattedWithCommas

@Composable
fun ManagerHomeView(
    navController: NavController,
    squadViewModel: SquadViewModel = viewModel(),
    loaderManager: LoaderManager = LoaderManager.shared
) {
    val context = LocalContext.current

    // 🔹 Navigation states
    var openNotificationView by remember { mutableStateOf(false) }
    var openPaymentHistoryView by remember { mutableStateOf(false) }
    var openGroupFundRulesView by remember { mutableStateOf(false) }
    var openDuesScreen by remember { mutableStateOf(false) }
    var openVerifyPayment by remember { mutableStateOf(false) }
    var openManagerGroupFund by remember { mutableStateOf(false) }
    var openMembersList by remember { mutableStateOf(false) }
    var openLoanDetails by remember { mutableStateOf(false) }

    // 🔹 Observing ViewModel state via StateFlow
    val groupFund by squadViewModel.groupFund.collectAsStateWithLifecycle()
    val groupFundMembersCount by squadViewModel.groupFundMembersCount.collectAsStateWithLifecycle()
    val users by squadViewModel.users.collectAsStateWithLifecycle()

    // 🔹 Main Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {
        AppBackgroundGradient()

        if (groupFund != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 10.dp)
            ) {
                // 🔹 Top Navigation Bar
                SSNavigationBar(
                    title = "Savings Squad",
                    navController = navController,
                    showBackButton = false,
                    rightButtonDrawable = R.drawable.switch_account
                ) {
                    squadViewModel.fetchUserLogins(
                        showLoader = true,
                        phoneNumber = squadViewModel.loginMember?.phoneNumber.orEmpty()
                    ) { success, error ->
                        Log.d("ManagerHomeView", if (success) "✅ Logins fetched" else "❌ $error")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 🔹 Scrollable content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 40.dp)
                ) {
                    val gf = groupFund!!

                    item {
                        val remainingMonths = CommonFunctions.getRemainingMonths(
                            startDate = Date(),
                            endDate = gf.groupFundEndDate?.toDate() ?: Date()
                        )
                        ProgressCircleView(
                            completedMonths = gf.totalDuration - remainingMonths,
                            totalMonths = gf.totalDuration,
                            monthlyContribution = gf.monthlyContribution.currencyFormattedWithCommas(),
                            onClick = { openManagerGroupFund = true }
                        )
                    }

                    item {
                        ManagerHeaderView(groupFund = groupFund!!, squadViewModel = squadViewModel)
                    }

                    item {
                        ManagerTwoButtons(
                            addMemberAction = { squadViewModel.setShowAddMemberPopup(true) },
                            acceptAmountAction = { openVerifyPayment = true }
                        )
                    }

                    item {
                        TotalMemberContributionCard(
                            totalMembers = groupFundMembersCount,
                            totalContribution = gf.totalContributionAmountReceived.currencyFormattedWithCommas(),
                            subDetails = listOf(
                                "creditcard" to "As of ${CommonFunctions.dateToString(Date(), "MMM yyyy")}"
                            ),
                            onClick = { openMembersList = true }
                        )
                    }

                    item {
                        LoanSummaryCard(
                            totalSent = gf.totalLoanAmountSent.currencyFormattedWithCommas(),
                            totalReceived = gf.totalLoanAmountReceived.currencyFormattedWithCommas(),
                            pending = (gf.totalLoanAmountSent - gf.totalLoanAmountReceived).currencyFormattedWithCommas(),
                            interestEarned = gf.totalInterestAmountReceived.currencyFormattedWithCommas(),
                            onClick = { openLoanDetails = true }
                        )
                    }
                }
            }
        } else {
            // 🔹 Empty State
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No GroupFund Data Available",
                    style = AppFont.ibmPlexSans(18, FontWeight.Normal),
                    color = AppColors.infoAccent
                )
            }
        }

        // 🔹 Floating Button
        FloatingGroupFundButton(
            onGroupFundActivity = { openNotificationView = true },
            onPaymentHistory = { openPaymentHistoryView = true },
            onGroupFundRules = { openGroupFundRulesView = true }
        )

        // 🔹 Popups
        val showAddMemberPopup = squadViewModel.showAddMemberPopup.collectAsStateWithLifecycle()

        if (showAddMemberPopup.value) {
            OverlayBackgroundView(
                showPopup = showAddMemberPopup,
                onDismiss = { squadViewModel.setShowAddMemberPopup(false) }
            ) {
                AddMemberPopup(
                    squadViewModel = squadViewModel,
                    onDismiss = { squadViewModel.setShowAddMemberPopup(false) }
                )
            }
        }


        val showPopup = squadViewModel.showPopup.collectAsStateWithLifecycle()
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

    // 🔹 Fetch initial data
    LaunchedEffect(Unit) {
        squadViewModel.fetchGroupFundByID(showLoader = true) { success, _, _ ->
            if (success) {
                squadViewModel.fetchEMIConfigurations(showLoader = true) { _, _ ->
                    loaderManager.hideLoader()

                }
            }
        }
    }

    // 🔹 Handle navigation
    if (openNotificationView) {
        navController.navigate("groupFund_activity")
        openNotificationView = false
    }
    if (openPaymentHistoryView) {
        navController.navigate("payment_history")
        openPaymentHistoryView = false
    }
    if (openGroupFundRulesView) {
        navController.navigate("groupFund_rules")
        openGroupFundRulesView = false
    }
    if (openManagerGroupFund) {
        navController.navigate("manage_groupFund")
        openManagerGroupFund = false
    }
    if (openMembersList) {
        val id = groupFund?.groupFundID ?: ""
        navController.navigate("members_list/$id")
        openMembersList = false
    }
    if (openLoanDetails) {
        navController.navigate("loan_details")
        openLoanDetails = false
    }
}

@Composable
fun ManagerHeaderView(
    groupFund: GroupFund,
    squadViewModel: SquadViewModel,
    onAccountSummaryClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                color = AppColors.surface,
                shape = RoundedCornerShape(20.dp)
            )
            .appShadow(AppShadows.card, RoundedCornerShape(20.dp))
            .border(
                width = 0.5.dp,
                color = AppColors.border,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 🔹 Greeting + Label
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Hi, ${groupFund.groupFundName}",
                style = AppFont.ibmPlexSans(22, FontWeight.SemiBold),
                color = AppColors.headerText
            )

            Text(
                text = "Your available balance",
                style = AppFont.ibmPlexSans(18, FontWeight.Normal),
                color = AppColors.secondaryText
            )
        }

        // 🔹 Balance Amount (clickable)
        Text(
            text = groupFund.currentAvailableAmount.currencyFormattedWithCommas(),
            style = AppFont.ibmPlexSans(26, FontWeight.Bold),
            color = AppColors.headerText,
            modifier = Modifier
                .clickable { onAccountSummaryClick() }
                .padding(start = 8.dp)
        )
    }
}

@Composable
fun ManagerTwoButtons(
    addMemberAction: () -> Unit,
    acceptAmountAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 🔹 Add Member Button
        TwoButtonGradient(
            icon = Icons.Default.PersonAdd, // ✅ Built-in Material icon
            title = "Add Member",
            gradientColors = listOf(AppColors.primaryButton, AppColors.successAccent),
            onClick = addMemberAction
        )

        // 🔹 Approve Payment Button
        TwoButtonGradient(
            icon = Icons.Default.ThumbUp, // ✅ Built-in Material icon
            title = "Approve Payment",
            gradientColors = listOf(AppColors.secondaryAccent, AppColors.warningAccent),
            onClick = acceptAmountAction
        )
    }
}

@Composable
fun TwoButtonGradient(
    icon: ImageVector,
    title: String,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = Brush.horizontalGradient(gradientColors)
            )
            .clickable(onClick = onClick)
            .appShadow(AppShadows.card, RoundedCornerShape(14.dp))
            .padding(vertical = 14.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon, // ✅ using ImageVector directly
                contentDescription = null,
                tint = AppColors.primaryButtonText,
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 6.dp)
            )

            Text(
                text = title,
                style = AppFont.ibmPlexSans(14, FontWeight.SemiBold),
                color = AppColors.primaryButtonText
            )
        }
    }
}

@Composable
fun getDrawableId(name: String): Int {
    val context = LocalContext.current
    return remember(name) {
        context.resources.getIdentifier(name, "drawable", context.packageName)
    }
}

@Composable
fun TotalMemberContributionCard(
    totalMembers: Int,
    totalContribution: String,
    subDetails: List<Pair<String, String>>, // (icon, text)
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.surface)
            .then(
                if (onClick != null)
                    Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .appShadow(AppShadows.card, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start
    ) {

        // 🔹 Total Members
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Total Members",
                style = AppFont.ibmPlexSans(14, FontWeight.Normal),
                color = AppColors.secondaryText
            )
            Text(
                text = totalMembers.toString(),
                style = AppFont.ibmPlexSans(22, FontWeight.SemiBold),
                color = AppColors.headerText
            )
        }

        // 🔹 Total Contribution
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Contribution Received",
                style = AppFont.ibmPlexSans(14, FontWeight.Normal),
                color = AppColors.secondaryText
            )
            Text(
                text = totalContribution,
                style = AppFont.ibmPlexSans(22, FontWeight.SemiBold),
                color = AppColors.primaryButton
            )
        }

        // 🔹 Optional Sub Details
        if (subDetails.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                subDetails.forEach { detail ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = rememberVectorPainter(Icons.Default.CreditCard),
                            contentDescription = null,
                            tint = AppColors.secondaryText,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = detail.second,
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
fun LoanSummaryCard(
    totalSent: String,
    totalReceived: String,
    pending: String,
    interestEarned: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.surface)
            .appShadow(AppShadows.card, RoundedCornerShape(16.dp))
            .clickable { onClick() } // ✅ simple & ripple-safe
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Loan Summary",
            style = AppFont.ibmPlexSans(16, FontWeight.SemiBold),
            color = AppColors.headerText
        )

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LoanStatView(
                    title = "Total Sent",
                    value = totalSent,
                    icon = Icons.Default.ArrowUpward,
                    color = AppColors.errorAccent
                )
                LoanStatView(
                    title = "Total Received",
                    value = totalReceived,
                    icon = Icons.Default.ArrowDownward,
                    color = AppColors.successAccent
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LoanStatView(
                    title = "Pending",
                    value = pending,
                    icon = Icons.Default.HourglassEmpty,
                    color = AppColors.warningAccent
                )
                LoanStatView(
                    title = "Interest Earned",
                    value = interestEarned,
                    icon = Icons.Default.AccountBalance,
                    color = AppColors.infoAccent
                )
            }
        }
    }
}

@Composable
fun LoanStatView(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )

            Text(
                text = title,
                style = AppFont.ibmPlexSans(13, FontWeight.Normal),
                color = AppColors.secondaryText
            )
        }

        Text(
            text = value,
            style = AppFont.ibmPlexSans(18, FontWeight.SemiBold),
            color = AppColors.headerText
        )
    }
}
