package com.aetherbound.game.dialogue

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Content pack for the dialogue system. Bundles three layers:
 *
 *  1. **Literal map** (`msgid → text`) — used for KEEP_STRUCTURE,
 *     HIGH_REUSE, ITEM_LABEL, TRAINER_OUTCOME and pre-translated
 *     QUEST_LORE. The text is returned verbatim (then normalized).
 *  2. **Category Tracery grammar** — for msgids that don't have a
 *     literal mapping, the resolver routes by category (NPC_FLAVOR,
 *     WORLD_SIGN, TRAINER_PRE) into a category-specific Tracery
 *     grammar that generates a fresh line on every interaction.
 *  3. **Slot overrides** (optional, daily-pack injected) — values
 *     for `today_weather`, `current_rumor`, `merchant_modifier_water`
 *     etc. that ride on top of the static grammar without touching
 *     the schema.
 *
 * Packs are loaded from `assets/content/` at app boot. The loader is
 * resilient — missing files fall back to defaults rather than crashing.
 */
data class ContentPack(
    /** msgid → final text. First lookup the resolver hits. */
    val literals: Map<String, String>,
    /** Per-category Tracery grammar. Key = category (e.g. "npc_flavor"). */
    val grammars: Map<String, Map<String, List<String>>>,
    /** Daily-pack slot overrides (currently empty until DailyPackFetcher lands). */
    val slotOverrides: Map<String, String> = emptyMap(),
) {

    /** True when this pack has nothing — used to detect missing assets. */
    val isEmpty: Boolean get() = literals.isEmpty() && grammars.isEmpty()

    companion object {

        private const val ROOT = "content"

        /**
         * Load the bundled pack from `assets/content/`. Always succeeds —
         * missing files yield empty maps so the resolver can at least
         * fall back to msgid-as-text.
         */
        fun load(ctx: Context): ContentPack {
            val literals = mutableMapOf<String, String>()
            val grammars = mutableMapOf<String, Map<String, List<String>>>()

            // ── Layer 1: literal dialogue files ──────────────────────────
            for (name in listOf(
                "keep_structure", "high_reuse", "trainer_outcome",
                "item_label", "quest_lore", "world_sign_literals",
            )) {
                readJsonObject(ctx, "$ROOT/dialogues/$name.json")?.let { obj ->
                    obj.keys().forEach { k ->
                        val v = obj.optString(k, "")
                        if (v.isNotBlank()) literals[k] = v
                    }
                }
            }

            // ── Layer 2: category grammars ───────────────────────────────
            for (name in listOf(
                "npc_flavor", "world_sign", "trainer_pre",
                "barmaid", "alchemist", "aviator", "merchant", "child",
            )) {
                readJsonObject(ctx, "$ROOT/grammar/$name.json")?.let { obj ->
                    grammars[name] = obj.toRuleMap()
                }
            }

            return ContentPack(literals = literals, grammars = grammars)
        }

        // ── helpers ─────────────────────────────────────────────────────

        private fun readJsonObject(ctx: Context, assetPath: String): JSONObject? = try {
            ctx.assets.open(assetPath).use { stream ->
                val bytes = stream.readBytes()
                JSONObject(String(bytes, Charsets.UTF_8))
            }
        } catch (_: Exception) { null }

        /**
         * Convert a Tracery JSON object into the engine's rules map.
         * Each value can be either a single string or an array of strings.
         */
        private fun JSONObject.toRuleMap(): Map<String, List<String>> {
            val out = mutableMapOf<String, List<String>>()
            keys().forEach { k ->
                val v = opt(k)
                when (v) {
                    is String -> out[k] = listOf(v)
                    is JSONArray -> {
                        val list = mutableListOf<String>()
                        for (i in 0 until v.length()) list += v.optString(i, "")
                        out[k] = list.filter { it.isNotEmpty() }
                    }
                }
            }
            return out
        }
    }
}
