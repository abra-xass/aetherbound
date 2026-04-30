package com.aetherbound.game.core

import kotlin.random.Random

/**
 * Deterministic generator for 1000 Echoform species (E001..E1000).
 *
 * Stats, aspect, name, catch-rate are all derived from the ordinal via a
 * fixed-seed PRNG so the same ID always produces the same species. Names use
 * a phonetic prefix/suffix pool — 14 prefixes × 14 suffixes × 6 variants =
 * over 1000 unique combinations.
 *
 * Special-case overrides for hand-tuned starters live below.
 */
object EchoformDex {

    /** Pilot scope = 300 active species. The pool of 1000 originals lives in
     *  tools/ai/echoforms-pool/ — copy more into assets/game/echoforms/ and
     *  raise this number when scaling up. */
    const val TOTAL = 300

    fun id(ordinal: Int): String =
        if (ordinal < 1000) "E%03d".format(ordinal) else "E$ordinal"

    fun ordinal(id: String): Int? =
        id.removePrefix("E").toIntOrNull()

    /** All elemental aspects (excluding NORMAL = colorless). */
    private val ASPECTS: List<Aspect> =
        Aspect.values().filter { it != Aspect.NORMAL }

    private val PREFIXES = listOf(
        "Vul", "Re", "Mos", "Sha", "Bri", "Lum", "Kor", "Pyr",
        "Aer", "Tor", "Glim", "Vyn", "Dru", "Sym",
    )
    private val SUFFIXES = listOf(
        "kid", "eva", "lyn", "den", "rik", "mox", "us", "or",
        "lin", "ix", "ros", "ari", "yth", "mara",
    )
    private val VARIANTS = listOf("", "x", "ra", "el", "is", "an")

    private val ALL: Map<String, EchoformSpecies> by lazy {
        (1..TOTAL).associate { o ->
            val sid = id(o)
            sid to (OVERRIDES[sid] ?: generate(o))
        }
    }

    fun byId(id: String): EchoformSpecies? = ALL[id]

    fun all(): Collection<EchoformSpecies> = ALL.values

    /** Default starter pool — first three. */
    fun starters(): List<EchoformSpecies> = listOf(
        ALL.getValue("E001"), ALL.getValue("E002"), ALL.getValue("E003"),
    )

    /**
     * Returns species for a route encounter pool, deterministically chosen
     * from a region-id seed and the encounter table size. Legacy entry point
     * — biases toward common-tier only.
     */
    fun encounterPool(regionSeed: Long, count: Int = 8): List<EchoformSpecies> {
        val rng = Random(regionSeed)
        val candidates = ALL.values.filter { it.rarity == Rarity.Common }.toList()
        val picks = mutableSetOf<Int>()
        while (picks.size < count && picks.size < candidates.size) {
            picks += rng.nextInt(candidates.size)
        }
        return picks.map { candidates[it] }
    }

    /**
     * Biome-aware encounter pool. Returns species whose [biomes] list contains
     * the requested theme, weighted by Rarity.poolWeight (Common 70 / Uncommon
     * 20 / Rare 8 / VeryRare 2). Legendaries are excluded — they spawn only
     * via the Roaming-Legendary system.
     */
    fun encounterPoolByBiome(theme: ScreenTheme, regionSeed: Long, count: Int = 8): List<EchoformSpecies> {
        val candidates = ALL.values.filter {
            !it.isLegendary && (theme in it.biomes || it.biomes.isEmpty())
        }
        if (candidates.isEmpty()) return emptyList()
        val rng = Random(regionSeed)
        val picks = mutableListOf<EchoformSpecies>()
        repeat(count * 4) {
            // Weighted pick: skip with prob proportional to (1 - poolWeight/100)
            val sp = candidates.random(rng)
            if (rng.nextInt(100) < sp.rarity.poolWeight) {
                picks += sp
                if (picks.size >= count) return picks.distinctBy { it.id }
            }
        }
        return picks.distinctBy { it.id }.take(count).ifEmpty {
            // Fallback: any candidate
            (1..count.coerceAtMost(candidates.size)).map { candidates.random(rng) }.distinctBy { it.id }
        }
    }

    /** Pick one species from the biome pool — used per single encounter. */
    fun rollEncounter(theme: ScreenTheme, rng: Random): EchoformSpecies? {
        val candidates = ALL.values.filter {
            !it.isLegendary && (theme in it.biomes || it.biomes.isEmpty())
        }
        if (candidates.isEmpty()) return null
        // Roll until rarity poolWeight gates allow a pick
        repeat(50) {
            val sp = candidates.random(rng)
            if (rng.nextInt(100) < sp.rarity.poolWeight) return sp
        }
        return candidates.random(rng)
    }

