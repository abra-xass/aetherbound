package com.aetherbound.game.core.data

import com.aetherbound.game.core.Aspect
import com.aetherbound.game.core.BaseStats
import com.aetherbound.game.core.EchoformSpecies
import com.aetherbound.game.core.Rarity
import com.aetherbound.game.core.ScreenTheme
import com.aetherbound.game.core.Technique
import com.aetherbound.game.core.TechniqueCategory

/**
 * Bridge between imported Tuxemon JSON content and Aetherbound's engine
 * data classes. Converts:
 *
 *   TuxemonMonster   → EchoformSpecies   (via shape-derived BaseStats)
 *   TuxemonTechnique → Technique         (power scale + range/sort → category)
 *   List<String>     → List<ScreenTheme> (terrain slugs → biomes)
 *
 * All conversions are pure; the adapter doesn't mutate either side.
 */
object TuxemonAdapter {

    // ───────────────────────────────────────────────────────────────
    // Stats: Shape attributes (4..8) scaled to rarity's stat budget.
    // ───────────────────────────────────────────────────────────────

    /**
     * Builds [BaseStats] from a Tuxemon shape and an Aetherbound rarity tier.
     *
     * The shape's six attributes (sum ~36) are scaled proportionally to fill
     * the rarity's stat budget. A "dragon" (6/6/6/6/6/6=36) at Common
     * (budget=420) produces a balanced 70/70/70/70/70/70. A "blob"
     * (8/4/8/4/8/4=36) becomes 93/47/93/47/93/47.
     */
    fun baseStatsFromShape(shape: String, rarity: Rarity): BaseStats {
        val attrs = MonsterShapes.forSlug(shape)
        val sum = attrs.sum().coerceAtLeast(1)
        val budget = rarity.statBudget.toDouble()
        fun scale(v: Int): Int = (v.toDouble() / sum * budget).toInt().coerceIn(20, 200)
        // Mapping is symmetric with AetherStats.fromShape:
        //   vigor←hp  force←melee  focus←ranged  guard←armour  ward←dodge  tempo←speed
        return BaseStats(
            vigor = scale(attrs.hp),
            force = scale(attrs.melee),
            focus = scale(attrs.ranged),
            guard = scale(attrs.armour),
            ward = scale(attrs.dodge),
            tempo = scale(attrs.speed),
        )
    }

    // ───────────────────────────────────────────────────────────────
    // Rarity inference from Tuxemon's catch_rate (1..255 typical).
    // Lower catch_rate = harder to catch = rarer.
    // ───────────────────────────────────────────────────────────────

    fun rarityFromCatchRate(catchRate: Double, isLegendaryHint: Boolean = false): Rarity = when {
        isLegendaryHint || catchRate <= 5.0 -> Rarity.Legendary
        catchRate <= 25.0 -> Rarity.VeryRare
        catchRate <= 60.0 -> Rarity.Rare
        catchRate <= 120.0 -> Rarity.Uncommon
        else -> Rarity.Common
    }

    // ───────────────────────────────────────────────────────────────
    // Terrain slugs → ScreenTheme (Aetherbound biome enum).
    // ───────────────────────────────────────────────────────────────

    private val TERRAIN_MAP: Map<String, List<ScreenTheme>> = mapOf(
        "grassland"  to listOf(ScreenTheme.Plains),
        "plains"     to listOf(ScreenTheme.Plains),
        "meadow"     to listOf(ScreenTheme.Plains),
        "field"      to listOf(ScreenTheme.Plains),
        "savanna"    to listOf(ScreenTheme.Plains),
        "forest"     to listOf(ScreenTheme.Forest),
        "woodland"   to listOf(ScreenTheme.Forest),
        "jungle"     to listOf(ScreenTheme.Forest),
        "rainforest" to listOf(ScreenTheme.Forest),
        "marsh"      to listOf(ScreenTheme.Marsh),
        "swamp"      to listOf(ScreenTheme.Marsh),
        "wetland"    to listOf(ScreenTheme.Marsh),
        "bog"        to listOf(ScreenTheme.Marsh),
        "mountain"   to listOf(ScreenTheme.Mountain),
        "alpine"     to listOf(ScreenTheme.Mountain),
        "highland"   to listOf(ScreenTheme.Mountain),
        "tundra"     to listOf(ScreenTheme.Mountain),
        "cave"       to listOf(ScreenTheme.Cave),
        "cavern"     to listOf(ScreenTheme.Cave),
        "underground" to listOf(ScreenTheme.Cave),
        "desert"     to listOf(ScreenTheme.Desert),
        "arid"       to listOf(ScreenTheme.Desert),
        "wasteland"  to listOf(ScreenTheme.Desert),
        "ocean"      to listOf(ScreenTheme.Beach, ScreenTheme.Harbor),
        "sea"        to listOf(ScreenTheme.Beach, ScreenTheme.Harbor),
        "coast"      to listOf(ScreenTheme.Beach),
        "beach"      to listOf(ScreenTheme.Beach),
        "shore"      to listOf(ScreenTheme.Beach),
        "tropical"   to listOf(ScreenTheme.Beach),
        "river"      to listOf(ScreenTheme.Marsh, ScreenTheme.Beach),
        "lake"       to listOf(ScreenTheme.Beach),
        "harbor"     to listOf(ScreenTheme.Harbor),
        "port"       to listOf(ScreenTheme.Harbor),
        "city"       to listOf(ScreenTheme.Town),
        "town"       to listOf(ScreenTheme.Town),
        "urban"      to listOf(ScreenTheme.Town),
        "castle"     to listOf(ScreenTheme.Castle),
        "fortress"   to listOf(ScreenTheme.Castle),
        "ruins"      to listOf(ScreenTheme.Castle, ScreenTheme.Sanctum),
        "temple"     to listOf(ScreenTheme.Sanctum),
        "shrine"     to listOf(ScreenTheme.Sanctum),
        "sanctum"    to listOf(ScreenTheme.Sanctum),
    )

