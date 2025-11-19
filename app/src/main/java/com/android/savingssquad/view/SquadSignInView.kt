package com.android.savingssquad.view

// ✅ Jetpack Compose
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
import androidx.compose.material3.TextButton
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
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.viewmodel.AlertManager
import com.android.savingssquad.viewmodel.AppDestination
import com.android.savingssquad.viewmodel.LoaderManager
import com.android.savingssquad.viewmodel.SquadViewModel
import com.google.firebase.auth.PhoneAuthOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun SquadSignInView( navController: NavController, squadViewModel: SquadViewModel, loaderManager: LoaderManager) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope() // ✅ define this
    val phoneNumber  = remember { mutableStateOf("") }
    val otpCode = remember { mutableStateOf("") }
    val verificationID = remember { mutableStateOf("") }
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

                    SSTextField(
                        icon = Icons.Default.Phone,
                        placeholder = SquadStrings.enterPhoneNumber,
                        textState = phoneNumber,
                        keyboardType = KeyboardType.Number
                    )

                    if (isOTPSent) {

                        SSTextField(
                            icon = Icons.Default.Numbers,
                            placeholder = SquadStrings.enterOTP,
                            textState = otpCode,
                            keyboardType = KeyboardType.Number
                        )
                    }
                }

                // 🔹 Action Button (Send/Verify OTP)
                SSButton(
                    isButtonLoading = isButtonLoading,
                    title = if (isOTPSent) SquadStrings.verifyOTP else SquadStrings.sendOTP
                ) {
                    coroutineScope.launch {
                        if (isOTPSent) {

                            if (verificationID.value.isEmpty()) {
                                AlertManager.shared.showAlert(
                                    title = SquadStrings.appName,
                                    message = SquadStrings.invalidOTP
                                )
                                return@launch
                            }

                            verifyOTP(
                                verificationID = verificationID.value,
                                otpCode = otpCode.value,
                                onStart = { isButtonLoading = true },
                                onSuccess = {
                                    isButtonLoading = false

                                    // 🔥 Call ViewModel function
                                    squadViewModel.fetchUserLogins(
                                        showLoader = true,
                                        phoneNumber = phoneNumber.value
                                    ) { success, loginList, message ->

                                        if (success) {
                                            // 🔥 ALWAYS switch to MAIN THREAD for state updates
                                            coroutineScope.launch(Dispatchers.Main) {

                                                println("✅ User logins fetched successfully $loginList")

                                                loginList?.let {
                                                    squadViewModel.setUsers(it)
                                                    squadViewModel.setShowPopup(it.size > 1)
                                                }

                                                if (loginList?.size == 1) {
                                                    val selectedUser = loginList.first()

                                                    squadViewModel.setSelectedUser(selectedUser)
                                                    UserDefaultsManager.saveLogin(selectedUser)
                                                    UserDefaultsManager.saveIsLoggedIn(true)

                                                    if (selectedUser.role == SquadUserType.SQUAD_MANAGER) {
                                                        UserDefaultsManager.saveSquadManagerLogged(true)
                                                        navController.navigate(AppDestination.MANAGER_HOME.route) {
                                                            popUpTo(AppDestination.SIGN_IN.route) { inclusive = true }
                                                            launchSingleTop = true
                                                        }
                                                    } else {
                                                        UserDefaultsManager.saveSquadManagerLogged(false)
                                                        navController.navigate(AppDestination.MEMBER_HOME.route) {
                                                            popUpTo(AppDestination.SIGN_IN.route) { inclusive = true }
                                                            launchSingleTop = true
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            println("❌ $message")
                                        }
                                    }
                                },
                                onError = {
                                    isButtonLoading = false
                                    errorMessage = it
                                }
                            )

                        } else {
                            // ---- SEND OTP FLOW ----
                            if (phoneNumber.value.isEmpty()) {
                                AlertManager.shared.showAlert(
                                    title = SquadStrings.appName,
                                    message = SquadStrings.enterPhoneNumber
                                )
                                return@launch
                            }

                            sendOTP(
                                context = context,
                                phoneNumber = phoneNumber.value,
                                onStart = { isButtonLoading = true },
                                onSuccess = { id ->
                                    verificationID.value = id
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
                TextButton(
                    onClick = {
                        navController.navigate(AppDestination.SIGN_UP.route)
                    },
                    contentPadding = PaddingValues(0.dp) // keeps it looking like text, not a big button
                ) {
                    Text(
                        text = SquadStrings.addSquad,
                        style = AppFont.ibmPlexSans(14, FontWeight.Medium),
                        color = Color(0xFF007AFF),
                        textDecoration = TextDecoration.Underline
                    )
                }

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
        SSLoaderView()

        val showPopup = squadViewModel.showPopup.collectAsState()
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

fun sendOTP(
    context: Context,
    phoneNumber: String,
    onStart: () -> Unit,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    val fullNumber = "+91$phoneNumber"
    onStart()

    val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
        .setPhoneNumber(fullNumber)
        .setTimeout(60L, TimeUnit.SECONDS)
        .setActivity(context as Activity)
        .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // ✅ Auto verification - directly sign in
                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            onSuccess("AUTO_VERIFIED") // flag
                        } else {
                            onError(task.exception?.localizedMessage ?: "Auto verification failed")
                        }
                    }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                onError(e.localizedMessage ?: "OTP failed")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                // ✅ Save this verificationId
                onSuccess(verificationId)
            }
        }).build()

    PhoneAuthProvider.verifyPhoneNumber(options)
}


fun verifyOTP(
    verificationID: String,
    otpCode: String,
    onStart: () -> Unit,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    if (verificationID.isEmpty() || verificationID == "AUTO_VERIFIED") {
        onError("Invalid verification ID. Please request OTP again.")
        return
    }

    onStart()

    val credential = PhoneAuthProvider.getCredential(verificationID, otpCode)
    FirebaseAuth.getInstance().signInWithCredential(credential)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) onSuccess()
            else onError(task.exception?.localizedMessage ?: "Verification failed")
        }
}
