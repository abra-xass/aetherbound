package com.aetherbound.game.render.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

/**
 * Global toggle for the "mature mood" sprite filter. When on, a desaturated +
 * higher-contrast + slightly-darkened ColorMatrix is applied to every Echoform
 * PNG render so the cuter free-pack sprites read as more serious / dark-fantasy.
 *
 * Toggle from the Sandbox header to A/B compare. If you like it: bake the
 * filter into the source PNGs via tools/ai/postprocess_sprites.py and disable
 * the runtime filter (so the cost vanishes).
 */
object SpriteFilter {
    var matureMood: Boolean by mutableStateOf(true)

    /**
     * Saturation 0.85, contrast 1.15, brightness -0.05.
     * Result: less candy-coloured, more weathered, slightly grimmer.
     */
    val matureMatrix: ColorMatrix = run {
        val s = 0.85f
        val c = 1.15f
        val b = -0.05f * 255f
        val inv = 1f - s
        // Luminance weights from Rec.601 (matches Android ColorMatrix.setSaturation)
        val r = 0.213f * inv
        val g = 0.715f * inv
        val bw = 0.072f * inv
        ColorMatrix(floatArrayOf(
            (r + s) * c, g * c,        bw * c,       0f, b,
            r * c,       (g + s) * c,  bw * c,       0f, b,
            r * c,       g * c,        (bw + s) * c, 0f, b,
            0f,          0f,           0f,           1f, 0f,
        ))
    }

    fun activeFilter(): ColorFilter? =
        if (matureMood) ColorFilter.colorMatrix(matureMatrix) else null
}
