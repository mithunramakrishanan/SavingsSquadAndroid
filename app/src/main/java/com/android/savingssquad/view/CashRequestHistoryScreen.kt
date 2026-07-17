package com.android.savingssquad.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.input.pointer.pointerInput
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.ShadowStyle
import com.android.savingssquad.singleton.appShadow

import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import com.android.savingssquad.model.Installment
import com.android.savingssquad.model.Login
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.viewmodel.SquadViewModel
import com.yourapp.utils.CommonFunctions
import kotlinx.coroutines.delay
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import java.util.Date
import java.util.concurrent.TimeUnit
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.android.savingssquad.R
import com.android.savingssquad.SquadSubscription.SubscriptionManager
import com.android.savingssquad.model.CashRequest
import com.android.savingssquad.model.CashRequestStatus
import com.android.savingssquad.model.EMIConfiguration
import com.android.savingssquad.model.Member
import com.android.savingssquad.singleton.EMIStatus
import com.android.savingssquad.singleton.RecordStatus
import com.android.savingssquad.singleton.color
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.android.savingssquad.singleton.displayText
import com.android.savingssquad.viewmodel.AppDestination
import com.google.firebase.Timestamp
import com.google.firebase.auth.PhoneAuthOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashRequestHistoryScreen(
    navController: NavController,
    squadViewModel: SquadViewModel
) {

    var selectedUser by remember { mutableStateOf("All") }
    var selectedMemberId by remember { mutableStateOf<String?>(null) }

    val screenType =
        if (UserDefaultsManager.getSquadManagerLogged())
            SquadUserType.SQUAD_MANAGER
        else
            SquadUserType.SQUAD_MEMBER

    if (screenType == SquadUserType.SQUAD_MEMBER) {
        selectedMemberId = UserDefaultsManager.getLogin()?.squadUserId
    }

    val cashRequests by squadViewModel.squadCashRequests
    val members by squadViewModel.squadMembers.collectAsState()

    val userList = remember(members) {
        buildList {
            add("All")
            addAll(members.map { it.name }.distinct())
        }
    }

    var hasLoaded by rememberSaveable { mutableStateOf(false) }

    val activity = LocalContext.current as Activity
    val appContext = LocalContext.current.applicationContext

    fun configureInitialFilter() {

        if (screenType == SquadUserType.SQUAD_MEMBER) {

            selectedMemberId = squadViewModel.currentMember.value?.id
            selectedUser = squadViewModel.currentMember.value?.name ?: ""

        } else {

            selectedUser = "All"
            selectedMemberId = null
        }
    }

    fun reloadCashRequests() {

        squadViewModel.resetCashRequestsPagination()

        squadViewModel.fetchCashRequests(
            showLoader = true,
            memberId = selectedMemberId
        ) { success, error ->

            if (!success) {
                Log.e("CashRequest", error ?: "")
            }
        }
    }

    LaunchedEffect(Unit) {

        if (!hasLoaded) {

            hasLoaded = true

            configureInitialFilter()

            reloadCashRequests()
        }
    }

    Box(
        modifier = Modifier

            .fillMaxSize()

            .windowInsetsPadding(WindowInsets.safeDrawing)
    )
    {
        AppBackgroundGradient()
        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            SSNavigationBar(
                title = SquadStrings.cashRequests,
                navController = navController
            )

            Spacer(modifier = Modifier.height(12.dp))

//                if (screenType != SquadUserType.SQUAD_MEMBER) {

                    DropdownMenuPicker(
                        selected = selectedUser,
                        items = userList,
                        icon = Icons.Default.Tune,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                    ) { newUser ->

                            selectedUser = newUser

                            selectedMemberId =
                                if (newUser == "All") {
                                    null
                                } else {
                                    members.firstOrNull {
                                        it.name == newUser
                                    }?.id
                                }

                            reloadCashRequests()
                        }

//                }

            Spacer(modifier = Modifier.height(8.dp))

                if (
                    cashRequests.isEmpty() &&
                    !squadViewModel.cashRequestsIsLoadingMore
                ) {

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Icon(
                            Icons.Default.Payments,
                            null,
                            modifier = Modifier.size(72.dp),
                            tint = Color.Gray.copy(alpha = 0.6f)
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "No cash requests yet",
                            style = AppFont.ibmPlexSans(
                                15,
                                FontWeight.Medium
                            ),
                            color = AppColors.secondaryText
                        )
                    }

                }

                //----------------------------------------------------
                // List
                //----------------------------------------------------

                else {

                    LazyColumn(

                        modifier = Modifier

                            .fillMaxSize()

                            .background(AppColors.background),

                        verticalArrangement = Arrangement.spacedBy(16.dp),

                        contentPadding = PaddingValues(vertical = 16.dp)

                    ) {

                        items(
                            cashRequests,
                            key = { it.id ?: "" }
                        ) { cashRequest ->

                            CashRequestRow(

                                cashRequest = cashRequest,

                                showActions =
                                    screenType != SquadUserType.SQUAD_MEMBER &&
                                            cashRequest.cashRequestStatus ==
                                            CashRequestStatus.CREATED,

                                onAccept = {

                                    val member = Member(
                                        id = cashRequest.requestedByID,
                                        name = cashRequest.requestedByName,
                                        profileImage = "",
                                        phoneNumber = cashRequest.requestedByPhone,
                                        password = "",
                                        squadID = squadViewModel.squad.value?.squadID ?: "",
                                        role = SquadUserType.SQUAD_MEMBER,
                                        memberCreatedDate = Timestamp.now(),
                                        upiBeneId = "",
                                        bankBeneId = "",
                                        upiID = cashRequest.requestedByUPI,
                                        fcmToken = "",
                                        currentLoanApproveStatus = EMIStatus.INVERIFICATION,
                                        cashRequested = true
                                    )

                                    cashRequest.requestedEMIConfig?.let { emi ->

                                        cashRequest.id?.let { cashRequestId ->

                                            squadViewModel.makeLoanPayment(
                                                activity = activity,
                                                appContext,
                                                selectedMember = member,
                                                selectedLoan = emi,
                                                cashRequestId = cashRequestId
                                            ) { success, error ->

                                                if (success) {

                                                    println("✅ Payment added successfully!")

                                                } else {

                                                    println("❌ Error adding payment: ${error ?: "Unknown error"}")
                                                }
                                            }
                                        }
                                    }
                                },

                                onReject = {

                                    squadViewModel.updateCashRequestStatus(
                                        squadID = squadViewModel.squad.value?.squadID ?: "",
                                        cashRequestId = cashRequest.id ?: "",
                                        memberId = cashRequest.requestedByID,
                                        status = CashRequestStatus.REJECTED
                                    ) { _, _ ->

                                        reloadCashRequests()
                                    }
                                }
                            )

                            LaunchedEffect(cashRequest.id) {

                                squadViewModel.loadMoreCashRequestsIfNeeded(
                                    currentCashRequest = cashRequest,
                                    memberId = selectedMemberId
                                )
                            }
                        }

                        if (
                            squadViewModel.cashRequestsIsLoadingMore &&
                            cashRequests.isNotEmpty()
                        ) {

                            item {

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 20.dp),
                                    contentAlignment = Alignment.Center
                                ) {

                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }

    }
}

@Composable
fun CashRequestRow(
    cashRequest: CashRequest,
    showActions: Boolean = false,
    onAccept: () -> Unit = {},
    onReject: () -> Unit = {}
) {

    val statusColor = when (cashRequest.cashRequestStatus) {
        CashRequestStatus.CREATED -> AppColors.warningAccent
        CashRequestStatus.ACCEPTED -> AppColors.successAccent
        CashRequestStatus.REJECTED -> AppColors.errorAccent
    }

    val formattedDate = remember(cashRequest.requestedOn) {
        cashRequest.requestedOn?.toDate()?.let {
            SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH).format(it)
        } ?: ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.surface)
            .border(
                1.dp,
                AppColors.border.copy(alpha = 0.45f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        //---------------------------------------------------------
        // Header
        //---------------------------------------------------------

        Row(
            verticalAlignment = Alignment.Top
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = cashRequest.requestedByName,
                    style = AppFont.ibmPlexSans(
                        15,
                        FontWeight.SemiBold
                    ),
                    color = AppColors.headerText
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = formattedDate,
                    style = AppFont.ibmPlexSans(
                        12,
                        FontWeight.Normal
                    ),
                    color = AppColors.secondaryText
                )
            }

            StatusChip(
                text = if (cashRequest.cashRequestStatus == CashRequestStatus.CREATED){"Requested"}else{cashRequest.cashRequestStatus.name} ,
                statusDate = CommonFunctions.dateToString(cashRequest.requestAcceptedOn?.toDate() ?: Date()),
                color = statusColor
            )


        }

        //---------------------------------------------------------
        // EMI Details
        //---------------------------------------------------------

        cashRequest.requestedEMIConfig?.let { emi ->

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column {

                    Text(
                        "Amount",
                        style = AppFont.ibmPlexSans(
                            11,
                            FontWeight.Normal
                        ),
                        color = AppColors.secondaryText
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        "₹${emi.loanAmount}",
                        style = AppFont.ibmPlexSans(
                            18,
                            FontWeight.Bold
                        ),
                        color = AppColors.headerText
                    )
                }

                Spacer(Modifier.weight(1f))

                if (emi.emiInterestRate > 0) {

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {

                        Text(
                            text = "Interest",
                            style = AppFont.ibmPlexSans(11, FontWeight.Normal),
                            color = AppColors.secondaryText
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "${emi.emiInterestRate}%",
                            style = AppFont.ibmPlexSans(18, FontWeight.Bold),
                            color = AppColors.headerText
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .background(
                                    color = AppColors.primaryBrand.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(50)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = emi.interestType.name.lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                style = AppFont.ibmPlexSans(10, FontWeight.Medium),
                                color = AppColors.primaryBrand
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = AppColors.border
            )

            Row {

                InfoItem(
                    title = "Months",
                    value = "${emi.emiMonths}"
                )

                Spacer(Modifier.weight(1f))

                InfoItem(
                    title = "Monthly EMI",
                    value = "₹${emi.emiAmount}"
                )

                Spacer(Modifier.weight(1f))

                InfoItem(
                    title = "Total Interest",
                    value = "₹${emi.interestAmount}"
                )
            }
        }
        if (showActions) {

            HorizontalDivider(
                color = AppColors.border
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = AppColors.errorAccent.copy(alpha = .08f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        AppColors.errorAccent.copy(alpha = .25f)
                    )
                ) {

                    Text(
                        "Reject",
                        style = AppFont.ibmPlexSans(
                            13,
                            FontWeight.SemiBold
                        ),
                        color = AppColors.errorAccent
                    )
                }

                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.successAccent
                    )
                ) {

                    Text(
                        "Accept",
                        style = AppFont.ibmPlexSans(
                            13,
                            FontWeight.SemiBold
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    statusDate: String,
    color: Color
) {

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {

            Text(
                text = text.replaceFirstChar { it.uppercase() },
                style = AppFont.ibmPlexSans(
                    11,
                    FontWeight.SemiBold
                ),
                color = color
            )
        }

        if (statusDate.isNotEmpty()) {
            Text(
                text = statusDate,
                style = AppFont.ibmPlexSans(
                    9,
                    FontWeight.Normal
                ),
                color = AppColors.secondaryText
            )
        }
    }
}

@Composable
private fun InfoItem(
    title: String,
    value: String
) {

    Column {

        Text(
            title,
            style = AppFont.ibmPlexSans(
                11,
                FontWeight.Normal
            ),
            color = AppColors.secondaryText
        )

        Spacer(Modifier.height(2.dp))

        Text(
            value,
            style = AppFont.ibmPlexSans(
                14,
                FontWeight.Bold
            ),
            color = AppColors.headerText
        )
    }
}

@Composable
fun CashRequestEmptyView() {

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector = Icons.Default.AccountBalanceWallet,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = AppColors.secondaryText.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Cash Requests Yet",
            style = AppFont.ibmPlexSans(
                16,
                FontWeight.SemiBold
            ),
            color = AppColors.headerText
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Cash requests will appear here.",
            style = AppFont.ibmPlexSans(
                13,
                FontWeight.Medium
            ),
            color = AppColors.secondaryText
        )
    }
}

@Composable
fun RequestCashEMIListView(
    title: String = "Request Cash",
    emiConfigs: List<EMIConfiguration>,
    onRequestCash: (EMIConfiguration) -> Unit = {},
    onDismiss: () -> Unit
) {

    var appeared by remember { mutableStateOf(false) }

    var expandedIndex by rememberSaveable {
        mutableStateOf<Int?>(null)
    }

    LaunchedEffect(Unit) {
        appeared = true
    }

    Column(
        modifier = Modifier
            .width(330.dp)
            .heightIn(max = 700.dp)
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.25f)
            )
            .background(
                color = AppColors.surface,
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                color = AppColors.border.copy(alpha = 0.4f),
                shape = RoundedCornerShape(24.dp)
            )
    ) {

        // MARK: Top accent bar — small brand gradient sliver for a premium finish

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            AppColors.primaryBrand,
                            AppColors.primaryBrand.copy(alpha = 0.5f)
                        )
                    )
                )
        )

        // MARK: Header

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 14.dp, start = 12.dp, end = 12.dp)
        ) {

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    AppColors.primaryBrand,
                                    AppColors.primaryBrand.copy(alpha = 0.7f)
                                )
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = title,
                    style = AppFont.ibmPlexSans(size = 18, weight = FontWeight.Bold),
                    color = AppColors.headerText
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .background(
                            color = AppColors.primaryBrand.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${emiConfigs.size} option${if (emiConfigs.size == 1) "" else "s"} available",
                        style = AppFont.ibmPlexSans(size = 11, weight = FontWeight.SemiBold),
                        color = AppColors.primaryBrand
                    )
                }
            }
        }

        HorizontalDivider(color = AppColors.secondaryText.copy(alpha = 0.12f))

        // MARK: EMI List

        if (emiConfigs.isEmpty()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No EMI options available right now",
                    style = AppFont.ibmPlexSans(size = 12, weight = FontWeight.Medium),
                    color = AppColors.secondaryText
                )
            }

        } else {

            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 260.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 14.dp)
            ) {

                itemsIndexed(
                    items = emiConfigs,
                    key = { index, item -> item.id ?: index }
                ) { index, emi ->

                    AnimatedVisibility(
                        visible = appeared,
                        enter = fadeIn(
                            animationSpec = tween(durationMillis = 280, delayMillis = index * 35)
                        ) + slideInVertically(initialOffsetY = { 8 })
                    ) {
                        EMIRequestRow(
                            emi = emi,
                            index = index,
                            expanded = expandedIndex == index,
                            onExpand = {
                                expandedIndex =
                                    if (expandedIndex == index) null
                                    else index
                            },
                            onRequestCash = {
                                onRequestCash(emi)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = AppColors.secondaryText.copy(alpha = 0.12f))

        // MARK: Footer

        TextButton(
            onClick = { onDismiss() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = "Close",
                style = AppFont.ibmPlexSans(size = 13, weight = FontWeight.SemiBold),
                color = AppColors.secondaryText
            )
        }
    }
}

// MARK: - EMI Row

@Composable
private fun EMIRequestRow(

    emi: EMIConfiguration,

    index: Int,

    expanded: Boolean,

    onExpand: () -> Unit,

    onRequestCash: () -> Unit

) {

    val memberLoan = remember(emi) {
        CommonFunctions.generateMemberLoan(
            emiConfig = emi,
            memberID = "",
            memberName = ""
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AppColors.background,
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 1.dp,
                color = AppColors.border.copy(alpha = 0.25f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // MARK: Header row — loan amount + request button

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    "Loan Amount",
                    style = AppFont.ibmPlexSans(10, FontWeight.Medium),
                    color = AppColors.secondaryText
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    emi.loanAmount.currencyFormattedWithCommas(),
                    style = AppFont.ibmPlexSans(20, FontWeight.Bold),
                    color = AppColors.headerText
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        "${emi.emiMonths} Months",
                        style = AppFont.ibmPlexSans(10, FontWeight.Medium),
                        color = AppColors.primaryBrand
                    )

                    Spacer(Modifier.width(6.dp))

                    Box(
                        Modifier
                            .size(3.dp)
                            .background(
                                AppColors.border,
                                CircleShape
                            )
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        "${"%.2f".format(emi.emiInterestRate)}% ${emi.interestType}",
                        style = AppFont.ibmPlexSans(10, FontWeight.Medium),
                        color = AppColors.secondaryText
                    )
                }
            }

            Button(
                onClick = onRequestCash,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.primaryBrand
                )
            ) {

                Text(
                    "Request",
                    style = AppFont.ibmPlexSans(12, FontWeight.SemiBold),
                    color = Color.White
                )
            }
        }

        HorizontalDivider(color = AppColors.secondaryText.copy(alpha = 0.12f))

        // MARK: Monthly EMI / Interest / Total — mirrors iOS infoView row

        Row(modifier = Modifier.fillMaxWidth()) {
            InfoView(
                modifier = Modifier.weight(1f),
                title = "Monthly EMI",
                value = emi.emiAmount.currencyFormattedWithCommas()
            )
            InfoView(
                modifier = Modifier.weight(1f),
                title = "Interest",
                value = emi.interestAmount.currencyFormattedWithCommas()
            )
            InfoView(
                modifier = Modifier.weight(1f),
                title = "Total",
                value = (emi.loanAmount + emi.interestAmount).currencyFormattedWithCommas()
            )
        }

        HorizontalDivider(color = AppColors.secondaryText.copy(alpha = 0.12f))

        // MARK: Installment Schedule toggle — mirrors iOS expandable button

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpand() },
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = AppColors.primaryBrand,
                modifier = Modifier.size(14.dp)
            )

            Spacer(Modifier.width(6.dp))

            Text(
                "Installment Schedule",
                style = AppFont.ibmPlexSans(12, FontWeight.SemiBold),
                color = AppColors.primaryBrand
            )

            Spacer(Modifier.weight(1f))

            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = AppColors.primaryBrand
            )
        }

        // MARK: Expanded installment list

        AnimatedVisibility(visible = expanded) {

            Column {

                HorizontalDivider(color = AppColors.secondaryText.copy(alpha = 0.12f))

                Spacer(Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = AppColors.surface,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    memberLoan.installments.forEachIndexed { i, installment ->

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Column(modifier = Modifier.weight(1f)) {

                                Text(
                                    installment.installmentNumber,
                                    style = AppFont.ibmPlexSans(12, FontWeight.SemiBold),
                                    color = AppColors.headerText
                                )

                                installment.dueDate?.toDate()?.let { date ->
                                    Text(
                                        CommonFunctions.dateToString(date),
                                        style = AppFont.ibmPlexSans(10, FontWeight.Normal),
                                        color = AppColors.secondaryText
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {

                                Text(
                                    installment.installmentAmount.currencyFormattedWithCommas(),
                                    style = AppFont.ibmPlexSans(12, FontWeight.Bold),
                                    color = AppColors.headerText
                                )

                                Text(
                                    "Interest ${installment.interestAmount.currencyFormattedWithCommas()}",
                                    style = AppFont.ibmPlexSans(10, FontWeight.Normal),
                                    color = AppColors.secondaryText
                                )
                            }
                        }

                        if (i != memberLoan.installments.lastIndex) {
                            HorizontalDivider(color = AppColors.secondaryText.copy(alpha = 0.12f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoView(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = AppFont.ibmPlexSans(size = 10, weight = FontWeight.Medium),
            color = AppColors.secondaryText
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = AppFont.ibmPlexSans(size = 12, weight = FontWeight.Bold),
            color = AppColors.headerText
        )
    }
}

@Composable
fun CashRequestButton(
    pendingCount: Int,
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.surface)
            .border(
                width = 1.dp,
                color = AppColors.border.copy(alpha = .4f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(AppColors.primaryBrand.copy(alpha = .12f)),
            contentAlignment = Alignment.Center
        ) {

            Icon(

                painter = painterResource(R.drawable.cash_request),

                contentDescription = "Cash Requests",

                tint = AppColors.primaryBrand,

                modifier = Modifier.size(18.dp)

            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = "Cash Requests",
                style = AppFont.ibmPlexSans(
                    14,
                    FontWeight.SemiBold
                ),
                color = AppColors.headerText
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text =
                    if (UserDefaultsManager.getSquadManagerLogged()) {
                        if (pendingCount > 0)
                            "$pendingCount Pending"
                        else
                            "No Pending"
                    } else {
                        "Squad Cash Requests"
                    },
                style = AppFont.ibmPlexSans(
                    11,
                    FontWeight.Normal
                ),
                color =
                    if (UserDefaultsManager.getSquadManagerLogged() && pendingCount > 0)
                        AppColors.errorAccent
                    else
                        AppColors.secondaryText
            )
        }

        if (pendingCount > 0) {

            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(
                        AppColors.errorAccent,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = pendingCount.toString(),
                    style = AppFont.ibmPlexSans(
                        11,
                        FontWeight.Bold
                    ),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(10.dp))
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = AppColors.secondaryText,
            modifier = Modifier.size(12.dp)
        )
    }
}