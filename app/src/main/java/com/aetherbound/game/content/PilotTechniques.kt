package com.aetherbound.game.content

import com.aetherbound.game.core.Aspect
import com.aetherbound.game.core.Technique
import com.aetherbound.game.core.TechniqueCategory
import com.aetherbound.game.render.animation.AnimationRecipe
import com.aetherbound.game.render.animation.CasterMotion
import com.aetherbound.game.render.animation.ImpactVisual
import com.aetherbound.game.render.animation.ProjectilePath
import com.aetherbound.game.render.animation.ShaderEffect
import com.aetherbound.game.render.animation.TimingEnvelope

/**
 * Pilot moveset. Powers cover all 5 PowerTiers so the cinematic system is
 * fully exercisable in a single battle:
 *   Snap (35-49)      — Ember Snap, Verdance Bind
 *   Strike (50-89)    — Ember Lance, Tide Surge, Spark Hit (priority), Frost Pulse
 *   Storm (140-199)   — Solar Pyre (cinematic Big)
 *   Legendary (200+)  — Aether Cascade (cinematic Full: letterbox + slow-mo)
 */
object PilotTechniques {

    val EmberSnap = Technique(
        id = "TC001", name = "Ember Snap", aspect = Aspect.FIRE,
        category = TechniqueCategory.STRIKE, power = 40, accuracy = 100, priority = 0,
    )
    val EmberLance = Technique(
        id = "TC002", name = "Ember Lance", aspect = Aspect.FIRE,
        category = TechniqueCategory.PULSE, power = 55, accuracy = 95, priority = 0,
    )
    val SparkHit = Technique(
        id = "TC003", name = "Spark Hit", aspect = Aspect.LIGHTNING,
        category = TechniqueCategory.STRIKE, power = 40, accuracy = 100, priority = 1,
    )
    val TideSurge = Technique(
        id = "TC004", name = "Tide Surge", aspect = Aspect.WATER,
        category = TechniqueCategory.PULSE, power = 50, accuracy = 95, priority = 0,
    )
    val FrostPulse = Technique(
        id = "TC005", name = "Frost Pulse", aspect = Aspect.FROST,
        category = TechniqueCategory.PULSE, power = 45, accuracy = 100, priority = 0,
    )
    val VerdanceBind = Technique(
        id = "TC006", name = "Verdance Bind", aspect = Aspect.WOOD,
        category = TechniqueCategory.BIND, power = 35, accuracy = 90, priority = -1,
    )

    /** Storm tier — heavy hitter, exercises CinematicLevel.Big */
    val SolarPyre = Technique(
        id = "TC050", name = "Solar Pyre", aspect = Aspect.HEROIC,
        category = TechniqueCategory.PULSE, power = 150, accuracy = 90, priority = 0,
    )

    /** Legendary tier — only 13 of 450 in the full game; exercises CinematicLevel.Full */
    val AetherCascade = Technique(
        id = "TC450", name = "Aether Cascade", aspect = Aspect.VENOM,
        category = TechniqueCategory.PULSE, power = 280, accuracy = 85, priority = 0,
    )

    /**
     * Recipes are now power-driven: durationMs/particleBudget/cameraShake all
     * derive from the tech's power via PowerTier. The recipe only describes
     * the *composition* (motion, path, impact, shader, envelope, color).
     */
    val recipes: Map<String, AnimationRecipe> = mapOf(
        EmberSnap.id to AnimationRecipe(
            techniqueId = EmberSnap.id, techniqueName = EmberSnap.name, aspect = Aspect.FIRE,
            casterMotion = CasterMotion.LUNGE, projectilePath = ProjectilePath.ARC,
            impactVisual = ImpactVisual.SHATTER, shader = ShaderEffect.HEAT,
            envelope = TimingEnvelope.SNAP, power = EmberSnap.power,
        ),
        EmberLance.id to AnimationRecipe(
            techniqueId = EmberLance.id, techniqueName = EmberLance.name, aspect = Aspect.FIRE,
            casterMotion = CasterMotion.DRAW, projectilePath = ProjectilePath.BEAM,
            impactVisual = ImpactVisual.BLOOM, shader = ShaderEffect.HEAT,
            envelope = TimingEnvelope.CHARGE, power = EmberLance.power,
        ),
        SparkHit.id to AnimationRecipe(
            techniqueId = SparkHit.id, techniqueName = SparkHit.name, aspect = Aspect.LIGHTNING,
            casterMotion = CasterMotion.SPIN, projectilePath = ProjectilePath.MULTI,
            impactVisual = ImpactVisual.SPARK_FORK, shader = ShaderEffect.BLOOM,
            envelope = TimingEnvelope.PULSE, power = SparkHit.power,
        ),
        TideSurge.id to AnimationRecipe(
            techniqueId = TideSurge.id, techniqueName = TideSurge.name, aspect = Aspect.WATER,
            casterMotion = CasterMotion.FLOAT, projectilePath = ProjectilePath.WAVE,
            impactVisual = ImpactVisual.RING_POP, shader = ShaderEffect.RIPPLE,
            envelope = TimingEnvelope.DRAWN, power = TideSurge.power,
        ),
        FrostPulse.id to AnimationRecipe(
            techniqueId = FrostPulse.id, techniqueName = FrostPulse.name, aspect = Aspect.FROST,
            casterMotion = CasterMotion.BRACE, projectilePath = ProjectilePath.GROUND_RUSH,
            impactVisual = ImpactVisual.FREEZE, shader = ShaderEffect.FREEZE_REFRACT,
            envelope = TimingEnvelope.CHARGE, power = FrostPulse.power,
        ),
        VerdanceBind.id to AnimationRecipe(
            techniqueId = VerdanceBind.id, techniqueName = VerdanceBind.name, aspect = Aspect.WOOD,
            casterMotion = CasterMotion.SLAM, projectilePath = ProjectilePath.AURA_SELF,
            impactVisual = ImpactVisual.BIND_VINES, shader = ShaderEffect.NONE,
            envelope = TimingEnvelope.DANCE, power = VerdanceBind.power,
        ),
        SolarPyre.id to AnimationRecipe(
            techniqueId = SolarPyre.id, techniqueName = SolarPyre.name, aspect = Aspect.HEROIC,
            casterMotion = CasterMotion.DRAW, projectilePath = ProjectilePath.BEAM,
            impactVisual = ImpactVisual.BLOOM, shader = ShaderEffect.BLOOM,
            envelope = TimingEnvelope.CHARGE, power = SolarPyre.power,
        ),
        AetherCascade.id to AnimationRecipe(
            techniqueId = AetherCascade.id, techniqueName = AetherCascade.name, aspect = Aspect.VENOM,
            casterMotion = CasterMotion.FLOAT, projectilePath = ProjectilePath.SPIRAL,
            impactVisual = ImpactVisual.RING_POP, shader = ShaderEffect.BLOOM,
            envelope = TimingEnvelope.DRAWN, power = AetherCascade.power,
        ),
    )
}
