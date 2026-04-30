package com.aetherbound.game.core.data

import com.aetherbound.game.core.BattleEvent
import com.aetherbound.game.core.EchoformInstance
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Single source of truth for the Matrix wire protocol Aetherbound uses to
 * tunnel multiplayer events through Thot's Matrix client. The Thot-side
 * `MultiplayerBridge` impl reads these constants and encoders so we never
 * have a string mismatch between sender and receiver.
 *
 * Event type namespace: `io.aether.*`
 *
 * Wire bodies are JSON. They will be wrapped by Matrix in
 * `m.room.encrypted` automatically — we don't reimplement crypto.
 *
 * See `docs/multiplayer-thot-binding.md` for the protocol overview.
 */
object MatrixWireFormat {

    // ── Event types ──────────────────────────────────────────────

    object EventType {
        const val CAPABILITY_PRESENT  = "io.aether.capability.present"
        const val CAPABILITY_REMOVED  = "io.aether.capability.removed"
        const val INVITE_TO_PLAY      = "io.aether.invite.to_play"

        const val TRADE_OFFER         = "io.aether.trade.offer"
        const val TRADE_ACCEPT        = "io.aether.trade.accept"
        const val TRADE_CANCEL        = "io.aether.trade.cancel"

        const val BATTLE_INVITE       = "io.aether.battle.invite"
        const val BATTLE_INVITE_REPLY = "io.aether.battle.invite.reply"
        const val BATTLE_START        = "io.aether.battle.start"
        const val BATTLE_MOVE         = "io.aether.battle.move"
        const val BATTLE_SWITCH       = "io.aether.battle.switch"
        const val BATTLE_FAINT        = "io.aether.battle.faint"
        const val BATTLE_END          = "io.aether.battle.end"

        const val SPECTATE_SUBSCRIBE  = "io.aether.spectate.subscribe"
        const val REPLAY_SHARE        = "io.aether.replay.share"
    }

    // ── Capability advertise ────────────────────────────────────

    fun encodeCapability(version: String, features: List<String>): JSONObject = JSONObject().apply {
        put("version", version)
        put("features", JSONArray(features))
        put("ts", System.currentTimeMillis())
    }

    data class Capability(val version: String, val features: List<String>, val timestampMs: Long)

