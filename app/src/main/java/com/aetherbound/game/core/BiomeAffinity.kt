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
        // Coastal — kalter Stahl & Wasser
        ScreenTheme.Harbor to listOf(Aspect.WATER, Aspect.FROST, Aspect.METAL),
        // Strand mit Sturm-Resonanz
        ScreenTheme.Beach to listOf(Aspect.WATER, Aspect.SKY, Aspect.LIGHTNING),
        // Tiefer mystischer Wald
        ScreenTheme.Forest to listOf(Aspect.WOOD, Aspect.EARTH, Aspect.COSMIC),
        // Offene Felder, Anfänger-Pool
        ScreenTheme.Plains to listOf(Aspect.WOOD, Aspect.SKY, Aspect.NORMAL),
        // Gipfel mit Erz und Schnee
        ScreenTheme.Mountain to listOf(Aspect.FROST, Aspect.EARTH, Aspect.METAL),
        // Giftiger Sumpf
        ScreenTheme.Marsh to listOf(Aspect.WATER, Aspect.SHADOW, Aspect.VENOM),
        // Sengende Wüste
        ScreenTheme.Desert to listOf(Aspect.FIRE, Aspect.LIGHTNING, Aspect.EARTH),
        // Lava-Höhlen
        ScreenTheme.Cave to listOf(Aspect.SHADOW, Aspect.METAL, Aspect.FIRE),
        // Königliche Festung
        ScreenTheme.Castle to listOf(Aspect.METAL, Aspect.HEROIC, Aspect.COSMIC),
        // Bürgerstadt mit dunklen Gassen
        ScreenTheme.Town to listOf(Aspect.NORMAL, Aspect.HEROIC, Aspect.VENOM),
        // Heiligtum mit Schattenseiten
        ScreenTheme.Sanctum to listOf(Aspect.COSMIC, Aspect.VENOM, Aspect.SHADOW),
        // Surf-only — Tiefsee-exklusiv
        ScreenTheme.WaterSurface to listOf(Aspect.WATER, Aspect.FROST, Aspect.COSMIC),
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
