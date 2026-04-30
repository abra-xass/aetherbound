package com.aetherbound.game.render.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherbound.game.core.BattleAction
import com.aetherbound.game.core.BattleEvent
import com.aetherbound.game.core.BattleResolver
import com.aetherbound.game.core.BattleState
import com.aetherbound.game.core.EchoformInstance
import com.aetherbound.game.core.Side
import com.aetherbound.game.core.data.MultiplayerRewards
import com.aetherbound.game.core.data.MultiplayerSnapshot
import com.aetherbound.game.render.theme.AetherColors
import com.aetherbound.game.render.ui.MultiplayerStatusIndicator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Live multiplayer-battle scene driven by:
 *
 *   - **Local input** — player taps a move-index; we route it to
 *     [onLocalAction] which the activity ships via the Matrix bridge.
 *   - **Remote input** — `peerActions` Flow emits the opponent's
 *     move-index when their `BATTLE_MOVE` Matrix-event arrives.
 *
 * Both clients run [BattleResolver.resolveTurn] with the SAME `rngSeed`
 * and the same paired actions, so output state is bit-identical.
 */
@Composable
fun MultiplayerArenaScene(
    initialPlayer: EchoformInstance,
    initialOpponent: EchoformInstance,
    rngSeed: Long,
    snapshot: MultiplayerSnapshot,
    selfDisplayName: String,
    peerDisplayName: String,
    selfWins: Int,
    selfLosses: Int,
    peerWins: Int,
    peerLosses: Int,
    mode: MultiplayerRewards.Mode,
    peerActions: Flow<Int>,
    onLocalAction: (Int) -> Unit,
    onMatchEnd: (MatchEndResult) -> Unit,
    onConcede: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var state by remember(rngSeed) {
        mutableStateOf(
            BattleState(
                player = initialPlayer,
                opponent = initialOpponent,
                turn = 1,
                rngSeed = rngSeed,
            )
        )
    }
    var statusLine by remember { mutableStateOf("Pick a move.") }
    var phase by remember { mutableStateOf(Phase.Choosing) }
    var playerFaints by remember { mutableIntStateOf(0) }

    // Channel for local move picks — the resolver loop suspends on receive().
    val pickGate = remember { Channel<Int>(Channel.CONFLATED) }

    // Resolver loop — keyed by rngSeed so a new match restarts cleanly.
    LaunchedEffect(rngSeed) {
        while (!state.isOver) {
            phase = Phase.Choosing
            statusLine = "Pick a move."
            val mine = pickGate.receive()
            onLocalAction(mine)
            statusLine = "Waiting for $peerDisplayName…"
            phase = Phase.Waiting
            val peer = peerActions.first()
            val resolved = BattleResolver.resolveTurn(
                state = state,
                playerAction = BattleAction.UseTechnique(mine),
                opponentAction = BattleAction.UseTechnique(peer),
            )
            phase = Phase.Animating
            val tail = resolved.log.drop(state.log.size)
            tail.filterIsInstance<BattleEvent.TechniqueResolved>().firstOrNull()?.let { ev ->
                statusLine = if (ev.missed) "${nameForSide(state, ev.side)} missed!"
                else "${nameForSide(state, ev.side)} dealt ${ev.damage}" +
                    (if (ev.crit) " CRIT" else "") +
                    (if (ev.stab) " STAB" else "")
            }
            tail.filterIsInstance<BattleEvent.Faint>().forEach { f ->
                if (f.side == Side.PLAYER) playerFaints++
            }
            state = resolved
            delay(700)
        }
        // Match over — compute reward (activity supplies real wallet figures
        // via its onMatchEnd handler; here we send the structural deltas).
        val won = state.winner == Side.PLAYER
        val sweep = won && playerFaints == 0
        val rewards = MultiplayerRewards.compute(
            winnerMVP = if (won) state.player else state.opponent,
            loserActive = if (won) state.opponent else state.player,
            winnerMoney = 0,
            loserMoney = 0,
            mode = mode,
            matchTurns = state.turn - 1,
        )
        onMatchEnd(
            MatchEndResult(
                won = won,
                isDraw = state.winner == null,
                potDelta = if (won) rewards.winnerMoneyDelta else rewards.loserMoneyDelta,
                xpDelta = if (won) rewards.winnerXpToActive else 0,
                finalTurn = state.turn,
                playerSweep = sweep,
            )
        )
    }

    Box(modifier.fillMaxSize()) {
        AetherArenaBackdrop(Modifier.fillMaxSize())

        // Top banner
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BannerSlot(selfDisplayName, selfWins, selfLosses, AetherColors.GoldBright, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text("VS", color = AetherColors.GoldBright, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                BannerSlot(peerDisplayName, peerWins, peerLosses, AetherColors.GoldCore, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
                MultiplayerStatusIndicator()
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Center) {
                Text(
                    if (mode == MultiplayerRewards.Mode.RANKED)
                        "RANKED · pot ≥ \$${MultiplayerRewards.MIN_POT}"
                    else "CASUAL · sparring",
                    color = AetherColors.MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                )
            }
        }

        // HP bars + status line
        Column(Modifier.align(Alignment.Center).fillMaxWidth().padding(20.dp)) {
            HpReadout(
                "${state.opponent.species.name} Lv.${state.opponent.level}",
                state.opponent.currentVigor, state.opponent.maxVigor, AetherColors.GoldCore,
            )
            Spacer(Modifier.height(40.dp))
            HpReadout(
                "${state.player.species.name} Lv.${state.player.level}",
                state.player.currentVigor, state.player.maxVigor, AetherColors.GoldBright,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Turn ${state.turn}  ·  $statusLine",
                color = AetherColors.ParchmentText, fontSize = 12.sp,
            )
        }

        // Bottom: move selector / waiting / animating overlay
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp)) {
            when (phase) {
                Phase.Choosing -> MoveButtons(
                    techniques = state.player.techniques,
                    enabled = !state.isOver,
                    onPick = { idx -> pickGate.trySend(idx) },
                )
                Phase.Waiting -> InfoBox("Waiting for ${peerDisplayName}'s move…", AetherColors.GoldBright)
                Phase.Animating -> InfoBox(statusLine, AetherColors.ParchmentText)
            }
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AetherColors.Slate)
                    .clickable(onClick = onConcede)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .align(Alignment.End),
            ) {
                Text("Concede", color = AetherColors.WarningRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private enum class Phase { Choosing, Waiting, Animating }

@Composable
private fun MoveButtons(
    techniques: List<com.aetherbound.game.core.Technique>,
    enabled: Boolean,
    onPick: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        techniques.forEachIndexed { i, t ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AetherColors.Slate)
                    .clickable(enabled = enabled) { onPick(i) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    "${t.name}  ·  pwr ${t.power}  ·  acc ${t.accuracy}%",
                    color = if (enabled) AetherColors.GoldBright else AetherColors.MutedText,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun InfoBox(text: String, accent: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AetherColors.Obsidian)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = accent, fontSize = 13.sp)
    }
}

@Composable
private fun HpReadout(name: String, current: Int, max: Int, accent: Color) {
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

private fun nameForSide(state: BattleState, side: Side): String = when (side) {
    Side.PLAYER -> state.player.species.name
    Side.OPPONENT -> state.opponent.species.name
}

/** Match-end snapshot delivered via [onMatchEnd]. Activity applies it. */
data class MatchEndResult(
    val won: Boolean,
    val isDraw: Boolean = false,
    val potDelta: Int,
    val xpDelta: Int,
    val finalTurn: Int,
    val playerSweep: Boolean,
)

@Composable
private fun BannerSlot(
    name: String,
    wins: Int,
    losses: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .background(AetherColors.Obsidian.copy(alpha = 0.78f))
            .padding(8.dp),
    ) {
        Text(name, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("$wins W / $losses L", color = AetherColors.MutedText, fontSize = 9.sp)
    }
}
