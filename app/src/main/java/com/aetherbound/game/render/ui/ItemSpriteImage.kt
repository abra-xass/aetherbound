package com.aetherbound.game.render.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads an item icon from `assets/game/items/<source>/<slug>.png`, with
 * the standard override-then-tuxemon fallback. Tuxemon ships 32×32 PNG
 * icons; the importer (Phase 3) copied 177 of them to
 * `game/items/tuxemon/<slug>.png`.
 */
@Composable
fun ItemSpriteImage(
    slug: String,
    modifier: Modifier = Modifier,
    pixelArt: Boolean = true,
) {
    val ctx = LocalContext.current
    var bitmap by remember(slug) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(slug) {
        bitmap = withContext(Dispatchers.IO) {
            val candidates = listOf(
                "game/items/overrides/$slug.png",
                "game/items/tuxemon/$slug.png",
            )
            for (path in candidates) {
                val stream = com.aetherbound.game.core.data.AssetPackDownloader.resolveStream(ctx, path)
                if (stream != null) {
                    stream.use {
                        val bm = BitmapFactory.decodeStream(it)?.asImageBitmap()
                        if (bm != null) return@withContext bm
                    }
                }
            }
            null
        }
    }

    Box(modifier) {
        val bm = bitmap ?: return@Box
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width.toInt().coerceAtLeast(1)
            val h = size.height.toInt().coerceAtLeast(1)
            drawImage(
                image = bm,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(bm.width, bm.height),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(w, h),
                filterQuality = if (pixelArt) FilterQuality.None else FilterQuality.Low,
            )
        }
    }
}
