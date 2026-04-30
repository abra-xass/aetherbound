package com.aetherbound.game.render.map

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.IntOffset

/**
 * Tile-by-tile player movement on top of a [CollisionMap].
 *
 * The controller is grid-locked: while a step is in flight (smooth sliding
 * between cells), additional input queues a single follow-up direction.
 * This matches Tuxemon's / Pokémon's classic movement feel.
 *
 *   step time = STEP_MS (default 220ms)
 *   one tile per step
 *
 * Caller drives the controller with [tick] each frame and [requestStep]
 * each input event. [pixelOffset] is what the renderer should pass to
 * [TmxScene.cameraPx] (or its equivalent) to render the player at the
 * correct sub-tile position.
 */
@Stable
class MovementController(
    initialTileX: Int,
    initialTileY: Int,
    private val tileWidth: Int = 16,
    private val tileHeight: Int = 16,
    private val stepMs: Int = STEP_MS,
) {
    enum class Facing { NORTH, SOUTH, EAST, WEST }

    var tileX: Int = initialTileX; private set
    var tileY: Int = initialTileY; private set
    var facing: Facing = Facing.SOUTH; private set
    var moving: Boolean = false; private set

    /** Sub-tile pixel offset for smooth rendering, 0..tileW/H during a step. */
    var pixelOffset: IntOffset = IntOffset.Zero; private set

    private var stepProgress: Float = 0f
    private var stepFromX: Int = initialTileX
    private var stepFromY: Int = initialTileY
    private var queued: Facing? = null

    /**
     * Advance the controller. Returns true if the player just completed a
     * step (callers can use this to roll an [EncounterEngine] check).
     */
    fun tick(deltaMs: Long, collision: CollisionMap?): Boolean {
        if (!moving) {
            // Try to start a queued move.
            val next = queued ?: return false
            queued = null
            return startStep(next, collision)
        }

        stepProgress += deltaMs.toFloat() / stepMs
        if (stepProgress >= 1f) {
            stepProgress = 0f
            moving = false
            pixelOffset = IntOffset.Zero
            // Step completed — try queued direction next frame.
            return true
        }

        val dx = (tileX - stepFromX) * tileWidth
        val dy = (tileY - stepFromY) * tileHeight
        pixelOffset = IntOffset(
            x = (dx * stepProgress).toInt(),
            y = (dy * stepProgress).toInt(),
        )
        return false
    }

    /** Request a step in [direction]. Held buttons should call this every frame. */
    fun requestStep(direction: Facing, collision: CollisionMap?) {
        if (moving) {
            queued = direction
        } else {
            startStep(direction, collision)
        }
    }

    /** Snap the player to a new tile (e.g. after a warp). */
    fun warpTo(tx: Int, ty: Int, newFacing: Facing = facing) {
        tileX = tx
        tileY = ty
        facing = newFacing
        moving = false
        stepProgress = 0f
        pixelOffset = IntOffset.Zero
        queued = null
    }

    /** Camera-px offset that follows the player (top-left). */
    fun cameraPx(): IntOffset = IntOffset(
        x = tileX * tileWidth - pixelOffset.x,
        y = tileY * tileHeight - pixelOffset.y,
    )

    /** Pixel position for sprite rendering at top-left of the player tile. */
    fun spritePx(): IntOffset = IntOffset(
        x = stepFromX * tileWidth + pixelOffset.x,
        y = stepFromY * tileHeight + pixelOffset.y,
    )

    private fun startStep(direction: Facing, collision: CollisionMap?): Boolean {
        facing = direction
        val (dx, dy) = when (direction) {
            Facing.NORTH -> 0 to -1
            Facing.SOUTH -> 0 to 1
            Facing.EAST -> 1 to 0
            Facing.WEST -> -1 to 0
        }
        val targetX = tileX + dx
        val targetY = tileY + dy
        if (collision != null && collision.isBlocked(targetX, targetY)) {
            // Bumped a wall — face the direction but don't move.
            return false
        }
        stepFromX = tileX
        stepFromY = tileY
        tileX = targetX
        tileY = targetY
        moving = true
        stepProgress = 0f
        return false  // not yet "step completed"
    }

    companion object {
        const val STEP_MS = 220
    }
}
