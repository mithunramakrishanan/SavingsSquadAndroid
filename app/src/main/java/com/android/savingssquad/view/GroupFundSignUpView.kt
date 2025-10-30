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


@Composable
fun GroupFundSignUpView(
    navController: NavController,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager = LoaderManager.shared
) {
    val context = LocalContext.current

    // 🔹 States
    val groupFundName by remember { mutableStateOf("") }
    val email by remember { mutableStateOf("") }
    val phoneNumber by remember { mutableStateOf("") }
    val otpCode by remember { mutableStateOf("") }
    val verificationID by remember { mutableStateOf("") }
    val totalMonths by remember { mutableStateOf("") }
    val groupFundAmount by remember { mutableStateOf("") }
    val groupFundStartAmount by remember { mutableStateOf("") }

    val groupFundNameError by remember { mutableStateOf("") }
    val emailError by remember { mutableStateOf("") }
    val phoneError by remember { mutableStateOf("") }
    var sendOTPError by remember { mutableStateOf("") }
    val verifyOTPError by remember { mutableStateOf("") }
    val groupFundMonthError by remember { mutableStateOf("") }
    val groupFundAmountError by remember { mutableStateOf("") }
    val groupFundStartAmountError by remember { mutableStateOf("") }

    var isTermsAccepted by remember { mutableStateOf(false) }
    var isButtonLoading by remember { mutableStateOf(false) }
    val sendOTPLoading by remember { mutableStateOf(false) }
    val verifyOTPLoading by remember { mutableStateOf(false) }
    val isOTPSent by remember { mutableStateOf(false) }
    val otpVerified by remember { mutableStateOf(false) }
    val otpProcessStarted by remember { mutableStateOf(false) }

    // 🔹 Main UI
    Box(modifier = Modifier.fillMaxSize()) {
        AppBackgroundGradient()

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 🔸 Header
            SSNavigationBar(
                title = SquadStrings.signUp,
                navController = navController,
                showBackButton = true,
            )

            Text(
                text = SquadStrings.signUpDescription,
                style = AppFont.ibmPlexSans(15, FontWeight.Normal),
                color = AppColors.successAccent,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 30.dp)
            )

            // 🔹 Form
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SSTextField(
                        icon = Icons.Default.Group,
                        placeholder = SquadStrings.groupFundName,
                        textState = remember { mutableStateOf(groupFundName) },
                        keyboardType = KeyboardType.Text,
                        error = groupFundNameError
                    )
                }

                item {
                    SSTextField(
                        icon = Icons.Default.Email,
                        placeholder = SquadStrings.email,
                        textState = remember { mutableStateOf(email) },
                        keyboardType = KeyboardType.Email,
                        error = emailError
                    )
                }

                item {
                    SSTextField(
                        icon = Icons.Default.Phone,
                        placeholder = SquadStrings.phone,
                        textState = remember { mutableStateOf(phoneNumber) },
                        keyboardType = KeyboardType.Number,
                        showDropdown = sendOTPLoading,
                        isLoading = sendOTPLoading,
                        error = phoneError
                    )
                }

                if (isOTPSent) {
                    item {
                        SSTextField(
                            icon = Icons.Default.Numbers,
                            placeholder = SquadStrings.enterOTP,
                            textState = remember { mutableStateOf(otpCode) },
                            keyboardType = KeyboardType.Number,
                            showDropdown = verifyOTPLoading || otpVerified,
                            dropdownIcon = Icons.Default.CheckCircle,
                            dropdownColor = Color.Blue,
                            isLoading = verifyOTPLoading,
                            error = verifyOTPError
                        )
                    }
                }

                item {
                    SSTextField(
                        icon = Icons.Default.CalendarMonth,
                        placeholder = SquadStrings.groupFundMonths,
                        textState = remember { mutableStateOf(totalMonths) },
                        keyboardType = KeyboardType.Number,
                        error = groupFundMonthError
                    )

                    val months = totalMonths.toIntOrNull() ?: 0
                    Text(
                        text = CommonFunctions.convertMonthsToYearsAndMonths(months),
                        style = AppFont.ibmPlexSans(13, FontWeight.Normal),
                        color = AppColors.successAccent,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }

                item {
                    SSTextField(
                        icon = Icons.Default.CurrencyRupee,
                        placeholder = SquadStrings.groupFundAmount,
                        textState = remember { mutableStateOf(groupFundAmount) },
                        keyboardType = KeyboardType.Number,
                        error = groupFundAmountError
                    )
                }

                item {
                    SSTextField(
                        icon = Icons.Default.Wallet,
                        placeholder = SquadStrings.groupFundStartAmount,
                        textState = remember { mutableStateOf(groupFundStartAmount) },
                        keyboardType = KeyboardType.Number,
                        error = groupFundStartAmountError
                    )
                }
            }

            // 🔹 Terms + Button
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isTermsAccepted,
                        onCheckedChange = { isTermsAccepted = it },
                        colors = CheckboxDefaults.colors(checkedColor = AppColors.successAccent)
                    )
                    Text(
                        text = SquadStrings.agreeToTerms,
                        style = AppFont.ibmPlexSans(14, FontWeight.Normal),
                        color = AppColors.successAccent
                    )
                }

                SSButton(
                    isButtonLoading = isButtonLoading,
                    title = SquadStrings.addGroupFund,
                    isDisabled = !isTermsAccepted || otpProcessStarted
                ) {
                    // 🔹 Handle Sign-Up logic
                    handleSignUp(
                        context,
                        loaderManager,
                        groupFundName,
                        email,
                        phoneNumber,
                        otpCode,
                        verificationID,
                        totalMonths,
                        groupFundAmount,
                        groupFundStartAmount,
                        onLoading = { isButtonLoading = it },
                        onError = { sendOTPError = it }
                    )
                }
            }
        }
    }
}

