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

        composable(AppDestination.ACCOUNT_SUMMARY.route) {
            AccountSummaryView(navController, squadViewModel)
        }

        composable(AppDestination.MANAGE_GROUP_FUND.route) {
            ManageGroupFundView(navController, squadViewModel)
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
            GroupFundActivityView(navController, squadViewModel)
        }

        composable(AppDestination.OPEN_PAYMENT_HISTORY.route) {
            PaymentHistoryView(navController, squadViewModel)
        }

        composable(AppDestination.OPEN_GROUP_RULES.route) {
            GroupFundRulesView(navController, squadViewModel)
        }

    }
}

// 🔹 Routes
sealed class AppDestination(val route: String) {
    object SIGN_IN : AppDestination("sign_in")
    object MANAGER_HOME : AppDestination("manager_home")
    object MEMBER_HOME : AppDestination("member_home")
    object ACCOUNT_SUMMARY : AppDestination("account_summary")
    object MANAGE_GROUP_FUND : AppDestination("manager_group_fund")
    object OPEN_MEMBERS_LIST : AppDestination("open_members_list")
    object OPEN_MEMBER_PROFILE : AppDestination("open_member_profile")
    object OPEN_CONTRUBUTION_DETAILS : AppDestination("open_contribution_detail")
    object OPEN_LOAD_DETAILS : AppDestination("open_loan_detail")
    object OPEN_VERIFY_PAYMENTS : AppDestination("open_verify_payments")
    object OPEN_ACTIITY : AppDestination("open_activity")
    object OPEN_PAYMENT_HISTORY : AppDestination("open_payment_history")
    object OPEN_GROUP_RULES : AppDestination("open_group_rules")



}