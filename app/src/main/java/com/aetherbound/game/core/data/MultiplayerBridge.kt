package com.aetherbound.game.core.data

import com.aetherbound.game.core.EchoformInstance

/**
 * Aetherbound ↔ Thot Messenger multiplayer interface.
 *
 * Aetherbound's parent project is the Thot encrypted messenger
 * (Matrix-based). Trades and battles will tunnel through the same Matrix
 * room a chat already lives in. Cryptographic identity reuse means every
 * Aetherbound match is end-to-end encrypted with the same keys as the chat.
 *
 * This file only declares the Kotlin-side interface. The implementation
 * lives in the Thot module and plugs in via dependency injection at app
 * startup. We keep the contract here so `core.data` remains the single
 * source of truth for game-domain objects that cross the wire.
 *
 * Wire format (JSON, sent as Matrix custom event type):
 *   trade.offer    { partyMember: EchoformInstance }
 *   trade.accept   {}
 *   trade.cancel   {}
 *   battle.invite  { teamHash: "<sha256>" }
 *   battle.start   { teamMembers: [EchoformInstance], rngSeed: long }
 *   battle.move    { turn: int, moveIdx: int }
 *   battle.switch  { turn: int, partyIdx: int }
 *   battle.faint   { turn: int, side: "host"|"guest", index: int }
 *   battle.end     { winner: "host"|"guest"|"draw" }
 */
interface MultiplayerBridge {

    val available: Boolean

    /**
     * Current connection metadata. UI uses this to show a Tor-aware
     * status indicator (latency badge, hop count, connection state).
     * Returns null if [available] is false.
     */
    suspend fun connectionInfo(): ConnectionInfo? = null

    /** Roster of Matrix DMs / rooms the player can challenge. */
    suspend fun listOpponents(): List<RemotePeer>

    suspend fun proposeTrade(peerId: String, offering: EchoformInstance): TradeSession?
    suspend fun acceptTrade(session: TradeSession): EchoformInstance?

    suspend fun inviteBattle(peerId: String, team: List<EchoformInstance>): BattleSession?
    suspend fun acceptBattleInvite(invite: BattleInvite): BattleSession?

    /** Send a battle action over the wire. */
    suspend fun sendAction(session: BattleSession, action: BattleAction)

    /** Cold-start observable stream of inbound battle actions for [session]. */
    fun observeActions(session: BattleSession): kotlinx.coroutines.flow.Flow<BattleAction>

    data class RemotePeer(val id: String, val displayName: String, val online: Boolean)

    /**
     * Snapshot of the underlying Matrix connection.
     *
     * @param transport NetworkTransport.TOR / CLEARNET / OFFLINE
     * @param latencyMs measured ping to the homeserver via current circuit;
     *   null when uncalibrated
     * @param torHops number of Tor relays in the active circuit; null when
     *   not on Tor
     * @param connected true when the homeserver is reachable
     */
    data class ConnectionInfo(
        val transport: NetworkTransport,
        val latencyMs: Long? = null,
        val torHops: Int? = null,
        val connected: Boolean = true,
        val homeserverHost: String = "",
    )

    enum class NetworkTransport { CLEARNET, TOR, OFFLINE, UNKNOWN }
    data class TradeSession(val id: String, val peer: RemotePeer)
    data class BattleSession(val id: String, val peer: RemotePeer, val rngSeed: Long, val isHost: Boolean)
    data class BattleInvite(val sessionId: String, val peer: RemotePeer, val teamPreviewHash: String)

    sealed class BattleAction {
        data class Move(val turn: Int, val moveIdx: Int) : BattleAction()
        data class Switch(val turn: Int, val partyIdx: Int) : BattleAction()
        data class Faint(val turn: Int, val side: String, val index: Int) : BattleAction()
        data class End(val winner: String) : BattleAction()
    }
}

/**
 * Default no-op bridge used when the Thot module isn't installed (single-
 * player builds, demo APKs). All operations report unavailability without
 * crashing.
 */
object NoOpMultiplayerBridge : MultiplayerBridge {
    override val available: Boolean = false
    override suspend fun listOpponents(): List<MultiplayerBridge.RemotePeer> = emptyList()
    override suspend fun proposeTrade(peerId: String, offering: EchoformInstance) = null
    override suspend fun acceptTrade(session: MultiplayerBridge.TradeSession) = null
    override suspend fun inviteBattle(peerId: String, team: List<EchoformInstance>) = null
    override suspend fun acceptBattleInvite(invite: MultiplayerBridge.BattleInvite) = null
    override suspend fun sendAction(session: MultiplayerBridge.BattleSession, action: MultiplayerBridge.BattleAction) {}
    override fun observeActions(session: MultiplayerBridge.BattleSession) = kotlinx.coroutines.flow.emptyFlow<MultiplayerBridge.BattleAction>()
}

/**
 * Static holder so the rest of the engine can call into multiplayer
 * without knowing how it's wired. App entry-point installs the real bridge:
 *
 *     Multiplayer.bridge = ThotMatrixBridge(thotModule.matrixClient)
 */
object Multiplayer {
    var bridge: MultiplayerBridge = NoOpMultiplayerBridge
}
