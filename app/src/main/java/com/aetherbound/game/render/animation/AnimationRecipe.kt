package com.aetherbound.game.render.animation

import com.aetherbound.game.core.Aspect
import com.aetherbound.game.core.CinematicLevel
import com.aetherbound.game.core.PowerTier

/**
 * 6-axis animation recipe. 450 unique attacks come from combining ~80 reusable
 * primitives along these axes; only the recipe data changes, not the renderer.
 *
 * Power-driven scaling: when [power] is set, the recipe automatically picks
 * a [PowerTier] which scales durationMs / particleBudget / cameraShake /
 * cinematic to match the attack's strength. Explicit non-null overrides win.
 */
data class AnimationRecipe(
    val techniqueId: String,
    val techniqueName: String,
    val aspect: Aspect,
    val casterMotion: CasterMotion,
    val projectilePath: ProjectilePath,
    val impactVisual: ImpactVisual,
    val shader: ShaderEffect,
    val envelope: TimingEnvelope,
    /** Move power 10..300; drives the visual tier. */
    val power: Int = 50,
    /** Explicit overrides: leave null to derive from PowerTier. */
    val durationMs: Int? = null,
    val hitFrameMs: Int? = null,
    val cameraShake: Float? = null,
    val particleBudget: Int? = null,
) {
    val tier: PowerTier get() = PowerTier.forPower(power)
    val cinematic: CinematicLevel get() = tier.cinematic

    val effectiveDurationMs: Int get() = durationMs ?: tier.durationMs
    val effectiveHitFrameMs: Int get() =
        hitFrameMs ?: (effectiveDurationMs * 4 / 10)
    val effectiveParticleBudget: Int get() = particleBudget ?: tier.particleBudget
    val effectiveCameraShake: Float get() = cameraShake ?: tier.shakeIntensity
    val effectiveBloomRadiusPx: Float get() = tier.bloomRadiusPx
}

enum class CasterMotion { BRACE, LUNGE, SPIN, FLOAT, DIVE, SLAM, DRAW }

enum class ProjectilePath { ARC, BEAM, HOMING, MULTI, SPIRAL, WAVE, AURA_SELF, GROUND_RUSH }

enum class ImpactVisual { SHATTER, RING_POP, BLOOM, CRATER, FREEZE, BIND_VINES, SPARK_FORK, MIST }

enum class ShaderEffect { NONE, BLOOM, HEAT, RIPPLE, FREEZE_REFRACT }

enum class TimingEnvelope { SNAP, CHARGE, PULSE, DRAWN, INSTANT, DANCE }
