package com.aetherbound.game.render.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.aetherbound.game.core.BattleMenu
import com.aetherbound.game.render.theme.AetherColors

@Composable
fun ActionMenu(
    onPick: (BattleMenu) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AetherColors.Onyx.copy(alpha = 0.92f))
            .border(1.dp, AetherColors.GoldGradient, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionTile("Fight", "Choose a technique", BattleMenu.Fight, enabled, onPick, Modifier.weight(1f), AetherColors.GoldCore)
            ActionTile("Bag", "Use an item", BattleMenu.Bag, enabled, onPick, Modifier.weight(1f), AetherColors.GoldBright)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionTile("Switch", "Change Echoform", BattleMenu.Switch, enabled, onPick, Modifier.weight(1f), AetherColors.GoldCore)
            ActionTile("Capture", "Bind with a Prism", BattleMenu.Capture, enabled, onPick, Modifier.weight(1f), AetherColors.GoldBright)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionTile("Run", "Try to escape", BattleMenu.Run, enabled, onPick, Modifier.weight(1f), AetherColors.WarningRed)
        }
    }
}

@Composable
private fun ActionTile(
    title: String,
    subtitle: String,
    menu: BattleMenu,
    enabled: Boolean,
    onPick: (BattleMenu) -> Unit,
    modifier: Modifier,
    accent: androidx.compose.ui.graphics.Color,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(accent.copy(alpha = if (enabled) 0.36f else 0.14f), AetherColors.Slate)
                )
            )
            .border(1.dp, accent.copy(alpha = if (enabled) 0.7f else 0.30f), RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) { onPick(menu) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, color = AetherColors.ParchmentText)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = accent)
        }
    }
}

@Composable
fun BackChip(label: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AetherColors.Slate.copy(alpha = 0.85f))
            .clickable { onBack() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = AetherColors.MutedText, style = MaterialTheme.typography.labelMedium)
    }
}
