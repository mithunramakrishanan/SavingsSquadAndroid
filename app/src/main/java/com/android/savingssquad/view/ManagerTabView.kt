package com.android.savingssquad.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp

import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.viewmodel.LoaderManager
import com.android.savingssquad.viewmodel.SquadViewModel
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.android.savingssquad.SquadSubscription.SubscriptionManager
import com.android.savingssquad.SquadSubscription.UpgradePlanScreen
import com.android.savingssquad.SquadSubscription.UpgradeSuccessScreen
import com.android.savingssquad.viewmodel.SSToast

@Composable
fun ManagerTabView(
    navController: NavController,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    // 🔹 Shared State
    val showPayment by squadViewModel.showPayment.collectAsState()
    val showUpgradePlan by squadViewModel.showUpgradePlan.collectAsState()
    val showUpgradeSuccess by squadViewModel.showUpgradeSuccess.collectAsState()
    val paymentOrderId by squadViewModel.paymentOrderId.collectAsState()
    val squadState by squadViewModel.squad.collectAsState()
    val squad = squadState

    Box(modifier = Modifier.fillMaxSize()) {
        AppBackgroundGradient()

        Column(modifier = Modifier.fillMaxSize()) {

            // ✔ Tab content must take remaining space (NOT full screen)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> ManagerHomeView(
                        navController,
                        squadViewModel,
                        loaderManager
                    )
                    1 -> ManagerPaymentView(navController, squadViewModel)
                    2 -> ManagerSettingsView(navController, squadViewModel)
                }
            }

            // ✔ Bottom Tab Bar stays fixed
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .shadow(
                        elevation = 4.dp,
                        spotColor = AppColors.primaryButton,
                        ambientColor = AppColors.primaryButton
                    )
                    .background(AppColors.surface)
                    .padding(top = 6.dp),   // 🔥 Added top padding
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabBarItem(
                    iconName = "home_icon",
                    title = "Home",
                    index = 0,
                    selectedTab = selectedTab,
                    onClick = { selectedTab = 0 }
                )

                TabBarItem(
                    iconName = "pay_icon",
                    title = "Pay",
                    index = 1,
                    selectedTab = selectedTab,
                    isCenter = true,
                    onClick = { selectedTab = 1 }
                )

                TabBarItem(
                    iconName = "settings_icon",
                    title = "Account",
                    index = 2,
                    selectedTab = selectedTab,
                    onClick = { selectedTab = 2 }
                )
            }
        }

        // ✅ Global Overlays
        SSAlert()
        SSLoaderView()
        SSToast()
        // ✅ Payment overlay
        if (showPayment && squad != null) {
            RazorpayPaymentView (
                orderId = paymentOrderId,
                squadId = squad.squadID,
                onSuccess = { orderId ->
                    println("✅ Payment Success for order: $orderId")
                    squadViewModel.setShowPayment(false)
                },
                onFailure = { error ->
                    println("❌ Payment Failed: $error")
                    squadViewModel.setShowPayment(false)
                }
            )
        }

        if (showUpgradePlan && squad != null) {
            UpgradePlanScreen(squadViewModel = squadViewModel, onDismiss = {
                squadViewModel.setShowUpgradePlan(false)
            })
        }

        if (showUpgradeSuccess && squad != null) {
            UpgradeSuccessScreen(plan = SubscriptionManager.shared.subscription.plan) {
                squadViewModel.setShowUpgradeSuccess(false)
            }
        }
    }

    // ✅ Load data once
    LaunchedEffect(Unit) {
        squadViewModel.fetchSquadByID(showLoader = true) { success, _, _ ->
            if (success) {
                squadViewModel.fetchEMIConfigurations(showLoader = true) { _, _ -> }
            }
        }
    }
}

@Composable
fun AppIconView(
    name: String,
    tint: Color,
    size: Dp
) {
    // Try to load from drawable resources first
    val context = LocalContext.current
    val resourceId = remember(name) {
        context.resources.getIdentifier(name, "drawable", context.packageName)
    }

    if (resourceId != 0) {
        Icon(
            painter = painterResource(id = resourceId),
            contentDescription = name,
            tint = Color.Unspecified,
            modifier = Modifier.size(size)
        )
    } else {
        // Fallback system icon if not found
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = name,
            tint = Color.Unspecified,
            modifier = Modifier.size(size)
        )
    }
}