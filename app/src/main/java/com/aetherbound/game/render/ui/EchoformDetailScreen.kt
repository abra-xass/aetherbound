package com.aetherbound.game.render.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherbound.game.core.EchoformInstance
import com.aetherbound.game.core.StatKey
import com.aetherbound.game.core.data.TuxemonDex
import com.aetherbound.game.core.data.TuxemonMonster
import com.aetherbound.game.render.theme.AetherColors
import com.aetherbound.game.render.theme.aspectColors

/**
 * Full detail page for a single Echoform: large sprite, full stats with
 * progress bars, type chips, all 4 active moves with power/accuracy,
 * evolution chain preview.
 */
@Composable
fun EchoformDetailScreen(
    instance: EchoformInstance,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    var monster by remember(instance.species.id) { mutableStateOf<TuxemonMonster?>(null) }

    LaunchedEffect(instance.species.id) {
        monster = TuxemonDex.bySlug(ctx, instance.species.id)
    }

    val scroll = rememberScrollState()

    Column(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(AetherColors.ObsidianDeep, AetherColors.Obsidian, AetherColors.Onyx)
                )
            )
            .padding(12.dp)
            .verticalScroll(scroll),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "back", tint = AetherColors.GoldBright)
            }
            Text(
                text = instance.species.name.uppercase(),
                color = AetherColors.GoldBright,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Lv.${instance.level}",
                color = AetherColors.GoldBright,
                fontSize = 14.sp,
            )
        }
        Spacer(Modifier.height(8.dp))

        // Big sprite + summary line
        Row(verticalAlignment = Alignment.CenterVertically) {
            val (primary, _) = aspectColors(instance.species.primaryAspect)
            Box(
                Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(primary.copy(alpha = 0.18f))
                    .border(2.dp, AetherColors.GoldCore, RoundedCornerShape(12.dp)),
            ) {
                EchoformSpriteImage(
                    slug = instance.species.id,
                    variant = EchoformSpriteVariant.FRONT,
                    modifier = Modifier.matchParentSize(),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Row {
                    AspectChip(instance.species.primaryAspect)
                    instance.species.secondaryAspect?.let {
                        Spacer(Modifier.width(6.dp))
                        AspectChip(it)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Rarity: ${instance.species.rarity.name}",
                    color = AetherColors.MutedText,
                    fontSize = 12.sp,
                )
                Text(
                    "Catch rate: ${instance.species.catchRate}",
                    color = AetherColors.MutedText,
                    fontSize = 12.sp,
                )
                Text(
                    "Temperament: ${instance.temperament.name}",
                    color = AetherColors.MutedText,
                    fontSize = 12.sp,
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        // STATS
        SectionHeader("STATS")
        StatBar("HP",  instance.currentVigor, instance.maxVigor, true)
        StatBar("ATK", instance.stat(StatKey.FORCE), 200)
        StatBar("DEF", instance.stat(StatKey.GUARD), 200)
        StatBar("SP.ATK", instance.stat(StatKey.FOCUS), 200)
        StatBar("SP.DEF", instance.stat(StatKey.WARD), 200)
        StatBar("SPD", instance.stat(StatKey.TEMPO), 200)

        Spacer(Modifier.height(16.dp))

        // MOVES
        SectionHeader("MOVES (${instance.techniques.size}/4)")
        for (tech in instance.techniques) {
            MoveRow(tech)
        }

        Spacer(Modifier.height(16.dp))

        // EVOLUTION
        monster?.let { mon ->
            if (mon.evolvesInto.isNotEmpty() || mon.evolvesFrom.isNotEmpty()) {
                SectionHeader("EVOLUTION")
                if (mon.evolvesFrom.isNotEmpty()) {
                    Text(
                        "← Evolved from: ${mon.evolvesFrom.joinToString(", ")}",
                        color = AetherColors.MutedText,
                        fontSize = 12.sp,
                    )
                }
                if (mon.evolvesInto.isNotEmpty()) {
                    Text(
                        "→ Evolves into: ${mon.evolvesInto.joinToString(", ")}",
                        color = AetherColors.GoldBright,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
            // Bio-style metadata
            SectionHeader("PROFILE")
            Text("Species: ${mon.species.replace('_', ' ')}", color = AetherColors.ParchmentText, fontSize = 12.sp)
            Text("Shape: ${mon.shape}", color = AetherColors.MutedText, fontSize = 12.sp)
            Text("Stage: ${mon.stage}", color = AetherColors.MutedText, fontSize = 12.sp)
            Text("Height: ${mon.heightCm}cm · Weight: ${mon.weightKg}kg", color = AetherColors.MutedText, fontSize = 12.sp)
            if (mon.terrains.isNotEmpty()) {
                Text("Terrains: ${mon.terrains.joinToString(", ")}", color = AetherColors.MutedText, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Spacer(Modifier.height(4.dp))
    Text(
        text = label,
        color = AetherColors.GoldBright,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun StatBar(label: String, current: Int, max: Int, isHp: Boolean = false) {
    val ratio = (current.toFloat() / max.coerceAtLeast(1)).coerceIn(0f, 1f)
    val color = if (isHp) {
        when {
            ratio > 0.5f -> Color(0xFF4CAF50)
            ratio > 0.2f -> Color(0xFFFFC107)
            else -> AetherColors.WarningRed
        }
    } else {
        when {
            ratio > 0.66f -> AetherColors.GoldBright
            ratio > 0.33f -> AetherColors.GoldCore
            else -> AetherColors.GoldDeep
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = AetherColors.MutedText, fontSize = 11.sp, modifier = Modifier.width(70.dp))
        Box(
            Modifier
                .weight(1f)
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
        Spacer(Modifier.width(8.dp))
        Text("$current", color = AetherColors.ParchmentText, fontSize = 11.sp, modifier = Modifier.width(40.dp))
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun MoveRow(tech: com.aetherbound.game.core.Technique) {
    val (primary, _) = aspectColors(tech.aspect)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(AetherColors.Slate)
            .border(1.dp, primary.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AspectChip(tech.aspect)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(tech.name, color = AetherColors.ParchmentText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                "${tech.category.name}  · pwr ${tech.power}  · acc ${tech.accuracy}%  · prio ${tech.priority}",
                color = AetherColors.MutedText,
                fontSize = 10.sp,
            )
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
