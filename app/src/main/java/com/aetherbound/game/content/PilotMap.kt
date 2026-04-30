package com.aetherbound.game.content

import com.aetherbound.game.render.world.CoastalTile

/**
 * Namaris Harbor — pilot map. 32×24 tiles, hand-designed using
 * classic 16-bit RPG town layout principles (not derived from any specific IP):
 *
 *   • Three clear zones: HARBOR (north) → TOWN (middle) → FIELD (south)
 *   • One central north-south path connecting them
 *   • Four 3×3 buildings: Echo Center, Item Shop, two homes
 *   • Three encounter zones (tall grass) along the southern field edges
 *   • Lighthouse landmark NE corner (col=27, row=4)
 *   • Player spawn south-central
 *
 * Legend:
 *  ~ deep sea  . sea shallow  f shore foam  S sand
 *  g grass     G tall grass  > encounter zone (tall grass)
 *  p path      P stone path  : stair
 *  o pier      O pier edge   c crate         b barrel-lamp  L bollard
 *  R roof      W wall        D door          n window       F flag pole
 *  H hedge     l lantern     e bench         s sign         # seal stone
 *  _ empty shadow
 */
data class TileMap(
    val cols: Int,
    val rows: Int,
    val tiles: List<List<CoastalTile>>,
    val encounter: Set<Pair<Int, Int>>,
    val solid: Set<Pair<Int, Int>>,
)

object PilotMap {

    private val rows = listOf(
        // Row 0..2: deep sea + shallow + foam (north). 33 chars to match the rest.
        "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~",
        "~~~~..ff..~~~~..~~~~..~~~~~..~~~~",
        "~..ffSS..~~..fff..ff~~~..fff~~~..",
        // Row 3..4: shore strip + harbor pier
        "ffSSSSSSSSSSSSSSSSSSSSSSSSSSSffff",
        "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
        // Row 5..7: harbor row — pier with bollards/barrels, lighthouse NE
        "Sb_oOOoOOoOOoOOoSSSSSSSSSSSLSgggS",
        "Sc_oPPPPPPPPPPPPSSSSSSSSSSSLSgGgS",
        "Sc_oPPPPPpPPPPPPSSSSSSSSSSLLSGGGS",
        // Row 8: sand strip + path begin southward
        "S__oPPPPPpPPPPPPPSSSSSSSSSSSSgggS",
        // Row 9..11: TOWN ROW 1 — Echo Center (left), Item Shop (right)
        "SgHHWWnDWWnHHHgggggHHWWnDWWnHHggS",
        "SgHHRRRRRRRRRHgggggHRRRRRRRRRHggS",
        "SggHHHHHHHHHHHgggggHHHHHHHHHHHggS",
        // Row 12..13: town plaza + path
        "SggggPPPPPPPPPPpPPPPPPPPPPPPPPggS",
        "SggleeePPpPPPPPPPPPPPPPpPPPlssggS",
        // Row 14..16: TOWN ROW 2 — two homes flanking
        "SggHHWWnDWWnHHgggggHHWWnDWWnHHggS",
        "SggHHRRRRRRRRRHgggHRRRRRRRRRRHggS",
        "SggHHHHHHHHHHHgggggHHHHHHHHHHHggS",
        // Row 17: path continues + benches + sign
        "SgggPPPpPPPPPPPPPPPPPPPPPPPpPPggS",
        // Row 18..20: FIELD — encounter grass zones west, south, east
        "Sgggggggggg>>gggGGGGGGgg>>gggggGS",
        "SGGgggGGggg>>>gGGGGGggg>>>ggGGGGS",
        "SGGGggGGggg>>>gGGGGGGgg>>>ggGGGGS",
        // Row 21: south path through field
        "SggggGGgggggggGGGgggggggggggggggS",
        // Row 22: south exit corridor
        "SgggggggggggGGGggggggGGgggggggggS",
        // Row 23: field edge
        "SSSSSSSSSSSSSSSS#SSSSSSSSSSSSSSSS",
    )

    fun build(): TileMap {
        val cols = rows[0].length
        val matrix = MutableList(rows.size) { MutableList(cols) { CoastalTile.SeaDeep } }
        val encounter = mutableSetOf<Pair<Int, Int>>()
        val solid = mutableSetOf<Pair<Int, Int>>()
        for ((r, line) in rows.withIndex()) {
            for ((c, ch) in line.withIndex()) {
                val (tile, isSolid, isEnc) = mapChar(ch)
                matrix[r][c] = tile
                if (isSolid) solid += c to r
                if (isEnc) encounter += c to r
            }
        }
        return TileMap(cols, rows.size, matrix, encounter, solid)
    }

    private data class CharSpec(val tile: CoastalTile, val solid: Boolean, val enc: Boolean)
    private fun mapChar(ch: Char): CharSpec = when (ch) {
        '~' -> CharSpec(CoastalTile.SeaDeep, true, false)
        '.' -> CharSpec(CoastalTile.SeaShallow, true, false)
        'f' -> CharSpec(CoastalTile.ShoreFoam, true, false)
        'S' -> CharSpec(CoastalTile.Sand, false, false)
        'g' -> CharSpec(CoastalTile.Grass, false, false)
        'G' -> CharSpec(CoastalTile.GrassTall, false, false)
        '>' -> CharSpec(CoastalTile.GrassTall, false, true)
        'p' -> CharSpec(CoastalTile.Path, false, false)
        'P' -> CharSpec(CoastalTile.PathStone, false, false)
        'o' -> CharSpec(CoastalTile.Pier, false, false)
        'O' -> CharSpec(CoastalTile.PierEdge, false, false)
        'c' -> CharSpec(CoastalTile.Crate, true, false)
        'b' -> CharSpec(CoastalTile.BarrelLamp, true, false)
        'L' -> CharSpec(CoastalTile.BollardGold, true, false)
        'R' -> CharSpec(CoastalTile.RoofTile, true, false)
        'W' -> CharSpec(CoastalTile.Wall, true, false)
        'D' -> CharSpec(CoastalTile.Door, false, false)
        'n' -> CharSpec(CoastalTile.Window, true, false)
        'F' -> CharSpec(CoastalTile.FlagPole, true, false)
        'H' -> CharSpec(CoastalTile.Hedge, true, false)
        ':' -> CharSpec(CoastalTile.Stair, false, false)
        'l' -> CharSpec(CoastalTile.Lantern, true, false)
        'e' -> CharSpec(CoastalTile.Bench, true, false)
        's' -> CharSpec(CoastalTile.Sign, true, false)
        '#' -> CharSpec(CoastalTile.SealStone, false, false)
        '_' -> CharSpec(CoastalTile.EmptyShadow, false, false)
        else -> CharSpec(CoastalTile.Sand, false, false)
    }
}
