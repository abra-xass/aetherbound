package com.aetherbound.game.render.animation

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import java.util.concurrent.ConcurrentHashMap

/**
 * Frame-by-frame player for Tuxemon move animations imported into
 * `assets/game/techniques/tuxemon/<slug>/frame_NNNN.png`.
 *
 * The pilot uses [com.aetherbound.game.render.animation.AnimationRecipe]
 * for its 8 hand-tuned signature moves — that system stays. This player
 * provides a fallback that *any* of Tuxemon's 274 imported move slugs can
 * use by lazily loading the frame sheet on first play.
 */
class TuxemonMoveFx(private val ctx: Context) {

    private val frameCache = ConcurrentHashMap<String, List<ImageBitmap>>()
    private val durations = ConcurrentHashMap<String, Int>()

    /**
     * Default per-frame duration (ms). Tuxemon ships meta.json next to its
     * animation frames in the python source but our import phase 6 didn't
     * preserve it; we use a flat rate that works for ~24 fps move FX.
     */
    private val defaultFrameMs = 42

    /** Total animation duration in ms for a given slug. */
    fun durationMs(slug: String): Int = durations.getOrPut(slug) {
        loadFrames(slug).size * defaultFrameMs
    }

    /**
     * Draw the frame at [elapsedMs] for [slug] centered on [center].
     * Returns true if the animation is still running, false when it's done.
     */
    fun draw(scope: DrawScope, slug: String, elapsedMs: Int, center: Offset): Boolean {
        val frames = loadFrames(slug)
        if (frames.isEmpty()) return false
        val totalMs = frames.size * defaultFrameMs
        if (elapsedMs >= totalMs) return false
        val idx = (elapsedMs / defaultFrameMs).coerceAtMost(frames.size - 1)
        val frame = frames[idx]
        val w = frame.width
        val h = frame.height
        scope.drawImage(
            image = frame,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(w, h),
            dstOffset = IntOffset((center.x - w / 2).toInt(), (center.y - h / 2).toInt()),
            dstSize = IntSize(w, h),
            filterQuality = FilterQuality.None,
        )
        return true
    }

    private fun loadFrames(slug: String): List<ImageBitmap> {
        frameCache[slug]?.let { return it }
        val frames = mutableListOf<ImageBitmap>()
        val dir = "game/techniques/tuxemon/$slug"
        try {
            val list = com.aetherbound.game.core.data.AssetPackDownloader.resolveListing(ctx, dir)
                .filter { it.startsWith("frame_") }
                .sorted()
            for (name in list) {
                com.aetherbound.game.core.data.AssetPackDownloader.resolveStream(ctx, "$dir/$name")
                    ?.use { stream -> BitmapFactory.decodeStream(stream)?.asImageBitmap() }
                    ?.let { frames += it }
            }
        } catch (_: Exception) {
            // Slug has no animation directory — return empty list to signal
            // "fall back to recipe-based vector animation".
        }
        frameCache[slug] = frames
        return frames
    }

    /** Clear cached bitmaps (e.g. low-memory pressure). */
    fun release() { frameCache.clear() }
}
