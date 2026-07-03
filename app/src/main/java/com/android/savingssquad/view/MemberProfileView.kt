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
import com.android.savingssquad.model.Squad
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.android.savingssquad.singleton.AmountEditType
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.viewmodel.AppDestination
import com.android.savingssquad.viewmodel.SSToast
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
    val showUpdatePhoneNumberPopup by squadViewModel.showUpdateMemberPopup.collectAsStateWithLifecycle()


    val showEditAmountPopup by squadViewModel.showEditAmountPopup.collectAsStateWithLifecycle()

    var selectedEditAmount by remember { mutableStateOf(0) }
    var openLoanDetailsView by remember { mutableStateOf(false) }

    var openContributionDetails by remember { mutableStateOf(false) }
    val screenType = if (UserDefaultsManager.getSquadManagerLogged()) {
        SquadUserType.SQUAD_MANAGER
    } else {
        SquadUserType.SQUAD_MEMBER
    }

// Get the member depending on the screen type
    val member by if (screenType == SquadUserType.SQUAD_MANAGER) {
        squadViewModel.selectedMember.collectAsStateWithLifecycle()
    } else {
        squadViewModel.currentMember.collectAsStateWithLifecycle()
    }


    LaunchedEffect(openContributionDetails) {
        if (openContributionDetails) {
            navController.navigate(AppDestination.OPEN_CONTRUBUTION_DETAILS.route)
            openContributionDetails = false
        }
    }

    LaunchedEffect(openLoanDetailsView) {
        if (openLoanDetailsView) {
            navController.navigate(AppDestination.OPEN_LOAD_DETAILS.route)
            openLoanDetailsView = false
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
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            SSNavigationBar(
                title = if (screenType == SquadUserType.SQUAD_MANAGER)
                    SquadStrings.memberProfile
                else
                    SquadStrings.yourProfile,
                navController = navController
            )

            member?.let { safeMember ->

                val stats: List<MemberStatItem> =
                    if (screenType == SquadUserType.SQUAD_MANAGER) {
                        listOf(
                            MemberStatItem(
                                title = "Contribution Received",
                                value = safeMember.totalContributionPaid.currencyFormattedWithCommas(),
                                icon = Icons.Default.ArrowDownward,
                                color = Color.Green,
                                onClick = { openContributionDetails = true },
                                onEditClick = {
                                    squadViewModel.setShowEditAmountPopup(true)
                                    selectedEditAmount = safeMember.totalContributionPaid
                                    squadViewModel.setEditAmountType(AmountEditType.contribution)
                                }
                            ),
                            MemberStatItem(
                                title = "Total Loan Borrowed",
                                value = safeMember.totalLoanBorrowed.currencyFormattedWithCommas(),
                                icon = Icons.Default.ArrowUpward,
                                color = Color.Red,
                                onClick = { openLoanDetailsView = true },
                                onEditClick = {
                                    squadViewModel.setShowEditAmountPopup(true)
                                    selectedEditAmount = safeMember.totalLoanBorrowed
                                    squadViewModel.setEditAmountType(AmountEditType.loanBorrowed)
                                }
                            ),
                            MemberStatItem(
                                title = "Paid Loan Amount",
                                value = safeMember.totalLoanPaid.currencyFormattedWithCommas(),
                                icon = Icons.Default.CheckCircle,
                                color = Color.Blue,
                                onClick = { openLoanDetailsView = true },
                                onEditClick = {
                                    squadViewModel.setShowEditAmountPopup(true)
                                    selectedEditAmount = safeMember.totalLoanPaid
                                    squadViewModel.setEditAmountType(AmountEditType.paidLoadAmount)
                                }
                            ),
                            MemberStatItem(
                                title = "Interest Received",
                                value = safeMember.totalInterestPaid.currencyFormattedWithCommas(),
                                icon = Icons.Default.Percent,
                                color = Color(0xFF7B61FF),
                                onEditClick = {
                                    squadViewModel.setShowEditAmountPopup(true)
                                    selectedEditAmount = safeMember.totalInterestPaid
                                    squadViewModel.setEditAmountType(AmountEditType.intrestAmount)
                                }
                            )
                        )
                    } else {
                        listOf(
                            MemberStatItem(
                                title = "Contribution Sent",
                                value = safeMember.totalContributionPaid.currencyFormattedWithCommas(),
                                icon = Icons.Default.ArrowDownward,
                                color = Color.Green,
                                onClick = { openContributionDetails = true }
                            ),
                            MemberStatItem(
                                title = "Total Loan Borrowed",
                                value = safeMember.totalLoanBorrowed.currencyFormattedWithCommas(),
                                icon = Icons.Default.ArrowUpward,
                                color = Color.Red,
                                onClick = { openLoanDetailsView = true }
                            ),
                            MemberStatItem(
                                title = "Paid Loan Amount",
                                value = safeMember.totalLoanPaid.currencyFormattedWithCommas(),
                                icon = Icons.Default.CheckCircle,
                                color = Color.Blue,
                                onClick = { openLoanDetailsView = true }
                            ),
                            MemberStatItem(
                                title = "Interest Sent",
                                value = safeMember.totalInterestPaid.currencyFormattedWithCommas(),
                                icon = Icons.Default.Percent,
                                color = Color(0xFF7B61FF)
                            )
                        )
                    }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {

                    item {
                        MemberProfileHeaderView(
                            member = safeMember,
                            screenType = SquadUserType.SQUAD_MEMBER,
                            squadViewModel = squadViewModel,
                            onUpdatePhone = {
                                squadViewModel.setShowUpdateMemberPopup(true)
                            },
                            onUpdateUPI = {
                                navController.navigate(AppDestination.OPEN_BANK_DETAILS.route)
                            }
                        )
                    }

                    items(stats) { item ->
                        MemberProfileStatsCard(
                            title = item.title,
                            value = item.value,
                            icon = item.icon,
                            color = item.color,
                            onClick = item.onClick,
                            onEditClick = item.onEditClick
                        )
                    }
                }
            }
        }

        // Update member popup (Overlay style)
        if (showUpdatePhoneNumberPopup) {
            OverlayBackgroundView(
                showPopup = remember { mutableStateOf(showUpdatePhoneNumberPopup) },
                onDismiss = { squadViewModel.setShowUpdateMemberPopup(false) }
            ) {
                UpdateMemberPopup(
                    navController = navController,
                    squadViewModel = squadViewModel,
                    onCancel = {
                        squadViewModel.setShowUpdateMemberPopup(false)
                    }
                )
            }
        }

        if (showEditAmountPopup) {
            OverlayBackgroundView(
                showPopup = remember { mutableStateOf(showEditAmountPopup) },
                onDismiss = { squadViewModel.setShowEditAmountPopup(false) }
            ) {

                EditAmountPopup(

                    phoneNumber = member!!.phoneNumber,

                    currentAmount = selectedEditAmount,

                    onDismiss = {

                        squadViewModel.setShowEditAmountPopup(false)

                    }

                ) { newAmount ->

                    println("Updated Amount: $newAmount")

                    LoaderManager.shared.showLoader()

                    squadViewModel.updateMemberAmount(
                        showLoader = true,
                        squadID = squadViewModel.squad.value?.squadID ?: "",
                        memberID = member!!.id ?: "",
                        amount = newAmount,
                        editAmountType = squadViewModel.editAmountType.value ?: AmountEditType.others
                    ) { success, error ->

                        LoaderManager.shared.hideLoader()

                        if (success) {

                            // Success

                        } else {

                            println(error ?: "Unknown error")
                        }
                    }

                }
            }
        }
    }
}

