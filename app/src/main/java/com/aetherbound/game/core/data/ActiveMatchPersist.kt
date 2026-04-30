package com.aetherbound.game.core.data

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * Crash-resilient persistence for an in-flight multiplayer match.
 *
 * If Aetherbound is killed mid-match (system OOM, user force-stop,
 * battery-saver), without this file the snapshot/state is lost and the
 * pre-match Party-restore can't run. We periodically write a small JSON
 * blob to the app's private storage with just the data needed to either
 * (a) resume the match if both peers are still around, or (b) at least
 * roll back the party so the player isn't stuck with phantom state.
 *
 * The file lives at `<files>/aetherbound/active_match.json`. It's deleted
 * cleanly on graceful match-end; the **presence** of the file at app-
 * start signals "previous match was interrupted, prompt user".
 *
 * Stored fields (all decided at lobby + first-turn):
 *   - room_id, peer_matrix_id (so resume can re-broadcast)
 *   - rng_seed (deterministic state-replay)
 *   - mode (RANKED/CASUAL)
 *   - snapshot of the Party + world coords (the pre-match restore point)
 *   - turn (last completed)
 *   - timestamp_ms (so a stale match >30 min old auto-discards)
 */
object ActiveMatchPersist {

    private const val DIR = "aetherbound"
    private const val FILE = "active_match.json"
    private const val STALE_AGE_MS = 30L * 60 * 1000  // 30 minutes

    data class Snapshot(
        val roomId: String,
        val peerMatrixId: String,
        val rngSeed: Long,
        val mode: String,             // "RANKED" / "CASUAL"
        val turn: Int,
        val matchSnapshot: MultiplayerSnapshot,
        val timestampMs: Long = System.currentTimeMillis(),
    )

    fun save(ctx: Context, s: Snapshot): Boolean = runCatching {
        val dir = File(ctx.filesDir, DIR).apply { mkdirs() }
        val obj = JSONObject().apply {
            put("roomId", s.roomId)
            put("peerMatrixId", s.peerMatrixId)
            put("rngSeed", s.rngSeed)
            put("mode", s.mode)
            put("turn", s.turn)
            put("timestampMs", s.timestampMs)
            put("worldMapPath", s.matchSnapshot.worldMapPath)
            put("worldTileX", s.matchSnapshot.worldTileX)
            put("worldTileY", s.matchSnapshot.worldTileY)
            // Encode the party via SaveGameIO so we share format with main saves.
            val saveStub = com.aetherbound.game.core.data.SaveGame(
                playerName = "active-match",
                party = s.matchSnapshot.partyDeepCopy,
            )
            put("partyJson", SaveGameIO.toJson(saveStub).toString())
        }
        File(dir, FILE).writeText(obj.toString(2), Charsets.UTF_8)
        true
    }.getOrDefault(false)

    fun load(ctx: Context): Snapshot? {
        val file = File(File(ctx.filesDir, DIR), FILE)
        if (!file.exists()) return null
        return runCatching {
            val obj = JSONObject(file.readText(Charsets.UTF_8))
            val ts = obj.optLong("timestampMs", 0)
            // Auto-discard stale snapshots so the user isn't haunted by
            // ancient interrupted matches forever.
            if (System.currentTimeMillis() - ts > STALE_AGE_MS) {
                file.delete()
                return null
            }
            val partyJsonStr = obj.optString("partyJson", "")
            if (partyJsonStr.isEmpty()) return null
            val saveStub = SaveGameIO.fromJson(JSONObject(partyJsonStr))
            val ms = MultiplayerSnapshot(
                partyDeepCopy = saveStub.party,
                worldMapPath = obj.optString("worldMapPath"),
                worldTileX = obj.optInt("worldTileX"),
                worldTileY = obj.optInt("worldTileY"),
                capturedAtMs = ts,
            )
            Snapshot(
                roomId = obj.optString("roomId"),
                peerMatrixId = obj.optString("peerMatrixId"),
                rngSeed = obj.optLong("rngSeed"),
                mode = obj.optString("mode", "RANKED"),
                turn = obj.optInt("turn", 0),
                matchSnapshot = ms,
                timestampMs = ts,
            )
        }.getOrNull()
    }

    /** Clean delete — graceful match-end calls this. */
    fun clear(ctx: Context) {
        runCatching {
            File(File(ctx.filesDir, DIR), FILE).delete()
        }
    }
}
