package com.aetherbound.game.core

data class EchoformSpecies(
    val id: String,
    val name: String,
    val primaryAspect: Aspect,
    val secondaryAspect: Aspect? = null,
    val baseStats: BaseStats,
    /** Catch-rate analog (3..255). Lower = harder. Common ~190, legendary ~3. */
    val catchRate: Int = 190,
    /** Rarity tier — drives encounter frequency, stat budget, catch rate. */
    val rarity: Rarity = Rarity.Common,
    /** Biomes where this species can spawn naturally. Empty = aspect-driven default. */
    val biomes: List<ScreenTheme> = emptyList(),
    /** True for the 3 unique roaming legendary species (E298/E299/E300). */
    val isLegendary: Boolean = false,
)

data class EchoformInstance(
    val species: EchoformSpecies,
    val level: Int,
    val techniques: List<Technique>,
    val potential: Potential = Potential(),
    val training: TrainingPoints = TrainingPoints(),
    val temperament: Temperament = Temperament(),
    val currentVigor: Int = StatFormula.calc(
        StatKey.VIGOR, species.baseStats, level, Potential(), TrainingPoints(), Temperament()
    ),
) {
    val maxVigor: Int get() = StatFormula.calc(StatKey.VIGOR, species.baseStats, level, potential, training, temperament)
    fun stat(key: StatKey): Int = StatFormula.calc(key, species.baseStats, level, potential, training, temperament)
    val isFainted: Boolean get() = currentVigor <= 0
}
