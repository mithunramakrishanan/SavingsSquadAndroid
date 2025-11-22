package com.android.savingssquad.view

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.savingssquad.R

@Composable
fun CashfreePaymentView(
    orderId: String,
    payment_session_id: String,
    squadId: String,
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit
) {
    val context = LocalContext.current

    // Step 1: Create Activity Result Launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val orderIdResult = result.data?.getStringExtra("orderId")
                onSuccess(orderIdResult ?: "Unknown")
            }
            Activity.RESULT_CANCELED -> {
                val errorMsg = result.data?.getStringExtra("error") ?: "Payment canceled"
                onFailure(errorMsg)
            }
        }
    }

    // Step 2: Trigger launch once
    LaunchedEffect(Unit) {
        val intent = Intent(context, PaymentViewController::class.java).apply {
            putExtra("orderId", orderId)
            putExtra("payment_session_id", payment_session_id)
            putExtra("squadId", squadId)
        }
        launcher.launch(intent)
    }
}

@Composable
fun PaymentResultScreen(
    success: Boolean,
    message: String,
    recipientName: String,
    amount: String,
    onDone: () -> Unit
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF9EFE5), Color.White)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // ✅ Animated Icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(if (success) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFFF44336).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        id = if (success) R.drawable.back_icon else R.drawable.back_icon
                    ),
                    contentDescription = null,
                    tint = if (success) Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (success) "Payment Sent" else "Payment Failed",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                fontSize = 16.sp,
                color = Color.Gray,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 30.dp),
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ✅ Amount card
            AnimatedVisibility(
                visible = success,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                        .fillMaxWidth()
                        .shadow(2.dp)
                ) {
                    Text("Total Transferred", color = Color.Gray, fontSize = 14.sp)
                    Text(amount, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ✅ Recipient info (if success)
            AnimatedVisibility(visible = success) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.back_icon),
                        contentDescription = null,
                        tint = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier.size(50.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(recipientName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ✅ Done button
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (success) Color(0xFF4CAF50) else Color(0xFFF44336)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Done", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun PaymentScreen(
    isLoading: Boolean,
    message: String
) {
    // Fullscreen layout with centered content
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9EFE5)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated loading indicator
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color(0xFF4CAF50),
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Message text
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color(0xFF333333),
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}