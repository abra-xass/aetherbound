package com.aetherbound.game.render.particle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Pre-allocated particle pool. Hard cap = QualityPreset.maxParticles to keep
 * per-frame work bounded on mid-range hardware.
 *
 * Texture-aware: if a Particle has [textureKey] set and the system was
 * constructed with a `textures` map containing that key, the texture is
 * drawn at the particle position. Otherwise we fall back to drawCircle (the
 * pre-texture behaviour).
 */
class ParticleSystem(
    maxParticles: Int,
    /** Map from texture key (e.g. "ember_spark") to loaded ImageBitmap. */
    val textures: Map<String, ImageBitmap> = emptyMap(),
) {
    private val pool: Array<Particle> = Array(maxParticles) { Particle() }
    private var aliveCount = 0
    val capacity: Int = maxParticles

    val activeCount: Int get() = aliveCount

    fun clear() { aliveCount = 0 }

    fun emit(
        origin: Offset,
        direction: Float,
        spreadRad: Float,
        speedMin: Float,
        speedMax: Float,
        lifeMs: Int,
        radiusPx: Float,
        color: Color,
        gravity: Float = 0f,
        rng: Random,
        count: Int,
        kind: ParticleKind = ParticleKind.SPARK,
        textureKey: String? = null,
    ) {
        repeat(count) {
            if (aliveCount >= pool.size) return
            val p = pool[aliveCount]
            val angle = direction + (rng.nextFloat() - 0.5f) * 2f * spreadRad
            val speed = speedMin + rng.nextFloat() * (speedMax - speedMin)
            p.x = origin.x; p.y = origin.y
            p.vx = cos(angle) * speed
            p.vy = sin(angle) * speed
            p.lifeRemaining = lifeMs.toFloat()
            p.lifeTotal = lifeMs.toFloat()
            p.radius = radiusPx
            p.color = color
            p.gravity = gravity
            p.kind = kind
            p.rotation = rng.nextFloat() * 6.2832f
            p.textureKey = textureKey
            aliveCount++
        }
    }

    fun update(dtMs: Float) {
        var i = 0
        while (i < aliveCount) {
            val p = pool[i]
            p.lifeRemaining -= dtMs
            if (p.lifeRemaining <= 0f) {
                aliveCount--
                if (i != aliveCount) {
                    val tmp = pool[i]; pool[i] = pool[aliveCount]; pool[aliveCount] = tmp
                }
                continue
            }
            val dt = dtMs / 1000f
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy += p.gravity * dt
            p.rotation += dt * 4f
            i++
        }
    }

    fun draw(scope: DrawScope) {
        for (i in 0 until aliveCount) {
            val p = pool[i]
            val lifeFrac = (p.lifeRemaining / p.lifeTotal).coerceIn(0f, 1f)
            val alpha = if (p.kind == ParticleKind.EMBER) lifeFrac * lifeFrac else lifeFrac
            val color = p.color.copy(alpha = p.color.alpha * alpha)
            // Always draw the bright vector core first (guarantees visibility)
            drawVector(scope, p, color, lifeFrac)
            // Then optionally additively layer the textured detail on top
            val tex = p.textureKey?.let { textures[it] }
            if (tex != null) drawTextured(scope, p, tex, alpha)
        }
    }

    private fun drawTextured(scope: DrawScope, p: Particle, tex: ImageBitmap, alpha: Float) {
        // Texture sprites use ADDITIVE blending (Plus). Reason: the AI-generated
        // textures sit on solid black backgrounds with no alpha channel — under
        // SrcOver every particle would paint a black square over the scene.
        // Plus makes black act as transparent (0 + x = x), and bright cores
        // light up. This is the standard VFX convention for particles.
        val lifeFrac = (p.lifeRemaining / p.lifeTotal).coerceIn(0f, 1f)
        // Sprites grow a bit at start, shrink at end — but never below 50%
        // of authoring radius, so impact bursts stay visible from frame 1.
        val sizeFactor = when (p.kind) {
            ParticleKind.MIST -> 0.8f + (1f - lifeFrac) * 0.7f       // grows
            ParticleKind.EMBER -> 0.6f + lifeFrac * 0.6f              // shrinks
            else -> 0.6f + lifeFrac * 0.6f                            // shrinks
        }
        // Sprite is at least 16px wide so it reads even on small phones.
        val side = (p.radius * 4f * sizeFactor).coerceAtLeast(16f)
        val half = side / 2f
        scope.drawImage(
            image = tex,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(tex.width, tex.height),
            dstOffset = IntOffset((p.x - half).toInt(), (p.y - half).toInt()),
            dstSize = IntSize(side.toInt().coerceAtLeast(2), side.toInt().coerceAtLeast(2)),
            alpha = alpha.coerceIn(0f, 1f),
            colorFilter = null,
            blendMode = BlendMode.Plus,
            filterQuality = FilterQuality.Low,
        )
    }

    private fun drawVector(scope: DrawScope, p: Particle, color: Color, lifeFrac: Float) {
        when (p.kind) {
            ParticleKind.SPARK -> scope.drawCircle(color, p.radius * lifeFrac, Offset(p.x, p.y))
            ParticleKind.EMBER -> scope.drawCircle(color, p.radius * (0.5f + lifeFrac * 0.5f), Offset(p.x, p.y), blendMode = BlendMode.Plus)
            ParticleKind.MIST -> scope.drawCircle(color, p.radius * (1.5f - lifeFrac), Offset(p.x, p.y))
            ParticleKind.SHARD -> scope.drawCircle(color, p.radius, Offset(p.x, p.y), style = Stroke(2f))
            ParticleKind.LEAF -> scope.drawCircle(color, p.radius * lifeFrac, Offset(p.x, p.y))
        }
    }
}

enum class ParticleKind { SPARK, EMBER, MIST, SHARD, LEAF }

internal class Particle {
    var x = 0f; var y = 0f
    var vx = 0f; var vy = 0f
    var lifeRemaining = 0f
    var lifeTotal = 1f
    var radius = 4f
    var color: Color = Color.White
    var gravity = 0f
    var rotation = 0f
    var kind = ParticleKind.SPARK
    var textureKey: String? = null
}
