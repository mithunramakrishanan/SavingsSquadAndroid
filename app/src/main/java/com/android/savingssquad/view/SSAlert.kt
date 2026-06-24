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
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SSAlert(
    alertManager: AlertManager = AlertManager.shared
) {

    val isShowing by alertManager.isShowing.collectAsState()

    AnimatedVisibility(
        visible = isShowing,
        enter = fadeIn() + scaleIn(
            initialScale = 0.9f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ),
        exit = fadeOut() + scaleOut()
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
                    .blur(8.dp)
            )

            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 320.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AppColors.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 12.dp
                ),
                border = BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.5f)
                )
            ) {

                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {

                    PremiumIconView(
                        type = alertManager.alertType
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        Text(
                            text = alertManager.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.headerText,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = alertManager.message,
                            fontSize = 15.sp,
                            color = AppColors.secondaryText,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        alertManager.secondaryButtonTitle?.let { title ->

                            alertManager.secondaryAction?.let { action ->

                                OutlinedButton(
                                    onClick = {
                                        action()
                                        alertManager.hideAlert()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        AppColors.border
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = AppColors.background
                                    )
                                ) {

                                    Text(
                                        text = title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppColors.headerText
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                alertManager.primaryAction?.invoke()
                                alertManager.hideAlert()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues()
                        ) {

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = primaryGradient(
                                            alertManager.alertType
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {

                                Text(
                                    text = alertManager.primaryButtonTitle,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun primaryGradient(
    type: AlertType
): Brush {

    return when (type) {

        AlertType.SUCCESS ->
            Brush.horizontalGradient(
                listOf(
                    AppColors.successAccent,
                    AppColors.successAccent.copy(alpha = 0.8f)
                )
            )

        AlertType.ERROR ->
            Brush.horizontalGradient(
                listOf(
                    AppColors.errorAccent,
                    AppColors.errorAccent.copy(alpha = 0.8f)
                )
            )

        AlertType.INFO ->
            Brush.horizontalGradient(
                listOf(
                    AppColors.primaryBrand,
                    AppColors.primaryBrand.copy(alpha = 0.8f)
                )
            )
    }
}

@Composable
private fun PremiumIconView(
    type: AlertType
) {

    val color = when (type) {
        AlertType.SUCCESS -> AppColors.successAccent
        AlertType.ERROR -> AppColors.errorAccent
        AlertType.INFO -> AppColors.infoAccent
    }

    val icon = when (type) {
        AlertType.SUCCESS -> Icons.Default.Check
        AlertType.ERROR -> Icons.Default.Close
        AlertType.INFO -> Icons.Default.Info
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(72.dp)
    ) {

        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    color.copy(alpha = 0.12f),
                    CircleShape
                )
                .border(
                    1.dp,
                    color.copy(alpha = 0.25f),
                    CircleShape
                )
        )

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(30.dp)
        )
    }
}