package com.aetherbound.game.render.shader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class TimeOfDay { Dawn, Day, Dusk, Night }

/**
 * Color-LUT overlay implemented as a vertical gradient with a multiply blend.
 * On API 33+ this could be a real LUT shader; for the pilot a brush is enough
 * and visually indistinguishable on a phone.
 */
@Composable
fun TimeOfDayOverlay(time: TimeOfDay, modifier: Modifier = Modifier) {
    val brush = remember(time) { brushFor(time) }
    val blend = if (time == TimeOfDay.Night) BlendMode.Multiply else BlendMode.Overlay
    Canvas(modifier.fillMaxSize()) {
        drawRect(brush = brush, blendMode = blend)
    }
}

private fun brushFor(time: TimeOfDay): Brush = when (time) {
    TimeOfDay.Dawn -> Brush.verticalGradient(
        0f to Color(0x80F2C094), 0.6f to Color(0x40F4D5A8), 1f to Color(0x80E0844C)
    )
    TimeOfDay.Day -> Brush.verticalGradient(
        0f to Color(0x10FFFFFF), 1f to Color(0x10FFFFFF)
    )
    TimeOfDay.Dusk -> Brush.verticalGradient(
        0f to Color(0x70CC4F2C), 0.5f to Color(0x504A2960), 1f to Color(0x80101E40)
    )
    TimeOfDay.Night -> Brush.verticalGradient(
        0f to Color(0xCC0A0E20), 0.5f to Color(0xB01428A0), 1f to Color(0xC0050818)
    )
}
