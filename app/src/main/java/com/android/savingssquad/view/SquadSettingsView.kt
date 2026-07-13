package com.android.savingssquad.view

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.room.util.copy
import com.android.savingssquad.model.Login
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.singleton.LoaderManager
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

    data object YourSquads : SquadSettingsEvent()

    data object Activity : SquadSettingsEvent()
    data object PaymentHistory : SquadSettingsEvent()
    data object ChitRules : SquadSettingsEvent()
    data object ContactUs : SquadSettingsEvent()
}

// =========================================================
//  SQUAD SETTINGS — PREMIUM REDESIGN
// =========================================================

@Composable
fun SquadSettingsView(
    navController: NavController,
    squadViewModel: SquadViewModel
) {

    var navigationEvent by remember { mutableStateOf<SquadSettingsEvent?>(null) }

    val usersList by squadViewModel.users.collectAsStateWithLifecycle()

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

            is SquadSettingsEvent.YourSquads ->
                navController.navigate(AppDestination.OPEN_YOUR_SQUADS.route)

            null -> Unit
        }

        navigationEvent = null
    }

    val isManager = UserDefaultsManager.getSquadManagerLogged()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    )
    {
        AppBackgroundGradient()

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            SSNavigationBar(
                title = SquadStrings.manageAccount,
                navController = navController,
                showBackButton = false
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 10.dp)
            ) {

                // ---------- HERO / GREETING CARD ----------
                item(span = { GridItemSpan(2) }) {
                    HeroGreetingCard(isManager = isManager)
                }

                if (isManager) {
                    item(span = { GridItemSpan(2) }) {
                        SectionHeader(text = "Squad Management")
                    }

                    item {
                        SettingsDashboardCard(
                            title = "Manage Squad",
                            subtitle = "Members & Settings",
                            icon = Icons.Default.Groups,
                            gradient = listOf(Color(0xFF3B82F6), Color(0xFF60A5FA))
                        ) { navigationEvent = SquadSettingsEvent.ManageSquad }
                    }

                    item {
                        SettingsDashboardCard(
                            title = "Manage Loan",
                            subtitle = "Manage Loans",
                            icon = Icons.Default.CreditCard,
                            gradient = listOf(Color(0xFF10B981), Color(0xFF34D399))
                        ) { navigationEvent = SquadSettingsEvent.ManageLoan }
                    }

                    item {
                        SettingsDashboardCard(
                            title = "Manual Entry",
                            subtitle = "Cash Records",
                            icon = Icons.Default.EditNote,
                            gradient = listOf(Color(0xFFF59E0B), Color(0xFFFBBF24))
                        ) { navigationEvent = SquadSettingsEvent.ManualEntry }
                    }

                    item {
                        SettingsDashboardCard(
                            title = "Restore Purchases",
                            subtitle = "Subscription",
                            icon = Icons.Default.Restore,
                            gradient = listOf(Color(0xFF64748B), Color(0xFF94A3B8))
                        ) { navigationEvent = SquadSettingsEvent.RestorePurchase }
                    }
                }

                item(span = { GridItemSpan(2) }) {
                    SectionHeader(text = "Account")
                }

                item {
                    SettingsDashboardCard(
                        title = "Bank Details",
                        subtitle = "UPI Setup",
                        icon = Icons.Default.AccountBalance,
                        gradient = listOf(Color(0xFF8B5CF6), Color(0xFFA78BFA))
                    ) { navigationEvent = SquadSettingsEvent.BankDetails }
                }

                item {
                    SettingsDashboardCard(
                        title = "Squad Activity",
                        subtitle = "Track Insights",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        gradient = listOf(Color(0xFFEC4899), Color(0xFFF472B6))
                    ) { navigationEvent = SquadSettingsEvent.Activity }
                }

                item {
                    SettingsDashboardCard(
                        title = "Payment History",
                        subtitle = "Transactions",
                        icon = Icons.Default.History,
                        gradient = listOf(Color(0xFF6366F1), Color(0xFF818CF8))
                    ) { navigationEvent = SquadSettingsEvent.PaymentHistory }
                }

                item {
                    SettingsDashboardCard(
                        title = "Squad Rules",
                        subtitle = "Policies & Limits",
                        icon = Icons.AutoMirrored.Filled.Rule,
                        gradient = listOf(Color(0xFF14B8A6), Color(0xFF2DD4BF))
                    ) { navigationEvent = SquadSettingsEvent.ChitRules }
                }

                if (isManager) {

                    item {
                        SettingsDashboardCard(
                            title = "Your Squads",
                            subtitle = "Enable or disable squad access for members",
                            icon = Icons.Default.Person,
                            gradient = listOf(Color(0xFF64748B), Color(0xFF94A3B8))
                        ) {
                            navigationEvent = SquadSettingsEvent.YourSquads
                        }
                    }
                }


                item(span = { GridItemSpan(2) }) {
                    LogoutCard {
                        AlertManager.shared.showAlert(
                            title = SquadStrings.appName,
                            message = "Are you sure you want to logout?",
                            primaryButtonTitle = "LOGOUT",
                            primaryAction = {
                                squadViewModel.logoutUser(navController)
                            },
                            secondaryButtonTitle = "NO",
                            secondaryAction = {}
                        )
                    }
                }
            }

            ContactUsButton(
                onClick = { navigationEvent = SquadSettingsEvent.ContactUs },
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }
    }
}

