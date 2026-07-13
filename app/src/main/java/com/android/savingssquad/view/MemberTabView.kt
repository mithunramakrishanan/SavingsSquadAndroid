package com.android.savingssquad.view

// Core Compose
import android.os.Build
import androidx.annotation.RequiresApi
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

// ViewModels & Managers
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.singleton.LoaderManager

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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.android.savingssquad.R
import com.android.savingssquad.SquadSubscription.SubscriptionManager
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.viewmodel.SSToast

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MemberTabView(
    navController: NavController,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager = LoaderManager.shared
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }  // 0 = Home, 1 = Pay

    // Shared State from ViewModel
    val showPayment by squadViewModel.showPayment.collectAsState()
    val showUpgradePlan by squadViewModel.showUpgradePlan.collectAsState()
    val paymentOrderId by squadViewModel.paymentOrderId.collectAsState()
    val squadState by squadViewModel.squad.collectAsState()
    val squad = squadState


    LaunchedEffect(Unit) {

        UserDefaultsManager.getLogin()?.let { login ->

            squadViewModel.startObservers(login.squadID)
            squadViewModel.fetchSquadByID(showLoader = true) { success, _, _ ->
                LoaderManager.shared.hideLoader()
                println(if (success) "✅ Squad re-fetched on update" else "❌ Re-fetch failed")
            }
        }
    }

    Box(
        modifier = Modifier

            .fillMaxSize()

            .windowInsetsPadding(WindowInsets.safeDrawing)
    )
    {

        AppBackgroundGradient()

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

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

                    2 -> SquadSettingsView(
                        navController = navController,
                        squadViewModel = squadViewModel
                    )
                }
            }

            // MARK: Premium Bottom Tab Bar
            Box(
                modifier = Modifier
                    .padding(horizontal = 0.dp)
                    .padding(bottom = 0.dp)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(24.dp),
                            clip = false
                        )
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(vertical = 12.dp),
                ) {

                    TabBarItem(
                        iconName = "home_icon",
                        title = "Home",
                        index = 0,
                        selectedTab = selectedTab,
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedTab = 0
                    }

                    Spacer(
                        modifier = Modifier.width(60.dp)
                    )

                    TabBarItem(
                        iconName = "settings_icon",
                        title = "Account",
                        index = 2,
                        selectedTab = selectedTab,
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedTab = 2
                    }
                }

                // Floating Pay Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-18).dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember {
                                MutableInteractionSource()
                            }
                        ) {
                            selectedTab = 1
                        }
                ) {

                    Box(
                        contentAlignment = Alignment.Center
                    ) {

                        // Glow
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            AppColors.primaryButton.copy(alpha = 0.35f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .blur(6.dp)
                        )

                        // Main Circle Button
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .shadow(
                                    elevation = 12.dp,
                                    shape = CircleShape
                                )
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            AppColors.primaryButton,
                                            AppColors.successAccent
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {

                            Image(
                                painter = painterResource(R.drawable.pay_icon),
                                contentDescription = "Pay",
                                modifier = Modifier.size(58.dp)
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(2.dp)
                    )

                    Text(
                        text = "Pay",
                        style = AppFont.ibmPlexSans(
                            size = 12,
                            weight = FontWeight.SemiBold
                        ),
                        color = if (selectedTab == 1)
                            AppColors.headerText
                        else
                            AppColors.secondaryText
                    )
                }
            }
        }
        if (showPayment && squad != null) {
            RazorpayPaymentView(
                orderId = paymentOrderId,
                squadId = squad!!.squadID,
                onSuccess = {
                    squadViewModel.setShowPayment(false)
                },
                onFailure = {
                    squadViewModel.setShowPayment(false)
                }
            )
        }
    }
}