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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherbound.game.core.data.SaveGame
import com.aetherbound.game.core.data.SaveGameIO
import com.aetherbound.game.render.theme.AetherColors
import java.io.File
import java.text.DateFormat
import java.util.Date

/**
 * Save/Load menu. Lists all saved slots with metadata and lets the player
 * save the current game state, load an existing slot, or delete one.
 *
 * @param mode SAVE writes [currentSave] into the chosen slot;
 *             LOAD loads the chosen slot and dispatches via [onLoaded].
 */
@Composable
fun SaveLoadMenu(
    mode: SaveLoadMode,
    currentSave: SaveGame? = null,
    onBack: () -> Unit,
    onLoaded: (SaveGame) -> Unit = {},
    onSaved: (slot: Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    var slots by remember { mutableStateOf<List<SlotEntry>>(emptyList()) }
    var refreshTick by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTick) {
        slots = (0..MAX_SLOTS - 1).map { i ->
            val save = SaveGameIO.load(ctx, i)
            SlotEntry(
                slot = i,
                save = save,
                lastModifiedMs = if (save != null) {
                    File(File(ctx.filesDir, "aetherbound"), "save_$i.json").lastModified()
                } else 0L,
            )
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
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "back", tint = AetherColors.GoldBright)
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (mode == SaveLoadMode.SAVE) "SAVE GAME" else "LOAD GAME",
                color = AetherColors.GoldBright,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(slots, key = { it.slot }) { entry ->
                SlotRow(
                    entry = entry,
                    mode = mode,
                    onClick = {
                        when (mode) {
                            SaveLoadMode.SAVE -> {
                                if (currentSave != null) {
                                    if (SaveGameIO.save(ctx, entry.slot, currentSave)) {
                                        refreshTick++
                                        onSaved(entry.slot)
                                    }
                                }
                            }
                            SaveLoadMode.LOAD -> {
                                entry.save?.let(onLoaded)
                            }
                        }
                    },
                    onDelete = {
                        SaveGameIO.deleteSlot(ctx, entry.slot)
                        refreshTick++
                    },
                )
            }
        }
    }
}

enum class SaveLoadMode { SAVE, LOAD }

private data class SlotEntry(
    val slot: Int,
    val save: SaveGame?,
    val lastModifiedMs: Long,
)

@Composable
private fun SlotRow(entry: SlotEntry, mode: SaveLoadMode, onClick: () -> Unit, onDelete: () -> Unit) {
    val isEmpty = entry.save == null
    val canClick = mode == SaveLoadMode.SAVE || !isEmpty

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AetherColors.Slate)
            .border(1.dp, AetherColors.SlateLight, RoundedCornerShape(8.dp))
            .clickable(enabled = canClick, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Slot number badge
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(AetherColors.GoldGradient)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = "${entry.slot + 1}",
                color = AetherColors.Obsidian,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            if (entry.save == null) {
                Text(
                    text = "— empty slot —",
                    color = AetherColors.MutedText,
                    fontSize = 14.sp,
                )
            } else {
                val s = entry.save
                Text(
                    text = s.playerName,
                    color = AetherColors.ParchmentText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Party ${s.party.members.size}/6 · ${'$'}${s.inventory.money} · ${formatPlaytime(s.playtimeSec)}",
                    color = AetherColors.MutedText,
                    fontSize = 11.sp,
                )
                if (s.currentMap.isNotEmpty()) {
                    Text(
                        text = s.currentMap.substringAfterLast('/').removeSuffix(".tmx"),
                        color = AetherColors.GoldBright,
                        fontSize = 11.sp,
                    )
                }
                Text(
                    text = DateFormat.getDateTimeInstance().format(Date(entry.lastModifiedMs)),
                    color = AetherColors.MutedText,
                    fontSize = 10.sp,
                )
            }
        }

        if (entry.save != null) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "delete", tint = AetherColors.WarningRed)
            }
        }
    }
}

private fun formatPlaytime(sec: Long): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    return "${h}h ${m}m"
}

private const val MAX_SLOTS = 5