// =========================================================
//  HERO GREETING CARD
// =========================================================

@Composable
fun HeroGreetingCard(isManager: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        AppColors.primaryBrand,
                        AppColors.primaryBrand.copy(alpha = 0.75f)
                    )
                )
            )
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isManager) "Squad Manager" else "Squad Member",
                    style = AppFont.ibmPlexSans(13, FontWeight.Medium),
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Your account, on your terms",
                    style = AppFont.ibmPlexSans(17, FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// =========================================================
//  SECTION HEADER
// =========================================================

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = AppFont.ibmPlexSans(12, FontWeight.SemiBold),
        color = AppColors.secondaryText.copy(alpha = 0.7f),
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp, start = 2.dp)
    )
}

// =========================================================
//  DASHBOARD CARD (with gradient icon + press animation)
// =========================================================

@Composable
fun SettingsDashboardCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    action: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "cardScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 116.dp)
            .scale(scale)
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.surface)
            .border(
                width = 1.dp,
                color = AppColors.border.copy(alpha = 0.12f),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(
                indication = null,
                interactionSource = interactionSource
            ) { action() }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(gradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(AppColors.primaryBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NorthEast,
                        contentDescription = null,
                        tint = AppColors.secondaryText.copy(alpha = 0.6f),
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = AppFont.ibmPlexSans(15, FontWeight.SemiBold),
                    color = AppColors.headerText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = AppFont.ibmPlexSans(12, FontWeight.Medium),
                    color = AppColors.secondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// =========================================================
//  LOGOUT — full-width, distinct danger styling
// =========================================================

@Composable
fun LogoutCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFEF4444).copy(alpha = 0.08f))
            .border(
                width = 1.dp,
                color = Color(0xFFEF4444).copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFFEF4444).copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Logout",
                style = AppFont.ibmPlexSans(15, FontWeight.SemiBold),
                color = Color(0xFFEF4444)
            )
            Text(
                text = "Sign out securely from this device",
                style = AppFont.ibmPlexSans(12, FontWeight.Medium),
                color = Color(0xFFEF4444).copy(alpha = 0.7f)
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFEF4444).copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
    }
}

// =========================================================
//  CONTACT US — premium bottom bar
// =========================================================

@Composable
fun ContactUsButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {

        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.98f else 1f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
            label = "contactScale"
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .scale(scale)
                .clickable(
                    indication = null,
                    interactionSource = interactionSource
                ) { onClick() }
                .appShadow(
                    style = AppShadows.card,
                    shape = RoundedCornerShape(18.dp)
                ),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    AppColors.primaryBrand,
                                    AppColors.primaryBrand.copy(alpha = 0.7f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Contact Us",
                        style = AppFont.ibmPlexSans(14, FontWeight.SemiBold),
                        color = AppColors.headerText
                    )
                    Text(
                        "We're here to help you",
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

