package com.aetherbound.game.core.data

import com.aetherbound.game.core.EchoformInstance
import com.aetherbound.game.core.Technique
import kotlin.random.Random

/**
 * Critical-hit + random-damage-range layer applied on top of Aetherbound's
 * existing DamageFormula. The formula stays canonical; this file just
 * provides the multiplicative noise modern monster-RPGs expect.
 *
 *   crit   : 1.5× damage  (default 6.25% chance, raised by tech.critRate)
 *   range  : 85..100% multiplier on every hit
 *   STAB   : 1.5× when attacker shares the move's aspect
 */
object DamageVariance {

    /**
     * Roll crit + random-range + STAB and return the multiplier. Multiply
     * this against your base damage (after type effectiveness) to produce
     * the final outgoing hit.
     *
     * @return [Result] holding the multiplier and metadata (was-crit etc.)
     */
    fun roll(
        attacker: EchoformInstance,
        tech: Technique,
        rng: Random,
    ): Result {
        val critChance = critChance(tech)
        val isCrit = rng.nextDouble() < critChance
        val critMult = if (isCrit) 1.5 else 1.0

        // Tuxemon uses 0.85..1.0 random range, like Pokémon Gen-3+.
        val rangeMult = 0.85 + rng.nextDouble() * 0.15

        // Same-Type-Attack-Bonus: 50% boost when move type matches attacker
        // primary or secondary.
        val stab = if (tech.aspect == attacker.species.primaryAspect ||
                       tech.aspect == attacker.species.secondaryAspect) 1.5 else 1.0

        return Result(
            multiplier = critMult * rangeMult * stab,
            isCrit = isCrit,
            stab = stab > 1.0,
            rangeMult = rangeMult,
        )
    }

    /**
     * Crit chance from tech.critRate following Pokémon's tier table:
     *   0 → 1/16 = 6.25%
     *   1 → 1/8  = 12.5%
     *   2 → 1/4  = 25%
     *   3 → 1/3  = 33.3%
     *   4+ → 50%
     */
    fun critChance(tech: Technique): Double = when (tech.critRate.coerceAtLeast(0)) {
        0 -> 0.0625
        1 -> 0.125
        2 -> 0.25
        3 -> 0.333
        else -> 0.5
    }

    data class Result(
        val multiplier: Double,
        val isCrit: Boolean,
        val stab: Boolean,
        val rangeMult: Double,
    )
}
