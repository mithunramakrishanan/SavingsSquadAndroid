package com.android.savingssquad.viewmodel

import android.R.attr.alpha
import android.R.attr.scaleX
import android.R.attr.scaleY
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.android.savingssquad.singleton.AppColors
import com.android.savingssquad.singleton.AppFont
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class ToastState(
    val isShowing: Boolean = false,
    val title: String = "",
    val message: String = "",
    val type: ToastType = ToastType.INFO
)

enum class ToastType {
    SUCCESS, ERROR, INFO
}

object ToastManager {

    private val _state = mutableStateOf(ToastState())

    val state: State<ToastState> = _state

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun show(

        title: String = "",

        message: String,

        type: ToastType = ToastType.INFO

    ) {

        _state.value = ToastState(

            isShowing = true,

            title = title,

            message = message,

            type = type

        )

        scope.launch {

            delay(2200.milliseconds)

            hide()

        }

    }

    fun hide() {

        _state.value = _state.value.copy(isShowing = false)

    }

}

@Composable
fun SSToast() {

    val state = ToastManager.state.value

    var animateTick by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(state.isShowing) {

        if (state.isShowing) {

            animateTick = false
            showContent = false

            delay(100)

            animateTick = true

            delay(150)

            showContent = true
        }
    }

    if (state.isShowing) {

        val color = when (state.type) {
            ToastType.SUCCESS -> AppColors.successAccent
            ToastType.ERROR -> AppColors.errorAccent
            ToastType.INFO -> AppColors.infoAccent
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(260.dp)
                    .shadow(
                        elevation = 22.dp,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .background(
                        AppColors.surface,
                        RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = color.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(
                        vertical = 22.dp,
                        horizontal = 18.dp
                    )
            ) {

                // MARK: - Animated Icon

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(54.dp)
                ) {

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .scale(
                                animateFloatAsState(
                                    if (animateTick) 1f else 0.6f,
                                    label = ""
                                ).value
                            )
                            .alpha(
                                animateFloatAsState(
                                    if (animateTick) 1f else 0f,
                                    label = ""
                                ).value
                            )
                            .border(
                                3.dp,
                                color.copy(alpha = 0.2f),
                                CircleShape
                            )
                    )

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .scale(
                                animateFloatAsState(
                                    if (animateTick) 1f else 0.6f,
                                    label = ""
                                ).value
                            )
                            .alpha(
                                animateFloatAsState(
                                    if (animateTick) 1f else 0f,
                                    label = ""
                                ).value
                            )
                            .background(
                                color.copy(alpha = 0.12f),
                                CircleShape
                            )
                    )

                    Icon(
                        imageVector = when (state.type) {
                            ToastType.SUCCESS -> Icons.Default.CheckCircle
                            ToastType.ERROR -> Icons.Default.Error
                            ToastType.INFO -> Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier
                            .size(22.dp)
                            .scale(
                                animateFloatAsState(
                                    if (animateTick) 1f else 0.3f,
                                    label = ""
                                ).value
                            )
                            .alpha(
                                animateFloatAsState(
                                    if (animateTick) 1f else 0f,
                                    label = ""
                                ).value
                            )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (state.title.isNotEmpty()) {

                    Text(
                        text = state.title,
                        style = AppFont.ibmPlexSans(
                            16,
                            FontWeight.SemiBold
                        ),
                        color = AppColors.headerText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .alpha(if (showContent) 1f else 0f)
                            .offset(y = if (showContent) 0.dp else 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = state.message,
                    style = AppFont.ibmPlexSans(13),
                    color = AppColors.secondaryText,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    modifier = Modifier
                        .alpha(if (showContent) 1f else 0f)
                        .offset(y = if (showContent) 0.dp else 8.dp)
                )
            }
        }
    }
}