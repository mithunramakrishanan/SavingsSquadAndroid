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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.savingssquad.singleton.AppFont
import com.android.savingssquad.singleton.SquadStrings
import kotlinx.coroutines.delay

@Composable
fun LaunchView(
    showLaunchView: Boolean,
    onFinish: () -> Unit
) {
    if (!showLaunchView) return

    val scale = remember { Animatable(0.9f) }
    val opacity = remember { Animatable(0f) }

    LaunchedEffect(Unit) {

        // Fade + slight zoom
        launch {
            opacity.animateTo(
                1f,
                animationSpec = tween(
                    durationMillis = 800,
                    easing = FastOutSlowInEasing
                )
            )
        }

        launch {
            scale.animateTo(
                1f,
                animationSpec = tween(
                    durationMillis = 800,
                    easing = FastOutSlowInEasing
                )
            )
        }

        // Wait 2 seconds
        delay(2000)

        // Fade out
        opacity.animateTo(
            0f,
            animationSpec = tween(500)
        )

        delay(500)

        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // App Icon
            Image(
                painter = painterResource(id = R.drawable.app_icon),
                contentDescription = null,
                modifier = Modifier
                    .size(110.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        alpha = opacity.value
                    }
                    .clip(RoundedCornerShape(24.dp))
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(24.dp)
                    )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // App Name
            Text(
                text = "Savings Squad",
                style = AppFont.ibmPlexSans(
                    size = 26,
                    weight = FontWeight.SemiBold
                ),
                color = Color.Black,
                modifier = Modifier.graphicsLayer {
                    alpha = opacity.value
                }
            )

            // Tagline
            Text(
                text = "Your Pocket Accountant",
                style = AppFont.ibmPlexSans(
                    size = 12,
                    weight = FontWeight.Normal
                ),
                color = Color.Gray,
                modifier = Modifier
                    .offset(y = (8).dp)
                    .graphicsLayer {
                        alpha = opacity.value
                    }
            )
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_7"
)
@Composable
fun LaunchViewPreview() {
    LaunchView(
        showLaunchView = true,
        onFinish = {}
    )
}