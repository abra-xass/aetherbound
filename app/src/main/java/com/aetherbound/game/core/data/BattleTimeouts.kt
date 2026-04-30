package com.aetherbound.game.core.data

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Timeout configuration for multiplayer battles.
 *
 * **Tor-aware defaults.** Aetherbound rides on Thot's Matrix client, which
 * is mandatorily routed over Tor (per Thot's threat model). Tor adds
 * 500-1500ms baseline latency plus occasional 5-10s spikes during
 * circuit rebuild. Our defaults are tuned for that, not for clearnet.
 *
 * Three primary timeouts:
 *
 *   - **moveTimeout** — how long a player has to pick a move before the
 *     opponent forfeits the turn for them. 45s instead of Pokémon's
 *     classic 30s, leaving slack for a Tor-circuit-flap mid-decision.
 *
 *   - **inviteExpiry** — how long a battle/trade invite is valid. 5min
 *     so the recipient has plenty of time to react, even if push is
 *     delayed by Tor relay churn.
 *
 *   - **syncTolerance** — max gap between an action being sent and the
 *     opponent's confirmation/refute (state-hash check) before we abort
 *     the battle for desync. 30s is generous — a typical Tor-Matrix
 *     round-trip is well under 5s.
 */
data class BattleTimeouts(
    val moveTimeout: Duration = 45.seconds,
    val inviteExpiry: Duration = 5.minutes,
    val syncTolerance: Duration = 30.seconds,
    val captureWaitTimeout: Duration = 20.seconds,
) {
    companion object {
        /** Tor-aware defaults — used for production multiplayer. */
        val Tor = BattleTimeouts()

        /** Aggressive defaults for clearnet single-player previews / debug. */
        val Clearnet = BattleTimeouts(
            moveTimeout = 30.seconds,
            inviteExpiry = 2.minutes,
            syncTolerance = 8.seconds,
            captureWaitTimeout = 8.seconds,
        )

        /** Pre-recorded replay playback — no network at all. */
        val Replay = BattleTimeouts(
            moveTimeout = Duration.INFINITE,
            inviteExpiry = Duration.INFINITE,
            syncTolerance = Duration.INFINITE,
            captureWaitTimeout = Duration.INFINITE,
        )
    }
}
