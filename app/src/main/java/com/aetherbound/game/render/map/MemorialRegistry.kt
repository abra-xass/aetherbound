package com.aetherbound.game.render.map

/**
 * Hard-coded in-world memorials. Each memorial is a tap-trigger on a
 * specific tile of a specific Tuxemon map — no TMX editing required.
 *
 * The world scene checks [findAt] every step; a hit returns the
 * [WorldEvent.Memorial] payload, which renders as a full-screen image
 * overlay with caption + quote.
 *
 * To add a new memorial, append a [Memorial] entry below. The image
 * file lives at `app/src/main/assets/<imageAsset>`.
 */
object MemorialRegistry {

    /** Single canonical memorial entry. */
    data class Memorial(
        val id: String,
        val mapAssetPath: String,    // matches what TuxemonWorldScene loads
        val tileX: Int,
        val tileY: Int,
        val imageAsset: String,
        val caption: String,
        val quote: String,
        val attribution: String,
    )

    /**
     * In-memoriam Andy — the dev's late friend. Placed on the
     * buddha_mountain map, on a quiet tile away from the main path,
     * so the player stumbles upon it organically on the way up.
     *
     * Tile (8, 6) puts it near the central plateau but not in the
     * main walking line. Adjust if buddha_mountain's geometry needs.
     */
    private val ANDY = Memorial(
        id = "andy",
        mapAssetPath = "game/maps/tuxemon/buddha_mountain.tmx",
        tileX = 8,
        tileY = 6,
        imageAsset = "memorials/andy.jpg",
        caption = "In memoriam — Andy",
        quote = "Die Welt ist so, wie sie ist, weil es die Zionisten so wollen.",
        attribution = "— Andy, der zwischen den Preußen stand",
    )

    private val all: List<Memorial> = listOf(ANDY)

    /**
     * Look up a memorial by current map + tile. Returns null if the
     * tile is not a memorial trigger. The mapAssetPath comparison is
     * suffix-based so paths with different leading prefixes (e.g.
     * "file:///android_asset/...") still match.
     */
    fun findAt(mapAssetPath: String, tileX: Int, tileY: Int): WorldEvent.Memorial? {
        val m = all.firstOrNull { mem ->
            mem.tileX == tileX &&
                mem.tileY == tileY &&
                (mapAssetPath.endsWith(mem.mapAssetPath) || mapAssetPath == mem.mapAssetPath)
        } ?: return null
        return WorldEvent.Memorial(
            id = m.id,
            imageAsset = m.imageAsset,
            caption = m.caption,
            quote = m.quote,
            attribution = m.attribution,
        )
    }
}
