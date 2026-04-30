package com.aetherbound.game.render.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherbound.game.core.data.Gender
import com.aetherbound.game.core.data.Inventory
import com.aetherbound.game.core.data.PlayerProgress
import com.aetherbound.game.render.theme.AetherColors

/**
 * Single-page consolidated profile of the player. Shows:
 *   - Name, Gender, Spielzeit
 *   - Geld
 *   - #Echoforms gefangen / gesehen
 *   - #Trainer besiegt (Badges)
 *   - Besuchte Städte
 *   - Surf / Bike / Fly Status
 *
 * All read-only — pure visualization. Closed via tap-anywhere or back-
 * button.
 */
@Composable
fun ProfileScreen(
    progress: PlayerProgress,
    inventory: Inventory,
    totalSpeciesCount: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AetherColors.ObsidianDeep.copy(alpha = 0.96f))
            .clickable(onClick = onClose),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "PROFIL",
                color = AetherColors.GoldHighlight,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))

            // Name + gender + playtime row
            ProfileCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GenderAvatar(progress.playerGender)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            progress.playerName.ifBlank { "Reisender" },
                            color = AetherColors.GoldBright,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Spielzeit · ${formatPlaytime(progress.playtimeSec)}",
                            color = AetherColors.MutedText,
                            fontSize = 11.sp,
                        )
                        Text(
                            (progress.playerGender?.displayDe ?: "nichtbinär"),
                            color = AetherColors.GoldDeep,
                            fontSize = 10.sp,
                        )
                    }
                }
            }

            // Stats grid
            ProfileCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatRow("💰 Geld", "$${inventory.money}")
                    StatRow(
                        "📖 Aetherdex",
                        "${progress.caughtSlugs.size} gefangen · ${progress.seenSlugs.size} gesehen",
                    )
                    StatRow(
                        "🏆 Trainer besiegt",
                        "${progress.badgesEarned.size}",
                    )
                    StatRow(
                        "🗺 Städte besucht",
                        "${progress.visitedTowns.size} / ${
                            com.aetherbound.game.core.data.TownRegistry.all.size
                        }",
                    )
                    StatRow(
                        "📊 Komplettierung",
                        "${progress.completionPercent(totalSpeciesCount)}%",
                    )
                }
            }

            // Abilities row
            ProfileCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "FÄHIGKEITEN",
                        color = AetherColors.GoldCore,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    AbilityRow("🌊 Surf", progress.hasSurf)
                    AbilityRow("🚲 Fahrrad", inventory.has("bicycle"))
                    AbilityRow("🪶 Aether-Flug", progress.visitedTowns.isNotEmpty())
                }
            }

            // Badges list (if any)
            if (progress.badgesEarned.isNotEmpty()) {
                ProfileCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "BESIEGTE TRAINER",
                            color = AetherColors.GoldCore,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        for (badge in progress.badgesEarned.take(8)) {
                            Text(
                                "🏆 $badge",
                                color = AetherColors.ParchmentText,
                                fontSize = 11.sp,
                            )
                        }
                        if (progress.badgesEarned.size > 8) {
                            Text(
                                "… und ${progress.badgesEarned.size - 8} weitere",
                                color = AetherColors.MutedText,
                                fontSize = 10.sp,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "tippe, um zu schließen",
                color = AetherColors.MutedText.copy(alpha = 0.6f),
                fontSize = 9.sp,
            )
        }
    }
}

@Composable
private fun ProfileCard(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth(0.92f)
            .clip(RoundedCornerShape(10.dp))
            .background(AetherColors.Obsidian)
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(AetherColors.GoldDeep, AetherColors.GoldDeep.copy(alpha = 0.4f)),
                ),
                RoundedCornerShape(10.dp),
            )
            .padding(14.dp),
    ) { content() }
}

@Composable
private fun GenderAvatar(gender: Gender?) {
    val emoji = when (gender) {
        Gender.MASCULINE -> "♂"
        Gender.FEMININE -> "♀"
        Gender.NEUTRAL, null -> "✶"
    }
    Box(
        Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(AetherColors.Onyx)
            .border(
                2.dp,
                Brush.verticalGradient(
                    listOf(AetherColors.GoldBright, AetherColors.GoldDeep),
                ),
                RoundedCornerShape(27.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, color = AetherColors.GoldBright, fontSize = 22.sp)
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = AetherColors.ParchmentText, fontSize = 12.sp)
        Text(value, color = AetherColors.GoldBright, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AbilityRow(label: String, unlocked: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            color = if (unlocked) AetherColors.GoldBright else AetherColors.MutedText,
            fontSize = 12.sp,
        )
        Text(
            if (unlocked) "freigeschaltet" else "—",
            color = if (unlocked) AetherColors.GoldDeep else AetherColors.MutedText,
            fontSize = 11.sp,
        )
    }
}

private fun formatPlaytime(sec: Long): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m"
        else -> "${sec}s"
    }
}
