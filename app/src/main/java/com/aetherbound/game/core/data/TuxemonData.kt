package com.aetherbound.game.core.data

import android.content.Context
import com.aetherbound.game.core.Aspect
import org.json.JSONArray
import org.json.JSONObject

/**
 * JSON-driven loaders for Tuxemon's full content database, imported into
 * `app/src/main/assets/game/data/` by `tools/ai/import_tuxemon.py`.
 *
 * Files (counts as of import 2026-04-29):
 *   monsters.json    — 411 species
 *   techniques.json  — 274 moves
 *   items.json       — 223 items
 *   statuses.json    —  35 status effects
 *   elements.json    —  13 type chart (already used by Aspect.kt)
 *
 * Each loader is a singleton with lazy first-call init and cached results.
 * Call `TuxemonDex.load(ctx)` once at app start (or first access) — it reads
 * the JSON, parses into `Tuxemon*` data classes, and serves O(1) lookups
 * from then on.
 *
 * License: content is CC-BY-SA-3.0 (Tuxemon). Attribution lives in
 * docs/asset-credits.md.
 */

// ───────────────────────────────────────────────────────────────────
// Data classes — denormalized snapshots of Tuxemon's YAML schema.
// ───────────────────────────────────────────────────────────────────

data class TuxemonMonster(
    val slug: String,
    val species: String,
    val shape: String,
    val stage: String,
    val types: List<Aspect>,
    val heightCm: Int,
    val weightKg: Int,
    val catchRate: Double,
    val lowerCatchResistance: Double,
    val upperCatchResistance: Double,
    val moveset: List<MovesetEntry>,
    val terrains: List<String>,
    val tags: List<String>,
    val evolvesInto: List<String>,
    val evolvesFrom: List<String>,
) {
    val primaryType: Aspect get() = types.firstOrNull() ?: Aspect.NORMAL
    val secondaryType: Aspect? get() = types.getOrNull(1)?.takeIf { it != primaryType }
}

data class MovesetEntry(
    val levelLearned: Int,
    val technique: String,
    val learningMethod: String?, // "fallback" or null
)

data class TuxemonTechnique(
    val slug: String,
    val techId: Int,
    val types: List<Aspect>,
    val sort: String,        // damage / status / meta
    val category: String,    // basic / charging / etc
    val power: Double,       // Tuxemon's tech power (multiplier-style)
    val accuracy: Double,    // 0.0..1.0
    val potency: Double,
    val recharge: Int,
    val speed: String,       // very_slow / slow / normal / fast / very_fast
    val range: String,       // melee / reliable / touch / ranged / reach / etc
    val tags: List<String>,
    val animation: String?,  // visuals.animation key
    val sfx: String?,        // sound.sfx key
) {
    val primaryType: Aspect get() = types.firstOrNull() ?: Aspect.NORMAL
}

data class TuxemonItem(
    val slug: String,
    val sort: String,        // utility / consumable / quest / equipment
    val category: String,
    val sprite: String,      // path inside Tuxemon's gfx/items/<x>.png
    val usableIn: List<String>, // WorldState / CombatState
    val consumable: Boolean,
    /** Raw `effects: [{type, parameters: [...]}]` from Tuxemon's JSON. */
    val effects: List<ItemEffectSpec> = emptyList(),
)

/**
 * One entry in an item's `effects` list. Tuxemon's schema is uniform:
 *   { "type": "heal", "parameters": ["-100", "fixed"] }
 *
 * The [type] is dispatched by [ItemEngine]; [parameters] are positional
 * and per-type (Tuxemon's Python engine reads them by index).
 */
data class ItemEffectSpec(
    val type: String,
    val parameters: List<String> = emptyList(),
)

data class TuxemonStatus(
    val slug: String,
    val condId: Int,
    val sort: String,
    val category: String,    // positive / negative / neutral
    val icon: String?,
    val statModifiers: Map<String, StatModifier>,
)

data class StatModifier(
    val stat: String,
    val value: Double,
    val operation: String,   // "*", "+", etc
)

// ───────────────────────────────────────────────────────────────────
// Loader — generic helper to slurp an asset JSON file as a JSONArray.
// ───────────────────────────────────────────────────────────────────

private fun readJsonArray(ctx: Context, assetPath: String): JSONArray {
    ctx.assets.open(assetPath).use { stream ->
        val text = stream.bufferedReader(Charsets.UTF_8).readText()
        return JSONArray(text)
    }
}

