package com.aetherbound.game.render.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherbound.game.core.EchoformInstance
import com.aetherbound.game.core.data.MultiplayerRewards
import com.aetherbound.game.core.data.MultiplayerSnapshot
import com.aetherbound.game.render.theme.AetherColors
import com.aetherbound.game.render.ui.MultiplayerStatusIndicator

/**
 * Multiplayer-Arena battle wrapper. Distinct from [BattleScene]:
 *
 *   - Uses [AetherArenaBackdrop] (cosmic void + gold ring) instead of the
 *     biome-derived single-player arena.
 *   - Top banner shows BOTH trainer cards with name + W/L + connection status.
 *   - No Capture / Run-Away buttons. "Concede" replaces Run.
 *   - On match end, applies [MultiplayerRewards.compute] and snaps the
 *     world state back via the carried [MultiplayerSnapshot].
 *
 * **MVP scope.** This composable is a skeleton: it lays out the arena,
 * banner, and result-flow, then defers to [BattleScene]'s existing
 * resolver+animation stack for the per-turn rendering. Real wire-protocol
 * sync (BATTLE_MOVE / state-hash exchange) is wired by the activity-level
 * MultiplayerBridge integration when it lands.
 */
@Composable
fun MultiplayerArenaScene(
    snapshot: MultiplayerSnapshot,
    selfDisplayName: String,
    peerDisplayName: String,
    selfWins: Int,
    selfLosses: Int,
    peerWins: Int,
    peerLosses: Int,
    mode: MultiplayerRewards.Mode,
    rngSeed: Long,
    onMatchEnd: (result: MatchEndResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    var matchTurns by remember { mutableIntStateOf(0) }
    var ended by remember { mutableStateOf(false) }
    var endResult by remember { mutableStateOf<MatchEndResult?>(null) }

    Box(modifier.fillMaxSize()) {
        AetherArenaBackdrop(Modifier.fillMaxSize())

        // Top banner: both trainers + connection
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BannerSlot(
                    name = selfDisplayName,
                    wins = selfWins, losses = selfLosses,
                    accent = AetherColors.GoldBright,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "VS",
                    color = AetherColors.GoldBright,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(8.dp))
                BannerSlot(
                    name = peerDisplayName,
                    wins = peerWins, losses = peerLosses,
                    accent = AetherColors.GoldCore,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
            ) {
                MultiplayerStatusIndicator()
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                val modeBadge = if (mode == MultiplayerRewards.Mode.RANKED)
                    "RANKED · pot ≥ \$${MultiplayerRewards.MIN_POT}"
                else "CASUAL · sparring"
                Text(
                    modeBadge,
                    color = AetherColors.MutedText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Centre: actual battle render
        // For MVP this is a placeholder showing the snapshot's first member
        // and "Match starting…". Real per-turn rendering will reuse
        // BattleScene's resolver-driven animation stack once the bridge
        // pumps BATTLE_MOVE events into a shared BattleState here.
        Column(
            Modifier.align(Alignment.Center).padding(24.dp),
        ) {
            Text(
                "Match seed: ${rngSeed.toString(16).take(12)}…",
                color = AetherColors.MutedText, fontSize = 10.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Turn $matchTurns",
                color = AetherColors.GoldBright,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "Match resolution streams here once the bridge dispatches BATTLE_MOVE events.",
                color = AetherColors.ParchmentText,
                fontSize = 11.sp,
            )
        }

        // End-of-match overlay
        endResult?.let { r ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (r.won) "VICTORY" else "DEFEAT",
                        color = if (r.won) AetherColors.GoldBright else AetherColors.WarningRed,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (r.potDelta != 0) {
                        Text(
                            "${if (r.potDelta > 0) "+" else ""}\$${r.potDelta}",
                            color = AetherColors.GoldBright,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (r.xpDelta > 0) {
                        Text(
                            "+${r.xpDelta} XP",
                            color = AetherColors.GoldBright,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}

/** Result snapshot delivered via [onMatchEnd]. Activity applies it to state. */
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
    accent: androidx.compose.ui.graphics.Color,
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
