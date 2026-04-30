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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.aetherbound.game.core.data.AudioSettings
import com.aetherbound.game.core.data.ControlSettings
import com.aetherbound.game.core.data.DayNightPhase
import com.aetherbound.game.core.data.PlayerProgress
import com.aetherbound.game.render.theme.AetherColors

@Composable
fun SettingsScreen(
    progress: PlayerProgress,
    onChange: (PlayerProgress) -> Unit,
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
            Text("SETTINGS", color = AetherColors.GoldBright, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))

        // ── AUDIO ───────────────────────────────────────────────
        Section("AUDIO")
        ToggleRow(
            label = "Music",
            checked = progress.audio.musicEnabled,
            onChecked = { onChange(progress.copy(audio = progress.audio.copy(musicEnabled = it))) },
        )
        SliderRow(
            label = "Music volume",
            value = progress.audio.musicVolume,
            onChange = { onChange(progress.copy(audio = progress.audio.copy(musicVolume = it))) },
        )
        ToggleRow(
            label = "Sound effects",
            checked = progress.audio.sfxEnabled,
            onChecked = { onChange(progress.copy(audio = progress.audio.copy(sfxEnabled = it))) },
        )
        SliderRow(
            label = "SFX volume",
            value = progress.audio.sfxVolume,
            onChange = { onChange(progress.copy(audio = progress.audio.copy(sfxVolume = it))) },
        )

        Spacer(Modifier.height(16.dp))

        // ── CONTROLS ────────────────────────────────────────────
        Section("CONTROLS")
        SliderRow(
            label = "Walk speed (lower = faster)",
            value = (progress.controls.stepDurationMs / 400f).coerceIn(0.2f, 1f),
            onChange = { v ->
                onChange(progress.copy(controls = progress.controls.copy(stepDurationMs = (v * 400).toInt().coerceIn(80, 400))))
            },
        )
        ToggleRow(
            label = "Hardware-back opens menu",
            checked = progress.controls.backOpensMenu,
            onChecked = { onChange(progress.copy(controls = progress.controls.copy(backOpensMenu = it))) },
        )

        Spacer(Modifier.height(16.dp))

        // ── AKKU / BATTERIE ────────────────────────────────────
        Section("AKKU")
        ToggleRow(
            label = "Kampfanimationen",
            checked = progress.controls.battleAnimationsEnabled,
            onChecked = {
                onChange(progress.copy(controls = progress.controls.copy(battleAnimationsEnabled = it)))
            },
        )
        Text(
            "Aus = Kämpfe lösen sich sofort auf, kein Visual-FX. Spart Akku.",
            color = AetherColors.MutedText, fontSize = 10.sp,
        )
        Spacer(Modifier.height(8.dp))
        ToggleRow(
            label = "Auto-Save (alle 10 Min)",
            checked = progress.controls.autoSaveEnabled,
            onChecked = {
                onChange(progress.copy(controls = progress.controls.copy(autoSaveEnabled = it)))
            },
        )
        Text(
            "Aus = nur manuell speichern. Empfohlen: an.",
            color = AetherColors.MutedText, fontSize = 10.sp,
        )

        Spacer(Modifier.height(16.dp))

        // ── DAY/NIGHT OVERRIDE ──────────────────────────────────
        Section("DAY/NIGHT")
        Text(
            "Override real-clock cycle (debug)",
            color = AetherColors.MutedText,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PhasePill(label = "AUTO", selected = progress.controls.forcedPhase == null) {
                onChange(progress.copy(controls = progress.controls.copy(forcedPhase = null)))
            }
            DayNightPhase.values().forEach { phase ->
                PhasePill(
                    label = phase.displayName,
                    selected = progress.controls.forcedPhase == phase,
                    onClick = {
                        onChange(progress.copy(controls = progress.controls.copy(forcedPhase = phase)))
                    },
                )
            }
        }
    }
}

@Composable
private fun Section(label: String) {
    Text(
        text = label,
        color = AetherColors.GoldBright,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = AetherColors.ParchmentText, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AetherColors.GoldBright,
                checkedTrackColor = AetherColors.GoldDeep,
                uncheckedThumbColor = AetherColors.MutedText,
                uncheckedTrackColor = AetherColors.Slate,
            ),
        )
    }
}

@Composable
private fun SliderRow(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Row {
            Text(label, color = AetherColors.ParchmentText, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text("${(value * 100).toInt()}%", color = AetherColors.GoldBright, fontSize = 12.sp)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            colors = SliderDefaults.colors(
                thumbColor = AetherColors.GoldBright,
                activeTrackColor = AetherColors.GoldCore,
                inactiveTrackColor = AetherColors.Slate,
            ),
        )
    }
}

@Composable
private fun PhasePill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) AetherColors.GoldCore else AetherColors.Slate)
            .border(
                width = 1.dp,
                color = if (selected) AetherColors.GoldBright else AetherColors.SlateLight,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            color = if (selected) AetherColors.Obsidian else AetherColors.ParchmentText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
