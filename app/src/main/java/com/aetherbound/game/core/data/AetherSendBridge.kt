package com.aetherbound.game.core.data

import android.content.Context
import android.content.Intent
import org.json.JSONObject

/**
 * Sends Aetherbound-protocol events back through Thot's Matrix-bridge.
 *
 * Inverse of [BattleRelayChannel] — that one carries Thot → Aetherbound;
 * this carries Aetherbound → Thot. Aetherbound has no Matrix client of
 * its own (architectural choice — see docs/multiplayer-thot-binding.md
 * "Tor-Compliance" section), so it dispatches a package-targeted
 * broadcast at Thot, and Thot's receiver invokes its
 * `AetherTextTunnelSender.sendCustomEvent` to actually post the
 * `io.aether.*` event into the Matrix room.
 *
 * Wire:
 *
 *   Intent action = "com.thot.messenger.AETHER_SEND"
 *   Extras:
 *     "eventType" — e.g. "io.aether.battle.move"
 *     "body"      — JSON string
 *     "room"      — Matrix room id
 */
object AetherSendBridge {

    const val ACTION = "com.thot.messenger.AETHER_SEND"
    const val PACKAGE = "com.thot.messenger"

    fun send(ctx: Context, roomId: String, eventType: String, body: JSONObject) {
        val intent = Intent(ACTION).apply {
            setPackage(PACKAGE)
            putExtra("eventType", eventType)
            putExtra("body", body.toString())
            putExtra("room", roomId)
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        }
        runCatching { ctx.sendBroadcast(intent) }
    }

    fun sendBattleMove(ctx: Context, roomId: String, turn: Int, moveIdx: Int, stateHash: String) {
        send(ctx, roomId, MatrixWireFormat.EventType.BATTLE_MOVE, JSONObject().apply {
            put("turn", turn)
            put("moveIdx", moveIdx)
            put("stateHash", stateHash)
            put("ts", System.currentTimeMillis())
        })
    }

    /**
     * Host-only: emit BATTLE_START after both peers have ready'd in the
     * lobby. Carries the rngSeed (deterministic resolver) plus full host
     * + guest team JSONs. Both sides transition to the arena right after.
     */
    fun sendBattleStart(
        ctx: Context,
        roomId: String,
        rngSeed: Long,
        hostTeam: List<com.aetherbound.game.core.EchoformInstance>,
        guestTeam: List<com.aetherbound.game.core.EchoformInstance>,
    ) {
        send(
            ctx, roomId, MatrixWireFormat.EventType.BATTLE_START,
            MatrixWireFormat.encodeBattleStart(rngSeed, hostTeam, guestTeam),
        )
    }

    /**
     * Lobby ready-signal — both peers fire this when tapping Ready.
     * Reuses BATTLE_INVITE_REPLY as a generic "I'm in the lobby committed"
     * signal so we don't need a new event-type.
     */
    fun sendLobbyReady(ctx: Context, roomId: String, inviteEventId: String) {
        send(
            ctx, roomId, MatrixWireFormat.EventType.BATTLE_INVITE_REPLY,
            JSONObject().apply {
                put("originalEventId", inviteEventId)
                put("accepted", true)
                put("phase", "lobby_ready")
                put("ts", System.currentTimeMillis())
            },
        )
    }

    fun sendBattleEnd(ctx: Context, roomId: String, winner: String) {
        send(ctx, roomId, MatrixWireFormat.EventType.BATTLE_END, JSONObject().apply {
            put("winner", winner)
            put("ts", System.currentTimeMillis())
        })
    }
}
