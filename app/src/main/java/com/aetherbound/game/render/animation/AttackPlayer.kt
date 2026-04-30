package com.aetherbound.game.render.animation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import com.aetherbound.game.render.particle.ParticleKind
import com.aetherbound.game.render.particle.ParticleSystem
import com.aetherbound.game.render.theme.aspectColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Drives a recipe forward in real time. Stateless — caller passes elapsedMs.
 * The caller's Canvas calls draw() with the current progress.
 */
class AttackAnimationState(
    val recipe: AnimationRecipe,
    val origin: Offset,
    val target: Offset,
    val particles: ParticleSystem,
    seed: Long,
) {
    val rng: Random = Random(seed)
    var elapsedMs: Float = 0f
    val progress: Float get() = (elapsedMs / recipe.effectiveDurationMs).coerceIn(0f, 1f)
    val finished: Boolean get() = elapsedMs >= recipe.effectiveDurationMs
    var hitFired: Boolean = false
}

@Composable
fun rememberAttackState(
    recipe: AnimationRecipe,
    origin: Offset,
    target: Offset,
    particles: ParticleSystem,
    seed: Long,
): AttackAnimationState = remember(recipe.techniqueId, seed) {
    AttackAnimationState(recipe, origin, target, particles, seed)
}

/** Advance the animation; returns true if the hit-frame just fired this tick. */
fun AttackAnimationState.advance(dtMs: Float): Boolean {
    val before = elapsedMs
    elapsedMs += dtMs
    particles.update(dtMs)
    val justHit = !hitFired && before < recipe.effectiveHitFrameMs && elapsedMs >= recipe.effectiveHitFrameMs
    if (justHit) {
        hitFired = true
        emitImpactBurst()
    }
    if (!hitFired && elapsedMs > 0f) emitProjectileTrail(dtMs)
    return justHit
}

/** Maps a particle kind + element aspect to the matching texture key in
 *  assets/game/particles/. Returns null when caller should fall back to the
 *  vector circle. */
private fun textureFor(aspect: com.aetherbound.game.core.Aspect, kind: ParticleKind): String {
    val type = when (kind) {
        ParticleKind.SPARK -> "spark"
        ParticleKind.EMBER -> "orb"
        ParticleKind.MIST -> "background_glow"
        ParticleKind.SHARD -> "shard"
        ParticleKind.LEAF -> "sub_particle"
    }
    return com.aetherbound.game.render.particle.ParticleTextures.key(aspect, type)
}

private fun AttackAnimationState.emitProjectileTrail(dtMs: Float) {
    val (cPrimary, cSecondary) = aspectColors(recipe.aspect)
    val pos = projectilePos(progress.coerceAtMost(recipe.effectiveHitFrameMs.toFloat() / recipe.effectiveDurationMs))
    val perTickBudget = (recipe.effectiveParticleBudget * dtMs / recipe.effectiveDurationMs).toInt().coerceAtLeast(1)
    val direction = kotlin.math.atan2(target.y - origin.y, target.x - origin.x)
    when (recipe.projectilePath) {
        ProjectilePath.ARC, ProjectilePath.BEAM, ProjectilePath.HOMING ->
            particles.emit(pos, direction + PI.toFloat(), 0.6f, 30f, 90f,
                lifeMs = 380, radiusPx = 4f, color = cSecondary, gravity = 60f,
                rng = rng, count = perTickBudget, kind = ParticleKind.EMBER, textureKey = textureFor(recipe.aspect, ParticleKind.EMBER))
        ProjectilePath.MULTI ->
            particles.emit(pos, rng.nextFloat() * 6.28f, 3.14f, 50f, 140f,
                lifeMs = 320, radiusPx = 3f, color = cPrimary,
                rng = rng, count = perTickBudget, kind = ParticleKind.SPARK, textureKey = textureFor(recipe.aspect, ParticleKind.SPARK))
        ProjectilePath.SPIRAL ->
            particles.emit(pos, direction, 0.3f, 60f, 120f,
                lifeMs = 360, radiusPx = 3f, color = cSecondary,
                rng = rng, count = perTickBudget, kind = ParticleKind.SPARK, textureKey = textureFor(recipe.aspect, ParticleKind.SPARK))
        ProjectilePath.WAVE ->
            particles.emit(pos, direction + PI.toFloat(), 0.4f, 20f, 60f,
                lifeMs = 540, radiusPx = 6f, color = cSecondary,
                rng = rng, count = perTickBudget, kind = ParticleKind.MIST, textureKey = textureFor(recipe.aspect, ParticleKind.MIST))
        ProjectilePath.AURA_SELF ->
            particles.emit(origin, rng.nextFloat() * 6.28f, 3.14f, 30f, 70f,
                lifeMs = 520, radiusPx = 5f, color = cPrimary,
                rng = rng, count = perTickBudget, kind = ParticleKind.LEAF, textureKey = textureFor(recipe.aspect, ParticleKind.LEAF))
        ProjectilePath.GROUND_RUSH ->
            particles.emit(pos, direction + PI.toFloat() / 2, 0.2f, 0f, 30f,
                lifeMs = 440, radiusPx = 5f, color = cSecondary,
                rng = rng, count = perTickBudget, kind = ParticleKind.MIST, textureKey = textureFor(recipe.aspect, ParticleKind.MIST))
    }
}

