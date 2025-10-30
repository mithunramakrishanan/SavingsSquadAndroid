package com.android.savingssquad.view

// ✅ Jetpack Compose
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Text

// ✅ Firebase Authentication
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider

// ✅ Android + Kotlin
import android.app.Activity
import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.navigation.NavController
import com.android.savingssquad.R
import java.util.concurrent.TimeUnit


import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.appShadow
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.viewmodel.AlertManager
import com.android.savingssquad.viewmodel.LoaderManager
import com.android.savingssquad.viewmodel.SquadViewModel


@Composable
fun GroupFundSignInView( navController: NavController, squadViewModel: SquadViewModel, loaderManager: LoaderManager) {
    val context = LocalContext.current

    var phoneNumber by remember { mutableStateOf("") }
    val otpCode by remember { mutableStateOf("") }
    var verificationID by remember { mutableStateOf("") }
    var isOTPSent by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isButtonLoading by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 🌈 Background gradient (your AppBackgroundGradient component)
        AppBackgroundGradient()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 🔹 App Icon + Welcome text
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = "App Icon",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .appShadow(AppShadows.card)
                )

                Text(
                    text = SquadStrings.welcome,
                    style = AppFont.ibmPlexSans(size = 20, weight = FontWeight.SemiBold),
                    color = AppColors.headerText
                )
            }

            // 🔹 Glassmorphic Login Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.35f), // top frost
                                Color.LightGray.copy(alpha = 0.25f) // bottom tint
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(24.dp)) // light border edge
                    .appShadow(AppShadows.card)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 🔸 Title
                Text(
                    text = if (isOTPSent) SquadStrings.confirmOTP else SquadStrings.confirmPhone,
                    style = AppFont.ibmPlexSans(22, FontWeight.Bold),
                    color = AppColors.headerText,
                    textAlign = TextAlign.Center
                )

                // 🔸 Input Fields
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    val phoneNumberState = remember { mutableStateOf(phoneNumber) }

                    SSTextField(
                        icon = Icons.Default.Phone,
                        placeholder = SquadStrings.enterPhoneNumber,
                        textState = phoneNumberState,
                        keyboardType = KeyboardType.Number
                    )

                    if (isOTPSent) {
                        val otpCodeState = remember { mutableStateOf(otpCode) }

                        SSTextField(
                            icon = Icons.Default.Numbers,
                            placeholder = SquadStrings.enterOTP,
                            textState = otpCodeState,
                            keyboardType = KeyboardType.Number
                        )
                    }
                }

                // 🔹 Action Button (Send/Verify OTP)
                SSButton(
                    isButtonLoading = isButtonLoading,
                    title = if (isOTPSent) SquadStrings.verifyOTP else SquadStrings.sendOTP
                ) {
                    if (isOTPSent) {
                        if (verificationID.isEmpty()) {
                            AlertManager.shared.showAlert(
                                title = SquadStrings.appName,
                                message = SquadStrings.invalidOTP
                            )
                        } else {
                            verifyOTP(
                                verificationID = verificationID,
                                otpCode = otpCode,
                                onStart = { isButtonLoading = true },
                                onSuccess = {
                                    isButtonLoading = false
                                    squadViewModel.fetchUserLogins(
                                        showLoader = true,
                                        phoneNumber = phoneNumber
                                    ) { success, message ->
                                        if (success) println("✅ User logins fetched successfully")
                                        else println("❌ $message")
                                    }
                                },
                                onError = {
                                    isButtonLoading = false
                                    errorMessage = it
                                }
                            )
                        }
                    } else {
                        if (phoneNumber.isEmpty()) {
                            AlertManager.shared.showAlert(
                                title = SquadStrings.appName,
                                message = SquadStrings.enterPhoneNumber
                            )
                        } else {
                            sendOTP(
                                context = context,
                                phoneNumber = phoneNumber,
                                onStart = { isButtonLoading = true },
                                onSuccess = { id ->
                                    verificationID = id
                                    isOTPSent = true
                                    isButtonLoading = false
                                },
                                onError = {
                                    isButtonLoading = false
                                    errorMessage = it
                                }
                            )
                        }
                    }
                }

                // 🔹 Secondary action (Sign up link)
                Text(
                    text = SquadStrings.addGroupFund,
                    style = AppFont.ibmPlexSans(14, FontWeight.Medium),
                    color = Color(0xFF007AFF), // iOS system blue
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        // 🔜 Navigate to Sign-Up screen
                        navController.navigate("sign_up")
                    }
                )

                // 🔹 Error message
                errorMessage?.let {
                    Text(
                        text = it,
                        style = AppFont.ibmPlexSans(13, FontWeight.Normal),
                        color = AppColors.errorAccent
                    )
                }

                // 🔹 Security note
                Text(
                    text = SquadStrings.informationSafe,
                    style = AppFont.ibmPlexSans(12, FontWeight.Normal),
                    color = AppColors.successAccent
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        SSAlert()

        // 🔹 Popup overlay for users (like SwiftUI `.sheet`)
        val showPopup by squadViewModel.showPopup.collectAsState()
        if (showPopup) {
            OverlayBackgroundView()
            LoginListPopup(
                showPopup = remember { mutableStateOf(squadViewModel.showPopup.value) },
                selectedUser = remember { mutableStateOf(squadViewModel.selectedUser.value) },
                users = squadViewModel.users.collectAsState().value,
                squadViewModel = squadViewModel
            )
        }
    }
}

fun sendOTP(
    context: Context,
    phoneNumber: String,
    onStart: () -> Unit,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    val fullNumber = "+91$phoneNumber"
    onStart()

    PhoneAuthProvider.getInstance().verifyPhoneNumber(
        fullNumber,
        60,
        TimeUnit.SECONDS,
        context as Activity,
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                onSuccess("")
            }

            override fun onVerificationFailed(e: FirebaseException) {
                onError(e.localizedMessage ?: "OTP failed")
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                onSuccess(verificationId)
            }
        }
    )
}

fun verifyOTP(
    verificationID: String,
    otpCode: String,
    onStart: () -> Unit,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    onStart()
    val credential = PhoneAuthProvider.getCredential(verificationID, otpCode)
    FirebaseAuth.getInstance().signInWithCredential(credential)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) onSuccess()
            else onError(task.exception?.localizedMessage ?: "Verification failed")
        }
}
