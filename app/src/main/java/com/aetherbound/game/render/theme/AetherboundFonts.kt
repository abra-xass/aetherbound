package com.aetherbound.game.render.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aetherbound.game.R

/**
 * Locked Thot fonts. Cinzel for display, Cormorant Italic for accents,
 * Inter for body. Never system / Roboto.
 */
val CinzelFamily = FontFamily(
    Font(R.font.cinzel, FontWeight.Normal),
    Font(R.font.cinzel_semibold, FontWeight.SemiBold),
)

val CormorantFamily = FontFamily(
    Font(R.font.cormorant_italic, FontWeight.Normal, FontStyle.Italic),
)

val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

val AetherTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = CinzelFamily, fontWeight = FontWeight.SemiBold, fontSize = 36.sp,
        letterSpacing = 0.5.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = CinzelFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = CinzelFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp,
        letterSpacing = 0.4.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = CinzelFamily, fontWeight = FontWeight.Normal, fontSize = 18.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = CinzelFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
        letterSpacing = 0.6.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = CormorantFamily, fontWeight = FontWeight.Normal, fontStyle = FontStyle.Italic,
        fontSize = 14.sp,
    ),
)
