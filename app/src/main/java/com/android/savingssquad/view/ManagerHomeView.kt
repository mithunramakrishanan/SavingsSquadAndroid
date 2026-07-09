package com.android.savingssquad.view

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.appShadow

import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.viewmodel.SquadViewModel
import com.yourapp.utils.CommonFunctions
import com.android.savingssquad.viewmodel.LoaderManager
import java.util.Date
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.savingssquad.R
import com.android.savingssquad.SquadSubscription.BillingHelper
import com.android.savingssquad.SquadSubscription.SubscriptionManager
import com.android.savingssquad.SquadSubscription.TrialBadgeView
import com.android.savingssquad.model.Squad
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.android.savingssquad.viewmodel.AppDestination
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.android.savingssquad.viewmodel.FirestoreManager
import com.android.savingssquad.viewmodel.SSToast
import kotlinx.coroutines.selects.select

@Composable
fun ManagerHomeView(
    navController: NavController,
    squadViewModel: SquadViewModel = viewModel(),
    loaderManager: LoaderManager = LoaderManager.shared
) {
    val context = LocalContext.current

    // 🔹 Navigation states
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

    LaunchedEffect(openDuesScreen) {
        if (openDuesScreen) {
            navController.navigate(AppDestination.OPEN_DUES_SCREEN.route)
            openDuesScreen = false
        }
    }

    LaunchedEffect(Unit) {
        // runs once when Composable enters composition
        if (UserDefaultsManager.getIsFromnotification()) {

            UserDefaultsManager.getLogin()?.let { user ->
                if (user.role == SquadUserType.SQUAD_MANAGER) {
                    UserDefaultsManager.saveSquadManagerLogged(true)
                    openVerifyPayment = true
                }else {
                    UserDefaultsManager.saveSquadManagerLogged(false)
                    navController.navigate(AppDestination.MEMBER_HOME.route) {
                        popUpTo(AppDestination.SIGN_IN.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }

        val squadID = UserDefaultsManager.getLogin()?.squadID ?: return@LaunchedEffect

        SubscriptionManager.shared.refreshFromServer(
            squadID = squadID,
        ) { success, error ->

            if (!success) {
                Log.e("Subscription", error ?: "Unknown error")
                return@refreshFromServer
            }

            val memberCount = squadViewModel.squad.value?.totalMembers ?: 0

            if (SubscriptionManager.shared.shouldForceUpgrade(memberCount)) {
                squadViewModel.setShowUpgradePlan(true)
            }
        }

        FirestoreManager.shared.updateLastActiveDate(squadID,"", SquadUserType.SQUAD_MANAGER) { success,error ->
        }
    }
    // 🔹 Main Layout
    Box(
        modifier = Modifier

            .fillMaxSize()

            .windowInsetsPadding(WindowInsets.safeDrawing)
    )
    {
        AppBackgroundGradient()

        if (squad != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 10.dp)
            )
            {
                // 🔹 Top Navigation Bar
                SSNavigationBar(
                    title = "Manager Dashboard",
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
                        if (loginList != null) {
                            squadViewModel.setShowPopup(UserDefaultsManager.getIsMultipleAccount())
                            Log.d("ManagerHomeView", if (success) "✅ Logins fetched: ${loginList.size}" else "❌ $error")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TrialBadgeView(
                    onClick = {
                        squadViewModel.setShowUpgradePlan(true)
                    }
                )

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

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            SSBadge(
                                title = "Squad ID",
                                value = squadViewModel.squad.value?.squadID ?: "",
                                icon = "🏆",
                                style = BadgeStyle.PRIMARY
                            )
                        }
                    }

                    item {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 7.dp),
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

                            CheckDuesButton(
                                modifier = Modifier.padding(bottom = 10.dp),
                                onClick = {
                                    openDuesScreen = true
                                })
                        }
                    }

                    if (squad?.upiID.isNullOrBlank()) {

                        item {

                            UpdateUPIHintCard(onClick = {

                                navController.navigate(AppDestination.OPEN_BANK_DETAILS.route)

                            }, SquadUserType.SQUAD_MANAGER)

                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    item {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 0.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ManagerTwoButtons(
                                addMemberAction = { squadViewModel.setShowAddMemberPopup(true) },
                                acceptAmountAction = { openVerifyPayment = true },
                                verifyCount = squadViewModel.squad.collectAsState().value?.verifyAmountCount
                                    ?: 0
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
                                onClick = {

                                    if (SubscriptionManager.shared.canUseLoan()) {
                                        openLoanDetails = true
                                    }
                                    else {
                                        squadViewModel.setShowUpgradePlan(true)
                                    }
                                }
                            )
                        }


                    }
                }
            }
        }

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
                color = AppColors.border.copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onAccountSummaryClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // ================= LEFT SECTION =================
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            Text(
                text = "Hi, ${squad.squadName}",
                style = AppFont.ibmPlexSans(18, FontWeight.SemiBold),
                color = AppColors.headerText,
                maxLines = 1
            )

            Row(verticalAlignment = Alignment.CenterVertically) {

                Text(
                    text = "Available balance",
                    style = AppFont.ibmPlexSans(12, FontWeight.Medium),
                    color = AppColors.secondaryText
                )

                Spacer(modifier = Modifier.width(6.dp))

                // subtle dot indicator (iOS feel)
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            AppColors.secondaryText.copy(alpha = 0.4f),
                            CircleShape
                        )
                )
            }
        }

        // ================= RIGHT BALANCE (iOS HERO BLOCK) =================
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            // small label chip
            Box(
                modifier = Modifier
                    .background(
                        AppColors.secondaryText.copy(alpha = 0.08f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Balance",
                    style = AppFont.ibmPlexSans(11, FontWeight.Medium),
                    color = AppColors.secondaryText
                )
            }

            Text(
                text = squad.currentAvailableAmount.currencyFormattedWithCommas(),
                style = AppFont.ibmPlexSans(22, FontWeight.Bold),
                color = AppColors.primaryButton,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ManagerTwoButtons(
    addMemberAction: () -> Unit,
    acceptAmountAction: () -> Unit,
    verifyCount: Int
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ================= ADD MEMBER =================
        TwoButtonGradient(
            icon = Icons.Filled.PersonAdd,
            title = "Add Member",
            subtitle = "Grow your squad",
            gradientColors = listOf(
                AppColors.primaryButton,
                AppColors.successAccent.copy(alpha = 0.95f)
            ),
            onClick = addMemberAction,
            modifier = Modifier.weight(1f)
        )

        // ================= VERIFY PAYMENT =================
        Box(
            modifier = Modifier.weight(1f)
        )
        {

            TwoButtonGradient(
                icon = Icons.Default.VerifiedUser,
                title = "Verify Payment",
                subtitle = "Review requests",
                gradientColors = listOf(
                    AppColors.secondaryAccent,
                    AppColors.warningAccent
                ),
                onClick = acceptAmountAction,
                modifier = Modifier.fillMaxWidth()
            )

            // ================= BADGE (floating count) =================
            if (verifyCount > 0) {

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .offset(x = (-4).dp, y = (-6).dp)
                        .align(Alignment.TopEnd)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                            ),
                            shape = CircleShape
                        )
                        .border(2.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = if (verifyCount > 9) "9+" else verifyCount.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MemberTwoButtons(
    requestCashAction: () -> Unit,
    approveCashAction: () -> Unit,
    verifyCount: Int,
    isRequestCashEnabled: Boolean = true, // flip to true when Request Cash ships
    onRequestCashComingSoon: (() -> Unit)? = null
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ================= REQUEST CASH =================
        TwoButtonGradient(
            icon = Icons.Filled.AddCircle,
            title = "Request Cash",
            subtitle = if (isRequestCashEnabled) "Ask your squad" else "Coming soon",
            gradientColors = listOf(AppColors.primaryButton, AppColors.successAccent),
            onClick = requestCashAction,
            modifier = Modifier.weight(1f),
            isEnabled = isRequestCashEnabled,
            comingSoon = !isRequestCashEnabled,
            onDisabledClick = onRequestCashComingSoon
        )

        Box(
            modifier = Modifier.weight(1f)
        )
        {

            TwoButtonGradient(
                icon = Icons.Default.VerifiedUser,
                title = "Verify Payment",
                subtitle = "Review requests",
                gradientColors = listOf(
                    AppColors.secondaryAccent,
                    AppColors.warningAccent
                ),
                onClick = approveCashAction,
                modifier = Modifier.fillMaxWidth()
            )

            // ================= BADGE (floating count) =================
            if (verifyCount > 0) {

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .offset(x = (-4).dp, y = (-6).dp)
                        .align(Alignment.TopEnd)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                            ),
                            shape = CircleShape
                        )
                        .border(2.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = if (verifyCount > 9) "9+" else verifyCount.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }



    }
}

// =========================================================
//  PREMIUM GRADIENT ACTION BUTTON
// =========================================================

@Composable
fun TwoButtonGradient(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    comingSoon: Boolean = false,
    onDisabledClick: (() -> Unit)? = null
) {

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "actionButtonScale"
    )

    // desaturate the gradient when disabled instead of hiding the button
    val resolvedColors = if (isEnabled) {
        gradientColors
    } else {
        listOf(AppColors.secondaryText.copy(alpha = 0.35f), AppColors.secondaryText.copy(alpha = 0.25f))
    }

    Button(
        onClick = { if (isEnabled) onClick() else onDisabledClick?.invoke() },
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp),
        interactionSource = interactionSource,
        modifier = modifier
            .height(64.dp)
            .scale(scale)
            .appShadow(AppShadows.card, RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(resolvedColors),
                shape = RoundedCornerShape(20.dp)
            )
    ) {

        Box(modifier = Modifier.fillMaxSize()) {

            // subtle glossy highlight across the top for a premium sheen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .align(Alignment.TopCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.14f), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (isEnabled) 0.20f else 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = if (isEnabled) 1f else 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = AppFont.ibmPlexSans(14, FontWeight.SemiBold),
                        color = Color.White.copy(alpha = if (isEnabled) 1f else 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = AppFont.ibmPlexSans(11, FontWeight.Medium),
                            color = Color.White.copy(alpha = if (isEnabled) 0.8f else 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ================= "SOON" PILL =================
            if (comingSoon) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-8).dp, y = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.92f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "SOON",
                        style = AppFont.ibmPlexSans(9, FontWeight.Bold),
                        color = AppColors.headerText
                    )
                }
            }
        }
    }
}

