package com.android.savingssquad.view

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

import com.android.savingssquad.model.*
import com.android.savingssquad.singleton.*
import com.android.savingssquad.SquadSubscription.SubscriptionFirebaseManager
import com.android.savingssquad.viewmodel.AlertManager
import com.android.savingssquad.viewmodel.AppDestination
import com.android.savingssquad.viewmodel.FirestoreManager
import com.android.savingssquad.viewmodel.SquadViewModel

import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider

import com.yourapp.utils.CommonFunctions
import com.yourapp.utils.IDGenerator

import java.util.Date
import java.util.concurrent.TimeUnit

import kotlinx.coroutines.launch


@Composable
fun SquadSignUpView(
    navController: NavController,
    squadViewModel: SquadViewModel
) {

    val context = LocalContext.current
    val activity = context as? Activity
    val appContext = context.applicationContext

    val coroutineScope = rememberCoroutineScope()

    // ---------------------------------------------------------
    // FORM VALUES
    // ---------------------------------------------------------

    var squadName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var otpCode by rememberSaveable { mutableStateOf("") }

    var totalMonths by rememberSaveable { mutableStateOf("") }
    var squadAmount by rememberSaveable { mutableStateOf("") }
    var squadStartAmount by rememberSaveable { mutableStateOf("") }

    var verificationID by rememberSaveable { mutableStateOf("") }

    // Firebase UID of the verified Manager
    var firebaseUID by rememberSaveable { mutableStateOf("") }


    // ---------------------------------------------------------
    // ERRORS
    // ---------------------------------------------------------

    var squadNameError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf("") }

    var squadMonthError by remember { mutableStateOf("") }
    var squadAmountError by remember { mutableStateOf("") }
    var squadStartAmountError by remember { mutableStateOf("") }

    var sendOTPError by remember { mutableStateOf("") }
    var verifyOTPError by remember { mutableStateOf("") }


    // ---------------------------------------------------------
    // OTP STATES
    // ---------------------------------------------------------

    var isOTPSent by rememberSaveable { mutableStateOf(false) }
    var OTPVerified by rememberSaveable { mutableStateOf(false) }

    // Prevent duplicate OTP sending / verification
    var OTPProcessStarted by rememberSaveable { mutableStateOf(false) }

    var isTermsAccepted by rememberSaveable { mutableStateOf(false) }


    // ---------------------------------------------------------
    // BUTTON STATES
    // ---------------------------------------------------------

    var isButtonLoading by remember { mutableStateOf(false) }


    // ---------------------------------------------------------
    // TERMS & CONDITIONS RESULT
    // ---------------------------------------------------------

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {

        navController.currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Boolean>("termsAccepted")
            ?.observe(lifecycleOwner) { accepted ->

                isTermsAccepted = accepted == true
            }
    }


    // ---------------------------------------------------------
    // MAIN UI
    // ---------------------------------------------------------

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {

        AppBackgroundGradient()

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // -------------------------------------------------
            // HEADER
            // -------------------------------------------------

            SSNavigationBar(
                title = SquadStrings.signUp,
                navController = navController,
                showBackButton = true
            )


            // -------------------------------------------------
            // DESCRIPTION
            // -------------------------------------------------

            Box(
                modifier = Modifier
                    .padding(horizontal = 30.dp)
                    .background(
                        color = AppColors.successAccent.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = AppColors.successAccent.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {

                Text(
                    text = SquadStrings.signUpDescription,
                    style = AppFont.ibmPlexSans(
                        15,
                        FontWeight.Medium
                    ),
                    color = AppColors.headerText,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }


            // -------------------------------------------------
            // FORM
            // -------------------------------------------------

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp)
            ) {

                Spacer(modifier = Modifier.height(8.dp))


                // SQUAD NAME

                val squadNameState = remember {
                    mutableStateOf(squadName)
                }

                LaunchedEffect(squadNameState.value) {
                    squadName = squadNameState.value
                }

                SSTextField(
                    icon = Icons.Default.Group,
                    placeholder = SquadStrings.squadName,
                    textState = squadNameState,
                    keyboardType = KeyboardType.Text,
                    error = squadNameError
                )


                Spacer(modifier = Modifier.height(12.dp))


                // EMAIL

                val emailState = remember {
                    mutableStateOf(email)
                }

                LaunchedEffect(emailState.value) {
                    email = emailState.value
                }

                SSTextField(
                    icon = Icons.Default.Email,
                    placeholder = SquadStrings.email,
                    textState = emailState,
                    keyboardType = KeyboardType.Email,
                    error = emailError
                )


                Spacer(modifier = Modifier.height(12.dp))


                // PHONE NUMBER

                val phoneState = remember {
                    mutableStateOf(phoneNumber)
                }

                LaunchedEffect(phoneState.value) {

                    val updatedPhone = phoneState.value

                    if (updatedPhone != phoneNumber) {

                        phoneNumber = updatedPhone

                        // A changed phone number requires fresh verification.
                        OTPVerified = false
                        firebaseUID = ""
                        verificationID = ""
                        otpCode = ""

                        isOTPSent = false
                        OTPProcessStarted = false

                        verifyOTPError = ""
                        sendOTPError = ""
                    }
                }

                SSTextField(
                    icon = Icons.Default.Phone,
                    placeholder = SquadStrings.phone,
                    textState = phoneState,
                    keyboardType = KeyboardType.Number,
                    error = phoneError +
                            if (sendOTPError.isNotEmpty()) "\n$sendOTPError" else "",
                    maxLength = 10
                )


                Spacer(modifier = Modifier.height(12.dp))


                // -------------------------------------------------
                // OTP FIELD
                // -------------------------------------------------

                if (isOTPSent) {

                    val otpState = remember {
                        mutableStateOf(otpCode)
                    }

                    LaunchedEffect(otpState.value) {

                        otpCode = otpState.value

                        // Automatically verify when six digits are entered.
                        if (
                            otpCode.length == 6 &&
                            !OTPVerified &&
                            !OTPProcessStarted
                        ) {

                            OTPProcessStarted = true

                            squadViewModel.setIsVerifyingOTP(true)

                            verifyOTPError = ""

                            verifyOTP(
                                verificationID = verificationID,
                                otpCode = otpCode,
                                expectedPhoneNumber = "+91$phoneNumber",

                                onSuccess = { verifiedFirebaseUID ->

                                    firebaseUID = verifiedFirebaseUID

                                    OTPVerified = true
                                    OTPProcessStarted = false

                                    verifyOTPError = ""

                                    squadViewModel.setIsVerifyingOTP(false)

                                    Toast.makeText(
                                        context,
                                        "OTP verified successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },

                                onError = { err ->

                                    OTPProcessStarted = false

                                    squadViewModel.setIsVerifyingOTP(false)

                                    verifyOTPError = err
                                }
                            )
                        }
                    }

                    SSTextField(
                        icon = Icons.Default.Numbers,
                        placeholder = SquadStrings.enterOTP,
                        textState = otpState,
                        keyboardType = KeyboardType.Number,
                        dropdownIcon = Icons.Default.CheckCircle,
                        dropdownColor = Color.Blue,
                        error = verifyOTPError
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }


                // -------------------------------------------------
                // TOTAL MONTHS
                // -------------------------------------------------

                val monthsState = remember {
                    mutableStateOf(totalMonths)
                }

                LaunchedEffect(monthsState.value) {
                    totalMonths = monthsState.value
                }

                SSTextField(
                    icon = Icons.Default.CalendarMonth,
                    placeholder = SquadStrings.squadMonths,
                    textState = monthsState,
                    keyboardType = KeyboardType.Number,
                    error = squadMonthError
                )

                val months = totalMonths.toIntOrNull() ?: 0

                Text(
                    text = CommonFunctions.convertMonthsToYearsAndMonths(months),
                    style = AppFont.ibmPlexSans(
                        13,
                        FontWeight.Normal
                    ),
                    color = AppColors.successAccent,
                    modifier = Modifier.padding(
                        start = 24.dp,
                        top = 6.dp
                    )
                )


                Spacer(modifier = Modifier.height(12.dp))


                // -------------------------------------------------
                // SQUAD AMOUNT
                // -------------------------------------------------

                val amountState = remember {
                    mutableStateOf(squadAmount)
                }

                LaunchedEffect(amountState.value) {
                    squadAmount = amountState.value
                }

                SSTextField(
                    icon = Icons.Default.CurrencyRupee,
                    placeholder = SquadStrings.squadAmount,
                    textState = amountState,
                    keyboardType = KeyboardType.Number,
                    error = squadAmountError
                )


                Spacer(modifier = Modifier.height(12.dp))


                // -------------------------------------------------
                // SQUAD START AMOUNT
                // -------------------------------------------------

                /*
                val startAmountState = remember {
                    mutableStateOf(squadStartAmount)
                }

                LaunchedEffect(startAmountState.value) {
                    squadStartAmount = startAmountState.value
                }

                SSTextField(
                    icon = Icons.Default.AccountBalanceWallet,
                    placeholder = SquadStrings.squadStartAmount,
                    textState = startAmountState,
                    keyboardType = KeyboardType.Number,
                    error = squadStartAmountError
                )
                */

                Spacer(modifier = Modifier.height(20.dp))
            }


            // -------------------------------------------------
            // FOOTER
            // -------------------------------------------------

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {

                // TERMS & CONDITIONS

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            AppColors.successAccent.copy(alpha = 0.06f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isTermsAccepted) {
                                AppColors.successAccent.copy(alpha = 0.30f)
                            } else {
                                AppColors.border.copy(alpha = 0.60f)
                            },
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable {

                            if (!isTermsAccepted) {

                                navController.navigate(
                                    AppDestination.OPEN_TERMS_CONDITIONS.route
                                )

                            } else {

                                isTermsAccepted = false
                            }
                        }
                        .padding(
                            horizontal = 16.dp,
                            vertical = 14.dp
                        )
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = if (isTermsAccepted) {
                                Icons.Filled.CheckCircle
                            } else {
                                Icons.Outlined.RadioButtonUnchecked
                            },
                            contentDescription = null,
                            tint = if (isTermsAccepted) {
                                AppColors.successAccent
                            } else {
                                AppColors.secondaryText.copy(alpha = 0.5f)
                            },
                            modifier = Modifier.size(22.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = SquadStrings.byContinuingIAgreeToThe,
                                style = AppFont.ibmPlexSans(
                                    13,
                                    FontWeight.Normal
                                ),
                                color = AppColors.secondaryText
                            )

                            Text(
                                text = SquadStrings.termsConditions,
                                style = AppFont.ibmPlexSans(
                                    14,
                                    FontWeight.SemiBold
                                ),
                                color = AppColors.successAccent
                            )
                        }
                    }
                }


                Spacer(modifier = Modifier.height(12.dp))


                // -------------------------------------------------
                // ADD SQUAD BUTTON
                // -------------------------------------------------

                SSButton(
                    isButtonLoading = isButtonLoading,
                    title = SquadStrings.addSquad,
                    isDisabled = !isTermsAccepted || OTPProcessStarted
                ) {

                    coroutineScope.launch {

                        if (isButtonLoading || OTPProcessStarted) {
                            return@launch
                        }

                        isButtonLoading = true


                        // -------------------------------------------------
                        // VALIDATE FORM
                        // -------------------------------------------------

                        val errs = validateFields(
                            squadName = squadName,
                            email = email,
                            phoneNumber = phoneNumber,
                            totalMonths = totalMonths,
                            squadAmount = squadAmount
                        )

                        squadNameError = ""
                        emailError = ""
                        phoneError = ""
                        squadMonthError = ""
                        squadAmountError = ""

                        if (errs.isNotEmpty()) {

                            squadNameError = errs["squadName"] ?: ""
                            emailError = errs["email"] ?: ""
                            phoneError = errs["phone"] ?: ""
                            squadMonthError = errs["totalMonths"] ?: ""
                            squadAmountError = errs["squadAmount"] ?: ""

                            isButtonLoading = false

                            return@launch
                        }


                        // -------------------------------------------------
                        // STEP 1: SEND OTP
                        // -------------------------------------------------

                        if (!isOTPSent) {

                            val currentActivity = activity

                            if (currentActivity == null) {

                                sendOTPError = "Unable to access Activity."
                                isButtonLoading = false

                                return@launch
                            }

                            OTPProcessStarted = true

                            squadViewModel.setIsSendingOTP(true)

                            sendOTPError = ""
                            verifyOTPError = ""

                            sendOTPSignUp(
                                context = context,
                                phoneNumber = phoneNumber,

                                onCodeSent = { vid ->

                                    verificationID = vid

                                    isOTPSent = true
                                    OTPVerified = false
                                    OTPProcessStarted = false

                                    firebaseUID = ""

                                    otpCode = ""

                                    squadViewModel.setIsSendingOTP(false)

                                    sendOTPError = ""
                                    verifyOTPError = SquadStrings.enterOTP

                                    isButtonLoading = false
                                },

                                onAutoVerified = { verifiedUID ->

                                    firebaseUID = verifiedUID

                                    isOTPSent = true
                                    OTPVerified = true
                                    OTPProcessStarted = false

                                    squadViewModel.setIsSendingOTP(false)

                                    sendOTPError = ""
                                    verifyOTPError = ""

                                    isButtonLoading = false

                                    Toast.makeText(
                                        context,
                                        "OTP verified successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },

                                onError = { err ->

                                    squadViewModel.setIsSendingOTP(false)

                                    sendOTPError = err

                                    OTPProcessStarted = false
                                    OTPVerified = false
                                    firebaseUID = ""

                                    isButtonLoading = false
                                }
                            )

                            return@launch
                        }


                        // -------------------------------------------------
                        // STEP 2: VERIFY OTP
                        // -------------------------------------------------

                        if (isOTPSent && !OTPVerified) {

                            if (otpCode.length != 6) {

                                verifyOTPError = "Enter 6-digit OTP"

                                isButtonLoading = false

                                return@launch
                            }

                            if (OTPProcessStarted) {

                                isButtonLoading = false

                                return@launch
                            }

                            OTPProcessStarted = true

                            squadViewModel.setIsVerifyingOTP(true)

                            verifyOTPError = ""

                            verifyOTP(
                                verificationID = verificationID,
                                otpCode = otpCode,
                                expectedPhoneNumber = "+91$phoneNumber",

                                onSuccess = { verifiedFirebaseUID ->

                                    firebaseUID = verifiedFirebaseUID

                                    OTPVerified = true
                                    OTPProcessStarted = false

                                    verifyOTPError = ""

                                    squadViewModel.setIsVerifyingOTP(false)

                                    isButtonLoading = false
                                },

                                onError = { err ->

                                    OTPProcessStarted = false

                                    squadViewModel.setIsVerifyingOTP(false)

                                    verifyOTPError = err

                                    isButtonLoading = false
                                }
                            )

                            return@launch
                        }


                        // -------------------------------------------------
                        // STEP 3: SAVE SQUAD
                        // -------------------------------------------------

                        if (OTPVerified && firebaseUID.isNotBlank()) {

                            saveSquadData(
                                squadViewModel = squadViewModel,

                                squadName = squadName,
                                email = email,
                                phoneNumber = phoneNumber,

                                totalMonths = totalMonths,
                                squadAmount = squadAmount,
                                squadStartAmount = squadStartAmount,

                                firebaseUID = firebaseUID,

                                activity = activity!!,
                                context = appContext,
                                navController = navController,

                                onComplete = {

                                    isButtonLoading = false
                                },

                                onError = { err ->

                                    isButtonLoading = false
                                    squadAmountError = err
                                }
                            )

                        } else {

                            isButtonLoading = false

                            if (OTPVerified && firebaseUID.isBlank()) {
                                verifyOTPError = "Firebase verification is incomplete. Please verify OTP again."
                            }
                        }
                    }
                }
            }
        }


        // ---------------------------------------------------------
        // OTP LOADING OVERLAY
        // ---------------------------------------------------------

        val isSendingOTP by squadViewModel.isSendingOTP.collectAsStateWithLifecycle()

        val isVerifyingOTP by squadViewModel.isVerifyingOTP.collectAsStateWithLifecycle()

        when {

            isSendingOTP -> {
                OTPOverlayView(
                    state = OTPOverlayState.SENDING
                )
            }

            isVerifyingOTP -> {
                OTPOverlayView(
                    state = OTPOverlayState.VERIFYING
                )
            }
        }
    }
}


// =====================================================================
// FIELD VALIDATION
// =====================================================================

fun validateFields(
    squadName: String,
    email: String,
    phoneNumber: String,
    totalMonths: String,
    squadAmount: String
): Map<String, String> {

    val errors = mutableMapOf<String, String>()

    if (squadName.isBlank()) {
        errors["squadName"] = SquadStrings.nameRequired
    }

    if (!email.contains("@") || !email.contains(".")) {
        errors["email"] = SquadStrings.enterValidEmailAddress
    }

    if (!Regex("^[0-9]{10}$").matches(phoneNumber)) {
        errors["phone"] = SquadStrings.enterValidPhoneNumber
    }

    if (totalMonths.isBlank() || totalMonths.toIntOrNull() == null) {
        errors["totalMonths"] = SquadStrings.squadMonthRequired
    }

    if (squadAmount.isBlank()) {
        errors["squadAmount"] = SquadStrings.squadAmountRequired
    }

    return errors
}


// =====================================================================
// SEND OTP - MANAGER SIGN UP
// Uses DEFAULT Firebase Auth
// =====================================================================

private fun sendOTPSignUp(
    context: Context,
    phoneNumber: String,
    onCodeSent: (String) -> Unit,
    onAutoVerified: (String) -> Unit,
    onError: (String) -> Unit
) {

    val auth = FirebaseAuth.getInstance()

    val activity = context as? Activity

    if (activity == null) {

        onError("Invalid context for sending OTP")

        return
    }

    val expectedPhoneNumber = "+91$phoneNumber"

    val options = PhoneAuthOptions.newBuilder(auth)
        .setPhoneNumber(expectedPhoneNumber)
        .setTimeout(60L, TimeUnit.SECONDS)
        .setActivity(activity)
        .setCallbacks(

            object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(
                    credential: PhoneAuthCredential
                ) {

                    Log.d(
                        "SquadSignUp",
                        "OTP automatically verified"
                    )

                    auth.signInWithCredential(credential)
                        .addOnCompleteListener { task ->

                            if (!task.isSuccessful) {

                                onError(
                                    task.exception?.localizedMessage
                                        ?: "Automatic verification failed."
                                )

                                return@addOnCompleteListener
                            }

                            val user = task.result?.user

                            if (user == null) {

                                onError(
                                    "Unable to retrieve Firebase user."
                                )

                                return@addOnCompleteListener
                            }

                            if (user.phoneNumber != expectedPhoneNumber) {

                                auth.signOut()

                                onError(
                                    "Verified phone number does not match."
                                )

                                return@addOnCompleteListener
                            }

                            Log.d(
                                "SquadSignUp",
                                "Manager Firebase UID: ${user.uid}"
                            )

                            onAutoVerified(user.uid)
                        }
                }


                override fun onVerificationFailed(
                    exception: FirebaseException
                ) {

                    Log.e(
                        "SquadSignUp",
                        "OTP sending failed",
                        exception
                    )

                    onError(
                        exception.localizedMessage
                            ?: "OTP sending failed."
                    )
                }


                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {

                    Log.d(
                        "SquadSignUp",
                        "OTP sent successfully"
                    )

                    onCodeSent(verificationId)
                }
            }
        )
        .build()

    PhoneAuthProvider.verifyPhoneNumber(options)
}


