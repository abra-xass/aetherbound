package com.aetherbound.game.render.battle

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class SpritePose { Idle, Attacking, Hit, Fainted }
enum class EchoformVisualId { Vulkid, Reeva }

/**
 * Vector-rigged Echoform sprite with **volumetric shading**:
 *   • cast shadow with soft falloff
 *   • aspect-coloured under-glow aura
 *   • body painted with radial gradients (volume)
 *   • rim light from upper-right (lit edge)
 *   • subsurface highlight (warm rim under)
 *   • specular eye dot
 *   • signature elemental flourish per species
 *
 * Pose states drive scale/translate/tilt — no spritesheets.
 */
@Composable
fun EchoformSprite(
    visualId: EchoformVisualId,
    pose: SpritePose,
    facingRight: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "idle")
    val breathe by transition.animateFloat(
        initialValue = 0.96f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label = "breathe",
    )
    val auraPulse by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse),
        label = "aura",
    )

    val poseScaleY = when (pose) {
        SpritePose.Idle -> breathe
        SpritePose.Attacking -> 1.05f
        SpritePose.Hit -> 0.92f
        SpritePose.Fainted -> 0.4f
    }
    val poseTilt = when (pose) {
        SpritePose.Hit -> -8f
        SpritePose.Attacking -> 4f
        SpritePose.Fainted -> 25f
        SpritePose.Idle -> 0f
    }

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h * 0.62f
        val flip = if (facingRight) 1f else -1f

        // 1) cast shadow with soft falloff (radial gradient ellipse)
        translate(left = cx, top = cy) {
            drawOval(
                brush = Brush.radialGradient(
                    listOf(Color(0x80000000), Color(0x00000000)),
                    center = Offset(0f, h * 0.36f),
                    radius = w * 0.36f,
                ),
                topLeft = Offset(-w * 0.36f, h * 0.30f),
                size = Size(w * 0.72f, h * 0.14f),
            )
        }

        // 2) aspect-coloured ground glow (additive)
        val auraColor = when (visualId) {
            EchoformVisualId.Vulkid -> Color(0xFFF05A28)
            EchoformVisualId.Reeva -> Color(0xFF2F9EEA)
        }
        translate(left = cx, top = cy) {
            val auraR = w * (0.34f + auraPulse * 0.08f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(auraColor.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(0f, h * 0.32f),
                    radius = auraR,
                ),
                radius = auraR,
                center = Offset(0f, h * 0.32f),
                blendMode = BlendMode.Plus,
            )
        }

        // 3) body
        translate(left = cx, top = cy) {
            scale(scaleX = flip, scaleY = poseScaleY, pivot = Offset(0f, 0f)) {
                rotate(poseTilt, Offset.Zero) {
                    when (visualId) {
                        EchoformVisualId.Vulkid -> drawVulkid(w, h, auraPulse)
                        EchoformVisualId.Reeva -> drawReeva(w, h, auraPulse)
                    }
                }
            }
        }
    }
}

