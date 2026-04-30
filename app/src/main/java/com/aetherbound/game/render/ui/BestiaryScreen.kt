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
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherbound.game.core.data.PlayerProgress
import com.aetherbound.game.core.data.TuxemonDex
import com.aetherbound.game.core.data.TuxemonMonster
import com.aetherbound.game.render.theme.AetherColors

/**
 * Pokédex-style index of all 411 species.
 *
 * Three states per cell:
 *   - **Caught**  → full-colour sprite + name + types
 *   - **Seen**    → silhouette (greyscale) + name only
 *   - **Unknown** → blanked slot with `???`
 *
 * Tap a caught species to see its full profile.
 */
@Composable
fun BestiaryScreen(
    progress: PlayerProgress,
    onBack: () -> Unit,
    onSelect: (slug: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    var allMons by remember { mutableStateOf<List<TuxemonMonster>>(emptyList()) }

    LaunchedEffect(Unit) {
        allMons = TuxemonDex.load(ctx).values.toList().sortedBy { it.slug }
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
            Text("BESTIARY", color = AetherColors.GoldBright, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                "${progress.caughtSlugs.size}/${allMons.size}  (${progress.completionPercent(allMons.size.coerceAtLeast(1))}%)",
                color = AetherColors.GoldBright,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 84.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(allMons, key = { it.slug }) { mon ->
                BestiaryCell(
                    mon = mon,
                    seen = mon.slug in progress.seenSlugs,
                    caught = mon.slug in progress.caughtSlugs,
                    onClick = { if (mon.slug in progress.caughtSlugs) onSelect(mon.slug) },
                )
            }
        }
    }
}

@Composable
private fun BestiaryCell(mon: TuxemonMonster, seen: Boolean, caught: Boolean, onClick: () -> Unit) {
    val borderColor = when {
        caught -> AetherColors.GoldCore
        seen -> AetherColors.SlateLight
        else -> AetherColors.Slate
    }
    Column(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AetherColors.Slate)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(enabled = caught, onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(AetherColors.Onyx),
            contentAlignment = Alignment.Center,
        ) {
            when {
                caught -> EchoformSpriteImage(
                    slug = mon.slug,
                    variant = EchoformSpriteVariant.FRONT,
                    modifier = Modifier.matchParentSize(),
                )
                seen -> Box(Modifier.matchParentSize()) {
                    // Greyscale silhouette: tint via ColorFilter would require
                    // re-rendering; we approximate with a dimmed sprite + dark
                    // overlay so the silhouette is recognisable but de-emphasised.
                    EchoformSpriteImage(
                        slug = mon.slug,
                        variant = EchoformSpriteVariant.FRONT,
                        modifier = Modifier.matchParentSize(),
                    )
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.75f)),
                    )
                }
                else -> Text("???", color = AetherColors.MutedText, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (caught || seen) mon.slug.replace('_', ' ').replaceFirstChar { it.uppercase() } else "???",
            color = if (caught) AetherColors.ParchmentText else AetherColors.MutedText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
        if (caught && mon.types.isNotEmpty()) {
            Text(
                text = mon.types.joinToString("/") { it.slug },
                color = AetherColors.GoldBright,
                fontSize = 8.sp,
            )
        }
    }
}

@Suppress("unused")
private val GREYSCALE_FILTER = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            0.33f, 0.33f, 0.33f, 0f, 0f,
            0.33f, 0.33f, 0.33f, 0f, 0f,
            0.33f, 0.33f, 0.33f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
    )
)
