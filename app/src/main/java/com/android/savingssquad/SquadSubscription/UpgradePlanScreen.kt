package com.android.savingssquad.SquadSubscription

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.view.SSAlert
import com.android.savingssquad.view.SSLoaderView
import com.android.savingssquad.viewmodel.AlertManager
import com.android.savingssquad.viewmodel.AppDestination
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.viewmodel.ToastManager
import com.android.savingssquad.viewmodel.ToastType
import kotlinx.coroutines.launch

@Composable
fun UpgradePlanScreen(
    viewModel: SubscriptionManager = SubscriptionManager.shared,
    squadViewModel: SquadViewModel,
    onDismiss: () -> Unit
) {

    val activity = LocalContext.current as Activity
    val scope = rememberCoroutineScope()

    val cfg by viewModel.remoteConfig.collectAsState()
    val sub by viewModel.subscription.collectAsState()

    var selectedPlan by remember {
        mutableStateOf(SubscriptionModel.Plan.FREE)
    }

    var expandedPlan by remember {
        mutableStateOf<SubscriptionModel.Plan?>(null)
    }

    var enableLoanAddon by remember {
        mutableStateOf(false)
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    val memberCount = squadViewModel.squad.value?.totalMembers ?: 0
    val forceUpgrade = viewModel.shouldForceUpgrade(memberCount)

    // ---------------------------
    // FORCE UPGRADE HANDLING
    // ---------------------------
    LaunchedEffect(forceUpgrade) {
        if (forceUpgrade) {
            selectedPlan = SubscriptionModel.Plan.BASIC
        }
    }

    // ---------------------------
    // SYNC FROM SUBSCRIPTION
    // ---------------------------
    LaunchedEffect(sub) {

        sub?.let {

            if (!forceUpgrade) {
                selectedPlan = it.plan
            }

            enableLoanAddon = it.loanAddon
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {

        // ---------------- TOP BAR ----------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.Start
        ) {

            if (!forceUpgrade) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = AppColors.headerText
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {

            // ---------------- HEADER ----------------
            Text(
                "Upgrade Your Squad",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.headerText
            )

            Text(
                "Contributions • Loans • Scaling",
                fontSize = 13.sp,
                color = AppColors.secondaryText
            )

            Spacer(Modifier.height(16.dp))

            // ---------------- TRIAL BANNER ----------------
            if (viewModel.isTrialActive()) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.successAccent.copy(alpha = 0.08f))
                        .padding(12.dp)
                ) {

                    Icon(Icons.Default.CardGiftcard, contentDescription = null)

                    Column(modifier = Modifier.padding(start = 8.dp)) {

                        Text(
                            "Free Trial Active",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.successAccent
                        )

                        Text(
                            "${viewModel.trialDaysRemaining()} days remaining",
                            fontSize = 11.sp,
                            color = AppColors.secondaryText
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
            }

            // ---------------- FREE PLAN ----------------
            PlanCard(
                title = "FREE",
                subtitle = "Core tracking for small squads (up to 10 members)",
                price = cfg.free_priceText,
                isSelected = selectedPlan == SubscriptionModel.Plan.FREE,
                isExpanded = expandedPlan == SubscriptionModel.Plan.FREE,
                enabled = !forceUpgrade,
                included = listOf(
                    "Up to 10 Members",
                    "Contribution Tracking",
                    "Payment History",
                    "Activity Logs",
                    "Basic Dashboard"
                ),
                excluded = listOf("Loan Management"),
                onClick = {
                    selectedPlan = SubscriptionModel.Plan.FREE
                    expandedPlan =
                        if (expandedPlan == SubscriptionModel.Plan.FREE) null
                        else SubscriptionModel.Plan.FREE
                }
            )

            // ---------------- BASIC PLAN ----------------
            PlanCard(
                title = "BASIC",
                subtitle = "Advanced tracking for growing squads (up to 50 members)",
                price = cfg.basic_priceText,
                isSelected = selectedPlan == SubscriptionModel.Plan.BASIC,
                isExpanded = expandedPlan == SubscriptionModel.Plan.BASIC,
                included = listOf(
                    "Everything in FREE",
                    "Up to 50 Members",
                    "Advanced Reports",
                    "Better Insights",
                    "Faster Management"
                ),
                excluded = listOf("Loan Management"),
                onClick = {
                    selectedPlan = SubscriptionModel.Plan.BASIC
                    expandedPlan =
                        if (expandedPlan == SubscriptionModel.Plan.BASIC) null
                        else SubscriptionModel.Plan.BASIC
                }
            )

            // ---------------- BUSINESS PLAN ----------------
            PlanCard(
                title = "BUSINESS",
                subtitle = "Full tracking + loans + analytics",
                price = cfg.biz_priceText,
                isSelected = selectedPlan == SubscriptionModel.Plan.BUSINESS,
                isExpanded = expandedPlan == SubscriptionModel.Plan.BUSINESS,
                included = listOf(
                    "Everything in BASIC",
                    "Up to 200+ Members",
                    "Loan Management Included",
                    "Advanced Analytics",
                    "Priority Support"
                ),
                excluded = emptyList(),
                onClick = {
                    selectedPlan = SubscriptionModel.Plan.BUSINESS
                    expandedPlan =
                        if (expandedPlan == SubscriptionModel.Plan.BUSINESS) null
                        else SubscriptionModel.Plan.BUSINESS

                    enableLoanAddon = true
                }
            )

            Spacer(Modifier.height(12.dp))

            // ---------------- LOAN ADDON ----------------
            if (cfg.addon_loan_enabled &&
                selectedPlan != SubscriptionModel.Plan.BUSINESS
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.surface, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Loan Add-On")
                        Text(cfg.addon_loan_priceText)
                    }

                    Text(
                        cfg.addon_loan_tagline,
                        fontSize = 11.sp,
                        color = AppColors.secondaryText
                    )

                    Switch(
                        checked = enableLoanAddon,
                        onCheckedChange = { enableLoanAddon = it },
                        enabled = selectedPlan != SubscriptionModel.Plan.BUSINESS
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ---------------- ACTION BUTTON ----------------
            Button(
                onClick = {

                    val squadID = squadViewModel.squad.value?.squadID ?: return@Button

                    BillingHelper.startPurchaseFlow(
                        activity = activity,
                        selectedPlan = selectedPlan,
                        enableLoanAddon = enableLoanAddon,
                        squadID = squadID,

                        onLoading = {
                            isLoading = it
                        },

                        onError = { error ->
                            ToastManager.show(
                                title = SquadStrings.appName,
                                message = error,
                                type = ToastType.ERROR
                            )
                        },

                        onSuccess = {

                            squadViewModel.setShowUpgradePlan(false)
                            squadViewModel.setShowUpgradeSuccess(true)
                        }
                    )
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Upgrade Now")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun PlanCard(
    title: String,
    subtitle: String,
    price: String,
    isSelected: Boolean,
    isExpanded: Boolean,
    enabled: Boolean = true,
    included: List<String>,
    excluded: List<String>,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier

            .fillMaxWidth()

            .alpha(if (enabled) 1f else 0.45f)

            .padding(vertical = 8.dp)

            .background(

                if (isSelected) AppColors.primaryBackground else AppColors.surface,

                RoundedCornerShape(18.dp)

            )

            .border(

                1.dp,

                if (isSelected) AppColors.primaryBrand else AppColors.border,

                RoundedCornerShape(18.dp)

            )

            .clickable(enabled = enabled) {

                onClick()

            }

            .padding(14.dp)
    ) {

        Row {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, fontSize = 12.sp, color = AppColors.secondaryText)
            }

            Text(price, color = AppColors.primaryBrand)
        }

        Spacer(Modifier.height(6.dp))

        Row {
            Text(
                if (isExpanded) "Hide details" else "View details",
                fontSize = 12.sp,
                color = AppColors.primaryBrand
            )
        }

        if (isExpanded) {

            Spacer(Modifier.height(10.dp))

            Divider()

            Column(Modifier.padding(top = 8.dp)) {

                included.forEach {
                    FeatureRow(it, true)
                }

                excluded.forEach {
                    FeatureRow(it, false)
                }
            }
        }
    }
}

@Composable
fun FeatureRow(text: String, included: Boolean) {

    Row(modifier = Modifier.padding(vertical = 2.dp)) {

        Icon(
            imageVector = if (included) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (included) AppColors.successAccent else AppColors.errorAccent
        )

        Spacer(Modifier.width(6.dp))

        Text(
            text,
            fontSize = 12.sp,
            color = AppColors.secondaryText
        )
    }
}