private fun JSONArray.iter(): Sequence<JSONObject> = sequence {
    for (i in 0 until length()) yield(getJSONObject(i))
}

private fun JSONArray.toStringList(): List<String> =
    (0 until length()).map { getString(it) }

private fun JSONObject.optStringOrNull(key: String): String? =
    if (isNull(key) || !has(key)) null else getString(key)

private fun JSONObject.optAspectList(key: String): List<Aspect> {
    if (!has(key) || isNull(key)) return emptyList()
    val arr = getJSONArray(key)
    return (0 until arr.length()).mapNotNull { Aspect.fromSlug(arr.getString(it)) }
}

// ───────────────────────────────────────────────────────────────────
// TuxemonDex — 411 monsters
// ───────────────────────────────────────────────────────────────────

object TuxemonDex {
    private var cached: Map<String, TuxemonMonster>? = null

    fun load(ctx: Context): Map<String, TuxemonMonster> {
        cached?.let { return it }
        val arr = readJsonArray(ctx, "game/data/monsters.json")
        val map = LinkedHashMap<String, TuxemonMonster>(arr.length())
        for (obj in arr.iter()) {
            val moveset = obj.optJSONArray("moveset")?.let { ms ->
                (0 until ms.length()).map { i ->
                    val m = ms.getJSONObject(i)
                    MovesetEntry(
                        levelLearned = m.optInt("level_learned", 1),
                        technique = m.optString("technique", "struggle"),
                        learningMethod = m.optStringOrNull("learning_method"),
                    )
                }
            } ?: emptyList()

            // history list contains evolution chain — we project it to
            // evolvesInto/evolvesFrom for the *current* slug only.
            val slug = obj.getString("slug")
            var evolvesInto: List<String> = emptyList()
            var evolvesFrom: List<String> = emptyList()
            obj.optJSONArray("history")?.let { hist ->
                for (i in 0 until hist.length()) {
                    val h = hist.getJSONObject(i)
                    if (h.optString("slug") == slug) {
                        evolvesInto = h.optJSONArray("evolves_into")?.toStringList() ?: emptyList()
                        evolvesFrom = h.optJSONArray("evolves_from")?.toStringList() ?: emptyList()
                    }
                }
            }

            val mon = TuxemonMonster(
                slug = slug,
                species = obj.optString("species", slug),
                shape = obj.optString("shape", "default"),
                stage = obj.optString("stage", "basic"),
                types = obj.optAspectList("types"),
                heightCm = obj.optInt("height", 0),
                weightKg = obj.optInt("weight", 0),
                catchRate = obj.optDouble("catch_rate", 100.0),
                lowerCatchResistance = obj.optDouble("lower_catch_resistance", 1.0),
                upperCatchResistance = obj.optDouble("upper_catch_resistance", 1.0),
                moveset = moveset,
                terrains = obj.optJSONArray("terrains")?.toStringList() ?: emptyList(),
                tags = obj.optJSONArray("tags")?.toStringList() ?: emptyList(),
                evolvesInto = evolvesInto,
                evolvesFrom = evolvesFrom,
            )
            map[slug] = mon
        }
        cached = map
        return map
    }

    fun bySlug(ctx: Context, slug: String): TuxemonMonster? = load(ctx)[slug]
    fun all(ctx: Context): Collection<TuxemonMonster> = load(ctx).values
    fun count(ctx: Context): Int = load(ctx).size
}

// ───────────────────────────────────────────────────────────────────
// TuxemonTechniqueDex — 274 moves
// ───────────────────────────────────────────────────────────────────

object TuxemonTechniqueDex {
    private var cached: Map<String, TuxemonTechnique>? = null

    fun load(ctx: Context): Map<String, TuxemonTechnique> {
        cached?.let { return it }
        val arr = readJsonArray(ctx, "game/data/techniques.json")
        val map = LinkedHashMap<String, TuxemonTechnique>(arr.length())
        for (obj in arr.iter()) {
            val slug = obj.getString("slug")
            val visuals = obj.optJSONObject("visuals")
            val sound = obj.optJSONObject("sound")
            val tech = TuxemonTechnique(
                slug = slug,
                techId = obj.optInt("tech_id", 0),
                types = obj.optAspectList("types"),
                sort = obj.optString("sort", "damage"),
                category = obj.optString("category", "basic"),
                power = obj.optDouble("power", 1.0),
                accuracy = obj.optDouble("accuracy", 1.0),
                potency = obj.optDouble("potency", 0.0),
                recharge = obj.optInt("recharge", 0),
                speed = obj.optString("speed", "normal"),
                range = obj.optString("range", "melee"),
                tags = obj.optJSONArray("tags")?.toStringList() ?: emptyList(),
                animation = visuals?.optStringOrNull("animation"),
                sfx = sound?.optStringOrNull("sfx"),
            )
            map[slug] = tech
        }
        cached = map
        return map
    }

