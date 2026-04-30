package com.aetherbound.game.render.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherbound.game.core.BattleAction
import com.aetherbound.game.core.BattleResolver
import com.aetherbound.game.core.BattleState
import com.aetherbound.game.core.EchoformInstance
import com.aetherbound.game.core.Side
import com.aetherbound.game.render.theme.AetherColors
import com.aetherbound.game.render.ui.EchoformSpriteImage
import com.aetherbound.game.render.ui.EchoformSpriteVariant
import com.aetherbound.game.render.ui.MultiplayerStatusIndicator
import kotlinx.coroutines.flow.Flow

/**
 * Read-only Arena scene for a third-party Thot-room peer who's
 * subscribed to a battle in progress. Renders exactly what the host
 * + guest see, but with no input — just observes the move-stream.
 *
 * Architecture: same deterministic resolver, same rngSeed. Spectator
 * starts from the BATTLE_START event's host/guest team payload (which
 * carries the initial state) and replays each BATTLE_MOVE as it
 * arrives. Output is identical to what host + guest experience locally.
 *
 * Spectators don't pay pots, don't get XP/wins, can't interact. They
 * tap "Stop spectating" to leave; the timeline auto-closes when the
 * battle.end event arrives.
 */
@Composable
fun SpectatorArenaScene(
    initialHost: EchoformInstance,
    initialGuest: EchoformInstance,
    rngSeed: Long,
    hostDisplayName: String,
    guestDisplayName: String,
    /**
     * Pairs of (host-move, guest-move) in turn order. Both moves of a
     * turn must arrive before we resolve. The activity collects
     * BATTLE_MOVE events and pairs them up.
     */
    turnPairs: Flow<Pair<Int, Int>>,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var state by remember(rngSeed) {
        mutableStateOf(
            BattleState(
                player = initialHost,
                opponent = initialGuest,
                turn = 1,
                rngSeed = rngSeed,
            )
        )
    }
    var lastEventLine by remember { mutableStateOf("Spectating $hostDisplayName vs $guestDisplayName") }

    LaunchedEffect(rngSeed) {
        turnPairs.collect { (hostMove, guestMove) ->
            if (state.isOver) return@collect
            val resolved = BattleResolver.resolveTurn(
                state = state,
                playerAction = BattleAction.UseTechnique(hostMove),
                opponentAction = BattleAction.UseTechnique(guestMove),
            )
            // Surface a brief readout
            val ev = resolved.log.drop(state.log.size)
                .filterIsInstance<com.aetherbound.game.core.BattleEvent.TechniqueResolved>()
                .firstOrNull()
            if (ev != null) {
                val name = if (ev.side == Side.PLAYER) hostDisplayName else guestDisplayName
                lastEventLine = "$name → ${ev.damage} dmg" +
                    (if (ev.crit) " CRIT" else "") +
                    (if (ev.stab) " STAB" else "")
            }
            state = resolved
        }
    }

    Box(modifier.fillMaxSize()) {
        AetherArenaBackdrop(Modifier.fillMaxSize())

        // Banner: host vs guest names + connection
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BannerBadge(hostDisplayName, AetherColors.GoldBright, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text("VS", color = AetherColors.GoldBright, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                BannerBadge(guestDisplayName, AetherColors.GoldCore, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                MultiplayerStatusIndicator()
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Center) {
                Text(
                    "👁  SPECTATING  ·  read-only",
                    color = AetherColors.MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                )
            }
        }

        // Sprites
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = maxWidth
            val h = maxHeight
            Box(Modifier.offset(x = w * 0.55f, y = h * 0.20f).size(160.dp)) {
                EchoformSpriteImage(
                    slug = state.opponent.species.id,
                    variant = EchoformSpriteVariant.FRONT,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(Modifier.offset(x = w * 0.05f, y = h * 0.50f).size(200.dp)) {
                EchoformSpriteImage(
                    slug = state.player.species.id,
                    variant = EchoformSpriteVariant.BACK,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // HP bars + last-event readout
        Column(Modifier.align(Alignment.Center).fillMaxWidth().padding(20.dp)) {
            HpReadoutSpec(
                "${state.opponent.species.name} Lv.${state.opponent.level}",
                state.opponent.currentVigor, state.opponent.maxVigor, AetherColors.GoldCore,
            )
            Spacer(Modifier.height(40.dp))
            HpReadoutSpec(
                "${state.player.species.name} Lv.${state.player.level}",
                state.player.currentVigor, state.player.maxVigor, AetherColors.GoldBright,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Turn ${state.turn}  ·  $lastEventLine",
                color = AetherColors.ParchmentText, fontSize = 12.sp,
            )
        }

        // Bottom: leave-button only
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AetherColors.Slate)
                .clickable(onClick = onLeave)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text("Stop spectating", color = AetherColors.GoldBright, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BannerBadge(name: String, accent: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(
        modifier
            .background(AetherColors.Obsidian.copy(alpha = 0.78f))
            .padding(8.dp),
    ) {
        Text(name, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HpReadoutSpec(name: String, current: Int, max: Int, accent: androidx.compose.ui.graphics.Color) {
    val ratio = (current.toFloat() / max.coerceAtLeast(1)).coerceIn(0f, 1f)
    Column {
        Text(name, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(AetherColors.ObsidianDeep)
                .padding(1.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(ratio)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accent),
            )
        }
        Text("$current / $max", color = AetherColors.MutedText, fontSize = 10.sp)
    }
}
