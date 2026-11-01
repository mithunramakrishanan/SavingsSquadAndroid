package com.android.savingssquad.view

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.android.savingssquad.SquadSubscription.BillingHelper
import com.android.savingssquad.model.Login
import com.android.savingssquad.model.PaymentsDetails
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.ObserveAppResume
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.UPIPaymentManager
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.viewmodel.AppNavHost
import com.android.savingssquad.singleton.LoaderManager
import com.android.savingssquad.singleton.clearFocusOnTapOutside
import com.android.savingssquad.viewmodel.AppDestination
import com.android.savingssquad.viewmodel.RazorpayPaymentManager
import com.android.savingssquad.viewmodel.SSToast
import com.android.savingssquad.viewmodel.SquadViewModel
import com.google.firebase.auth.FirebaseAuth
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.google.gson.Gson

class MainActivity : ComponentActivity(),PaymentResultWithDataListener {

    companion object {
        private const val TAG = "MainActivity"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        UPIPaymentManager.shared.register(this)
        handleNotification(intent)
        BillingHelper.init(this)
        setContent {
            SavingsSquadRoot()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)
        handleNotification(intent)
    }


    /**

     * Razorpay payment success callback.

     *

     * This callback confirms that the Razorpay Checkout SDK reported success.

     * Your backend/webhook must still verify the payment before activating

     * the subscription.

     */

    override fun onPaymentSuccess(

        paymentId: String,

        paymentData: PaymentData

    ) {

        Log.d(TAG, "Razorpay payment success callback received")

        Log.d(TAG, "Payment ID: $paymentId")

        Log.d(TAG, "Order ID: ${paymentData.orderId}")

        RazorpayPaymentManager.onPaymentSuccess(

            paymentId,

            paymentData

        )

    }

    /**

     * Razorpay payment failure callback.

     */

    override fun onPaymentError(

        code: Int,

        message: String,

        paymentData: PaymentData

    ) {

        Log.e(TAG, "Razorpay payment failed")

        Log.e(TAG, "Error code: $code")

        Log.e(TAG, "Error message: $message")

        Log.e(TAG, "Order ID: ${paymentData.orderId}")

        RazorpayPaymentManager.onPaymentError(

            code,

            message,

            paymentData

        )

    }

