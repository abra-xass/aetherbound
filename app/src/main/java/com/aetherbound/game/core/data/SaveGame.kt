package com.aetherbound.game.core.data

import android.content.Context
import com.aetherbound.game.core.Aspect
import com.aetherbound.game.core.BaseStats
import com.aetherbound.game.core.EchoformInstance
import com.aetherbound.game.core.EchoformSpecies
import com.aetherbound.game.core.Potential
import com.aetherbound.game.core.Rarity
import com.aetherbound.game.core.ScreenTheme
import com.aetherbound.game.core.Technique
import com.aetherbound.game.core.TechniqueCategory
import com.aetherbound.game.core.Temperament
import com.aetherbound.game.core.TrainingPoints
import com.aetherbound.game.core.StatKey
import org.json.JSONArray
import org.json.JSONObject

/**
 * On-device save game. Single file, JSON, lives in
 * `<files>/aetherbound/save_<slot>.json`.
 *
 * Schema (top-level):
 *   { "version": 1,
 *     "playerName": "...",
 *     "money": 1234,
 *     "currentMap": "game/maps/tuxemon/route_1.tmx",
 *     "playerTileX": 12, "playerTileY": 8, "playerFacing": "SOUTH",
 *     "party": [...EchoformInstance...],
 *     "pcStorage": [[...box1...],[...box2...]],
 *     "inventory": { "potion": 5, "tuxeball": 12 },
 *     "totalXp": 1500,
 *     "playtimeSec": 1800,
 *     "encounterTotal": 47,
 *     "flags": { "story_intro_done": true, ... },
 *     "trainersDefeated": ["route1_hiker"]
 *   }
 *
 * Encoding notes — Echoforms persist as data-only snapshots; the species's
 * heavy fields (stats / biomes / catchRate) are recomputed on load via
 * [TuxemonAdapter.echoformFromTuxemon] so save sizes stay tiny.
 */
data class SaveGame(
    val version: Int = 1,
    val playerName: String = "Player",
    val currentMap: String = "",
    val playerTileX: Int = 0,
    val playerTileY: Int = 0,
    val playerFacing: String = "SOUTH",
    val party: Party = Party(),
    val pcStorage: PcStorage = PcStorage(),
    val inventory: Inventory = Inventory(),
    val totalXp: Long = 0,
    val playtimeSec: Long = 0,
    val encounterTotal: Int = 0,
    val flags: Map<String, Boolean> = emptyMap(),
    val trainersDefeated: Set<String> = emptySet(),
)

object SaveGameIO {

    fun save(ctx: Context, slot: Int, save: SaveGame): Boolean {
        return runCatching {
            val dir = java.io.File(ctx.filesDir, "aetherbound").apply { mkdirs() }
            val file = java.io.File(dir, "save_$slot.json")
            file.writeText(toJson(save).toString(2), Charsets.UTF_8)
            true
        }.getOrDefault(false)
    }

    fun load(ctx: Context, slot: Int): SaveGame? {
        val file = java.io.File(java.io.File(ctx.filesDir, "aetherbound"), "save_$slot.json")
        if (!file.exists()) return null
        return runCatching { fromJson(JSONObject(file.readText(Charsets.UTF_8))) }.getOrNull()
    }

    fun deleteSlot(ctx: Context, slot: Int): Boolean {
        val file = java.io.File(java.io.File(ctx.filesDir, "aetherbound"), "save_$slot.json")
        return file.delete()
    }

    fun listSlots(ctx: Context): List<Int> {
        val dir = java.io.File(ctx.filesDir, "aetherbound")
        if (!dir.exists()) return emptyList()
        return dir.listFiles { _, name -> name.startsWith("save_") && name.endsWith(".json") }
            ?.mapNotNull { it.nameWithoutExtension.removePrefix("save_").toIntOrNull() }
            ?.sorted() ?: emptyList()
    }

    // ── Auto-save slot — for post-match commits ──────────────────────
    //
    // Multiplayer matches MUST persist immediately on end so the W/L
    // counter, pot delta, and bestiary updates can't be reverted by a
    // force-stop. The auto-save is a separate file from the 5 manual
    // slots so it never overwrites user-curated saves.

    private const val AUTO_SAVE_FILE = "save_auto.json"

    fun saveAuto(ctx: Context, save: SaveGame): Boolean = runCatching {
        val dir = java.io.File(ctx.filesDir, "aetherbound").apply { mkdirs() }
        // Use atomic write: tmp file then rename, so a crash mid-write
        // never produces a corrupt file.
        val tmp = java.io.File(dir, "$AUTO_SAVE_FILE.tmp")
        val final = java.io.File(dir, AUTO_SAVE_FILE)
        tmp.writeText(toJson(save).toString(), Charsets.UTF_8)
        tmp.renameTo(final)
    }.getOrDefault(false)

    fun loadAuto(ctx: Context): SaveGame? {
        val file = java.io.File(java.io.File(ctx.filesDir, "aetherbound"), AUTO_SAVE_FILE)
        if (!file.exists()) return null
        return runCatching { fromJson(JSONObject(file.readText(Charsets.UTF_8))) }.getOrNull()
    }

