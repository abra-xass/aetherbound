package com.aetherbound.game.core.data

import android.content.Context
import com.aetherbound.game.core.EchoformInstance
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * On-device replay archive. Every multiplayer match-end (or single-
 * player capture-victory if interesting) saves the bare minimum needed
 * to deterministically replay the battle: rngSeed + initial teams +
 * sequence of move-pairs.
 *
 * Replays live at `<files>/aetherbound/replays/<id>.json`. Listed in
 * the Trainer Card so the player can re-watch their own past battles.
 *
 * Sharing: a replay can be exported as JSON and posted to a Thot room
 * via the bridge's `replay.share` event-type. The receiver decodes
 * + opens [SpectatorArenaScene] in playback mode.
 */
data class Replay(
    val id: String,
    val title: String,
    val rngSeed: Long,
    val hostName: String,
    val guestName: String,
    val hostInitial: EchoformInstance,
    val guestInitial: EchoformInstance,
    /** Each entry = (host-move-idx, guest-move-idx) for one resolved turn. */
    val turns: List<Pair<Int, Int>>,
    val winner: String,         // "host" / "guest" / "draw"
    val timestampMs: Long = System.currentTimeMillis(),
)

object ReplayStore {

    private const val DIR = "aetherbound/replays"
    private const val MAX_REPLAYS = 20

    fun save(ctx: Context, replay: Replay): Boolean = runCatching {
        val dir = File(ctx.filesDir, DIR).apply { mkdirs() }
        val obj = JSONObject().apply {
            put("id", replay.id)
            put("title", replay.title)
            put("rngSeed", replay.rngSeed)
            put("hostName", replay.hostName)
            put("guestName", replay.guestName)
            put("hostInitial", echoformToJson(replay.hostInitial))
            put("guestInitial", echoformToJson(replay.guestInitial))
            put("turns", JSONArray().apply {
                replay.turns.forEach { (h, g) ->
                    put(JSONArray().put(h).put(g))
                }
            })
            put("winner", replay.winner)
            put("timestampMs", replay.timestampMs)
        }
        File(dir, "${replay.id}.json").writeText(obj.toString(2), Charsets.UTF_8)

        // Trim old replays — keep the newest 20.
        prune(dir)
        true
    }.getOrDefault(false)

    fun list(ctx: Context): List<Replay> {
        val dir = File(ctx.filesDir, DIR)
        if (!dir.exists()) return emptyList()
        return dir.listFiles { _, name -> name.endsWith(".json") }
            ?.mapNotNull { load(it) }
            ?.sortedByDescending { it.timestampMs }
            ?: emptyList()
    }

    fun load(ctx: Context, id: String): Replay? {
        val file = File(File(ctx.filesDir, DIR), "$id.json")
        return load(file)
    }

    private fun load(file: File): Replay? {
        if (!file.exists()) return null
        return runCatching {
            val obj = JSONObject(file.readText(Charsets.UTF_8))
            val turns = mutableListOf<Pair<Int, Int>>()
            val arr = obj.optJSONArray("turns")
            if (arr != null) for (i in 0 until arr.length()) {
                val pair = arr.getJSONArray(i)
                turns += pair.getInt(0) to pair.getInt(1)
            }
            Replay(
                id = obj.optString("id"),
                title = obj.optString("title"),
                rngSeed = obj.optLong("rngSeed"),
                hostName = obj.optString("hostName"),
                guestName = obj.optString("guestName"),
                hostInitial = echoformFromJson(obj.getJSONObject("hostInitial")),
                guestInitial = echoformFromJson(obj.getJSONObject("guestInitial")),
                turns = turns,
                winner = obj.optString("winner", "draw"),
                timestampMs = obj.optLong("timestampMs", 0),
            )
        }.getOrNull()
    }

    fun delete(ctx: Context, id: String): Boolean {
        return File(File(ctx.filesDir, DIR), "$id.json").delete()
    }

    /** Encode a Replay to a single JSON string suitable for the wire format. */
    fun toJsonString(replay: Replay): String {
        val obj = JSONObject().apply {
            put("rngSeed", replay.rngSeed)
            put("hostName", replay.hostName)
            put("guestName", replay.guestName)
            put("hostInitial", echoformToJson(replay.hostInitial))
            put("guestInitial", echoformToJson(replay.guestInitial))
            put("turns", JSONArray().apply {
                replay.turns.forEach { (h, g) -> put(JSONArray().put(h).put(g)) }
            })
            put("winner", replay.winner)
        }
        return obj.toString()
    }

    fun fromJsonString(json: String, fallbackTitle: String = "Shared Replay"): Replay? = runCatching {
        val obj = JSONObject(json)
        val turns = mutableListOf<Pair<Int, Int>>()
        val arr = obj.optJSONArray("turns")
        if (arr != null) for (i in 0 until arr.length()) {
            val pair = arr.getJSONArray(i)
            turns += pair.getInt(0) to pair.getInt(1)
        }
        Replay(
            id = "shared-${System.currentTimeMillis()}",
            title = fallbackTitle,
            rngSeed = obj.optLong("rngSeed"),
            hostName = obj.optString("hostName"),
            guestName = obj.optString("guestName"),
            hostInitial = echoformFromJson(obj.getJSONObject("hostInitial")),
            guestInitial = echoformFromJson(obj.getJSONObject("guestInitial")),
            turns = turns,
            winner = obj.optString("winner", "draw"),
        )
    }.getOrNull()

    private fun prune(dir: File) {
        val files = dir.listFiles { _, name -> name.endsWith(".json") }?.sortedByDescending { it.lastModified() }
            ?: return
        files.drop(MAX_REPLAYS).forEach { it.delete() }
    }

    // ── Reuse SaveGameIO's echoform encoder for consistency ─────────
    private fun echoformToJson(instance: EchoformInstance): JSONObject {
        // Wrap-and-unwrap via SaveGameIO's full echoform serializer.
        val stub = SaveGame(playerName = "replay", party = Party(members = listOf(instance)))
        return SaveGameIO.toJson(stub).getJSONObject("party").getJSONArray("members").getJSONObject(0)
    }
    private fun echoformFromJson(o: JSONObject): EchoformInstance {
        val envelope = JSONObject().apply {
            put("party", JSONObject().apply {
                put("activeIndex", 0)
                put("members", JSONArray().put(o))
            })
        }
        return SaveGameIO.fromJson(envelope).party.members.first()
    }
}
