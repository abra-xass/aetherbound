package com.aetherbound.game.core

/**
 * Aetherbound's 19 elemental aspects. Each Echoform has one primary and
 * optionally one secondary aspect. Drives damage relationships, biome
 * filtering, spawn conditions, and visual palette.
 *
 * Display-name mapping (some legacy enum names are kept for back-compat
 * but render under their canonical Aetherbound name):
 *   FROST    → "Ice"
 *   LIGHTNING → "Storm"
 *   SKY      → "Wind"
 *   HEROIC   → "Light"
 *   NORMAL   → "Aether"   ← Aetherbound's signature element
 *
 * The 6 newest elements (DREAM, MIND, SOUND, TIME, CRYSTAL, BLOOD)
 * keep their canonical name.
 *
 * Ordinal order MUST match the AspectAffinity CHART below — both arrays
 * are length 19 and indexed by enum.ordinal.
 */
enum class Aspect(val slug: String, val displayName: String) {
    // ── Original 13 (slug = legacy lowercase, displayName = canonical) ──
    COSMIC("cosmic", "Cosmic"),
    EARTH("earth", "Earth"),
    FIRE("fire", "Fire"),
    FROST("frost", "Ice"),
    HEROIC("heroic", "Light"),
    LIGHTNING("lightning", "Storm"),
    METAL("metal", "Metal"),
    NORMAL("normal", "Aether"),
    SHADOW("shadow", "Shadow"),
    SKY("sky", "Wind"),
    VENOM("venom", "Venom"),
    WATER("water", "Water"),
    WOOD("wood", "Wood"),
    // ── 6 new — Dream/Mind/Sound/Time/Crystal/Blood ──
    DREAM("dream", "Dream"),
    MIND("mind", "Mind"),
    SOUND("sound", "Sound"),
    TIME("time", "Time"),
    CRYSTAL("crystal", "Crystal"),
    BLOOD("blood", "Blood");

    companion object {
        private val bySlug: Map<String, Aspect> = values().associateBy { it.slug }
        // Also map by displayName for the new 19-element matrix lookup.
        private val byDisplay: Map<String, Aspect> = values().associateBy { it.displayName.lowercase() }
        fun fromSlug(slug: String): Aspect? = bySlug[slug.lowercase()]
        fun fromDisplay(name: String): Aspect? = byDisplay[name.lowercase()]
        fun fromSlugOrNormal(slug: String?): Aspect =
            slug?.let { bySlug[it.lowercase()] ?: byDisplay[it.lowercase()] } ?: NORMAL
    }
}

/**
 * 19×19 elemental damage matrix — multiplier when ATTACKER hits DEFENDER.
 *
 *   1.0 = neutral
 *   2.0 = strong   (Aetherbound: 3 strong relationships per element)
 *   0.5 = weak     (Aetherbound: 3 weak relationships per element)
 *   0.0 = immune   (special — only a handful of pairs)
 *
 * Generated from `docs/element-chart.json` so the table here and the
 * gameplay-design source-of-truth stay in lockstep.
 */
object AspectAffinity {
    // CHART[attacker.ordinal][defender.ordinal] = multiplier
    // Order must match the Aspect enum declaration above (length 19).
    private val CHART: Array<DoubleArray> = arrayOf(
        // COSMIC
        doubleArrayOf(1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 0.5, 0.5, 1.0, 0.5, 1.0, 1.0, 1.0, 2.0, 1.0, 2.0, 1.0, 1.0),
        // EARTH
        doubleArrayOf(1.0, 1.0, 2.0, 0.5, 1.0, 2.0, 1.0, 1.0, 1.0, 0.5, 1.0, 1.0, 0.5, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0),
        // FIRE
        doubleArrayOf(1.0, 0.5, 1.0, 2.0, 1.0, 0.5, 2.0, 1.0, 1.0, 1.0, 1.0, 0.5, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
        // FROST
        doubleArrayOf(1.0, 2.0, 0.5, 1.0, 1.0, 0.5, 0.5, 1.0, 1.0, 2.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
        // HEROIC
        doubleArrayOf(0.5, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.5, 2.0, 1.0),
        // LIGHTNING
        doubleArrayOf(1.0, 0.5, 1.0, 0.5, 1.0, 1.0, 2.0, 1.0, 1.0, 2.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.5, 1.0),
        // METAL
        doubleArrayOf(1.0, 0.5, 0.5, 2.0, 1.0, 0.5, 1.0, 1.0, 1.0, 2.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0),
        // NORMAL
        doubleArrayOf(2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 2.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.5, 1.0, 1.0),
        // SHADOW
        doubleArrayOf(1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 0.5, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 2.0, 0.5, 1.0, 1.0, 1.0),
        // SKY
        doubleArrayOf(1.0, 2.0, 1.0, 0.5, 1.0, 0.5, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 0.5, 1.0),
        // VENOM
        doubleArrayOf(1.0, 0.5, 1.0, 1.0, 1.0, 1.0, 0.5, 0.5, 1.0, 1.0, 1.0, 2.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0),
        // WATER
        doubleArrayOf(1.0, 2.0, 2.0, 0.5, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.5, 1.0, 1.0, 0.5, 1.0, 1.0, 1.0),
        // WOOD
        doubleArrayOf(1.0, 2.0, 0.5, 0.5, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.5, 2.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0),
        // DREAM
        doubleArrayOf(1.0, 1.0, 1.0, 1.0, 0.5, 1.0, 1.0, 1.0, 0.5, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 0.5, 2.0, 1.0, 2.0),
        // MIND
        doubleArrayOf(0.5, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.5, 0.5, 1.0, 2.0, 1.0, 1.0, 2.0, 1.0, 2.0, 1.0, 1.0, 1.0),
        // SOUND
        doubleArrayOf(1.0, 0.5, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 2.0, 1.0, 0.5, 0.5, 1.0, 2.0, 1.0, 1.0, 2.0, 1.0),
        // TIME
        doubleArrayOf(0.5, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 2.0, 0.5, 1.0, 1.0, 1.0, 1.0, 0.5, 1.0, 1.0, 1.0, 1.0, 2.0),
        // CRYSTAL
        doubleArrayOf(1.0, 2.0, 1.0, 1.0, 0.5, 2.0, 0.5, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.5, 1.0, 1.0, 1.0),
        // BLOOD
        doubleArrayOf(1.0, 1.0, 1.0, 1.0, 0.5, 1.0, 1.0, 2.0, 1.0, 1.0, 0.5, 1.0, 1.0, 0.5, 1.0, 1.0, 2.0, 2.0, 1.0),
    )

    fun multiplier(attack: Aspect, defenderPrimary: Aspect, defenderSecondary: Aspect?): Double {
        var mult = pairMultiplier(attack, defenderPrimary)
        if (defenderSecondary != null && defenderSecondary != defenderPrimary) {
            mult *= pairMultiplier(attack, defenderSecondary)
        }
        return mult
    }

    fun pairMultiplier(attack: Aspect, defender: Aspect): Double =
        CHART[attack.ordinal][defender.ordinal]

    /** All 19 aspects in canonical order — useful for UI iteration. */
    val allAspects: List<Aspect> = Aspect.values().toList()
}
