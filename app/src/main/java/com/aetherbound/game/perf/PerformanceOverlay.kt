package com.aetherbound.game.perf

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aetherbound.game.render.theme.AetherColors
import com.aetherbound.game.render.theme.QualityPreset

@Composable
fun PerformanceOverlay(
    quality: QualityPreset,
    onCycleQuality: () -> Unit,
    activeParticles: Int,
    modifier: Modifier = Modifier,
) {
    var fps by remember { mutableFloatStateOf(0f) }
    var lastNs by remember { mutableLongStateOf(0L) }
    var smoothed by remember { mutableFloatStateOf(60f) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { now ->
                if (lastNs != 0L) {
                    val dt = (now - lastNs) / 1_000_000f
                    if (dt > 0f) {
                        val instant = 1000f / dt
                        smoothed = smoothed * 0.92f + instant * 0.08f
                        fps = smoothed
                    }
                }
                lastNs = now
            }
        }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AetherColors.ObsidianDeep.copy(alpha = 0.78f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "${fps.toInt()} fps",
            color = if (fps >= 55f) AetherColors.GoldBright
            else if (fps >= 40f) AetherColors.GoldCore
            else AetherColors.WarningRed,
            style = MaterialTheme.typography.labelLarge,
        )
        Text("· $activeParticles px", color = AetherColors.MutedText, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.size(4.dp))
        Row(
            Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(AetherColors.Slate)
                .clickable { onCycleQuality() }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(quality.name, color = AetherColors.ParchmentText, style = MaterialTheme.typography.labelLarge)
        }
    }
}

