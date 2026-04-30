package com.aetherbound.game.render.asset

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aetherbound.game.render.theme.SpriteFilter

/**
 * Renders an Echoform PNG from `assets/game/echoforms/`. Uses Coil with
 * the `file:///android_asset/<path>` scheme.
 *
 * Back-view rules:
 *   1. If [facingBack] and `<id>@back.png` exists (real AI/hand-drawn), use it
 *   2. else if `<id>@<pose>.png` exists, use it (pose-specific front)
 *   3. else fall back to `<id>.png` (base front)
 *
 * If [facingBack] is true and no real back PNG exists, we MIRROR the front
 * horizontally (`scaleX = -1f`) so the creature visually faces away. This
 * is the "good enough" placeholder until proper back-views are sourced.
 *
 * Sprite filter: when [SpriteFilter.matureMood] is on, a desaturated +
 * higher-contrast ColorMatrix is applied to dull the cute / candy look.
 */
@Composable
fun PngEchoform(
    speciesId: String,
    pose: String,
    modifier: Modifier = Modifier,
    facingBack: Boolean = false,
) {
    val ctx = LocalContext.current
    val backSpec = AssetSpecs.echoform(speciesId, "back")
    val poseSpec = AssetSpecs.echoform(speciesId, pose)
    val baseSpec = AssetSpecs.echoformBase(speciesId)

    val hasRealBack = facingBack && AssetCache.exists(ctx, backSpec)
    val path = when {
        hasRealBack -> backSpec.path
        AssetCache.exists(ctx, poseSpec) -> poseSpec.path
        else -> baseSpec.path
    }
    val needsMirror = facingBack && !hasRealBack

    AsyncImage(
        model = ImageRequest.Builder(ctx)
            .data("file:///android_asset/$path")
            .crossfade(true)
            .build(),
        contentDescription = speciesId,
        contentScale = ContentScale.Fit,
        colorFilter = SpriteFilter.activeFilter(),
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { if (needsMirror) scaleX = -1f },
    )
}
