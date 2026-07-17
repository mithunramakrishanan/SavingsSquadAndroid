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
import androidx.compose.ui.draw.shadow
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.ui.draw.scale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.savingssquad.R
import com.android.savingssquad.viewmodel.AppDestination
import com.android.savingssquad.viewmodel.FirestoreManager
import com.android.savingssquad.viewmodel.SSToast
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
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                // 🔹 Top Navigation Bar
                item {
                    SSNavigationBar(
                        title = "Member Dashboard",
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


                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            squad?.let { fund ->
                                val completed = fund.totalDuration - remainingMonths
                                val total = fund.totalDuration
                                val monthlyContribution = fund.monthlyContribution.currencyFormattedWithCommas()

                                ProgressCircleView(
                                    completedMonths = completed,
                                    totalMonths = total,
                                    monthlyContribution = monthlyContribution,
                                    onClick = { }
                                )
                            } ?: CircularProgressIndicator()

                            SSBadge(
                                title = "Member ID",
                                value = squadViewModel.selectedUser.collectAsState().value?.squadUserId ?: "-",
                                icon = "👤",
                                style = BadgeStyle.INFO
                            )
                        }

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

                if (squadViewModel.currentMember.value?.upiID.isNullOrBlank()) {

                    item {

                        UpdateUPIHintCard(onClick = {

                            navController.navigate(AppDestination.OPEN_BANK_DETAILS.route)

                        }, SquadUserType.SQUAD_MEMBER)

                        Spacer(modifier = Modifier.height(12.dp))
                    }
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
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            AllCaughUPView(
                                title = "All Due’s Paid",
                                subtitle = "Squad all caught up!",
                                icon = Icons.Default.CheckCircle,
                                iconColor = Color(0xFF4CAF50),
                                showChevron = false
                            )
                        }
                    }
                }

                // 🔹 Action Buttons (Request / Approve)
                item {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .padding(vertical = 0.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        MemberTwoButtons(
                            requestCashAction = {

                                val member = squadViewModel.currentMember ?: return@MemberTwoButtons

                                if (member.value?.cashRequested == true || member.value?.currentLoanApproveStatus != EMIStatus.CREATED) {

                                    AlertManager.shared.showAlert(
                                        title = "Request Not Available",
                                        message = "You already have a pending loan or cash request. Please wait until the existing request is confirmed before creating a new request",
                                        type = AlertType.INFO,
                                        primaryButtonTitle = SquadStrings.ok,
                                        primaryAction = {

                                        }
                                    )
                                    return@MemberTwoButtons
                                }


                                squadViewModel.fetchEMIConfigurations(true){success,error->

                                    if (success) {
                                        squadViewModel.setShowRequestCashPopup(true)
                                    }
                                }




                                                },
                            approveCashAction = { navController.navigate(AppDestination.OPEN_VERIFY_PAYMENTS.route) } , verifyCount = verifySquadMemberAmountBadgeCount
                                ?: 0
                        )
                    }
                }

                item {

                    Box(
                        modifier = Modifier.padding(top = 2.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp)
                            .padding(vertical = 0.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CashRequestButton(0) {
                            openCashRequestList = true
                        }
                    }
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
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            lastFivePayments.forEach { payment ->
                                PaymentRow(
                                    payment = payment,
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
                            title = "Request Cash Confirmation",
                            message = "Are you sure you want to request cash for this EMI? Your request will be sent to the Squad Manager. Once approved, the manager will make the payment to you.",
                            primaryButtonTitle = "Request Cash",
                            primaryAction =
                                {
                                    val cashRequest = CashRequest(
                                        id = IDGenerator.generateCashRequestID(),
                                        requestedByName = squadViewModel.currentMember.value?.name ?: "",
                                        requestedByID = squadViewModel.currentMember.value?.id ?: "",
                                        requestedByUPI = squadViewModel.currentMember.value?.upiID ?: "",
                                        requestedByPhone = squadViewModel.currentMember.value?.phoneNumber ?: "",
                                        requestedByEmail = squadViewModel.currentMember.value?.mailID ?: "",
                                        requestedEMIConfig = emi
                                    )

                                    squadViewModel.addCashRequest(true,cashRequest) {success,error ->

                                        ToastManager.show(SquadStrings.appName,"Request Sent Successfully", type = ToastType.SUCCESS)
                                    }

                                },
                            secondaryButtonTitle = "Cancel",
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

                Log.d("MemberHomeView", "✅ Member fetched: ${fetchedMember.name}")
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
        "CONTRIBUTION" -> 168.dp
        "EMI" -> 132.dp
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