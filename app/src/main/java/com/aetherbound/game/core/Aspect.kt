package com.aetherbound.game.core

/**
 * Aetherbound's elemental aspects — port of Tuxemon's 13-element type system.
 *
 * Slugs match `app/src/main/assets/game/data/elements.json` 1:1 so the
 * JSON-driven dex loaders can lookup by `valueOf(slug.uppercase())`.
 *
 * Legacy Aetherbound names (EMBER/TIDE/VERDANCE/STONE/GALE/SPARK/SHADE/RADIANT/MIND/ECHO/NULL)
 * map to the new slugs as follows — see [LegacyAspect] for migration helpers:
 *   EMBER     → FIRE
 *   TIDE      → WATER
 *   VERDANCE  → WOOD
 *   STONE     → EARTH
 *   GALE      → SKY
 *   SPARK     → LIGHTNING
 *   FROST     → FROST
 *   METAL     → METAL
 *   SHADE     → SHADOW
 *   RADIANT   → HEROIC
 *   MIND      → COSMIC
 *   ECHO      → VENOM
 *   NULL      → NORMAL
 */
enum class Aspect(val slug: String) {
    COSMIC("cosmic"),
    EARTH("earth"),
    FIRE("fire"),
    FROST("frost"),
    HEROIC("heroic"),
    LIGHTNING("lightning"),
    METAL("metal"),
    NORMAL("normal"),
    SHADOW("shadow"),
    SKY("sky"),
    VENOM("venom"),
    WATER("water"),
    WOOD("wood");

    companion object {
        private val bySlug: Map<String, Aspect> = values().associateBy { it.slug }
        fun fromSlug(slug: String): Aspect? = bySlug[slug.lowercase()]
        fun fromSlugOrNormal(slug: String?): Aspect =
            slug?.let { bySlug[it.lowercase()] } ?: NORMAL
    }
}

/**
 * Tuxemon's full 13×13 type chart, transcribed from `elements.json`.
 *
 * Indexed by [Aspect.ordinal] for O(1) lookup.
 * Order must match the [Aspect] enum declaration above:
 *   COSMIC, EARTH, FIRE, FROST, HEROIC, LIGHTNING, METAL,
 *   NORMAL, SHADOW, SKY, VENOM, WATER, WOOD
 */
object AspectAffinity {
    // CHART[attacker.ordinal][defender.ordinal] = multiplier
    private val CHART: Array<DoubleArray> = arrayOf(
        // COSMIC
        doubleArrayOf(1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 0.5, 1.0, 2.0, 1.0, 1.0),
        // EARTH
        doubleArrayOf(1.0, 1.0, 2.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 0.5, 1.0, 1.0, 0.5),
        // FIRE
        doubleArrayOf(1.0, 0.5, 0.5, 0.5, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 2.0, 0.5, 2.0),
        // FROST
        doubleArrayOf(1.0, 2.0, 1.0, 0.5, 1.0, 1.0, 0.5, 1.0, 0.5, 2.0, 1.0, 0.5, 2.0),
        // HEROIC
        doubleArrayOf(0.5, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0, 2.0, 2.0, 1.0, 0.5, 1.0, 1.0),
        // LIGHTNING
        doubleArrayOf(1.0, 0.5, 1.0, 1.0, 1.0, 0.5, 2.0, 1.0, 1.0, 2.0, 1.0, 2.0, 0.5),
        // METAL
        doubleArrayOf(2.0, 2.0, 0.5, 1.0, 1.0, 1.0, 0.5, 1.0, 1.0, 1.0, 1.0, 0.5, 1.0),
        // NORMAL
        doubleArrayOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.5, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
        // SHADOW
        doubleArrayOf(2.0, 0.5, 1.0, 1.0, 0.5, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, 1.0, 1.0),
        // SKY
        doubleArrayOf(1.0, 2.0, 0.5, 1.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0),
        // VENOM
        doubleArrayOf(1.0, 0.5, 1.0, 1.0, 1.0, 1.0, 0.5, 2.0, 1.0, 1.0, 0.5, 0.5, 2.0),
        // WATER
        doubleArrayOf(1.0, 1.0, 2.0, 0.5, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.5, 0.5),
        // WOOD
        doubleArrayOf(1.0, 2.0, 0.5, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.5, 2.0, 0.5),
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
}

/**
 * Compatibility shim: maps legacy Aetherbound aspect names (used in older
 * docs, save files, and a few hard-coded tables) to the canonical Tuxemon
 * slug enum. Use during refactor; remove once nothing references legacy
 * names anymore.
 */
object LegacyAspect {
    fun fromLegacy(name: String): Aspect = when (name.uppercase()) {
        "EMBER" -> Aspect.FIRE
        "TIDE" -> Aspect.WATER
        "VERDANCE" -> Aspect.WOOD
        "STONE" -> Aspect.EARTH
        "GALE" -> Aspect.SKY
        "SPARK" -> Aspect.LIGHTNING
        "FROST" -> Aspect.FROST
        "METAL" -> Aspect.METAL
        "SHADE" -> Aspect.SHADOW
        "RADIANT" -> Aspect.HEROIC
        "MIND" -> Aspect.COSMIC
        "ECHO" -> Aspect.VENOM
        "NULL" -> Aspect.NORMAL
        else -> Aspect.fromSlug(name) ?: Aspect.NORMAL
    }
}
