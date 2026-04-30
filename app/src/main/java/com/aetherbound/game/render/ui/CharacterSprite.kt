package com.aetherbound.game.render.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.aetherbound.game.render.map.MovementController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Animates a Tuxemon character sprite-sheet. Tuxemon's player and NPC
 * sheets are laid out 12 frames wide × 1 row tall:
 *
 *   [back-stand][back-walk-l][back-walk-r]
 *   [front-stand][front-walk-l][front-walk-r]
 *   [left-stand][left-walk-l][left-walk-r]
 *   [right-stand][right-walk-l][right-walk-r]
 *
 * (Each set is 3 frames; the walk frames alternate at ~5fps while moving.)
 *
 * Aetherbound's importer copied each Tuxemon sheet to:
 *   `assets/game/characters/tuxemon/<name>/sheet.png`
 *
 * If [moving] is false, only the stand frame for [facing] is drawn.
 *
 * @param sheetAssetPath path inside assets, e.g. `game/characters/tuxemon/adventurer/sheet.png`
 */
@Composable
fun CharacterSprite(
    sheetAssetPath: String,
    facing: MovementController.Facing,
    moving: Boolean,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    var bitmap by remember(sheetAssetPath) { mutableStateOf<ImageBitmap?>(null) }
    var walkPhase by remember { mutableIntStateOf(0) }

    LaunchedEffect(sheetAssetPath) {
        bitmap = withContext(Dispatchers.IO) {
            com.aetherbound.game.core.data.AssetPackDownloader.resolveStream(ctx, sheetAssetPath)?.use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }
    }

    LaunchedEffect(moving) {
        walkPhase = 0
        if (!moving) return@LaunchedEffect
        // Cycle 0 → 1 → 0 → 2 → 0 …  classic 4-step walk loop.
        var i = 0
        val sequence = intArrayOf(0, 1, 0, 2)
        while (true) {
            walkPhase = sequence[i % sequence.size]
            i++
            delay(180)
        }
    }

    Box(modifier) {
        val bm = bitmap ?: return@Box

        // The sheet has 12 frames; we infer frame_w = sheet_width / 12 and
        // frame_h = sheet_height (single-row layout).
        val frameW = bm.width / 12
        val frameH = bm.height

        // Direction → row offset within the 12-cell strip.
        val rowOffset = when (facing) {
            MovementController.Facing.NORTH -> 0  // back
            MovementController.Facing.SOUTH -> 3  // front
            MovementController.Facing.WEST -> 6   // left
            MovementController.Facing.EAST -> 9   // right
        }
        val frameIndex = rowOffset + if (moving) walkPhase else 0

        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width.toInt().coerceAtLeast(1)
            val h = size.height.toInt().coerceAtLeast(1)
            drawImage(
                image = bm,
                srcOffset = IntOffset(frameIndex * frameW, 0),
                srcSize = IntSize(frameW.coerceAtLeast(1), frameH.coerceAtLeast(1)),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(w, h),
                filterQuality = FilterQuality.None,
            )
        }
    }
}
