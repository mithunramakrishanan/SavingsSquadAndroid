package com.android.savingssquad.viewmodel

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.savingssquad.view.*
import com.android.savingssquad.singleton.GroupFundUserType


@Composable
fun AppNavHost(
    navController: NavHostController,
    squadViewModel: SquadViewModel = viewModel(),
    loaderManager: LoaderManager = LoaderManager.shared,
    startDestination: String = AppDestination.SIGN_IN.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 🔹 Login
        composable(AppDestination.SIGN_IN.route) {
            GroupFundSignInView(navController, squadViewModel,loaderManager)
        }

        // 🔹 Manager TabView (with internal navigation)
        composable(AppDestination.MANAGER_HOME.route) {
            ManagerTabView(navController, squadViewModel, loaderManager)
        }

        // 🔹 Member TabView (with internal navigation)
        composable(AppDestination.MEMBER_HOME.route) {
            MemberTabView(navController, squadViewModel, loaderManager)
        }
    }
}

// 🔹 Routes
sealed class AppDestination(val route: String) {
    object SIGN_IN : AppDestination("sign_in")
    object MANAGER_HOME : AppDestination("manager_home")
    object MEMBER_HOME : AppDestination("member_home")
}