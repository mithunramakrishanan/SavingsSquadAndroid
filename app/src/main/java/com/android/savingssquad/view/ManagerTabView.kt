package com.android.savingssquad.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp

import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.viewmodel.LoaderManager
import com.android.savingssquad.viewmodel.SquadViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.android.savingssquad.R
import com.android.savingssquad.SquadSubscription.SubscriptionManager
import com.android.savingssquad.SquadSubscription.UpgradePlanScreen
import com.android.savingssquad.SquadSubscription.UpgradeSuccessScreen
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.viewmodel.AppDestination
import com.android.savingssquad.viewmodel.SSToast

@Composable
fun ManagerTabView(
    navController: NavController,
    squadViewModel: SquadViewModel,
    loaderManager: LoaderManager
) {

    var selectedTab by rememberSaveable { mutableStateOf(0) }

    val showPayment by squadViewModel.showPayment.collectAsState()
    val showUpgradePlan by squadViewModel.showUpgradePlan.collectAsState()
    val showUpgradeSuccess by squadViewModel.showUpgradeSuccess.collectAsState()
    val paymentOrderId by squadViewModel.paymentOrderId.collectAsState()
    val squad by squadViewModel.squad.collectAsState()

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
        )
        {

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {

                    0 -> ManagerHomeView(
                        navController = navController,
                        squadViewModel = squadViewModel,
                        loaderManager = loaderManager
                    )

                    1 -> ManagerPaymentView(
                        navController = navController,
                        squadViewModel = squadViewModel
                    )

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

        if (showUpgradePlan) {

            Box(

                modifier = Modifier

                    .fillMaxSize()

            ) {

                // 👇 THIS blocks background clicks

                Box(

                    modifier = Modifier

                        .fillMaxSize()

                        .background(Color.Black.copy(alpha = 0.5f))

                        .clickable(

                            indication = null,

                            interactionSource = remember { MutableInteractionSource() }

                        ) {

                            // consume clicks (do nothing)

                        }

                )

                UpgradePlanScreen(

                    squadViewModel = squadViewModel

                ) {

                    squadViewModel.setShowUpgradePlan(false)

                }

            }
        }

        val sub by SubscriptionManager.shared.subscription.collectAsState()

        if (showUpgradeSuccess && sub != null) {

            val plan = sub?.plan ?: return

            UpgradeSuccessScreen(
                plan = plan,
                viewModel = SubscriptionManager.shared
            ) {
                squadViewModel.setShowUpgradeSuccess(false)
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    val isSelected = selectedTab == index

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(
            indication = null,
            interactionSource = remember {
                MutableInteractionSource()
            }
        ) {
            onClick()
        }
    ) {

        AppIconView(
            name = iconName,
            tint = if (isSelected)
                AppColors.primaryButton
            else
                AppColors.secondaryText,
            size = 24.dp
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = title,
            style = AppFont.ibmPlexSans(
                size = 11,
                weight = FontWeight.Medium
            ),
            color = if (isSelected)
                AppColors.headerText
            else
                AppColors.secondaryText
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(
                        AppColors.primaryButton,
                        CircleShape
                    )
            )
        } else {
            Spacer(
                modifier = Modifier.height(5.dp)
            )
        }
    }
}

@Composable
fun AppIconView(
    name: String,
    tint: Color,
    size: Dp
) {

    val context = LocalContext.current

    val resourceId = remember(name) {
        context.resources.getIdentifier(
            name,
            "drawable",
            context.packageName
        )
    }

    if (resourceId != 0) {

        Icon(
            painter = painterResource(resourceId),
            contentDescription = name,
            tint = tint,
            modifier = Modifier.size(size)
        )

    } else {

        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = name,
            tint = tint,
            modifier = Modifier.size(size)
        )
    }
}

