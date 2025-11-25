package com.android.savingssquad.viewmodel

import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.savingssquad.view.*
import com.android.savingssquad.singleton.SquadUserType
import com.google.accompanist.navigation.animation.rememberAnimatedNavController

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.animation.*
import androidx.compose.animation.core.tween

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavHost(
    navController: NavHostController,
    squadViewModel: SquadViewModel = viewModel(),
    loaderManager: LoaderManager = LoaderManager.shared,
    startDestination: String = AppDestination.SIGN_IN.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(350)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(350)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(350)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(350)
            )
        }
    ) {

        composable(AppDestination.SIGN_IN.route) {
            SquadSignInView(navController, squadViewModel, loaderManager)
        }

        composable(AppDestination.SIGN_UP.route) {
            SquadSignUpView(navController, squadViewModel, loaderManager)
        }

        composable(AppDestination.MANAGER_HOME.route) {
            ManagerTabView(navController, squadViewModel, loaderManager)
        }

        composable(AppDestination.MEMBER_HOME.route) {
            MemberTabView(navController, squadViewModel, loaderManager)
        }

        composable(AppDestination.ACCOUNT_SUMMARY.route) {
            AccountSummaryView(navController, squadViewModel)
        }

        composable(AppDestination.MANAGE_SQUAD.route) {
            ManageSquadView(navController, squadViewModel)
        }

        composable(AppDestination.OPEN_MEMBERS_LIST.route) {
            MembersListView(navController, squadViewModel)
        }

        composable(AppDestination.OPEN_MEMBER_PROFILE.route) {
            MemberProfileView(navController, squadViewModel)
        }

        composable(AppDestination.OPEN_CONTRUBUTION_DETAILS.route) {
            ContributionDetailsView(navController, squadViewModel)
        }

        composable(AppDestination.OPEN_LOAD_DETAILS.route) {
            LoanDetailsView(navController, squadViewModel)
        }

        composable(AppDestination.OPEN_VERIFY_PAYMENTS.route) {
            VerifyPaymentsView(navController, squadViewModel)
        }

        composable(AppDestination.OPEN_ACTIITY.route) {
            SquadActivityView(navController, squadViewModel)
        }

        composable(AppDestination.OPEN_PAYMENT_HISTORY.route) {
            PaymentHistoryView(navController, squadViewModel)
        }

        composable(AppDestination.OPEN_GROUP_RULES.route) {
            SquadRulesView(navController, squadViewModel)
        }

        composable(AppDestination.OPEN_MANUAL_ENTRY.route) {
            ManualEntryView(navController, squadViewModel)
        }

        composable(AppDestination.OPEN_BANK_DETAILS.route) {
            BankDetailsView(navController, squadViewModel)
        }

        composable(AppDestination.OPEN_MANAGE_LOAN.route) {
            ManageLoanView(navController, squadViewModel)
        }

        composable(AppDestination.OPEN_DUES_SCREEN.route) {
            DuesScreenView(navController, squadViewModel)
        }
    }
}

// 🔹 Routes
sealed class AppDestination(val route: String) {
    object SIGN_IN : AppDestination("sign_in")
    object SIGN_UP : AppDestination("sign_up")
    object MANAGER_HOME : AppDestination("manager_home")
    object MEMBER_HOME : AppDestination("member_home")
    object ACCOUNT_SUMMARY : AppDestination("account_summary")
    object MANAGE_SQUAD : AppDestination("manager_group_fund")
    object OPEN_MEMBERS_LIST : AppDestination("open_members_list")
    object OPEN_MEMBER_PROFILE : AppDestination("open_member_profile")
    object OPEN_CONTRUBUTION_DETAILS : AppDestination("open_contribution_detail")
    object OPEN_LOAD_DETAILS : AppDestination("open_loan_detail")
    object OPEN_VERIFY_PAYMENTS : AppDestination("open_verify_payments")
    object OPEN_ACTIITY : AppDestination("open_activity")
    object OPEN_PAYMENT_HISTORY : AppDestination("open_payment_history")
    object OPEN_GROUP_RULES : AppDestination("open_group_rules")

    object OPEN_MANUAL_ENTRY : AppDestination("open_manual_entry")
    object OPEN_BANK_DETAILS : AppDestination("open_bank_details")
    object OPEN_MANAGE_LOAN: AppDestination("open_manage_loan")

    object OPEN_DUES_SCREEN: AppDestination("open_dues_screen")


}