package com.android.savingssquad.view

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import androidx.room.util.copy
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.viewmodel.LoaderManager
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.singleton.appShadow
import com.android.savingssquad.viewmodel.AlertManager
import com.android.savingssquad.viewmodel.AppDestination
import com.android.savingssquad.viewmodel.FirestoreManager
import com.android.savingssquad.viewmodel.SSToast

sealed class SquadSettingsEvent {
    data object ManageSquad : SquadSettingsEvent()
    data object ManualEntry : SquadSettingsEvent()
    data object BankDetails : SquadSettingsEvent()
    data object ManageLoan : SquadSettingsEvent()
    data object RestorePurchase : SquadSettingsEvent()
    data object Activity : SquadSettingsEvent()
    data object PaymentHistory : SquadSettingsEvent()
    data object ChitRules : SquadSettingsEvent()
    data object ContactUs : SquadSettingsEvent()
}

@Composable
fun SquadSettingsView(
    navController: NavController,
    squadViewModel: SquadViewModel
) {

    var navigationEvent by remember { mutableStateOf<SquadSettingsEvent?>(null) }

    LaunchedEffect(navigationEvent) {
        when (navigationEvent) {

            is SquadSettingsEvent.ManageSquad ->
                navController.navigate(AppDestination.MANAGE_SQUAD.route)

            is SquadSettingsEvent.ManualEntry ->
                navController.navigate(AppDestination.OPEN_MANUAL_ENTRY.route)

            is SquadSettingsEvent.BankDetails ->
                navController.navigate(AppDestination.OPEN_BANK_DETAILS.route)

            is SquadSettingsEvent.ManageLoan ->
                navController.navigate(AppDestination.OPEN_MANAGE_LOAN.route)

            is SquadSettingsEvent.RestorePurchase ->
                navController.navigate(AppDestination.OPEN_RESTORE_PURCHASE.route)

            is SquadSettingsEvent.Activity ->
                navController.navigate(AppDestination.OPEN_ACTIITY.route)

            is SquadSettingsEvent.PaymentHistory ->
                navController.navigate(AppDestination.OPEN_PAYMENT_HISTORY.route)

            is SquadSettingsEvent.ChitRules ->
                navController.navigate(AppDestination.OPEN_GROUP_RULES.route)

            is SquadSettingsEvent.ContactUs ->
                navController.navigate(AppDestination.OPEN_CONTACT_US.route)

            null -> Unit
        }

        navigationEvent = null
    }

    Box(
        modifier = Modifier

            .fillMaxSize()

            .windowInsetsPadding(WindowInsets.safeDrawing)
    )
    {
        AppBackgroundGradient()

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        )
        {

            // 📌 Title
            SSNavigationBar(
                title = SquadStrings.manageAccount,
                navController = navController,
                showBackButton = false
            )

            Spacer(modifier = Modifier.height(20.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .weight(1f)   // ✅ IMPORTANT FIX
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp)
                    .padding(bottom = 10.dp)
            )
            {

                if (UserDefaultsManager.getSquadManagerLogged()) {

                    item {
                        SettingsDashboardCard(
                            title = "Manage Squad",
                            subtitle = "Members & Settings",
                            icon = Icons.Default.Groups,
                            color = Color(0xFF3B82F6)
                        ) {
                            navigationEvent = SquadSettingsEvent.ManageSquad
                        }
                    }

                    item {
                        SettingsDashboardCard(
                            title = "Manage Loan",
                            subtitle = "Manage Loans",
                            icon = Icons.Default.CreditCard,
                            color = Color(0xFF10B981)
                        ) {
                            navigationEvent = SquadSettingsEvent.ManageLoan

                        }
                    }

                    item {
                        SettingsDashboardCard(
                            title = "Manual Entry",
                            subtitle = "Cash Records",
                            icon = Icons.Default.EditNote,
                            color = Color(0xFFF59E0B)
                        ) {
                            navigationEvent = SquadSettingsEvent.ManualEntry
                        }
                    }
                }

                item {
                    SettingsDashboardCard(
                        title = "Bank Details",
                        subtitle = "UPI Setup",
                        icon = Icons.Default.AccountBalance,
                        color = Color(0xFF8B5CF6)
                    ) {
                        navigationEvent = SquadSettingsEvent.BankDetails
                    }
                }

                item {
                    SettingsDashboardCard(
                        title = "Squad Activity",
                        subtitle = "Track Insights",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        color = Color(0xFFEC4899)
                    ) {
                        navigationEvent = SquadSettingsEvent.Activity
                    }
                }

                item {
                    SettingsDashboardCard(
                        title = "Payment History",
                        subtitle = "Transactions",
                        icon = Icons.Default.History,
                        color = Color(0xFF6366F1)
                    ) {
                        navigationEvent = SquadSettingsEvent.PaymentHistory
                    }
                }

                item {
                    SettingsDashboardCard(
                        title = "Squad Rules",
                        subtitle = "Policies & Limits",
                        icon = Icons.AutoMirrored.Filled.Rule,
                        color = Color(0xFF14B8A6)
                    ) {
                        navigationEvent = SquadSettingsEvent.ChitRules
                    }
                }

                if (UserDefaultsManager.getSquadManagerLogged()) {

                    item {
                        SettingsDashboardCard(
                            title = "Restore Purchases",
                            subtitle = "Subscription",
                            icon = Icons.Default.Restore,
                            color = Color.Gray
                        ) {
                            navigationEvent = SquadSettingsEvent.RestorePurchase
                        }
                    }

                }

                item {
                    SettingsDashboardCard(
                        title = "Logout",
                        subtitle = "Sign out securely",
                        icon = Icons.AutoMirrored.Filled.Logout,
                        color = Color.Red
                    ) {
                        AlertManager.shared.showAlert(
                            title = SquadStrings.appName,
                            message = "Are you sure you want to logout?",
                            primaryButtonTitle = "LOGOUT",
                            primaryAction = {
                                logoutUser(navController,squadViewModel)
                            },
                            secondaryButtonTitle = "NO",
                            secondaryAction = {}
                        )
                    }
                }
            }

            ContactUsButton(

                onClick = {

                    navigationEvent = SquadSettingsEvent.ContactUs

                },

                modifier = Modifier.padding(bottom = 20.dp)

            )


        }
    }
}