private fun AttackAnimationState.emitImpactBurst() {
    val (cPrimary, cSecondary) = aspectColors(recipe.aspect)
    when (recipe.impactVisual) {
        ImpactVisual.SHATTER -> particles.emit(target, 0f, 3.14f, 200f, 360f,
            lifeMs = 480, radiusPx = 4f, color = cPrimary, rng = rng, count = recipe.effectiveParticleBudget,
            kind = ParticleKind.SHARD, textureKey = textureFor(recipe.aspect, ParticleKind.SHARD))
        ImpactVisual.RING_POP -> particles.emit(target, 0f, 3.14f, 260f, 320f,
            lifeMs = 420, radiusPx = 5f, color = cSecondary, rng = rng, count = recipe.effectiveParticleBudget,
            kind = ParticleKind.SPARK, textureKey = textureFor(recipe.aspect, ParticleKind.SPARK))
        ImpactVisual.BLOOM -> particles.emit(target, 0f, 3.14f, 80f, 200f,
            lifeMs = 640, radiusPx = 8f, color = cPrimary, rng = rng, count = recipe.effectiveParticleBudget,
            kind = ParticleKind.EMBER, textureKey = textureFor(recipe.aspect, ParticleKind.EMBER))
        ImpactVisual.CRATER -> particles.emit(target, -1.57f, 1.0f, 180f, 360f,
            lifeMs = 540, radiusPx = 5f, color = cPrimary, rng = rng, count = recipe.effectiveParticleBudget,
            kind = ParticleKind.SHARD, textureKey = textureFor(recipe.aspect, ParticleKind.SHARD))
        ImpactVisual.FREEZE -> particles.emit(target, 0f, 3.14f, 90f, 220f,
            lifeMs = 700, radiusPx = 6f, color = cSecondary, rng = rng, count = recipe.effectiveParticleBudget,
            kind = ParticleKind.MIST, textureKey = textureFor(recipe.aspect, ParticleKind.MIST))
        ImpactVisual.BIND_VINES -> particles.emit(target, 0f, 3.14f, 60f, 160f,
            lifeMs = 800, radiusPx = 6f, color = cPrimary, rng = rng, count = recipe.effectiveParticleBudget,
            kind = ParticleKind.LEAF, textureKey = textureFor(recipe.aspect, ParticleKind.LEAF))
        ImpactVisual.SPARK_FORK -> particles.emit(target, 0f, 3.14f, 240f, 420f,
            lifeMs = 360, radiusPx = 3f, color = cSecondary, rng = rng, count = recipe.effectiveParticleBudget,
            kind = ParticleKind.SPARK, textureKey = textureFor(recipe.aspect, ParticleKind.SPARK))
        ImpactVisual.MIST -> particles.emit(target, 0f, 3.14f, 50f, 140f,
            lifeMs = 720, radiusPx = 8f, color = cSecondary, rng = rng, count = recipe.effectiveParticleBudget,
            kind = ParticleKind.MIST, textureKey = textureFor(recipe.aspect, ParticleKind.MIST))
    }
}

/** Position of the "projectile head" along the path at progress p∈[0,1]. */
fun AttackAnimationState.projectilePos(p: Float): Offset {
    val t = p.coerceIn(0f, 1f)
    val dx = target.x - origin.x
    val dy = target.y - origin.y
    return when (recipe.projectilePath) {
        ProjectilePath.ARC -> {
            val arcLift = -240f * (4f * t * (1f - t))
            Offset(origin.x + dx * t, origin.y + dy * t + arcLift)
        }
        ProjectilePath.BEAM -> Offset(origin.x + dx * t, origin.y + dy * t)
        ProjectilePath.HOMING -> {
            val wobble = sin(t * 8f) * 30f * (1f - t)
            Offset(origin.x + dx * t, origin.y + dy * t + wobble)
        }
        ProjectilePath.MULTI -> Offset(origin.x + dx * t, origin.y + dy * t)
        ProjectilePath.SPIRAL -> {
            val r = 40f * (1f - t)
            val angle = t * 14f
            Offset(origin.x + dx * t + cos(angle) * r, origin.y + dy * t + sin(angle) * r)
        }
        ProjectilePath.WAVE -> {
            val wave = sin(t * 6f * PI.toFloat()) * 30f
            Offset(origin.x + dx * t, origin.y + dy * t + wave)
        }
        ProjectilePath.AURA_SELF -> origin
        ProjectilePath.GROUND_RUSH -> Offset(origin.x + dx * t, origin.y + 60f)
    }
}

