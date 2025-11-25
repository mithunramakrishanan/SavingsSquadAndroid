package com.android.savingssquad.view
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.android.savingssquad.R
import kotlinx.coroutines.delay
import com.android.savingssquad.singleton.AppColors
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.savingssquad.singleton.AppFont
import kotlinx.coroutines.delay

@Composable
fun LaunchView(
    showLaunchView: Boolean,
    onFinish: () -> Unit
) {
    if (!showLaunchView) return

    val appName = "Savings Squad"
    val characters = appName.toList()

    // States
    val scale = remember { Animatable(0.6f) }
    val opacity = remember { Animatable(0f) }

    val textOpacity = remember {
        characters.map { Animatable(0f) }
    }

    // Same gradient as SwiftUI
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(
            AppColors.primaryBrand,
            AppColors.secondaryAccent.copy(alpha = 0.8f),
            AppColors.successAccent.copy(alpha = 0.8f),
            AppColors.primaryBrand.copy(alpha = 0.9f)
        )
    )

    // Timings
    val fadeInDuration = 800
    val bounceDelay = 800L
    val textStartDelay = 600L
    val fadeOutDelay = 2200L
    val fadeOutDuration = 600

    // -----------------------------------------------------
    // ANIMATION SEQUENCE
    // -----------------------------------------------------
    LaunchedEffect(Unit) {

        // 1️⃣ Fade in + scale
        launch {
            scale.animateTo(
                1.2f,
                tween(fadeInDuration, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            opacity.animateTo(
                1f,
                tween(fadeInDuration, easing = LinearOutSlowInEasing)
            )
        }

        // 2️⃣ Bounce
        delay(bounceDelay)
        scale.animateTo(
            1f,
            spring(dampingRatio = 0.5f)
        )

        // 3️⃣ Char by char fade
        delay(textStartDelay)
        textOpacity.forEachIndexed { i, anim ->
            launch {
                anim.animateTo(
                    1f,
                    tween(150)
                )
            }
            delay(50)
        }

        // 4️⃣ Fade out all
        delay(fadeOutDelay)
        launch {
            opacity.animateTo(0f, tween(fadeOutDuration))
        }
        launch {
            scale.animateTo(0.9f, tween(fadeOutDuration))
        }
        textOpacity.forEach { anim ->
            launch {
                anim.animateTo(0f, tween(fadeOutDuration))
            }
        }

        delay(fadeOutDuration.toLong())
        onFinish()
    }

    // -----------------------------------------------------
    // UI (exact same layout as SwiftUI)
    // -----------------------------------------------------
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // App Icon
            Image(
                painter = painterResource(id = R.drawable.app_icon),
                contentDescription = null,
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        alpha = opacity.value
                        shape = RoundedCornerShape(32.dp)
                        clip = true
                    }
                    .shadow(
                        20.dp,
                        spotColor = Color.Black.copy(alpha = 0.15f),
                        ambientColor = Color.Black.copy(alpha = 0.15f)
                    )
            )

            Spacer(modifier = Modifier.height(16.dp))
            Box(contentAlignment = Alignment.Center) {

                // 1️⃣ Character-by-character stagger fade layer
                Row {
                    characters.forEachIndexed { idx, ch ->
                        Text(
                            text = ch.toString(),
                            style = AppFont.ibmPlexSans(30, FontWeight.SemiBold),
                            color = Color.White,
                            modifier = Modifier.graphicsLayer {
                                alpha = textOpacity[idx].value
                            }
                        )
                    }
                }

                // 2️⃣ Full-word gradient text (Material 3 compatible)
                Text(
                    text = appName,
                    style = AppFont.ibmPlexSans(30, FontWeight.SemiBold).copy(
                        brush = gradientBrush       // ✔️ WORKS in Material 3
                    ),
                    color = Color.Unspecified       // must use Unspecified with brush
                )
            }
        }
    }
}