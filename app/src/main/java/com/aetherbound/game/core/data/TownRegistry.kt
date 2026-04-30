package com.aetherbound.game.core.data

/**
 * Registry of map paths that count as "towns" — i.e. valid Fly /
 * Fast-Travel destinations. Walking onto any of these maps for the
 * first time auto-adds the town to [PlayerProgress.visitedTowns].
 *
 * The list is hand-curated from the Tuxemon TMX import. Each entry
 * pairs the asset path with a display name shown in the FlyMenu.
 */
object TownRegistry {

    data class Town(
        val mapAssetPath: String,
        val displayName: String,
        /** Tile coordinates the player lands on when flying here. */
        val arrivalTileX: Int,
        val arrivalTileY: Int,
    )

    val all: List<Town> = listOf(
        Town(
            mapAssetPath = "game/maps/tuxemon/cotton_town.tmx",
            displayName = "Cotton Town",
            arrivalTileX = 14,
            arrivalTileY = 14,
        ),
        Town(
            mapAssetPath = "game/maps/tuxemon/candy_town.tmx",
            displayName = "Candy Town",
            arrivalTileX = 12,
            arrivalTileY = 12,
        ),
        Town(
            mapAssetPath = "game/maps/tuxemon/azure_town.tmx",
            displayName = "Azure Town",
            arrivalTileX = 10,
            arrivalTileY = 10,
        ),
        Town(
            mapAssetPath = "game/maps/tuxemon/spyder_papertown.tmx",
            displayName = "Paper Town",
            arrivalTileX = 8,
            arrivalTileY = 12,
        ),
        Town(
            mapAssetPath = "game/maps/tuxemon/citypark.tmx",
            displayName = "City Park",
            arrivalTileX = 8,
            arrivalTileY = 8,
        ),
        Town(
            mapAssetPath = "game/maps/tuxemon/buddha_mountain.tmx",
            displayName = "Buddha-Berg",
            arrivalTileX = 8,
            arrivalTileY = 6,
        ),
        Town(
            mapAssetPath = "game/maps/tuxemon/37707_town.tmx",
            displayName = "Stadt 37707",
            arrivalTileX = 12,
            arrivalTileY = 14,
        ),
    )

    /**
     * Look up a town by map asset path. Tolerates suffix matches so an
     * absolute path / scheme prefix doesn't break detection.
     */
    fun byMapPath(mapPath: String): Town? = all.firstOrNull { mapPath.endsWith(it.mapAssetPath) }

    /** Convenience: just the path is a "town" we should record? */
    fun isTown(mapPath: String): Boolean = byMapPath(mapPath) != null

    /** Resolve display name for any of our visitedTowns ids. */
    fun displayNameFor(townId: String): String =
        all.firstOrNull { it.mapAssetPath == townId }?.displayName ?: townId
}
