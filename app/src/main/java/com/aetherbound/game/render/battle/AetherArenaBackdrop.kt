package com.aetherbound.game.render.battle

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.aetherbound.game.render.theme.AetherColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Cosmic-void backdrop for multiplayer matches. Distinct from
 * [BattleArenaBackdrop] (single-player) — signals "you're in the Aether
 * Arena, this is a ceremonial match" via:
 *
 *   - dark obsidian → cosmic-purple radial gradient
 *   - 24 soft star-points slowly drifting in two layers
 *   - hexagonal gold aether-ring on the floor, pulsing at 1 Hz
 *   - subtle gold particles falling from above ring-perimeter
 *
 * Reuses Aetherbound's 4-stop gold palette and Obsidian base so it
 * theme-locks with the rest of the app.
 */
@Composable
fun AetherArenaBackdrop(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "aether-arena")
    // Ring pulse 1 Hz
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ring-pulse",
    )
    // Slow rotation for the star field
    val starRot by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(180_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "star-rot",
    )

    val stars = remember {
        // Two layers: 16 small + 8 medium, each at fixed polar coords.
        List(24) { i ->
            val rng = Random(i.toLong() * 17)
            Triple(
                rng.nextFloat() * 2f * PI.toFloat(),  // angle
                0.05f + rng.nextFloat() * 0.95f,        // radius (0..1)
                if (i < 16) 1.2f else 2.4f,             // size px
            )
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A1F4F),   // cosmic-violet centre
                        Color(0xFF101725),   // onyx mid
                        AetherColors.ObsidianDeep,
                    ),
                ),
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val maxR = size.minDimension / 2f * 0.95f

            // ── Star field (rotating) ────────────────────────────
            for ((angle, radius, sizePx) in stars) {
                val theta = angle + starRot * (PI / 180f).toFloat()
                val r = radius * maxR
                val x = cx + cos(theta) * r
                val y = cy * 0.55f + sin(theta) * r * 0.55f  // squashed for floor-perspective
                drawCircle(
                    color = AetherColors.GoldHighlight.copy(alpha = 0.45f),
                    radius = sizePx,
                    center = Offset(x, y),
                )
            }

            // ── Aether-Ring (hexagonal, on the floor) ────────────
            val floorY = size.height * 0.78f
            val ringR = size.width * 0.36f * pulse
            val verts = (0 until 6).map { i ->
                val a = (PI / 3 * i).toFloat() - PI.toFloat() / 2
                Offset(cx + cos(a) * ringR, floorY + sin(a) * ringR * 0.3f)
            }
            // Outer ring stroke (gold)
            for (i in verts.indices) {
                val from = verts[i]
                val to = verts[(i + 1) % verts.size]
                drawLine(
                    brush = Brush.linearGradient(
                        listOf(AetherColors.GoldHighlight, AetherColors.GoldDeep),
                    ),
                    start = from, end = to, strokeWidth = 3f,
                )
            }
            // Inner glow ring
            for (i in verts.indices) {
                val from = verts[i]
                val to = verts[(i + 1) % verts.size]
                drawLine(
                    color = AetherColors.GoldBright.copy(alpha = 0.30f * pulse),
                    start = from, end = to, strokeWidth = 8f,
                )
            }
        }
    }
}