fun handleSignUp(
    context: Context,
    loaderManager: LoaderManager,
    groupFundName: String,
    email: String,
    phoneNumber: String,
    otpCode: String,
    verificationID: String,
    totalMonths: String,
    groupFundAmount: String,
    groupFundStartAmount: String,
    onLoading: (Boolean) -> Unit,
    onError: (String) -> Unit
) {
    // ✅ Validation checks (same as SwiftUI)
    val groupFundNameError =
        if (groupFundName.trim().isEmpty()) "GroupFund Name is required" else ""
    val emailError =
        if (!email.contains("@") || !email.contains(".")) "Enter a valid email address" else ""
    val phoneError =
        if (!Regex("^[0-9]{10}$").matches(phoneNumber)) "Enter a valid 10-digit phone number" else ""
    val groupFundMonthError =
        if (totalMonths.trim().isEmpty() || totalMonths.toIntOrNull() == null)
            "Enter a valid number for GroupFund Month" else ""
    val groupFundAmountError =
        if (groupFundAmount.trim().isEmpty()) "GroupFund Amount is required" else ""

    // ✅ If any validation failed
    if (
        groupFundNameError.isNotEmpty() ||
        emailError.isNotEmpty() ||
        phoneError.isNotEmpty() ||
        groupFundMonthError.isNotEmpty() ||
        groupFundAmountError.isNotEmpty()
    ) {
        onError(
            listOf(
                groupFundNameError,
                emailError,
                phoneError,
                groupFundMonthError,
                groupFundAmountError
            ).filter { it.isNotEmpty() }.joinToString("\n")
        )
        return
    }

    // ✅ Continue to OTP flow
    if (verificationID.isEmpty()) {
        // Send OTP if not yet sent
        sendOTPSignUp(
            context = context,
            phoneNumber = phoneNumber
        ) { id, error ->
            if (error != null) {
                onError(error)
            } else if (id != null) {
                println("📩 OTP Sent, verification ID: $id")
                Toast.makeText(context, "OTP sent successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    } else {
        // Verify OTP if already sent
        verifyOTPSignUp(
            verificationID = verificationID,
            otpCode = otpCode,
            onStart = { onLoading(true) },
            onSuccess = {
                onLoading(false)
                saveGroupFundData(
                    loaderManager = loaderManager,
                    groupFundName = groupFundName,
                    email = email,
                    phoneNumber = phoneNumber,
                    totalMonths = totalMonths,
                    groupFundAmount = groupFundAmount,
                    groupFundStartAmount = groupFundStartAmount,
                    onError = onError
                )
            },
            onError = {
                onLoading(false)
                onError(it)
            }
        )
    }
}

fun sendOTPSignUp(
    context: Context,
    phoneNumber: String,
    completion: (String?, String?) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val activity = context as? ComponentActivity

    if (activity == null) {
        completion(null, "Invalid context: must be a ComponentActivity")
        return
    }

    val options = PhoneAuthOptions.newBuilder(auth)
        .setPhoneNumber("+91$phoneNumber") // Add country code
        .setTimeout(60L, TimeUnit.SECONDS)
        .setActivity(activity)
        .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                println("✅ Auto verification completed.")
                completion(null, null)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                println("❌ OTP sending failed: ${e.localizedMessage}")
                completion(null, e.localizedMessage)
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                println("📩 OTP sent successfully. Verification ID: $verificationId")
                completion(verificationId, null)
            }
        })
        .build()

    PhoneAuthProvider.verifyPhoneNumber(options)
}

fun verifyOTPSignUp(
    verificationID: String,
    otpCode: String,
    onStart: () -> Unit,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val credential =
        PhoneAuthProvider.getCredential(verificationID, otpCode)
    onStart()

    FirebaseAuth.getInstance().signInWithCredential(credential)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                println("✅ OTP Verified for ${task.result?.user?.phoneNumber}")
                onSuccess()
            } else {
                val message = task.exception?.localizedMessage ?: "Invalid OTP"
                onError(message)
            }
        }
}

fun saveGroupFundData(
    loaderManager: LoaderManager,
    groupFundName: String,
    email: String,
    phoneNumber: String,
    totalMonths: String,
    groupFundAmount: String,
    groupFundStartAmount: String,
    onError: (String) -> Unit
) {
    loaderManager.showLoader()

    val groupFundID = System.currentTimeMillis().toString()
    val groupFundStartDate = Date()
    val totalMonthsInt = totalMonths.toIntOrNull() ?: 12
    val groupFundAmountInt = groupFundAmount.toIntOrNull() ?: 0
    val groupFundStartAmountInt = groupFundStartAmount.toIntOrNull() ?: 0

    val groupFund = GroupFund(
        groupFundID = groupFundID,
        groupFundName = groupFundName,
        mailID = email,
        countryCode = "+91",
        phoneNumber = phoneNumber,
        groupFundStartDate = groupFundStartDate.asTimestamp,
        totalDuration = totalMonthsInt,
        monthlyContribution = groupFundAmountInt,
        groupFundStartAmount = groupFundStartAmountInt
    )

    FirestoreManager.shared.addGroupFund(
        groupFund = groupFund
    ) { success, error ->
        loaderManager.hideLoader()
        if (success) {
            println("✅ GroupFund created successfully!")
        } else {
            onError(error ?: "Failed to create GroupFund")
        }
    }
}