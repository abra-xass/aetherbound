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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherbound.game.core.data.TownRegistry
import com.aetherbound.game.render.theme.AetherColors

/**
 * Fast-travel destination picker. Shown as an overlay when the player
 * taps the Fly button in the world action chips. Lists every town the
 * player has personally visited at least once. Tapping a town warps
 * the player straight there (handled by the activity via [onPick]).
 *
 * If the player hasn't visited any towns yet, the menu shows a hint
 * line directing them to walk into a town first.
 */
@Composable
fun FlyMenu(
    visitedTowns: Set<String>,
    onPick: (TownRegistry.Town) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AetherColors.ObsidianDeep.copy(alpha = 0.92f))
            .clickable(onClick = onCancel),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(12.dp))
                .background(AetherColors.Obsidian)
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(AetherColors.GoldBright, AetherColors.GoldDeep),
                    ),
                    RoundedCornerShape(12.dp),
                )
                .padding(16.dp)
                .clickable(enabled = false) { /* swallow taps inside the panel */ },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "🪶  AETHER-FLUG",
                color = AetherColors.GoldBright,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Wähle ein Ziel — du kannst nur zu Orten reisen, die du schon einmal besucht hast.",
                color = AetherColors.MutedText,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))

            val available = TownRegistry.all.filter { it.mapAssetPath in visitedTowns }
            if (available.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AetherColors.Onyx.copy(alpha = 0.5f))
                        .padding(16.dp),
                ) {
                    Text(
                        "Du hast noch keine Stadt besucht.\nBesuche eine zu Fuß, dann kannst du dahin fliegen.",
                        color = AetherColors.ParchmentText,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                for (town in available) {
                    TownDestinationRow(town = town, onTap = { onPick(town) })
                }
            }

            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AetherColors.Slate)
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                    .align(Alignment.CenterHorizontally),
            ) {
                Text("Abbrechen", color = AetherColors.MutedText, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun TownDestinationRow(
    town: TownRegistry.Town,
    onTap: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AetherColors.Onyx)
            .clickable(onClick = onTap)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("📍", fontSize = 16.sp)
        Column {
            Text(
                town.displayName,
                color = AetherColors.GoldBright,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                town.mapAssetPath.substringAfterLast('/').removeSuffix(".tmx"),
                color = AetherColors.MutedText,
                fontSize = 9.sp,
            )
        }
    }
}
