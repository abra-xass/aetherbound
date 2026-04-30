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
) {
    val totalMatches: Int get() = wins + losses
    val winRate: Double get() = if (totalMatches == 0) 0.0 else wins.toDouble() / totalMatches

    fun recordMatch(record: MatchRecord, keepRecent: Int = 10): MultiplayerStats =
        copy(
            wins = wins + (if (record.won) 1 else 0),
            losses = losses + (if (record.won) 0 else 1),
            recentMatches = (listOf(record) + recentMatches).take(keepRecent),
        )
}
