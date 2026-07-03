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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
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
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun LaunchView(
    showLaunchView: Boolean,
    onFinish: () -> Unit
) {
    if (!showLaunchView) return

    var iconScale by remember { mutableStateOf(0.6f) }
    var contentOpacity by remember { mutableStateOf(0f) }
    var showBadges by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {

        val iconAnim = Animatable(0.6f)
        val fadeAnim = Animatable(0f)

        launch {
            iconAnim.animateTo(
                1f,
                spring(dampingRatio = 0.75f)
            )
        }

        launch {
            snapshotFlow { iconAnim.value }.collect {
                iconScale = it
            }
        }

        delay(250)

        launch {
            fadeAnim.animateTo(
                1f,
                tween(900)
            )
        }

        launch {
            snapshotFlow { fadeAnim.value }.collect {
                contentOpacity = it
            }
        }

        delay(450)
        showBadges = true

        delay(3000)

        launch {
            fadeAnim.animateTo(0f, tween(400))
        }

        delay(400)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.weight(1f))

            // ================= LOGO SECTION =================
            Box(
                modifier = Modifier
                    .scale(iconScale)
                    .padding(bottom = 28.dp),
                contentAlignment = Alignment.Center
            ) {

                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .background(
                            AppColors.primaryBrand.copy(alpha = 0.12f),
                            CircleShape
                        )
                )

                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .background(
                            AppColors.surface,
                            RoundedCornerShape(40.dp)
                        )
                        .shadow(0.dp, RoundedCornerShape(40.dp))
                )

                Image(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp)
                )
            }

            // ================= TITLE =================
            Column(
                modifier = Modifier
                    .alpha(contentOpacity)
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    "Savings Squad",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.headerText
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Where Savings Become Success",
                    fontSize = 15.sp,
                    color = AppColors.secondaryText
                )
            }

            // ================= BADGES =================
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .alpha(contentOpacity)
                    .padding(bottom = 36.dp)
            ) {
                LaunchBadge("Secure", showBadges, 0)
                LaunchBadge("Smart", showBadges, 120)
                LaunchBadge("Transparent", showBadges, 240)
            }

            Spacer(modifier = Modifier.weight(1.2f))

            // ================= LOADER =================
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(contentOpacity)
                    .padding(bottom = 20.dp)
            ) {

                CircularProgressIndicator(
                    color = AppColors.loaderColor
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Initializing your financial workspace...",
                    fontSize = 13.sp,
                    color = AppColors.secondaryText
                )
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun LaunchBadge(
    title: String,
    visible: Boolean,
    delayMs: Int
) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.7f) }
    val offsetY = remember { Animatable(10f) }

    LaunchedEffect(visible) {
        if (visible) {
            delay(delayMs.toLong())

            launch {
                alpha.animateTo(1f, animationSpec = spring())
            }
            launch {
                scale.animateTo(1f, animationSpec = spring())
            }
            launch {
                offsetY.animateTo(0f, animationSpec = spring())
            }
        }
    }

    Box(
        modifier = Modifier
            .graphicsLayer {
                this.alpha = alpha.value
                scaleX = scale.value
                scaleY = scale.value
                translationY = offsetY.value
            }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, AppColors.primaryBrand.copy(alpha = 0.15f), CircleShape)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.primaryBrand
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LaunchViewPreview() {

    LaunchView(
        showLaunchView = true,
        onFinish = {}
    )
}