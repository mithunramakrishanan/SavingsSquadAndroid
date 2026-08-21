package com.android.savingssquad.view

import android.os.Build
import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.savingssquad.model.*
import com.android.savingssquad.singleton.*
import com.yourapp.utils.CommonFunctions
import com.android.savingssquad.singleton.LoaderManager
import com.android.savingssquad.viewmodel.SquadViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import androidx.navigation.NavController
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.People
import androidx.compose.ui.draw.scale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.savingssquad.R
import com.android.savingssquad.viewmodel.AppDestination
import androidx.compose.runtime.collectAsState
import com.android.savingssquad.viewmodel.AlertManager
import com.android.savingssquad.viewmodel.ToastManager
import com.android.savingssquad.viewmodel.ToastType
import com.yourapp.utils.IDGenerator

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MemberHomeView(
    selectedTab: Int,
    onChangeTab: (Int) -> Unit,
    navController: NavController,
    squadViewModel: SquadViewModel) {
    // Observe state from ViewModel
    val squad by squadViewModel.squad.collectAsStateWithLifecycle()
    val currentMember by squadViewModel.currentMember.collectAsStateWithLifecycle()
    val squadPayments by squadViewModel.squadPayments.collectAsStateWithLifecycle()
    val users by squadViewModel.users.collectAsStateWithLifecycle()
    val showPopup by squadViewModel.showPopup.collectAsStateWithLifecycle()
    val selectedUser by squadViewModel.selectedUser.collectAsStateWithLifecycle()
    var remainders by remember { mutableStateOf(listOf<RemainderModel>()) }
    var currentOrOverDueContribution by remember { mutableStateOf(listOf<ContributionDetail>()) }

    val verifySquadMemberAmountBadgeCount by squadViewModel.verifySquadMemberAmountBadgeCount.collectAsState()

    var openCashRequestList by remember { mutableStateOf(false) }


    LaunchedEffect(openCashRequestList) {
        if (openCashRequestList) {
            navController.navigate(AppDestination.CASH_REQUEST_LIST.route)
            openCashRequestList = false
        }
    }

    LaunchedEffect(Unit) {
        // runs once when Composable enters composition
        if (UserDefaultsManager.getIsFromnotification()) {

            UserDefaultsManager.getLogin()?.let { user ->
                if (user.role == SquadUserType.SQUAD_MEMBER) {
                    UserDefaultsManager.saveSquadManagerLogged(false)
                    navController.navigate(AppDestination.OPEN_VERIFY_PAYMENTS.route)
                }else {
                    UserDefaultsManager.saveSquadManagerLogged(true)
                    navController.navigate(AppDestination.MANAGER_HOME.route) {
                        popUpTo(AppDestination.SIGN_IN.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
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
        if (squad != null) {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(
                    top = 20.dp,
                    bottom = 90.dp
                )
            ) {

                // ---------------------------------------------------------
                // Navigation Bar
                // ---------------------------------------------------------
                item {

                    SSNavigationBar(
                        title = "${SquadStrings.hi}, ${squadViewModel.selectedUser.collectAsState().value?.localizedMemberName ?: ""}",
                        navController = navController,
                        showBackButton = false,
                        rightButtonDrawable =
                            if (UserDefaultsManager.getIsMultipleAccount())
                                R.drawable.switch_account
                            else
                                null,
                        rightButtonAction = {

                            squadViewModel.fetchUserLogins(
                                showLoader = true,
                                phoneNumber = squadViewModel.loginMember?.phoneNumber ?: ""
                            ) { success, loginList, error ->

                                if (loginList != null) {

                                    squadViewModel.setShowPopup(
                                        UserDefaultsManager.getIsMultipleAccount()
                                    )

                                    Log.d(
                                        "MemberHomeView",
                                        if (success)
                                            "✅ User logins fetched: ${loginList.size}"
                                        else
                                            "❌ $error"
                                    )
                                }
                            }
                        },
                        titleTap = {
                            navController.navigate(
                                AppDestination.OPEN_MEMBER_PROFILE.route
                            )
                        }
                    )
                }


                // ---------------------------------------------------------
                // Top Summary
                // iOS:
                // HStack {
                //     ProgressCircle + Badge
                //     Spacer
                //     SquadName + TotalMembers
                // }
                // ---------------------------------------------------------
                item {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // -------------------------
                        // Left Side
                        // -------------------------
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {

                            SSBadge(
                                title = "",
                                value = squadViewModel.loginMember?.squadUserId ?: "-",
                                icon = "👤",
                                style = BadgeStyle.INFO
                            )

                            squad?.let { currentSquad ->

                                val remainingMonths =
                                    squadViewModel.remainingMonths.collectAsState().value

                                ProgressCircleView(
                                    completedMonths =
                                        currentSquad.totalDuration - remainingMonths,
                                    totalMonths =
                                        currentSquad.totalDuration,
                                    monthlyContribution =
                                        currentSquad.monthlyContribution
                                            .currencyFormattedWithCommas(),
                                    onClick = {}
                                )

                            } ?: CircularProgressIndicator()
                        }


                        Spacer(modifier = Modifier.width(16.dp))


                        // -------------------------
                        // Right Side
                        // -------------------------
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 40.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {

                            squad?.let { currentSquad ->

                                SquadNameView(
                                    squadName = currentSquad.localizedSquadName
                                )
                            }

                            TotalMembersCountView(
                                count =
                                    squadViewModel.squadMembersCount
                                        .collectAsStateWithLifecycle()
                                        .value
                            ) {
                                navController.navigate(
                                    AppDestination.OPEN_MEMBERS_LIST.route
                                )
                            }
                        }
                    }
                }


                // ---------------------------------------------------------
                // Member Header
                // ---------------------------------------------------------
                item {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {

                        MemberHeaderView(
                            selectedUser =
                                squadViewModel.selectedUser
                                    .collectAsState()
                                    .value,

                            currentMember =
                                squadViewModel.currentMember
                                    .collectAsState()
                                    .value,

                            amountClicked = {
                                navController.navigate(
                                    AppDestination.OPEN_CONTRUBUTION_DETAILS.route
                                )
                            }
                        )
                    }


                }


                // ---------------------------------------------------------
                // Update UPI
                // ---------------------------------------------------------
                if (
                    squadViewModel.currentMember
                        .value
                        ?.upiID
                        .isNullOrBlank()
                ) {

                    item {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {

                            UpdateUPIHintCard(
                                selectedUserType = SquadUserType.SQUAD_MEMBER,
                                onClick = {

                                    navController.navigate(
                                        AppDestination.OPEN_BANK_DETAILS.route
                                    )
                                }
                            )
                        }


                    }
                }


                // ---------------------------------------------------------
                // Current Available Fund
                // ---------------------------------------------------------
                item {

                    squad?.let { currentSquad ->

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {

                            MemberDashBoardCard(
                                title = SquadStrings.currentAvailableFund,
                                value =
                                    currentSquad.currentAvailableAmount
                                        .currencyFormattedWithCommas(),

                                subDetails = listOf(
                                    "creditcard" to
                                            SquadStrings.asOfDate(CommonFunctions.dateToString(
                                                Date(),
                                                "MMM yyyy"
                                            ))
                                ),

                                onClick = {
                                    navController.navigate(
                                        AppDestination.ACCOUNT_SUMMARY.route
                                    )
                                }
                            )
                        }


                    }
                }


                // ---------------------------------------------------------
                // Reminders
                // ---------------------------------------------------------
                if (remainders.isNotEmpty()) {

                    item {

                            SectionView(
                                title = SquadStrings.remainders
                            )
                            {

                                LazyRow(
                                    contentPadding =
                                        PaddingValues(horizontal = 16.dp),

                                    horizontalArrangement =
                                        Arrangement.spacedBy(15.dp)
                                ) {

                                    items(
                                        items = remainders,
                                        key = { it.id }
                                    ) { reminder ->

                                        RemainderCardView(
                                            title = reminder.remainderTitle,
                                            subtitle = reminder.remainderSubTitle,
                                            amount =
                                                reminder.remainderAmount.toString(),
                                            dueDate =
                                                reminder.remainderDueDate.orNow
                                        ) {

                                            UserDefaultsManager.saveRemainder(
                                                reminder
                                            )

                                            onChangeTab(1)
                                        }
                                    }
                                }
                            }
                    }

                } else {

                    // -----------------------------------------------------
                    // All Dues Paid
                    // -----------------------------------------------------
                    item {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {

                                AllCaughUPView(
                                    title = SquadStrings.allDuesPaid,
                                    subtitle = SquadStrings.squadAllCaughtUp,
                                    icon = Icons.Default.CheckCircle,
                                    iconColor = Color.Green,
                                    showChevron = false
                                )
                            }

                        }


                    }
                }


                // ---------------------------------------------------------
                // Request / Approve Buttons
                // ---------------------------------------------------------
                item {

                    MemberTwoButtons(
                        requestCashAction = {

                            val member =
                                squadViewModel.currentMember
                                    .value
                                    ?: return@MemberTwoButtons


                            // -----------------------------
                            // UPI validation
                            // -----------------------------
                            if (member.upiID.isNullOrBlank()) {

                                AlertManager.shared.showAlert(
                                    title = SquadStrings.savingsSquad,
                                    message =
                                        SquadStrings.updateUPIForCashRequest,
                                    type = AlertType.INFO,
                                    primaryButtonTitle =
                                        SquadStrings.ok,
                                    primaryAction = {

                                        navController.navigate(
                                            AppDestination.OPEN_BANK_DETAILS.route
                                        )
                                    }
                                )

                                return@MemberTwoButtons
                            }


                            // -----------------------------
                            // Pending request validation
                            // -----------------------------
                            if (
                                member.cashRequested == true ||
                                member.currentLoanApproveStatus !=
                                EMIStatus.CREATED
                            ) {

                                AlertManager.shared.showAlert(
                                    title =
                                        SquadStrings.requestNotAvailable,

                                    message =
                                        SquadStrings.pendingLoanOrCashRequestMessage,

                                    type = AlertType.INFO,

                                    primaryButtonTitle =
                                        SquadStrings.ok,

                                    primaryAction = {}
                                )

                                return@MemberTwoButtons
                            }


                            // -----------------------------
                            // Fetch EMI configuration
                            // -----------------------------
                            squadViewModel.fetchEMIConfigurations(
                                true
                            ) { success, error ->

                                if (success) {

                                    squadViewModel.setShowRequestCashPopup(
                                        true
                                    )
                                }
                            }
                        },

                        approveCashAction = {

                            navController.navigate(
                                AppDestination.OPEN_VERIFY_PAYMENTS.route
                            )
                        },

                        verifyCount =
                            verifySquadMemberAmountBadgeCount ?: 0
                    )
                }


                // ---------------------------------------------------------
                // Cash Request Button
                // ---------------------------------------------------------
                item {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {

                        CashRequestButton(
                            pendingCount = 0
                        ) {

                            openCashRequestList = true
                        }
                    }
                }


                // ---------------------------------------------------------
                // Recent Transactions
                // ---------------------------------------------------------
                item {

                    val currentMemberId =
                        squadViewModel.currentMember
                            .value
                            ?.id
                            ?: ""

                    val lastFivePayments =
                        squadPayments
                            .filter {
                                it.memberId == currentMemberId
                            }
                            .sortedByDescending {
                                it.paymentUpdatedDate
                                    ?.toDate()
                                    ?: Date(0)
                            }
                            .take(5)

                    if (lastFivePayments.isNotEmpty()) {

                        SectionView(
                            title = SquadStrings.recentTransactions
                        )
                        {

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),

                                verticalArrangement =
                                    Arrangement.spacedBy(16.dp)
                            ) {

                                lastFivePayments.forEach { payment ->

                                    PaymentRow(
                                        payment = payment
                                    )
                                }


                                // -----------------------------
                                // View All
                                // -----------------------------
                                if (
                                    squadPayments.count {
                                        it.memberId == currentMemberId
                                    } > 5
                                ) {

                                    ViewAllButton(
                                        title = SquadStrings.viewAll,
                                        icon =
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight
                                    ) {

                                        navController.navigate(
                                            AppDestination.OPEN_PAYMENT_HISTORY.route
                                        )
                                    }
                                }
                            }
                        }
                    }


                }
            }
        }

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

        val showRequestCashPopup by squadViewModel.showRequestCashPopup
            .collectAsStateWithLifecycle()

        if (showRequestCashPopup) {

            OverlayBackgroundView(
                showPopup = remember { mutableStateOf(showRequestCashPopup) },
                onDismiss = {
                    squadViewModel.setShowRequestCashPopup(false)
                }
            ) {

                RequestCashEMIListView(

                    emiConfigs = squadViewModel.emiConfigurations
                        .collectAsState()
                        .value,

                    onRequestCash = { emi ->

                        println("Request Cash : ${emi.loanAmount}")

                        AlertManager.shared.showAlert(
                            title = SquadStrings.requestCashConfirmation,
                            message = SquadStrings.requestCashConfirmationMessage,
                            primaryButtonTitle = SquadStrings.requestCash,
                            primaryAction =
                                {
                                    val cashRequest = CashRequest(
                                        id = IDGenerator.generateCashRequestID(),
                                        requestedByName = squadViewModel.currentMember.value?.memberName ?: "",
                                        requestedByID = squadViewModel.currentMember.value?.id ?: "",
                                        requestedByUPI = squadViewModel.currentMember.value?.upiID ?: "",
                                        requestedByPhone = squadViewModel.currentMember.value?.phoneNumber ?: "",
                                        requestedByEmail = squadViewModel.currentMember.value?.mailID ?: "",
                                        requestedEMIConfig = emi
                                    )

                                    squadViewModel.addCashRequest(true,cashRequest) {success,error ->

                                        ToastManager.show(SquadStrings.savingsSquad, SquadStrings.requestSentSuccessfully, type = ToastType.SUCCESS)
                                    }

                                },
                            secondaryButtonTitle = SquadStrings.cancel,
                            secondaryAction = {}
                        )



                    },

                    onDismiss = {

                        squadViewModel.setShowRequestCashPopup(false)

                    }
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
                                remainderTitle = SquadStrings.contribution,
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
                )
                { _, _ ->
                    val pendingUnpaidInstallments =
                        (squadViewModel.memberPendingLoans.value?.firstOrNull()?.installments?.currentAndOverdueUnpaid()
                            ?: emptyList()) +
                                (squadViewModel.memberPendingLoans.value?.firstOrNull()?.installments?.upcomingUnpaid()
                                    ?: emptyList())

                    val loanRemainders = pendingUnpaidInstallments.map { emi ->
                        RemainderModel(
                            remainderTitle = SquadStrings.emi,
                            remainderSubTitle = emi.installmentNumber,
                            remainderType = RemainderType.EMI,
                            remainderAmount = emi.installmentAmount + emi.interestAmount,
                            remainderID = emi.id ?: "",
                            remainderDueDate = emi.dueDate ?: Timestamp.now()
                        )
                    }

                    remainders = (remainders + loanRemainders)
                        .sortedBy { it.remainderDueDate?.toDate() ?: Date() }


                    squadViewModel.fetchPayments(
                        showLoader = true,
                        memberId = fetchedMember.id ?: ""
                    ) { _, error ->
                        if (error != null) {
                            println("❌ $error")
                        }
                    }

//                    FirestoreManager.shared.updateLastActiveDate(fetchedMember.squadID,fetchedMember.id ?: "", SquadUserType.SQUAD_MEMBER) { success,error ->
//                    }

                    LoaderManager.shared.hideLoader()
                }

                squadViewModel.fetchMemberOtherPayments(false,fetchedMember.id ?: "", paidStatus = PaidStatus.NOT_PAID, type = MemberPaymentSubType.RE_PAYMENT, completion = { success, error ->
                if (squadViewModel.memberOtherPayments.value?.size != 0) {


                    val otherPaymentsRemainder =

                        squadViewModel.memberOtherPayments.value?.map { payment ->

                            RemainderModel(

                                remainderTitle = payment.localizedDescription,

                                remainderSubTitle = "",

                                remainderType = RemainderType.OTHER_REMAINDER,

                                remainderAmount = payment.amount,

                                remainderID = payment.id ?: "",

                                remainderDueDate = Timestamp.now()

                            )

                        } ?: emptyList()
                    remainders = remainders + otherPaymentsRemainder
                }

                })

                Log.d("MemberHomeView", "✅ Member fetched: ${fetchedMember.memberName}")
            } else {
                Log.e("MemberHomeView", "❌ Error: $error")
                LoaderManager.shared.hideLoader()
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
        SimpleDateFormat("dd MMM", Locale.ENGLISH).format(dueDate)

    val cardWidth = when (title.uppercase()) {
        SquadStrings.contribution -> 168.dp
        SquadStrings.emi ->  132.dp
        else -> 168.dp
    }

    val statusColor =
        if (isOverdue) AppColors.errorAccent
        else AppColors.warningAccent

    val interactionSource = remember { MutableInteractionSource() }

    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 400f
        ),
        label = "ReminderCardScale"
    )

    Column(
        modifier = Modifier
            .width(cardWidth)
            .padding(horizontal = 4.dp)
            .scale(scale)
            .appShadow(
                style = AppShadows.card,
                shape = RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.surface)
            .border(
                1.dp,
                AppColors.border.copy(alpha = 0.5f),
                RoundedCornerShape(14.dp)
            )
            .clickable(
                indication = null,
                interactionSource = interactionSource
            ) {
                onTap()
            }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        // MARK: Header
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = title,
                style = AppFont.ibmPlexSans(
                    13,
                    FontWeight.SemiBold
                ),
                color = AppColors.headerText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        // MARK: Subtitle
        Text(
            text = subtitle,
            style = AppFont.ibmPlexSans(
                11,
                FontWeight.Medium
            ),
            color = AppColors.secondaryText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // MARK: Amount
        Text(
            text = "₹$amount",
            style = AppFont.ibmPlexSans(
                17,
                FontWeight.Bold
            ),
            color = AppColors.headerText
        )

        // MARK: Due Status
        Text(
            text = "${if (isOverdue) "Overdue" else "Due"} • $formattedDate",
            style = AppFont.ibmPlexSans(
                11,
                FontWeight.Medium
            ),
            color = statusColor
        )
    }
}

@Composable
fun SquadNameView(
    squadName: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AppColors.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = AppColors.primaryButton.copy(alpha = 0.10f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(
                horizontal = 10.dp,
                vertical = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    AppColors.primaryButton.copy(alpha = 0.10f)
                ),
            contentAlignment = Alignment.Center
        ) {

            AppIconView(
                name = "person.3.fill",
                tint = AppColors.primaryButton,
                size = 12.dp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {

            Text(
                text = SquadStrings.squadName,
                style = AppFont.ibmPlexSans(
                    size = 10,
                    weight = FontWeight.Medium
                ),
                color = AppColors.secondaryText,
                maxLines = 1
            )

            Text(
                text = squadName,
                style = AppFont.ibmPlexSans(
                    size = 13,
                    weight = FontWeight.SemiBold
                ),
                color = AppColors.headerText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TotalMembersCountView(
    count: Int,
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.surface)
            .border(
                width = 1.dp,
                color = AppColors.primaryButton.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(
                horizontal = 12.dp,
                vertical = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    AppColors.primaryButton.copy(alpha = 0.10f)
                ),
            contentAlignment = Alignment.Center
        ) {

            Icon(
                imageVector = Icons.Default.People,
                contentDescription = null,
                tint = AppColors.primaryButton,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {

            Text(
                text = SquadStrings.members,
                style = AppFont.ibmPlexSans(
                    11,
                    FontWeight.Medium
                ),
                color = AppColors.secondaryText
            )

            Text(
                text = count.toString(),
                style = AppFont.ibmPlexSans(
                    19,
                    FontWeight.Bold
                ),
                color = AppColors.headerText
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun MemberHeaderView(
    selectedUser: Login?,
    currentMember: Member?,
    amountClicked: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                amountClicked()
            }
            .appShadow(
                style = AppShadows.card,
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.surface
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = AppColors.primaryButton,
                        modifier = Modifier.size(16.dp)
                    )

                    Text(
                        text = SquadStrings.yourCorrentContribution,
                        style = AppFont.ibmPlexSans(
                            14,
                            FontWeight.Medium
                        ),
                        color = AppColors.secondaryText
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = AppColors.secondaryText.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Amount + Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {

                Text(
                    text = currentMember
                        ?.totalContributionPaid
                        ?.currencyFormattedWithCommas()
                        ?: "₹ 0",
                    style = AppFont.ibmPlexSans(
                        30,
                        FontWeight.Bold
                    ),
                    color = AppColors.headerText,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = SquadStrings.viewDetails,
                    style = AppFont.ibmPlexSans(
                        11,
                        FontWeight.Medium
                    ),
                    color = AppColors.primaryButton
                )
            }
        }
    }
}


@Composable
fun MemberDashBoardCard(
    title: String,
    value: String,
    subDetails: List<Pair<String, String>>,
    onClick: (() -> Unit)? = null
) {

    val interactionSource = remember {
        MutableInteractionSource()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .appShadow(AppShadows.card)
            .background(
                color = AppColors.surface,
                shape = RoundedCornerShape(14.dp)
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = androidx.compose.material3.ripple()
                    ) {
                        onClick()
                    }
                } else {
                    Modifier
                }
            )
            .padding(12.dp),

        verticalArrangement = Arrangement.spacedBy(7.dp),
        horizontalAlignment = Alignment.Start
    ) {

        // Title
        Text(
            text = title,
            style = AppFont.ibmPlexSans(
                size = 12,
                weight = FontWeight.Medium
            ),
            color = AppColors.secondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )


        // Value
        Text(
            text = value,
            style = AppFont.ibmPlexSans(
                size = 20,
                weight = FontWeight.Bold
            ),
            color = AppColors.headerText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false
        )


        // Sub Details
        if (subDetails.isNotEmpty()) {

            subDetails.forEach { (icon, text) ->

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {

                    AppIconView(
                        name = icon,
                        tint = AppColors.primaryButton,
                        size = 9.dp
                    )

                    Text(
                        text = text,
                        style = AppFont.ibmPlexSans(
                            size = 10,
                            weight = FontWeight.Normal
                        ),
                        color = AppColors.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ViewAllButton(
    title: String = SquadStrings.viewAll,
    icon: ImageVector = Icons.AutoMirrored.Filled.ArrowForward,
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.surface)
            .border(
                1.dp,
                AppColors.border.copy(alpha = 0.35f),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text(
            text = title,
            style = AppFont.ibmPlexSans(
                13,
                FontWeight.SemiBold
            ),
            color = AppColors.headerText
        )

        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(AppColors.primaryBrand.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.primaryBrand,
                modifier = Modifier.size(11.dp)
            )
        }
    }
}