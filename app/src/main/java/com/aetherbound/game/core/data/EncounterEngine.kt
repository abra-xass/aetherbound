package com.aetherbound.game.core.data

import android.content.Context
import com.aetherbound.game.core.EchoformInstance
import com.aetherbound.game.core.ScreenTheme
import kotlin.random.Random

/**
 * Wild-encounter trigger. Each step on an "encounter tile" rolls against
 * a base rate (Pokémon convention: ~10% per step in tall grass) modified
 * by repellent-active flag and recent-encounter cooldown.
 *
 * The engine itself is stateless; per-session counters live in
 * [EncounterState]. Callers tick `onStepCompleted()` at the end of every
 * grid-step from [com.aetherbound.game.render.map.MovementController].
 */
class EncounterEngine(
    private val ctx: Context,
    private val baseRate: Double = 0.10,
    private val cooldownAfterBattle: Int = 6,
) {
    var state: EncounterState = EncounterState()
        private set

    /**
     * Call after the player walks onto a new tile. Returns the species to
     * spawn, or null if no encounter triggered.
     *
     * @param theme biome of the new tile
     * @param onEncounterTile true if the tile counts as encounter terrain (grass, sand, water, etc)
     * @param level level the wild Echoform should spawn at
     * @param rng deterministic RNG
     */
    fun onStepCompleted(
        theme: ScreenTheme,
        onEncounterTile: Boolean,
        level: Int,
        rng: Random,
    ): EchoformInstance? {
        if (!onEncounterTile) return null
        if (state.cooldownRemaining > 0) {
            state = state.copy(cooldownRemaining = state.cooldownRemaining - 1)
            return null
        }
        if (state.repellentSteps > 0) {
            state = state.copy(repellentSteps = state.repellentSteps - 1)
            return null
        }
        if (rng.nextDouble() >= baseRate) return null

        // Pick a species from the biome's pool, weighted by rarity.
        val pool = TuxemonEchoformDex.encounterPool(ctx, theme, regionSeed = rng.nextLong(), count = 8)
        val species = pool.firstOrNull() ?: return null
        val instance = TuxemonBattleSetup.build(ctx, species.id, level) ?: return null

        state = state.copy(
            cooldownRemaining = cooldownAfterBattle,
            totalEncounters = state.totalEncounters + 1,
        )
        return instance
    }

    fun applyRepellent(steps: Int) {
        state = state.copy(repellentSteps = state.repellentSteps + steps)
    }

    fun resetCooldown() { state = state.copy(cooldownRemaining = 0) }
}

data class EncounterState(
    val cooldownRemaining: Int = 0,
    val repellentSteps: Int = 0,
    val totalEncounters: Int = 0,
)
