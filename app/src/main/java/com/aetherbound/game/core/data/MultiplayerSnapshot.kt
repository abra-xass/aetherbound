package com.aetherbound.game.core.data

import com.aetherbound.game.core.EchoformInstance

/**
 * Pre-match snapshot of everything that has to roll back when the
 * multiplayer match ends. The match itself runs entirely in-memory —
 * no disk I/O while it's live — so this snapshot is the *only* source
 * of truth for "the world before".
 *
 * Restore semantics (per multiplayer-arena-design.md):
 *
 *   - HP / PP / status conditions on every party member → reset to
 *     pre-match state. Carry-over within a single match, gone after.
 *   - Items used "in battle" → not consumed (MVP rule, items disabled).
 *   - Avatar position → unchanged. Player wakes at the exact same tile
 *     they entered the multiplayer flow from.
 *   - Currency / inventory / bestiary → no change at all (MP gives no XP /
 *     no money / no caught-mark).
 *   - PlayerProgress → only `multiplayerWins / multiplayerLosses` and
 *     achievement flags update post-match, never during.
 *
 * The snapshot deep-copies via Kotlin's data-class auto-copy semantics:
 * every field is either immutable (val) or a primitive, so the copy is
 * truly independent from the live `Party` after restore.
 */
data class MultiplayerSnapshot(
    val partyDeepCopy: Party,
    val worldMapPath: String,
    val worldTileX: Int,
    val worldTileY: Int,
    val capturedAtMs: Long = System.currentTimeMillis(),
) {
    /** Apply this snapshot's party to the live state (used at match end). */
    fun restoredParty(): Party = partyDeepCopy

    /**
     * Build the world-state arguments the activity passes back to
     * `TuxemonWorldScene` so the player wakes up at the exact pre-match tile.
     */
    val worldRestoreTriple: Triple<String, Int, Int> get() =
        Triple(worldMapPath, worldTileX, worldTileY)

    companion object {
        /**
         * Capture the current state right before the match begins. Caller
         * passes the live party + world coordinates. The snapshot is
         * structurally immutable — no further work needed to "freeze" it.
         */
        fun capture(
            party: Party,
            worldMapPath: String,
            worldTileX: Int,
            worldTileY: Int,
        ): MultiplayerSnapshot = MultiplayerSnapshot(
            partyDeepCopy = party.copy(
                members = party.members.map { it.copy() },
            ),
            worldMapPath = worldMapPath,
            worldTileX = worldTileX,
            worldTileY = worldTileY,
        )
    }
}

/** Compact record of a finished multiplayer match — drives Trainer-Card history. */
data class MatchRecord(
    val opponentDisplayName: String,
    val opponentMatrixId: String,
    val won: Boolean,
    val finalTurn: Int,
    val playerSweep: Boolean,    // true if zero own faints
    val timestampMs: Long = System.currentTimeMillis(),
)

/**
 * Multiplayer-only fields layered onto [PlayerProgress] via extension —
 * keeps the meta-progress data class focused on single-player concerns.
 *
 * Stored alongside the existing PlayerProgress in SaveGameIO; defaults are
 * 0/empty so old saves load cleanly.
 */
data class MultiplayerStats(
    val wins: Int = 0,
    val losses: Int = 0,
    val recentMatches: List<MatchRecord> = emptyList(),
    /** Current consecutive-win streak. Negative values = consecutive losses. */
    val currentStreak: Int = 0,
    /** All-time peak win streak. */
    val longestStreak: Int = 0,
    /**
     * Biggest level-differential upset: positive number = how many levels
     * lower the player was when they beat a higher-level opponent.
     * 0 = no upsets logged yet.
     */
    val biggestUpset: Int = 0,
    /** Sum of all pot-anti winnings over the player's lifetime. */
    val totalPotWon: Int = 0,
    /** Sum of all pot-anti losses (positive number, "spent on pots"). */
    val totalPotLost: Int = 0,
) {
    val totalMatches: Int get() = wins + losses
    val winRate: Double get() = if (totalMatches == 0) 0.0 else wins.toDouble() / totalMatches
    val netPot: Int get() = totalPotWon - totalPotLost

    /**
     * Apply a finished match. Updates streak monotonically (resets on
     * opposite outcome), tracks longest streak as a high-water-mark, and
     * appends to the recent-N rolling list.
     */
    fun recordMatch(
        record: MatchRecord,
        potDelta: Int = 0,
        levelDifferential: Int = 0,
        keepRecent: Int = 10,
    ): MultiplayerStats {
        val newStreak = when {
            record.won && currentStreak >= 0 -> currentStreak + 1
            record.won && currentStreak < 0 -> 1
            !record.won && currentStreak <= 0 -> currentStreak - 1
            else -> -1
        }
        return copy(
            wins = wins + (if (record.won) 1 else 0),
            losses = losses + (if (record.won) 0 else 1),
            recentMatches = (listOf(record) + recentMatches).take(keepRecent),
            currentStreak = newStreak,
            longestStreak = maxOf(longestStreak, if (newStreak > 0) newStreak else 0),
            biggestUpset = if (record.won) maxOf(biggestUpset, levelDifferential) else biggestUpset,
            totalPotWon = totalPotWon + (if (potDelta > 0) potDelta else 0),
            totalPotLost = totalPotLost + (if (potDelta < 0) -potDelta else 0),
        )
    }
}
