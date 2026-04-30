package com.aetherbound.game.render.shader

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import com.aetherbound.game.render.theme.LocalQualityPreset
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Compose-native bloom: paints content twice, second pass blurred + tinted +
 * blended additive. Works on every API level. AGSL bloom is identical visually
 * but cheaper for very large blur radii; we cap radius so this is fine.
 *
 * radius is read from QualityPreset; pass 0 to skip.
 */
@Composable
fun BloomLayer(
    tint: Color = Color.White,
    intensity: Float = 0.7f,
    content: @Composable () -> Unit,
) {
    val preset by rememberQualityPreset()
    val radius: Dp = (preset.bloomRadiusPx).dp
    if (radius.value <= 0f) {
        content(); return
    }
    Box {
        content()
        Box(
            Modifier
                .blur(radius)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    alpha = intensity
                }
        ) {
            CompositionLocalProvider(LocalAdditiveTint provides tint) {
                content()
            }
        }
    }
}

@Composable
private fun rememberQualityPreset() = androidx.compose.runtime.rememberUpdatedState(LocalQualityPreset.current)

/** Set by BloomLayer's blurred pass; sprites can pick this up to recolor. */
val LocalAdditiveTint = compositionLocalOf<Color?> { null }

/** Convenience for additive draws: returns BlendMode.Plus only when bloom is active. */
val LocalBloomBlend = compositionLocalOf { BlendMode.SrcOver }
