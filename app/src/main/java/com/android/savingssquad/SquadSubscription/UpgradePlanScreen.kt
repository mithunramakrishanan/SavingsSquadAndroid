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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.viewmodel.CashfreePaymentManager
import com.android.savingssquad.viewmodel.SquadViewModel
import com.android.savingssquad.viewmodel.ToastManager
import com.android.savingssquad.viewmodel.ToastType

import com.google.firebase.functions.FirebaseFunctions

import com.google.firebase.Timestamp

import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine

import kotlin.coroutines.resume

private const val UPGRADE_TAG = "UpgradePlanScreen"

// MARK: - Upgrade Plan Screen

@Composable
fun UpgradePlanScreen(
    viewModel: SubscriptionManager = SubscriptionManager.shared,
    squadViewModel: SquadViewModel,
    onDismiss: () -> Unit
) {

    val context = LocalContext.current
    val activity = context as? Activity

    val cfg by viewModel.remoteConfig.collectAsState()
    val sub by viewModel.subscription.collectAsState()
    val squad by squadViewModel.squad.collectAsState()

    var selectedPlan by remember {
        mutableStateOf(SubscriptionModel.Plan.BASIC)
    }

    var selectedPeriod by remember {
        mutableStateOf(
            SubscriptionModel.BillingPeriod.MONTHLY
        )
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

    var disableUpgradeButton by remember {
        mutableStateOf(false)
    }

    val squadID = squad?.squadID
    val memberCount = squad?.totalMembers ?: 0

    val forceUpgrade =
        viewModel.shouldForceUpgrade(memberCount)

    // MARK: - Force Upgrade

    LaunchedEffect(forceUpgrade) {
        if (forceUpgrade) {
            selectedPlan = SubscriptionModel.Plan.BASIC
        }

        sub?.let { subscription ->

            disableUpgradeButton = shouldDisableUpgradeButton(
                selectedPlan = selectedPlan,
                selectedPeriod = selectedPeriod,
                currentPlan = subscription.plan,
                currentPeriod = subscription.billingPeriod,
                subscriptionStatus = subscription.subscriptionStatus
            )
        }
    }

    // MARK: - Synchronize Existing Subscription

    LaunchedEffect(sub) {
        sub?.let { subscription ->

            if (!forceUpgrade) {
                selectedPlan = subscription.plan
            }

            selectedPeriod = subscription.billingPeriod
            enableLoanAddon = subscription.loanAddon
        }
    }

    // MARK: - Cashfree Payment Events

    LaunchedEffect(squadID) {

        CashfreePaymentManager.paymentEvents.collect { event ->

            when (event) {

                is CashfreePaymentManager.PaymentEvent.VerificationRequested -> {

                    Log.d(
                        UPGRADE_TAG,
                        "Payment verification requested: ${event.orderID}"
                    )

                    val currentSquadID = squadID

                    if (currentSquadID.isNullOrBlank()) {

                        isLoading = false

                        showUpgradeFailure(
                            SquadStrings.squadNotFound
                        )

                        CashfreePaymentManager
                            .completePaymentVerification(
                                success = false
                            )

                        return@collect
                    }

                    /*
                     * The backend/webhook must verify the specific
                     * Cashfree order and activate its subscription.
                     *
                     * This refresh checks that the resulting subscription
                     * is active, unexpired, and matches the selected plan.
                     */

                    val verified = refreshSubscriptionAfterPayment(
                        viewModel = viewModel,
                        squadID = currentSquadID,
                        expectedPlan = selectedPlan,
                        expectedPeriod = selectedPeriod
                    )

                    CashfreePaymentManager
                        .completePaymentVerification(
                            success = verified
                        )
                }

                is CashfreePaymentManager.PaymentEvent.PaymentFailed -> {

                    Log.e(
                        UPGRADE_TAG,
                        "Cashfree payment failed: ${event.message}"
                    )

                    isLoading = false

                    showUpgradeFailure(
                        event.message
                    )
                }
            }
        }
    }

    // MARK: - Main UI

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {

        // MARK: - Top Bar

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.Start
        ) {

            if (!forceUpgrade) {

                IconButton(
                    onClick = onDismiss
                ) {

                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close upgrade screen",
                        tint = AppColors.headerText
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 18.dp)
                .verticalScroll(
                    rememberScrollState()
                )
        ) {

            // MARK: - Header

            Text(
                text = SquadStrings.upgradeYourSquad,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.headerText
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = SquadStrings.contributionsLoansScaling,
                fontSize = 13.sp,
                color = AppColors.secondaryText
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            // MARK: - Trial Banner

            if (viewModel.isTrialActive()) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            AppColors.successAccent.copy(
                                alpha = 0.08f
                            ),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null,
                        tint = AppColors.successAccent
                    )

                    Column(
                        modifier = Modifier.padding(
                            start = 8.dp
                        )
                    ) {

                        Text(
                            text = SquadStrings.freeTrialActive,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.successAccent
                        )

                        Text(
                            text = SquadStrings.trialDaysRemaining(
                                viewModel
                                    .trialDaysRemaining()
                                    .toString()
                            ),
                            fontSize = 11.sp,
                            color = AppColors.secondaryText
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }

            // MARK: - Billing Period Picker

            if (selectedPlan != SubscriptionModel.Plan.FREE) {

                BillingPeriodPicker(
                    selectedPlan = selectedPlan,
                    selectedPeriod = selectedPeriod,
                    cfg = cfg,
                    onSelect = {
                        selectedPeriod = it


                        sub?.let { subscription ->

                            disableUpgradeButton = shouldDisableUpgradeButton(
                                selectedPlan = selectedPlan,
                                selectedPeriod = selectedPeriod,
                                currentPlan = subscription.plan,
                                currentPeriod = subscription.billingPeriod,
                                subscriptionStatus = subscription.subscriptionStatus
                            )
                        }

                    }
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )
            }

            // MARK: - Basic Plan

            PlanCard(
                title = SquadStrings.basicPlan,
                subtitle = SquadStrings.basicPlanDescription,
                price = cfg.priceText(
                    SubscriptionModel.Plan.BASIC,
                    selectedPeriod
                ),
                isSelected =
                    selectedPlan == SubscriptionModel.Plan.BASIC,
                isExpanded =
                    expandedPlan == SubscriptionModel.Plan.BASIC,
                included = listOf(
                    SquadStrings.everythingInFree,
                    SquadStrings.upTo50Members,
                    SquadStrings.advancedReports,
                    SquadStrings.betterInsights,
                    SquadStrings.fasterManagement
                ),
                excluded = listOf(
                    SquadStrings.loanManagement
                ),
                onClick = {

                    selectedPlan =
                        SubscriptionModel.Plan.BASIC

                    expandedPlan =
                        if (
                            expandedPlan ==
                            SubscriptionModel.Plan.BASIC
                        ) {
                            null
                        } else {
                            SubscriptionModel.Plan.BASIC
                        }

                    sub?.let { subscription ->

                        disableUpgradeButton = shouldDisableUpgradeButton(
                            selectedPlan = selectedPlan,
                            selectedPeriod = selectedPeriod,
                            currentPlan = subscription.plan,
                            currentPeriod = subscription.billingPeriod,
                            subscriptionStatus = subscription.subscriptionStatus
                        )
                    }
                }
            )

            // MARK: - Business Plan

            PlanCard(
                title = SquadStrings.businessPlan,
                subtitle = SquadStrings.businessPlanDescription,
                price = cfg.priceText(
                    SubscriptionModel.Plan.BUSINESS,
                    selectedPeriod
                ),
                isSelected =
                    selectedPlan == SubscriptionModel.Plan.BUSINESS,
                isExpanded =
                    expandedPlan == SubscriptionModel.Plan.BUSINESS,
                included = listOf(
                    SquadStrings.everythingInBasic,
                    SquadStrings.upTo200PlusMembers,
                    SquadStrings.loanManagementIncluded,
                    SquadStrings.advancedAnalytics,
                    SquadStrings.prioritySupport
                ),
                excluded = emptyList(),
                onClick = {

                    selectedPlan =
                        SubscriptionModel.Plan.BUSINESS

                    expandedPlan =
                        if (
                            expandedPlan ==
                            SubscriptionModel.Plan.BUSINESS
                        ) {
                            null
                        } else {
                            SubscriptionModel.Plan.BUSINESS
                        }

                    enableLoanAddon = true

                    sub?.let { subscription ->

                        disableUpgradeButton = shouldDisableUpgradeButton(
                            selectedPlan = selectedPlan,
                            selectedPeriod = selectedPeriod,
                            currentPlan = subscription.plan,
                            currentPeriod = subscription.billingPeriod,
                            subscriptionStatus = subscription.subscriptionStatus
                        )
                    }
                }
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            // MARK: - Loan Add-on

            if (
                cfg.addon_loan_enabled &&
                selectedPlan != SubscriptionModel.Plan.BUSINESS
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            AppColors.surface,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        Text(
                            text = SquadStrings.loanAddOn,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = cfg.addon_loan_priceText,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = cfg.addon_loan_tagline,
                        fontSize = 11.sp,
                        color = AppColors.secondaryText
                    )

                    Switch(
                        checked = enableLoanAddon,
                        onCheckedChange = {
                            enableLoanAddon = it
                        },
                        enabled =
                            selectedPlan !=
                                    SubscriptionModel.Plan.BUSINESS
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )
        }

        // MARK: - Upgrade Button

        Button(
            onClick = {

                val currentSquadID = squadID

                if (currentSquadID.isNullOrBlank()) {

                    showUpgradeFailure(
                        SquadStrings.squadNotFound
                    )

                    return@Button
                }

                if (isLoading) {
                    return@Button
                }

                val currentActivity = activity

                if (currentActivity == null) {

                    showUpgradeFailure(
                        "Unable to open the payment screen."
                    )

                    return@Button
                }

                startCashfreePayment(
                    activity = currentActivity,
                    squadID = currentSquadID,
                    selectedPlan = selectedPlan,
                    selectedPeriod = selectedPeriod,
                    enableLoanAddon = enableLoanAddon,
                    onLoading = { loading ->
                        isLoading = loading
                    },
                    onError = { message ->
                        isLoading = false
                        showUpgradeFailure(message)
                    },
                    onSuccess = {

                        isLoading = false

                        squadViewModel.setShowUpgradePlan(false)

                        squadViewModel.setShowUpgradeSuccess(true)

                        Log.d(
                            UPGRADE_TAG,
                            "Verified subscription. Showing success screen."
                        )
                    }
                )
            },
            enabled = !isLoading && !disableUpgradeButton,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
        ) {

            if (isLoading) {

                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text("Processing payment...")

            } else {

                Text(
                    text = SquadStrings.upgradeNow
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )
    }
}



private fun shouldDisableUpgradeButton(
    selectedPlan: SubscriptionModel.Plan,
    selectedPeriod: SubscriptionModel.BillingPeriod,
    currentPlan: SubscriptionModel.Plan,
    currentPeriod: SubscriptionModel.BillingPeriod,
    subscriptionStatus: SubscriptionModel.SubscriptionStatus?
): Boolean {

    // 1. FREE plan: allow upgrading
    if (currentPlan == SubscriptionModel.Plan.FREE) {
        return false
    }

    // 2. Expired subscription: allow upgrading
    if (subscriptionStatus == SubscriptionModel.SubscriptionStatus.EXPIRED) {
        return false
    }

    // 3. Plan ranking
    val planRank = mapOf(
        SubscriptionModel.Plan.FREE to 0,
        SubscriptionModel.Plan.BASIC to 1,
        SubscriptionModel.Plan.BUSINESS to 2
    )

    val currentPlanRank = planRank[currentPlan] ?: 0
    val selectedPlanRank = planRank[selectedPlan] ?: 0

    // 4. Billing period ranking
    val periodRank = mapOf(
        SubscriptionModel.BillingPeriod.MONTHLY to 0,
        SubscriptionModel.BillingPeriod.SIX_MONTH to 1,
        SubscriptionModel.BillingPeriod.YEARLY to 2
    )

    val currentPeriodRank = periodRank[currentPeriod] ?: 0
    val selectedPeriodRank = periodRank[selectedPeriod] ?: 0

    // 5. Prevent plan downgrades when the billing period
    //    is the same or shorter
    if (
        selectedPlanRank < currentPlanRank &&
        selectedPeriodRank <= currentPeriodRank
    ) {
        return true
    }

    // 6. Prevent billing period downgrades when selecting
    //    the same plan
    if (
        selectedPlanRank == currentPlanRank &&
        selectedPeriodRank < currentPeriodRank
    ) {
        return true
    }

    // 7. Disable when the selected plan and period are unchanged
    if (
        selectedPlan == currentPlan &&
        selectedPeriod == currentPeriod
    ) {
        return true
    }

    // 8. Allow upgrades and longer billing periods
    return false
}

private fun startCashfreePayment(
    activity: Activity,
    squadID: String,
    selectedPlan: SubscriptionModel.Plan,
    selectedPeriod: SubscriptionModel.BillingPeriod,
    enableLoanAddon: Boolean,
    onLoading: (Boolean) -> Unit,
    onError: (String) -> Unit,
    onSuccess: () -> Unit
) {

    onLoading(true)

    val payload = hashMapOf<String, Any>(
        "squadID" to squadID,
        "plan" to selectedPlan.name,
        "billingPeriod" to selectedPeriod.name,
        "loanAddon" to enableLoanAddon
    )

    Log.d(
        UPGRADE_TAG,
        "Creating Cashfree order for squad: $squadID"
    )

    FirebaseFunctions
        .getInstance("asia-south1")
        .getHttpsCallable("createSubscriptionPayment")
        .call(payload)
        .addOnSuccessListener { result ->

            val response = result.data as? Map<*, *>

            val orderID =
                response?.get("orderId") as? String

            val paymentSessionID =
                response?.get("paymentSessionId") as? String

            if (
                orderID.isNullOrBlank() ||
                paymentSessionID.isNullOrBlank()
            ) {

                Log.e(
                    UPGRADE_TAG,
                    "Missing orderId or paymentSessionId."
                )

                onLoading(false)

                onError(
                    "Unable to create the Cashfree payment session."
                )

                return@addOnSuccessListener
            }

            Log.d(
                UPGRADE_TAG,
                "Cashfree order created: $orderID"
            )

            CashfreePaymentManager.startCheckout(
                activity = activity,
                orderID = orderID,
                paymentSessionID = paymentSessionID
            ) { paymentSucceeded ->

                activity.runOnUiThread {

                    if (paymentSucceeded) {

                        /*
                         * The verification event handler calls this
                         * completion only after the subscription refresh
                         * has succeeded.
                         */
                        onSuccess()

                    } else {

                        /*
                         * The payment-failure event listener handles
                         * the error message.
                         */
                        onLoading(false)
                    }
                }
            }
        }
        .addOnFailureListener { error ->

            Log.e(
                UPGRADE_TAG,
                "Failed to create Cashfree order.",
                error
            )

            onLoading(false)

            onError(
                error.localizedMessage
                    ?: "Unable to initiate payment. Please try again."
            )
        }
}

// MARK: - Refresh Subscription After Payment

private suspend fun refreshSubscriptionAfterPayment(
    viewModel: SubscriptionManager,
    squadID: String,
    expectedPlan: SubscriptionModel.Plan,
    expectedPeriod: SubscriptionModel.BillingPeriod
): Boolean {

    repeat(10) { index ->

        val result: Pair<Boolean, String?> =
            suspendCancellableCoroutine { continuation ->

                viewModel.loadSubscription(
                    squadID = squadID
                ) { success, error ->

                    if (continuation.isActive) {

                        continuation.resume(
                            Pair(success, error)
                        )
                    }
                }
            }

        Log.d(
            UPGRADE_TAG,
            "Subscription refresh ${index + 1}: " +
                    "success=${result.first}, error=${result.second}"
        )

        if (result.first) {

            val subscription = viewModel.subscription.value

            if (subscription != null) {

                val isActive =
                    subscription.subscriptionStatus ==
                            SubscriptionModel.SubscriptionStatus.ACTIVE

                val isNotExpired =
                    subscription.subscriptionEndDate
                        ?.toDate()
                        ?.after(java.util.Date()) == true

                val matchesSelectedPlan =
                    subscription.plan == expectedPlan

                val matchesSelectedPeriod =
                    subscription.billingPeriod == expectedPeriod

                if (
                    isActive &&
                    isNotExpired &&
                    matchesSelectedPlan &&
                    matchesSelectedPeriod
                ) {

                    Log.d(
                        UPGRADE_TAG,
                        "Subscription matches the selected plan and is active."
                    )

                    return true
                }
            }
        }

        if (index < 9) {
            delay(1_000)
        }
    }

    Log.w(
        UPGRADE_TAG,
        "Subscription was not confirmed active after 10 attempts."
    )

    return false
}

// MARK: - Billing Period Picker

@Composable
fun BillingPeriodPicker(
    selectedPlan: SubscriptionModel.Plan,
    selectedPeriod: SubscriptionModel.BillingPeriod,
    cfg: RemoteConfig,
    onSelect: (SubscriptionModel.BillingPeriod) -> Unit
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        SubscriptionModel.BillingPeriod.entries.forEach { period ->

            val isSelected = selectedPeriod == period

            val savings = cfg.savingsText(
                selectedPlan,
                period
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (isSelected) {
                            AppColors.primaryBrand
                        } else {
                            AppColors.surface
                        },
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) {
                            Color.Transparent
                        } else {
                            AppColors.border
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        onSelect(period)
                    }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = period.localizedName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) {
                        Color.White
                    } else {
                        AppColors.headerText
                    }
                )

                if (savings != null) {

                    Text(
                        text = savings,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) {
                            Color.White.copy(alpha = 0.85f)
                        } else {
                            AppColors.successAccent
                        }
                    )
                }
            }
        }
    }
}

// MARK: - Plan Card

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
            .alpha(
                if (enabled) 1f else 0.45f
            )
            .padding(vertical = 8.dp)
            .background(
                if (isSelected) {
                    AppColors.primaryBackground
                } else {
                    AppColors.surface
                },
                RoundedCornerShape(18.dp)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) {
                    AppColors.primaryBrand
                } else {
                    AppColors.border
                },
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(enabled = enabled) {
                onClick()
            }
            .padding(14.dp)
    ) {

        Row {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.headerText
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = AppColors.secondaryText
                )
            }

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Text(
                text = price,
                color = AppColors.primaryBrand,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = if (isExpanded) {
                SquadStrings.hideDetails
            } else {
                SquadStrings.viewDetails
            },
            fontSize = 12.sp,
            color = AppColors.primaryBrand
        )

        if (isExpanded) {

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Divider()

            Column(
                modifier = Modifier.padding(top = 8.dp)
            ) {

                included.forEach { feature ->
                    FeatureRow(
                        text = feature,
                        included = true
                    )
                }

                excluded.forEach { feature ->
                    FeatureRow(
                        text = feature,
                        included = false
                    )
                }
            }
        }
    }
}

// MARK: - Feature Row

@Composable
fun FeatureRow(
    text: String,
    included: Boolean
) {

    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = if (included) {
                Icons.Default.CheckCircle
            } else {
                Icons.Default.Cancel
            },
            contentDescription = null,
            tint = if (included) {
                AppColors.successAccent
            } else {
                AppColors.errorAccent
            }
        )

        Spacer(
            modifier = Modifier.width(6.dp)
        )

        Text(
            text = text,
            fontSize = 12.sp,
            color = AppColors.secondaryText
        )
    }
}

// MARK: - Failure Message

private fun showUpgradeFailure(
    message: String
) {

    ToastManager.show(
        title = SquadStrings.savingsSquad,
        message = message,
        type = ToastType.ERROR
    )
}