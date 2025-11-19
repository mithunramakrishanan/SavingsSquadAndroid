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
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.ThumbUp
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
import com.android.savingssquad.singleton.SquadUserType
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.savingssquad.R
import com.android.savingssquad.model.Squad
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.android.savingssquad.viewmodel.AppDestination
import kotlinx.coroutines.withContext

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
    var openSquadRulesView by remember { mutableStateOf(false) }
    var openDuesScreen by remember { mutableStateOf(false) }
    var openVerifyPayment by remember { mutableStateOf(false) }
    var openManagerSquad by remember { mutableStateOf(false) }
    var openMembersList by remember { mutableStateOf(false) }
    var openLoanDetails by remember { mutableStateOf(false) }
    var openAccountSummary by remember { mutableStateOf(false) }


    // 🔹 Observing ViewModel state via StateFlow
    val squad by squadViewModel.squad.collectAsStateWithLifecycle()
    val squadMembersCount by squadViewModel.squadMembersCount.collectAsStateWithLifecycle()
    val users by squadViewModel.users.collectAsStateWithLifecycle()

    LaunchedEffect(openAccountSummary) {
        if (openAccountSummary) {
            navController.navigate(AppDestination.ACCOUNT_SUMMARY.route)
            openAccountSummary = false
        }
    }

    LaunchedEffect(openManagerSquad) {
        if (openManagerSquad) {
            navController.navigate(AppDestination.MANAGE_SQUAD.route)
            openManagerSquad = false
        }
    }

    LaunchedEffect(openMembersList) {
        if (openMembersList) {
            navController.navigate(AppDestination.OPEN_MEMBERS_LIST.route)
            openMembersList = false
        }
    }

    LaunchedEffect(openLoanDetails) {
        if (openLoanDetails) {
            navController.navigate(AppDestination.OPEN_LOAD_DETAILS.route)
            openLoanDetails = false
        }
    }

    LaunchedEffect(openVerifyPayment) {
        if (openVerifyPayment) {
            navController.navigate(AppDestination.OPEN_VERIFY_PAYMENTS.route)
            openVerifyPayment = false
        }
    }

    LaunchedEffect(openNotificationView) {
        if (openNotificationView) {
            navController.navigate(AppDestination.OPEN_ACTIITY.route)
            openNotificationView = false
        }
    }

    LaunchedEffect(openPaymentHistoryView) {
        if (openPaymentHistoryView) {
            navController.navigate(AppDestination.OPEN_PAYMENT_HISTORY.route)
            openPaymentHistoryView = false
        }
    }

    LaunchedEffect(openSquadRulesView) {
        if (openSquadRulesView) {
            navController.navigate(AppDestination.OPEN_GROUP_RULES.route)
            openSquadRulesView = false
        }
    }

    LaunchedEffect(openDuesScreen) {
        if (openDuesScreen) {
            navController.navigate(AppDestination.OPEN_DUES_SCREEN.route)
            openDuesScreen = false
        }
    }

    // 🔹 Main Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {
        AppBackgroundGradient()

        if (squad != null) {
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
                    rightButtonDrawable = if (UserDefaultsManager.getIsMultipleAccount())
                        R.drawable.switch_account
                    else null
                ) {
                    squadViewModel.fetchUserLogins(
                        showLoader = true,
                        phoneNumber = squadViewModel.loginMember?.phoneNumber.orEmpty()
                    ) { success, loginList,error ->
                        Log.d("ManagerHomeView", if (success) "✅ Logins fetched" else "❌ $error")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 🔹 Scrollable content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    contentPadding = PaddingValues(bottom = 40.dp)
                ) {
                    val gf = squad!!

                    // 🔹 Progress Circle (Centered)
                    item {
                        val remainingMonths = CommonFunctions.getRemainingMonths(
                            startDate = Date(),
                            endDate = gf.squadEndDate?.toDate() ?: Date()
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp), // ensures full width
                            contentAlignment = Alignment.Center // centers horizontally
                        ) {
                            ProgressCircleView(
                                completedMonths = gf.totalDuration - remainingMonths,
                                totalMonths = gf.totalDuration,
                                monthlyContribution = gf.monthlyContribution.currencyFormattedWithCommas(),
                                onClick = { openManagerSquad = true }
                            )
                        }
                    }

                    item {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ManagerHeaderView(squad = squad!!, squadViewModel = squadViewModel, onAccountSummaryClick = {
                                openAccountSummary = true
                            })
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 0.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CheckDuesButton {
                                openDuesScreen = true
                            }
                        }
                    }

                    item {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ManagerTwoButtons(
                                addMemberAction = { squadViewModel.setShowAddMemberPopup(true) },
                                acceptAmountAction = { openVerifyPayment = true }
                            )
                        }
                    }

                    item {


                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TotalMemberContributionCard(
                                totalMembers = squadMembersCount,
                                totalContribution = gf.totalContributionAmountReceived.currencyFormattedWithCommas(),
                                subDetails = listOf(
                                    "creditcard" to "As of ${CommonFunctions.dateToString(Date(), "MMM yyyy")}"
                                ),
                                onClick = { openMembersList = true }
                            )
                        }


                    }

                    item {


                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
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
            }
        }

        // 🔹 Floating Button
        FloatingSquadButton(
            onSquadActivity = { openNotificationView = true },
            onPaymentHistory = { openPaymentHistoryView = true },
            onSquadRules = { openSquadRulesView = true }
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
        squadViewModel.fetchSquadByID(showLoader = true) { success, _, _ ->
            if (success) {
                squadViewModel.fetchEMIConfigurations(showLoader = true) { _, _ ->
                    loaderManager.hideLoader()

                }
            }
        }
    }
}

