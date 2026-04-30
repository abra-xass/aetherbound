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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherbound.game.core.data.UpdateChecker
import com.aetherbound.game.render.theme.AetherColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * One-tap "Check for updates" screen. Hits GitHub Releases, shows
 * version-info, downloads APK with progress, hands to system installer.
 *
 * The UI walks a tiny state machine:
 *   Idle           → user taps "Check"
 *   Checking       → spinner
 *   UpToDate       → green badge, "you're current"
 *   UpdateReady    → version info + release notes + DOWNLOAD button
 *   Downloading    → progress bar 0-100%
 *   InstallReady   → "Tap to install" → fires Android installer intent
 *   Failed         → red badge with message
 */
@Composable
fun UpdatePromptScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<UiState>(UiState.Idle) }
    var progress by remember { mutableFloatStateOf(0f) }

    // Auto-check once when the screen opens.
    LaunchedEffect(Unit) {
        if (state is UiState.Idle) {
            state = UiState.Checking
            val result = withContext(Dispatchers.IO) { UpdateChecker.check(ctx) }
            state = when (result) {
                is UpdateChecker.CheckResult.UpToDate -> UiState.UpToDate(result.installed)
                is UpdateChecker.CheckResult.UpdateAvailable ->
                    UiState.UpdateReady(result.installed, result.latest)
                is UpdateChecker.CheckResult.Error -> UiState.Failed(result.message)
            }
        }
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
                "UPDATES",
                color = AetherColors.GoldBright,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            BackChip("Close", onBack)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Pulled directly from github.com/abra-xass/Aetherbound — no browser detour.",
            color = AetherColors.MutedText,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(16.dp))

        when (val s = state) {
            UiState.Idle, UiState.Checking -> StatusBox(
                title = "Checking GitHub…",
                subtitle = "https://github.com/abra-xass/Aetherbound",
                color = AetherColors.GoldBright,
            )
            is UiState.UpToDate -> StatusBox(
                title = "Up to date",
                subtitle = "Installed version: ${s.installed}",
                color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
            )
            is UiState.UpdateReady -> UpdateReadyCard(
                installed = s.installed,
                info = s.latest,
                onDownload = {
                    state = UiState.Downloading(s.latest, 0f)
                    scope.launch {
                        val file = withContext(Dispatchers.IO) {
                            UpdateChecker.downloadApk(ctx, s.latest) { p ->
                                progress = p
                                state = UiState.Downloading(s.latest, p)
                            }
                        }
                        state = if (file != null) UiState.InstallReady(s.latest, file)
                                else UiState.Failed("Download failed")
                    }
                },
            )
            is UiState.Downloading -> DownloadingCard(s.info, s.progress)
            is UiState.InstallReady -> InstallReadyCard(s.info) {
                UpdateChecker.startInstall(ctx, s.file)
            }
            is UiState.Failed -> StatusBox(
                title = "Update failed",
                subtitle = s.message,
                color = AetherColors.WarningRed,
            )
        }
    }
}

private sealed class UiState {
    object Idle : UiState()
    object Checking : UiState()
    data class UpToDate(val installed: String) : UiState()
    data class UpdateReady(val installed: String, val latest: UpdateChecker.ReleaseInfo) : UiState()
    data class Downloading(val info: UpdateChecker.ReleaseInfo, val progress: Float) : UiState()
    data class InstallReady(val info: UpdateChecker.ReleaseInfo, val file: java.io.File) : UiState()
    data class Failed(val message: String) : UiState()
}

@Composable
private fun BackChip(label: String, onBack: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AetherColors.Slate)
            .clickable(onClick = onBack)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(label, color = AetherColors.GoldBright, fontSize = 13.sp)
    }
}

@Composable
private fun StatusBox(title: String, subtitle: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AetherColors.Slate)
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(16.dp),
    ) {
        Column {
            Text(title, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = AetherColors.MutedText, fontSize = 11.sp)
        }
    }
}

@Composable
private fun UpdateReadyCard(
    installed: String,
    info: UpdateChecker.ReleaseInfo,
    onDownload: () -> Unit,
) {
    val scroll = rememberScrollState()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AetherColors.Slate)
            .border(2.dp, AetherColors.GoldBright, RoundedCornerShape(10.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row {
            Column(Modifier.weight(1f)) {
                Text(
                    "Update available",
                    color = AetherColors.GoldBright,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "$installed → ${info.versionLabel}",
                    color = AetherColors.ParchmentText,
                    fontSize = 12.sp,
                )
            }
            Text(
                "%.1f MB".format(info.apkSize / 1024f / 1024f),
                color = AetherColors.GoldBright,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        if (info.notes.isNotBlank()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(AetherColors.ObsidianDeep)
                    .padding(10.dp)
                    .verticalScroll(scroll),
            ) {
                Text(
                    info.notes,
                    color = AetherColors.MutedText,
                    fontSize = 11.sp,
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(AetherColors.GoldGradient)
                .clickable(onClick = onDownload)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "DOWNLOAD ${info.versionLabel}",
                color = AetherColors.Obsidian,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun DownloadingCard(info: UpdateChecker.ReleaseInfo, progress: Float) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AetherColors.Slate)
            .border(1.dp, AetherColors.GoldBright, RoundedCornerShape(10.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Downloading ${info.versionLabel}",
            color = AetherColors.GoldBright,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = AetherColors.GoldBright,
            trackColor = AetherColors.ObsidianDeep,
        )
        Text(
            "${(progress * 100).toInt()}% · ${"%.1f".format(info.apkSize * progress / 1024f / 1024f)} MB / ${"%.1f".format(info.apkSize / 1024f / 1024f)} MB",
            color = AetherColors.MutedText,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun InstallReadyCard(info: UpdateChecker.ReleaseInfo, onInstall: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AetherColors.Slate)
            .border(2.dp, AetherColors.GoldBright, RoundedCornerShape(10.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Ready to install",
            color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Aetherbound ${info.versionLabel} downloaded.",
            color = AetherColors.ParchmentText,
            fontSize = 12.sp,
        )
        Text(
            "Android will ask you to confirm the install once. Future updates after that are silent.",
            color = AetherColors.MutedText,
            fontSize = 11.sp,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(AetherColors.GoldGradient)
                .clickable(onClick = onInstall)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "INSTALL ${info.versionLabel}",
                color = AetherColors.Obsidian,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
