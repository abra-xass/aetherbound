package com.aetherbound.game.core

/**
 * Theme of a single Screen — drives encounter pool, ambient palette,
 * music in the future. Each Screen has exactly one theme.
 */
enum class ScreenTheme {
    Town, Harbor, Forest, Mountain, Marsh, Desert, Castle,
    Plains, Beach, Cave, Sanctum,
}

/** Direction of a cross-screen exit. */
enum class Edge { North, South, East, West }

/**
 * One cell of the WorldGrid. Holds its own AtlasMap (the tile data),
 * its theme, and an optional landmark name.
 */
data class Screen(
    val gridCol: Int,
    val gridRow: Int,
    val theme: ScreenTheme,
    val map: AtlasMap,
    val landmark: String? = null,
    val regionSeed: Long = (gridCol * 1000L + gridRow) * 1_000_003L,
) {
    val cols: Int get() = map.cols
    val rows: Int get() = map.rows
}

/**
 * The full game world: a 2D grid of Screens. Sparse — not every (col, row)
 * needs to be populated; missing cells block movement at that edge.
 *
 * Edge transitions: when the player walks off the east edge of screen
 * (gridCol, gridRow), the engine switches to (gridCol+1, gridRow) and
 * places the player at localCol=0 with the same localRow (mirror across).
 */
data class WorldGrid(
    val cols: Int,
    val rows: Int,
    val screens: Map<Pair<Int, Int>, Screen>,
    val startScreen: Pair<Int, Int>,
    val startLocalCol: Int,
    val startLocalRow: Int,
) {
    fun screenAt(col: Int, row: Int): Screen? = screens[col to row]

    fun neighbor(of: Screen, edge: Edge): Screen? = when (edge) {
        Edge.North -> screenAt(of.gridCol, of.gridRow - 1)
        Edge.South -> screenAt(of.gridCol, of.gridRow + 1)
        Edge.East -> screenAt(of.gridCol + 1, of.gridRow)
        Edge.West -> screenAt(of.gridCol - 1, of.gridRow)
    }
}

/**
 * Computes the next screen + local position when the player walks off an
 * edge. Returns null if the neighbor doesn't exist (movement blocked).
 *
 * Mirror rule: leaving the east edge enters the next screen from the west,
 * so localRow is preserved and localCol jumps to 0 (or to the rightmost
 * column for west-exits, similarly for north/south).
 */
fun WorldGrid.crossEdge(
    current: Screen,
    edge: Edge,
    localCol: Int,
    localRow: Int,
): Triple<Screen, Int, Int>? {
    val next = neighbor(current, edge) ?: return null
    val (nc, nr) = when (edge) {
        Edge.East -> 0 to localRow.coerceIn(0, next.rows - 1)
        Edge.West -> (next.cols - 1) to localRow.coerceIn(0, next.rows - 1)
        Edge.North -> localCol.coerceIn(0, next.cols - 1) to (next.rows - 1)
        Edge.South -> localCol.coerceIn(0, next.cols - 1) to 0
    }
    return Triple(next, nc, nr)
}
