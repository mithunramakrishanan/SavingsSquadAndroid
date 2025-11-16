package com.android.savingssquad.view

import android.app.Activity
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.android.savingssquad.R
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.viewmodel.LoaderManager
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import kotlinx.coroutines.launch
import java.util.Date
import com.android.savingssquad.model.GroupFund
import com.android.savingssquad.model.Member
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.singleton.appShadow
import com.android.savingssquad.singleton.asTimestamp
import com.android.savingssquad.singleton.currencyFormattedWithCommas
import com.android.savingssquad.viewmodel.AlertManager
import com.yourapp.utils.CommonFunctions
import java.util.Calendar
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.android.savingssquad.singleton.GroupFundUserType
import com.android.savingssquad.viewmodel.AppDestination
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

// ---------------------------
// MemberProfileScreen (root)
// ---------------------------
@Composable
fun MemberProfileView(
    navController: NavController,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager = LoaderManager.shared
) {
    val scope = rememberCoroutineScope()
    val showPopup by squadViewModel.showUpdateMemberPopup.collectAsStateWithLifecycle()

    var openContributionDetails by remember { mutableStateOf(false) }
    var openLoanDetailsView by remember { mutableStateOf(false) }

    val member by squadViewModel.selectedMember.collectAsStateWithLifecycle()

    val screenType =
        if (UserDefaultsManager.getGroupFundManagerLogged())
            GroupFundUserType.GROUP_FUND_MANAGER
        else
            GroupFundUserType.GROUP_FUND_MEMBER


    LaunchedEffect(openContributionDetails) {
        if (openContributionDetails) {
            navController.navigate(AppDestination.OPEN_CONTRUBUTION_DETAILS.route)
            openContributionDetails = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackgroundGradient()

        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            SSNavigationBar(title = if (screenType == GroupFundUserType.GROUP_FUND_MANAGER) "Member Profile" else "Your Profile",navController)

            member?.let { safeMember ->
                MemberProfileHeaderView(
                    member = safeMember,
                    screenType = screenType,
                    squadViewModel = squadViewModel,
                    navController = navController
                )

                if (screenType == GroupFundUserType.GROUP_FUND_MEMBER) {
                    ActionButton(
                        title = SquadStrings.manageBankDetails,
                        caption = "Update UPI for seamless transactions"
                    ) {
                        navController.navigate("bank_details_screen")
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {

                    // -------------------------
                    // 1️⃣ Stats List (SwiftUI Equivalent)
                    // -------------------------
                    val stats: List<MemberStatItem> = if (screenType == GroupFundUserType.GROUP_FUND_MANAGER) {
                        listOf(
                            MemberStatItem(
                                title = "Contribution Received",
                                value = member!!.totalContributionPaid.currencyFormattedWithCommas(),
                                icon = Icons.Default.ArrowDownward,
                                color = Color.Green,
                                onClick = { openContributionDetails = true }
                            ),
                            MemberStatItem(
                                title = "Total Loan Borrowed",
                                value = member!!.totalLoanBorrowed.currencyFormattedWithCommas(),
                                icon = Icons.Default.ArrowUpward,
                                color = Color.Red,
                                onClick = { openLoanDetailsView = true }
                            ),
                            MemberStatItem(
                                title = "Paid Loan Amount",
                                value = member!!.totalLoanPaid.currencyFormattedWithCommas(),
                                icon = Icons.Default.CheckCircle,
                                color = Color.Blue,
                                onClick = { openLoanDetailsView = true }
                            ),
                            MemberStatItem(
                                title = "Interest Received",
                                value = member!!.totalInterestPaid.currencyFormattedWithCommas(),
                                icon = Icons.Default.Percent,
                                color = Color(0xFF7B61FF)
                            )
                        )
                    } else {
                        listOf(
                            MemberStatItem(
                                title = "Contribution Sent",
                                value = member!!.totalContributionPaid.currencyFormattedWithCommas(),
                                icon = Icons.Default.ArrowDownward,
                                color = Color.Green,
                                onClick = { openContributionDetails = true }
                            ),
                            MemberStatItem(
                                title = "Total Loan Borrowed",
                                value = member!!.totalLoanBorrowed.currencyFormattedWithCommas(),
                                icon = Icons.Default.ArrowUpward,
                                color = Color.Red,
                                onClick = { openLoanDetailsView = true }
                            ),
                            MemberStatItem(
                                title = "Paid Loan Amount",
                                value = member!!.totalLoanPaid.currencyFormattedWithCommas(),
                                icon = Icons.Default.CheckCircle,
                                color = Color.Blue,
                                onClick = { openLoanDetailsView = true }
                            ),
                            MemberStatItem(
                                title = "Interest Sent",
                                value = member!!.totalInterestPaid.currencyFormattedWithCommas(),
                                icon = Icons.Default.Percent,
                                color = Color(0xFF7B61FF)
                            )
                        )
                    }

                    // -------------------------
                    // 2️⃣ Scrollable Stats List — EXACT SwiftUI Equivalent
                    // -------------------------
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                    ) {
                        items(stats) { item ->
                            MemberProfileStatsCard(
                                title = item.title,
                                value = item.value,
                                icon = item.icon,
                                color = item.color,
                                onClick = item.onClick
                            )
                        }
                    }
                }

            }


        }

        // Update member popup (Overlay style)
        if (showPopup) {
            OverlayBackgroundView(
                showPopup = remember { mutableStateOf(showPopup) },
                onDismiss = { squadViewModel.setShowUpdateMemberPopup(false) }
            ) {
                UpdateMemberPopup(
                    squadViewModel = squadViewModel,
                    showPopup = remember { mutableStateOf(showPopup) }
                )
            }
        }

        // Loader
        if (loaderManager.isLoading) {
            SSLoaderView()
        }
    }
}

// ---------------------------
// MemberProfileHeaderView
// ---------------------------
@Composable
fun MemberProfileHeaderView(
    member: Member,
    screenType: GroupFundUserType,
    squadViewModel: SquadViewModel,
    navController: NavController
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(15.dp))
            .appShadow(AppShadows.card, RoundedCornerShape(15.dp))
            .background(AppColors.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(AppColors.primaryButton.copy(alpha = 0.3f), AppColors.primaryButton.copy(alpha = 0.1f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.name.takeIf { it.isNotEmpty() }?.first().toString(),
                    style = AppFont.ibmPlexSans(28, FontWeight.Bold),
                    color = AppColors.primaryButton
                )
            }

            Text(
                text = member.name,
                style = AppFont.ibmPlexSans(20, FontWeight.SemiBold),
                color = AppColors.headerText
            )

            Text(
                text = "ID: ${member.id ?: "-"}",
                style = AppFont.ibmPlexSans(13, FontWeight.Normal),
                color = AppColors.secondaryText
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = AppColors.secondaryText.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = member.phoneNumber, style = AppFont.ibmPlexSans(14, FontWeight.Normal), color = AppColors.secondaryText, modifier = Modifier.widthIn(max = 140.dp))
                if (screenType == GroupFundUserType.GROUP_FUND_MEMBER) {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = { squadViewModel.setShowUpdateMemberPopup(true) }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "edit", tint = AppColors.primaryButton)
                    }
                }
            }
        }
    }
}

