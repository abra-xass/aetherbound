package com.aetherbound.game.render.map

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize

/**
 * Resolves a [TmxMap]'s tilesets into bitmap atlases and provides a
 * culled per-frame draw call that paints visible tiles into a [DrawScope].
 *
 * Asset paths inside .tmx are typically relative (e.g. "../gfx/tilesets/x.png"
 * for Tuxemon). The renderer normalises them to the assets root so the
 * Aetherbound import layout (`tilesets/tuxemon/<x>.png`) works.
 */
class TmxRenderer private constructor(
    val map: TmxMap,
    private val tilesets: List<ResolvedTileset>,
) {
    /** Map width/height in pixels (= tile count × tile px). */
    val widthPx: Int = map.width * map.tileWidth
    val heightPx: Int = map.height * map.tileHeight

    /**
     * Draws every visible layer to [scope], culled to [viewport] (in map-px).
     * Caller is responsible for translating/scaling the DrawScope before
     * invoking this — the renderer assumes (0,0) = top-left of the map.
     */
    fun draw(scope: DrawScope, viewport: IntRect = IntRect(0, 0, widthPx, heightPx)) {
        val tileW = map.tileWidth
        val tileH = map.tileHeight
        val firstCol = (viewport.left / tileW).coerceAtLeast(0)
        val lastCol = ((viewport.right + tileW - 1) / tileW).coerceAtMost(map.width)
        val firstRow = (viewport.top / tileH).coerceAtLeast(0)
        val lastRow = ((viewport.bottom + tileH - 1) / tileH).coerceAtMost(map.height)

        for (layer in map.tileLayers) {
            if (!layer.visible) continue
            val alpha = layer.opacity.coerceIn(0f, 1f)
            for (row in firstRow until lastRow) {
                for (col in firstCol until lastCol) {
                    val gid = layer.data[row * layer.width + col]
                    if (gid == 0) continue
                    val (atlas, localId) = resolveGid(gid) ?: continue
                    val tilesetTilesPerRow = atlas.columns.coerceAtLeast(1)
                    val srcCol = localId % tilesetTilesPerRow
                    val srcRow = localId / tilesetTilesPerRow
                    scope.drawImage(
                        image = atlas.bitmap,
                        srcOffset = IntOffset(srcCol * atlas.tileWidth, srcRow * atlas.tileHeight),
                        srcSize = IntSize(atlas.tileWidth, atlas.tileHeight),
                        dstOffset = IntOffset(col * tileW, row * tileH),
                        dstSize = IntSize(tileW, tileH),
                        alpha = alpha,
                        filterQuality = FilterQuality.None,
                    )
                }
            }
        }
    }

    private fun resolveGid(gid: Int): Pair<ResolvedTileset, Int>? {
        // Tilesets are sorted by firstGid asc; find the one whose range contains gid.
        var picked: ResolvedTileset? = null
        for (ts in tilesets) {
            if (ts.firstGid <= gid) picked = ts
        }
        val ts = picked ?: return null
        val localId = gid - ts.firstGid
        return ts to localId
    }

    /** A tileset with its bitmap atlas resolved & loaded. */
    data class ResolvedTileset(
        val firstGid: Int,
        val name: String,
        val tileWidth: Int,
        val tileHeight: Int,
        val columns: Int,
        val bitmap: ImageBitmap,
    )

    companion object {
        /**
         * Builds a renderer for [map] by resolving each tileset to a loaded
         * bitmap. [tmxAssetPath] is the .tmx path inside `assets/`, used to
         * resolve relative tileset/image references.
         *
         * @param assetPathRewrite optional fixup applied to the resolved
         *   image asset path. Default: tries the natural relative path first;
         *   on miss falls back to `tilesets/tuxemon/<basename>.png` so
         *   Aetherbound's import layout works without modifying the .tmx files.
         */
        fun build(
            ctx: Context,
            map: TmxMap,
            tmxAssetPath: String,
            assetPathRewrite: (String) -> String = { it },
        ): TmxRenderer {
            val resolved = mutableListOf<ResolvedTileset>()
            for (ref in map.tilesets) {
                val tileset = if (ref.source != null) {
                    val tsxPath = resolveRelative(tmxAssetPath, ref.source)
                    runCatching { TmxLoader.loadTileset(ctx, tsxPath) }.getOrNull()
                        ?: continue
                } else ref.inline ?: continue

                // Tileset's <image source> is relative to the .tsx (or .tmx for inline).
                val basePath = if (ref.source != null)
                    resolveRelative(tmxAssetPath, ref.source!!)
                else tmxAssetPath
                val rawImagePath = resolveRelative(basePath, tileset.imageSource)
                val imagePath = assetPathRewrite(rawImagePath)
                val bitmap = loadBitmap(ctx, imagePath, tileset)
                    ?: loadBitmap(ctx, fallbackTilesetPath(tileset.imageSource), tileset)
                    ?: continue

                resolved += ResolvedTileset(
                    firstGid = ref.firstGid,
                    name = tileset.name,
                    tileWidth = tileset.tileWidth,
                    tileHeight = tileset.tileHeight,
                    columns = if (tileset.columns > 0) tileset.columns
                        else (tileset.imageWidth / tileset.tileWidth.coerceAtLeast(1)),
                    bitmap = bitmap,
                )
            }
            // Sort ascending so resolveGid's linear scan produces the right tileset.
            return TmxRenderer(map = map, tilesets = resolved.sortedBy { it.firstGid })
        }

        private fun loadBitmap(ctx: Context, path: String, ts: TmxTileset): ImageBitmap? {
            return runCatching {
                com.aetherbound.game.core.data.AssetPackDownloader.resolveStream(ctx, path)
                    ?.use { BitmapFactory.decodeStream(it) }
            }.getOrNull()?.asImageBitmap()
        }

        /** Tuxemon-import fallback: tilesets are mirrored to `tilesets/tuxemon/<name>.png`. */
        private fun fallbackTilesetPath(originalImageSource: String): String {
            val base = originalImageSource.substringAfterLast('/').substringAfterLast('\\')
            return "game/tilesets/tuxemon/$base"
        }

        /** Resolves [relative] against the directory of [base] (both asset paths). */
        private fun resolveRelative(base: String, relative: String): String {
            if (relative.startsWith("/")) return relative.trimStart('/')
            val baseDir = base.substringBeforeLast('/', "")
            val combined = if (baseDir.isEmpty()) relative else "$baseDir/$relative"
            return normalisePath(combined)
        }

        private fun normalisePath(path: String): String {
            val parts = path.split('/').toMutableList()
            val out = ArrayDeque<String>()
            for (p in parts) {
                when (p) {
                    "", "." -> Unit
                    ".." -> if (out.isNotEmpty() && out.last() != "..") out.removeLast() else out.addLast(p)
                    else -> out.addLast(p)
                }
            }
            return out.joinToString("/")
        }
    }
}
