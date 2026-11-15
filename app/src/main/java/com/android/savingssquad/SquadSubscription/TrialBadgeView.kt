package com.android.savingssquad.SquadSubscription

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.savingssquad.SquadSubscription.SubscriptionModel.Plan
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.SquadStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TrialBadgeView(
    viewModel: SubscriptionManager = SubscriptionManager.shared,
    onClick: () -> Unit
) {
    val sub by viewModel.subscription.collectAsState()
    val subscription = sub ?: return

    val isFreePlan = subscription.plan == Plan.FREE
    val isFreeTrialActive = isFreePlan && viewModel.isTrialActive()
    val daysLeft = viewModel.trialDaysRemaining()

    // Plan name
    val planName = when (subscription.plan) {
        Plan.FREE -> SquadStrings.freePlan
        Plan.BASIC -> SquadStrings.basicPlan
        Plan.BUSINESS -> SquadStrings.businessPlan
    }

    // Badge title
    val badgeTitle = when (subscription.plan) {
        Plan.FREE -> if (isFreeTrialActive) "TRIAL" else "FREE"
        Plan.BASIC, Plan.BUSINESS -> "ACTIVE"
    }

    // Pack expiry date
    val formattedPackEndDate = remember(subscription.subscriptionEndDate) {
        subscription.subscriptionEndDate?.toDate()?.let { date ->
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
        }
    }

    // Subtitle
    val subtitle = if (isFreeTrialActive) {
        SquadStrings.trialDaysRemaining(daysLeft.toString())
    } else {
        when (subscription.plan) {
            Plan.FREE -> "Free plan"

            Plan.BASIC, Plan.BUSINESS -> {
                val billingPeriod = when (subscription.billingPeriod) {
                    SubscriptionModel.BillingPeriod.MONTHLY -> SquadStrings.monthly
                    SubscriptionModel.BillingPeriod.SIX_MONTH -> SquadStrings.sixMonths
                    SubscriptionModel.BillingPeriod.YEARLY -> SquadStrings.yearly
                }

                if (formattedPackEndDate != null) {
                    "$billingPeriod · ${SquadStrings.expires} ${SquadStrings.localizedMonthInText(formattedPackEndDate)}"
                } else {
                    billingPeriod
                }
            }
        }
    }

    // Plan icon
    val icon = when (subscription.plan) {
        Plan.FREE -> Icons.Default.CardGiftcard
        Plan.BASIC -> Icons.Default.Star
        Plan.BUSINESS -> Icons.Default.WorkspacePremium
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .background(
                color = AppColors.surface,
                shape = RoundedCornerShape(11.dp)
            )
            .border(
                width = 1.dp,
                color = AppColors.successAccent.copy(alpha = 0.20f),
                shape = RoundedCornerShape(11.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Plan icon
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    color = AppColors.successAccent.copy(alpha = 0.12f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.successAccent,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Plan details
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = if (isFreeTrialActive) {
                    SquadStrings.freeTrialActive
                } else {
                    planName
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.headerText,
                maxLines = 1
            )

            Text(
                text = subtitle,
                fontSize = 9.sp,
                fontWeight = FontWeight.Normal,
                color = AppColors.secondaryText,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Trial days / plan status
        Box(
            modifier = Modifier
                .background(
                    color = AppColors.successAccent,
                    shape = RoundedCornerShape(5.dp)
                )
                .padding(
                    horizontal = 7.dp,
                    vertical = 3.dp
                )
        ) {
            Text(
                text = if (isFreeTrialActive) {
                    daysLeft.toString()
                } else {
                    badgeTitle
                },
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}