    fun autoSlotTimestamp(ctx: Context): Long {
        val file = java.io.File(java.io.File(ctx.filesDir, "aetherbound"), AUTO_SAVE_FILE)
        return if (file.exists()) file.lastModified() else 0L
    }

    // ───────────────────────────────────────────────────────────────
    // Serialisation — keep Tuxemon-derived heavy data out of the file.
    // ───────────────────────────────────────────────────────────────

    fun toJson(save: SaveGame): JSONObject = JSONObject().apply {
        put("version", save.version)
        put("playerName", save.playerName)
        put("currentMap", save.currentMap)
        put("playerTileX", save.playerTileX)
        put("playerTileY", save.playerTileY)
        put("playerFacing", save.playerFacing)
        put("party", JSONObject().apply {
            put("activeIndex", save.party.activeIndex)
            put("members", JSONArray().also { arr ->
                save.party.members.forEach { arr.put(echoformToJson(it)) }
            })
        })
        put("pcStorage", JSONArray().also { arr ->
            save.pcStorage.boxes.forEach { box ->
                arr.put(JSONObject().apply {
                    put("name", box.name)
                    put("slots", JSONArray().also { sl -> box.slots.forEach { sl.put(echoformToJson(it)) } })
                })
            }
        })
        put("inventory", JSONObject().apply {
            put("money", save.inventory.money)
            put("stacks", JSONObject(save.inventory.stacks))
        })
        put("totalXp", save.totalXp)
        put("playtimeSec", save.playtimeSec)
        put("encounterTotal", save.encounterTotal)
        put("flags", JSONObject(save.flags))
        put("trainersDefeated", JSONArray(save.trainersDefeated.toList()))
    }

    fun fromJson(obj: JSONObject): SaveGame {
        val partyObj = obj.optJSONObject("party")
        val members = partyObj?.optJSONArray("members")?.let { arr ->
            (0 until arr.length()).map { echoformFromJson(arr.getJSONObject(it)) }
        } ?: emptyList()
        val party = Party(members = members, activeIndex = partyObj?.optInt("activeIndex", 0) ?: 0)

        val pcStorage = obj.optJSONArray("pcStorage")?.let { arr ->
            val boxes = (0 until arr.length()).map { i ->
                val b = arr.getJSONObject(i)
                val slots = b.optJSONArray("slots")?.let { sl ->
                    (0 until sl.length()).map { echoformFromJson(sl.getJSONObject(it)) }
                } ?: emptyList()
                PcBox(name = b.optString("name", "Box $i"), slots = slots)
            }
            PcStorage(boxes = if (boxes.isEmpty()) listOf(PcBox("Box 1")) else boxes)
        } ?: PcStorage()

        val inv = obj.optJSONObject("inventory") ?: JSONObject()
        val stacks = inv.optJSONObject("stacks")?.let { s ->
            buildMap {
                val keys = s.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    put(k, s.optInt(k, 0))
                }
            }
        } ?: emptyMap()