    fun decodeCapability(obj: JSONObject): Capability {
        val features = obj.optJSONArray("features")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        } ?: emptyList()
        return Capability(
            version = obj.optString("version", "0"),
            features = features,
            timestampMs = obj.optLong("ts", 0),
        )
    }

    // ── Trade ───────────────────────────────────────────────────

    fun encodeTradeOffer(offering: EchoformInstance, expiresAtMs: Long): JSONObject = JSONObject().apply {
        put("partyMember", SaveGameIO.toJson(saveStub(offering)).getJSONObject("party").getJSONArray("members").getJSONObject(0))
        put("expiresAt", expiresAtMs)
    }

    fun decodeTradeOffer(obj: JSONObject): TradeOffer {
        // We ride on SaveGameIO's echoform encoder so trade and save stay aligned.
        val memberObj = obj.getJSONObject("partyMember")
        // Re-wrap into a SaveGame-style envelope for decoding.
        val envelope = JSONObject().apply {
            put("party", JSONObject().apply {
                put("activeIndex", 0)
                put("members", JSONArray().put(memberObj))
            })
        }
        val save = SaveGameIO.fromJson(envelope)
        val instance = save.party.members.firstOrNull()
            ?: throw IllegalStateException("trade.offer: missing partyMember")
        return TradeOffer(instance, obj.optLong("expiresAt", 0))
    }

    data class TradeOffer(val offering: EchoformInstance, val expiresAtMs: Long)

    fun encodeTradeAccept(originalEventId: String): JSONObject = JSONObject().apply {
        put("originalEventId", originalEventId)
    }

    fun encodeTradeCancel(originalEventId: String, reason: String): JSONObject = JSONObject().apply {
        put("originalEventId", originalEventId)
        put("reason", reason)
    }

    // ── Battle invite ───────────────────────────────────────────

    fun encodeBattleInvite(team: List<EchoformInstance>, ruleset: String, expiresAtMs: Long): JSONObject =
        JSONObject().apply {
            put("teamHash", hashTeam(team))
            put("ruleset", ruleset)
            put("expiresAt", expiresAtMs)
            put("teamPreview", JSONArray(team.take(6).map {
                JSONObject().apply {
                    put("speciesId", it.species.id)
                    put("level", it.level)
                }
            }))
        }

    data class BattleInvite(
        val teamHash: String,
        val ruleset: String,
        val expiresAtMs: Long,
        val teamPreview: List<Pair<String, Int>>,
    )

    fun decodeBattleInvite(obj: JSONObject): BattleInvite {
        val preview = obj.optJSONArray("teamPreview")?.let { arr ->
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                o.optString("speciesId") to o.optInt("level")
            }
        } ?: emptyList()
        return BattleInvite(
            teamHash = obj.optString("teamHash", ""),
            ruleset = obj.optString("ruleset", "tuxemon-pilot"),
            expiresAtMs = obj.optLong("expiresAt", 0),
            teamPreview = preview,
        )
    }

    fun encodeBattleInviteReply(originalEventId: String, accepted: Boolean): JSONObject = JSONObject().apply {
        put("originalEventId", originalEventId)
        put("accepted", accepted)
    }

    // ── Battle start (full team reveal) ─────────────────────────

    fun encodeBattleStart(rngSeed: Long, hostTeam: List<EchoformInstance>, guestTeam: List<EchoformInstance>): JSONObject =
        JSONObject().apply {
            put("rngSeed", rngSeed)
            put("hostTeam", JSONArray(hostTeam.map { encodeEchoform(it) }))
            put("guestTeam", JSONArray(guestTeam.map { encodeEchoform(it) }))
        }

    data class BattleStart(
        val rngSeed: Long,
        val hostTeam: List<EchoformInstance>,
        val guestTeam: List<EchoformInstance>,
    )

    fun decodeBattleStart(obj: JSONObject): BattleStart = BattleStart(
        rngSeed = obj.optLong("rngSeed"),
        hostTeam = decodeEchoforms(obj.optJSONArray("hostTeam")),
        guestTeam = decodeEchoforms(obj.optJSONArray("guestTeam")),
    )

    // ── Team reveal (carried inside BATTLE_INVITE_REPLY when used as
    //    a lobby-ready signal). Lets the host build BATTLE_START with the
    //    guest's real team, and the guest receive the host's team via
    //    BATTLE_START. No separate event-type — the team rides on the
    //    existing reply payload to keep the wire surface small. ──────────

    /** Encode a single team as JSON for lobby-ready exchange. */
    fun encodeTeam(team: List<EchoformInstance>): JSONArray =
        JSONArray(team.map { encodeEchoform(it) })

    /** Decode a team payload — empty list when absent or malformed. */
    fun decodeTeam(arr: JSONArray?): List<EchoformInstance> = decodeEchoforms(arr)

    // ── Battle action events ────────────────────────────────────

    fun encodeBattleMove(turn: Int, side: String, moveIdx: Int, stateHash: String): JSONObject =
        JSONObject().apply {
            put("turn", turn)
            put("side", side)
            put("moveIdx", moveIdx)
            put("stateHash", stateHash)
        }

    fun encodeBattleSwitch(turn: Int, side: String, partyIdx: Int): JSONObject = JSONObject().apply {
        put("turn", turn)
        put("side", side)
        put("partyIdx", partyIdx)
    }

    fun encodeBattleFaint(turn: Int, side: String, index: Int): JSONObject = JSONObject().apply {
        put("turn", turn)
        put("side", side)
        put("index", index)
    }

    fun encodeBattleEnd(winner: String, finalLog: List<BattleEvent>? = null): JSONObject = JSONObject().apply {
        put("winner", winner)  // "host" / "guest" / "draw"
        if (finalLog != null) {
            put("finalLog", JSONArray(finalLog.map { encodeBattleEvent(it) }))
        }
    }

    // ── Spectate / replay ──────────────────────────────────────

    fun encodeSpectateSubscribe(battleEventId: String): JSONObject = JSONObject().apply {
        put("battleEventId", battleEventId)
    }

    fun encodeReplayShare(
        rngSeed: Long,
        teams: Pair<List<EchoformInstance>, List<EchoformInstance>>,
        log: List<BattleEvent>,
    ): JSONObject = JSONObject().apply {
        put("rngSeed", rngSeed)
        put("hostTeam", JSONArray(teams.first.map { encodeEchoform(it) }))
        put("guestTeam", JSONArray(teams.second.map { encodeEchoform(it) }))
        put("log", JSONArray(log.map { encodeBattleEvent(it) }))
    }

    // ── Helpers ────────────────────────────────────────────────

    /**
     * Stable SHA-256 of a team — used as a commit-reveal to prevent the
     * receiver from peeking at the inviter's team before deciding.
     */
    fun hashTeam(team: List<EchoformInstance>): String {
        val s = team.joinToString("|") { "${it.species.id}:${it.level}:${it.currentVigor}/${it.maxVigor}" }
        val md = MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
        return md.joinToString("") { "%02x".format(it) }
    }

    /** Hash of a battle state — used for desync detection across peers. */
    fun hashState(playerHp: Int, opponentHp: Int, turn: Int): String {
        val s = "$turn|$playerHp|$opponentHp"
        val md = MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
        return md.joinToString("") { "%02x".format(it) }
    }

    /** Reuse SaveGameIO's echoform encoder so the wire format stays canonical. */
    private fun encodeEchoform(instance: EchoformInstance): JSONObject {
        // SaveGameIO's encoder is package-private; we round-trip through a save stub.
        val stub = saveStub(instance)
        val saveObj = SaveGameIO.toJson(stub)
        return saveObj.getJSONObject("party").getJSONArray("members").getJSONObject(0)
    }

    private fun decodeEchoforms(arr: JSONArray?): List<EchoformInstance> {
        if (arr == null) return emptyList()
        val envelope = JSONObject().apply {
            put("party", JSONObject().apply {
                put("activeIndex", 0)
                put("members", arr)
            })
        }
        return SaveGameIO.fromJson(envelope).party.members
    }

    private fun saveStub(instance: EchoformInstance): SaveGame = SaveGame(
        playerName = "wire",
        party = Party(members = listOf(instance), activeIndex = 0),
    )

    private fun encodeBattleEvent(event: BattleEvent): JSONObject {
        val o = JSONObject().apply {
            put("turn", event.turn)
            put("type", event::class.simpleName ?: "?")
        }
        when (event) {
            is BattleEvent.TechniqueDeclared -> {
                o.put("side", event.side.name); o.put("techniqueId", event.techniqueId); o.put("techniqueName", event.techniqueName)
            }
            is BattleEvent.TechniqueResolved -> {
                o.put("side", event.side.name); o.put("techniqueId", event.techniqueId)
                o.put("damage", event.damage); o.put("effectiveness", event.effectiveness)
                o.put("crit", event.crit); o.put("missed", event.missed); o.put("stab", event.stab)
            }
            is BattleEvent.Faint -> o.put("side", event.side.name)
            is BattleEvent.MatchEnd -> o.put("winner", event.winner.name)
            is BattleEvent.StatusApplied -> { o.put("side", event.side.name); o.put("statusSlug", event.statusSlug) }
            is BattleEvent.StatusTickDamage -> { o.put("side", event.side.name); o.put("statusSlug", event.statusSlug); o.put("damage", event.damage) }
            is BattleEvent.StatusBlockedTurn -> { o.put("side", event.side.name); o.put("statusSlug", event.statusSlug); o.put("reason", event.reason) }
            is BattleEvent.StatusExpired -> { o.put("side", event.side.name); o.put("statusSlug", event.statusSlug) }
            is BattleEvent.TurnStart -> Unit
        }
        return o
    }
}