// ---------------------------
// MemberProfileHeaderView
// ---------------------------
@Composable
fun MemberProfileHeaderView(
    member: Member,
    screenType: SquadUserType,
    squadViewModel: SquadViewModel,
    onUpdatePhone: () -> Unit,
    onUpdateUPI: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.surface)
            .border(
                1.dp,
                AppColors.border,
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Avatar
        Box(
            modifier = Modifier.size(88.dp),
            contentAlignment = Alignment.Center
        ) {

            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                AppColors.primaryButton.copy(alpha = 0.18f),
                                AppColors.primaryButton.copy(alpha = 0.06f)
                            )
                        ),
                        CircleShape
                    )
            )

            Box(
                modifier = Modifier
                    .size(70.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                AppColors.primaryButton,
                                AppColors.primaryButton.copy(alpha = 0.82f)
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = member.name.take(1).uppercase(),
                    style = AppFont.ibmPlexSans(
                        size = 24,
                        weight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(14.dp)
                    .background(Color(0xFF22C55E), CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = member.name,
            style = AppFont.ibmPlexSans(
                size = 19,
                weight = FontWeight.Bold
            ),
            color = AppColors.headerText
        )

        Text(
            text = "Squad Member",
            style = AppFont.ibmPlexSans(
                size = 12,
                weight = FontWeight.Medium
            ),
            color = AppColors.secondaryText
        )

        Spacer(Modifier.height(10.dp))

        SSBadge(
            title = "Member ID",
            value = member.id ?: "-",
            icon = "👤",
            style = BadgeStyle.INFO
        )

        Spacer(Modifier.height(12.dp))

        HorizontalDivider(color = AppColors.border)

        ProfileInfoRow(
            icon = Icons.Default.Phone,
            title = "Phone",
            value = member.phoneNumber,
            buttonTitle = "Edit",
            showButton = screenType == SquadUserType.SQUAD_MEMBER,
            onClick = onUpdatePhone
        )

        if (screenType == SquadUserType.SQUAD_MEMBER) {

            HorizontalDivider(color = AppColors.border)

            ProfileInfoRow(
                icon = Icons.Default.QrCode,
                title = "UPI ID",
                value = if (member.upiID.isBlank()) "Not Added" else member.upiID,
                buttonTitle = if (member.upiID.isBlank()) "Add" else "Update",
                showButton = true,
                onClick = onUpdateUPI
            )
        }
    }
}

@Composable
fun ProfileInfoRow(
    icon: ImageVector,
    title: String,
    value: String,
    buttonTitle: String,
    showButton: Boolean,
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.primaryButton,
            modifier = Modifier.size(18.dp)
        )

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                title,
                style = AppFont.ibmPlexSans(
                    size = 12,
                    weight = FontWeight.Medium
                ),
                color = AppColors.secondaryText
            )

            Spacer(Modifier.height(2.dp))

            Text(
                value,
                style = AppFont.ibmPlexSans(
                    size = 15,
                    weight = FontWeight.SemiBold
                ),
                color = AppColors.headerText
            )
        }

        if (showButton) {

            TextButton(
                onClick = onClick,
                shape = RoundedCornerShape(50)
            ) {

                Text(
                    buttonTitle,
                    style = AppFont.ibmPlexSans(
                        size = 13,
                        weight = FontWeight.SemiBold
                    ),
                    color = AppColors.primaryButton
                )
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
    onClick: () -> Unit = {},
    onEditClick: (() -> Unit)? = null
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp)
            )
            .background(AppColors.surface)
            .border(
                1.dp,
                AppColors.border.copy(alpha = 0.6f),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Icon
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Details
        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                style = AppFont.ibmPlexSans(
                    size = 12,
                    weight = FontWeight.Medium
                ),
                color = AppColors.secondaryText
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = value,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = AppFont.ibmPlexSans(
                    size = 17,
                    weight = FontWeight.Bold
                ),
                color = AppColors.headerText
            )
        }

        if (onEditClick != null) {

            IconButton(
                onClick = onEditClick
            ) {

                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = AppColors.primaryButton
                )
            }

        } else {

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
@Composable
fun UpdateMemberPopup(
    navController: NavController,
    squadViewModel: SquadViewModel,
    onCancel: () -> Unit
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

    val coroutineScope = rememberCoroutineScope()
    var debounceJob by remember { mutableStateOf<Job?>(null) }

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

    fun handlePhoneNameChange(
        newValue: String,
        setError: (String) -> Unit,
        squadMembers: List<Member>,   // list from ViewModel
        coroutineScope: CoroutineScope,
        debounceJob: Job?,
        onUpdateDebounceJob: (Job?) -> Unit
    )
    {
        // Clear error immediately (like Swift)
        setError("")

        // Cancel previous debounce job
        debounceJob?.cancel()

        // Create new debounce job
        val newJob = coroutineScope.launch {
            delay(500)

            val cleanedName = CommonFunctions.cleanUpPhoneNumber(newValue)

            val exists = squadMembers
                .map { it.phoneNumber.trim().lowercase() }
                .contains(cleanedName.trim().lowercase())

            if (exists) {
                setError("Mobile name already exists")
            }
        }

        // Update external reference
        onUpdateDebounceJob(newJob)
    }

    fun handleUpdateClick(navController: NavController) {
        if (!validateFields()) return

        if (otpVerified) {
            val phone = CommonFunctions.cleanUpPhoneNumber(phoneNumber)
            val squad = squadViewModel.squad.value
            val memberId = squadViewModel.currentMember.value?.id

            if (squad != null && memberId != null) {
                LoaderManager.shared.showLoader()

                squadViewModel.updateMemberMobileNumber(
                    showLoader = true,
                    squadID = squad.squadID,
                    memberID = memberId,
                    mobileNumber = phone
                ) { success, error ->
                    LoaderManager.shared.hideLoader()
                    if (success) {
                        squadViewModel.setShowUpdateMemberPopup(false)

                        AlertManager.shared.showAlert(
                            title = "Mobile Number Updated",
                            message = "Your mobile number has been updated successfully. For security reasons, you have been signed out. Please sign in again using your new mobile number to continue.",
                            primaryButtonTitle = SquadStrings.ok,
                            primaryAction = {
                                squadViewModel.logoutUser(navController = navController)
                            }
                        )


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
                    handlePhoneNameChange(
                        newValue = phoneState.value,
                        setError = { phoneError = it },
                        squadMembers = squadViewModel.squadMembers.value,
                        coroutineScope = coroutineScope,
                        debounceJob = debounceJob,
                        onUpdateDebounceJob = { debounceJob = it }
                    )
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
                action = { handleUpdateClick(navController = navController) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Cancel Button
            SSCancelButton(
                title = SquadStrings.cancel,
                action = { squadViewModel.setShowUpdateMemberPopup(false) }
            )
        }
    }
}

data class MemberStatItem(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit = {},
    val onEditClick: (() -> Unit)? = null
)