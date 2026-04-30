package com.aetherbound.game.content

import com.aetherbound.game.core.AtlasCell
import com.aetherbound.game.core.AtlasMap
import com.aetherbound.game.core.AtlasMapBuilder
import com.aetherbound.game.core.Building
import com.aetherbound.game.core.Screen
import com.aetherbound.game.core.ScreenTheme
import com.aetherbound.game.core.WorldGrid

/**
 * The Aetherbound pilot world: a 3×3 grid of 32×32 screens. Each screen has
 * a distinct theme, layout, and encounter pool. Player starts in the center
 * (Harbor) and can walk off any edge to enter the neighbour screen.
 *
 *      (0,0) Forest NW    (1,0) Mountain N    (2,0) Forest NE
 *      (0,1) Town W       (1,1) Harbor C      (2,1) Castle E
 *      (0,2) Beach SW     (1,2) Sanctum S     (2,2) Field SE
 *
 * All atlas (col, row) positions in this file are INITIAL GUESSES against
 * the Abraxas Outside_E.png layout. After visual verification on device,
 * adjust the constants in [Tiles] and rebuild.
 */
object PilotWorld {

    // Screen dimensions — each screen is a self-contained 32×32 area.
    const val W = 32
    const val H = 32

    /** Atlas-position constants — refined as the player visually verifies. */
    private object Tiles {
        // GROUND
        val GRASS = AtlasCell(0, 8)
        val GRASS_TALL = AtlasCell(0, 12)         // encounter zone marker
        val SAND = AtlasCell(0, 20)
        val PATH_DIRT = AtlasCell(0, 24)
        val PATH_STONE = AtlasCell(0, 28)
        val ROCK_FLOOR = AtlasCell(2, 28)
        val SNOW = AtlasCell(0, 4)
        val WATER_DEEP = AtlasCell(4, 8)
        val WATER_SHALLOW = AtlasCell(4, 12)
        val SHORE_FOAM = AtlasCell(4, 16)
        val PIER = AtlasCell(2, 32)
        val MARSH = AtlasCell(4, 24)

        // PROPS / overlay decorations
        val TREE = AtlasCell(6, 36)
        val BUSH = AtlasCell(6, 40)
        val ROCK = AtlasCell(6, 44)
        val FLOWER_RED = AtlasCell(6, 48)
        val FLOWER_BLUE = AtlasCell(7, 48)
        val LANTERN = AtlasCell(6, 52)
        val SIGN = AtlasCell(6, 56)
        val BENCH = AtlasCell(6, 60)
        val FOUNTAIN_TOP = AtlasCell(0, 64)
        val FOUNTAIN_BOTTOM = AtlasCell(0, 68)
        val STATUE = AtlasCell(2, 64)
        val CASTLE_WALL = AtlasCell(0, 72)
        val CASTLE_GATE = AtlasCell(2, 72)
        val BRIDGE = AtlasCell(4, 72)
    }

    /** Building catalog — multi-tile entities. */
    private object Buildings {
        val Cottage = Building(
            width = 3, height = 3,
            cells = listOf(
                listOf(AtlasCell(0, 0), AtlasCell(1, 0), AtlasCell(2, 0)),  // roof
                listOf(AtlasCell(0, 1), AtlasCell(1, 1), AtlasCell(2, 1)),  // wall+window+wall
                listOf(AtlasCell(0, 2), AtlasCell(1, 2), AtlasCell(2, 2)),  // wall+door+wall
            ),
            doorCol = 1, doorRow = 2,
        )
        val Manor = Building(
            width = 4, height = 3,
            cells = listOf(
                listOf(AtlasCell(3, 0), AtlasCell(4, 0), AtlasCell(5, 0), AtlasCell(6, 0)),
                listOf(AtlasCell(3, 1), AtlasCell(4, 1), AtlasCell(5, 1), AtlasCell(6, 1)),
                listOf(AtlasCell(3, 2), AtlasCell(4, 2), AtlasCell(5, 2), AtlasCell(6, 2)),
            ),
            doorCol = 1, doorRow = 2,
        )
    }

