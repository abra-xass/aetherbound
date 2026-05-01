package com.aetherbound.game.core.data

import android.content.Context
import org.json.JSONObject
import kotlin.random.Random

/**
 * In-memory registry of all 152 Aetherbound evolution chains, loaded
 * lazily from `assets/content/evolution_chains.json`.
 *
 * Each chain has 2 or 3 stages. The trigger is one of:
 *   - `level`         (probability ramp; bypassed if stage already evolved)
 *   - `stone`         (use a specific element-stone item)
 *   - `moon_full`     (real-clock full moon, ±1 day)
 *   - `moon_new`      (real-clock new moon, ±1 day)
 *   - `weather_storm` (DailyPack weather = STORM)
 *   - `weather_rain`  (DailyPack weather = RAIN)
 *
 * Probability curves (level trigger only):
 *   stage 1 evo: P(level) = max(0, (level - 10) / 10)   → 100% at lvl 20
 *   stage 2 evo: P(level) = max(0, (level - 30) / 15)   → 100% at lvl 45
 *
 * Stones / moon / weather triggers fire **immediately** when matched —
 * they bypass the probability roll entirely.
 */
object EvolutionChainRegistry {

    /** One member of a chain — either basic / mid / final. */
    data class ChainMember(
        val name: String,
        val stage: Int,        // 1 = basic, 2 = mid, 3 = final
        val totalStages: Int,
    )

    /** Full chain with all members and trigger spec. */
    data class Chain(
        val chainId: String,
        val aspect: String,
        val members: List<String>,
        val triggerType: String,
        val stoneItem: String?,
    )

    private var cached: List<Chain>? = null
    private var memberIndex: Map<String, Pair<Chain, Int>>? = null

    fun load(ctx: Context): List<Chain> {
        cached?.let { return it }
        val raw = runCatching {
            ctx.assets.open("content/evolution_chains.json").use { it.readBytes() }
                .toString(Charsets.UTF_8)
        }.getOrNull() ?: run { cached = emptyList(); return emptyList() }
        val obj = JSONObject(raw)
        val arr = obj.optJSONArray("chains") ?: return emptyList<Chain>().also { cached = it }
        val list = mutableListOf<Chain>()
        for (i in 0 until arr.length()) {
            val c = arr.getJSONObject(i)
            val members = c.optJSONArray("members")?.let { m ->
                List(m.length()) { m.getString(it) }
            } ?: emptyList()
            list += Chain(
                chainId = c.optString("chainId"),
                aspect = c.optString("aspect"),
                members = members,
                triggerType = c.optString("triggerType", "level"),
                stoneItem = c.optString("stoneItem", "").takeIf { it.isNotEmpty() },
            )
        }
        cached = list
        memberIndex = list.flatMap { ch ->
            ch.members.mapIndexed { idx, name -> name to (ch to idx) }
        }.toMap()
        return list
    }

    /** Look up the chain entry for [name]. Returns null if standalone. */
    fun forMember(ctx: Context, name: String): Pair<Chain, Int>? {
        load(ctx)
        return memberIndex?.get(name)
    }

    /** Next-stage name for [name] in its chain, or null if final / standalone. */
    fun nextStage(ctx: Context, name: String): String? {
        val (chain, idx) = forMember(ctx, name) ?: return null
        return chain.members.getOrNull(idx + 1)
    }

    /**
     * Probability that an Echoform of [name] at [level] evolves on a
     * level-up roll, given its trigger type is `level`. Returns 0 for
     * non-level triggers (those use exact-match conditions, not chance).
     */
    fun levelEvolutionProbability(ctx: Context, name: String, level: Int): Float {
        val (chain, idx) = forMember(ctx, name) ?: return 0f
        if (chain.triggerType != "level") return 0f
        if (idx + 1 >= chain.members.size) return 0f    // already final
        return when (idx) {
            0 -> ((level - 10) / 10f).coerceIn(0f, 1f)    // basic → mid
            1 -> ((level - 30) / 15f).coerceIn(0f, 1f)    // mid → final
            else -> 0f
        }
    }

    /**
     * Roll for level-up evolution. Returns the next-stage name if the
     * roll succeeds, null otherwise. Use [Random] for testability.
     */
    fun rollLevelEvolution(ctx: Context, name: String, level: Int, rng: Random): String? {
        val p = levelEvolutionProbability(ctx, name, level)
        if (p <= 0f) return null
        if (rng.nextFloat() <= p) return nextStage(ctx, name)
        return null
    }
}
