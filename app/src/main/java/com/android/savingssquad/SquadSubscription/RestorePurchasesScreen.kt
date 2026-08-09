package com.android.savingssquad.SquadSubscription

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.view.AppBackgroundGradient
import com.android.savingssquad.viewmodel.SquadViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun RestorePurchasesScreen(
    navController: NavController,
    squadViewModel: SquadViewModel,
    viewModel: SubscriptionManager = SubscriptionManager.shared
) {

    var isRestoring by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }
    var resultSuccess by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf("") }

    val sub by viewModel.subscription.collectAsState()


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

            // NAV BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = AppColors.headerText
                    )
                }

                Text(
                    SquadStrings.restorePurchases,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 18.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                Spacer(Modifier.height(10.dp))

                // HEADER CARD
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.surface, RoundedCornerShape(20.dp))
                        .border(1.dp, AppColors.border, RoundedCornerShape(20.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(AppColors.primaryBrand.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            tint = AppColors.primaryBrand,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        SquadStrings.restorePurchases,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        SquadStrings.restorePurchasesHeaderDescription,
                        fontSize = 14.sp,
                        color = AppColors.secondaryText,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(16.dp))

                // CURRENT PLAN CARD
                sub?.let {
                    CurrentPlanCard(it, viewModel)
                }

                Spacer(Modifier.height(12.dp))

                // INFO CARDS
                InfoCard()

                Spacer(Modifier.height(12.dp))

                // RESULT BANNER
                if (showResult) {
                    ResultBanner(resultSuccess, resultMessage)
                }

                Spacer(Modifier.height(20.dp))
            }

            // BUTTONS
            Column(

                modifier = Modifier

                    .weight(1f)

                    .padding(horizontal = 18.dp)

                    .verticalScroll(rememberScrollState())

            ) {
                Button(
                    onClick = {
                        val squadID = squadViewModel.squad.value?.squadID
                            ?: return@Button

                        isRestoring = true
                        showResult = false

                        SubscriptionManager.shared.restorePurchases(squadID) { success, error ->

                            isRestoring = false
                            resultSuccess = success

                            resultMessage =
                                if (success) {
                                    SquadStrings.restoreSuccessMessage(
                                        viewModel.subscription.value?.plan?.localizedName ?: ""
                                    )
                                } else {
                                    error ?: SquadStrings.restoreFailedMessage
                                }

                            showResult = true

                            if (success) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    delay(2000.milliseconds)
                                    navController.popBackStack()
                                }
                            }
                        }
                    },
                    enabled = !isRestoring,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = if (isRestoring)
                            "Restoring..."
                        else
                            SquadStrings.restorePurchases
                    )
                }
            }
        }

    }


}

@Composable
fun CurrentPlanCard(
    sub: SubscriptionModel,
    viewModel: SubscriptionManager
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.surface, RoundedCornerShape(16.dp))
            .border(1.dp, AppColors.border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(46.dp)
                .background(AppColors.primaryBrand.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CreditCard, null, tint = AppColors.primaryBrand)
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {

            Text(SquadStrings.currentPlan(""), fontSize = 12.sp, color = AppColors.secondaryText)

            Text(
                sub.plan.localizedName,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (viewModel.isTrialActive()) {
                Text(
                    SquadStrings.freeTrialActive,
                    fontSize = 12.sp,
                    color = AppColors.successAccent
                )
            }
        }

        Box(
            modifier = Modifier
                .background(
                    when (sub.plan) {
                        SubscriptionModel.Plan.FREE -> AppColors.secondaryText
                        SubscriptionModel.Plan.BASIC -> AppColors.infoAccent
                        SubscriptionModel.Plan.BUSINESS -> AppColors.primaryBrand
                    },
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                sub.plan.localizedName,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun InfoCard() {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.surface, RoundedCornerShape(16.dp))
            .border(1.dp, AppColors.border, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {

        InfoRow(Icons.Default.Shield, AppColors.successAccent,
            SquadStrings.safeSecureTitle,
            SquadStrings.safeSecureSubtitle
        )

        InfoRow(Icons.Default.Cloud, AppColors.infoAccent,
            SquadStrings.syncAcrossDevicesTitle,
            SquadStrings.syncAcrossDevicesSubtitle
        )

        InfoRow(Icons.Default.Schedule, AppColors.warningAccent,
            SquadStrings.previousPurchasesTitle,
            SquadStrings.previousPurchasesSubtitle
        )
    }
}

@Composable
fun InfoRow(
    icon: ImageVector,
    color: Color,
    title: String,
    subtitle: String
) {

    Row(modifier = Modifier.padding(vertical = 8.dp)) {

        Box(
            modifier = Modifier
                .size(38.dp)
                .background(color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color)
        }

        Spacer(Modifier.width(10.dp))

        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 12.sp, color = AppColors.secondaryText)
        }
    }
}

@Composable
fun ResultBanner(success: Boolean, message: String) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.surface, RoundedCornerShape(14.dp))
            .border(
                1.dp,
                if (success) AppColors.successAccent else AppColors.errorAccent,
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    (if (success) AppColors.successAccent else AppColors.errorAccent)
                        .copy(alpha = 0.15f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (success) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (success) AppColors.successAccent else AppColors.errorAccent
            )
        }

        Spacer(Modifier.width(10.dp))

        Column {
            Text(
                if (success) SquadStrings.restoreSuccessful else SquadStrings.restoreFailed,
                fontWeight = FontWeight.SemiBold
            )

            Text(message, fontSize = 12.sp, color = AppColors.secondaryText)
        }
    }
}