@Composable
fun ManagerHeaderView(
    squad: Squad,
    squadViewModel: SquadViewModel,
    onAccountSummaryClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp)
            .appShadow(AppShadows.card, RoundedCornerShape(20.dp))
            .background(
                color = AppColors.surface,
                shape = RoundedCornerShape(20.dp)
            )
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
                text = "Hi, ${squad.squadName}",
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
            text = squad.currentAvailableAmount.currencyFormattedWithCommas(),
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp) // 🔹 Left–right margin
    ) {
        TwoButtonGradient(
            icon = Icons.Filled.PersonAdd,
            title = "Add Member",
            gradientColors = listOf(AppColors.primaryButton, AppColors.successAccent),
            onClick = addMemberAction,
            modifier = Modifier.weight(1f)
        )

        TwoButtonGradient(
            icon = Icons.Filled.ThumbUp,
            title = "Approve Payment",
            gradientColors = listOf(AppColors.secondaryAccent, AppColors.warningAccent),
            onClick = acceptAmountAction,
            modifier = Modifier.weight(1f)
        )
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
            .appShadow(AppShadows.card, RoundedCornerShape(16.dp))
            .background(
                color = AppColors.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .then(
                if (onClick != null)
                    Modifier.clickable(onClick = onClick)
                else Modifier
            )
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
            .appShadow(AppShadows.card, RoundedCornerShape(16.dp))
            .background(
                color = AppColors.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // 🔹 Header
        Text(
            text = "Loan Summary",
            style = AppFont.ibmPlexSans(16, FontWeight.SemiBold),
            color = AppColors.headerText
        )

        // 🔹 2x2 Grid (Left–Right aligned)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LoanStatView(
                    title = "Total Sent",
                    value = totalSent,
                    icon = Icons.Filled.ArrowUpward,
                    color = AppColors.errorAccent,
                    modifier = Modifier.weight(1f)
                )

                LoanStatView(
                    title = "Total Received",
                    value = totalReceived,
                    icon = Icons.Filled.ArrowDownward,
                    color = AppColors.successAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LoanStatView(
                    title = "Pending",
                    value = pending,
                    icon = Icons.Filled.HourglassEmpty,
                    color = AppColors.warningAccent,
                    modifier = Modifier.weight(1f)
                )

                LoanStatView(
                    title = "Interest Earned",
                    value = interestEarned,
                    icon = Icons.Filled.AccountBalance,
                    color = AppColors.infoAccent,
                    modifier = Modifier.weight(1f)
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
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier, // ✅ allows use of Modifier.weight(1f)
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // 🔹 Title + Icon Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp) // ⬆️ slightly bigger for balance
            )

            Text(
                text = title,
                style = AppFont.ibmPlexSans(13, FontWeight.Medium),
                color = AppColors.secondaryText
            )
        }

        // 🔹 Value text
        Text(
            text = value,
            style = AppFont.ibmPlexSans(18, FontWeight.SemiBold),
            color = AppColors.headerText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis // ✅ avoids text overflow in narrow layouts
        )
    }
}

@Composable
fun CheckDuesButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(0.dp) // No extra padding for one-line look
    ) {
        Text(
            text = "Check Dues",
            color = AppColors.primaryBrand,
            style = AppFont.ibmPlexSans(16, FontWeight.SemiBold),
            textDecoration = TextDecoration.Underline,
            textAlign = TextAlign.Center
        )
    }
}
