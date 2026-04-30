package com.thot.fusion.prep.engine

import kotlin.math.floor
import kotlin.math.pow

object StatFormula {
    const val MAX_LEVEL = 100
    const val MAX_POTENTIAL = 31
    const val MAX_TRAINING_PER_STAT = 252
    const val MAX_TRAINING_TOTAL = 510

    fun calculate(stat: StatKey, base: BaseStats, potential: Potential, training: TrainingPoints, level: Int, temperament: Temperament): Int {
        require(level in 1..MAX_LEVEL) { "Level must be 1..100" }
        require(potential.get(stat) in 0..MAX_POTENTIAL) { "Potential must be 0..31" }
        require(training.get(stat) in 0..MAX_TRAINING_PER_STAT) { "Training per stat must be 0..252" }
        require(training.total <= MAX_TRAINING_TOTAL) { "Training total must be <= 510" }

        val baseValue = base.get(stat)
        val potentialValue = potential.get(stat)
        val trainingValue = training.get(stat)
        val inner = floor(((2 * baseValue + potentialValue + floor(trainingValue / 4.0)) * level) / 100.0).toInt()

        return if (stat == StatKey.VIGOR) {
            inner + level + 10
        } else {
            floor((inner + 5) * temperament.modifier(stat)).toInt()
        }
    }
}

enum class GrowthCurve(val level100Xp: Int) {
    FAST(800_000),
    MEDIUM(1_250_000),
    SLOW(1_900_000),
    LEGENDARY(2_700_000);

    fun requiredTotalXp(level: Int): Int {
        require(level in 1..100) { "Level must be 1..100" }
        val progress = level / 100.0
        return floor(level100Xp * progress.pow(3.0)).toInt()
    }
}

object ExperienceFormula {
    fun flatXp(opponentBaseYield: Int, opponentLevel: Int, battleTypeMultiplier: Double, participationMultiplier: Double): Int {
        return floor((opponentBaseYield * opponentLevel / 5.0) * battleTypeMultiplier * participationMultiplier).toInt().coerceAtLeast(1)
    }

    fun scaledXp(
        opponentBaseYield: Int,
        opponentLevel: Int,
        participantLevel: Int,
        battleTypeMultiplier: Double,
        participationMultiplier: Double,
        antiFarmMultiplier: Double,
    ): Int {
        val base = opponentBaseYield * opponentLevel / 5.0
        val scaledLevelFactor = ((2 * opponentLevel + 10).toDouble().pow(2.5)) /
            ((opponentLevel + participantLevel + 10).toDouble().pow(2.5))
        return floor(base * scaledLevelFactor * battleTypeMultiplier * participationMultiplier * antiFarmMultiplier).toInt().coerceAtLeast(1)
    }
}

object DamageFormula {
    fun calculate(
        attackerLevel: Int,
        techniquePower: Int,
        attackStat: Int,
        defenseStat: Int,
        targets: Double,
        weather: Double,
        critical: Double,
        randomPercent: Int,
        aspectAffinity: Double,
        effectiveness: Double,
        burn: Double,
        otherModifiers: Double,
    ): Int {
        require(attackerLevel in 1..100)
        require(randomPercent in 85..100)
        require(defenseStat > 0)

        val step1 = floor((2 * attackerLevel) / 5.0 + 2).toInt()
        val step2 = floor(step1 * techniquePower * attackStat / defenseStat.toDouble()).toInt()
        val baseDamage = floor(step2 / 50.0).toInt() + 2
        val modifier = targets * weather * critical * (randomPercent / 100.0) * aspectAffinity * effectiveness * burn * otherModifiers
        return floor(baseDamage * modifier).toInt().coerceAtLeast(1)
    }
}

object ElementChart {
    private val strongAgainst = mapOf(
        Aspect.EMBER to setOf(Aspect.VERDANCE, Aspect.FROST, Aspect.METAL),
        Aspect.TIDE to setOf(Aspect.EMBER, Aspect.STONE, Aspect.METAL),
        Aspect.VERDANCE to setOf(Aspect.TIDE, Aspect.STONE, Aspect.MIND),
        Aspect.STONE to setOf(Aspect.SPARK, Aspect.EMBER, Aspect.GALE),
        Aspect.GALE to setOf(Aspect.VERDANCE, Aspect.ECHO, Aspect.SHADE),
        Aspect.SPARK to setOf(Aspect.TIDE, Aspect.GALE, Aspect.METAL),
        Aspect.FROST to setOf(Aspect.VERDANCE, Aspect.GALE, Aspect.MIND),
        Aspect.METAL to setOf(Aspect.FROST, Aspect.RADIANT, Aspect.SHADE),
        Aspect.SHADE to setOf(Aspect.MIND, Aspect.RADIANT, Aspect.ECHO),
        Aspect.RADIANT to setOf(Aspect.SHADE, Aspect.TIDE, Aspect.FROST),
        Aspect.MIND to setOf(Aspect.STONE, Aspect.RADIANT, Aspect.EMBER),
        Aspect.ECHO to setOf(Aspect.MIND, Aspect.SPARK, Aspect.FROST),
    )

    private val weakAgainst = mapOf(
        Aspect.EMBER to setOf(Aspect.TIDE, Aspect.STONE, Aspect.RADIANT),
        Aspect.TIDE to setOf(Aspect.VERDANCE, Aspect.SPARK, Aspect.FROST),
        Aspect.VERDANCE to setOf(Aspect.EMBER, Aspect.FROST, Aspect.METAL),
        Aspect.STONE to setOf(Aspect.TIDE, Aspect.VERDANCE, Aspect.METAL),
        Aspect.GALE to setOf(Aspect.STONE, Aspect.SPARK, Aspect.METAL),
        Aspect.SPARK to setOf(Aspect.STONE, Aspect.VERDANCE, Aspect.ECHO),
        Aspect.FROST to setOf(Aspect.EMBER, Aspect.METAL, Aspect.RADIANT),
        Aspect.METAL to setOf(Aspect.EMBER, Aspect.TIDE, Aspect.SPARK),
        Aspect.SHADE to setOf(Aspect.EMBER, Aspect.METAL, Aspect.RADIANT),
        Aspect.RADIANT to setOf(Aspect.METAL, Aspect.MIND, Aspect.EMBER),
        Aspect.MIND to setOf(Aspect.SHADE, Aspect.ECHO, Aspect.FROST),
        Aspect.ECHO to setOf(Aspect.GALE, Aspect.SHADE, Aspect.METAL),
    )

    fun multiplier(attacking: Aspect, defending: List<Aspect>): Double {
        return defending.fold(1.0) { current, defense ->
            current * when {
                strongAgainst[attacking].orEmpty().contains(defense) -> 2.0
                weakAgainst[attacking].orEmpty().contains(defense) -> 0.5
                else -> 1.0
            }
        }
    }
}
