package com.android.savingssquad.view

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.viewmodel.AppNavHost
import com.android.savingssquad.viewmodel.LoaderManager
import com.android.savingssquad.viewmodel.SquadViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SavingsSquadRoot() }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SavingsSquadRoot() {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background),
        contentAlignment = Alignment.Center
    ) {
        // ✅ Launch the main navigation host
        AppNavHost(
            navController = navController,
            squadViewModel = squadViewModel,
            loaderManager = loaderManager,
            startDestination = startDestination
        )

        // Optional: Show splash screen overlay here
        // if (showLaunchView) LaunchView()
    }

    // Optional launch delay
    LaunchedEffect(Unit) {
        delay(2000)
        showLaunchView = false
    }
}