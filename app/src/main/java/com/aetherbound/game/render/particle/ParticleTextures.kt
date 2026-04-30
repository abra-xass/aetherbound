package com.aetherbound.game.render.particle

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.aetherbound.game.core.Aspect

/**
 * Maps particle texture keys → loaded ImageBitmaps.
 *
 * Convention:
 *   key = "<element>_<type>"  e.g. "ember_spark", "tide_orb"
 *   key = "universal_<type>"  e.g. "universal_shockwave"
 *
 * All 144 textures live in `assets/game/particles/`. Loaded lazily on first
 * access; cached for the rest of the composition tree's lifetime.
 */
object ParticleTextures {

    private const val DIR = "game/particles"

    /** Element name (lower-case) for texture-key lookup. */
    fun elementName(aspect: Aspect): String = aspect.name.lowercase()

    /** Build "ember_spark" / "tide_orb" / etc from aspect + type. */
    fun key(aspect: Aspect, type: String): String = "${elementName(aspect)}_$type"

    /** Build "universal_shockwave" / "universal_explosion" / etc. */
    fun universal(type: String): String = "universal_$type"

    /** Loads every PNG in assets/game/particles/ into a key → bitmap map. */
    fun loadAll(ctx: Context): Map<String, ImageBitmap> {
        val out = HashMap<String, ImageBitmap>()
        val names = try { ctx.assets.list(DIR) ?: emptyArray() } catch (e: Exception) { emptyArray() }
        for (n in names) {
            if (!n.endsWith(".png")) continue
            val key = n.removeSuffix(".png")
            try {
                ctx.assets.open("$DIR/$n").use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()?.let { out[key] = it }
                }
            } catch (_: Exception) { /* ignore single failures */ }
        }
        return out
    }
}

@Composable
fun rememberParticleTextures(): Map<String, ImageBitmap> {
    val ctx = LocalContext.current
    return remember(ctx) { ParticleTextures.loadAll(ctx) }
}
