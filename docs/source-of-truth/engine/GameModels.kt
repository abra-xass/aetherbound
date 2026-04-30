package com.thot.fusion.prep.engine

enum class Aspect {
    EMBER, TIDE, VERDANCE, STONE, GALE, SPARK, FROST, METAL, SHADE, RADIANT, MIND, ECHO
}

enum class StatKey {
    VIGOR, FORCE, FOCUS, GUARD, WARD, TEMPO
}

enum class TechniqueCategory {
    STRIKE, PULSE, GUARD, FIELD, BIND, RECOVERY
}

data class BaseStats(
    val vigor: Int,
    val force: Int,
    val focus: Int,
    val guard: Int,
    val ward: Int,
    val tempo: Int,
) {
    fun get(stat: StatKey): Int = when (stat) {
        StatKey.VIGOR -> vigor
        StatKey.FORCE -> force
        StatKey.FOCUS -> focus
        StatKey.GUARD -> guard
        StatKey.WARD -> ward
        StatKey.TEMPO -> tempo
    }
}

data class Potential(
    val vigor: Int = 0,
    val force: Int = 0,
    val focus: Int = 0,
    val guard: Int = 0,
    val ward: Int = 0,
    val tempo: Int = 0,
) {
    fun get(stat: StatKey): Int = when (stat) {
        StatKey.VIGOR -> vigor
        StatKey.FORCE -> force
        StatKey.FOCUS -> focus
        StatKey.GUARD -> guard
        StatKey.WARD -> ward
        StatKey.TEMPO -> tempo
    }
}

data class TrainingPoints(
    val vigor: Int = 0,
    val force: Int = 0,
    val focus: Int = 0,
    val guard: Int = 0,
    val ward: Int = 0,
    val tempo: Int = 0,
) {
    val total: Int get() = vigor + force + focus + guard + ward + tempo

    fun get(stat: StatKey): Int = when (stat) {
        StatKey.VIGOR -> vigor
        StatKey.FORCE -> force
        StatKey.FOCUS -> focus
        StatKey.GUARD -> guard
        StatKey.WARD -> ward
        StatKey.TEMPO -> tempo
    }
}

data class Temperament(
    val name: String,
    val boosted: StatKey?,
    val reduced: StatKey?,
) {
    fun modifier(stat: StatKey): Double = when (stat) {
        boosted -> 1.1
        reduced -> 0.9
        else -> 1.0
    }
}

data class EchoformSpecies(
    val id: String,
    val name: String,
    val aspects: List<Aspect>,
    val rarity: String,
    val baseStats: BaseStats,
    val baseXpYield: Int,
    val uniquePerSave: Boolean,
)

fun EchoformSpecies.canLearn(technique: Technique): Boolean {
    return aspects.contains(technique.aspect)
}

data class EchoformInstance(
    val instanceId: String,
    val species: EchoformSpecies,
    val level: Int,
    val experience: Int,
    val potential: Potential,
    val trainingPoints: TrainingPoints,
    val temperament: Temperament,
    val currentVigor: Int,
)

data class Technique(
    val id: String,
    val name: String,
    val aspect: Aspect,
    val category: TechniqueCategory,
    val power: Int,
    val accuracy: Int,
    val fluxDelta: Int,
    val priority: Int,
)
