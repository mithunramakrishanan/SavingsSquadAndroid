package com.android.savingssquad.singleton

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape

import com.android.savingssquad.R

object AppColors {
    // Backgrounds
    val background = Color(0xFFE6EFEB)
    val surface = Color(0xFFFFFFFF)

    // Text
    val headerText = Color(0xFF1A202C)
    val secondaryText = Color(0xFF6B7280)
    val placeholderText = Color(0xFFA0A0A0)

    // Primary Brand Colors
    val primaryBrand = Color(0xFF1A9988)
    val primaryBackground = primaryBrand.copy(alpha = 0.1f)

    // Secondary Accent
    val secondaryAccent = Color(0xFFFF7F50)
    val secondaryBackground = secondaryAccent.copy(alpha = 0.1f)

    // Buttons / Actions
    val primaryButton = primaryBrand
    val primaryButtonText = Color.White
    val disabledButton = Color(0xFFC0C0C0)

    // Accents / Status
    val successAccent = Color(0xFF2ECC71)
    val warningAccent = Color(0xFFF39C12)
    val errorAccent = Color(0xFFE74C3C)
    val infoAccent = Color(0xFF3498DB)

    // Borders / Dividers
    val border = Color(0xFFE0E0E0)

    // Input / Loader
    val textFieldBackground = Color(0xFFFFFFFF)
    val loaderColor = primaryBrand
}

object AppFont {

    // ✅ Load IBM Plex Sans font weights (add these font files in res/font)
    private val ibmPlexSansFamily = FontFamily(
        Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
        Font(R.font.ibm_plex_sans_medium, FontWeight.Medium),
        Font(R.font.ibm_plex_sans_semibold, FontWeight.SemiBold),
        Font(R.font.ibm_plex_sans_bold, FontWeight.Bold)
    )

    /**
     * ✅ Returns a custom IBM Plex Sans TextStyle (like SwiftUI's AppFont.ibmPlexSans)
     * @param size font size in sp
     * @param weight FontWeight (default = Normal)
     */
    fun ibmPlexSans(
        size: Int,
        weight: FontWeight = FontWeight.Normal,
        tamilSizeReduction: Int = 2
    ): TextStyle {

        val finalSize = if (SquadStrings.isTamil) {
            (size - tamilSizeReduction).coerceAtLeast(1)
        } else {
            size
        }

        return TextStyle(
            fontFamily = ibmPlexSansFamily,
            fontWeight = weight,
            fontSize = finalSize.sp
        )
    }
}


data class ShadowStyle(
    val color: Color,
    val radius: androidx.compose.ui.unit.Dp,
    val x: androidx.compose.ui.unit.Dp,
    val y: androidx.compose.ui.unit.Dp
)

object AppShadows {
    // 🔹 Light card shadow
    val card = ShadowStyle(
        color = Color.Black.copy(alpha = 0.05f),
        radius = 5.dp,
        x = 0.dp,
        y = 5.dp
    )

    // 🔹 Elevated / modal shadow
    val elevated = ShadowStyle(
        color = Color.Black.copy(alpha = 0.1f),
        radius = 8.dp,
        x = 0.dp,
        y = 4.dp
    )
}

fun Modifier.appShadow(
    style: ShadowStyle,
    shape: Shape = RectangleShape,
    clip: Boolean = false
): Modifier {
    return this.shadow(
        elevation = style.radius,
        shape = shape,
        clip = clip,
        ambientColor = style.color,
        spotColor = style.color
    )
}