    fun build(): WorldGrid {
        val screens = mapOf(
            (0 to 0) to forestNw(),
            (1 to 0) to mountainN(),
            (2 to 0) to forestNe(),
            (0 to 1) to townW(),
            (1 to 1) to harborC(),
            (2 to 1) to castleE(),
            (0 to 2) to beachSw(),
            (1 to 2) to sanctumS(),
            (2 to 2) to fieldSe(),
        )
        return WorldGrid(
            cols = 3, rows = 3,
            screens = screens,
            startScreen = 1 to 1,             // Harbor
            startLocalCol = W / 2,
            startLocalRow = H / 2,
        )
    }

    // ── Screens ──────────────────────────────────────────────────────────

    /** Center screen: Harbor with pier, lighthouse, water on north side. */
    private fun harborC(): Screen {
        val b = AtlasMapBuilder(W, H, Tiles.GRASS)
        // North band: deep sea + shallow + foam + sand
        b.groundRect(0, 0, W, 4, Tiles.WATER_DEEP)
        b.groundRect(0, 4, W, 1, Tiles.WATER_SHALLOW)
        b.groundRect(0, 5, W, 1, Tiles.SHORE_FOAM)
        b.groundRect(0, 6, W, 2, Tiles.SAND)
        // Pier reaching north
        b.groundRect(W / 2 - 2, 4, 4, 4, Tiles.PIER)
        // Stone plaza in center
        b.groundRect(8, 14, 16, 8, Tiles.PATH_STONE)
        // Two cottages flanking plaza
        b.place(Buildings.Cottage, 5, 16)
        b.place(Buildings.Cottage, 24, 16)
        // Vertical path connecting harbor to plaza
        b.groundRect(15, 8, 2, 6, Tiles.PATH_DIRT)
        // South path leading to next screen
        b.groundRect(15, 22, 2, 10, Tiles.PATH_DIRT)
        // Sea cells solid
        for (r in 0..3) for (c in 0 until W) b.solid(c, r)

        return Screen(1, 1, ScreenTheme.Harbor, b.build(), landmark = "Lighthouse")
    }

    /** West screen: dense town with multiple cottages and central fountain. */
    private fun townW(): Screen {
        val b = AtlasMapBuilder(W, H, Tiles.GRASS)
        // Main stone plaza filling the screen
        b.groundRect(2, 2, W - 4, H - 4, Tiles.PATH_STONE)
        // Outer hedge ring (overlay solids)
        for (c in 1 until W - 1) {
            b.overlay(c, 1, Tiles.BUSH); b.solid(c, 1)
            b.overlay(c, H - 2, Tiles.BUSH); b.solid(c, H - 2)
        }
        for (r in 1 until H - 1) {
            b.overlay(1, r, Tiles.BUSH); b.solid(1, r)
            b.overlay(W - 2, r, Tiles.BUSH); b.solid(W - 2, r)
        }
        // East exit gap (towards harbor)
        b.overlay(W - 2, H / 2, null)
        b.solid(W - 2, H / 2)  // remove solid
        b.solid(W - 2, H / 2 - 1)
        // 6 cottages in 2 rows
        b.place(Buildings.Cottage, 4, 6)
        b.place(Buildings.Cottage, 12, 6)
        b.place(Buildings.Cottage, 20, 6)
        b.place(Buildings.Cottage, 4, 22)
        b.place(Buildings.Cottage, 12, 22)
        b.place(Buildings.Cottage, 20, 22)
        // Central fountain
        b.overlay(W / 2 - 1, H / 2 - 1, Tiles.FOUNTAIN_TOP); b.solid(W / 2 - 1, H / 2 - 1)
        b.overlay(W / 2, H / 2 - 1, Tiles.FOUNTAIN_TOP); b.solid(W / 2, H / 2 - 1)
        b.overlay(W / 2 - 1, H / 2, Tiles.FOUNTAIN_BOTTOM); b.solid(W / 2 - 1, H / 2)
        b.overlay(W / 2, H / 2, Tiles.FOUNTAIN_BOTTOM); b.solid(W / 2, H / 2)

        return Screen(0, 1, ScreenTheme.Town, b.build(), landmark = "Plaza Fountain")
    }

