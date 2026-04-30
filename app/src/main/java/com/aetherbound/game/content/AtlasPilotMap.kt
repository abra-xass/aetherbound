package com.aetherbound.game.content

import com.aetherbound.game.core.AtlasCell
import com.aetherbound.game.core.AtlasMap
import com.aetherbound.game.core.AtlasMapBuilder

/**
 * Namaris Harbor — atlas-based map. Hand-authored using the Abraxas
 * "Outside E" tileset. Layout follows classic 16-bit RPG town principles:
 * harbor (north) → town (center) → field (south).
 *
 * Atlas (col, row) constants below are initial guesses; refine after seeing
 * the rendered output. The atlas has 8 columns × ~422 rows of 32×32 cells.
 */
object AtlasPilotMap {

    // ── Initial guess atlas positions ────────────────────────────────────
    // These must be visually verified once the game runs. Update freely.
    private val GRASS = AtlasCell(0, 8)
    private val GRASS_TALL = AtlasCell(0, 12)
    private val SAND = AtlasCell(0, 20)
    private val PATH_DIRT = AtlasCell(0, 24)
    private val PATH_STONE = AtlasCell(0, 28)
    private val SEA_DEEP = AtlasCell(4, 8)
    private val SEA_SHALLOW = AtlasCell(4, 12)
    private val SHORE_FOAM = AtlasCell(4, 16)
    private val PIER = AtlasCell(2, 32)

    fun build(): AtlasMap {
        val cols = 33
        val rows = 24
        val b = AtlasMapBuilder(cols, rows, defaultGround = GRASS)

        // ── ZONE 1: HARBOR (rows 0..8) ───────────────────────────────────
        // deep sea top band
        b.groundRect(0, 0, cols, 3, SEA_DEEP)
        // shallow + foam transitional band
        b.groundRect(0, 3, cols, 1, SEA_SHALLOW)
        b.groundRect(0, 4, cols, 1, SHORE_FOAM)
        // sand strip
        b.groundRect(0, 5, cols, 2, SAND)
        // pier in the middle
        b.groundRect(13, 5, 7, 3, PIER)

        // sea cells solid (cannot walk)
        for (r in 0..3) for (c in 0 until cols) b.solid(c, r)

        // ── ZONE 2: TOWN (rows 7..16) ────────────────────────────────────
        // grass base
        b.groundRect(0, 7, cols, 10, GRASS)
        // central plaza of stone path
        b.groundRect(8, 11, 17, 4, PATH_STONE)
        // dirt path going north and south
        b.groundRect(15, 7, 3, 4, PATH_DIRT)
        b.groundRect(15, 15, 3, 4, PATH_DIRT)

        // four buildings: 2 north of plaza, 2 south
        b.place(PilotBuildings.SmallHouse, anchorCol = 4, anchorRow = 8)
        b.place(PilotBuildings.SmallHouse, anchorCol = 22, anchorRow = 8)
        b.place(PilotBuildings.LargeHouse, anchorCol = 4, anchorRow = 16)
        b.place(PilotBuildings.LargeHouse, anchorCol = 21, anchorRow = 16)

        // ── ZONE 3: FIELD (rows 17..23) ──────────────────────────────────
        // grass with three encounter zones
        for (r in 18..21) {
            for (c in 1..8) {
                b.ground(c, r, GRASS_TALL)
                b.encounter(c, r)
            }
            for (c in 12..18) {
                if (c in 14..16) {
                    b.ground(c, r, GRASS_TALL); b.encounter(c, r)
                }
            }
            for (c in 22..30) {
                b.ground(c, r, GRASS_TALL); b.encounter(c, r)
            }
        }

        return b.build()
    }
}
