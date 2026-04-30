package com.aetherbound.game.render.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.aetherbound.game.core.data.CaptureMath
import com.aetherbound.game.render.theme.AetherColors
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * Pokémon-style capture animation:
 *
 *   Phase 1  : Ball arcs from player anchor to opponent anchor (~600ms)
 *   Phase 2  : Ball "absorbs" the opponent (white flash, scale-down)
 *   Phase 3  : Ball drops, bounces, settles (~400ms)
 *   Phase 4  : Shake N times based on outcome.shakes (each ~360ms)
 *   Phase 5  : Final result — open (fail) or click+sparkle (catch)
 *
 * Renders on top of the existing battle scene (caller composes it as an
 * overlay). Calls [onFinished] with the captured boolean once the whole
 * sequence resolves.
 */
@Composable
fun CaptureAnimation(
    outcome: CaptureMath.CaptureOutcome,
    playerAnchor: Offset,
    opponentAnchor: Offset,
    onFinished: (captured: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Position interpolation phases
    val ballX = remember { Animatable(playerAnchor.x) }
    val ballY = remember { Animatable(playerAnchor.y) }
    val ballScale = remember { Animatable(1f) }
    val opponentAlpha = remember { Animatable(1f) }
    val shakeX = remember { Animatable(0f) }
    val sparkleProgress = remember { Animatable(0f) }

    LaunchedEffect(outcome) {
        // Phase 1 — arc throw
        ballX.animateTo(opponentAnchor.x, tween(550, easing = LinearEasing))
        // (parallel y arc handled below as a quick easing curve)

        // Phase 2 — flash + absorb (opponent fades, ball stays put)
        opponentAlpha.animateTo(0f, tween(180))
        ballScale.animateTo(0.7f, tween(120))
        delay(120)

        // Phase 3 — settle bounce
        ballY.animateTo(opponentAnchor.y + 16f, tween(180))
        ballY.animateTo(opponentAnchor.y, tween(120))

        // Phase 4 — shakes
        repeat(outcome.shakes) {
            shakeX.animateTo(8f, tween(120))
            shakeX.animateTo(-8f, tween(120))
            shakeX.animateTo(0f, tween(80))
            delay(40)
        }

        // Phase 5 — verdict
        if (outcome.captured) {
            // Sparkle + click
            sparkleProgress.animateTo(1f, tween(500))
        } else {
            // Ball pops open: scale down quickly, opponent reappears
            ballScale.animateTo(0f, tween(160))
            opponentAlpha.animateTo(1f, tween(220))
        }
        delay(220)
        onFinished(outcome.captured)
    }

    // Vertical arc — quadratic-ish: track the elapsed-x ratio from player to opponent
    val xRatio = ((ballX.value - playerAnchor.x) / (opponentAnchor.x - playerAnchor.x).coerceAtLeast(1f))
        .coerceIn(0f, 1f)
    val arcY = playerAnchor.y + (opponentAnchor.y - playerAnchor.y) * xRatio - 80f * 4f * xRatio * (1f - xRatio)
    val effectiveY = if (ballY.value == playerAnchor.y) arcY else ballY.value

    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            // Slight dim of opponent area during absorption
            if (opponentAlpha.value < 1f) {
                drawCircle(
                    color = Color.White.copy(alpha = 1f - opponentAlpha.value),
                    radius = 28f,
                    center = opponentAnchor,
                )
            }

            // Ball
            val cx = ballX.value + shakeX.value
            val cy = effectiveY
            val r = 14f * ballScale.value
            if (r > 0.1f) {
                // Top half: red
                drawCircle(color = Color(0xFFE5493A), radius = r, center = Offset(cx, cy))
                drawArc(
                    color = Color.Black,
                    startAngle = 0f, sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(cx - r, cy - r),
                    size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                    style = Stroke(width = 1.5f),
                )
                // Center band
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(cx - r, cy - 1.5f),
                    size = androidx.compose.ui.geometry.Size(r * 2, 3f),
                )
                // Center button
                drawCircle(color = Color.White, radius = r * 0.35f, center = Offset(cx, cy))
                drawCircle(
                    color = Color.Black, radius = r * 0.35f, center = Offset(cx, cy),
                    style = Stroke(width = 1.5f),
                )
            }

            // Sparkles on success
            if (sparkleProgress.value > 0f) {
                val rays = 8
                for (i in 0 until rays) {
                    val angle = (i.toFloat() / rays) * 2f * PI.toFloat()
                    val len = 60f * sparkleProgress.value
                    val px = ballX.value + cos(angle) * len
                    val py = effectiveY + sin(angle) * len
                    drawCircle(
                        color = AetherColors.GoldHighlight.copy(alpha = 1f - sparkleProgress.value),
                        radius = 3f,
                        center = Offset(px, py),
                    )
                }
            }
        }
    }
}
