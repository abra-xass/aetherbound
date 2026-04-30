package com.aetherbound.game.core.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.json.JSONObject

/**
 * Receives Aether-protocol battle events that Thot's `AetherBridgeProvider`
 * decoded out of the Matrix event-stream and forwarded to us via explicit
 * package-targeted broadcast.
 *
 * Wire format:
 *
 *   Intent action = "io.aether.action.BATTLE_RELAY"
 *   Extras:
 *     "type"   → e.g. "io.aether.battle.move"
 *     "body"   → JSON string of the event body
 *     "from"   → sender's Matrix ID
 *     "room"   → Matrix room ID
 *
 * Registered as `android:exported="true"` in the manifest because cross-
 * app delivery requires it. We mitigate spoofing risk by only acting on
 * the parsed JSON and never trusting payload contents to escape the
 * battle scene (no file writes, no privileged actions). State-hash
 * validation in [com.aetherbound.game.core.BattleResolver] catches
 * malicious or buggy senders that desync from the deterministic state.
 */
class BattleRelayReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val type = intent.getStringExtra(EXTRA_TYPE) ?: return
        val bodyText = intent.getStringExtra(EXTRA_BODY) ?: "{}"
        val from = intent.getStringExtra(EXTRA_FROM) ?: ""
        val room = intent.getStringExtra(EXTRA_ROOM) ?: ""
        val body = runCatching { JSONObject(bodyText) }.getOrElse {
            Log.w(TAG, "BattleRelay: malformed body for $type")
            return
        }
        BattleRelayChannel.relay(
            BattleRelayChannel.RelayEvent(
                type = type,
                body = body,
                senderMatrixId = from,
                roomId = room,
            )
        )
    }

    companion object {
        const val ACTION = "io.aether.action.BATTLE_RELAY"
        const val EXTRA_TYPE = "type"
        const val EXTRA_BODY = "body"
        const val EXTRA_FROM = "from"
        const val EXTRA_ROOM = "room"
        private const val TAG = "BattleRelayReceiver"
    }
}