/** Draws projectile head shape (so even with zero particles you see something). */
fun DrawScope.drawProjectileHead(state: AttackAnimationState) {
    if (state.hitFired || state.elapsedMs < 1f) return
    val p = (state.elapsedMs / state.recipe.effectiveHitFrameMs).coerceIn(0f, 1f)
    val pos = state.projectilePos(p)
    val (cPrimary, cSecondary) = aspectColors(state.recipe.aspect)
    when (state.recipe.projectilePath) {
        ProjectilePath.BEAM -> {
            val from = state.origin
            val to = pos
            drawLine(
                Brush.linearGradient(listOf(cPrimary, cSecondary), start = from, end = to),
                start = from, end = to, strokeWidth = 6f, blendMode = BlendMode.Plus,
            )
        }
        ProjectilePath.AURA_SELF -> {
            val r = 60f + 30f * sin(state.elapsedMs / 60f)
            drawCircle(cPrimary.copy(alpha = 0.35f), r, state.origin, blendMode = BlendMode.Plus)
        }
        ProjectilePath.GROUND_RUSH -> {
            val seg = Path().apply {
                moveTo(state.origin.x, state.origin.y + 60f)
                lineTo(pos.x, pos.y)
            }
            drawPath(seg, color = cSecondary.copy(alpha = 0.6f), style = Stroke(8f))
        }
        else -> {
            drawCircle(cSecondary, 10f, pos, blendMode = BlendMode.Plus)
            drawCircle(cPrimary, 5f, pos, blendMode = BlendMode.Plus)
        }
    }
}

/** Caster motion offset: returns where the caster sprite should be relative to its rest pos. */
fun AttackAnimationState.casterOffset(facingRight: Boolean): Offset {
    val t = (elapsedMs / recipe.effectiveHitFrameMs.toFloat()).coerceIn(0f, 1f)
    val flip = if (facingRight) 1f else -1f
    return when (recipe.casterMotion) {
        CasterMotion.LUNGE -> Offset(flip * 80f * easeOutBack(t) * (1f - t), 0f)
        CasterMotion.BRACE -> Offset(-flip * 30f * (1f - t), 10f * t)
        CasterMotion.DRAW -> Offset(-flip * 30f * t, 0f)
        CasterMotion.SPIN -> Offset(0f, -20f * sin(t * PI.toFloat() * 2f))
        CasterMotion.FLOAT -> Offset(0f, -40f * t)
        CasterMotion.DIVE -> Offset(flip * 60f * t, 30f * t)
        CasterMotion.SLAM -> Offset(0f, 40f * t)
    }
}

private fun easeOutBack(t: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val u = t - 1f
    return 1f + c3 * u * u * u + c1 * u * u
}

/** Hit reaction offset for the defender. */
fun AttackAnimationState.defenderImpactOffset(): Offset {
    if (!hitFired) return Offset.Zero
    val tSinceHit = (elapsedMs - recipe.effectiveHitFrameMs).coerceAtLeast(0f)
    val falloff = (1f - tSinceHit / 320f).coerceIn(0f, 1f)
    return Offset(
        x = sin(tSinceHit / 8f) * 14f * falloff,
        y = -cos(tSinceHit / 8f) * 6f * falloff,
    )
}

/** Camera shake decays from the hit frame for ~280ms. */
@Composable
fun rememberCameraShake(state: AttackAnimationState): Offset {
    var shake by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(state) {
        var last = 0f
        while (!state.finished) {
            val dt = state.elapsedMs - last
            last = state.elapsedMs
            shake = if (state.hitFired) {
                val dur = (state.elapsedMs - state.recipe.effectiveHitFrameMs).coerceAtLeast(0f)
                state.recipe.effectiveCameraShake * (1f - dur / 280f).coerceIn(0f, 1f)
            } else 0f
            kotlinx.coroutines.delay(16)
            if (dt < 0f) break
        }
        shake = 0f
    }
    return Offset(shake * 18f * (state.rng.nextFloat() - 0.5f), shake * 18f * (state.rng.nextFloat() - 0.5f))
}

/** Defender flash on hit. */
fun DrawScope.drawHitFlash(state: AttackAnimationState, area: Size, anchor: Offset) {
    if (!state.hitFired) return
    val tSinceHit = (state.elapsedMs - state.recipe.effectiveHitFrameMs).coerceAtLeast(0f)
    val alpha = (1f - tSinceHit / 200f).coerceIn(0f, 1f) * 0.55f
    if (alpha <= 0f) return
    val (cPrimary, _) = aspectColors(state.recipe.aspect)
    translate(anchor.x, anchor.y) {
        drawCircle(cPrimary.copy(alpha = alpha), 90f, Offset.Zero, blendMode = BlendMode.Plus)
    }
}