        val flags = obj.optJSONObject("flags")?.let { f ->
            buildMap {
                val keys = f.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    put(k, f.optBoolean(k, false))
                }
            }
        } ?: emptyMap()

        val trainersArr = obj.optJSONArray("trainersDefeated")
        val trainers = trainersArr?.let { a ->
            (0 until a.length()).map { a.getString(it) }.toSet()
        } ?: emptySet()

        return SaveGame(
            version = obj.optInt("version", 1),
            playerName = obj.optString("playerName", "Player"),
            currentMap = obj.optString("currentMap", ""),
            playerTileX = obj.optInt("playerTileX", 0),
            playerTileY = obj.optInt("playerTileY", 0),
            playerFacing = obj.optString("playerFacing", "SOUTH"),
            party = party,
            pcStorage = pcStorage,
            inventory = Inventory(stacks = stacks, money = inv.optInt("money", 0)),
            totalXp = obj.optLong("totalXp", 0),
            playtimeSec = obj.optLong("playtimeSec", 0),
            encounterTotal = obj.optInt("encounterTotal", 0),
            flags = flags,
            trainersDefeated = trainers,
        )
    }

    // ── Echoform serialisation ────────────────────────────────────

    private fun echoformToJson(inst: EchoformInstance): JSONObject = JSONObject().apply {
        val s = inst.species
        put("speciesId", s.id)
        put("name", s.name)
        put("primaryAspect", s.primaryAspect.slug)
        s.secondaryAspect?.let { put("secondaryAspect", it.slug) }
        put("baseStats", JSONObject().apply {
            put("vigor", s.baseStats.vigor); put("force", s.baseStats.force); put("focus", s.baseStats.focus)
            put("guard", s.baseStats.guard); put("ward", s.baseStats.ward); put("tempo", s.baseStats.tempo)
        })
        put("catchRate", s.catchRate)
        put("rarity", s.rarity.name)
        put("biomes", JSONArray(s.biomes.map { it.name }))
        put("isLegendary", s.isLegendary)

        put("level", inst.level)
        put("currentVigor", inst.currentVigor)
        put("techniques", JSONArray(inst.techniques.map { techToJson(it) }))
        put("potential", JSONObject().apply {
            put("vigor", inst.potential.vigor); put("force", inst.potential.force); put("focus", inst.potential.focus)
            put("guard", inst.potential.guard); put("ward", inst.potential.ward); put("tempo", inst.potential.tempo)
        })
        put("training", JSONObject().apply {
            put("vigor", inst.training.vigor); put("force", inst.training.force); put("focus", inst.training.focus)
            put("guard", inst.training.guard); put("ward", inst.training.ward); put("tempo", inst.training.tempo)
        })
        put("temperament", JSONObject().apply {
            put("name", inst.temperament.name)
            inst.temperament.boosted?.let { put("boosted", it.name) }
            inst.temperament.reduced?.let { put("reduced", it.name) }
        })
    }

    private fun echoformFromJson(o: JSONObject): EchoformInstance {
        val baseObj = o.getJSONObject("baseStats")
        val base = BaseStats(
            vigor = baseObj.optInt("vigor", 60), force = baseObj.optInt("force", 60),
            focus = baseObj.optInt("focus", 60), guard = baseObj.optInt("guard", 60),
            ward = baseObj.optInt("ward", 60), tempo = baseObj.optInt("tempo", 60),
        )
        val biomes = o.optJSONArray("biomes")?.let { arr ->
            (0 until arr.length()).mapNotNull { runCatching { ScreenTheme.valueOf(arr.getString(it)) }.getOrNull() }
        } ?: emptyList()
        val species = EchoformSpecies(
            id = o.optString("speciesId", ""),
            name = o.optString("name", ""),
            primaryAspect = Aspect.fromSlugOrNormal(o.optString("primaryAspect")),
            secondaryAspect = o.optString("secondaryAspect", "").takeIf { it.isNotEmpty() }?.let { Aspect.fromSlug(it) },
            baseStats = base,
            catchRate = o.optInt("catchRate", 190),
            rarity = runCatching { Rarity.valueOf(o.optString("rarity", "Common")) }.getOrDefault(Rarity.Common),
            biomes = biomes,
            isLegendary = o.optBoolean("isLegendary", false),
        )
        val potObj = o.optJSONObject("potential")
        val potential = potObj?.let {
            Potential(
                vigor = it.optInt("vigor", 31), force = it.optInt("force", 31),
                focus = it.optInt("focus", 31), guard = it.optInt("guard", 31),
                ward = it.optInt("ward", 31), tempo = it.optInt("tempo", 31),
            )
        } ?: Potential()
        val trObj = o.optJSONObject("training")
        val training = trObj?.let {
            TrainingPoints(
                vigor = it.optInt("vigor", 0), force = it.optInt("force", 0),
                focus = it.optInt("focus", 0), guard = it.optInt("guard", 0),
                ward = it.optInt("ward", 0), tempo = it.optInt("tempo", 0),
            )
        } ?: TrainingPoints()
        val tmpObj = o.optJSONObject("temperament")
        val temperament = tmpObj?.let {
            Temperament(
                name = it.optString("name", "Balanced"),
                boosted = it.optString("boosted", "").takeIf { s -> s.isNotEmpty() }?.let { s -> runCatching { StatKey.valueOf(s) }.getOrNull() },
                reduced = it.optString("reduced", "").takeIf { s -> s.isNotEmpty() }?.let { s -> runCatching { StatKey.valueOf(s) }.getOrNull() },
            )
        } ?: Temperament()
        val techsArr = o.optJSONArray("techniques")
        val techniques = techsArr?.let { a ->
            (0 until a.length()).map { techFromJson(a.getJSONObject(it)) }
        } ?: emptyList()
        return EchoformInstance(
            species = species,
            level = o.optInt("level", 1),
            techniques = techniques,
            potential = potential,
            training = training,
            temperament = temperament,
            currentVigor = o.optInt("currentVigor", 1),
        )
    }

    private fun techToJson(t: Technique): JSONObject = JSONObject().apply {
        put("id", t.id)
        put("name", t.name)
        put("aspect", t.aspect.slug)
        put("category", t.category.name)
        put("power", t.power)
        put("accuracy", t.accuracy)
        put("priority", t.priority)
        put("critRate", t.critRate)
    }

    private fun techFromJson(o: JSONObject): Technique = Technique(
        id = o.optString("id", "struggle"),
        name = o.optString("name", "Struggle"),
        aspect = Aspect.fromSlugOrNormal(o.optString("aspect")),
        category = runCatching { TechniqueCategory.valueOf(o.optString("category", "STRIKE")) }.getOrDefault(TechniqueCategory.STRIKE),
        power = o.optInt("power", 30),
        accuracy = o.optInt("accuracy", 100),
        priority = o.optInt("priority", 0),
        critRate = o.optInt("critRate", 1),
    )
}
