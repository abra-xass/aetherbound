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
import com.aetherbound.game.core.AspectAffinity
import com.aetherbound.game.core.EchoformInstance
import com.aetherbound.game.core.Technique
import com.aetherbound.game.render.theme.AetherColors
import com.aetherbound.game.render.theme.aspectColors

@Composable
fun MoveSelector(
    techniques: List<Technique>,
    enabled: Boolean,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    target: EchoformInstance? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AetherColors.Onyx.copy(alpha = 0.85f))
            .border(
                width = 1.dp,
                brush = AetherColors.GoldGradient,
                shape = RoundedCornerShape(14.dp),
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        techniques.chunked(2).forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEachIndexed { colIndex, tech ->
                    val idx = rowIndex * 2 + colIndex
                    val mult = target?.let {
                        AspectAffinity.multiplier(tech.aspect, it.species.primaryAspect, it.species.secondaryAspect)
                    } ?: 1.0
                    MoveButton(
                        tech = tech,
                        effectiveness = mult,
                        enabled = enabled,
                        onPick = { onPick(idx) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MoveButton(
    tech: Technique,
    effectiveness: Double,
    enabled: Boolean,
    onPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (cPrimary, cSecondary) = aspectColors(tech.aspect)
    val (effLabel, effColor) = when {
        effectiveness >= 2.0 -> "SUPER" to androidx.compose.ui.graphics.Color(0xFF59B66B)
        effectiveness >= 1.5 -> "STRONG" to androidx.compose.ui.graphics.Color(0xFF8FCBE1)
        effectiveness <= 0.5 -> "WEAK" to AetherColors.WarningRed
        effectiveness < 1.0 -> "RESISTED" to AetherColors.MutedText
        else -> null to null
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                brush = Brush.horizontalGradient(
                    listOf(cPrimary.copy(alpha = if (enabled) 0.42f else 0.18f), AetherColors.Slate)
                )
            )
            .border(1.dp, cSecondary.copy(alpha = if (enabled) 0.7f else 0.3f), RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) { onPick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    tech.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = AetherColors.ParchmentText,
                )
                if (effLabel != null && effColor != null) {
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(effColor.copy(alpha = 0.85f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            effLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = AetherColors.Obsidian,
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    tech.aspect.name.lowercase().replaceFirstChar { it.titlecase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = cSecondary,
                )
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                Text(
                    "Pwr ${tech.power}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherColors.MutedText,
                )
            }
        }
    }
}
