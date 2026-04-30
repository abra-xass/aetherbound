package com.aetherbound.game.core

/**
 * Atlas-based map data. Replaces the per-cell CoastalTile char system with
 * direct (col, row) references into a single tileset PNG. This is the
 * RPG Maker / Tiled approach: maps reference atlas positions, the engine
 * slices them at draw time. Allows multi-tile entities (buildings) to be
 * authored as 3×3 (or NxM) blocks of coordinated atlas cells.
 */
data class AtlasCell(val col: Int, val row: Int)

const val ATLAS_TILE_PX = 32

/**
 * A two-layer tilemap: ground (every cell has one) + overlay (optional, for
 * buildings and props). Solid + encounter sets reference grid coordinates.
 */
data class AtlasMap(
    val cols: Int,
    val rows: Int,
    val ground: List<List<AtlasCell>>,
    val overlay: List<List<AtlasCell?>>,
    val solid: Set<Pair<Int, Int>>,
    val encounter: Set<Pair<Int, Int>>,
)

/**
 * A multi-tile building entity. The anchor places the top-left cell at
 * (anchorCol, anchorRow); the rows/cols arrays span its full extent.
 * doorCol/doorRow indicate the walkable entrance cell (relative to anchor).
 */
data class Building(
    val width: Int,
    val height: Int,
    val cells: List<List<AtlasCell>>,   // [row][col] = AtlasCell
    val doorCol: Int,
    val doorRow: Int,
)

/**
 * Place a Building into an AtlasMap.Builder at the given anchor. All
 * non-door cells are marked solid; the door cell stays walkable.
 */
class AtlasMapBuilder(val cols: Int, val rows: Int, defaultGround: AtlasCell) {
    private val ground = MutableList(rows) { MutableList(cols) { defaultGround } }
    private val overlay = MutableList(rows) { MutableList<AtlasCell?>(cols) { null } }
    private val solid = mutableSetOf<Pair<Int, Int>>()
    private val encounter = mutableSetOf<Pair<Int, Int>>()

    fun ground(c: Int, r: Int, cell: AtlasCell) = apply { if (inBounds(c, r)) ground[r][c] = cell }
    fun overlay(c: Int, r: Int, cell: AtlasCell?) = apply { if (inBounds(c, r)) overlay[r][c] = cell }
    fun solid(c: Int, r: Int) = apply { if (inBounds(c, r)) solid += c to r }
    fun encounter(c: Int, r: Int) = apply { if (inBounds(c, r)) encounter += c to r }

    /** Fill a rect with a single ground tile. */
    fun groundRect(c0: Int, r0: Int, w: Int, h: Int, cell: AtlasCell) = apply {
        for (r in r0 until (r0 + h)) for (c in c0 until (c0 + w)) ground(c, r, cell)
    }

    fun place(building: Building, anchorCol: Int, anchorRow: Int) = apply {
        for (r in 0 until building.height) {
            for (c in 0 until building.width) {
                val gx = anchorCol + c
                val gy = anchorRow + r
                if (!inBounds(gx, gy)) continue
                overlay(gx, gy, building.cells[r][c])
                if (c == building.doorCol && r == building.doorRow) {
                    // door cell stays walkable
                } else {
                    solid(gx, gy)
                }
            }
        }
    }

    fun build(): AtlasMap = AtlasMap(
        cols = cols, rows = rows,
        ground = ground.map { it.toList() },
        overlay = overlay.map { it.toList() },
        solid = solid.toSet(),
        encounter = encounter.toSet(),
    )

    private fun inBounds(c: Int, r: Int): Boolean = c in 0 until cols && r in 0 until rows
}
