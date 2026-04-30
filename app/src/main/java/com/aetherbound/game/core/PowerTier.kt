package com.aetherbound.game.core

import kotlin.random.Random

/**
 * Animation/visual budget tier for an attack, derived from its power value.
 *
 * Pokemon-style design principle: weak attacks read as quick bonks, strong
 * attacks read as cinematic spectacles. The tier drives duration, particle
 * count, camera shake, bloom radius, and whether cinematic mode (letterbox
 * + slow-mo + screen flash) activates.
 *
 * Variante B distribution across 450 future attacks:
 *   - Snap        40%  (180)   — weak: Tackle-class
 *   - Strike      30%  (135)   — common offense
 *   - Pulse       20%  ( 90)   — mid-tier finishers
 *   - Storm        7%  ( 32)   — late-game power moves
 *   - Legendary    3%  ( 13)   — signature/legendary creature moves
 */
enum class PowerTier(
    val powerRange: IntRange,
    val durationMs: Int,
    val particleBudget: Int,
    val shakeIntensity: Float,
    val bloomRadiusPx: Float,
    val cinematic: CinematicLevel,
) {
    Snap(10..49, durationMs = 500, particleBudget = 24, shakeIntensity = 0.2f, bloomRadiusPx = 4f, cinematic = CinematicLevel.None),
    Strike(50..89, durationMs = 800, particleBudget = 48, shakeIntensity = 0.5f, bloomRadiusPx = 8f, cinematic = CinematicLevel.None),
    Pulse(90..139, durationMs = 1200, particleBudget = 96, shakeIntensity = 0.9f, bloomRadiusPx = 16f, cinematic = CinematicLevel.None),
    Storm(140..199, durationMs = 1800, particleBudget = 144, shakeIntensity = 1.4f, bloomRadiusPx = 24f, cinematic = CinematicLevel.Big),
    Legendary(200..350, durationMs = 3000, particleBudget = 240, shakeIntensity = 2.5f, bloomRadiusPx = 40f, cinematic = CinematicLevel.Full);

    companion object {
        fun forPower(power: Int): PowerTier =
            values().firstOrNull { power in it.powerRange } ?: Snap

        /**
         * Variante B distribution function — used by the future 450-attack
         * generator to assign powers consistent with the visual tier balance.
         */
        fun samplePower(rng: Random): Int {
            val r = rng.nextDouble()
            return when {
                r < 0.40 -> rng.nextInt(35, 50)            // Snap
                r < 0.70 -> rng.nextInt(50, 90)            // Strike
                r < 0.90 -> rng.nextInt(90, 140)           // Pulse
                r < 0.97 -> rng.nextInt(140, 200)          // Storm
                else -> listOf(220, 250, 280, 300, 320, 350).random(rng)  // Legendary, 350 reserved for the 3 unique legendary signature moves
            }
        }
    }
}

/**
 * Cinematic intensity controls how aggressively the attack takes over the
 * screen. None = no overlay; Big = camera zoom + extra bloom; Full = letterbox
 * bars + slow-motion at hit + full-screen flash + post-hit pause.
 */
enum class CinematicLevel(
    val cameraZoom: Float,
    val letterboxFraction: Float,
    val slowMoTimeScale: Float,
    val slowMoDurationMs: Int,
    val fullscreenFlashAlpha: Float,
    val postHitPauseMs: Int,
) {
    None(cameraZoom = 1.0f, letterboxFraction = 0f, slowMoTimeScale = 1.0f, slowMoDurationMs = 0, fullscreenFlashAlpha = 0f, postHitPauseMs = 0),
    Big(cameraZoom = 1.2f, letterboxFraction = 0.05f, slowMoTimeScale = 0.7f, slowMoDurationMs = 240, fullscreenFlashAlpha = 0.35f, postHitPauseMs = 240),
    Full(cameraZoom = 1.4f, letterboxFraction = 0.12f, slowMoTimeScale = 0.3f, slowMoDurationMs = 400, fullscreenFlashAlpha = 0.85f, postHitPauseMs = 600),
}
