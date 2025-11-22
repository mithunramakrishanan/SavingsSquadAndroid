package com.android.savingssquad.view

import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.savingssquad.model.*
import com.android.savingssquad.singleton.*
import com.yourapp.utils.CommonFunctions
import com.android.savingssquad.viewmodel.LoaderManager
import java.util.*
import androidx.navigation.NavController
import com.android.savingssquad.viewmodel.FirestoreManager
import com.android.savingssquad.viewmodel.SquadViewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.android.savingssquad.viewmodel.AlertManager

import com.google.firebase.auth.*

import kotlinx.coroutines.launch


@Composable
fun SquadSignUpView(
    navController: NavController,
    squadViewModel: SquadViewModel, // keep for parity; not used here but present in SwiftUI signature
    loaderManager: LoaderManager = LoaderManager.shared
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // VALUES
    var squadName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var totalMonths by remember { mutableStateOf("") }
    var squadAmount by remember { mutableStateOf("") }
    var squadStartAmount by remember { mutableStateOf("") }
    var verificationID by remember { mutableStateOf("") }

// ERRORS
    var squadNameError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf("") }
    var squadMonthError by remember { mutableStateOf("") }
    var squadAmountError by remember { mutableStateOf("") }
    var squadStartAmountError by remember { mutableStateOf("") }
    var sendOTPError by remember { mutableStateOf("") }
    var verifyOTPError by remember { mutableStateOf("") }

// OTP states
    var isOTPSent by remember { mutableStateOf(false) }
    var OTPVerified by remember { mutableStateOf(false) }
    var OTPProcessStarted by remember { mutableStateOf(false) }
    var sendOTPLoading by remember { mutableStateOf(false) }
    var verifyOTPLoading by remember { mutableStateOf(false) }