// =========================================================
//  PREMIUM GRADIENT ACTION BUTTON
// =========================================================



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
    subDetails: List<Pair<String, String>>,
    onClick: (() -> Unit)? = null
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .appShadow(
                AppShadows.card,
                RoundedCornerShape(16.dp)
            )
            .background(
                color = AppColors.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .then(
                if (onClick != null)
                    Modifier.clickable { onClick() }
                else Modifier
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        // ================= HEADER =================
        Row(verticalAlignment = Alignment.CenterVertically) {

            Text(
                text = "Overview",
                style = AppFont.ibmPlexSans(14, FontWeight.SemiBold),
                color = AppColors.headerText
            )

            Spacer(modifier = Modifier.weight(1f))

            // 🔥 unified iOS-style icon chip
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(
                        AppColors.secondaryText.copy(alpha = 0.10f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = AppColors.secondaryText.copy(alpha = 0.7f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        // ================= MAIN STATS =================
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "Members",
                    style = AppFont.ibmPlexSans(12, FontWeight.Medium),
                    color = AppColors.secondaryText
                )

                Text(
                    text = totalMembers.toString(),
                    style = AppFont.ibmPlexSans(18, FontWeight.SemiBold),
                    color = AppColors.headerText
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "Contribution",
                    style = AppFont.ibmPlexSans(12, FontWeight.Medium),
                    color = AppColors.secondaryText
                )

                Text(
                    text = totalContribution,
                    style = AppFont.ibmPlexSans(18, FontWeight.SemiBold),
                    color = AppColors.primaryButton
                )
            }
        }

        // ================= SUB DETAILS =================
        if (subDetails.isNotEmpty()) {

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

                subDetails.forEach { detail ->

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {

                        // 🔥 unified icon chip (same system everywhere)
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    AppColors.secondaryText.copy(alpha = 0.10f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = AppColors.secondaryText.copy(alpha = 0.7f),
                                modifier = Modifier.size(10.dp)
                            )
                        }

                        Text(
                            text = detail.second,
                            style = AppFont.ibmPlexSans(11, FontWeight.Medium),
                            color = AppColors.secondaryText,
                            maxLines = 1
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
            .appShadow(AppShadows.card, RoundedCornerShape(18.dp))
            .background(
                color = AppColors.surface,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ================= HEADER (iOS FIXED) =================
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Loan Summary",
                style = AppFont.ibmPlexSans(15, FontWeight.SemiBold),
                color = AppColors.headerText
            )

            Spacer(modifier = Modifier.weight(1f))

            // 🔥 FIXED: replaces "chart.bar.fill"
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = AppColors.secondaryText.copy(alpha = 0.08f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.InsertChart, // ✅ Android equivalent
                    contentDescription = null,
                    tint = AppColors.secondaryText.copy(alpha = 0.65f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // ================= GRID =================
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LoanStatViewIOS(
                    title = "Sent",
                    value = totalSent,
                    icon = Icons.Filled.ArrowUpward,
                    color = AppColors.errorAccent,
                    modifier = Modifier.weight(1f)
                )

                LoanStatViewIOS(
                    title = "Received",
                    value = totalReceived,
                    icon = Icons.Filled.ArrowDownward,
                    color = AppColors.successAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LoanStatViewIOS(
                    title = "Pending",
                    value = pending,
                    icon = Icons.Filled.HourglassEmpty,
                    color = AppColors.warningAccent,
                    modifier = Modifier.weight(1f)
                )

                LoanStatViewIOS(
                    title = "Interest",
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
fun LoanStatViewIOS(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(
                        color = color.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
            }

            Text(
                text = title,
                style = AppFont.ibmPlexSans(12, FontWeight.Medium),
                color = AppColors.secondaryText,
                maxLines = 1
            )
        }

        Text(
            text = value,
            style = AppFont.ibmPlexSans(18, FontWeight.SemiBold),
            color = AppColors.headerText,
            maxLines = 1
        )
    }
}

@Composable
fun UpdateUPIHintCard(
    onClick: () -> Unit,
    selectedUserType : SquadUserType

) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(AppColors.warningAccent.copy(alpha = 0.12f))
            .border(
                1.dp,
                AppColors.warningAccent.copy(alpha = 0.25f),
                RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    AppColors.warningAccent.copy(alpha = 0.18f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = AppColors.warningAccent,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = "Add your UPI ID",
                style = AppFont.ibmPlexSans(16, FontWeight.SemiBold),
                color = AppColors.headerText
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (selectedUserType == SquadUserType.SQUAD_MANAGER)
                    "Members can't send contributions until your UPI ID is added."
                else
                    "Complete your payment setup to contribute securely and effortlessly." ,
                style = AppFont.ibmPlexSans(13),
                color = AppColors.secondaryText
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = AppColors.warningAccent
        )
    }
}