    fun bySlug(ctx: Context, slug: String): TuxemonTechnique? = load(ctx)[slug]
    fun all(ctx: Context): Collection<TuxemonTechnique> = load(ctx).values
    fun count(ctx: Context): Int = load(ctx).size
}

// ───────────────────────────────────────────────────────────────────
// TuxemonItemDex — 223 items
// ───────────────────────────────────────────────────────────────────

object TuxemonItemDex {
    private var cached: Map<String, TuxemonItem>? = null

    fun load(ctx: Context): Map<String, TuxemonItem> {
        cached?.let { return it }
        val arr = readJsonArray(ctx, "game/data/items.json")
        val map = LinkedHashMap<String, TuxemonItem>(arr.length())
        for (obj in arr.iter()) {
            val slug = obj.getString("slug")
            val behaviors = obj.optJSONObject("behaviors")
            val effects = obj.optJSONArray("effects")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val e = arr.getJSONObject(i)
                    val params = e.optJSONArray("parameters")?.let { p ->
                        (0 until p.length()).map { p.getString(it) }
                    } ?: emptyList()
                    ItemEffectSpec(
                        type = e.optString("type", "?"),
                        parameters = params,
                    )
                }
            } ?: emptyList()
            val item = TuxemonItem(
                slug = slug,
                sort = obj.optString("sort", "utility"),
                category = obj.optString("category", "none"),
                sprite = obj.optString("sprite", ""),
                usableIn = obj.optJSONArray("usable_in")?.toStringList() ?: emptyList(),
                consumable = behaviors?.optBoolean("consumable", false) ?: false,
                effects = effects,
            )
            map[slug] = item
        }
        cached = map
        return map
    }

    fun bySlug(ctx: Context, slug: String): TuxemonItem? = load(ctx)[slug]
    fun all(ctx: Context): Collection<TuxemonItem> = load(ctx).values
    fun count(ctx: Context): Int = load(ctx).size
}

// ───────────────────────────────────────────────────────────────────
// TuxemonStatusDex — 35 status effects
// ───────────────────────────────────────────────────────────────────

object TuxemonStatusDex {
    private var cached: Map<String, TuxemonStatus>? = null

    fun load(ctx: Context): Map<String, TuxemonStatus> {
        cached?.let { return it }
        val arr = readJsonArray(ctx, "game/data/statuses.json")
        val map = LinkedHashMap<String, TuxemonStatus>(arr.length())
        for (obj in arr.iter()) {
            val slug = obj.getString("slug")
            val statModifiers = mutableMapOf<String, StatModifier>()
            obj.optJSONObject("stat_modifiers")?.let { sm ->
                val keys = sm.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val mod = sm.getJSONObject(k)
                    statModifiers[k] = StatModifier(
                        stat = k,
                        value = mod.optDouble("value", 1.0),
                        operation = mod.optString("operation", "*"),
                    )
                }
            }
            val status = TuxemonStatus(
                slug = slug,
                condId = obj.optInt("cond_id", 0),
                sort = obj.optString("sort", "meta"),
                category = obj.optString("category", "neutral"),
                icon = obj.optStringOrNull("icon"),
                statModifiers = statModifiers,
            )
            map[slug] = status
        }
        cached = map
        return map
    }

    fun bySlug(ctx: Context, slug: String): TuxemonStatus? = load(ctx)[slug]
    fun all(ctx: Context): Collection<TuxemonStatus> = load(ctx).values
    fun count(ctx: Context): Int = load(ctx).size
}

// ───────────────────────────────────────────────────────────────────
// One-shot helper to warm all caches in parallel-safe order.
// Call from a background thread (assets I/O is small but blocking).
// ───────────────────────────────────────────────────────────────────

object TuxemonContent {
    data class Stats(
        val monsters: Int,
        val techniques: Int,
        val items: Int,
        val statuses: Int,
    )

    fun loadAll(ctx: Context): Stats = Stats(
        monsters = TuxemonDex.count(ctx),
        techniques = TuxemonTechniqueDex.count(ctx),
        items = TuxemonItemDex.count(ctx),
        statuses = TuxemonStatusDex.count(ctx),
    )
}
