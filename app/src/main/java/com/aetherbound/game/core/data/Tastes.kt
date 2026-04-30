package com.aetherbound.game.core.data

import com.aetherbound.game.core.StatKey
import com.aetherbound.game.core.Temperament
import kotlin.random.Random

/**
 * Port of Tuxemon's taste system (`Tuxemon/mods/tuxemon/db/taste/taste.yaml`).
 *
 * Every monster has TWO tastes — one "warm" (boost: ×1.1) and one "cold"
 * (penalty: ×0.9) — chosen at creation and persistent for the monster's
 * lifetime. Tastes affect five attribute slots (speed/melee/armour/ranged/
 * dodge) plus rare HP variants (savory ×1.05 / bland ×0.95).
 *
 * Aetherbound's existing [Temperament] is the same idea (Pokémon-style
 * Nature: +10% on one stat, -10% on another). [Tastes.toTemperament]
 * combines a warm + cold pair into one Temperament so the existing
 * StatFormula picks them up without engine changes.
 *
 * | slug      | type | stat   | mult |
 * |-----------|------|--------|------|
 * | mild      | cold | speed  | 0.9  |
 * | sweet     | cold | melee  | 0.9  |
 * | soft      | cold | armour | 0.9  |
 * | flakey    | cold | ranged | 0.9  |
 * | dry       | cold | dodge  | 0.9  |
 * | bland     | cold | hp     | 0.95 |
 * | peppy     | warm | speed  | 1.1  |
 * | salty     | warm | melee  | 1.1  |
 * | hearty    | warm | armour | 1.1  |
 * | zesty     | warm | ranged | 1.1  |
 * | refined   | warm | dodge  | 1.1  |
 * | savory    | warm | hp     | 1.05 |
 */

enum class TasteType { WARM, COLD }

data class Taste(
    val slug: String,
    val type: TasteType,
    val statKey: StatKey,
    val multiplier: Double,
    /** 1.0 = common, 0.4 = rare (savory/bland). */
    val rarityScore: Double = 1.0,
)

object Tastes {

    val MILD    = Taste("mild",    TasteType.COLD, StatKey.TEMPO, 0.9)
    val SWEET   = Taste("sweet",   TasteType.COLD, StatKey.FORCE, 0.9)
    val SOFT    = Taste("soft",    TasteType.COLD, StatKey.GUARD, 0.9)
    val FLAKEY  = Taste("flakey",  TasteType.COLD, StatKey.FOCUS, 0.9)
    val DRY     = Taste("dry",     TasteType.COLD, StatKey.WARD,  0.9)
    val BLAND   = Taste("bland",   TasteType.COLD, StatKey.VIGOR, 0.95, rarityScore = 0.4)

    val PEPPY   = Taste("peppy",   TasteType.WARM, StatKey.TEMPO, 1.1)
    val SALTY   = Taste("salty",   TasteType.WARM, StatKey.FORCE, 1.1)
    val HEARTY  = Taste("hearty",  TasteType.WARM, StatKey.GUARD, 1.1)
    val ZESTY   = Taste("zesty",   TasteType.WARM, StatKey.FOCUS, 1.1)
    val REFINED = Taste("refined", TasteType.WARM, StatKey.WARD,  1.1)
    val SAVORY  = Taste("savory",  TasteType.WARM, StatKey.VIGOR, 1.05, rarityScore = 0.4)

    val ALL: List<Taste> = listOf(MILD, SWEET, SOFT, FLAKEY, DRY, BLAND,
                                   PEPPY, SALTY, HEARTY, ZESTY, REFINED, SAVORY)

    val WARM: List<Taste> = ALL.filter { it.type == TasteType.WARM }
    val COLD: List<Taste> = ALL.filter { it.type == TasteType.COLD }

    private val bySlug: Map<String, Taste> = ALL.associateBy { it.slug }

    fun bySlug(slug: String): Taste? = bySlug[slug.lowercase()]

    /**
     * Roll one warm + one cold taste using rarityScore as the weight.
     * Mirrors Tuxemon's monster-creation flow.
     */
    fun roll(rng: Random): Pair<Taste, Taste> {
        return weightedPick(WARM, rng) to weightedPick(COLD, rng)
    }

    private fun weightedPick(pool: List<Taste>, rng: Random): Taste {
        val total = pool.sumOf { it.rarityScore }
        var r = rng.nextDouble() * total
        for (t in pool) {
            r -= t.rarityScore
            if (r <= 0) return t
        }
        return pool.last()
    }

    /**
     * Translate a (warm, cold) pair into Aetherbound's [Temperament].
     *
     * Aetherbound's StatFormula applies a fixed ×1.1 / ×0.9 modifier to the
     * boosted/reduced stat — a perfect match for the canonical 5 tastes
     * (speed/melee/armour/ranged/dodge → TEMPO/FORCE/GUARD/FOCUS/WARD).
     *
     * For the rare HP variants (savory ×1.05 / bland ×0.95) the engine still
     * applies the standard ±10%; the 5 % numerical mismatch is a deliberate
     * trade-off to avoid forking [StatFormula].
     */
    fun toTemperament(warm: Taste?, cold: Taste?): Temperament {
        val name = listOfNotNull(warm?.slug, cold?.slug).joinToString("/").ifEmpty { "Balanced" }
        return Temperament(
            name = name,
            boosted = warm?.statKey,
            reduced = cold?.statKey,
        )
    }
}