// UI Button state
    var isButtonLoading by remember { mutableStateOf(false) }
    var isTermsAccepted by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackgroundGradient()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            SSNavigationBar(
                title = SquadStrings.signUp,
                navController = navController,
                showBackButton = true
            )

            // Description
            Text(
                text = SquadStrings.signUpDescription,
                style = AppFont.ibmPlexSans(15, androidx.compose.ui.text.font.FontWeight.Normal),
                color = AppColors.successAccent,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 30.dp)
            )

            // Form (scrollable)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Squad Name
                SSTextField(
                    icon = Icons.Default.Group,
                    placeholder = SquadStrings.squadName,
                    textState = remember { mutableStateOf(squadName) }.also { state ->
                        // keep two-way mapping
                        LaunchedEffect(state.value) { squadName = state.value }
                    },
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                    error = squadNameError
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Email
                SSTextField(
                    icon = Icons.Default.Email,
                    placeholder = SquadStrings.email,
                    textState = remember { mutableStateOf(email) }.also { state ->
                        LaunchedEffect(state.value) { email = state.value }
                    },
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                    error = emailError
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Phone + send OTP dropdown loader
                SSTextField(
                    icon = Icons.Default.Phone,
                    placeholder = SquadStrings.phone,
                    textState = remember { mutableStateOf(phoneNumber) }.also { s ->
                        LaunchedEffect(s.value) { phoneNumber = s.value }
                    },
                    keyboardType = KeyboardType.Number,
                    showDropdown = sendOTPLoading,
                    isLoading = sendOTPLoading,
                    error = phoneError + if (sendOTPError.isNotEmpty()) "\n$sendOTPError" else ""
                )

                Spacer(modifier = Modifier.height(12.dp))

                // OTP field (if sent)
                if (isOTPSent) {
                    SSTextField(
                        icon = Icons.Default.Numbers,
                        placeholder = SquadStrings.enterOTP,
                        textState = remember { mutableStateOf(otpCode) }.also { state ->
                            LaunchedEffect(state.value) { otpCode = state.value
                                // auto verify when 6 digits entered
                                if (otpCode.length == 6 && !verifyOTPLoading && !OTPVerified) {
                                    // trigger verify with small delay
                                    coroutineScope.launch {
                                        verifyOTPLoading = true
                                        verifyOTP(
                                            context = context,
                                            verificationID = verificationID,
                                            otpCode = otpCode,
                                            onSuccess = {
                                                verifyOTPLoading = false
                                                OTPVerified = true
                                                OTPProcessStarted = false
                                                Toast.makeText(context, "OTP verified", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { err ->
                                                verifyOTPLoading = false
                                                verifyOTPError = err
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        showDropdown = verifyOTPLoading || OTPVerified,
                        dropdownIcon = Icons.Default.CheckCircle,
                        dropdownColor = androidx.compose.ui.graphics.Color.Blue,
                        isLoading = verifyOTPLoading,
                        error = verifyOTPError
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Total Months + small helper text
                SSTextField(
                    icon = Icons.Default.CalendarMonth,
                    placeholder = SquadStrings.squadMonths,
                    textState = remember { mutableStateOf(totalMonths) }.also { state ->
                        LaunchedEffect(state.value) { totalMonths = state.value }
                    },
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    error = squadMonthError
                )

                val months = totalMonths.toIntOrNull() ?: 0
               Text(
                    text = CommonFunctions.convertMonthsToYearsAndMonths(months),
                    style = AppFont.ibmPlexSans(13, androidx.compose.ui.text.font.FontWeight.Normal),
                    color = AppColors.successAccent,
                    modifier = Modifier.padding(start = 24.dp, top = 6.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Squad Amount
                SSTextField(
                    icon = Icons.Default.CurrencyRupee,
                    placeholder = SquadStrings.squadAmount,
                    textState = remember { mutableStateOf(squadAmount) }.also { state ->
                        LaunchedEffect(state.value) { squadAmount = state.value }
                    },
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    error = squadAmountError
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Squad Start Amount
                SSTextField(
                    icon = Icons.Default.AccountBalanceWallet,
                    placeholder = SquadStrings.squadStartAmount,
                    textState = remember { mutableStateOf(squadStartAmount) }.also { state ->
                        LaunchedEffect(state.value) { squadStartAmount = state.value }
                    },
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    error = squadStartAmountError
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Footer (Terms + Button)
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isTermsAccepted,
                        onCheckedChange = { isTermsAccepted = it },
                        colors = CheckboxDefaults.colors(checkedColor = AppColors.successAccent)
                    )

                 Text(
                        text = SquadStrings.agreeToTerms,
                        style = AppFont.ibmPlexSans(14, androidx.compose.ui.text.font.FontWeight.Normal),
                        color = AppColors.successAccent
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                SSButton(
                    isButtonLoading = isButtonLoading,
                    title = SquadStrings.addSquad,
                    isDisabled = !isTermsAccepted || OTPProcessStarted
                ) {
                    coroutineScope.launch {
                        isButtonLoading = true

                        // Validate
                        val errs = validateFields(squadName, email, phoneNumber, totalMonths, squadAmount)

                        // Reset old errors
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

                        // Send OTP if not sent
                        if (!isOTPSent) {
                            OTPProcessStarted = true
                            sendOTPLoading = true
                            sendOTPError = ""

                            sendOTPSignUp(
                                context = context,
                                phoneNumber = phoneNumber,
                                onCodeSent = { vid ->
                                    sendOTPLoading = false
                                    isOTPSent = true
                                    verificationID = vid ?: ""
                                    OTPProcessStarted = false
                                },
                                onError = { err ->
                                    sendOTPLoading = false
                                    sendOTPError = err        // ⬅️ ERROR SHOWN IN TEXTFIELD
                                    OTPProcessStarted = false
                                }
                            )

                            isButtonLoading = false
                            return@launch
                        }

                        // Verify OTP
                        if (isOTPSent && !OTPVerified) {
                            if (otpCode.length != 6) {
                                verifyOTPError = "Enter 6-digit OTP"
                                isButtonLoading = false
                                return@launch
                            }

                            verifyOTPLoading = true
                            verifyOTPError = ""

                            verifyOTP(
                                context = context,
                                verificationID = verificationID,
                                otpCode = otpCode,
                                onSuccess = {
                                    verifyOTPLoading = false
                                    OTPVerified = true
                                },
                                onError = { err ->
                                    verifyOTPLoading = false
                                    verifyOTPError = err      // ⬅️ ERROR SHOWN IN TEXTFIELD
                                    isButtonLoading = false
                                }
                            )

                            return@launch
                        }

                        // Save squad
                        if (OTPVerified) {
                            saveSquadData(
                                squadViewModel = squadViewModel,
                                loaderManager = loaderManager,
                                squadName = squadName,
                                email = email,
                                phoneNumber = phoneNumber,
                                totalMonths = totalMonths,
                                squadAmount = squadAmount,
                                squadStartAmount = squadStartAmount,
                                onComplete = {
                                    isButtonLoading = false
                                    AlertManager.shared.showAlert(
                                        title = SquadStrings.appName,
                                        message = SquadStrings.squadCreatedSuccessfully,
                                        primaryButtonTitle = "OK",
                                        primaryAction = {navController.popBackStack()}
                                    )
                                },
                                onError = { err ->
                                    isButtonLoading = false
                                    squadAmountError = err   // OR show toast if needed
                                }
                            )
                        } else {
                            isButtonLoading = false
                        }
                    }
                }
            }
        }

        SSAlert()
        SSLoaderView()
    }
}

fun validateFields(
    squadName: String,
    email: String,
    phoneNumber: String,
    totalMonths: String,
    squadAmount: String
): Map<String, String> {

    val errors = mutableMapOf<String, String>()

    if (squadName.isBlank()) errors["squadName"] = "Squad Name is required"
    if (!email.contains("@") || !email.contains(".")) errors["email"] = "Enter a valid email"
    if (!Regex("^[0-9]{10}$").matches(phoneNumber)) errors["phone"] = "Enter valid 10-digit phone"
    if (totalMonths.isBlank() || totalMonths.toIntOrNull() == null)
        errors["totalMonths"] = "Enter valid Squad Months"
    if (squadAmount.isBlank())
        errors["squadAmount"] = "Squad Amount is required"

    return errors
}

private fun sendOTPSignUp(
    context: Context,
    phoneNumber: String,
    onCodeSent: (verificationId: String?) -> Unit,
    onError: (String) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val activity = context as? ComponentActivity
    if (activity == null) {
        onError("Invalid context for sending OTP")
        return
    }

    val options = PhoneAuthOptions.newBuilder(auth)
        .setPhoneNumber("+91$phoneNumber")
        .setTimeout(60L, TimeUnit.SECONDS)
        .setActivity(activity)
        .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // auto-verified
                onCodeSent(null) // caller can interpret null as auto-verified
            }

            override fun onVerificationFailed(e: FirebaseException) {
                onError(e.localizedMessage ?: "OTP sending failed")
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                onCodeSent(verificationId)
            }
        })
        .build()

    PhoneAuthProvider.verifyPhoneNumber(options)
}

private fun verifyOTP(
    context: Context,
    verificationID: String,
    otpCode: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        val credential = PhoneAuthProvider.getCredential(verificationID, otpCode)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onError(task.exception?.localizedMessage ?: "Invalid OTP")
                }
            }
    } catch (e: Exception) {
        onError(e.localizedMessage ?: "OTP verify error")
    }
}

private fun saveSquadData(
    squadViewModel : SquadViewModel,
    loaderManager: LoaderManager,
    squadName: String,
    email: String,
    phoneNumber: String,
    totalMonths: String,
    squadAmount: String,
    squadStartAmount: String,
    onComplete: () -> Unit,
    onError: (String) -> Unit
) {
    loaderManager.showLoader()

    val squadID = System.currentTimeMillis().toString()
    val squadStartDate = java.util.Date()
    val monthsInt = totalMonths.toIntOrNull() ?: 12
    val squadAmountInt = squadAmount.toIntOrNull() ?: 0
    val startAmountInt = squadStartAmount.toIntOrNull() ?: 0

    val squad = Squad(
        squadID = squadID,
        squadName = squadName,
        mailID = email,
        countryCode = "+91",
        phoneNumber = phoneNumber,

        // 🔹 Virtual account details (Swift version didn't have these)
        virtualAccountNumber = "",
        paymentInstrumentId = "",
        virtualUPI = "",

        // 🔹 Account details
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

        currentAvailableAmount = startAmountInt,

        emiConfiguration = emptyList(),

        recordStatus = RecordStatus.ACTIVE,   // ✅ MUST be enum, not string
        recordDate = Date(),

        password = null
    )

    // Using FirestoreManager (assumed present in your project)
    FirestoreManager.shared.addSquad(squad) { success, error ->
        loaderManager.hideLoader()
        if (success) {
            // Optionally add initial payment if start amount > 0 (mirror your Swift logic)

            val login = Login(
                squadID = squadID,
                squadName = squadName,
                squadUsername = "",
                squadUserId = "Manager",
                phoneNumber = phoneNumber,
                role = SquadUserType.SQUAD_MANAGER,
                squadCreatedDate = squadStartDate.asTimestamp,
                userCreatedDate = squadStartDate.asTimestamp
            )

            FirestoreManager.shared.addUserLogin(login) { success, error ->
                loaderManager.hideLoader()
                if (success) {

                    if (startAmountInt > 0) {

                        val newPayment = PaymentsDetails(
                            id = CommonFunctions.generatePaymentID(squadID),
                            paymentUpdatedDate = Date().asTimestamp,
                            payoutUpdatedDate = null,
                            memberId = "",
                            memberName = "SQUAD MANAGER",
                            paymentPhone = squad.phoneNumber,
                            paymentEmail = squad.mailID,

                            userType = SquadUserType.SQUAD_MANAGER,

                            amount = startAmountInt,
                            intrestAmount = 0,

                            paymentEntryType = PaymentEntryType.MANUAL_ENTRY,
                            paymentType = PaymentType.PAYMENT_CREDIT,
                            paymentSubType = PaymentSubType.OTHERS_AMOUNT,

                            description = "Started a squad with an amount of",
                            squadId = squad.squadID,

                            payment_session_id = "",
                            order_id = "",
                            contributionId = "",
                            loanId = "",
                            installmentId = "",

                            transferMode = "",
                            beneId = "",

                            paymentSuccess = true,
                            paymentResponseMessage = "",
                            payoutSuccess = true,
                            payoutResponseMessage = "",

                            transferReferenceId = "",

                            recordStatus = RecordStatus.ACTIVE,
                            recordDate = Date().asTimestamp
                        )



                        // 1️⃣ ADD PAYMENT ENTRY
                        squadViewModel.savePayments(
                            showLoader = true,
                            squadID = squadID,
                            payment = listOf(newPayment)
                        ) { success, error ->
                            if (success) {
                                println("✅ Payment added successfully!")
                            } else {
                                println("❌ Error adding payment: $error")
                            }
                        }

                        // 2️⃣ ADD SQUAD ACTIVITY ENTRY
                        squadViewModel.createSquadActivity(
                            activityType = SquadActivityType.AMOUNT_CREDIT,
                            userName = "SQUAD MANAGER",
                            amount = startAmountInt,
                            description = "Started a squad with an amount of"
                        ) {
                            loaderManager.hideLoader()
                        }
                    }
                    onComplete()

                } else {
                    onError(error ?: "Failed to create squad")
                }
            }
        } else {
            onError(error ?: "Failed to create squad")
        }
    }
}