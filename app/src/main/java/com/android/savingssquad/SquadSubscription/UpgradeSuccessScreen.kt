package com.android.savingssquad.SquadSubscription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.savingssquad.singleton.AppColors

fun SubscriptionModel.Plan.label(): String {

    return when (this) {

        SubscriptionModel.Plan.FREE -> "Free"

        SubscriptionModel.Plan.BASIC -> "Basic"

        SubscriptionModel.Plan.BUSINESS -> "Business"

    }

}

@Composable
fun UpgradeSuccessScreen(
    plan: SubscriptionModel.Plan,
    viewModel: SubscriptionManager = SubscriptionManager.shared,
    onDone: () -> Unit
) {

    val remoteConfig by viewModel.remoteConfig.collectAsState()

    val features = remoteConfig.features(plan)
    val maxMembers = remoteConfig.maxMembers(plan)
    val price = remoteConfig.priceText(plan)

    LaunchedEffect(plan) {
        kotlinx.coroutines.delay(2500)
        onDone()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // SUCCESS ICON
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        AppColors.successAccent.copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = AppColors.successAccent,
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Upgrade Successful!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.headerText
            )

            Text(
                text = "You are now on ${plan.label()} plan",
                fontSize = 14.sp,
                color = AppColors.secondaryText
            )

            Spacer(modifier = Modifier.height(20.dp))

            // PLAN INFO CARD
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {

                Column(modifier = Modifier.padding(16.dp)) {

                    FeatureRow("Max Members", maxMembers.toString())

                    FeatureRow(
                        "Contribution",
                        if (features.contribution) "Enabled" else "Disabled"
                    )

                    FeatureRow(
                        "Loan Access",
                        if (features.loan) "Enabled" else "Disabled"
                    )

                    FeatureRow("Price", price)
                }
            }
        }
    }
}

@Composable
fun FeatureRow(title: String, value: String) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(
            text = title,
            fontSize = 13.sp,
            color = AppColors.secondaryText
        )

        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.headerText
        )
    }
}