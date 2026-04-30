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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherbound.game.core.data.Inventory
import com.aetherbound.game.core.data.TuxemonItem
import com.aetherbound.game.core.data.TuxemonItemDex
import com.aetherbound.game.render.theme.AetherColors

/**
 * Player bag screen. Shows items grouped by Tuxemon `sort` (utility,
 * consumable, quest, equipment, ...) with stack counts. Tapping an item
 * triggers [onUseItem] — the caller decides where (battle/world).
 */
@Composable
fun BagScreen(
    inventory: Inventory,
    onBack: () -> Unit,
    onUseItem: (TuxemonItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    var resolved by remember { mutableStateOf<Map<String, TuxemonItem>>(emptyMap()) }

    // Hot-load the item dex once per screen open. Cheap after the first call.
    LaunchedEffect(Unit) {
        resolved = TuxemonItemDex.load(ctx)
    }

    val grouped = inventory.groupedBySort { resolved[it] }

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
                text = "BAG",
                color = AetherColors.GoldBright,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AetherColors.Slate)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "${'$'}${inventory.money}",
                    color = AetherColors.GoldBright,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        if (inventory.isEmpty) {
            Box(
                Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Bag is empty.",
                    color = AetherColors.MutedText,
                    fontSize = 14.sp,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                grouped.entries.sortedBy { it.key }.forEach { (sort, entries) ->
                    item("h-$sort") {
                        SortHeader(label = sort)
                    }
                    items(items = entries, key = { (slug, _) -> "$sort-$slug" }) { (slug, count) ->
                        val item = resolved[slug]
                        BagRow(
                            slug = slug,
                            count = count,
                            item = item,
                            onClick = { item?.let(onUseItem) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SortHeader(label: String) {
    Text(
        text = label.replaceFirstChar { it.uppercase() },
        color = AetherColors.GoldBright,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun BagRow(slug: String, count: Int, item: TuxemonItem?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AetherColors.Slate)
            .border(1.dp, AetherColors.SlateLight, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Real Tuxemon item icon (with letter fallback if PNG missing)
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AetherColors.Onyx),
            contentAlignment = Alignment.Center,
        ) {
            ItemSpriteImage(slug = slug, modifier = Modifier.matchParentSize())
            // Letter fallback only visible when PNG fails — drawn underneath the
            // sprite, hidden once the bitmap loads.
            Text(
                text = slug.first().uppercase(),
                color = AetherColors.GoldBright.copy(alpha = 0.25f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = slug.replace('_', ' ').replaceFirstChar { it.uppercase() },
                color = AetherColors.ParchmentText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            if (item != null) {
                Text(
                    text = item.category.replaceFirstChar { it.uppercase() },
                    color = AetherColors.MutedText,
                    fontSize = 11.sp,
                )
            }
        }

        Text(
            text = "× $count",
            color = AetherColors.GoldBright,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
