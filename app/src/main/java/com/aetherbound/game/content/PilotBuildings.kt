package com.aetherbound.game.content

import com.aetherbound.game.core.AtlasCell
import com.aetherbound.game.core.Building

/**
 * Multi-tile building entities. Each is a 3×3 (or larger) block of
 * coordinated AtlasCell references; the door cell stays walkable.
 *
 * Atlas (col, row) values below are INITIAL GUESSES based on common RPG
 * Maker MV/MZ "Outside_E" custom layouts (8-col atlas, ~422 rows).
 * Refine after running and seeing what each (col, row) actually shows.
 */
object PilotBuildings {

    /** Generic small house, 3×3, door at bottom-center. */
    val SmallHouse = Building(
        width = 3, height = 3,
        cells = listOf(
            // roof row (top)
            listOf(AtlasCell(0, 0), AtlasCell(1, 0), AtlasCell(2, 0)),
            // wall row (middle)
            listOf(AtlasCell(0, 1), AtlasCell(1, 1), AtlasCell(2, 1)),
            // base row (bottom) — center cell is the door
            listOf(AtlasCell(0, 2), AtlasCell(1, 2), AtlasCell(2, 2)),
        ),
        doorCol = 1, doorRow = 2,
    )

    /** Larger house, 4×4, door at bottom-center-left. */
    val LargeHouse = Building(
        width = 4, height = 4,
        cells = listOf(
            listOf(AtlasCell(3, 0), AtlasCell(4, 0), AtlasCell(5, 0), AtlasCell(6, 0)),
            listOf(AtlasCell(3, 1), AtlasCell(4, 1), AtlasCell(5, 1), AtlasCell(6, 1)),
            listOf(AtlasCell(3, 2), AtlasCell(4, 2), AtlasCell(5, 2), AtlasCell(6, 2)),
            listOf(AtlasCell(3, 3), AtlasCell(4, 3), AtlasCell(5, 3), AtlasCell(6, 3)),
        ),
        doorCol = 1, doorRow = 3,
    )
}
