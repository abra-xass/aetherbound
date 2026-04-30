package com.aetherbound.game.render.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherbound.game.render.theme.AetherColors

/**
 * Compact stack of world-mode toggles (Surf / Bike / Fly) on the bottom-
 * right of [com.aetherbound.game.render.world.TuxemonWorldScene]. Each
 * toggle is only rendered when its corresponding ability is unlocked
 * in [com.aetherbound.game.core.data.PlayerProgress].
 *
 * Visual: small obsidian chip with metallic-gold outline. When active,
 * background flips to gold, label turns dark.
 */
@Composable
fun WorldActionToggles(
    hasSurf: Boolean,
    surfActive: Boolean,
    onSurfToggle: () -> Unit,
    hasBicycle: Boolean,
    bikeActive: Boolean,
    onBikeToggle: () -> Unit,
    canFly: Boolean,
    onFlyTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (hasSurf) {
            ToggleChip(
                label = "Surf",
                emoji = "🌊",
                active = surfActive,
                onTap = onSurfToggle,
            )
        }
        if (hasBicycle) {
            ToggleChip(
                label = "Bike",
                emoji = "🚲",
                active = bikeActive,
                onTap = onBikeToggle,
            )
        }
        if (canFly) {
            ToggleChip(
                label = "Fly",
                emoji = "🪶",
                active = false,
                onTap = onFlyTap,
            )
        }
    }
}

@Composable
private fun ToggleChip(
    label: String,
    emoji: String,
    active: Boolean,
    onTap: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (active) AetherColors.GoldDeep
                else AetherColors.Obsidian.copy(alpha = 0.85f),
            )
            .border(
                1.dp,
                if (active) AetherColors.GoldBright else AetherColors.GoldDeep.copy(alpha = 0.5f),
                RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onTap)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(emoji, fontSize = 14.sp)
        Text(
            label,
            color = if (active) AetherColors.Onyx else AetherColors.GoldBright,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
