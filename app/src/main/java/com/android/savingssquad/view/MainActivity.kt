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
import com.android.savingssquad.viewmodel.AppDestination
import com.android.savingssquad.viewmodel.SSToast
import com.android.savingssquad.viewmodel.SquadViewModel
import com.google.gson.Gson

class MainActivity : ComponentActivity() {

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
                navigate == SquadUserType.SQUAD_MANAGER.value ||
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
    val loaderManager = remember { LoaderManager.shared }
    val squadViewModel: SquadViewModel = viewModel()
    val navController = rememberNavController()

    // Splash / launch delay state
    var showLaunchView by remember { mutableStateOf(true) }

    // ✅ Determine start destination dynamically (like AppNavigator.setRoot)
    val startDestination = remember {
        val user = UserDefaultsManager.getLogin()
        val isLoggedIn = UserDefaultsManager.getIsLoggedIn()

        if (isLoggedIn && user != null) {
            when (user.role) {
                SquadUserType.SQUAD_MANAGER -> "manager_home"
                SquadUserType.SQUAD_MEMBER -> "member_home"
                else -> "sign_in"
            }
        } else {
            "sign_in"
        }
    }


    LaunchedEffect(Unit) {

        if (UserDefaultsManager.getIsLoggedIn()) {

            squadViewModel.fetchUserLogins(
                showLoader = false,
                phoneNumber = squadViewModel.loginMember?.phoneNumber.orEmpty()
            ) { success, loginList,error ->
                if (loginList != null) {
                    Log.d("FCMUPATED", if (success) "✅ Logins fetched: ${loginList.size}" else "❌ $error")
                }
            }
        }


    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background),
        contentAlignment = Alignment.Center
    ) {
        // ✅ Launch the main navigation host
        RequestNotificationPermission()
        ObserveAppResume(navController, context)
        AppNavHost(
            navController = navController,
            squadViewModel = squadViewModel,
            startDestination = startDestination
        )

        // SHOW LAUNCH / SPLASH VIEW OVERLAY
        if (showLaunchView) {
            LaunchView(
                showLaunchView = showLaunchView,
                onFinish = {
                    UserDefaultsManager.saveIsFromnotification(false)
                    showLaunchView = false
                }
            )
        }

        SSAlert()
        SSLoaderView()
        SSToast()
    }

}