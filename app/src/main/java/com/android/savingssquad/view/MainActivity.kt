package com.android.savingssquad.view

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.GroupFundUserType
import com.android.savingssquad.singleton.LocalDatabase
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.view.LaunchView
import com.android.savingssquad.view.ManagerTabView
import com.android.savingssquad.view.MemberTabView
import com.android.savingssquad.viewmodel.FirebaseFunctionsManager
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.android.savingssquad.viewmodel.LoaderManager
import com.android.savingssquad.viewmodel.SquadViewModel
import com.google.firebase.BuildConfig
import com.yourapp.utils.CommonFunctions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.zIndex
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex // ✅ this one is key!
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay

// ✅ Main Activity
class MainActivity : ComponentActivity() {

    private val loaderManager = LoaderManager.shared
    private val squadViewModel = SquadViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SavingsSquadRoot(
                loaderManager = loaderManager,
                squadViewModel = squadViewModel
            )
        }
    }
}

// ✅ Root Composable (Launch + Main Content)
@Composable
fun SavingsSquadRoot(
    loaderManager: LoaderManager,
    squadViewModel: SquadViewModel
) {
    var showLaunchView by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background),
        contentAlignment = Alignment.Center
    ) {
        MainContentView(loaderManager, squadViewModel)

        // Optional: animated launch view
        // AnimatedVisibility(...) { LaunchView(...) }
    }

    // Simulated launch delay
    LaunchedEffect(Unit) {
        delay(2000)
        showLaunchView = false
    }
}

// ✅ Main Navigation Content (SwiftUI `NavigationStack` equivalent)
@Composable
fun MainContentView(
    loaderManager: LoaderManager,
    squadViewModel: SquadViewModel
) {
    val isLoggedIn = remember { UserDefaultsManager.getIsLoggedIn() }
    val user = remember { UserDefaultsManager.getLogin() }

    // 🔹 Create navController here (like SwiftUI NavigationStack)
    val navController = rememberNavController()

    // 🔹 Define app navigation structure
    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn && user != null) {
            when (user.role) {
                GroupFundUserType.GROUP_FUND_MANAGER -> "manager_home"
                GroupFundUserType.GROUP_FUND_MEMBER -> "member_home"
            }
        } else "sign_in"
    ) {
        composable("sign_in") {
            GroupFundSignInView(
                navController = navController,
                squadViewModel = squadViewModel,
                loaderManager = loaderManager
            )
        }

        composable("sign_up") {
            GroupFundSignUpView(
                navController = navController,
                squadViewModel = squadViewModel,
                loaderManager = loaderManager
            )
        }

        composable("manager_home") {
            ManagerTabView()
        }

        composable("member_home") {
            MemberTabView()
        }
    }
}