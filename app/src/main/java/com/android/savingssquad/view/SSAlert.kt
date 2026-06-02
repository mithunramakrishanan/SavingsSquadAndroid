package com.android.savingssquad.view

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.android.savingssquad.R
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.viewmodel.AlertManager
import com.android.savingssquad.singleton.AlertType
import com.android.savingssquad.singleton.AppShadows
import com.android.savingssquad.singleton.appShadow
import androidx.compose.animation.core.tween

@Composable
fun SSAlert(alertManager: AlertManager = AlertManager.shared) {
    val isShowing by alertManager.isShowing.collectAsState()

    AnimatedVisibility(
        visible = isShowing,
        enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.95f),
        exit = fadeOut(tween(200)) + scaleOut(targetScale = 1.05f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .zIndex(2f)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .background(AppColors.surface, RoundedCornerShape(20.dp))
                    .widthIn(max = 340.dp)
                    .appShadow(AppShadows.elevated, RoundedCornerShape(20.dp))
                    .animateContentSize(animationSpec = tween(250)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 🔹 Icon
                IconView(type = alertManager.alertType)

                // 🔹 Title
                Text(
                    text = alertManager.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.headerText
                )

                // 🔹 Message
                Text(
                    text = alertManager.message,
                    fontSize = 16.sp,
                    color = AppColors.secondaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // 🔹 Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Secondary Button
                    alertManager.secondaryButtonTitle?.let { secondary ->
                        alertManager.secondaryAction?.let { secondaryAction ->
                            Button(
                                onClick = {
                                    secondaryAction()
                                    alertManager.hideAlert()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.surface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .appShadow(AppShadows.card, RoundedCornerShape(12.dp))
                            ) {
                                Text(
                                    text = secondary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.headerText
                                )
                            }
                        }
                    }

                    // Primary Button
                    Button(
                        onClick = {
                            alertManager.primaryAction?.invoke()
                            alertManager.hideAlert()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                brush = gradientBackground(alertManager.alertType),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .appShadow(AppShadows.elevated, RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = alertManager.primaryButtonTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.primaryButtonText,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IconView(type: AlertType) {
    val size = 40.dp
    val icon = when (type) {
        AlertType.SUCCESS -> Icons.Default.CheckCircle
        AlertType.ERROR -> Icons.Default.Error
        AlertType.INFO -> Icons.Default.Info
    }
    val color = when (type) {
        AlertType.SUCCESS -> AppColors.successAccent
        AlertType.ERROR -> AppColors.errorAccent
        AlertType.INFO -> AppColors.infoAccent
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(size)
    )
}

@Composable
private fun gradientBackground(type: AlertType): Brush {
    return when (type) {
        AlertType.SUCCESS -> Brush.linearGradient(
            listOf(
                AppColors.successAccent.copy(alpha = 0.9f),
                AppColors.successAccent.copy(alpha = 0.7f)
            )
        )
        AlertType.ERROR -> Brush.linearGradient(
            listOf(
                AppColors.errorAccent.copy(alpha = 0.9f),
                AppColors.errorAccent.copy(alpha = 0.7f)
            )
        )
        AlertType.INFO -> Brush.linearGradient(
            listOf(
                AppColors.infoAccent.copy(alpha = 0.85f),
                AppColors.infoAccent.copy(alpha = 0.6f)
            )
        )
    }
}