// ── Vulkid (Ember salamander) ────────────────────────────────────────────────
private fun DrawScope.drawVulkid(w: Float, h: Float, auraPulse: Float) {
    val s = minOf(w, h)
    val deepShadow = Color(0xFF1A0606)
    val bodyDark = Color(0xFF3A1410)
    val bodyMid = Color(0xFF6B1F18)
    val bodyHi = Color(0xFFA8331C)
    val bellyHi = Color(0xFFC9442A)
    val emberCore = Color(0xFFF05A28)
    val emberHot = Color(0xFFFFD43B)
    val emberWhite = Color(0xFFFFF6CC)
    val rimGold = Color(0xFFFFE08A)

    // ---- Tail (behind body) ----
    val tailBack = Path().apply {
        moveTo(-s * 0.34f, -s * 0.04f)
        cubicTo(-s * 0.62f, -s * 0.18f, -s * 0.58f, -s * 0.36f, -s * 0.42f, -s * 0.32f)
        cubicTo(-s * 0.22f, -s * 0.22f, -s * 0.20f, -s * 0.06f, -s * 0.20f, 0f)
        close()
    }
    drawPath(tailBack, brush = Brush.linearGradient(
        listOf(deepShadow, bodyDark),
        start = Offset(-s * 0.5f, -s * 0.3f), end = Offset(-s * 0.2f, 0f),
    ))
    // tail rim light
    drawPath(tailBack, color = rimGold.copy(alpha = 0.35f), style = Stroke(s * 0.012f))

    // ember at tail tip — glowing core + bright halo
    drawCircle(emberCore.copy(alpha = 0.3f * (1f + auraPulse)), s * 0.13f, Offset(-s * 0.46f, -s * 0.30f), blendMode = BlendMode.Plus)
    drawCircle(emberCore, s * 0.075f, Offset(-s * 0.46f, -s * 0.30f))
    drawCircle(emberHot, s * 0.045f, Offset(-s * 0.46f, -s * 0.30f), blendMode = BlendMode.Plus)
    drawCircle(emberWhite, s * 0.018f, Offset(-s * 0.455f, -s * 0.305f), blendMode = BlendMode.Plus)

    // ---- Body (radial gradient for volume) ----
    val bodyRect = Size(s * 0.70f, s * 0.38f)
    val bodyTL = Offset(-s * 0.35f, -s * 0.19f)
    drawOval(
        brush = Brush.radialGradient(
            listOf(bodyHi, bodyMid, bodyDark),
            center = Offset(s * 0.05f, -s * 0.10f), // light from upper right
            radius = s * 0.42f,
        ),
        topLeft = bodyTL, size = bodyRect,
    )

    // belly highlight (warmer, lower)
    drawOval(
        brush = Brush.radialGradient(
            listOf(bellyHi, Color.Transparent),
            center = Offset(0f, s * 0.04f), radius = s * 0.30f,
        ),
        topLeft = Offset(-s * 0.26f, -s * 0.06f), size = Size(s * 0.52f, s * 0.22f),
    )

    // ridge of ember scales
    val ridge = Path().apply {
        moveTo(-s * 0.30f, -s * 0.18f)
        quadraticBezierTo(-s * 0.18f, -s * 0.30f, -s * 0.04f, -s * 0.20f)
        quadraticBezierTo(s * 0.10f, -s * 0.32f, s * 0.22f, -s * 0.20f)
    }
    drawPath(ridge, color = emberCore, style = Stroke(s * 0.030f))
    drawPath(ridge, color = emberHot.copy(alpha = 0.7f), style = Stroke(s * 0.012f))

    // rim light along upper back (bright stroke)
    val rim = Path().apply {
        moveTo(-s * 0.30f, -s * 0.16f)
        quadraticBezierTo(0f, -s * 0.22f, s * 0.30f, -s * 0.14f)
    }
    drawPath(rim, color = rimGold.copy(alpha = 0.55f), style = Stroke(s * 0.010f))

    // ---- Legs ----
    drawOval(
        brush = Brush.verticalGradient(listOf(bodyMid, deepShadow)),
        topLeft = Offset(-s * 0.22f, s * 0.10f), size = Size(s * 0.10f, s * 0.16f),
    )
    drawOval(
        brush = Brush.verticalGradient(listOf(bodyMid, deepShadow)),
        topLeft = Offset(s * 0.10f, s * 0.10f), size = Size(s * 0.10f, s * 0.16f),
    )

    // ---- Head ----
    drawCircle(
        brush = Brush.radialGradient(
            listOf(bodyHi, bodyMid, deepShadow),
            center = Offset(s * 0.36f, -s * 0.16f), radius = s * 0.22f,
        ),
        radius = s * 0.21f,
        center = Offset(s * 0.30f, -s * 0.10f),
    )
    // snout
    drawOval(
        brush = Brush.radialGradient(listOf(bodyHi, bodyDark), radius = s * 0.18f, center = Offset(s * 0.50f, -s * 0.02f)),
        topLeft = Offset(s * 0.40f, -s * 0.06f), size = Size(s * 0.22f, s * 0.13f),
    )

    // jaw shadow
    drawOval(
        color = deepShadow.copy(alpha = 0.4f),
        topLeft = Offset(s * 0.38f, s * 0.02f), size = Size(s * 0.22f, s * 0.06f),
    )

    // ---- Eye (specular) ----
    drawCircle(emberWhite, s * 0.05f, Offset(s * 0.36f, -s * 0.16f))
    drawCircle(Color(0xFFFFC85A), s * 0.040f, Offset(s * 0.36f, -s * 0.16f))
    drawCircle(Color(0xFF101217), s * 0.026f, Offset(s * 0.37f, -s * 0.155f))
    drawCircle(emberWhite, s * 0.012f, Offset(s * 0.378f, -s * 0.165f), blendMode = BlendMode.Plus)

    // brow flame plume
    val plume = Path().apply {
        moveTo(s * 0.18f, -s * 0.26f)
        cubicTo(s * 0.25f, -s * 0.46f, s * 0.40f, -s * 0.46f, s * 0.44f, -s * 0.24f)
        cubicTo(s * 0.36f, -s * 0.36f, s * 0.26f, -s * 0.36f, s * 0.18f, -s * 0.26f)
        close()
    }
    drawPath(plume, brush = Brush.verticalGradient(listOf(emberHot, emberCore)), blendMode = BlendMode.Plus)
    drawPath(plume, color = emberWhite.copy(alpha = 0.6f), style = Stroke(s * 0.005f), blendMode = BlendMode.Plus)

    // tiny spark sparkles on the body
    drawCircle(emberHot, s * 0.012f, Offset(s * 0.10f, -s * 0.04f), blendMode = BlendMode.Plus)
    drawCircle(emberHot, s * 0.010f, Offset(-s * 0.05f, -s * 0.10f), blendMode = BlendMode.Plus)
}