@Composable
fun SettingsDashboardCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    action: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(AppColors.surface)
            .border(
                width = 0.8.dp,
                color = AppColors.border.copy(alpha = 0.18f),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { action() }
            .padding(16.dp)
    ) {

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // ================= TOP ROW =================
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = Icons.Default.NorthEast,
                    contentDescription = null,
                    tint = AppColors.secondaryText.copy(alpha = 0.5f),
                    modifier = Modifier.size(12.dp)
                )
            }

            // ================= TEXT BLOCK =================
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                Text(
                    text = title,
                    style = AppFont.ibmPlexSans(
                        size = 15,
                        weight = FontWeight.SemiBold
                    ),
                    color = AppColors.headerText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = subtitle,
                    style = AppFont.ibmPlexSans(
                        size = 12,
                        weight = FontWeight.Medium
                    ),
                    color = AppColors.secondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ContactUsButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {

        Card(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clickable { onClick() }
                .appShadow(
                    style = AppShadows.card,
                    shape = RoundedCornerShape(14.dp)
                ),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(Color.White)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            AppColors.primaryBackground,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = null,
                        tint = AppColors.primaryBrand,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Contact Us",
                        style = AppFont.ibmPlexSans(14, FontWeight.SemiBold),
                        color = AppColors.headerText
                    )

                    Text(
                        "We’re here to help you",
                        style = AppFont.ibmPlexSans(11),
                        color = AppColors.secondaryText,
                        maxLines = 1
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = AppColors.secondaryText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

fun logoutUser(navController: NavController, squadViewModel : SquadViewModel) {
    UserDefaultsManager.clearAll()

    FirestoreManager.shared.clearFCMTokenForAllUsers(squadViewModel.users.value) { success, error ->

        if (success) {
            Log.d("LOGOUT", "✅ FCM tokens cleared")
        } else {
            Log.e("LOGOUT", "❌ Error: $error")
        }

        navController.navigate(AppDestination.SIGN_IN.route) {
            // Remove the entire back stack
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }
}