package com.android.savingssquad.view

// Core Compose
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

// Animations
import androidx.compose.animation.animateContentSize

// ViewModel and Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// For mutableIntStateOf (Compose 1.5+)
import androidx.compose.runtime.mutableIntStateOf

// Optional (if using custom colors/shadows)
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.appShadow

// Your own composables
import com.android.savingssquad.view.SSAlert
import com.android.savingssquad.view.SSLoaderView
import com.android.savingssquad.view.AppBackgroundGradient
//import com.android.savingssquad.view.MemberHomeView
//import com.android.savingssquad.view.MemberPaymentView
import com.android.savingssquad.view.CashfreePaymentView

// ViewModels & Managers
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.viewmodel.LoaderManager

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.android.savingssquad.singleton.AppFont

@Composable
fun MemberTabView(
    navController: NavController,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager = LoaderManager.shared
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }  // 0 = Home, 1 = Pay

    // Shared State from ViewModel
    val showPayment by squadViewModel.showPayment.collectAsState()
    val paymentOrderId by squadViewModel.paymentOrderId.collectAsState()
    val paymentOrderToken by squadViewModel.paymentOrderToken.collectAsState()
    val squadState by squadViewModel.squad.collectAsState()
    val squad = squadState

    Box(modifier = Modifier.fillMaxSize()) {

        // Background Gradient
        AppBackgroundGradient()

        Column(modifier = Modifier.fillMaxSize()) {

            // --------- TAB CONTENT AREA (TAKES REMAINING HEIGHT) ----------
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> MemberHomeView(
                        selectedTab = selectedTab,
                        onChangeTab = { selectedTab = it },
                        navController = navController,
                        squadViewModel = squadViewModel
                    )
                    1 -> MemberPaymentView(navController, squadViewModel = squadViewModel)
                }
            }

            // --------- CUSTOM TAB BAR ----------
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
                    .padding(top = 6.dp),
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
            }
        }

        // Global Components
        SSAlert()
        SSLoaderView()

        // -------- PAYMENT OVERLAY ----------
        if (showPayment && squad != null) {
            CashfreePaymentView(
                orderId = paymentOrderId,
                paymentSessionId = paymentOrderToken,
                squadId = squad.squadID,
                onSuccess = { orderId ->
                    println("✅ Payment Success: $orderId")
                    squadViewModel.setShowPayment(false)
                },
                onFailure = { error ->
                    println("❌ Payment Failed: $error")
                    squadViewModel.setShowPayment(false)
                }
            )
        }
    }

    // -------- INITIAL DATA FETCH (ONCE) --------
    LaunchedEffect(Unit) {
        squadViewModel.fetchSquadByID(showLoader = true) { success, _, _ ->
            if (success) {
                squadViewModel.fetchEMIConfigurations(showLoader = true) { _, _ -> }
            }
        }
    }
}

@Composable
fun TabBarItem(
    iconName: String,
    title: String,
    index: Int,
    selectedTab: Int,
    isCenter: Boolean = false,
    onClick: () -> Unit
) {
    val selected = index == selectedTab

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clickable(
                indication = null,       // 🔥 No ripple – matches SwiftUI
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(
                top = if (isCenter) 0.dp else 2.dp,
                bottom = if (isCenter) 0.dp else 4.dp
            )
    ) {

        // ICON
        AppIconView(
            name = iconName,
            tint = if (selected) AppColors.primaryButton else AppColors.secondaryText,
            size = if (isCenter) 40.dp else 22.dp   // 🔥 Matches SwiftUI center pulse
        )

        Spacer(modifier = Modifier.height(4.dp))

        // TEXT
        Text(
            text = title,
            style = AppFont.ibmPlexSans(
                size = 12,
                weight = FontWeight.Medium
            ),
            color = if (selected) AppColors.primaryButton else AppColors.secondaryText
        )
    }
}