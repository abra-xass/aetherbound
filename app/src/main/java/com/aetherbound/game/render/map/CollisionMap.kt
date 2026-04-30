package com.aetherbound.game.render.map

/**
 * Tile-grid collision oracle derived from a [TmxMap].
 *
 * Tuxemon's maps mark walkability in two ways:
 *   1. **Object layer "Collisions"** — `TmxObject` rectangles that block
 *      movement (water, walls, NPCs).
 *   2. **Tile properties** — per-tileset, tiles can have a `collide` boolean
 *      property. We don't currently parse those (would require .tsx tile
 *      property scan); the object-layer path covers ~90% of Tuxemon maps.
 *
 * Coordinates here are **tile-space** (cells), not pixels.
 */
class CollisionMap private constructor(
    val widthTiles: Int,
    val heightTiles: Int,
    private val blocked: BooleanArray,
) {

    /** True if [tx,ty] cannot be entered. Out-of-bounds counts as blocked. */
    fun isBlocked(tx: Int, ty: Int): Boolean {
        if (tx < 0 || ty < 0 || tx >= widthTiles || ty >= heightTiles) return true
        return blocked[ty * widthTiles + tx]
    }

    fun isWalkable(tx: Int, ty: Int): Boolean = !isBlocked(tx, ty)

    /** Marks one tile as blocked/unblocked (e.g. NPC moves on/off a cell). */
    fun setBlocked(tx: Int, ty: Int, value: Boolean) {
        if (tx < 0 || ty < 0 || tx >= widthTiles || ty >= heightTiles) return
        blocked[ty * widthTiles + tx] = value
    }

    /** Bulk export for save-state. */
    fun snapshot(): BooleanArray = blocked.copyOf()

    companion object {
        /**
         * Builds a [CollisionMap] from [map]. Recognised object-layer names
         * (case-insensitive): "collision", "collisions", "blocks", "walls".
         */
        fun fromMap(map: TmxMap): CollisionMap {
            val w = map.width
            val h = map.height
            val blocked = BooleanArray(w * h)

            val collisionLayers = map.objectLayers.filter { layer ->
                layer.name.lowercase() in COLLISION_LAYER_NAMES ||
                    layer.objects.any { it.type.lowercase() == "collision" }
            }
            for (layer in collisionLayers) {
                for (obj in layer.objects) {
                    val left = (obj.x / map.tileWidth).toInt()
                    val top = (obj.y / map.tileHeight).toInt()
                    val widthCells = ((obj.width / map.tileWidth).toInt()).coerceAtLeast(1)
                    val heightCells = ((obj.height / map.tileHeight).toInt()).coerceAtLeast(1)
                    for (dy in 0 until heightCells) {
                        for (dx in 0 until widthCells) {
                            val tx = left + dx
                            val ty = top + dy
                            if (tx in 0 until w && ty in 0 until h) {
                                blocked[ty * w + tx] = true
                            }
                        }
                    }
                }
            }
            return CollisionMap(w, h, blocked)
        }

        private val COLLISION_LAYER_NAMES = setOf("collision", "collisions", "blocks", "walls", "obstacles")
    }
}
