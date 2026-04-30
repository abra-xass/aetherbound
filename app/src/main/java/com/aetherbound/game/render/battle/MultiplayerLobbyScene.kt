package com.aetherbound.game.render.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherbound.game.core.data.MultiplayerRewards
import com.aetherbound.game.core.data.MultiplayerStats
import com.aetherbound.game.render.theme.AetherColors
import com.aetherbound.game.render.ui.MultiplayerStatusIndicator

/**
 * Pre-match lobby for multiplayer battles.
 *
 *   ╔══════════ AETHER ARENA ══════════╗
 *   ║                                   ║
 *   ║  YOU              OPPONENT        ║
 *   ║  Aether (32W/18L) Bob (12W/3L)    ║
 *   ║  WinRate 64%      WinRate 80%     ║
 *   ║                                   ║
 *   ║          MATCH SETTINGS           ║
 *   ║  Mode: [Ranked] [Casual]          ║
 *   ║  Cap:  [10] [30] [50] [None]      ║
 *   ║  Pot:  $5,000 floor               ║
 *   ║                                   ║
 *   ║       [READY]    [DECLINE]        ║
 *   ║                                   ║
 *   ║  🟡 TOR · 3 hops · 850ms          ║
 *   ╚═══════════════════════════════════╝
 *
 * Both players see a synced version of this screen. Once both tap "Ready",
 * the host fires `BATTLE_START` which carries the rngSeed + agreed
 * MatchSettings; both clients then transition to [MultiplayerArenaScene].
 */
@Composable
fun MultiplayerLobbyScene(
    selfDisplayName: String,
    selfStats: MultiplayerStats,
    selfMoney: Int,
    peerDisplayName: String,
    peerStats: MultiplayerStats?,    // null if not yet revealed
    peerMoney: Int?,
    onReady: (mode: MultiplayerRewards.Mode, levelCap: Int) -> Unit,
    onDecline: () -> Unit,
    iAmReady: Boolean = false,
    peerReady: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var mode by remember { mutableStateOf(MultiplayerRewards.Mode.RANKED) }
    var levelCap by remember { mutableStateOf(50) }

    Box(modifier.fillMaxSize()) {
        AetherArenaBackdrop(Modifier.fillMaxSize())

        // Top status indicator
        Box(Modifier.align(Alignment.TopEnd).padding(12.dp)) {
            MultiplayerStatusIndicator()
        }

        Column(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(14.dp))
                .background(AetherColors.Obsidian.copy(alpha = 0.88f))
                .border(2.dp, AetherColors.GoldCore, RoundedCornerShape(14.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "AETHER ARENA — LOBBY",
                color = AetherColors.GoldBright,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))

            // Trainer cards side-by-side
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrainerSlot(
                    label = "YOU",
                    name = selfDisplayName,
                    stats = selfStats,
                    money = selfMoney,
                    ready = iAmReady,
                    modifier = Modifier.weight(1f),
                )
                TrainerSlot(
                    label = "OPPONENT",
                    name = peerDisplayName,
                    stats = peerStats,
                    money = peerMoney,
                    ready = peerReady,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(4.dp))

            // Match settings
            Text("MATCH SETTINGS", color = AetherColors.GoldBright, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ModePill("Ranked", mode == MultiplayerRewards.Mode.RANKED) { mode = MultiplayerRewards.Mode.RANKED }
                ModePill("Casual", mode == MultiplayerRewards.Mode.CASUAL) { mode = MultiplayerRewards.Mode.CASUAL }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(10, 30, 50, 100).forEach { cap ->
                    ModePill("Lv ≤ $cap", levelCap == cap) { levelCap = cap }
                }
            }
            if (mode == MultiplayerRewards.Mode.RANKED) {
                Text(
                    "Loser pays ≥ \$${MultiplayerRewards.MIN_POT}, debt floor -\$5,000",
                    color = AetherColors.MutedText, fontSize = 10.sp,
                )
            } else {
                Text(
                    "Casual — no money, no XP, just a sparring match.",
                    color = AetherColors.MutedText, fontSize = 10.sp,
                )
            }

            Spacer(Modifier.height(6.dp))

            // Ready / Decline
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (iAmReady) androidx.compose.ui.graphics.SolidColor(AetherColors.GoldCore)
                            else AetherColors.GoldGradient
                        )
                        .clickable(enabled = !iAmReady) { onReady(mode, levelCap) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (iAmReady) "WAITING FOR ${peerDisplayName.uppercase()}…" else "READY",
                        color = AetherColors.Obsidian, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    )
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AetherColors.Slate)
                        .border(1.dp, AetherColors.WarningRed, RoundedCornerShape(8.dp))
                        .clickable(onClick = onDecline)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Text("DECLINE", color = AetherColors.WarningRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TrainerSlot(
    label: String,
    name: String,
    stats: MultiplayerStats?,
    money: Int?,
    ready: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AetherColors.Slate)
            .border(
                width = if (ready) 2.dp else 1.dp,
                color = if (ready) AetherColors.GoldBright else AetherColors.SlateLight,
                shape = RoundedCornerShape(10.dp),
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, color = AetherColors.GoldBright, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(name, color = AetherColors.ParchmentText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        if (stats != null) {
            Text("${stats.wins}W / ${stats.losses}L · ${(stats.winRate * 100).toInt()}%",
                color = AetherColors.MutedText, fontSize = 10.sp)
        } else {
            Text("…", color = AetherColors.MutedText, fontSize = 10.sp)
        }
        if (money != null) {
            val moneyColor = if (money < 0) AetherColors.WarningRed else AetherColors.GoldBright
            Text("\$$money", color = moneyColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        if (ready) {
            Spacer(Modifier.height(2.dp))
            Text("✓ READY", color = AetherColors.GoldBright, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ModePill(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) AetherColors.GoldCore else AetherColors.Slate)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            label,
            color = if (active) AetherColors.Obsidian else AetherColors.ParchmentText,
            fontSize = 10.sp, fontWeight = FontWeight.Bold,
        )
    }
}
