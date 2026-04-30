package com.aetherbound.game.core

import kotlin.math.floor
import kotlin.random.Random

data class DamageResult(
    val damage: Int,
    val effectiveness: Double,
    val crit: Boolean,
    val missed: Boolean,
    val stab: Boolean,
)

object DamageFormula {

    fun resolve(
        attacker: EchoformInstance,
        defender: EchoformInstance,
        technique: Technique,
        rng: Random,
    ): DamageResult {
        val accRoll = rng.nextInt(0, 100)
        if (accRoll >= technique.accuracy) {
            return DamageResult(0, 1.0, crit = false, missed = true, stab = false)
        }

        val isPhysical = technique.category == TechniqueCategory.STRIKE
        val attack = if (isPhysical) attacker.stat(StatKey.FORCE) else attacker.stat(StatKey.FOCUS)
        val defense = if (isPhysical) defender.stat(StatKey.GUARD) else defender.stat(StatKey.WARD)

        val critRoll = rng.nextInt(0, 24)
        val crit = critRoll < technique.critRate
        val critMul = if (crit) 1.5 else 1.0

        val effectiveness = AspectAffinity.multiplier(
            technique.aspect,
            defender.species.primaryAspect,
            defender.species.secondaryAspect,
        )
        val stab = technique.aspect == attacker.species.primaryAspect ||
            technique.aspect == attacker.species.secondaryAspect
        val stabMul = if (stab) 1.5 else 1.0

        val randomMul = (85 + rng.nextInt(0, 16)) / 100.0

        val baseDamage = floor(
            floor(
                floor((2.0 * attacker.level) / 5.0 + 2.0)
                    * technique.power * attack / defense
            ) / 50.0
        ) + 2.0

        val final = floor(baseDamage * critMul * effectiveness * stabMul * randomMul).toInt()
            .coerceAtLeast(1)

        return DamageResult(final, effectiveness, crit, missed = false, stab = stab)
    }
}