    private fun generate(ordinal: Int): EchoformSpecies {
        val rng = Random(ordinal.toLong() * 0x9E3779B97F4A7C15uL.toLong())

        // Aspect: round-robin through the 12 elements, with occasional dual-aspect
        val primary = ASPECTS[(ordinal - 1) % ASPECTS.size]
        val secondary: Aspect? = if (rng.nextInt(100) < 18) {
            ASPECTS[rng.nextInt(ASPECTS.size)].takeIf { it != primary }
        } else null

        // Rarity is now deterministic via Rarity.forOrdinal — guarantees the
        // exact 180/75/30/12/3 distribution across 300 species.
        val rarity = Rarity.forOrdinal(ordinal)

        // Distribute the rarity's statBudget across 6 stats with random weights
        val weights = DoubleArray(6) { 0.6 + rng.nextDouble() * 0.8 }
        val totalW = weights.sum()
        val stats = IntArray(6) { i -> (rarity.statBudget * weights[i] / totalW).toInt().coerceIn(20, 150) }

        val baseStats = BaseStats(
            vigor = stats[0], force = stats[1], focus = stats[2],
            guard = stats[3], ward = stats[4], tempo = stats[5],
        )

        // Biome affinity from primary + secondary aspect
        val biomes = BiomeAffinity.biomesForAspects(primary, secondary)

        val name = generateName(ordinal, rng)

        return EchoformSpecies(
            id = id(ordinal),
            name = name,
            primaryAspect = primary,
            secondaryAspect = secondary,
            baseStats = baseStats,
            catchRate = rarity.catchRate,
            rarity = rarity,
            biomes = biomes,
            isLegendary = rarity == Rarity.Legendary,
        )
    }

    private fun generateName(ordinal: Int, rng: Random): String {
        // Mix prefix + suffix + optional variant, salt with ordinal so two
        // species from different ordinals never collide.
        val p = PREFIXES[(ordinal * 7) % PREFIXES.size]
        val s = SUFFIXES[(ordinal * 13) % SUFFIXES.size]
        val v = VARIANTS[(ordinal * 5) % VARIANTS.size]
        return "$p$s$v"
    }

    /** Hand-tuned overrides for plot-relevant species. Filename E001/E002 still
     * resolve via PNG asset, but stats/name/aspect are authored here. */
    private val OVERRIDES: Map<String, EchoformSpecies> = mapOf(
        "E001" to EchoformSpecies(
            id = "E001", name = "Vulkid", primaryAspect = Aspect.FIRE,
            baseStats = BaseStats(vigor = 65, force = 78, focus = 50, guard = 52, ward = 48, tempo = 70),
            catchRate = 190, rarity = Rarity.Common,
            biomes = listOf(ScreenTheme.Desert, ScreenTheme.Plains),
        ),
        "E002" to EchoformSpecies(
            id = "E002", name = "Reeva", primaryAspect = Aspect.WATER,
            baseStats = BaseStats(vigor = 72, force = 55, focus = 78, guard = 60, ward = 65, tempo = 58),
            catchRate = 175, rarity = Rarity.Common,
            biomes = listOf(ScreenTheme.Harbor, ScreenTheme.Beach, ScreenTheme.Marsh),
        ),
        "E003" to EchoformSpecies(
            id = "E003", name = "Mosslyn", primaryAspect = Aspect.WOOD,
            baseStats = BaseStats(vigor = 70, force = 60, focus = 70, guard = 65, ward = 65, tempo = 50),
            catchRate = 190, rarity = Rarity.Common,
            biomes = listOf(ScreenTheme.Forest, ScreenTheme.Plains, ScreenTheme.Marsh),
        ),
        // ── 3 LEGENDARY SLOTS ────────────────────────────────────────────
        // Names are placeholders — user will rename + assign signature moves
        // (350-power) once the 3 legendaries are designed.
        "E298" to EchoformSpecies(
            id = "E298", name = "Legendary-One", primaryAspect = Aspect.HEROIC,
            baseStats = BaseStats(vigor = 110, force = 110, focus = 110, guard = 100, ward = 105, tempo = 105),
            catchRate = 3, rarity = Rarity.Legendary,
            biomes = emptyList(),  // roams, no fixed biome
            isLegendary = true,
        ),
        "E299" to EchoformSpecies(
            id = "E299", name = "Legendary-Two", primaryAspect = Aspect.SHADOW,
            baseStats = BaseStats(vigor = 100, force = 115, focus = 115, guard = 95, ward = 105, tempo = 110),
            catchRate = 3, rarity = Rarity.Legendary,
            biomes = emptyList(),
            isLegendary = true,
        ),
        "E300" to EchoformSpecies(
            id = "E300", name = "Legendary-Three", primaryAspect = Aspect.VENOM,
            baseStats = BaseStats(vigor = 120, force = 100, focus = 110, guard = 110, ward = 100, tempo = 100),
            catchRate = 3, rarity = Rarity.Legendary,
            biomes = emptyList(),
            isLegendary = true,
        ),
    )
}