    /** East screen: castle complex with gate and moat-suggesting water flanks. */
    private fun castleE(): Screen {
        val b = AtlasMapBuilder(W, H, Tiles.GRASS)
        // Moat: water flanking left + right edges
        for (r in 4 until H - 4) {
            b.ground(0, r, Tiles.WATER_DEEP); b.solid(0, r)
            b.ground(1, r, Tiles.WATER_SHALLOW); b.solid(1, r)
            b.ground(W - 1, r, Tiles.WATER_DEEP); b.solid(W - 1, r)
            b.ground(W - 2, r, Tiles.WATER_SHALLOW); b.solid(W - 2, r)
        }
        // Castle floor
        b.groundRect(4, 4, W - 8, H - 8, Tiles.PATH_STONE)
        // Castle wall ring (overlay)
        for (c in 4 until W - 4) {
            b.overlay(c, 4, Tiles.CASTLE_WALL); b.solid(c, 4)
            b.overlay(c, H - 5, Tiles.CASTLE_WALL); b.solid(c, H - 5)
        }
        for (r in 4 until H - 4) {
            b.overlay(4, r, Tiles.CASTLE_WALL); b.solid(4, r)
            b.overlay(W - 5, r, Tiles.CASTLE_WALL); b.solid(W - 5, r)
        }
        // Gate at south wall
        b.overlay(W / 2, H - 5, Tiles.CASTLE_GATE)
        b.solid(W / 2, H - 5)  // gate cell — could be a trigger later
        // Bridge over moat south
        b.groundRect(W / 2 - 1, H - 5, 2, 5, Tiles.BRIDGE)
        // Statue inside courtyard
        b.overlay(W / 2, H / 2, Tiles.STATUE); b.solid(W / 2, H / 2)

        return Screen(2, 1, ScreenTheme.Castle, b.build(), landmark = "Sky Citadel")
    }

    /** North screen: snow-capped mountain pass. */
    private fun mountainN(): Screen {
        val b = AtlasMapBuilder(W, H, Tiles.SNOW)
        // Rocky path winding south through snow
        b.groundRect(W / 2 - 2, 0, 4, H, Tiles.ROCK_FLOOR)
        // Boulders flanking path
        for (r in 4..H - 4 step 6) {
            b.overlay(W / 2 - 4, r, Tiles.ROCK); b.solid(W / 2 - 4, r)
            b.overlay(W / 2 + 4, r + 2, Tiles.ROCK); b.solid(W / 2 + 4, r + 2)
        }
        // Trees on outer edges
        for (c in 0 until 4) for (r in 0 until H) {
            b.overlay(c, r, Tiles.TREE); b.solid(c, r)
            b.overlay(W - 1 - c, r, Tiles.TREE); b.solid(W - 1 - c, r)
        }
        return Screen(1, 0, ScreenTheme.Mountain, b.build(), landmark = "Frostspine Pass")
    }

    /** Forest screens: dense trees, winding paths, encounter zones. */
    private fun forestNw(): Screen = themedForest(0, 0, encounterX = 8..14, encounterY = 18..22)
    private fun forestNe(): Screen = themedForest(2, 0, encounterX = 16..22, encounterY = 8..14)

    private fun themedForest(gridCol: Int, gridRow: Int, encounterX: IntRange, encounterY: IntRange): Screen {
        val b = AtlasMapBuilder(W, H, Tiles.GRASS)
        // Tree canopy on all 4 edges (3-cell thick), with gap at one edge
        for (c in 0 until W) for (r in 0 until 3) { b.overlay(c, r, Tiles.TREE); b.solid(c, r) }
        for (c in 0 until W) for (r in H - 3 until H) { b.overlay(c, r, Tiles.TREE); b.solid(c, r) }
        for (r in 0 until H) for (c in 0 until 3) { b.overlay(c, r, Tiles.TREE); b.solid(c, r) }
        for (r in 0 until H) for (c in W - 3 until W) { b.overlay(c, r, Tiles.TREE); b.solid(c, r) }
        // Open gaps: south + the appropriate cardinal toward harbor center
        for (c in W / 2 - 1..W / 2 + 1) for (r in H - 3 until H) {
            b.overlay(c, r, null); b.solid(c, r)  // overlay null = no tree drawn, but we keep solid? No, walkable
        }
        // Re-mark gap as walkable
        for (c in W / 2 - 1..W / 2 + 1) for (r in H - 3 until H) {
            b.ground(c, r, Tiles.PATH_DIRT)
        }
        // Inner clearings with bushes + flowers
        for (r in 6..H - 6 step 4) for (c in 6..W - 6 step 4) {
            b.overlay(c, r, Tiles.BUSH); b.solid(c, r)
        }
        // Encounter grass patch
        for (r in encounterY) for (c in encounterX) {
            b.ground(c, r, Tiles.GRASS_TALL); b.encounter(c, r)
        }
        return Screen(gridCol, gridRow, ScreenTheme.Forest, b.build(), landmark = "Whispering Grove")
    }

