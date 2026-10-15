package com.android.savingssquad.SquadSubscription

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.savingssquad.SquadSubscription.SubscriptionModel.Plan
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.SquadStrings

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

    val planName = when (subscription.plan) {
        Plan.FREE -> SquadStrings.freePlan
        Plan.BASIC -> SquadStrings.basicPlan
        Plan.BUSINESS -> SquadStrings.businessPlan
    }

    val badgeTitle = when (subscription.plan) {
        Plan.FREE -> if (isFreeTrialActive) "TRIAL" else "FREE"
        Plan.BASIC,
        Plan.BUSINESS -> "ACTIVE"
    }

    val subtitle = if (isFreeTrialActive) {
        SquadStrings.trialDaysRemaining(daysLeft.toString())
    } else {
        when (subscription.plan) {
            Plan.FREE -> "Free plan"

            Plan.BASIC,
            Plan.BUSINESS -> when (subscription.billingPeriod) {
                SubscriptionModel.BillingPeriod.MONTHLY -> SquadStrings.monthly
                SubscriptionModel.BillingPeriod.SIX_MONTH -> SquadStrings.sixMonths
                SubscriptionModel.BillingPeriod.YEARLY -> SquadStrings.yearly
            }
        }
    }

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
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                width = 1.dp,
                color = AppColors.successAccent.copy(alpha = 0.25f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Plan Icon
        Box(
            modifier = Modifier
                .size(34.dp)
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
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Plan Details
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = if (isFreeTrialActive) {
                    SquadStrings.freeTrialActive
                } else {
                    planName
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.headerText
            )

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = AppColors.secondaryText,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Trial Days / Plan Status
        Box(
            modifier = Modifier
                .background(
                    color = AppColors.successAccent,
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(
                    horizontal = 8.dp,
                    vertical = if (isFreeTrialActive) 3.dp else 4.dp
                )
        ) {
            Text(
                text = if (isFreeTrialActive) {
                    daysLeft.toString()
                } else {
                    badgeTitle
                },
                fontSize = if (isFreeTrialActive) 10.sp else 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}