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
 * Variant of an Echoform sprite to load. Maps to the file naming convention
 * produced by the Tuxemon importer (Phase 2):
 *   menu     → 32×32 inventory / party-list icon (down-sampled front)
 *   front    → 64×64 player-side battle portrait
 *   back     → 64×64 opponent-side battle view
 */
enum class EchoformSpriteVariant(val fileName: String) {
    MENU("menu.png"),
    FRONT("front.png"),
    BACK("back.png"),
}

/**
 * Loads an Echoform sprite from `assets/game/echoforms/<source>/<slug>/<file>.png`,
 * with the standard override-then-tuxemon fallback (matches `AssetResolver`'s
 * convention).
 *
 * Falls back to a soft-coloured placeholder when no asset is present (e.g.
 * the species was generated procedurally and has no PNG).
 */
@Composable
fun EchoformSpriteImage(
    slug: String,
    variant: EchoformSpriteVariant = EchoformSpriteVariant.MENU,
    modifier: Modifier = Modifier,
    pixelArt: Boolean = true,
) {
    val ctx = LocalContext.current
    var bitmap by remember(slug, variant) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(slug, variant) {
        bitmap = withContext(Dispatchers.IO) {
            // Override path takes priority; fall back to tuxemon import.
            val candidates = listOf(
                "game/echoforms/overrides/$slug/${variant.fileName}",
                "game/echoforms/tuxemon/$slug/${variant.fileName}",
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
