package com.aetherbound.game.render.world

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.aetherbound.game.render.theme.AetherColors
import kotlin.math.sin

/**
 * Namaris Harbor's signature landmark. Pulsing lantern, vector-only.
 * Sized to caller's modifier; designed to read at ~80×220 dp.
 */
@Composable
fun Lighthouse(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "lighthouse")
    val pulse by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val sweep by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(5400, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        // base
        drawRect(Color(0xFF6B4D2F), Offset(w * 0.35f, h * 0.85f), Size(w * 0.30f, h * 0.10f))
        // shaft (gradient cream + red bands)
        val shaft = Brush.verticalGradient(
            0f to Color(0xFFEFE5C8), 1f to Color(0xFFCDC1A2),
        )
        drawRect(shaft, Offset(w * 0.32f, h * 0.18f), Size(w * 0.36f, h * 0.67f))
        // red band
        drawRect(Color(0xFF7A2C28),
            Offset(w * 0.32f, h * 0.45f), Size(w * 0.36f, h * 0.06f))
        drawRect(Color(0xFF7A2C28),
            Offset(w * 0.32f, h * 0.62f), Size(w * 0.36f, h * 0.06f))
        // lamp room
        drawRect(Color(0xFF1A2233),
            Offset(w * 0.28f, h * 0.10f), Size(w * 0.44f, h * 0.10f))
        drawRect(AetherColors.GoldDeep,
            Offset(w * 0.28f, h * 0.10f), Size(w * 0.44f, h * 0.10f),
            style = Stroke(2f))
        // dome roof
        val dome = Path().apply {
            moveTo(w * 0.25f, h * 0.10f)
            quadraticBezierTo(w * 0.50f, -h * 0.02f, w * 0.75f, h * 0.10f)
            close()
        }
        drawPath(dome, Color(0xFF7A2C28))
        // lamp glow
        val glowR = w * (0.18f + 0.12f * pulse)
        drawCircle(
            color = AetherColors.GoldHighlight.copy(alpha = 0.4f + 0.3f * pulse),
            radius = glowR,
            center = Offset(w * 0.5f, h * 0.155f),
            blendMode = BlendMode.Plus,
        )
        drawCircle(
            color = AetherColors.GoldBright,
            radius = w * 0.06f,
            center = Offset(w * 0.5f, h * 0.155f),
        )
        // sweep cone (vector beam)
        val len = w * 1.4f
        val ang = Math.toRadians(sweep.toDouble())
        val ex = (w * 0.5 + Math.cos(ang) * len).toFloat()
        val ey = (h * 0.155 + Math.sin(ang) * len).toFloat()
        val cone = Path().apply {
            moveTo(w * 0.5f, h * 0.155f)
            lineTo(
                (w * 0.5f + (ex - w * 0.5f) + sin(ang + 0.20).toFloat() * 30f),
                (h * 0.155f + (ey - h * 0.155f) + sin(ang + 0.20).toFloat() * 30f),
            )
            lineTo(
                (w * 0.5f + (ex - w * 0.5f) - sin(ang - 0.20).toFloat() * 30f),
                (h * 0.155f + (ey - h * 0.155f) - sin(ang - 0.20).toFloat() * 30f),
            )
            close()
        }
        drawPath(cone, Brush.radialGradient(
            colors = listOf(AetherColors.GoldHighlight.copy(alpha = 0.30f), Color.Transparent),
            center = Offset(w * 0.5f, h * 0.155f), radius = len,
        ), blendMode = BlendMode.Plus)
    }
}
