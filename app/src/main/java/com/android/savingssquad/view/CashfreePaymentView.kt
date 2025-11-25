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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.savingssquad.R
import com.android.savingssquad.singleton.AppFont

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
    recipientNumber: String,
    totalAmount: String,
    onDone: () -> Unit
) {

    // Soft background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.White, Color(0xFFF4F7F9))
                )
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(30.dp))

            // Main Card
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .shadow(
                        elevation = 20.dp,
                        spotColor = Color.Black.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .background(Color.White, RoundedCornerShape(28.dp))
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Status Icon
                Icon(
                    imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (success) Color(0xFF2ECC71) else Color.Red,
                    modifier = Modifier
                        .size(80.dp)
                        .padding(top = 12.dp)
                )

                // Title
                Text(
                    text = if (success) "Payment Successful" else "Payment Failed",
                    style = AppFont.ibmPlexSans(24, FontWeight.SemiBold),
                    color = Color.Black
                )

                // Message
                Text(
                    text = message,
                    style = AppFont.ibmPlexSans(16, FontWeight.Normal),
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                // Amount Card (Success only)
                if (success) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .shadow(
                                elevation = 6.dp,
                                spotColor = Color.Black.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Total Amount",
                            style = AppFont.ibmPlexSans(14, FontWeight.Normal),
                            color = Color.Gray
                        )

                        Text(
                            totalAmount,
                            style = AppFont.ibmPlexSans(28, FontWeight.Bold),
                            color = Color.Black
                        )
                    }
                }

                // Recipient Card (Success only)
                if (success) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .shadow(
                                elevation = 6.dp,
                                spotColor = Color.Black.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // Gray rounded icon background
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = recipientName,
                                style = AppFont.ibmPlexSans(16, FontWeight.SemiBold),
                                color = Color.Black
                            )
                            Text(
                                text = recipientNumber,
                                style = AppFont.ibmPlexSans(14, FontWeight.Normal),
                                color = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // DONE BUTTON
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (success) Color(0xFF2ECC71) else Color.Red
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(52.dp)
                    .shadow(
                        elevation = 8.dp,
                        spotColor = (if (success) Color(0xFF2ECC71) else Color.Red)
                            .copy(alpha = 0.25f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Done",
                    style = AppFont.ibmPlexSans(18, FontWeight.SemiBold),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PaymentResultPreview() {
    PaymentResultScreen(
        success = true,
        message = "Your transaction is complete and should reflect shortly.",
        recipientName = "Mithun",
        recipientNumber = "RESCPE32323",
        totalAmount = "₹1000",
        onDone = { }
    )
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