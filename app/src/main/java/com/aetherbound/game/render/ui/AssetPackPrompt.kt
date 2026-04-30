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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherbound.game.core.data.AssetPackDownloader
import com.aetherbound.game.core.data.AssetPackDownloader.Pack
import com.aetherbound.game.render.theme.AetherColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * First-run / on-demand prompt for downloading the heavy asset packs.
 *
 *   Audio (~150 MB)    — 192 SFX + 111 BGM tracks
 *   Graphics (~40 MB)  — full move-FX frame sets for all 274 techniques
 *   Maps (~25 MB)      — all 235 imported Tuxemon maps
 *
 * The core APK ships with a slim subset of each (one BGM, basic SFX, 3
 * pilot maps, hand-crafted move FX). Players can keep the slim experience
 * or upgrade to full content when on Wi-Fi.
 *
 * Tap a pack to start its download. State persists across launches via
 * [AssetPackDownloader.isInstalled].
 */
@Composable
fun AssetPackPrompt(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var manifest by remember { mutableStateOf<AssetPackDownloader.Manifest?>(null) }
    val statePerPack = remember {
        Pack.values().associateWith { mutableStateOf(initialStateOf(ctx, it)) }
    }

    LaunchedEffect(Unit) {
        manifest = withContext(Dispatchers.IO) { AssetPackDownloader.fetchManifest() }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(AetherColors.ObsidianDeep, AetherColors.Obsidian, AetherColors.Onyx)
                )
            )
            .padding(16.dp),
    ) {
        Row {
            Text(
                "ASSET PACKS",
                color = AetherColors.GoldBright,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AetherColors.Slate)
                    .clickable(onClick = onClose)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text("Close", color = AetherColors.GoldBright, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Download optional content for the full experience. Core game works without these.",
            color = AetherColors.MutedText,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(16.dp))

        for (pack in Pack.values()) {
            val state = statePerPack[pack]!!
            val entry = manifest?.packs?.get(pack.id)
            PackRow(
                pack = pack,
                sizeBytes = entry?.size,
                state = state.value,
                onTap = {
                    if (state.value is PackState.Idle && entry != null) {
                        state.value = PackState.Downloading(0f)
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) {
                                AssetPackDownloader.downloadAndExtract(
                                    ctx = ctx,
                                    pack = pack,
                                    manifest = manifest!!,
                                    onProgress = { p ->
                                        state.value = PackState.Downloading(p)
                                    },
                                )
                            }
                            state.value = if (ok) PackState.Installed else PackState.Failed
                        }
                    } else if (state.value == PackState.Installed) {
                        AssetPackDownloader.uninstall(ctx, pack)
                        state.value = PackState.Idle
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun initialStateOf(ctx: android.content.Context, pack: Pack): PackState =
    if (AssetPackDownloader.isInstalled(ctx, pack)) PackState.Installed else PackState.Idle

private sealed class PackState {
    object Idle : PackState()
    data class Downloading(val progress: Float) : PackState()
    object Installed : PackState()
    object Failed : PackState()
}

@Composable
private fun PackRow(
    pack: Pack,
    sizeBytes: Long?,
    state: PackState,
    onTap: () -> Unit,
) {
    val (label, sub) = when (pack) {
        Pack.AUDIO    -> "Audio Pack" to "192 SFX + 111 BGM tracks"
        Pack.GRAPHICS -> "Graphics Pack" to "Move animations for all 274 techniques"
        Pack.MAPS     -> "Maps Pack" to "All 235 imported Tuxemon maps"
    }
    val sizeText = sizeBytes?.let { "%.1f MB".format(it / 1024f / 1024f) } ?: "?"
    val (statusText, statusColor) = when (state) {
        PackState.Idle -> "DOWNLOAD" to AetherColors.GoldBright
        is PackState.Downloading -> "${(state.progress * 100).toInt()}%" to AetherColors.GoldBright
        PackState.Installed -> "INSTALLED · TAP TO REMOVE" to Color(0xFF4CAF50)
        PackState.Failed -> "FAILED · TAP TO RETRY" to AetherColors.WarningRed
    }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AetherColors.Slate)
            .border(1.dp, AetherColors.SlateLight, RoundedCornerShape(10.dp))
            .clickable(onClick = onTap)
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row {
                Text(label, color = AetherColors.ParchmentText, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(sizeText, color = AetherColors.GoldBright, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Text(sub, color = AetherColors.MutedText, fontSize = 11.sp)
            if (state is PackState.Downloading) {
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = AetherColors.GoldBright,
                    trackColor = AetherColors.ObsidianDeep,
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(statusText, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}
