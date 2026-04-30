package com.aetherbound.game.core

import kotlin.random.Random

/**
 * Tracks where the 3 Legendary Echoforms (E298/E299/E300) currently roam.
 * Each Legendary has a "current screen" position on the WorldGrid; the
 * position shifts deterministically every PHASE_STEPS player steps so they
 * feel like they're moving across the world.
 *
 * When a player triggers a wild encounter on a screen that contains a
 * Legendary's current position, there's a [LEGENDARY_ENCOUNTER_CHANCE]
 * (5%) probability that the encounter is the Legendary instead of a
 * normal biome roll.
 */
object RoamingLegendary {
    /** Number of player steps between Legendary position shifts. */
    const val PHASE_STEPS = 50

    /** Per-encounter chance of a Legendary appearing when on its current screen. */
    const val LEGENDARY_ENCOUNTER_CHANCE = 0.05

    private val LEGENDARY_IDS = listOf("E298", "E299", "E300")

    /**
     * Deterministic position of the [legendaryId] at the given step count.
     * Position cycles through a permutation of (col, row) pairs so each
     * Legendary visits every screen of the WorldGrid over time.
     */
    fun positionAt(legendaryId: String, gridCols: Int, gridRows: Int, stepCount: Long): Pair<Int, Int> {
        val phase = stepCount / PHASE_STEPS
        val seed = (legendaryId.hashCode().toLong() * 0x9E3779B97F4A7C15uL.toLong()) xor phase
        val rng = Random(seed)
        return rng.nextInt(gridCols) to rng.nextInt(gridRows)
    }

    /**
     * Returns the Legendary species that's on this screen right now, or null
     * if none. Caller still has to roll [LEGENDARY_ENCOUNTER_CHANCE] to
     * actually trigger the encounter — this just tells you which is here.
     */
    fun legendaryOnScreen(
        gridCol: Int, gridRow: Int,
        gridCols: Int, gridRows: Int,
        stepCount: Long,
    ): EchoformSpecies? {
        for (id in LEGENDARY_IDS) {
            val (lc, lr) = positionAt(id, gridCols, gridRows, stepCount)
            if (lc == gridCol && lr == gridRow) {
                return EchoformDex.byId(id)
            }
        }
        return null
    }

    /** True if a wild encounter on this screen should be the Legendary. */
    fun rollLegendaryEncounter(
        gridCol: Int, gridRow: Int,
        gridCols: Int, gridRows: Int,
        stepCount: Long,
        rng: Random,
    ): EchoformSpecies? {
        val candidate = legendaryOnScreen(gridCol, gridRow, gridCols, gridRows, stepCount)
            ?: return null
        return if (rng.nextDouble() < LEGENDARY_ENCOUNTER_CHANCE) candidate else null
    }
}