    fun biomesFromTerrains(terrains: List<String>): List<ScreenTheme> {
        val out = LinkedHashSet<ScreenTheme>()
        for (t in terrains) {
            TERRAIN_MAP[t.lowercase()]?.forEach { out += it }
        }
        return out.toList()
    }

    // ───────────────────────────────────────────────────────────────
    // EchoformSpecies from a TuxemonMonster snapshot.
    // ───────────────────────────────────────────────────────────────

    /**
     * @param ordinal optional override for [EchoformSpecies.id]. If null,
     *   uses the Tuxemon slug directly as the id (e.g. "agnidon" instead of
     *   "E001"). Most Aetherbound subsystems already key on slugs via the
     *   AssetResolver, so the slug-as-id path is the default.
     */
    fun echoformFromTuxemon(mon: TuxemonMonster, ordinal: Int? = null): EchoformSpecies {
        val isLegendary = "legendary" in mon.tags || mon.catchRate <= 5.0
        val rarity = rarityFromCatchRate(mon.catchRate, isLegendaryHint = isLegendary)
        val baseStats = baseStatsFromShape(mon.shape, rarity)
        val biomes = biomesFromTerrains(mon.terrains)
        val id = ordinal?.let { "E%03d".format(it) } ?: mon.slug
        return EchoformSpecies(
            id = id,
            name = mon.slug.replaceFirstChar { it.uppercase() },
            primaryAspect = mon.primaryType,
            secondaryAspect = mon.secondaryType,
            baseStats = baseStats,
            catchRate = mon.catchRate.toInt().coerceIn(3, 255),
            rarity = rarity,
            biomes = biomes,
            isLegendary = isLegendary,
        )
    }

    // ───────────────────────────────────────────────────────────────
    // Technique conversion. Tuxemon power is a 0.0..3.0 multiplier;
    // Aetherbound power is a 30..280 absolute scale.
    //
    //   Tuxemon 0.5  →  25  (very weak)
    //   Tuxemon 1.0  →  50  (Strike base)
    //   Tuxemon 1.4  →  70  (typical mid-tier)
    //   Tuxemon 2.0  → 100  (heavy hitter)
    //   Tuxemon 3.0  → 150  (Storm tier)
    // ───────────────────────────────────────────────────────────────

    fun techniqueFromTuxemon(t: TuxemonTechnique): Technique {
        val power = (t.power * 50.0).toInt().coerceAtLeast(1)
        val accuracy = (t.accuracy * 100.0).toInt().coerceIn(1, 100)
        val priority = when (t.speed) {
            "very_fast" -> 2
            "fast"      -> 1
            "slow"      -> -1
            "very_slow" -> -2
            else        -> 0   // "normal"
        }
        return Technique(
            id = t.slug,
            name = t.slug.replace('_', ' ').replaceFirstChar { it.uppercase() },
            aspect = t.primaryType,
            category = categoryFromTuxemon(t),
            power = power,
            accuracy = accuracy,
            priority = priority,
        )
    }

    /**
     * Map Tuxemon's `sort` + `range` to Aetherbound's TechniqueCategory.
     *
     * Tuxemon ranges: melee, touch, ranged, reach, reliable
     * Tuxemon sorts: damage, status, meta
     */
    fun categoryFromTuxemon(t: TuxemonTechnique): TechniqueCategory {
        if (t.sort == "damage") {
            return when (t.range) {
                "melee", "touch", "reach" -> TechniqueCategory.STRIKE
                "ranged", "reliable"      -> TechniqueCategory.PULSE
                else                      -> TechniqueCategory.STRIKE
            }
        }
        // Non-damage: classify by tag heuristics.
        val tags = t.tags.map { it.lowercase() }
        return when {
            "heal" in tags || "recovery" in tags || t.slug.contains("heal") -> TechniqueCategory.RECOVERY
            "shield" in tags || "guard" in tags || "block" in tags -> TechniqueCategory.GUARD
            "field" in tags || "weather" in tags || "terrain" in tags -> TechniqueCategory.FIELD
            "bind" in tags || "snare" in tags || "trap" in tags -> TechniqueCategory.BIND
            else -> TechniqueCategory.FIELD
        }
    }
}
