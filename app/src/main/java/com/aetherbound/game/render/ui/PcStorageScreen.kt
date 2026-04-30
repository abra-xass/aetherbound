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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherbound.game.core.data.Party
import com.aetherbound.game.core.data.PcStorage
import com.aetherbound.game.render.theme.AetherColors
import com.aetherbound.game.render.theme.aspectColors

/**
 * PC Storage screen — Pokémon-style box system.
 *
 * Top: tabs to switch between boxes (max 30 slots each).
 * Body: 5×6 grid of stored Echoforms with mini-sprite + name + level.
 * Bottom: actions on selected slot — **Withdraw** (move to party if room),
 * **Release** (permanent removal with confirm), **Move** (reserved for future
 * box-to-box swap).
 *
 * The screen is read-only over party state — caller commits via callbacks.
 */
@Composable
fun PcStorageScreen(
    storage: PcStorage,
    party: Party,
    onBack: () -> Unit,
    onWithdraw: (boxIndex: Int, slotIndex: Int) -> Unit,
    onRelease: (boxIndex: Int, slotIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var boxIndex by remember { mutableIntStateOf(0) }
    var selectedSlot by remember { mutableStateOf<Int?>(null) }
    var confirmRelease by remember { mutableStateOf(false) }

    val activeBox = storage.boxes.getOrNull(boxIndex) ?: storage.boxes.firstOrNull()

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
            Text("PC STORAGE", color = AetherColors.GoldBright, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                "${storage.totalCount} stored · party ${party.members.size}/6",
                color = AetherColors.MutedText,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.height(8.dp))

        // Box tabs
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            storage.boxes.forEachIndexed { i, box ->
                val active = i == boxIndex
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (active) AetherColors.GoldCore else AetherColors.Slate)
                        .clickable { boxIndex = i; selectedSlot = null }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        "${box.name} (${box.slots.size})",
                        color = if (active) AetherColors.Obsidian else AetherColors.ParchmentText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // Slot grid
        if (activeBox == null || activeBox.slots.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "This box is empty.\nCatch more Echoforms with a full party to store them here.",
                    color = AetherColors.MutedText,
                    fontSize = 13.sp,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
            ) {
                itemsIndexed(activeBox.slots) { idx, mon ->
                    val isSelected = idx == selectedSlot
                    val (primary, _) = aspectColors(mon.species.primaryAspect)
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) primary.copy(alpha = 0.30f) else AetherColors.Slate)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) AetherColors.GoldBright else AetherColors.SlateLight,
                                shape = RoundedCornerShape(6.dp),
                            )
                            .clickable { selectedSlot = idx; confirmRelease = false }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.size(56.dp).clip(RoundedCornerShape(4.dp)).background(AetherColors.Onyx)) {
                            EchoformSpriteImage(
                                slug = mon.species.id,
                                variant = EchoformSpriteVariant.FRONT,
                                modifier = Modifier.matchParentSize(),
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            mon.species.name,
                            color = AetherColors.ParchmentText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Lv.${mon.level}",
                            color = AetherColors.GoldBright,
                            fontSize = 9.sp,
                        )
                    }
                }
            }
        }

        // Action footer for selected slot
        val sel = selectedSlot
        if (sel != null && activeBox != null && sel in activeBox.slots.indices) {
            val mon = activeBox.slots[sel]
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AetherColors.Slate)
                    .border(1.dp, AetherColors.GoldCore, RoundedCornerShape(8.dp))
                    .padding(12.dp),
            ) {
                Column {
                    Text(
                        "${mon.species.name} · Lv.${mon.level} · HP ${mon.currentVigor}/${mon.maxVigor}",
                        color = AetherColors.ParchmentText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionPill(
                            label = if (party.isFull) "Party full" else "Withdraw",
                            color = if (party.isFull) AetherColors.MutedText else AetherColors.GoldBright,
                            enabled = !party.isFull,
                        ) { onWithdraw(boxIndex, sel); selectedSlot = null }

                        if (!confirmRelease) {
                            ActionPill("Release…", AetherColors.WarningRed, true) {
                                confirmRelease = true
                            }
                        } else {
                            ActionPill("Confirm release", AetherColors.WarningRed, true) {
                                onRelease(boxIndex, sel)
                                confirmRelease = false
                                selectedSlot = null
                            }
                            ActionPill("Cancel", AetherColors.MutedText, true) {
                                confirmRelease = false
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionPill(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (enabled) AetherColors.Onyx else AetherColors.ObsidianDeep)
            .border(1.dp, color.copy(alpha = if (enabled) 0.7f else 0.3f), RoundedCornerShape(6.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
