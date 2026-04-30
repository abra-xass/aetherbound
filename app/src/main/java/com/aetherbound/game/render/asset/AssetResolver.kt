package com.aetherbound.game.render.asset

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.io.IOException

/**
 * Resolves PNG game-assets bundled in `assets/game/...`. If the asset is
 * missing, the caller falls back to vector rendering. This lets you ship
 * the game with vector-only sprites today and replace each Echoform / city /
 * tile with an AI- or artist-generated PNG later, **without changing any code**.
 *
 * Layout convention:
 *   assets/game/echoforms/E001.png   — full sprite (ideally 256×256 alpha PNG)
 *   assets/game/echoforms/E001@idle.png    — optional pose variants
 *   assets/game/echoforms/E001@hit.png
 *   assets/game/cities/namaris_harbor.png   — full city background
 *   assets/game/tiles/sea_deep.png    — single tile, 64×64 ideal
 */
data class AssetSpec(val path: String) {
    val key: String get() = path
}

object AssetSpecs {
    // ── Production assets (game-ready, sized per Visual Bible) ───────────
    fun echoform(speciesId: String, pose: String = "idle"): AssetSpec =
        AssetSpec("game/echoforms/${speciesId}@${pose}.png")
    fun echoformBase(speciesId: String): AssetSpec =
        AssetSpec("game/echoforms/${speciesId}.png")
    fun city(cityId: String): AssetSpec = AssetSpec("game/cities/${cityId}.png")
    fun tile(tileKey: String): AssetSpec = AssetSpec("game/tiles/${tileKey}.png")
    fun playerSheet(): AssetSpec = AssetSpec("game/player/player_sheet.png")
    fun portrait(characterId: String): AssetSpec = AssetSpec("game/portraits/${characterId}.png")
    /** Top-down overworld painting for a region; replaces the vector tilemap. */
    fun worldMap(regionId: String): AssetSpec = AssetSpec("game/world-maps/${regionId}.png")

    // ── Style boards (Phase 1 of Visual Bible) ───────────────────────────
    fun styleBoardOverworld(): AssetSpec = AssetSpec("game/style-boards/wave01-overworld-style-board.png")
    fun styleBoardBattle(): AssetSpec = AssetSpec("game/style-boards/wave01-battle-style-board.png")
    fun styleBoardEchoform(): AssetSpec = AssetSpec("game/style-boards/wave01-echoform-style-board.png")
    fun styleBoardVfx(): AssetSpec = AssetSpec("game/style-boards/wave01-vfx-style-board.png")

    // ── Region / starter / VFX concepts (Wave 01 jobs 5-13) ──────────────
    fun regionConcept(slug: String): AssetSpec = AssetSpec("game/concepts/wave01-${slug}-concept.png")
    fun starterPremium(slug: String): AssetSpec = AssetSpec("game/concepts/wave01-${slug}-premium-concept.png")
    fun vfxPack(aspect: String): AssetSpec = AssetSpec("game/vfx/wave01-${aspect.lowercase()}-vfx-base-pack.png")
}

/**
 * Compose hook: returns true if the asset is present in the APK. Cached per
 * composition tree so we don't hit the asset manager every recomposition.
 */
@Composable
fun assetExists(spec: AssetSpec): Boolean {
    val ctx = LocalContext.current
    var present by remember(spec.key) { mutableStateOf<Boolean?>(null) }
    if (present == null) {
        present = AssetCache.exists(ctx, spec)
    }
    return present == true
}

/**
 * Process-wide cache of which asset paths exist. We probe the AssetManager
 * once per path and remember the result. Negative results stick — if you
 * ship a new APK with new assets, the process restarts and the cache resets.
 */
object AssetCache {
    private val present = mutableMapOf<String, Boolean>()

    @Synchronized
    fun exists(ctx: Context, spec: AssetSpec): Boolean {
        present[spec.key]?.let { return it }
        val ok = try {
            ctx.assets.open(spec.path).use { true }
        } catch (_: IOException) { false }
        present[spec.key] = ok
        return ok
    }
}
