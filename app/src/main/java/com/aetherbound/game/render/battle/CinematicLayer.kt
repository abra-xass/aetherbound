package com.aetherbound.game.render.battle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.aetherbound.game.core.CinematicLevel
import com.aetherbound.game.render.animation.AttackAnimationState

/**
 * Renders the cinematic overlay (letterbox bars + full-screen flash) when an
 * attack with CinematicLevel.Big or Full is playing. Sits on top of the battle
 * scene's sprite layer, below the perf overlay.
 *
 * Slow-motion is implemented elsewhere — this layer is pure visual.
 */
@Composable
fun CinematicLayer(
    state: AttackAnimationState?,
    modifier: Modifier = Modifier,
) {
    if (state == null) return
    val level = state.recipe.cinematic
    if (level == CinematicLevel.None) return

    val duration = state.recipe.effectiveDurationMs.toFloat()
    val hit = state.recipe.effectiveHitFrameMs.toFloat()
    val now = state.elapsedMs

    // Letterbox: bars slide in over first 20% of duration, hold, slide out at end
    val barFraction = level.letterboxFraction
    val barProgress = when {
        now < duration * 0.20f -> (now / (duration * 0.20f)).coerceIn(0f, 1f)
        now > duration * 0.85f -> ((duration - now) / (duration * 0.15f)).coerceIn(0f, 1f)
        else -> 1f
    }
    val barH = barFraction * barProgress

    // Full-screen flash on hit frame, peaking at hit, decaying ~280ms after
    val flashAlpha = when {
        now < hit -> 0f
        else -> {
            val t = (now - hit) / 280f
            (level.fullscreenFlashAlpha * (1f - t)).coerceAtLeast(0f)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            // Letterbox bars top + bottom
            if (barH > 0f) {
                val pxH = size.height * barH
                drawRect(Color.Black, topLeft = Offset.Zero, size = Size(size.width, pxH))
                drawRect(Color.Black, topLeft = Offset(0f, size.height - pxH), size = Size(size.width, pxH))
            }
            // Full-screen flash (additive white)
            if (flashAlpha > 0f) {
                drawRect(Color.White.copy(alpha = flashAlpha))
            }
        }
    }
}

/**
 * Computes the per-frame time scale to apply to attack animation advancement
 * during cinematic slow-motion (Storm / Legendary attacks). Returns 1.0 if
 * not in slow-mo window. Caller multiplies its dt by this when calling
 * `state.advance(dt * timeScale)`.
 */
fun cinematicTimeScale(state: AttackAnimationState): Float {
    val level = state.recipe.cinematic
    if (level == CinematicLevel.None) return 1f
    val hit = state.recipe.effectiveHitFrameMs.toFloat()
    val now = state.elapsedMs
    val slowDur = level.slowMoDurationMs.toFloat()
    if (slowDur <= 0f) return 1f
    val sinceHit = now - hit
    return if (sinceHit in 0f..slowDur) level.slowMoTimeScale else 1f
}
