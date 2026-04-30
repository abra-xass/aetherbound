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
        val spawn = autoSpawnConditions(
            primaryAspect = mon.primaryType,
            rarity = rarity,
            slugHash = mon.slug.hashCode(),
        )
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
            spawn = spawn,
        )
    }

    /**
     * Auto-curate spawn conditions from primary aspect + rarity.
     *
     * **Lock model (per design lock):**
     *   - Time-of-day (Tag/Nacht/Dämmerung) → ALWAYS hard lock
     *   - Weekday → ALWAYS hard lock
     *   - Weather → NEVER lock; only [SpawnConditions.spawnRateBoost] multiplier
     *
     * **Coverage by rarity:**
     *   - Common (180):  ~60% get a weather-boost only (no time/weekday lock)
     *   - Uncommon (75): ~80% get either a weekday-only OR time-only lock
     *   - Rare (30):     100% combined (weekday + dayPhase-half) hard lock
     *   - VeryRare (12): 100% combined hard lock, often the rarest combos
     *
     * **Weekday × dayPhase coverage** (Rare/VeryRare):
     *   42 hard-locked species spread over 7 weekdays × 2 day-halves
     *   (≈ 14 combinations). On average ~3 species per combination so
     *   each "Monday Night" / "Saturday Day" / etc. has its set.
     *
     * **Aspect-driven time bias** (overrides the random dayPhase pick):
     *   - SHADOW / COSMIC: forced into NIGHT half regardless of slugHash
     *   - HEROIC: forced into DAY half
     *   - VENOM: forced into DAWN/DUSK transition
     *
     * Everything else uses slugHash to pick day-half so distribution
     * stays even.
     */
    private fun autoSpawnConditions(
        primaryAspect: com.aetherbound.game.core.Aspect,
        rarity: com.aetherbound.game.core.Rarity,
        slugHash: Int,
    ): com.aetherbound.game.core.SpawnConditions {
        // ── Coverage gate by rarity ─────────────────────────────────────
        val nibble = slugHash ushr 4 and 0xF    // 0..15
        val gateThreshold = when (rarity) {
            com.aetherbound.game.core.Rarity.Common    -> 6     // ~62% conditioned (weather-only)
            com.aetherbound.game.core.Rarity.Uncommon  -> 12    // ~80% conditioned (weekday OR time)
            else                                       -> 16    // always conditioned (Rare/VeryRare)
        }
        val conditioned = nibble < gateThreshold
        if (!conditioned) return com.aetherbound.game.core.SpawnConditions.ANYTIME

        val isHard = rarity == com.aetherbound.game.core.Rarity.Rare ||
            rarity == com.aetherbound.game.core.Rarity.VeryRare

        // ── Pick weekday (hard lock for Rare/VeryRare) ──────────────────
        // For Rare/VeryRare: assign a single weekday derived from slugHash
        // so the 42 hard-locked species spread evenly across all 7 days.
        // For Uncommon: 50% chance of weekday lock (simpler 2-day window).
        val weekdayLock: Set<java.time.DayOfWeek>? = when {
            isHard -> setOf(java.time.DayOfWeek.values()[(slugHash and 0x7) % 7])
            rarity == com.aetherbound.game.core.Rarity.Uncommon && (slugHash ushr 2 and 1) == 1 -> {
                // 2-day window for Uncommon (Mon-Tue, Wed-Thu, Fri-Sat, Sun)
                val pair = (slugHash ushr 1 and 0x3)
                when (pair) {
                    0 -> setOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY)
                    1 -> setOf(java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.THURSDAY)
                    2 -> setOf(java.time.DayOfWeek.FRIDAY, java.time.DayOfWeek.SATURDAY)
                    else -> setOf(java.time.DayOfWeek.SUNDAY)
                }
            }
            else -> null
        }

        // ── Pick dayPhase half (hard lock when isHard) ─────────────────
        // Aspect-driven override: SHADOW/COSMIC always night, HEROIC always day,
        // VENOM always dawn/dusk, FROST always day-or-dawn (cold morning).
        // Else: slugHash picks day-half evenly so 50% of unrestricted aspects
        // are night-spawn, 50% day-spawn.
        val isNightHalf = (slugHash ushr 8) and 1 == 1
        val phaseLock: Set<com.aetherbound.game.core.data.DayNightPhase>? = when {
            primaryAspect == com.aetherbound.game.core.Aspect.SHADOW ||
                primaryAspect == com.aetherbound.game.core.Aspect.COSMIC ->
                setOf(
                    com.aetherbound.game.core.data.DayNightPhase.DUSK,
                    com.aetherbound.game.core.data.DayNightPhase.NIGHT,
                )
            primaryAspect == com.aetherbound.game.core.Aspect.HEROIC ->
                setOf(
                    com.aetherbound.game.core.data.DayNightPhase.MORNING,
                    com.aetherbound.game.core.data.DayNightPhase.NOON,
                    com.aetherbound.game.core.data.DayNightPhase.EVENING,
                )
            primaryAspect == com.aetherbound.game.core.Aspect.VENOM ->
                setOf(
                    com.aetherbound.game.core.data.DayNightPhase.DAWN,
                    com.aetherbound.game.core.data.DayNightPhase.DUSK,
                )
            isHard && isNightHalf -> setOf(
                com.aetherbound.game.core.data.DayNightPhase.DUSK,
                com.aetherbound.game.core.data.DayNightPhase.NIGHT,
            )
            isHard && !isNightHalf -> setOf(
                com.aetherbound.game.core.data.DayNightPhase.MORNING,
                com.aetherbound.game.core.data.DayNightPhase.NOON,
                com.aetherbound.game.core.data.DayNightPhase.EVENING,
            )
            // Uncommon w/o weekday: optional time-only lock (2-of-3 chance)
            !isHard && rarity == com.aetherbound.game.core.Rarity.Uncommon &&
                weekdayLock == null && (slugHash ushr 5 and 0x3) != 0 ->
                if (isNightHalf) setOf(
                    com.aetherbound.game.core.data.DayNightPhase.DUSK,
                    com.aetherbound.game.core.data.DayNightPhase.NIGHT,
                ) else setOf(
                    com.aetherbound.game.core.data.DayNightPhase.MORNING,
                    com.aetherbound.game.core.data.DayNightPhase.NOON,
                )
            else -> null
        }

        // ── Weather preference (always soft boost — never locks) ─────
        // Each aspect has 1-3 preferred weather types that boost spawn rate.
        val weatherPref: Set<com.aetherbound.game.core.Weather>? = when (primaryAspect) {
            com.aetherbound.game.core.Aspect.WATER -> setOf(
                com.aetherbound.game.core.Weather.RAIN,
                com.aetherbound.game.core.Weather.STORM,
            )
            com.aetherbound.game.core.Aspect.FROST -> setOf(
                com.aetherbound.game.core.Weather.SNOW,
                com.aetherbound.game.core.Weather.FOG,
            )
            com.aetherbound.game.core.Aspect.LIGHTNING -> setOf(
                com.aetherbound.game.core.Weather.STORM,
                com.aetherbound.game.core.Weather.RAIN,
            )
            com.aetherbound.game.core.Aspect.FIRE -> setOf(
                com.aetherbound.game.core.Weather.HEAT,
                com.aetherbound.game.core.Weather.CLEAR,
            )
            com.aetherbound.game.core.Aspect.SKY -> setOf(
                com.aetherbound.game.core.Weather.STORM,
                com.aetherbound.game.core.Weather.FOEHN,
                com.aetherbound.game.core.Weather.CLEAR,
            )
            com.aetherbound.game.core.Aspect.WOOD -> setOf(
                com.aetherbound.game.core.Weather.CLEAR,
                com.aetherbound.game.core.Weather.RAIN,
            )
            com.aetherbound.game.core.Aspect.SHADOW -> setOf(
                com.aetherbound.game.core.Weather.FOG,
                com.aetherbound.game.core.Weather.STORM,
            )
            com.aetherbound.game.core.Aspect.EARTH -> setOf(
                com.aetherbound.game.core.Weather.CLEAR,
                com.aetherbound.game.core.Weather.HEAT,
            )
            com.aetherbound.game.core.Aspect.METAL -> setOf(
                com.aetherbound.game.core.Weather.CLEAR,
                com.aetherbound.game.core.Weather.FOEHN,
            )
            com.aetherbound.game.core.Aspect.VENOM -> setOf(
                com.aetherbound.game.core.Weather.FOG,
                com.aetherbound.game.core.Weather.RAIN,
            )
            com.aetherbound.game.core.Aspect.COSMIC -> setOf(
                com.aetherbound.game.core.Weather.CLEAR,
                com.aetherbound.game.core.Weather.FOG,
            )
            com.aetherbound.game.core.Aspect.HEROIC -> setOf(
                com.aetherbound.game.core.Weather.CLEAR,
                com.aetherbound.game.core.Weather.HEAT,
            )
            else -> null
        }

        // Boost: harder rarity = bigger weather-match multiplier.
        val boost: Float = when (rarity) {
            com.aetherbound.game.core.Rarity.VeryRare -> 2.5f
            com.aetherbound.game.core.Rarity.Rare -> 2.0f
            com.aetherbound.game.core.Rarity.Uncommon -> 1.6f
            else -> 1.3f
        }

        return com.aetherbound.game.core.SpawnConditions(
            timeOfDay = phaseLock,
            weekdays = weekdayLock,
            weather = weatherPref,
            spawnRateBoost = boost,
        )
    }

    // Old aspect-driven when block — replaced by the combined-lock above.
    @Suppress("unused", "UNUSED_VARIABLE")
    private fun deprecatedAspectSwitch(
        primaryAspect: com.aetherbound.game.core.Aspect,
        rarity: com.aetherbound.game.core.Rarity,
        slugHash: Int,
        isHard: Boolean,
    ): com.aetherbound.game.core.SpawnConditions {
        val cond: com.aetherbound.game.core.SpawnConditions = when (primaryAspect) {
            // Tag/Nacht-Bindung ist IMMER hard — auch für Common-Species.
            // Schatten erscheint NIE bei Tag, Heroische NIE bei Nacht. Punkt.
            com.aetherbound.game.core.Aspect.SHADOW ->
                com.aetherbound.game.core.SpawnConditions.NIGHT_ONLY
            com.aetherbound.game.core.Aspect.COSMIC ->
                if (slugHash and 1 == 0)
                    com.aetherbound.game.core.SpawnConditions.NIGHT_ONLY
                else com.aetherbound.game.core.SpawnConditions.onlyOnWeekday(
                    java.time.DayOfWeek.WEDNESDAY)
            com.aetherbound.game.core.Aspect.HEROIC ->
                com.aetherbound.game.core.SpawnConditions.DAY_ONLY
            com.aetherbound.game.core.Aspect.FROST ->
                if (isHard) com.aetherbound.game.core.SpawnConditions(
                    weather = setOf(com.aetherbound.game.core.Weather.SNOW),
                    spawnRateBoost = 2.5f,
                ) else com.aetherbound.game.core.SpawnConditions.SNOW_BOOST
            com.aetherbound.game.core.Aspect.WATER ->
                com.aetherbound.game.core.SpawnConditions.RAIN_BOOST
            com.aetherbound.game.core.Aspect.LIGHTNING ->
                if (isHard) com.aetherbound.game.core.SpawnConditions.STORM_ONLY
                else com.aetherbound.game.core.SpawnConditions(
                    weather = setOf(
                        com.aetherbound.game.core.Weather.STORM,
                        com.aetherbound.game.core.Weather.RAIN,
                    ),
                    spawnRateBoost = 1.8f,
                )
            com.aetherbound.game.core.Aspect.FIRE ->
                com.aetherbound.game.core.SpawnConditions(
                    weather = if (isHard) setOf(com.aetherbound.game.core.Weather.HEAT)
                    else setOf(
                        com.aetherbound.game.core.Weather.HEAT,
                        com.aetherbound.game.core.Weather.CLEAR,
                    ),
                    spawnRateBoost = if (isHard) 2.0f else 1.5f,
                )
            com.aetherbound.game.core.Aspect.SKY ->
                com.aetherbound.game.core.SpawnConditions(
                    weather = setOf(
                        com.aetherbound.game.core.Weather.STORM,
                        com.aetherbound.game.core.Weather.CLEAR,
                        com.aetherbound.game.core.Weather.FOEHN,
                    ),
                    spawnRateBoost = if (isHard) 1.0f else 1.4f,
                )
            // Gift erscheint NUR an Dämmerungen — egal welche Seltenheit.
            com.aetherbound.game.core.Aspect.VENOM ->
                com.aetherbound.game.core.SpawnConditions.DAWN_DUSK
            com.aetherbound.game.core.Aspect.WOOD ->
                com.aetherbound.game.core.SpawnConditions(
                    timeOfDay = setOf(
                        com.aetherbound.game.core.data.DayNightPhase.DAWN,
                        com.aetherbound.game.core.data.DayNightPhase.MORNING,
                        com.aetherbound.game.core.data.DayNightPhase.NOON,
                    ),
                    spawnRateBoost = if (isHard) 1.0f else 1.3f,
                )
            com.aetherbound.game.core.Aspect.METAL ->
                com.aetherbound.game.core.SpawnConditions.onlyOnWeekday(java.time.DayOfWeek.MONDAY)
                    .takeIf { isHard }
                    ?: com.aetherbound.game.core.SpawnConditions(
                        weekdays = setOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY),
                        spawnRateBoost = 1.6f,
                    )
            com.aetherbound.game.core.Aspect.EARTH ->
                com.aetherbound.game.core.SpawnConditions(
                    weather = setOf(
                        com.aetherbound.game.core.Weather.CLEAR,
                        com.aetherbound.game.core.Weather.CLOUDY,
                        com.aetherbound.game.core.Weather.HEAT,
                    ),
                    spawnRateBoost = 1.3f,
                )
            else -> com.aetherbound.game.core.SpawnConditions.ANYTIME
        }
        return cond
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