// ── Reeva (Tide river guardian) ──────────────────────────────────────────────
private fun DrawScope.drawReeva(w: Float, h: Float, auraPulse: Float) {
    val s = minOf(w, h)
    val deepShadow = Color(0xFF071424)
    val bodyDeep = Color(0xFF14334C)
    val bodyMid = Color(0xFF1F567A)
    val bodyHi = Color(0xFF3D8DB2)
    val finHi = Color(0xFF9BDAF2)
    val foam = Color(0xFFEAFBFF)
    val rim = Color(0xFFCAEFFF)

    // ---- Flippers (behind body) ----
    val flipper = Path().apply {
        moveTo(-s * 0.34f, s * 0.02f)
        cubicTo(-s * 0.60f, -s * 0.10f, -s * 0.56f, -s * 0.30f, -s * 0.46f, -s * 0.26f)
        cubicTo(-s * 0.34f, -s * 0.18f, -s * 0.30f, -s * 0.06f, -s * 0.30f, -s * 0.04f)
        close()
    }
    drawPath(flipper, brush = Brush.linearGradient(
        listOf(bodyDeep, bodyMid),
        start = Offset(-s * 0.5f, -s * 0.2f), end = Offset(-s * 0.2f, 0f),
    ))
    drawPath(flipper, color = finHi, style = Stroke(s * 0.012f))
    drawPath(flipper, color = foam.copy(alpha = 0.4f), style = Stroke(s * 0.005f))

    // ---- Body with radial volume ----
    drawOval(
        brush = Brush.radialGradient(
            listOf(bodyHi, bodyMid, bodyDeep),
            center = Offset(s * 0.05f, -s * 0.08f), radius = s * 0.42f,
        ),
        topLeft = Offset(-s * 0.36f, -s * 0.18f), size = Size(s * 0.72f, s * 0.36f),
    )

    // belly silver glow
    drawOval(
        brush = Brush.radialGradient(
            listOf(rim.copy(alpha = 0.6f), Color.Transparent),
            center = Offset(0f, s * 0.04f), radius = s * 0.28f,
        ),
        topLeft = Offset(-s * 0.24f, -s * 0.04f), size = Size(s * 0.48f, s * 0.20f),
    )

    // dorsal fin
    val dorsal = Path().apply {
        moveTo(-s * 0.10f, -s * 0.16f)
        cubicTo(-s * 0.04f, -s * 0.40f, s * 0.04f, -s * 0.40f, s * 0.10f, -s * 0.16f)
        close()
    }
    drawPath(dorsal, brush = Brush.verticalGradient(listOf(finHi, bodyMid)))
    drawPath(dorsal, color = foam.copy(alpha = 0.6f), style = Stroke(s * 0.008f))

    // animated foam ring (drifting along body) — subtle
    val foamY = -s * 0.04f + sin(auraPulse * PI.toFloat()) * s * 0.01f
    drawArc(
        color = foam.copy(alpha = 0.55f),
        startAngle = 200f, sweepAngle = 140f, useCenter = false,
        topLeft = Offset(-s * 0.22f, foamY), size = Size(s * 0.44f, s * 0.16f),
        style = Stroke(s * 0.012f),
    )

    // rim light along upper back
    val rimPath = Path().apply {
        moveTo(-s * 0.30f, -s * 0.14f)
        cubicTo(-s * 0.10f, -s * 0.20f, s * 0.10f, -s * 0.20f, s * 0.30f, -s * 0.10f)
    }
    drawPath(rimPath, color = rim.copy(alpha = 0.65f), style = Stroke(s * 0.012f))

    // ---- Head ----
    drawCircle(
        brush = Brush.radialGradient(
            listOf(bodyHi, bodyMid, bodyDeep),
            center = Offset(s * 0.36f, -s * 0.10f), radius = s * 0.22f,
        ),
        radius = s * 0.21f, center = Offset(s * 0.30f, -s * 0.06f),
    )

    // cheek whisker fins
    drawLine(finHi, Offset(s * 0.20f, s * 0.00f), Offset(s * 0.04f, s * 0.10f), strokeWidth = s * 0.014f)
    drawLine(finHi, Offset(s * 0.40f, s * 0.00f), Offset(s * 0.56f, s * 0.10f), strokeWidth = s * 0.014f)

    // ---- Eye ----
    drawCircle(foam, s * 0.05f, Offset(s * 0.34f, -s * 0.10f))
    drawCircle(Color(0xFF8DC8E5), s * 0.040f, Offset(s * 0.34f, -s * 0.10f))
    drawCircle(Color(0xFF071424), s * 0.026f, Offset(s * 0.345f, -s * 0.095f))
    drawCircle(foam, s * 0.013f, Offset(s * 0.353f, -s * 0.105f), blendMode = BlendMode.Plus)

    // crest droplet (above head)
    val crest = Path().apply {
        moveTo(s * 0.30f, -s * 0.34f)
        cubicTo(s * 0.40f, -s * 0.20f, s * 0.34f, -s * 0.10f, s * 0.30f, -s * 0.10f)
        cubicTo(s * 0.26f, -s * 0.10f, s * 0.20f, -s * 0.20f, s * 0.30f, -s * 0.34f)
        close()
    }
    drawPath(crest, brush = Brush.verticalGradient(listOf(foam, finHi)))
    drawPath(crest, color = foam, style = Stroke(s * 0.005f), blendMode = BlendMode.Plus)
    // tiny specular dot on droplet
    drawCircle(foam, s * 0.014f, Offset(s * 0.30f, -s * 0.26f), blendMode = BlendMode.Plus)

    // foam highlight along belly
    drawArc(
        color = foam.copy(alpha = 0.55f),
        startAngle = 0f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(-s * 0.20f, s * 0.0f),
        size = Size(s * 0.40f, s * 0.16f),
        style = Stroke(s * 0.010f),
    )

    // micro sparkles
    drawCircle(rim, s * 0.010f, Offset(s * 0.08f, -s * 0.06f), blendMode = BlendMode.Plus)
    drawCircle(rim, s * 0.008f, Offset(-s * 0.05f, s * 0.02f), blendMode = BlendMode.Plus)
}
