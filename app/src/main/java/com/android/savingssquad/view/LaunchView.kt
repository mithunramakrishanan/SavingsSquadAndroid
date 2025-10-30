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
import androidx.compose.runtime.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun LaunchView(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    // tweak timings if desired
    fadeInDurationMs: Int = 800,
    bounceDelayMs: Long = 800L,
    rotationDelayMs: Long = 600L,
    fadeOutDelayMs: Long = 2200L,
    fadeOutDurationMs: Int = 600,
    imageSizeDp: Int = 140,
    imageResId: Int = R.drawable.app_icon // replace with your drawable id
) {
    // animatable state holders
    val scale = remember { Animatable(0.6f) }
    val opacity = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }

    // animation sequence
    LaunchedEffect(Unit) {
        // Step 1: Fade & Scale In
        scale.animateTo(1.2f, animationSpec = tween(durationMillis = fadeInDurationMs))
        opacity.animateTo(1f, animationSpec = tween(durationMillis = fadeInDurationMs))

        // Step 2: Slight bounce back
        delay(bounceDelayMs)
        scale.animateTo(1.0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))

        // Step 3: Rotation
        delay(rotationDelayMs)
        rotation.animateTo(360f, animationSpec = tween(durationMillis = 1200))

        // Step 4: Fade out after delay
        delay(fadeOutDelayMs)
        opacity.animateTo(0f, animationSpec = tween(durationMillis = fadeOutDurationMs))
        scale.animateTo(0.9f, animationSpec = tween(durationMillis = fadeOutDurationMs))

        // Step 5: wait the fade out to complete then call onFinish
        delay(fadeOutDurationMs.toLong())
        onFinish()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Replace `painterResource(imageResId)` with your own image resource (R.drawable.app_icon)
        Image(
            painter = painterResource(id = imageResId),
            contentDescription = null,
            modifier = Modifier
                .size(imageSizeDp.dp)
                .scale(scale.value)
                .graphicsLayer { rotationZ = rotation.value }
                .alpha(opacity.value)
        )
    }
}

suspend fun animate(
    from: Float,
    to: Float,
    duration: Int,
    onUpdate: (Float) -> Unit
) {
    val anim = androidx.compose.animation.core.Animatable(from)
    anim.animateTo(
        targetValue = to,
        animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing)
    ) {
        onUpdate(this.value)
    }
}