    private fun handleNotification(intent: Intent) {

        if (!UserDefaultsManager.getIsLoggedIn()) return


        val navigate = intent.getStringExtra("navigate") ?: return

        val notificationType = intent.getStringExtra("notificationType")

        if (notificationType == "REMAINDER" || notificationType == "REJECTED" || notificationType == "ACCEPTED") {
            return
        }

        val squadId = intent.getStringExtra("squadId")

        Log.d("NOTI", "navigate = $navigate")
        Log.d("NOTI", "squadId = $squadId")
        Log.d("NOTI", "notificationType = $notificationType")

        // Cash Request Notification
        UserDefaultsManager.saveIsCashReqNotification(
            navigate == "CASH_REQUEST"
        )

        // Load all saved squad logins
        val loginList = UserDefaultsManager.getSquadLogins()

        if (loginList.isNotEmpty()) {

            val selectedLogin = if (
                navigate == SquadUserType.SQUAD_MANAGER.name ||
                navigate == "CASH_REQUEST"
            ) {

                loginList.firstOrNull {
                    it.squadID == squadId &&
                            it.role == SquadUserType.SQUAD_MANAGER
                }

            } else {

                loginList.firstOrNull {
                    it.squadID == squadId &&
                            it.role == SquadUserType.SQUAD_MEMBER
                }
            }

            selectedLogin?.let {

                UserDefaultsManager.saveLogin(it)
                UserDefaultsManager.saveIsLoggedIn(true)
                UserDefaultsManager.saveIsFromnotification(true)

                val isManager = it.role == SquadUserType.SQUAD_MANAGER

                UserDefaultsManager.saveSquadManagerLogged(isManager)

                if (isManager) {
                    AppDestination.MANAGER_HOME.route
                } else {
                    AppDestination.MEMBER_HOME.route
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SavingsSquadRoot() {

    val context = LocalContext.current

    val loaderManager = remember {
        LoaderManager.shared
    }

    val squadViewModel: SquadViewModel = viewModel()

    val navController = rememberNavController()

    // MARK: Launch Screen

    var showLaunchView by remember {
        mutableStateOf(true)
    }

    // MARK: Initial Navigation

    val initialLogin = remember {
        UserDefaultsManager.getLogin()
    }

    val isLoggedIn = remember {
        UserDefaultsManager.getIsLoggedIn()
    }

    val startDestination = remember {

        if (isLoggedIn && initialLogin != null) {

            when (initialLogin.role) {

                SquadUserType.SQUAD_MANAGER ->
                    AppDestination.MANAGER_HOME.route

                SquadUserType.SQUAD_MEMBER ->
                    AppDestination.MEMBER_HOME.route

                else ->
                    AppDestination.SIGN_IN.route
            }

        } else {

            AppDestination.SIGN_IN.route
        }
    }


    LaunchedEffect(squadViewModel) {
        squadViewModel.logoutEvent.collect {
            squadViewModel.logoutUser(navController)
        }
    }

    // MARK: Restore Firebase Session

    LaunchedEffect(Unit) {

        if (UserDefaultsManager.getIsLoggedIn()) {

            val firebaseUser =
                FirebaseAuth.getInstance().currentUser

            if (firebaseUser == null) {

                Log.e(
                    "MainActivity",
                    "❌ No active Firebase Auth session found."
                )

                // Local session exists, but Firebase authentication is missing.
                // Use your existing logout implementation here.

                return@LaunchedEffect
            }

            val firebaseUID = firebaseUser.uid

            val phoneNumber = firebaseUser.phoneNumber
                ?: UserDefaultsManager.getLogin()?.phoneNumber
                ?: ""

            Log.d(
                "MainActivity",
                "✅ Restoring Firebase session"
            )

            Log.d(
                "MainActivity",
                "Firebase UID: $firebaseUID"
            )

            Log.d(
                "MainActivity",
                "Phone: $phoneNumber"
            )

            squadViewModel.fetchUserLogins(
                showLoader = false,
                firebaseUID = firebaseUID,
                phoneNumber = phoneNumber
            ) { success, loginList, error ->

                if (success && loginList != null) {

                    Log.d(
                        "MainActivity",
                        "✅ User profiles restored: ${loginList.size}"
                    )

                    // Automatically navigate when only one profile exists.
                    if (loginList.size == 1) {

                        val selectedLogin = loginList.first()

                        val destination = when (selectedLogin.role) {

                            SquadUserType.SQUAD_MANAGER ->
                                AppDestination.MANAGER_HOME.route

                            SquadUserType.SQUAD_MEMBER ->
                                AppDestination.MEMBER_HOME.route

                            else ->
                                AppDestination.SIGN_IN.route
                        }

                        navController.navigate(destination) {

                            popUpTo(
                                navController.graph.startDestinationId
                            ) {
                                inclusive = true
                            }

                            launchSingleTop = true
                        }

                    } else {

                        // Multiple profiles exist.
                        // Keep the existing selected profile and let your
                        // existing account-selection UI handle switching.

                        Log.d(
                            "MainActivity",
                            "Multiple profiles available: ${loginList.size}"
                        )
                    }

                } else {

                    Log.e(
                        "MainActivity",
                        "❌ Failed to restore user profiles: ${
                            error ?: "Unknown error"
                        }"
                    )
                }
            }
        }
    }

    // MARK: Main UI

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clearFocusOnTapOutside()
            .background(AppColors.background),
        contentAlignment = Alignment.Center
    ) {

        // Notification Permission

        RequestNotificationPermission()

        // App Resume Observer

        ObserveAppResume(
            navController,
            squadViewModel,
            context
        )

        // Main Navigation

        AppNavHost(
            navController = navController,
            squadViewModel = squadViewModel,
            startDestination = startDestination
        )

        // Launch Screen Overlay

        if (showLaunchView) {

            LaunchView(
                showLaunchView = showLaunchView,
                onFinish = {

                    UserDefaultsManager.saveIsFromnotification(false)

                    showLaunchView = false
                }
            )
        }

        // Global Components

        SSAlert()

        SSLoaderView()

        SSToast()
    }
}