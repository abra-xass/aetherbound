package com.aetherbound.game.core

import kotlin.random.Random

/**
 * Rarity tier for an Echoform species. Drives catch rate, base stat budget,
 * and spawn frequency in the per-biome encounter pool.
 *
 * Distribution across 300 species:
 *   Common      60%  (180)   — catch 190, stat budget ~420
 *   Uncommon    25%  ( 75)   — catch  90, stat budget ~480
 *   Rare        10%  ( 30)   — catch  45, stat budget ~540
 *   VeryRare     4%  ( 12)   — catch  15, stat budget ~580
 *   Legendary    1%  (  3)   — catch   3, stat budget ~640, roaming spawn only
 */
enum class Rarity(
    val catchRate: Int,
    val statBudget: Int,
    val poolWeight: Int,            // weighting in normal encounter pool
    val countOf300: Int,
) {
    Common(catchRate = 190, statBudget = 420, poolWeight = 70, countOf300 = 180),
    Uncommon(catchRate = 90, statBudget = 480, poolWeight = 20, countOf300 = 75),
    Rare(catchRate = 45, statBudget = 540, poolWeight = 8, countOf300 = 30),
    VeryRare(catchRate = 15, statBudget = 580, poolWeight = 2, countOf300 = 12),
    Legendary(catchRate = 3, statBudget = 640, poolWeight = 0, countOf300 = 3);

    companion object {
        /**
         * Deterministically assigns a rarity based on the species ordinal,
         * ensuring exactly 180/75/30/12/3 of each tier across 300 species.
         * Spread evenly using a permutation seeded on the dex constant.
         */
        fun forOrdinal(ordinal: Int): Rarity {
            // Build a fixed assignment list once: [Common×180, Uncommon×75, …]
            // shuffled with a stable seed so the same ordinal always gets the
            // same rarity. Last 3 ordinals (E298/E299/E300) are reserved as
            // the 3 Legendary slots regardless of shuffle.
            if (ordinal in 298..300) return Legendary
            return ASSIGNMENT[ordinal - 1]
        }

        private val ASSIGNMENT: List<Rarity> by lazy {
            val list = ArrayList<Rarity>(300)
            // 297 non-legendary slots: 180 common, 75 uncommon, 30 rare, 12 very-rare
            repeat(180) { list += Common }
            repeat(75) { list += Uncommon }
            repeat(30) { list += Rare }
            repeat(12) { list += VeryRare }
            list.shuffle(Random(0xAE7E_B0_07_C0_DE.toLong()))
            // Pad to 300 with placeholders (E298..E300 are always Legendary; the
            // shuffle only owns indices 0..296). Add 3 sentinel Commons that
            // forOrdinal() will never read.
            while (list.size < 300) list += Common
            list
        }
    }
}
