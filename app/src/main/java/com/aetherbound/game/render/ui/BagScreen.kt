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

    var activeTab by remember { mutableStateOf(BagTab.All) }

    // Slug-based classification (Tuxemon items.json only has potion/utility
    // sort fields — too coarse). We bucket by name pattern so the bag pages
    // map to player intent.
    val classified = inventory.stacks.entries.groupBy { (slug, _) ->
        BagTab.classify(slug, resolved[slug])
    }
    val visible = if (activeTab == BagTab.All) {
        inventory.stacks.entries.toList()
    } else {
        classified[activeTab] ?: emptyList()
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

        // Category tabs
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (tab in BagTab.values()) {
                val active = tab == activeTab
                val count = if (tab == BagTab.All) inventory.totalItems
                else (classified[tab]?.sumOf { it.value } ?: 0)
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (active) AetherColors.GoldCore else AetherColors.Slate)
                        .clickable { activeTab = tab }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Text(
                        "${tab.label} ($count)",
                        color = if (active) AetherColors.Obsidian else AetherColors.ParchmentText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))

        if (visible.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (inventory.isEmpty) "Bag is empty." else "No ${activeTab.label} items.",
                    color = AetherColors.MutedText,
                    fontSize = 14.sp,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items = visible, key = { (slug, _) -> "$activeTab-$slug" }) { entry ->
                    val (slug, count) = entry
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

/**
 * Render a one-line human description from an item's [TuxemonItem.effects]
 * payload. Tuxemon's locale `.po` files carry richer descriptions but we
 * don't import them; this synthesised line shows the *intent* of each effect
 * so the player knows what the item does.
 */
private fun describeItemEffects(item: com.aetherbound.game.core.data.TuxemonItem): String {
    if (item.effects.isEmpty()) return item.category.replaceFirstChar { it.uppercase() }
    return item.effects.joinToString(" · ") { spec ->
        val params = spec.parameters
        when (spec.type) {
            "heal" -> {
                val amount = params.getOrNull(0)?.toDoubleOrNull() ?: 0.0
                val mode = params.getOrNull(1) ?: "fixed"
                if (mode == "percent") "Restores ${amount.toInt()}% HP"
                else if (amount < 0) "Deals ${-amount.toInt()} damage"
                else "Restores ${amount.toInt()} HP"
            }
            "restore" -> "Cures ${params.getOrNull(0) ?: "any status"}"
            "capture" -> "Standard capture chance"
            "capture_combined" -> {
                val mult = params.getOrNull(3)?.toDoubleOrNull() ?: 1.0
                "Capture × ${"%.1f".format(mult)}"
            }
            "evolve" -> "Triggers evolution"
            "learn_tm" -> "Teaches ${params.getOrNull(0) ?: "a technique"}"
            "learn_mm" -> "Teaches a master move"
            "gain_xp" -> "+${params.getOrNull(0) ?: "?"} XP"
            "buff", "change_stat" -> {
                val stat = params.getOrNull(0) ?: "stats"
                val v = params.getOrNull(1) ?: "?"
                "Boosts $stat by $v"
            }
            "repellent" -> "Repels wild Echoforms for ${params.getOrNull(0) ?: "?"} steps"
            "fishing" -> "Used to fish water-type Echoforms"
            "switch_type" -> "Changes type to ${params.getOrNull(0) ?: "?"}"
            "teleport_item" -> "Teleports to a known location"
            "food_preference" -> "Affects flavor preference"
            "bivouac" -> "Sets up camp"
            else -> spec.type.replace('_', ' ').replaceFirstChar { it.uppercase() }
        }
    }
}

/** Bag-page category. Auto-classified from item slug + effects payload. */
enum class BagTab(val label: String) {
    All("All"),
    Heal("Heal"),
    Balls("Balls"),
    Berries("Berries"),
    Stones("Stones"),
    TMs("TMs"),
    Key("Key"),
    Misc("Misc");

    companion object {
        fun classify(slug: String, item: com.aetherbound.game.core.data.TuxemonItem?): BagTab {
            val s = slug.lowercase()
            // Effect-driven first (most reliable), slug-pattern as fallback.
            val effectTypes = item?.effects?.map { it.type } ?: emptyList()
            if (effectTypes.any { it == "heal" || it == "restore" }) return Heal
            if (effectTypes.any { it == "capture" || it == "capture_combined" }) return Balls
            if (effectTypes.any { it == "evolve" }) return Stones
            if (effectTypes.any { it == "learn_tm" || it == "learn_mm" }) return TMs

            return when {
                "potion" in s || "revive" in s || "elixir" in s -> Heal
                s.startsWith("tuxeball") || "capture" in s -> Balls
                "berry" in s -> Berries
                "stone" in s -> Stones
                s.startsWith("tm_") || s.startsWith("mm_") -> TMs
                item?.usableIn?.any { it.equals("WorldState", true) } == true &&
                    item.consumable.not() -> Key
                else -> Misc
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
                    text = describeItemEffects(item),
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
