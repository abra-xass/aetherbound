package com.aetherbound.game.core.data

import com.aetherbound.game.core.EchoformInstance
import com.aetherbound.game.core.StatKey

/**
 * Held items — one per Echoform, kicks in passively during battle.
 * Equivalent to Pokémon's held-items system.
 *
 * MVP set (4 archetypes):
 *
 *   - **CHOICE_BAND** — +50% Force at the cost of being locked into the
 *     first move chosen this match.
 *   - **FOCUS_SASH** — survive one KO-blow at 1 HP. One-shot per match.
 *   - **LUM_BERRY** — auto-cures the first major status condition.
 *     One-shot per match.
 *   - **SITRUS_BERRY** — restores 25% max HP when the holder drops below
 *     50% for the first time. One-shot per match.
 *
 * Held items are slot-locked to one EchoformInstance and persist across
 * battles. They are *not* consumed in single-player wild battles even
 * if their effect triggers — the once-per-match flag resets on match
 * start so the item is reusable.
 *
 * Multiplayer: items are toggled off by default in the lobby (per
 * docs/multiplayer-arena-design.md MVP rules). Future format-toggle
 * "items allowed" flips this on.
 */
enum class HeldItem(val displayName: String, val description: String) {
    NONE("—", "No held item"),
    CHOICE_BAND("Choice Band", "Force ×1.5 but locks into first move"),
    FOCUS_SASH("Focus Sash", "Survive one knockout at 1 HP"),
    LUM_BERRY("Lum Berry", "Cures first major status"),
    SITRUS_BERRY("Sitrus Berry", "Restores 25% HP at half-HP threshold"),
}

/**
 * Per-Echoform-per-match runtime flags for one-shot held-item effects.
 * Reset at the start of every battle.
 */
data class HeldItemMatchState(
    val sashUsed: Boolean = false,
    val lumUsed: Boolean = false,
    val sitrusUsed: Boolean = false,
    val chosenMoveLock: Int? = null,
)

object HeldItemEngine {

    /**
     * Apply Choice Band + Sitrus + Sash *passively* before each turn's damage
     * resolution. Returns the modified [EchoformInstance] and updated state.
     *
     * The function is a no-op for [HeldItem.NONE] holders. For Sash the
     * "survive at 1 HP" check is applied after damage in [postHitGuard].
     */
    fun preTurnEffects(
        holder: EchoformInstance,
        held: HeldItem,
        state: HeldItemMatchState,
    ): Pair<EchoformInstance, HeldItemMatchState> {
        var inst = holder
        var newState = state

        // Sitrus — heal once when crossing the 50% threshold.
        if (held == HeldItem.SITRUS_BERRY && !state.sitrusUsed) {
            val ratio = inst.currentVigor.toDouble() / inst.maxVigor.coerceAtLeast(1)
            if (ratio < 0.5 && ratio > 0) {
                val heal = (inst.maxVigor * 0.25).toInt().coerceAtLeast(1)
                val newHp = (inst.currentVigor + heal).coerceAtMost(inst.maxVigor)
                inst = inst.copy(currentVigor = newHp)
                newState = newState.copy(sitrusUsed = true)
            }
        }

        return inst to newState
    }

    /**
     * Apply the Focus Sash "survive at 1 HP" guard. Called immediately after
     * the resolver computes damage but before the holder's currentVigor is
     * actually mutated. Returns the clamped HP plus updated state flag.
     */
    fun postHitGuard(
        holder: EchoformInstance,
        held: HeldItem,
        state: HeldItemMatchState,
        intendedDamage: Int,
    ): Pair<Int, HeldItemMatchState> {
        val rawHp = (holder.currentVigor - intendedDamage).coerceAtLeast(0)
        if (held == HeldItem.FOCUS_SASH && !state.sashUsed) {
            val ratio = holder.currentVigor.toDouble() / holder.maxVigor.coerceAtLeast(1)
            if (ratio >= 0.99 && rawHp <= 0) {
                // Was full-HP, would have fainted — survive at 1 HP.
                return 1 to state.copy(sashUsed = true)
            }
        }
        return rawHp to state
    }

    /**
     * Lum Berry status cleanse — called when a status condition is about to
     * be applied. Returns true if the berry consumed itself to prevent
     * the application; false otherwise.
     */
    fun lumCheck(held: HeldItem, state: HeldItemMatchState): Pair<Boolean, HeldItemMatchState> {
        if (held == HeldItem.LUM_BERRY && !state.lumUsed) {
            return true to state.copy(lumUsed = true)
        }
        return false to state
    }

    /**
     * Force-stat multiplier from passive items. Currently only
     * Choice Band; extensible.
     */
    fun forceMultiplier(held: HeldItem): Double = when (held) {
        HeldItem.CHOICE_BAND -> 1.5
        else -> 1.0
    }

    /**
     * Lock-in for Choice Band: once the holder picks a move-index, they
     * can't switch to a different one for the rest of the match. Returns
     * the index they're forced to pick (or null if no lock).
     */
    fun forcedMoveIndex(held: HeldItem, state: HeldItemMatchState): Int? = when {
        held == HeldItem.CHOICE_BAND && state.chosenMoveLock != null -> state.chosenMoveLock
        else -> null
    }

    /** Record the move-index Choice Band has now committed to. */
    fun recordMovePick(
        held: HeldItem,
        state: HeldItemMatchState,
        moveIdx: Int,
    ): HeldItemMatchState =
        if (held == HeldItem.CHOICE_BAND && state.chosenMoveLock == null)
            state.copy(chosenMoveLock = moveIdx)
        else state
}