    /** Beach screen south-west: sand with shore. */
    private fun beachSw(): Screen {
        val b = AtlasMapBuilder(W, H, Tiles.SAND)
        // Sea band along south
        b.groundRect(0, H - 6, W, 4, Tiles.WATER_SHALLOW)
        b.groundRect(0, H - 2, W, 2, Tiles.WATER_DEEP)
        for (c in 0 until W) for (r in H - 2 until H) b.solid(c, r)
        // Path from north
        b.groundRect(W / 2 - 1, 0, 2, H - 6, Tiles.PATH_DIRT)
        // Driftwood / signs scattered
        for (i in 0..4) {
            b.overlay(4 + i * 5, 8, Tiles.SIGN); b.solid(4 + i * 5, 8)
        }
        return Screen(0, 2, ScreenTheme.Beach, b.build(), landmark = "Driftwood Strand")
    }

    /** Sanctum screen south: stone circle with statues. */
    private fun sanctumS(): Screen {
        val b = AtlasMapBuilder(W, H, Tiles.GRASS)
        // Sanctum stone floor
        b.groundRect(8, 8, W - 16, H - 16, Tiles.PATH_STONE)
        // 4 statues at compass points
        b.overlay(W / 2, 8, Tiles.STATUE); b.solid(W / 2, 8)
        b.overlay(W / 2, H - 9, Tiles.STATUE); b.solid(W / 2, H - 9)
        b.overlay(8, H / 2, Tiles.STATUE); b.solid(8, H / 2)
        b.overlay(W - 9, H / 2, Tiles.STATUE); b.solid(W - 9, H / 2)
        // Lanterns
        b.overlay(10, 10, Tiles.LANTERN); b.solid(10, 10)
        b.overlay(W - 11, 10, Tiles.LANTERN); b.solid(W - 11, 10)
        b.overlay(10, H - 11, Tiles.LANTERN); b.solid(10, H - 11)
        b.overlay(W - 11, H - 11, Tiles.LANTERN); b.solid(W - 11, H - 11)
        // North path entrance
        b.groundRect(W / 2 - 1, 0, 2, 8, Tiles.PATH_DIRT)
        return Screen(1, 2, ScreenTheme.Sanctum, b.build(), landmark = "Eye of the Echo")
    }

    /** Field screen south-east: rolling grass with encounter zones. */
    private fun fieldSe(): Screen {
        val b = AtlasMapBuilder(W, H, Tiles.GRASS)
        // Three encounter patches
        for (r in 6..10) for (c in 4..10) { b.ground(c, r, Tiles.GRASS_TALL); b.encounter(c, r) }
        for (r in 14..18) for (c in 18..26) { b.ground(c, r, Tiles.GRASS_TALL); b.encounter(c, r) }
        for (r in 22..26) for (c in 8..14) { b.ground(c, r, Tiles.GRASS_TALL); b.encounter(c, r) }
        // Scattered trees as natural barriers
        for ((c, r) in listOf(8 to 12, 16 to 8, 20 to 20, 24 to 4, 12 to 26)) {
            b.overlay(c, r, Tiles.TREE); b.solid(c, r)
        }
        // Path into screen from west + north
        b.groundRect(0, H / 2 - 1, W, 2, Tiles.PATH_DIRT)
        b.groundRect(W / 2 - 1, 0, 2, H, Tiles.PATH_DIRT)
        return Screen(2, 2, ScreenTheme.Plains, b.build(), landmark = "Aether Plains")
    }
}
