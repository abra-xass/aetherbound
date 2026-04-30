package com.aetherbound.game.core.data

import android.content.Intent

/**
 * Parses inbound Intents from Thot (or any caller using the
 * `io.aether.action.*` namespace) into a typed [IncomingIntent].
 *
 *   io.aether.action.OPEN_TITLE     → IncomingIntent.OpenTitle
 *   io.aether.action.BATTLE_INVITE  → IncomingIntent.BattleInvite(roomId, peer, eventId)
 *   io.aether.action.TRADE_OFFER    → IncomingIntent.TradeOffer(roomId, peer)
 *
 * The Activity calls [IncomingIntent.from] in onCreate / onNewIntent and
 * routes its scene-state from there.
 */
sealed class IncomingIntent {
    object None : IncomingIntent()
    object OpenTitle : IncomingIntent()
    data class BattleInvite(
        val roomId: String,
        val peerMatrixId: String,
        val inviteEventId: String,
    ) : IncomingIntent()
    data class TradeOffer(
        val roomId: String,
        val peerMatrixId: String,
    ) : IncomingIntent()

    companion object {
        const val ACTION_OPEN_TITLE = "io.aether.action.OPEN_TITLE"
        const val ACTION_BATTLE_INVITE = "io.aether.action.BATTLE_INVITE"
        const val ACTION_TRADE_OFFER = "io.aether.action.TRADE_OFFER"

        const val EXTRA_ROOM_ID = "io.aether.extra.ROOM_ID"
        const val EXTRA_PEER_MATRIX_ID = "io.aether.extra.PEER_MATRIX_ID"
        const val EXTRA_INVITE_EVENT_ID = "io.aether.extra.INVITE_EVENT_ID"

        fun from(intent: Intent?): IncomingIntent {
            if (intent == null) return None
            return when (intent.action) {
                ACTION_OPEN_TITLE -> OpenTitle
                ACTION_BATTLE_INVITE -> BattleInvite(
                    roomId = intent.getStringExtra(EXTRA_ROOM_ID).orEmpty(),
                    peerMatrixId = intent.getStringExtra(EXTRA_PEER_MATRIX_ID).orEmpty(),
                    inviteEventId = intent.getStringExtra(EXTRA_INVITE_EVENT_ID).orEmpty(),
                )
                ACTION_TRADE_OFFER -> TradeOffer(
                    roomId = intent.getStringExtra(EXTRA_ROOM_ID).orEmpty(),
                    peerMatrixId = intent.getStringExtra(EXTRA_PEER_MATRIX_ID).orEmpty(),
                )
                else -> None
            }
        }
    }
}
