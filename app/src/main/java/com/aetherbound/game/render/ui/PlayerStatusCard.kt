package com.aetherbound.game.render.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherbound.game.core.data.Inventory
import com.aetherbound.game.core.data.Party
import com.aetherbound.game.core.data.PlayerProgress
import com.aetherbound.game.render.theme.AetherColors

/**
 * Trainer-card / status overview. Aggregates player meta-stats:
 * trainer name + portrait, money, party fill, badges, bestiary completion,
 * playtime.
 */
@Composable
fun PlayerStatusCard(
    progress: PlayerProgress,
    party: Party,
    inventory: Inventory,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(AetherColors.ObsidianDeep, AetherColors.Obsidian, AetherColors.Onyx)
                )
            )
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "back", tint = AetherColors.GoldBright)
            }
            Text("TRAINER CARD", color = AetherColors.GoldBright, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))

        // Big card
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AetherColors.Slate)
                .border(2.dp, AetherColors.GoldCore, RoundedCornerShape(12.dp))
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Portrait placeholder
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(36.dp))
                        .background(AetherColors.GoldGradient),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        progress.playerName.first().uppercase(),
                        color = AetherColors.Obsidian,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        progress.playerName,
                        color = AetherColors.GoldBright,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Aether Trainer",
                        color = AetherColors.MutedText,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${'$'}${inventory.money}",
                        color = AetherColors.GoldBright,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Stat grid (2 columns)
            StatGrid(
                items = listOf(
                    "Party"      to "${party.members.size}/6",
                    "Items"      to "${inventory.totalItems}",
                    "Bestiary"   to "${progress.caughtSlugs.size}/411 (${progress.completionPercent()}%)",
                    "Seen"       to "${progress.seenSlugs.size}",
                    "Badges"     to "${progress.badgesEarned.size}",
                    "Playtime"   to formatPlaytime(progress.playtimeSec),
                )
            )
        }

        Spacer(Modifier.height(16.dp))

        // Aether Battle Record (only if any matches played)
        val mp = progress.multiplayer
        if (mp.totalMatches > 0 || inventory.money < 0) {
            Text(
                "AETHER BATTLE RECORD",
                color = AetherColors.GoldBright,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AetherColors.Slate)
                    .border(1.dp, AetherColors.GoldCore, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StatGrid(items = listOf(
                    "Wins"      to "${mp.wins}",
                    "Losses"    to "${mp.losses}",
                    "Win-Rate"  to "${(mp.winRate * 100).toInt()}%",
                    "Streak"    to streakLabel(mp),
                    "Biggest Upset" to (if (mp.biggestUpset > 0) "+${mp.biggestUpset} levels" else "—"),
                    "Net Pot"   to formatMoney(mp.netPot),
                ))
                if (inventory.money < 0) {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(AetherColors.WarningRed.copy(alpha = 0.18f))
                            .border(1.dp, AetherColors.WarningRed, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            "DEBT  ${'$'}${inventory.money}  /  -${'$'}5000 floor",
                            color = AetherColors.WarningRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (mp.recentMatches.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Last ${mp.recentMatches.size} matches:",
                        color = AetherColors.MutedText, fontSize = 10.sp)
                    mp.recentMatches.take(5).forEach { rec ->
                        Text(
                            "${if (rec.won) "W" else "L"} vs ${rec.opponentDisplayName} · " +
                                "${rec.finalTurn} turns" +
                                (if (rec.playerSweep) " · sweep!" else ""),
                            color = if (rec.won) AetherColors.GoldBright else AetherColors.MutedText,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Badge ribbon
        if (progress.badgesEarned.isNotEmpty()) {
            Text(
                "BADGES",
                color = AetherColors.GoldBright,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                progress.badgesEarned.forEach { badge ->
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(AetherColors.GoldGradient)
                            .border(2.dp, AetherColors.GoldHighlight, RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            badge.first().uppercase(),
                            color = AetherColors.Obsidian,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatGrid(items: List<Pair<String, String>>) {
    val rows = items.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, value) ->
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AetherColors.Onyx)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Column {
                            Text(label, color = AetherColors.MutedText, fontSize = 10.sp)
                            Text(
                                value,
                                color = AetherColors.GoldBright,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private fun formatPlaytime(sec: Long): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    return "${h}h ${m}m"
}

private fun streakLabel(mp: com.aetherbound.game.core.data.MultiplayerStats): String = when {
    mp.currentStreak > 0 -> "+${mp.currentStreak}  (max +${mp.longestStreak})"
    mp.currentStreak < 0 -> "${mp.currentStreak}  (max +${mp.longestStreak})"
    else -> "—  (max +${mp.longestStreak})"
}

private fun formatMoney(amount: Int): String =
    if (amount < 0) "-${'$'}${-amount}" else "${'$'}${amount}"
