package com.aetherbound.game.render.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders an imported Tuxemon `.tmx` map (in `assets/game/maps/tuxemon/<x>.tmx`)
 * inside a Compose Canvas.
 *
 * @param tmxAssetPath path to the .tmx file, e.g. `"game/maps/tuxemon/route_1.tmx"`
 * @param cameraPx top-left of the map area to render, in map-pixels.
 * @param zoom integer scale factor (1 = native 16-px tiles).
 *
 * Loading is asynchronous — the canvas paints a flat background colour
 * until the map+tilesets resolve. Callers that need to know when the map
 * is ready should observe the [onLoaded] callback.
 */
@Composable
fun TmxScene(
    tmxAssetPath: String,
    modifier: Modifier = Modifier,
    cameraPx: IntOffset = IntOffset.Zero,
    zoom: Float = 1f,
    background: Color = Color(0xFF101015),
    onLoaded: (TmxRenderer) -> Unit = {},
) {
    val ctx = LocalContext.current

    var renderer by remember(tmxAssetPath) { mutableStateOf<TmxRenderer?>(null) }

    LaunchedEffect(tmxAssetPath) {
        val r = withContext(Dispatchers.IO) {
            val map = TmxLoader.loadMap(ctx, tmxAssetPath)
            TmxRenderer.build(ctx, map, tmxAssetPath)
        }
        renderer = r
        onLoaded(r)
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRect(color = background, size = size)
            val r = renderer ?: return@Canvas
            // Compute viewport rect in map pixels visible inside this canvas.
            val viewW = (size.width / zoom).toInt().coerceAtLeast(1)
            val viewH = (size.height / zoom).toInt().coerceAtLeast(1)
            val viewport = IntRect(
                left = cameraPx.x.coerceAtLeast(0),
                top = cameraPx.y.coerceAtLeast(0),
                right = (cameraPx.x + viewW).coerceAtMost(r.widthPx),
                bottom = (cameraPx.y + viewH).coerceAtMost(r.heightPx),
            )
            scale(scaleX = zoom, scaleY = zoom, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                translate(left = -cameraPx.x.toFloat(), top = -cameraPx.y.toFloat()) {
                    r.draw(scope = this, viewport = viewport)
                }
            }
        }
    }
}
