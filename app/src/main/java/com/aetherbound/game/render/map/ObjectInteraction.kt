package com.aetherbound.game.render.map

/**
 * Classifies and dispatches Tiled object-layer entries. Tuxemon stuffs
 * many gameplay primitives into objects: warps between maps, sign text,
 * NPC spawn-points, encounter zones, healing-pad triggers, item pickups.
 *
 * The dispatcher only *recognises* the object kind here — gameplay wiring
 * (open dialog, switch maps, give item) lives in the WorldScene.
 */
sealed class WorldEvent {
    /** Walk-on warp to another map. [destMap] is asset path, [destX/Y] tile coords. */
    data class Warp(val destMap: String, val destTileX: Int, val destTileY: Int, val sound: String? = null) : WorldEvent()

    /** Examine-able sign / billboard. */
    data class Sign(val text: String) : WorldEvent()

    /** NPC spawn-point with dialog and optional trainer-battle hook. */
    data class Npc(
        val sprite: String,
        val dialog: String,
        val trainerId: String?,
        val moveType: String = "static",
    ) : WorldEvent()

    /** Encounter zone — overrides the default biome's encounter pool. */
    data class EncounterZone(val biome: String, val rateOverride: Double?) : WorldEvent()

    /** Healing pad — fully restores party HP/PP/status when stepped on. */
    object HealZone : WorldEvent()

    /** Item pickup — disappears once collected (track in save flags). */
    data class ItemDrop(val itemSlug: String, val flagId: String) : WorldEvent()

    /** Generic — unrecognised type, just pass type+props through. */
    data class Generic(val type: String, val properties: Map<String, String>) : WorldEvent()
}

object ObjectDispatcher {

    /** Convert a [TmxObject] to a high-level [WorldEvent]. */
    fun classify(obj: TmxObject): WorldEvent {
        val type = obj.type.lowercase()
        return when (type) {
            "interact", "interact_map", "warp", "transition_teleport", "teleport" -> {
                val destMap = obj.properties["transition_map"]
                    ?: obj.properties["target_map"] ?: obj.properties["map"] ?: ""
                val destX = obj.properties["x"]?.toIntOrNull()
                    ?: obj.properties["destination_x"]?.toIntOrNull() ?: 0
                val destY = obj.properties["y"]?.toIntOrNull()
                    ?: obj.properties["destination_y"]?.toIntOrNull() ?: 0
                WorldEvent.Warp(destMap = destMap, destTileX = destX, destTileY = destY)
            }
            "sign", "interact_sign" -> {
                WorldEvent.Sign(text = obj.properties["msgid"]
                    ?: obj.properties["text"]
                    ?: obj.name)
            }
            "interact_act", "npc", "trainer" -> {
                WorldEvent.Npc(
                    sprite = obj.properties["sprite"] ?: "adventurer",
                    dialog = obj.properties["msgid"] ?: "",
                    trainerId = obj.properties["trainer_id"] ?: obj.properties["combat_id"],
                    moveType = obj.properties["behaviour"] ?: "static",
                )
            }
            "encounter", "encounter_zone" -> {
                WorldEvent.EncounterZone(
                    biome = obj.properties["biome"] ?: "grassland",
                    rateOverride = obj.properties["rate"]?.toDoubleOrNull(),
                )
            }
            "healing_tile", "healing_pad", "heal_tile" -> WorldEvent.HealZone
            "item", "interact_item" -> {
                WorldEvent.ItemDrop(
                    itemSlug = obj.properties["item"] ?: obj.name,
                    flagId = obj.properties["flag"] ?: "item_${obj.id}",
                )
            }
            else -> WorldEvent.Generic(type = type, properties = obj.properties)
        }
    }

    /**
     * Find the event at tile-cell ([tx],[ty]) on [map]. Returns null when
     * no object covers that tile. Walks every object layer.
     */
    fun eventAt(map: TmxMap, tx: Int, ty: Int): WorldEvent? {
        val tw = map.tileWidth
        val th = map.tileHeight
        for (layer in map.objectLayers) {
            for (obj in layer.objects) {
                val objLeft = (obj.x / tw).toInt()
                val objTop = (obj.y / th).toInt()
                val objW = ((obj.width / tw).toInt()).coerceAtLeast(1)
                val objH = ((obj.height / th).toInt()).coerceAtLeast(1)
                if (tx in objLeft until objLeft + objW && ty in objTop until objTop + objH) {
                    return classify(obj)
                }
            }
        }
        return null
    }
}
