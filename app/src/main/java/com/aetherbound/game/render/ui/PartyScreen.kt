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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherbound.game.core.EchoformInstance
import com.aetherbound.game.core.StatKey
import com.aetherbound.game.core.data.Party
import com.aetherbound.game.render.theme.AetherColors
import com.aetherbound.game.render.theme.aspectColors

/**
 * Six-slot party screen. Tap a slot to set it as active or open detail.
 * Used by both world (out-of-battle) and battle (switch) flows.
 */
@Composable
fun PartyScreen(
    party: Party,
    onBack: () -> Unit,
    onMemberSelected: (index: Int) -> Unit = {},
    onDetail: ((index: Int) -> Unit)? = null,
    onSwitch: ((index: Int) -> Unit)? = null,
    /** When true, fainted members are dimmed and not selectable. */
    requireAlive: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

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
                text = "PARTY",
                color = AetherColors.GoldBright,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "${party.aliveCount}/${party.members.size} alive",
                color = AetherColors.MutedText,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.height(8.dp))

        if (party.members.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Your party is empty.\nCatch a wild Echoform to begin.",
                    color = AetherColors.MutedText,
                    fontSize = 14.sp,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(party.members) { index, member ->
                    val isActive = index == party.activeIndex
                    val isSelected = index == selectedIndex
                    val canSelect = !requireAlive || !member.isFainted
                    PartySlotRow(
                        member = member,
                        slotIndex = index,
                        isActive = isActive,
                        isSelected = isSelected,
                        enabled = canSelect,
                        showDetailIcon = onDetail != null,
                        onClick = {
                            if (canSelect) {
                                if (isSelected && onSwitch != null) onSwitch(index)
                                selectedIndex = index
                                onMemberSelected(index)
                            }
                        },
                        onDetail = { onDetail?.invoke(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PartySlotRow(
    member: EchoformInstance,
    slotIndex: Int,
    isActive: Boolean,
    isSelected: Boolean,
    enabled: Boolean,
    showDetailIcon: Boolean,
    onClick: () -> Unit,
    onDetail: () -> Unit,
) {
    val (primary, _) = aspectColors(member.species.primaryAspect)
    val borderColor = when {
        isActive -> AetherColors.GoldBright
        isSelected -> AetherColors.GoldCore
        else -> AetherColors.SlateLight
    }
    val backgroundAlpha = if (enabled) 1.0f else 0.45f

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AetherColors.Slate.copy(alpha = backgroundAlpha))
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Echoform sprite + slot index badge in the corner
        Box(Modifier.size(48.dp)) {
            Box(
                Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(primary.copy(alpha = 0.20f)),
            ) {
                EchoformSpriteImage(
                    slug = member.species.id,
                    variant = EchoformSpriteVariant.FRONT,
                    modifier = Modifier.matchParentSize(),
                )
            }
            Box(
                Modifier
                    .size(18.dp)
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(9.dp))
                    .background(primary.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${slotIndex + 1}",
                    color = AetherColors.Obsidian,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = member.species.name,
                    color = AetherColors.ParchmentText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Lv.${member.level}",
                    color = AetherColors.GoldBright,
                    fontSize = 13.sp,
                )
                if (member.isFainted) {
                    Spacer(Modifier.width(8.dp))
                    Text("FAINTED", color = AetherColors.WarningRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                AspectChip(member.species.primaryAspect)
                member.species.secondaryAspect?.let {
                    Spacer(Modifier.width(4.dp))
                    AspectChip(it)
                }
            }
            Spacer(Modifier.height(6.dp))
            HpBar(
                current = member.currentVigor,
                max = member.maxVigor,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "HP ${member.currentVigor}/${member.maxVigor}   ATK ${member.stat(StatKey.FORCE)}   DEF ${member.stat(StatKey.GUARD)}   SPD ${member.stat(StatKey.TEMPO)}",
                color = AetherColors.MutedText,
                fontSize = 11.sp,
            )
            // 4-move chip strip
            if (member.techniques.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row {
                    member.techniques.take(4).forEach { tech ->
                        val (techPrimary, _) = aspectColors(tech.aspect)
                        Box(
                            Modifier
                                .padding(end = 4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(techPrimary.copy(alpha = 0.65f))
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        ) {
                            Text(
                                text = tech.name,
                                color = AetherColors.Obsidian,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
        if (showDetailIcon) {
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AetherColors.GoldCore)
                    .clickable(onClick = onDetail),
                contentAlignment = Alignment.Center,
            ) {
                Text("ⓘ", color = AetherColors.Obsidian, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AspectChip(aspect: com.aetherbound.game.core.Aspect) {
    val (primary, _) = aspectColors(aspect)
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(primary.copy(alpha = 0.9f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = aspect.slug.uppercase(),
            color = AetherColors.Obsidian,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HpBar(current: Int, max: Int) {
    val ratio = (current.toFloat() / max.coerceAtLeast(1)).coerceIn(0f, 1f)
    val color = when {
        ratio > 0.5f -> Color(0xFF4CAF50)
        ratio > 0.2f -> Color(0xFFFFC107)
        else -> AetherColors.WarningRed
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(AetherColors.ObsidianDeep)
            .padding(1.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth(ratio)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color),
        )
    }
}

