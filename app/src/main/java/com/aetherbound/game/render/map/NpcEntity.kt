package com.aetherbound.game.render.map

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.IntOffset
import kotlin.random.Random

/**
 * A single NPC walking on a map. Owns its own position + facing + step
 * timer so multiple NPCs can drive themselves independently from the
 * world's frame loop.
 *
 * Behaviour modes (matches Tuxemon's `behaviour` object property):
 *
 *   - **static**  : stands still, faces [initialFacing]
 *   - **wander**  : every wanderIntervalMs ticks rolls a random direction
 *                   and tries to step (skips if blocked)
 *   - **patrol**  : walks back-and-forth along the [initialFacing] axis,
 *                   reverses when it hits an obstacle or after
 *                   patrolDistance tiles
 */
@Stable
class NpcEntity(
    val id: String,
    val sheetAssetPath: String,
    initialTileX: Int,
    initialTileY: Int,
    val behaviour: String,
    val initialFacing: MovementController.Facing = MovementController.Facing.SOUTH,
    val trainerId: String? = null,
    val dialog: String = "",
    private val wanderIntervalMs: Int = 2200,
    private val patrolDistance: Int = 4,
) {
    val controller = MovementController(initialTileX, initialTileY).apply {
        warpTo(initialTileX, initialTileY, initialFacing)
    }

    private var timeSinceLastDecisionMs: Int = 0
    private var patrolStepsTaken: Int = 0
    private var patrolForward: Boolean = true

    val tileX: Int get() = controller.tileX
    val tileY: Int get() = controller.tileY
    val facing: MovementController.Facing get() = controller.facing
    val moving: Boolean get() = controller.moving

    /** Convenience for renderers — current pixel offset within a step. */
    val pixelOffset: IntOffset get() = controller.pixelOffset

    /**
     * Advance the NPC. Caller passes the same dt as the world loop. The NPC
     * decides on its own when to start a step, picks the direction, and
     * lets [MovementController] handle the slide animation.
     */
    fun tick(deltaMs: Long, collision: CollisionMap?, rng: Random) {
        if (controller.moving) {
            controller.tick(deltaMs, collision)
            return
        }
        if (behaviour == "static") return

        timeSinceLastDecisionMs += deltaMs.toInt()
        if (timeSinceLastDecisionMs < wanderIntervalMs) return
        timeSinceLastDecisionMs = 0

        val direction = when (behaviour) {
            "wander" -> MovementController.Facing.values().random(rng)
            "patrol" -> {
                if (patrolStepsTaken >= patrolDistance) {
                    patrolForward = !patrolForward
                    patrolStepsTaken = 0
                }
                patrolStepsTaken++
                if (patrolForward) initialFacing else initialFacing.opposite()
            }
            else -> return
        }
        controller.requestStep(direction, collision)
    }
}

private fun MovementController.Facing.opposite(): MovementController.Facing = when (this) {
    MovementController.Facing.NORTH -> MovementController.Facing.SOUTH
    MovementController.Facing.SOUTH -> MovementController.Facing.NORTH
    MovementController.Facing.EAST -> MovementController.Facing.WEST
    MovementController.Facing.WEST -> MovementController.Facing.EAST
}

object NpcSpawner {
    /**
     * Reads NPC objects out of [map] and produces walkable entities. Tuxemon
     * declares NPCs via object-layer entries with type `"interact_act"` /
     * `"npc"` / `"trainer"` and a `behaviour` property.
     */
    fun fromMap(map: TmxMap): List<NpcEntity> {
        val out = mutableListOf<NpcEntity>()
        for (layer in map.objectLayers) {
            for (obj in layer.objects) {
                val event = ObjectDispatcher.classify(obj)
                if (event !is WorldEvent.Npc) continue
                val tx = (obj.x / map.tileWidth).toInt()
                val ty = (obj.y / map.tileHeight).toInt()
                val sprite = "game/characters/tuxemon/${event.sprite}/sheet.png"
                out += NpcEntity(
                    id = "${obj.id}",
                    sheetAssetPath = sprite,
                    initialTileX = tx,
                    initialTileY = ty,
                    behaviour = event.moveType,
                    trainerId = event.trainerId,
                    dialog = event.dialog,
                )
            }
        }
        return out
    }
}
