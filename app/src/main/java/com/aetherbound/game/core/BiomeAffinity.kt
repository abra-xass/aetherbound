package com.aetherbound.game.core

/**
 * Biome → preferred Aspect mapping. Determines which Echoform species can
 * spawn in which screen theme. Each Echoform species carries a biome list
 * derived from its primary + secondary aspects.
 *
 * Distribution goals:
 *   - every aspect appears in 2-3 biomes (no aspect is unreachable)
 *   - every biome supports 3 aspects (so each biome's encounter pool has variety)
 */
object BiomeAffinity {

    private val MAP: Map<ScreenTheme, List<Aspect>> = mapOf(
        ScreenTheme.Harbor to listOf(Aspect.WATER, Aspect.FROST, Aspect.EARTH),
        ScreenTheme.Beach to listOf(Aspect.WATER, Aspect.SKY, Aspect.LIGHTNING),
        ScreenTheme.Forest to listOf(Aspect.WOOD, Aspect.EARTH, Aspect.COSMIC),
        ScreenTheme.Plains to listOf(Aspect.WOOD, Aspect.SKY, Aspect.LIGHTNING),
        ScreenTheme.Mountain to listOf(Aspect.EARTH, Aspect.FROST, Aspect.METAL),
        ScreenTheme.Marsh to listOf(Aspect.WOOD, Aspect.WATER, Aspect.SHADOW),
        ScreenTheme.Desert to listOf(Aspect.FIRE, Aspect.LIGHTNING, Aspect.EARTH),
        ScreenTheme.Cave to listOf(Aspect.EARTH, Aspect.SHADOW, Aspect.METAL),
        ScreenTheme.Castle to listOf(Aspect.COSMIC, Aspect.VENOM, Aspect.HEROIC),
        ScreenTheme.Town to listOf(Aspect.COSMIC, Aspect.VENOM, Aspect.HEROIC),
        ScreenTheme.Sanctum to listOf(Aspect.VENOM, Aspect.HEROIC, Aspect.SHADOW),
    )

    /** Aspects that this biome favours. Empty list = no preference. */
    fun aspectsFor(theme: ScreenTheme): List<Aspect> = MAP[theme] ?: emptyList()

    /** Biomes a species can spawn in, based on its primary + secondary aspect. */
    fun biomesForAspects(primary: Aspect, secondary: Aspect?): List<ScreenTheme> {
        val match = mutableSetOf<ScreenTheme>()
        for ((theme, aspects) in MAP) {
            if (primary in aspects || (secondary != null && secondary in aspects)) {
                match += theme
            }
        }
        return match.toList()
    }
}
