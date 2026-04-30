package com.aetherbound.game.render.world

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.aetherbound.game.render.theme.AetherColors
import kotlin.math.sin

enum class Facing { Up, Down, Left, Right }

/**
 * Tiny vector-rigged player sprite. The walk cycle is implemented through the
 * animPhase parameter (0..1, looped by caller) — limbs swing as sin functions.
 * Cinzel-styled, gold accents, obsidian cape.
 */
@Composable
fun PlayerSprite(
    facing: Facing,
    walking: Boolean,
    animPhase: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val s = minOf(w, h)
        val swing = if (walking) sin(animPhase * 6.2832f) * (s * 0.06f) else 0f

        // shadow
        drawOval(Color(0x55000000), Offset(s * 0.14f, s * 0.78f), Size(s * 0.72f, s * 0.10f))

        when (facing) {
            Facing.Down -> drawDown(s, swing)
            Facing.Up -> drawUp(s, swing)
            Facing.Left -> drawSide(s, swing, flip = true)
            Facing.Right -> drawSide(s, swing, flip = false)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDown(s: Float, swing: Float) {
    // legs
    drawRect(AetherColors.Obsidian, Offset(s * 0.36f, s * 0.55f + swing), Size(s * 0.10f, s * 0.20f))
    drawRect(AetherColors.Obsidian, Offset(s * 0.54f, s * 0.55f - swing), Size(s * 0.10f, s * 0.20f))
    // body / cape
    val cape = Path().apply {
        moveTo(s * 0.20f, s * 0.30f)
        lineTo(s * 0.80f, s * 0.30f)
        lineTo(s * 0.72f, s * 0.62f)
        lineTo(s * 0.28f, s * 0.62f)
        close()
    }
    drawPath(cape, AetherColors.ObsidianDeep)
    // chest plate w/ gold trim
    drawRect(AetherColors.Slate, Offset(s * 0.34f, s * 0.36f), Size(s * 0.32f, s * 0.22f))
    drawRect(AetherColors.GoldCore, Offset(s * 0.34f, s * 0.36f), Size(s * 0.32f, s * 0.04f))
    // arms swinging
    drawRect(AetherColors.ObsidianDeep, Offset(s * 0.20f, s * 0.36f + swing * 0.7f), Size(s * 0.08f, s * 0.18f))
    drawRect(AetherColors.ObsidianDeep, Offset(s * 0.72f, s * 0.36f - swing * 0.7f), Size(s * 0.08f, s * 0.18f))
    // head
    drawCircle(Color(0xFFE5C39E), s * 0.13f, Offset(s * 0.5f, s * 0.22f))
    // hair
    val hair = Path().apply {
        moveTo(s * 0.36f, s * 0.18f)
        quadraticBezierTo(s * 0.5f, s * 0.05f, s * 0.64f, s * 0.18f)
        lineTo(s * 0.64f, s * 0.24f); lineTo(s * 0.36f, s * 0.24f)
        close()
    }
    drawPath(hair, Color(0xFF1F1B17))
    // eyes (face down: small)
    drawCircle(Color(0xFF101217), s * 0.012f, Offset(s * 0.46f, s * 0.225f))
    drawCircle(Color(0xFF101217), s * 0.012f, Offset(s * 0.54f, s * 0.225f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawUp(s: Float, swing: Float) {
    drawRect(AetherColors.Obsidian, Offset(s * 0.36f, s * 0.55f + swing), Size(s * 0.10f, s * 0.20f))
    drawRect(AetherColors.Obsidian, Offset(s * 0.54f, s * 0.55f - swing), Size(s * 0.10f, s * 0.20f))
    // back of cape (more cape visible)
    val cape = Path().apply {
        moveTo(s * 0.18f, s * 0.28f)
        lineTo(s * 0.82f, s * 0.28f)
        lineTo(s * 0.74f, s * 0.66f)
        lineTo(s * 0.26f, s * 0.66f)
        close()
    }
    drawPath(cape, AetherColors.ObsidianDeep)
    // cape gold seam
    drawRect(AetherColors.GoldCore, Offset(s * 0.48f, s * 0.30f), Size(s * 0.04f, s * 0.34f))
    // arms
    drawRect(AetherColors.ObsidianDeep, Offset(s * 0.20f, s * 0.36f + swing * 0.7f), Size(s * 0.08f, s * 0.18f))
    drawRect(AetherColors.ObsidianDeep, Offset(s * 0.72f, s * 0.36f - swing * 0.7f), Size(s * 0.08f, s * 0.18f))
    // back of head
    drawCircle(Color(0xFF1F1B17), s * 0.13f, Offset(s * 0.5f, s * 0.22f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSide(s: Float, swing: Float, flip: Boolean) {
    val mirror = if (flip) -1f else 1f
    val ox = if (flip) s * 0.5f else s * 0.5f

    // Use a translate + scale for mirror via simple coordinate flips on x
    fun fx(x: Float): Float = ox + mirror * (x - s * 0.5f)

    // legs
    drawRect(AetherColors.Obsidian, Offset(fx(s * 0.40f) - s * 0.05f, s * 0.55f + swing), Size(s * 0.10f, s * 0.20f))
    drawRect(AetherColors.Obsidian, Offset(fx(s * 0.55f) - s * 0.05f, s * 0.55f - swing), Size(s * 0.10f, s * 0.20f))
    // cape
    val cape = Path().apply {
        moveTo(fx(s * 0.30f), s * 0.30f)
        lineTo(fx(s * 0.70f), s * 0.30f)
        lineTo(fx(s * 0.66f), s * 0.62f)
        lineTo(fx(s * 0.34f), s * 0.62f)
        close()
    }
    drawPath(cape, AetherColors.ObsidianDeep)
    drawRect(AetherColors.Slate, Offset(fx(s * 0.36f) - s * 0.0f, s * 0.36f), Size(s * 0.28f, s * 0.22f))
    drawRect(AetherColors.GoldCore, Offset(fx(s * 0.36f), s * 0.36f), Size(s * 0.28f, s * 0.04f))
    // single visible arm
    drawRect(AetherColors.ObsidianDeep, Offset(fx(s * 0.40f), s * 0.36f + swing), Size(s * 0.08f, s * 0.20f))
    // head
    drawCircle(Color(0xFFE5C39E), s * 0.13f, Offset(fx(s * 0.5f), s * 0.22f))
    // hair tuft
    val hair = Path().apply {
        moveTo(fx(s * 0.36f), s * 0.18f)
        quadraticBezierTo(fx(s * 0.5f), s * 0.05f, fx(s * 0.64f), s * 0.18f)
        lineTo(fx(s * 0.64f), s * 0.24f); lineTo(fx(s * 0.36f), s * 0.24f)
        close()
    }
    drawPath(hair, Color(0xFF1F1B17))
    // single eye on facing side
    drawCircle(Color(0xFF101217), s * 0.014f, Offset(fx(s * 0.56f), s * 0.225f))
}
