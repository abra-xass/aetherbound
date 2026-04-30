package com.aetherbound.game.core

enum class StatKey { VIGOR, FORCE, FOCUS, GUARD, WARD, TEMPO }

data class BaseStats(
    val vigor: Int, val force: Int, val focus: Int,
    val guard: Int, val ward: Int, val tempo: Int,
) {
    fun get(key: StatKey): Int = when (key) {
        StatKey.VIGOR -> vigor; StatKey.FORCE -> force; StatKey.FOCUS -> focus
        StatKey.GUARD -> guard; StatKey.WARD -> ward; StatKey.TEMPO -> tempo
    }
}

/** Individual potential, 0..31 per stat — the IV equivalent. */
data class Potential(
    val vigor: Int = 31, val force: Int = 31, val focus: Int = 31,
    val guard: Int = 31, val ward: Int = 31, val tempo: Int = 31,
) {
    fun get(k: StatKey): Int = when (k) {
        StatKey.VIGOR -> vigor; StatKey.FORCE -> force; StatKey.FOCUS -> focus
        StatKey.GUARD -> guard; StatKey.WARD -> ward; StatKey.TEMPO -> tempo
    }
}

/** Training points, max 252 per stat, 510 total — the EV equivalent. */
data class TrainingPoints(
    val vigor: Int = 0, val force: Int = 0, val focus: Int = 0,
    val guard: Int = 0, val ward: Int = 0, val tempo: Int = 0,
) {
    val total: Int get() = vigor + force + focus + guard + ward + tempo
    fun get(k: StatKey): Int = when (k) {
        StatKey.VIGOR -> vigor; StatKey.FORCE -> force; StatKey.FOCUS -> focus
        StatKey.GUARD -> guard; StatKey.WARD -> ward; StatKey.TEMPO -> tempo
    }
}

/** Nature/temperament, 1.1 boost on one stat, 0.9 reduce on another. */
data class Temperament(
    val name: String = "Balanced",
    val boosted: StatKey? = null,
    val reduced: StatKey? = null,
) {
    fun modifier(s: StatKey): Double = when (s) {
        boosted -> 1.1; reduced -> 0.9; else -> 1.0
    }
}

/** Full Gen-3 style stat formula: Bulbapedia-documented, freely usable math. */
object StatFormula {
    fun calc(
        stat: StatKey,
        base: BaseStats,
        level: Int,
        potential: Potential = Potential(),
        training: TrainingPoints = TrainingPoints(),
        temperament: Temperament = Temperament(),
    ): Int {
        val b = base.get(stat)
        val iv = potential.get(stat).coerceIn(0, 31)
        val evQuarter = training.get(stat).coerceIn(0, 252) / 4
        val inner = ((2 * b + iv + evQuarter) * level) / 100
        return if (stat == StatKey.VIGOR) {
            inner + level + 10
        } else {
            kotlin.math.floor((inner + 5) * temperament.modifier(stat)).toInt()
        }
    }
}
