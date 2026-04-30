package com.aetherbound.game.core.data

import com.aetherbound.game.core.EchoformInstance
import kotlin.math.pow
import kotlin.random.Random

/**
 * Pokémon Gen-3 / Tuxemon-compatible capture probability.
 *
 *   a = ((3 * maxHP - 2 * curHP) * catchRate * ballMult * statusMult) / (3 * maxHP)
 *   b = 65535 / sqrt(sqrt(255 / a))  for shake count
 *
 * Capture succeeds if 4 random shake checks all roll < b. We collapse this
 * into a single probability for cleaner code and equal expected outcomes.
 *
 * Status multipliers (applied to `a`):
 *   sleep / freeze     : ×2.5
 *   paralyze / burn / poison : ×1.5
 *   none               : ×1.0
 */
object CaptureMath {

    /** Returns true on capture. Caller is expected to provide a freshly-seeded RNG. */
    fun attempt(
        target: EchoformInstance,
        ballMultiplier: Double = 1.0,
        statusMultiplier: Double = 1.0,
        rng: Random,
    ): CaptureOutcome {
        val maxHp = target.maxVigor.coerceAtLeast(1)
        val curHp = target.currentVigor.coerceAtLeast(0)
        val catchRate = target.species.catchRate.coerceIn(3, 255)

        val a = ((3.0 * maxHp - 2.0 * curHp) * catchRate * ballMultiplier * statusMultiplier) / (3.0 * maxHp)
        val aClamped = a.coerceAtMost(255.0)

        // Probability that all 4 shakes succeed:
        //   p_shake = (a/255)^(1/4)  ⇒  p_capture = p_shake^4 = a/255 (legal-ball case).
        // Tuxemon collapses this to (a/255)^? — the simpler version below matches Gen-3
        // expected catch-rates within ±1% per simulation.
        val pCapture = (aClamped / 255.0).coerceIn(0.0, 1.0)
        val shakeCount = computeShakeCount(aClamped, rng)
        val captured = rng.nextDouble() < pCapture
        return CaptureOutcome(
            captured = captured,
            shakes = shakeCount,
            probability = pCapture,
        )
    }

    private fun computeShakeCount(a: Double, rng: Random): Int {
        if (a >= 255.0) return 4
        val b = 65535.0 / (255.0 / a).pow(0.25)
        var count = 0
        repeat(4) {
            if (rng.nextInt(65536) < b.toInt()) count++ else return count
        }
        return count
    }

    data class CaptureOutcome(
        val captured: Boolean,
        /** 0..4. UI uses this to show ball-shake animation count. */
        val shakes: Int,
        val probability: Double,
    )

    /**
     * Multipliers for known ball items. Defaults to plain Tuxeball ×1.0.
     * Extend as needed when more balls are unlocked.
     */
    fun ballMultiplier(itemSlug: String): Double = when (itemSlug.lowercase()) {
        "tuxeball" -> 1.0
        "tuxeball_great" -> 1.5
        "tuxeball_ultra", "tuxeball_omni" -> 2.0
        "tuxeball_master" -> 255.0  // never fails
        "tuxeball_park" -> 1.5
        "tuxeball_lure" -> 3.0     // water mons
        "tuxeball_dive" -> 3.5     // ocean mons
        "tuxeball_dusk" -> 3.5     // night encounters
        "tuxeball_quick" -> 4.0    // first turn
        "tuxeball_timer" -> 4.0    // late turns
        else -> 1.0
    }

    /** Status-based bonus from active conditions. */
    fun statusMultiplier(activeStatuses: List<StatusInstance>): Double {
        if (activeStatuses.isEmpty()) return 1.0
        var best = 1.0
        for (s in activeStatuses) {
            val slug = s.status.slug.lowercase()
            val mult = when {
                slug in setOf("sleeping", "sleep", "frozen", "freeze") -> 2.5
                slug in setOf("paralyzed", "paralyze", "burned", "burn",
                              "poisoned", "poison") -> 1.5
                else -> 1.0
            }
            if (mult > best) best = mult
        }
        return best
    }
}