// =====================================================================
// VERIFY OTP - MANAGER SIGN UP
// Uses DEFAULT Firebase Auth
// Returns verified Firebase UID
// =====================================================================

private fun verifyOTP(
    verificationID: String,
    otpCode: String,
    expectedPhoneNumber: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {

    if (verificationID.isBlank()) {

        onError(
            "Invalid verification ID. Please request OTP again."
        )

        return
    }

    if (otpCode.length != 6) {

        onError(
            "Please enter a valid 6-digit OTP."
        )

        return
    }

    val auth = FirebaseAuth.getInstance()

    val credential = PhoneAuthProvider.getCredential(
        verificationID,
        otpCode
    )

    auth.signInWithCredential(credential)
        .addOnCompleteListener { task ->

            if (!task.isSuccessful) {

                onError(
                    task.exception?.localizedMessage
                        ?: SquadStrings.invalidOTP
                )

                return@addOnCompleteListener
            }

            val firebaseUser = task.result?.user

            if (firebaseUser == null) {

                onError(
                    "Unable to retrieve Firebase user."
                )

                return@addOnCompleteListener
            }

            // Validate verified phone number.

            if (firebaseUser.phoneNumber != expectedPhoneNumber) {

                auth.signOut()

                onError(
                    "Verified phone number does not match."
                )

                return@addOnCompleteListener
            }

            val verifiedUID = firebaseUser.uid

            Log.d(
                "SquadSignUp",
                "Manager OTP verified successfully"
            )

            Log.d(
                "SquadSignUp",
                "Firebase UID: $verifiedUID"
            )

            Log.d(
                "SquadSignUp",
                "Verified phone: ${firebaseUser.phoneNumber}"
            )

            onSuccess(verifiedUID)
        }
}


// =====================================================================
// SAVE SQUAD
// Saves Squad + Login + Activity + Subscription atomically
// =====================================================================

private fun saveSquadData(
    squadViewModel: SquadViewModel,
    squadName: String,
    email: String,
    phoneNumber: String,
    totalMonths: String,
    squadAmount: String,
    squadStartAmount: String,
    firebaseUID: String,
    activity: Activity,
    context: Context,
    navController: NavController,
    onComplete: () -> Unit,
    onError: (String) -> Unit
) {

    if (firebaseUID.isBlank()) {

        onError("Firebase UID is missing. Please verify OTP again.")

        return
    }

    LoaderManager.shared.showLoader()

    val squadID = IDGenerator.generateSquadID()

    val squadStartDate = Date()

    val monthsInt = totalMonths.toIntOrNull() ?: 12
    val squadAmountInt = squadAmount.toIntOrNull() ?: 0
    val startAmountInt = squadStartAmount.toIntOrNull() ?: 0


    // ---------------------------------------------------------
    // 1. CREATE SQUAD MODEL
    // ---------------------------------------------------------

    val squad = Squad(

        squadID = squadID,

        squadName = squadName,

        mailID = email,

        countryCode = "+91",

        phoneNumber = phoneNumber,

        virtualAccountNumber = "",
        paymentInstrumentId = "",
        virtualUPI = "",

        squadAccountName = "",
        squadAccountNumber = "",
        squadIFSCCode = "",

        upiBeneId = "",
        bankBeneId = "",
        upiID = "",

        squadStartDate = squadStartDate.asTimestamp,

        squadEndDate = CommonFunctions.getFutureMonthYearDate(
            squadStartDate,
            monthsInt
        )?.asTimestamp ?: squadStartDate.asTimestamp,

        squadCreatedDate = squadStartDate.asTimestamp,

        squadDueDate = CommonFunctions.getEndOfMonthFromDate(
            squadStartDate
        )?.asTimestamp ?: squadStartDate.asTimestamp,

        totalDuration = monthsInt,
        remainingDuration = monthsInt,

        totalMembers = 0,

        monthlyContribution = squadAmountInt,
        squadStartAmount = startAmountInt,

        totalAmount = 0,
        totalContributionAmountReceived = 0,
        totalLoanAmountReceived = 0,
        totalLoanAmountSent = 0,
        totalInterestAmountReceived = 0,
        currentAvailableAmount = 0,

        emiConfiguration = emptyList(),

        recordStatus = RecordStatus.ACTIVE,

        recordDate = Date(),

        password = null,

        showTotalPaymentToMember = false
    )


    // ---------------------------------------------------------
    // 2. CREATE MANAGER LOGIN MODEL
    // ---------------------------------------------------------

    val login = Login(

        firebaseUID = firebaseUID,

        squadID = squadID,

        squadName = squadName,

        memberName = SquadStrings.squadManager,

        memberNameTamil = "",
        memberNameEnglish = "",
        memberNameHindi = "",

        squadUserId = "",

        phoneNumber = phoneNumber,

        role = SquadUserType.SQUAD_MANAGER,

        squadCreatedDate = squadStartDate.asTimestamp,

        userCreatedDate = squadStartDate.asTimestamp
    )


    // ---------------------------------------------------------
    // 3. CREATE SQUAD ACTIVITY
    // ---------------------------------------------------------

    val squadActivity = SquadActivity(

        id = "",

        squadID = squadID,

        activityType = SquadActivityType.AMOUNT_CREDIT,

        memberName = SquadStrings.squadManager,

        memberNameHindi = SquadStringsHindi.squadManager,

        memberNameTamil = SquadStringsTamil.squadManager,

        memberNameEnglish = SquadStringsEnglish.squadManager,

        memberId = "",

        amount = startAmountInt,

        description = SquadStringsEnglishDesc.startedSquadWithAmountOf,

        descriptionTamil = SquadStringsTamilDesc.startedSquadWithAmountOf,

        descriptionHindi = SquadStringsHindiDesc.startedSquadWithAmountOf,

        recordDate = squadStartDate
    )


    // ---------------------------------------------------------
    // 4. PREPARE DEFAULT SUBSCRIPTION
    // ---------------------------------------------------------

    SubscriptionFirebaseManager.shared.prepareDefaultSquadSubscription { result ->

        result.fold(

            onSuccess = { subscription ->

                // -------------------------------------------------
                // 5. ATOMIC FIRESTORE WRITE
                // -------------------------------------------------

                FirestoreManager.shared.createSquadAtomically(

                    squad = squad,

                    login = login,

                    activity = squadActivity,

                    subscription = subscription

                ) { success, error ->

                    activity.runOnUiThread {

                        LoaderManager.shared.hideLoader()

                        if (!success) {

                            val errorMessage = error
                                ?: SquadStrings.genericError

                            AlertManager.shared.showAlert(
                                title = SquadStrings.savingsSquad,
                                message = errorMessage,
                                primaryButtonTitle = SquadStrings.ok
                            )

                            onError(errorMessage)

                            return@runOnUiThread
                        }


                        // -------------------------------------------------
                        // ALL FOUR DOCUMENTS SAVED SUCCESSFULLY
                        // -------------------------------------------------

                        squadViewModel.setSquad(squad)

                        AlertManager.shared.showAlert(

                            title = SquadStrings.savingsSquad,

                            message = SquadStrings.squadCreatedSuccessfully,

                            primaryButtonTitle = SquadStrings.loginTitle,

                            primaryAction = {

                                navController.popBackStack()

                                onComplete()
                            }
                        )
                    }
                }
            },


            onFailure = { exception ->

                activity.runOnUiThread {

                    LoaderManager.shared.hideLoader()

                    val errorMessage = exception.localizedMessage
                        ?: SquadStrings.genericError

                    AlertManager.shared.showAlert(

                        title = SquadStrings.savingsSquad,

                        message = errorMessage,

                        primaryButtonTitle = SquadStrings.ok
                    )

                    onError(errorMessage)
                }
            }
        )
    }
}