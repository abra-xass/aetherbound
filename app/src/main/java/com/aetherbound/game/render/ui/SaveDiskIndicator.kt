package com.aetherbound.game.render.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherbound.game.render.theme.AetherColors

/**
 * Tiny gold-floppy-disk that flashes briefly on every successful save.
 * 32 dp icon + tiny "GESPEICHERT" label, slides + fades in/out.
 *
 * Designed to sit anywhere — the parent positions it (typical placement:
 * top-right of [com.aetherbound.game.render.world.TuxemonWorldScene],
 * just under the WeatherHud).
 *
 * @param visible drives the entrance/exit animation
 * @param label optional override of the bottom text (default: "GESPEICHERT")
 */
@Composable
fun SaveDiskIndicator(
    visible: Boolean,
    label: String = "GESPEICHERT",
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.7f),
        exit = fadeOut() + scaleOut(targetScale = 0.7f),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(AetherColors.Obsidian.copy(alpha = 0.92f))
                .border(1.dp, AetherColors.GoldBright, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FloppyDiskIcon(
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    label,
                    color = AetherColors.GoldBright,
                    fontSize = 8.sp,
                )
            }
        }
    }
}

/**
 * Pure-vector floppy disk in metallic-gold. Drawn via Canvas so it
 * scales cleanly on any DPI without needing a PNG asset.
 */
@Composable
private fun FloppyDiskIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val goldGrad = Brush.verticalGradient(
            colors = listOf(
                AetherColors.GoldBright,
                AetherColors.GoldCore,
                AetherColors.GoldDeep,
            ),
            startY = 0f, endY = h,
        )
        // Floppy body — rounded rectangle
        drawRoundRect(
            brush = goldGrad,
            topLeft = Offset(w * 0.08f, h * 0.10f),
            size = Size(w * 0.84f, h * 0.80f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f),
        )
        // Inner outline (dark obsidian)
        drawRoundRect(
            color = AetherColors.ObsidianDeep,
            topLeft = Offset(w * 0.08f, h * 0.10f),
            size = Size(w * 0.84f, h * 0.80f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f),
            style = Stroke(width = w * 0.04f),
        )
        // Metal shutter at top
        drawRect(
            color = AetherColors.Onyx,
            topLeft = Offset(w * 0.30f, h * 0.13f),
            size = Size(w * 0.45f, h * 0.18f),
        )
        drawRect(
            color = AetherColors.GoldDeep,
            topLeft = Offset(w * 0.62f, h * 0.18f),
            size = Size(w * 0.06f, h * 0.10f),
        )
        // Label area at bottom
        drawRect(
            color = AetherColors.Onyx.copy(alpha = 0.85f),
            topLeft = Offset(w * 0.18f, h * 0.42f),
            size = Size(w * 0.64f, h * 0.40f),
        )
        // Three "label lines" inside
        for (i in 0..2) {
            drawRect(
                color = AetherColors.GoldHighlight.copy(alpha = 0.5f),
                topLeft = Offset(w * 0.22f, h * (0.50f + i * 0.10f)),
                size = Size(w * 0.50f, h * 0.04f),
            )
        }
    }
}
