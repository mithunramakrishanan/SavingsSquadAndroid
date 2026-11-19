package com.android.savingssquad.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8AD7B6),
    onPrimary = Color(0xFF103D35),
    secondary = Color(0xFFE3A874),
    onSecondary = Color(0xFF2D1B0B),
    tertiary = Color(0xFF6CC6A0),
    background = Color(0xFF10211B),
    surface = Color(0xFF182D25),
    onBackground = Color(0xFFE6F0E9),
    onSurface = Color(0xFFE6F0E9)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF126B58),
    onPrimary = Color.White,
    secondary = Color(0xFFD78448),
    onSecondary = Color.White,
    tertiary = Color(0xFF1BA56C),
    background = Color(0xFFEEF3EF),
    surface = Color(0xFFFCFDFC),
    onBackground = Color(0xFF112821),
    onSurface = Color(0xFF112821)
)

@Composable
fun SavingsSquadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}