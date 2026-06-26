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
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.Plan
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.SquadUserType
import com.android.savingssquad.singleton.UserDefaultsManager
import com.android.savingssquad.view.SSAlert
import com.android.savingssquad.view.SSLoaderView
import com.android.savingssquad.viewmodel.AlertManager
import com.android.savingssquad.viewmodel.AppDestination
import com.android.savingssquad.viewmodel.SquadViewModel
import kotlinx.coroutines.launch

@Composable
fun UpgradePlanScreen(
    viewModel: SubscriptionManager = SubscriptionManager.shared,
    squadViewModel: SquadViewModel,
    onDismiss: () -> Unit
) {
    val activity = LocalContext.current as Activity

    val scope = rememberCoroutineScope()

    var selectedPlan by remember { mutableStateOf(Plan.FREE) }
    var expandedPlan by remember { mutableStateOf<Plan?>(null) }
    var enableLoanAddon by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val cfg = viewModel.remoteConfig

    LaunchedEffect(Unit) {
        selectedPlan = viewModel.subscription.plan
        enableLoanAddon = viewModel.subscription.loanAddon
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {

        // MARK: TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = AppColors.headerText
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {

            // HEADER
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

            // TRIAL BANNER
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

            // PLAN CARDS
            PlanCard(
                title = "FREE",
                subtitle = "Core contribution tracking and activity monitoring for small squads (up to 10 members)",
                price = "₹0",
                isSelected = selectedPlan == Plan.FREE,
                isExpanded = expandedPlan == Plan.FREE,
                included = listOf(
                    "Up to 10 Members",
                    "Contribution Tracking",
                    "Payment History",
                    "Activity Logs",
                    "Basic Dashboard"
                ),
                excluded = listOf("Loan Management"),
                onClick = {
                    selectedPlan = Plan.FREE
                    expandedPlan = if (expandedPlan == Plan.FREE) null else Plan.FREE
                }
            )

            PlanCard(
                title = "BASIC",
                subtitle = "Advanced reporting, tracking and insights for growing squads (up to 50 members)",
                price = "₹99/mo",
                isSelected = selectedPlan == Plan.BASIC,
                isExpanded = expandedPlan == Plan.BASIC,
                included = listOf(
                    "Everything in FREE",
                    "Up to 50 Members",
                    "Advanced Reports",
                    "Better Insights",
                    "Faster Management"
                ),
                excluded = listOf("Loan Management"),
                onClick = {
                    selectedPlan = Plan.BASIC
                    expandedPlan = if (expandedPlan == Plan.BASIC) null else Plan.BASIC
                }
            )

            PlanCard(
                title = "BUSINESS",
                subtitle = "Enterprise analytics + full tracking + loan management",
                price = "₹299/mo",
                isSelected = selectedPlan == Plan.BUSINESS,
                isExpanded = expandedPlan == Plan.BUSINESS,
                included = listOf(
                    "Everything in BASIC",
                    "Up to 200+ Members",
                    "Loan Management Included",
                    "Advanced Analytics",
                    "Priority Support"
                ),
                excluded = emptyList(),
                onClick = {
                    selectedPlan = Plan.BUSINESS
                    expandedPlan = if (expandedPlan == Plan.BUSINESS) null else Plan.BUSINESS

                    // ⭐ AUTO ENABLE LOAN
                    enableLoanAddon = true
                }
            )

            Spacer(Modifier.height(12.dp))

            // LOAN ADDON
            if (cfg.addon_loan_enabled && selectedPlan != Plan.BUSINESS) {

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
                        Text(cfg.addon_loan_price)
                    }

                    Text(
                        cfg.addon_loan_tagline,
                        fontSize = 11.sp,
                        color = AppColors.secondaryText
                    )

                    Switch(
                        checked = enableLoanAddon,
                        onCheckedChange = { enableLoanAddon = it },
                        enabled = selectedPlan != Plan.BUSINESS
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {

                    val squadID = squadViewModel.squad.value?.squadID ?: return@Button

                    SubscriptionFirebaseManager.shared.updateSubscription(
                        squadID = squadID,
                        plan = selectedPlan,
                        loanAddon = enableLoanAddon
                    ) { updateSuccess, updateError ->

                        if (updateSuccess) {

                            SubscriptionManager.shared.loadSubscription(
                                squadID
                            ) { _, _ ->

                                squadViewModel.setShowUpgradePlan(false)
                                squadViewModel.setShowUpgradeSuccess(true)
                            }

                        }
                    }

                    /*BillingHelper.startPurchaseFlow(
                        activity = activity,
                        squadID = squadID,
                        selectedPlan = selectedPlan,
                        enableLoanAddon = enableLoanAddon,
                        onLoading = { isLoading = it },
                        onError = { error ->

                        }
                    ) */
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoading) "Loading..." else "Upgrade Now")
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
    included: List<String>,
    excluded: List<String>,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
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
            .clickable { onClick() }
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