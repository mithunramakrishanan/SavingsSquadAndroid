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
import com.android.savingssquad.singleton.AppColors

@Composable
fun TrialBadgeView(
    viewModel: SubscriptionManager = SubscriptionManager.shared,
    onClick: () -> Unit
) {

    val sub by viewModel.subscription.collectAsState()

    val isActive = sub?.let {
        viewModel.isTrialActive()
    } ?: false

    if (!isActive) return

    val daysLeft = viewModel.trialDaysRemaining()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .background(
                AppColors.surface,
                RoundedCornerShape(14.dp)
            )
            .border(
                1.dp,
                AppColors.successAccent.copy(alpha = 0.25f),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // ICON
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(
                    AppColors.successAccent.copy(alpha = 0.12f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CardGiftcard,
                contentDescription = null,
                tint = AppColors.successAccent,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {

            Text(
                text = "Free Trial Active",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.headerText
            )

            Text(
                text = "$daysLeft of ${viewModel.trialDaysTotal} days left • Full access",
                fontSize = 10.sp,
                color = AppColors.secondaryText,
                maxLines = 1
            )
        }

        Box(
            modifier = Modifier
                .background(
                    AppColors.successAccent,
                    RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = "${daysLeft}d",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}