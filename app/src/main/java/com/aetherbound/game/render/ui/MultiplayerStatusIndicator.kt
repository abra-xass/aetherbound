package com.aetherbound.game.render.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherbound.game.core.data.Multiplayer
import com.aetherbound.game.core.data.MultiplayerBridge
import com.aetherbound.game.render.theme.AetherColors
import kotlinx.coroutines.delay

/**
 * Compact connection-state badge for the top-right of any multiplayer
 * scene. Polls the bridge every 4 seconds and shows three things:
 *
 *   🟢 CLEARNET  · 120ms       (rare; only when running outside Thot)
 *   🟡 TOR · 3 hops · 850ms     (the expected normal during Thot multiplayer)
 *   🔴 RECONNECTING            (circuit rebuild / push retry)
 *
 * The component pulses while reconnecting so the user understands the
 * UI isn't frozen — just waiting for the next Tor circuit.
 */
@Composable
fun MultiplayerStatusIndicator(
    modifier: Modifier = Modifier,
    pollIntervalMs: Long = 4_000,
) {
    var info by remember { mutableStateOf<MultiplayerBridge.ConnectionInfo?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            info = Multiplayer.bridge.connectionInfo()
            delay(pollIntervalMs)
        }
    }

    val current = info ?: MultiplayerBridge.ConnectionInfo(
        transport = MultiplayerBridge.NetworkTransport.OFFLINE,
        connected = false,
    )

    val (dotColor, label) = when {
        !current.connected -> AetherColors.WarningRed to "RECONNECTING"
        current.transport == MultiplayerBridge.NetworkTransport.TOR ->
            AetherColors.GoldBright to buildTorLabel(current)
        current.transport == MultiplayerBridge.NetworkTransport.CLEARNET ->
            Color(0xFF4CAF50) to buildClearLabel(current)
        else -> AetherColors.MutedText to "OFFLINE"
    }

    // Pulse alpha when reconnecting
    val pulseTarget = if (current.connected) 1f else 0.4f
    val pulseAlpha by animateFloatAsState(
        targetValue = pulseTarget,
        animationSpec = tween(durationMillis = 800),
        label = "pulse",
    )

    Row(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AetherColors.Obsidian.copy(alpha = 0.85f))
            .border(1.dp, AetherColors.SlateLight, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .alpha(pulseAlpha),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(dotColor),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = AetherColors.ParchmentText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun buildTorLabel(info: MultiplayerBridge.ConnectionInfo): String {
    val hops = info.torHops?.let { "$it hops" } ?: "TOR"
    val latency = info.latencyMs?.let { "${it}ms" } ?: ""
    return listOf("TOR", hops, latency).filter { it.isNotEmpty() }.joinToString(" · ")
}

private fun buildClearLabel(info: MultiplayerBridge.ConnectionInfo): String {
    val latency = info.latencyMs?.let { " · ${it}ms" } ?: ""
    return "CLEARNET$latency"
}
