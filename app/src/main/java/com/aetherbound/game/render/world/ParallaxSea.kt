package com.aetherbound.game.render.world

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.aetherbound.game.render.theme.LocalQualityPreset
import kotlin.math.sin

/**
 * Subtle parallax sea wave overlay. The number of layers is read from
 * QualityPreset.parallaxLayers. Wave layers are pure cosine paths drawn as
 * thin strokes — almost free GPU-wise.
 */
@Composable
fun ParallaxSea(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "sea")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val layers = LocalQualityPreset.current.parallaxLayers

    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val baseY = h * 0.22f
        for (l in 0 until layers) {
            val depth = (l + 1f) / layers
            val amp = 6f + l * 4f
            val freq = 0.03f - l * 0.005f
            val speed = (l + 1) * 0.4f
            val color = Color(0x55B5E6F2).copy(alpha = 0.18f + 0.15f * depth)
            val step = 6f
            var x = 0f
            var prevY = baseY + sin((x * freq) + phase * speed * 6.2832f) * amp
            while (x < w) {
                val nx = x + step
                val ny = baseY + sin((nx * freq) + phase * speed * 6.2832f + l * 1.2f) * amp + l * 22f
                drawLine(color, Offset(x, prevY + l * 22f), Offset(nx, ny), strokeWidth = 1.5f)
                prevY = ny - l * 22f
                x = nx
            }
        }
        // shore foam line
        drawLine(
            color = Color(0x66FFFFFF),
            start = Offset(0f, baseY + layers * 22f + 2f),
            end = Offset(w, baseY + layers * 22f + 2f),
            strokeWidth = 1f,
        )
        // outer dark wash
        drawRect(Color(0x22000000),
            topLeft = Offset(0f, 0f),
            size = androidx.compose.ui.geometry.Size(w, baseY * 0.5f))
        // ensure stroke import is actually used somewhere visible
        drawRect(Color.Transparent, style = Stroke(0f))
    }
}
