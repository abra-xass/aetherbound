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
        // Tiefer mystischer Wald + Traum-/Klang-Resonanz
        ScreenTheme.Forest to listOf(
            Aspect.WOOD, Aspect.EARTH, Aspect.COSMIC,
            Aspect.DREAM, Aspect.SOUND,
        ),
        // Offene Felder, Anfänger-Pool
        ScreenTheme.Plains to listOf(Aspect.WOOD, Aspect.SKY, Aspect.NORMAL),
        // Gipfel mit Erz, Schnee, Kristallen + zeitliche Stille
        ScreenTheme.Mountain to listOf(
            Aspect.FROST, Aspect.EARTH, Aspect.METAL,
            Aspect.CRYSTAL, Aspect.TIME,
        ),
        // Giftiger Sumpf
        ScreenTheme.Marsh to listOf(Aspect.WATER, Aspect.SHADOW, Aspect.VENOM),
        // Sengende Wüste
        ScreenTheme.Desert to listOf(Aspect.FIRE, Aspect.LIGHTNING, Aspect.EARTH),
        // Lava-Höhlen + Kristalle, Blut-Magie, vergessene Zeit
        ScreenTheme.Cave to listOf(
            Aspect.SHADOW, Aspect.METAL, Aspect.FIRE,
            Aspect.CRYSTAL, Aspect.BLOOD, Aspect.TIME,
        ),
        // Königliche Festung + Mind-Schule, Blut-Schwur
        ScreenTheme.Castle to listOf(
            Aspect.METAL, Aspect.HEROIC, Aspect.COSMIC,
            Aspect.MIND, Aspect.BLOOD,
        ),
        // Bürgerstadt mit dunklen Gassen + Klang-Echo
        ScreenTheme.Town to listOf(
            Aspect.NORMAL, Aspect.HEROIC, Aspect.VENOM,
            Aspect.SOUND,
        ),
        // Heiligtum — alle mystischen Aspekte konvergieren hier
        ScreenTheme.Sanctum to listOf(
            Aspect.COSMIC, Aspect.VENOM, Aspect.SHADOW,
            Aspect.DREAM, Aspect.MIND, Aspect.TIME, Aspect.BLOOD,
        ),
        // Surf-only — Tiefsee-exklusiv + Kristall-Korallen
        ScreenTheme.WaterSurface to listOf(
            Aspect.WATER, Aspect.FROST, Aspect.COSMIC, Aspect.CRYSTAL,
        ),
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
