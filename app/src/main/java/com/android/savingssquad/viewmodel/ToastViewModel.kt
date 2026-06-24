package com.android.savingssquad.viewmodel

import android.R.attr.alpha
import android.R.attr.scaleX
import android.R.attr.scaleY
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.android.savingssquad.singleton.AppColors
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
    var showText by remember { mutableStateOf(false) }

    LaunchedEffect(state.isShowing) {

        if (state.isShowing) {

            animateTick = false
            showText = false

            // Step 1 - tick pop
            animateTick = true

            delay(200.milliseconds)

            // Step 2 - text fade
            showText = true
        }
    }

    if (state.isShowing) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(260.dp)
                    .background(Color.White, RoundedCornerShape(18.dp))
                    .border(
                        1.dp,
                        when (state.type) {
                            ToastType.SUCCESS -> AppColors.successAccent
                            ToastType.ERROR -> AppColors.errorAccent
                            ToastType.INFO -> AppColors.infoAccent
                        }.copy(alpha = 0.25f),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(vertical = 22.dp, horizontal = 18.dp)
                    .graphicsLayer {
                        scaleX = if (animateTick) 1f else 0.85f
                        scaleY = if (animateTick) 1f else 0.85f
                        alpha = if (animateTick) 1f else 0f
                    }
            ) {

                // MARK: - ANIMATED TICK
                val color = when (state.type) {
                    ToastType.SUCCESS -> AppColors.successAccent
                    ToastType.ERROR -> AppColors.errorAccent
                    ToastType.INFO -> AppColors.infoAccent
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(54.dp)
                ) {

                    CircularProgressIndicator(
                        progress = 1f,
                        color = color.copy(alpha = 0.2f),
                        strokeWidth = 3.dp,
                        modifier = Modifier.fillMaxSize()
                    )

                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier
                            .size(26.dp)
                            .scale(if (animateTick) 1f else 0.2f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // TITLE
                if (state.title.isNotEmpty()) {
                    Text(
                        text = state.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.headerText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .alpha(if (showText) 1f else 0f)
                    )
                }

                // MESSAGE
                Text(
                    text = state.message,
                    fontSize = 13.sp,
                    color = AppColors.secondaryText,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    modifier = Modifier
                        .alpha(if (showText) 1f else 0f)
                )
            }
        }
    }
}