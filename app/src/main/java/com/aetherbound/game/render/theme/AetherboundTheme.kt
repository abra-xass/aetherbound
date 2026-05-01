package com.aetherbound.game.render.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.aetherbound.game.core.Aspect

/**
 * Aetherbound game palette: obsidian navy base, metallic gold 4-stop, red warning.
 * Strictly per Thot design system; deeper / more saturated than ThotTheme because
 * a game needs higher-contrast UI than a chat app.
 */
object AetherColors {
    val Obsidian = Color(0xFF0A0E15)
    val ObsidianDeep = Color(0xFF050709)
    val Onyx = Color(0xFF101725)
    val Slate = Color(0xFF1A2233)
    val SlateLight = Color(0xFF26344E)

    // Gold 4-stop (the signature)
    val GoldHighlight = Color(0xFFFFF3C4)
    val GoldBright = Color(0xFFF5D37A)
    val GoldCore = Color(0xFFD4A043)
    val GoldDeep = Color(0xFF8B5F1A)

    val WarningRed = Color(0xFFE5493A)
    val ParchmentText = Color(0xFFF6F0DA)
    val MutedText = Color(0xFFB8C0CC)

    val GoldGradient: Brush = Brush.verticalGradient(
        0f to GoldHighlight,
        0.30f to GoldBright,
        0.70f to GoldCore,
        1f to GoldDeep,
    )
}

/**
 * Per-Aspect color pair. Used by attack animations (recipe color slot)
 * and by Echoform sprite tinting. Each pair is "primary, secondary".
 */
fun aspectColors(aspect: Aspect): Pair<Color, Color> = when (aspect) {
    Aspect.FIRE -> Color(0xFFF05A28) to Color(0xFFFFD43B)
    Aspect.WATER -> Color(0xFF2F9EEA) to Color(0xFFA7E8FF)
    Aspect.WOOD -> Color(0xFF43A047) to Color(0xFFC8E6C9)
    Aspect.EARTH -> Color(0xFF8D6E4D) to Color(0xFFD7B58A)
    Aspect.SKY -> Color(0xFFB8E0F6) to Color(0xFFE7F5FF)
    Aspect.LIGHTNING -> Color(0xFFFFD43B) to Color(0xFFFFFFE0)
    Aspect.FROST -> Color(0xFF8FCBE1) to Color(0xFFE0F7FA)
    Aspect.METAL -> Color(0xFFB0BEC5) to Color(0xFFECEFF1)
    Aspect.SHADOW -> Color(0xFF311B92) to Color(0xFF7C4DFF)
    Aspect.HEROIC -> Color(0xFFFFE082) to Color(0xFFFFF8E1)
    Aspect.COSMIC -> Color(0xFFA77CFF) to Color(0xFFE1BEE7)
    Aspect.VENOM -> Color(0xFF55D6C2) to Color(0xFFB2DFDB)
    Aspect.NORMAL -> Color(0xFFF5D37A) to Color(0xFFFFF3C4)    // Aether-gold
    Aspect.DREAM -> Color(0xFFE1BEE7) to Color(0xFFF8BBD0)      // pastel dusk
    Aspect.MIND -> Color(0xFF7C4DFF) to Color(0xFFB388FF)       // psychic violet
    Aspect.SOUND -> Color(0xFF00B0FF) to Color(0xFFFF4081)      // sonic blue + neon pink
    Aspect.TIME -> Color(0xFFC9A227) to Color(0xFFEFE5B5)       // bronze + sand
    Aspect.CRYSTAL -> Color(0xFFE0F7FA) to Color(0xFFFFFFFF)    // prismatic white
    Aspect.BLOOD -> Color(0xFFB71C1C) to Color(0xFFEF5350)      // crimson + scarlet
}

/** Render-quality preset, user-toggleable in PerformanceOverlay. */
enum class QualityPreset(
    val maxParticles: Int,
    val activeShaders: Int,
    val bloomRadiusPx: Float,
    val parallaxLayers: Int,
) {
    Quality(maxParticles = 200, activeShaders = 3, bloomRadiusPx = 24f, parallaxLayers = 3),
    Balanced(maxParticles = 100, activeShaders = 2, bloomRadiusPx = 12f, parallaxLayers = 2),
    Battery(maxParticles = 40, activeShaders = 1, bloomRadiusPx = 0f, parallaxLayers = 1),
}

val LocalQualityPreset = staticCompositionLocalOf { QualityPreset.Balanced }

private val AetherDarkColorScheme = darkColorScheme(
    primary = AetherColors.GoldCore,
    onPrimary = AetherColors.Obsidian,
    primaryContainer = AetherColors.Slate,
    onPrimaryContainer = AetherColors.GoldBright,
    secondary = AetherColors.SlateLight,
    onSecondary = AetherColors.ParchmentText,
    background = AetherColors.Obsidian,
    onBackground = AetherColors.ParchmentText,
    surface = AetherColors.Onyx,
    onSurface = AetherColors.ParchmentText,
    surfaceVariant = AetherColors.Slate,
    onSurfaceVariant = AetherColors.MutedText,
    outline = AetherColors.GoldDeep,
    outlineVariant = AetherColors.SlateLight,
    error = AetherColors.WarningRed,
    onError = AetherColors.ParchmentText,
)

@Composable
fun AetherboundTheme(
    quality: QualityPreset = QualityPreset.Balanced,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalQualityPreset provides quality) {
        MaterialTheme(
            colorScheme = AetherDarkColorScheme,
            typography = AetherTypography,
            content = content,
        )
    }
}
