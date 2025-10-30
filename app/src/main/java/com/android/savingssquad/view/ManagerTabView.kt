package com.android.savingssquad.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp

import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.appShadow
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.view.AppBackgroundGradient
import com.android.savingssquad.view.CashfreePaymentView
import com.android.savingssquad.view.SSAlert
import com.android.savingssquad.view.SSLoaderView
import com.android.savingssquad.viewmodel.AlertManager
import com.android.savingssquad.viewmodel.LoaderManager
import com.android.savingssquad.viewmodel.SquadViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.android.savingssquad.view.ManagerHomeView
import com.android.savingssquad.view.ManagerPaymentView
import com.android.savingssquad.view.ManagerSettingsView

@Composable
fun ManagerTabView(
    loaderManager: LoaderManager = LoaderManager.shared,
    squadViewModel: SquadViewModel = SquadViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AppBackgroundGradient()

        Column(modifier = Modifier.fillMaxSize()) {

            // 🔹 Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> ManagerHomeView()
                    1 -> ManagerPaymentView()
                    2 -> ManagerSettingsView()
                }
            }

            // 🔹 Custom Flat Tab Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.surface)
                    .shadow(
                        elevation = 4.dp,
                        spotColor = AppColors.primaryButton,
                        ambientColor = AppColors.primaryButton
                    )
                    .padding(vertical = 12.dp),
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

        // 🔹 Global Overlays
        SSAlert()
        SSLoaderView(true)

        // 🔹 Payment Overlay (like fullScreenCover)

        val showPayment by squadViewModel.showPayment.collectAsState()
        val paymentOrderId by squadViewModel.paymentOrderId.collectAsState()
        val paymentOrderToken by squadViewModel.paymentOrderToken.collectAsState()
        val groupFundState by squadViewModel.groupFund.collectAsState()
        val groupFund = groupFundState

        if (showPayment) {
            if (groupFund != null) {
                CashfreePaymentView (
                    orderId = paymentOrderId,
                    paymentSessionId = paymentOrderToken,
                    groupFundId = groupFund.groupFundID,
                    onSuccess = { orderId ->
                        println("✅ Payment Success for order: $orderId")
                        squadViewModel.setShowPayment(true)
                    },
                    onFailure = { error ->
                        println("❌ Payment Failed: $error")
                        squadViewModel.setShowPayment(false)
                    }
                )
            }
        }
    }

    // 🔹 Initial Data Load
    LaunchedEffect(Unit) {
        squadViewModel.fetchGroupFundByID(showLoader = true) { success, _, _ ->
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
            tint = tint,
            modifier = Modifier.size(size)
        )
    } else {
        // Fallback system icon if not found
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = name,
            tint = tint,
            modifier = Modifier.size(size)
        )
    }
}