package com.aetherbound.game.core.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject

/**
 * Process-singleton bridge that funnels Aetherbound events received by
 * Thot (over Matrix → BroadcastReceiver) into the running multiplayer
 * scene.
 *
 * Architecture:
 *
 *   Thot's MatrixEventWatcher
 *      ↓ ingestIfAetherMessage
 *   AetherBridgeProvider
 *      ↓ if type starts "io.aether.battle.*"
 *      ↓ context.sendBroadcast(BATTLE_RELAY, setPackage="com.aetherbound.game")
 *   Aetherbound.BattleRelayReceiver
 *      ↓ BattleRelayChannel.relay(type, body)
 *   MultiplayerArenaScene's resolver-loop consumes events.peerActions
 *
 * Why a singleton:
 *   - the BroadcastReceiver runs for every incoming intent regardless of
 *     which Activity/scene is active. We need a stable target for it to
 *     write into.
 *   - the Activity collects the same SharedFlow in its scene-LaunchedEffect.
 *     SharedFlow with replay=0 means events are dropped if no collector
 *     exists, which is the right behaviour: outside-of-arena events are
 *     stale.
 */
object BattleRelayChannel {

    data class RelayEvent(
        val type: String,           // e.g. "io.aether.battle.move"
        val body: JSONObject,
        val senderMatrixId: String,
        val roomId: String,
    )

    private val _events = MutableSharedFlow<RelayEvent>(
        replay = 0,
        extraBufferCapacity = 32,
    )
    val events: SharedFlow<RelayEvent> = _events.asSharedFlow()

    /** Called by [BattleRelayReceiver] when Thot dispatches a battle event. */
    fun relay(event: RelayEvent) {
        _events.tryEmit(event)
    }
}
