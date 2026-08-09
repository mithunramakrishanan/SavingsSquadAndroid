package com.android.savingssquad.view

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.savingssquad.R
import com.android.savingssquad.model.PaymentsDetails
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.SquadStrings
import com.android.savingssquad.singleton.UPIApp
import com.android.savingssquad.singleton.UPIAppDetector
import com.android.savingssquad.viewmodel.SquadViewModel
import kotlinx.coroutines.delay


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun PaymentWaitingView(
    payment: PaymentsDetails,
    squadViewModel: SquadViewModel,
    onCancel: (() -> Unit)? = null
) {

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var installedApps by remember { mutableStateOf<List<UPIApp>>(emptyList()) }
    var didCopyUPI by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    LaunchedEffect(didCopyUPI) {
        if (didCopyUPI) {
            delay(1400)
            didCopyUPI = false
        }
    }

    LaunchedEffect(Unit) {
        installedApps = UPIAppDetector.installedApps(context)
        clipboardManager.setText(AnnotatedString(payment.upiID))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {

        AppBackgroundGradient()
        Column(modifier = Modifier.fillMaxSize()) {

            //====================================================
            // Header
            //====================================================

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Complete Payment",
                    style = AppFont.ibmPlexSans(16, FontWeight.SemiBold),
                    color = AppColors.headerText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }

            //====================================================
            // Scrollable Content
            //====================================================

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                WaitingHero(
                    payment = payment,
                    pulseScale = pulseScale,
                    pulseAlpha = pulseAlpha
                )

                UpiCard(
                    payment = payment,
                    didCopyUPI = didCopyUPI,
                    onCopyUPI = {
                        clipboardManager.setText(AnnotatedString(payment.upiID))
                        didCopyUPI = true
                    }
                )

                AppsSection(
                    apps = installedApps,
                    onAppClick = { app ->
                        clipboardManager.setText(AnnotatedString(payment.upiID))
                        openUpiApp(context, app)
                        squadViewModel.setShowWaitingForPayment(false)
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))
            }

            //====================================================
            // Fixed Bottom Bar
            //====================================================

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.surface)
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 16.dp)
            ) {

                TextButton(
                    onClick = { onCancel?.invoke() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = SquadStrings.cancel,
                        style = AppFont.ibmPlexSans(13, FontWeight.SemiBold),
                        color = AppColors.errorAccent
                    )
                }
            }
        }
    }
}

// =========================================================
//  WAITING HERO
// =========================================================

@Composable
private fun WaitingHero(payment: PaymentsDetails, pulseScale: Float, pulseAlpha: Float) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {

        Box(
            modifier = Modifier.size(78.dp),
            contentAlignment = Alignment.Center
        ) {
            // pulsing outer ring — signals a live, in-progress state
            Box(
                modifier = Modifier
                    .size((60 * pulseScale).dp)
                    .clip(CircleShape)
                    .border(2.dp, AppColors.primaryBrand.copy(alpha = 0.35f * pulseAlpha), CircleShape)
            )

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(AppColors.primaryBrand.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = AppColors.primaryBrand,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = SquadStrings.amountToPay,
                style = AppFont.ibmPlexSans(10, FontWeight.SemiBold),
                color = AppColors.secondaryText.copy(alpha = 0.7f)
            )

            Text(
                text = "\u20B9${payment.amount}",
                style = AppFont.ibmPlexSans(36, FontWeight.Bold),
                color = AppColors.headerText
            )
        }

        Text(
            text = "Waiting for payment confirmation\u2026",
            style = AppFont.ibmPlexSans(12, FontWeight.Medium),
            color = AppColors.secondaryText
        )
    }
}

// =========================================================
//  UPI CARD
// =========================================================

@Composable
private fun UpiCard(
    payment: PaymentsDetails,
    didCopyUPI: Boolean,
    onCopyUPI: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.background.copy(alpha = 0.5f))
            .border(1.dp, AppColors.border.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = SquadStrings.upiID,
                    style = AppFont.ibmPlexSans(9, FontWeight.SemiBold),
                    color = AppColors.secondaryText.copy(alpha = 0.6f)
                )

                Text(
                    text = payment.upiID,
                    style = AppFont.ibmPlexSans(13, FontWeight.Medium).copy(fontFamily = FontFamily.Monospace),
                    color = AppColors.primaryBrand,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            CopyChip(copied = didCopyUPI, onClick = onCopyUPI)
        }

        HorizontalDivider(color = AppColors.border)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = AppColors.primaryBrand
            )

            Text(
                text = "Open any UPI app below to complete your payment",
                style = AppFont.ibmPlexSans(11, FontWeight.Medium),
                color = AppColors.secondaryText
            )
        }
    }
}

@Composable
private fun CopyChip(copied: Boolean, onClick: () -> Unit) {

    val chipColor = if (copied) AppColors.successAccent else AppColors.primaryBrand

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(chipColor.copy(alpha = 0.10f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        Icon(
            imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
            contentDescription = null,
            tint = chipColor,
            modifier = Modifier.size(12.dp)
        )

        Text(
            text = if (copied) SquadStrings.copied else SquadStrings.copy,
            style = AppFont.ibmPlexSans(11, FontWeight.SemiBold),
            color = chipColor
        )
    }
}

// =========================================================
//  APPS SECTION
// =========================================================

@Composable
private fun AppsSection(
    apps: List<UPIApp>,
    onAppClick: (UPIApp) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.background.copy(alpha = 0.5f))
            .border(1.dp, AppColors.border.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text(
            text = "PAY USING",
            style = AppFont.ibmPlexSans(10, FontWeight.SemiBold),
            color = AppColors.secondaryText.copy(alpha = 0.7f)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.heightIn(max = 260.dp)
        ) {
            items(apps) { app ->
                UpiAppRow(app = app, onClick = { onAppClick(app) })
            }
        }
    }
}

@Composable
private fun UpiAppRow(app: UPIApp, onClick: () -> Unit) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.surface)
            .border(1.dp, AppColors.border.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Image(
            painter = painterResource(id = app.iconRes),
            contentDescription = app.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
        )

        Text(
            text = app.name,
            style = AppFont.ibmPlexSans(12, FontWeight.Medium),
            color = AppColors.headerText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

// =========================================================
//  INTENT HANDLING
// =========================================================

private fun openUpiApp(context: android.content.Context, app: UPIApp) {

    val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)

    if (intent != null) {
        context.startActivity(intent)
        return
    }

    try {
        val schemeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(app.scheme))
        context.startActivity(schemeIntent)
    } catch (e: ActivityNotFoundException) {
        // App isn't actually available; silently ignore or surface a toast/snackbar
    }
}