// ---------------------------
// MemberProfileStatsCard
// ---------------------------
@Composable
fun MemberProfileStatsCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(15.dp))
            .appShadow(AppShadows.card, RoundedCornerShape(15.dp))
            .background(AppColors.surface)
            .border(0.5.dp, AppColors.border, RoundedCornerShape(15.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = AppFont.ibmPlexSans(14, FontWeight.Normal),
                color = AppColors.secondaryText
            )
            Text(
                text = value,
                style = AppFont.ibmPlexSans(18, FontWeight.SemiBold),
                color = AppColors.headerText
            )
        }
    }
}

@Composable
fun UpdateMemberPopup(
    squadViewModel: SquadViewModel,
    showPopup: MutableState<Boolean>
) {
    val context = LocalContext.current

    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var verificationID by remember { mutableStateOf("") }

    var sendOTPLoading by remember { mutableStateOf(false) }
    var verifyOTPLoading by remember { mutableStateOf(false) }
    var isOTPSent by remember { mutableStateOf(false) }
    var otpVerified by remember { mutableStateOf(false) }
    var otpProcessStarted by remember { mutableStateOf(false) }

    var phoneError by remember { mutableStateOf("") }
    var sendOTPError by remember { mutableStateOf("") }
    var verifyOTPError by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current

    fun validateFields(): Boolean {
        phoneError =
            if (Regex("^[0-9]{10}$").matches(phoneNumber)) "" else "Enter a valid 10-digit phone number"
        return phoneError.isEmpty()
    }

    fun verifyOTP() {
        val credential = PhoneAuthProvider.getCredential(verificationID, otpCode)

        verifyOTPLoading = true
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                verifyOTPLoading = false
                if (task.isSuccessful) {
                    otpVerified = true
                } else {
                    verifyOTPError = task.exception?.message ?: "Invalid OTP"
                }
            }
    }

    fun handleOTPChange(value: String) {
        verifyOTPError = ""
        if (value.length == 6) {
            verifyOTP()
        }
    }

    fun sendOTP() {
        val phoneWithCode = "+91$phoneNumber"
        sendOTPLoading = true
        otpProcessStarted = true

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            phoneWithCode,
            60,
            TimeUnit.SECONDS,
            (context as Activity),
            object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {}

                override fun onVerificationFailed(e: FirebaseException) {
                    sendOTPLoading = false
                    otpProcessStarted = false
                    sendOTPError = e.localizedMessage ?: "OTP failed"
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    sendOTPLoading = false
                    otpProcessStarted = false
                    verificationID = id
                    isOTPSent = true
                }
            }
        )
    }

    fun handleUpdateClick() {
        if (!validateFields()) return

        if (otpVerified) {
            val phone = CommonFunctions.cleanUpPhoneNumber(phoneNumber)
            val groupFund = squadViewModel.groupFund.value
            val memberId = squadViewModel.currentMember.value?.id

            if (groupFund != null && memberId != null) {
                LoaderManager.shared.showLoader()

                squadViewModel.updateMemberMobileNumber(
                    showLoader = true,
                    groupFundID = groupFund.groupFundID,
                    memberID = memberId,
                    mobileNumber = phone
                ) { success, error ->
                    LoaderManager.shared.hideLoader()
                    if (success) {
                        showPopup.value = false
                    } else {
                        println(error ?: "Unknown error")
                    }
                }
            }
        } else {
            sendOTP()
        }
    }

    // ----------------------------------------------------
    //  UI
    // ----------------------------------------------------

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        OverlayBackgroundView(showPopup) { }

        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .background(AppColors.background, RoundedCornerShape(20.dp))
                .appShadow(AppShadows.elevated, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {

            // Title
            Text(
                text = "Update",
                style = AppFont.ibmPlexSans(20, FontWeight.Bold),
                color = AppColors.headerText,
                modifier = Modifier.padding(top = 10.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Input Fields
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Phone Field
                val phoneState = remember { mutableStateOf("") }

                SSTextField(
                    icon = Icons.Default.Phone,
                    placeholder = "Mobile.No",
                    textState = phoneState,
                    keyboardType = KeyboardType.Number,
                    showDropdown = sendOTPLoading,
                    isLoading = sendOTPLoading,
                    error = phoneError
                )

                LaunchedEffect(phoneState.value) {
                    phoneNumber = phoneState.value
                }

                val otpState = remember { mutableStateOf("") }

                if (isOTPSent) {
                    SSTextField(
                        icon = Icons.Default.Pin,
                        placeholder = "Enter OTP",
                        textState = otpState,
                        keyboardType = KeyboardType.Number,
                        showDropdown = verifyOTPLoading || otpVerified,
                        dropdownIcon = Icons.Default.CheckCircle,
                        dropdownColor = AppColors.primaryButton,
                        isLoading = verifyOTPLoading,
                        error = verifyOTPError
                    )

                    LaunchedEffect(otpState.value) {
                        otpCode = otpState.value
                        if (otpCode.length == 6) handleOTPChange(otpCode)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Button
            SSButton(
                title = if (otpVerified) "Update Mobile" else "Send OTP",
                isDisabled = otpProcessStarted,
                action = { handleUpdateClick() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Cancel Button
            SSCancelButton(
                title = "Cancel",
                action = { showPopup.value = false }
            )
        }
    }
}

data class MemberStatItem(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit = {}
)