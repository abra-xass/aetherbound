package com.aetherbound.game.render.animation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/** Maps envelope choice to easing for the caster motion + projectile launch. */
fun TimingEnvelope.toEasing(): Easing = when (this) {
    TimingEnvelope.SNAP -> CubicBezierEasing(0.95f, 0f, 0.05f, 1f)
    TimingEnvelope.CHARGE -> CubicBezierEasing(0.4f, 0f, 0.9f, 0.5f)
    TimingEnvelope.PULSE -> CubicBezierEasing(0.5f, 0f, 0.5f, 1f)
    TimingEnvelope.DRAWN -> CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
    TimingEnvelope.INSTANT -> Easing { 1f }
    TimingEnvelope.DANCE -> Easing { t -> (sin(t * 2f * PI.toFloat()) * 0.5f + 0.5f) }
}

/** Decaying oscillation 1 -> 0 for shake / glow falloff. */
fun decay(t: Float, freq: Float = 14f, k: Float = 6f): Float {
    return (cos(t * freq) * exp(-k * t).toFloat())
}
