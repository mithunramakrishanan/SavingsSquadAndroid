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
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.android.savingssquad.singleton.AppFont

@Composable
fun MemberTabView(
    squadViewModel: SquadViewModel = SquadViewModel(),
    loaderManager: LoaderManager = LoaderManager.shared
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Home, 1 = Pay

    // 🧩 Collect states once at top (no duplicate collectors below)
    val showPayment by squadViewModel.showPayment.collectAsState()
    val paymentOrderId by squadViewModel.paymentOrderId.collectAsState()
    val paymentOrderToken by squadViewModel.paymentOrderToken.collectAsState()
    val groupFundState by squadViewModel.groupFund.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.primaryBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ✅ TabView equivalent
            when (selectedTab) {
                0 -> MemberHomeView(
                    memberSelectedTab = selectedTab,
                    squadViewModel = squadViewModel,
                    loaderManager = loaderManager
                )

                1 -> MemberPaymentView()
            }

            // ✅ Custom Tab Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.surface)
                    .appShadow(AppShadows.card)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
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
                    isCenter = true,
                    selectedTab = selectedTab,
                    onClick = { selectedTab = 1 }
                )
            }
        }

        // ✅ Global overlays
        SSAlert()
        SSLoaderView(true)
        val groupFund = groupFundState
        // ✅ Payment overlay
        if (showPayment && groupFund != null) {
            CashfreePaymentView(
                orderId = paymentOrderId,
                paymentSessionId = paymentOrderToken,
                groupFundId = groupFund.groupFundID,
                onSuccess = { orderID ->
                    println("✅ Payment Success for order: $orderID")
                    squadViewModel.setShowPayment(false)
                },
                onFailure = { errorMsg ->
                    println("❌ Payment Failed: $errorMsg")
                    squadViewModel.setShowPayment(false)
                }
            )
        }
    }

    // ✅ Equivalent to SwiftUI `.task {}` (runs once)
    LaunchedEffect(Unit) {
        squadViewModel.fetchGroupFundByID(showLoader = true) { success, groupFund, _ ->
            if (success) {
                squadViewModel.fetchEMIConfigurations(showLoader = true) { _, _ -> }
            }
        }
    }
}

@Composable
fun MemberPaymentView() {
    TODO("Not yet implemented")
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
            .clickable { onClick() }
            .padding(vertical = if (isCenter) 6.dp else 0.dp)
    ) {
        AppIconView(
            name = iconName,
            tint = if (selected) AppColors.primaryButton else AppColors.secondaryText,
            size = if (isCenter) 28.dp else 22.dp
        )

        Text(
            text = title,
            style = AppFont.ibmPlexSans(12, FontWeight.Medium),
            color = if (selected) AppColors.primaryButton else AppColors.secondaryText
        )
    }
}