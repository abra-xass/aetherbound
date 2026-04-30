package com.aetherbound.game.core.data

import kotlin.random.Random

/**
 * Port of Tuxemon's shape system (`Tuxemon/mods/tuxemon/db/shape/shapes.yaml`).
 *
 * Each Tuxemon monster has a `shape` slug (e.g. "dragon", "blob", "flier")
 * that determines its six base attributes:
 *   - armour  (physical defence)
 *   - dodge   (evasion / special defence)
 *   - hp      (vigor / health)
 *   - melee   (physical attack)
 *   - ranged  (special attack)
 *   - speed   (turn order)
 *
 * Values are 4..8, summing to ~36 per shape so different shapes excel at
 * different niches without being strictly stronger or weaker.
 *
 * Final stat = shape_attr * (level + coeffStats) + IV + TP_contribution
 *   coeffStats = 7  (Tuxemon mods/config_monster.yaml)
 *   IV range   = 0..15
 *   max TPs    = 150 per stat, 300 total
 */

data class ShapeAttributes(
    val armour: Int,
    val dodge: Int,
    val hp: Int,
    val melee: Int,
    val ranged: Int,
    val speed: Int,
) {
    fun sum(): Int = armour + dodge + hp + melee + ranged + speed
}

/** Tuxemon's 14 shapes. Default fallback = balanced 6/6/6/6/6/6. */
object MonsterShapes {
    private const val coeffStats = 7
    const val IV_MIN = 0
    const val IV_MAX = 15

    private val SHAPES: Map<String, ShapeAttributes> = mapOf(
        "blob" to ShapeAttributes(armour = 8, dodge = 4, hp = 8, melee = 4, ranged = 8, speed = 4),
        "brute" to ShapeAttributes(armour = 7, dodge = 5, hp = 7, melee = 8, ranged = 4, speed = 5),
        "dragon" to ShapeAttributes(armour = 7, dodge = 5, hp = 6, melee = 6, ranged = 6, speed = 6),
        "flier" to ShapeAttributes(armour = 5, dodge = 7, hp = 4, melee = 8, ranged = 4, speed = 8),
        "grub" to ShapeAttributes(armour = 7, dodge = 5, hp = 7, melee = 4, ranged = 8, speed = 5),
        "humanoid" to ShapeAttributes(armour = 5, dodge = 7, hp = 4, melee = 4, ranged = 8, speed = 8),
        "hunter" to ShapeAttributes(armour = 4, dodge = 8, hp = 5, melee = 8, ranged = 4, speed = 7),
        "landrace" to ShapeAttributes(armour = 8, dodge = 4, hp = 8, melee = 8, ranged = 4, speed = 4),
        "leviathan" to ShapeAttributes(armour = 8, dodge = 4, hp = 8, melee = 6, ranged = 6, speed = 4),
        "piscine" to ShapeAttributes(armour = 6, dodge = 6, hp = 8, melee = 6, ranged = 6, speed = 4),
        "polliwog" to ShapeAttributes(armour = 4, dodge = 8, hp = 5, melee = 4, ranged = 8, speed = 7),
        "serpent" to ShapeAttributes(armour = 6, dodge = 6, hp = 6, melee = 4, ranged = 8, speed = 6),
        "sprite" to ShapeAttributes(armour = 6, dodge = 6, hp = 4, melee = 6, ranged = 6, speed = 8),
        "varmint" to ShapeAttributes(armour = 6, dodge = 6, hp = 6, melee = 8, ranged = 4, speed = 6),
    )

    private val DEFAULT = ShapeAttributes(armour = 6, dodge = 6, hp = 6, melee = 6, ranged = 6, speed = 6)

    fun forSlug(shape: String): ShapeAttributes = SHAPES[shape.lowercase()] ?: DEFAULT

    fun allShapes(): Map<String, ShapeAttributes> = SHAPES

    /**
     * Tuxemon stat formula at a given level. Per-stat:
     *
     *     stat = shape_attr * (level + coeffStats) + IV
     *
     * Training points and custom boosts are not included — those are runtime
     * earned-progression features. Use this for canonical "base curve" stats.
     */
    fun statsAtLevel(shape: String, level: Int, ivs: ShapeAttributes? = null): ShapeAttributes {
        val s = forSlug(shape)
        val mult = level + coeffStats
        val iv = ivs ?: ShapeAttributes(0, 0, 0, 0, 0, 0)
        return ShapeAttributes(
            armour = s.armour * mult + iv.armour,
            dodge = s.dodge * mult + iv.dodge,
            hp = s.hp * mult + iv.hp,
            melee = s.melee * mult + iv.melee,
            ranged = s.ranged * mult + iv.ranged,
            speed = s.speed * mult + iv.speed,
        )
    }

    /** Random IVs (0..15) using a deterministic seed. */
    fun rollIvs(rng: Random): ShapeAttributes = ShapeAttributes(
        armour = rng.nextInt(IV_MIN, IV_MAX + 1),
        dodge = rng.nextInt(IV_MIN, IV_MAX + 1),
        hp = rng.nextInt(IV_MIN, IV_MAX + 1),
        melee = rng.nextInt(IV_MIN, IV_MAX + 1),
        ranged = rng.nextInt(IV_MIN, IV_MAX + 1),
        speed = rng.nextInt(IV_MIN, IV_MAX + 1),
    )
}

/**
 * Mapping between Tuxemon's 6 attributes and Aetherbound's existing
 * `BaseStats(vigor, force, focus, guard, ward, tempo)`. Used when an
 * Echoform is built from a TuxemonMonster snapshot.
 *
 *   vigor (HP)        ← hp
 *   force (atk)       ← melee
 *   focus (sp.atk)    ← ranged
 *   guard (def)       ← armour
 *   ward (sp.def)     ← dodge
 *   tempo (speed)     ← speed
 */
data class AetherStats(
    val vigor: Int,
    val force: Int,
    val focus: Int,
    val guard: Int,
    val ward: Int,
    val tempo: Int,
) {
    companion object {
        fun fromShape(s: ShapeAttributes): AetherStats = AetherStats(
            vigor = s.hp,
            force = s.melee,
            focus = s.ranged,
            guard = s.armour,
            ward = s.dodge,
            tempo = s.speed,
        )
    }
}
