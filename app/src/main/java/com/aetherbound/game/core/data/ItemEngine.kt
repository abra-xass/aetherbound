package com.aetherbound.game.core.data

import com.aetherbound.game.core.EchoformInstance

/**
 * Minimal item-effect engine for Tuxemon-imported items.
 *
 * Tuxemon items expose `effects: [{type, ...}]` lists in JSON. We don't
 * (yet) parse the per-type sub-payload (e.g. heal amount, capture rate),
 * because the JSON schema varies wildly per item — Tuxemon's Python engine
 * dispatches by `type` and reads side-channel attributes. For now we model
 * the *intent* with [ItemEffectKind], leaving the per-effect numeric
 * resolution to gameplay code that wants to honour an item's contract.
 *
 * Most common effect kinds (counts across 223 imported items):
 *   food_preference 25   capture 25   evolve 21   learn_tm 14
 *   switch_type 13       heal 10      change_stat 6   buff 5
 *   learn_mm 5           gain_xp 4    restore 3       fishing 3
 *
 * Use:
 *   val r = ItemEngine.use(item, target, ItemContext.battle())
 *   when (r) {
 *     is ItemResult.HpRestored -> println("Restored ${r.amount} HP")
 *     is ItemResult.NotApplicable -> println("Can't use here")
 *     ...
 *   }
 */

enum class ItemEffectKind(val slug: String) {
    HEAL("heal"),
    RESTORE("restore"),
    CAPTURE("capture"),
    CAPTURE_COMBINED("capture_combined"),
    EVOLVE("evolve"),
    LEARN_TM("learn_tm"),
    LEARN_MM("learn_mm"),
    SWITCH_TYPE("switch_type"),
    CHANGE_STAT("change_stat"),
    BUFF("buff"),
    GAIN_XP("gain_xp"),
    FOOD_PREFERENCE("food_preference"),
    REPELLENT("repellent"),
    BIVOUAC("bivouac"),
    FISHING("fishing"),
    REMOVE_ENTITY("remove_entity"),
    TELEPORT("teleport_item"),
    PARK("park"),
    DIE("die"),
    DRONE("drone"),
    UNKNOWN("?");

    companion object {
        private val bySlug = values().associateBy { it.slug }
        fun fromSlug(slug: String): ItemEffectKind = bySlug[slug] ?: UNKNOWN
    }
}

/** Where the player is when using the item, mirrors Tuxemon's `usable_in`. */
enum class ItemContextKind { World, Combat, Both }

data class ItemContext(
    val kind: ItemContextKind,
    /** True if the target Echoform is fainted (matters for revives). */
    val targetFainted: Boolean = false,
) {
    companion object {
        fun world(targetFainted: Boolean = false) = ItemContext(ItemContextKind.World, targetFainted)
        fun battle(targetFainted: Boolean = false) = ItemContext(ItemContextKind.Combat, targetFainted)
    }
}

sealed class ItemResult {
    /** HP restored on the target. amount > 0. */
    data class HpRestored(val amount: Int) : ItemResult()

    /** Status condition cleared by name. */
    data class StatusCleared(val statusSlug: String) : ItemResult()

    /** Ball-style capture attempt; success determined by caller using [chance]. */
    data class CaptureAttempt(val baseChance: Double, val combined: Boolean = false) : ItemResult()

    /** Triggers an evolution into [targetSlug]. */
    data class EvolveTo(val targetSlug: String) : ItemResult()

    /** Teaches a technique to the target by slug. */
    data class TaughtTechnique(val techniqueSlug: String, val mode: String) : ItemResult()

    /** XP awarded to the target. */
    data class XpAwarded(val amount: Int) : ItemResult()

    /** Stat boost / buff applied for [turns]. */
    data class StatChanged(val stat: String, val delta: Int, val turns: Int) : ItemResult()

    /** Wild-encounter-rate suppressor active for [steps]. */
    data class RepellentApplied(val steps: Int) : ItemResult()

    /** Generic effect that the engine recognised but didn't materialise. */
    data class EffectAcknowledged(val kind: ItemEffectKind) : ItemResult()

    /** Item cannot be used in this context. */
    data class NotApplicable(val reason: String) : ItemResult()

    /** Unknown effect type — fail soft. */
    data class Unknown(val slug: String) : ItemResult()
}

object ItemEngine {

    /**
     * Apply [item] to [target] in [ctx]. Returns one [ItemResult] per
     * effect entry; most items have exactly one effect so single-result
     * helpers are also provided.
     */
    fun use(item: TuxemonItem, target: EchoformInstance?, ctx: ItemContext): List<ItemResult> {
        // Tuxemon stores usable_in as ["WorldState"] / ["CombatState"] / both.
        // (Empty string in some imports means usable everywhere.)
        if (!isContextAllowed(item, ctx)) {
            return listOf(ItemResult.NotApplicable("usable_in=${item.usableIn} but ctx=${ctx.kind}"))
        }

        // Prefer real effects[] payloads from JSON; fall back to slug heuristic
        // for items whose effects list is empty or malformed.
        val specs = item.effects.ifEmpty {
            inferEffects(item, target).map { ItemEffectSpec(type = it.slug) }
        }
        return specs.map { spec -> dispatchSpec(spec, item, target, ctx) }
    }

    private fun isContextAllowed(item: TuxemonItem, ctx: ItemContext): Boolean {
        val u = item.usableIn.map { it.lowercase() }.filter { it.isNotEmpty() }
        if (u.isEmpty()) return true
        val matches = when (ctx.kind) {
            ItemContextKind.World -> u.any { "world" in it }
            ItemContextKind.Combat -> u.any { "combat" in it }
            ItemContextKind.Both -> true
        }
        return matches
    }

    /**
     * Heuristic: derive effect kinds from sort + category + name.
     *
     * The full Tuxemon JSON has the explicit `effects: [{type: …}]` list, but
     * our [TuxemonItem] data class only retains [TuxemonItem.sort] and
     * [TuxemonItem.category]. We infer the kind from those plus the slug
     * prefix — good enough for the Aetherbound pilot, can be replaced with
     * full effects-array parsing later by extending [TuxemonItemDex].
     */
    private fun inferEffects(item: TuxemonItem, target: EchoformInstance?): List<ItemEffectKind> {
        val s = item.slug.lowercase()
        return when {
            s.contains("potion") || s.contains("revive") -> listOf(ItemEffectKind.HEAL)
            s.contains("ball") || s.contains("capture") -> listOf(ItemEffectKind.CAPTURE)
            s.contains("stone") -> listOf(ItemEffectKind.EVOLVE)
            s.contains("repel") -> listOf(ItemEffectKind.REPELLENT)
            s.startsWith("tm_") || s.startsWith("hm_") || s.contains("technical_machine") ->
                listOf(ItemEffectKind.LEARN_TM)
            s.contains("xp_") || s.contains("rare_candy") -> listOf(ItemEffectKind.GAIN_XP)
            item.sort == "consumable" -> listOf(ItemEffectKind.HEAL) // safe default
            else -> listOf(ItemEffectKind.UNKNOWN)
        }
    }

    /**
     * Dispatch a single [ItemEffectSpec] from the JSON `effects[]` array.
     * Reads positional parameters per Tuxemon's per-type contract (see
     * `Tuxemon/tuxemon/item/effects/<type>.py`).
     */
    private fun dispatchSpec(
        spec: ItemEffectSpec,
        item: TuxemonItem,
        target: EchoformInstance?,
        ctx: ItemContext,
    ): ItemResult {
        val kind = ItemEffectKind.fromSlug(spec.type)
        val params = spec.parameters
        return when (kind) {
            ItemEffectKind.HEAL -> {
                if (target == null) ItemResult.NotApplicable("no target")
                else {
                    // Tuxemon heal params: ["amount", "mode"]
                    //   amount: int (positive = heal, negative = damage)
                    //   mode:   "fixed" or "percent"
                    val rawAmount = params.getOrNull(0)?.toDoubleOrNull() ?: 0.0
                    val mode = params.getOrNull(1) ?: "fixed"
                    val amount = if (mode == "percent") (target.maxVigor * rawAmount / 100.0).toInt()
                                 else rawAmount.toInt()
                    if (amount < 0) {
                        // negative heal = direct damage (e.g. bite_of_despair)
                        ItemResult.HpRestored(amount = amount)
                    } else if (amount == 0) {
                        ItemResult.HpRestored(amount = defaultHealAmount(item, target))
                    } else {
                        ItemResult.HpRestored(amount = amount.coerceAtMost(target.maxVigor))
                    }
                }
            }
            ItemEffectKind.RESTORE -> {
                // Tuxemon restore params optional ["status_slug"], else clears all.
                val statusSlug = params.getOrNull(0) ?: "any"
                ItemResult.StatusCleared(statusSlug)
            }
            ItemEffectKind.CAPTURE -> {
                // Plain Tuxeball: base chance 0.5
                ItemResult.CaptureAttempt(baseChance = 0.5)
            }
            ItemEffectKind.CAPTURE_COMBINED -> {
                // params: [target_kind, label, base_chance, multiplier]
                val baseChance = params.getOrNull(2)?.toDoubleOrNull() ?: 0.5
                val mult = params.getOrNull(3)?.toDoubleOrNull() ?: 1.0
                ItemResult.CaptureAttempt(
                    baseChance = (baseChance * mult).coerceIn(0.0, 1.0),
                    combined = true,
                )
            }
            ItemEffectKind.EVOLVE -> ItemResult.EvolveTo(targetSlug = target?.species?.id ?: "")
            ItemEffectKind.LEARN_TM -> {
                // params: [technique_slug]
                val tech = params.getOrNull(0) ?: item.slug.removePrefix("tm_")
                ItemResult.TaughtTechnique(techniqueSlug = tech, mode = "tm")
            }
            ItemEffectKind.LEARN_MM -> {
                val tech = params.getOrNull(0) ?: item.slug.removePrefix("mm_")
                ItemResult.TaughtTechnique(techniqueSlug = tech, mode = "mm")
            }
            ItemEffectKind.GAIN_XP -> {
                // params: [xp_amount]
                val xp = params.getOrNull(0)?.toIntOrNull() ?: 100
                ItemResult.XpAwarded(amount = xp)
            }
            ItemEffectKind.CHANGE_STAT -> {
                // params: [stat_slug, delta_fraction]
                val stat = params.getOrNull(0) ?: "?"
                val delta = params.getOrNull(1)?.toDoubleOrNull() ?: 0.05
                ItemResult.StatChanged(stat = stat, delta = (delta * 100).toInt(), turns = -1)
            }
            ItemEffectKind.BUFF -> {
                // params: [stat_slug, multiplier]
                val stat = params.getOrNull(0) ?: "all"
                val mult = params.getOrNull(1)?.toDoubleOrNull() ?: 0.2
                ItemResult.StatChanged(stat = stat, delta = (mult * 100).toInt(), turns = 5)
            }
            ItemEffectKind.REPELLENT -> {
                val steps = params.getOrNull(0)?.toIntOrNull() ?: 100
                ItemResult.RepellentApplied(steps = steps)
            }
            ItemEffectKind.UNKNOWN -> ItemResult.Unknown(spec.type)
            else -> ItemResult.EffectAcknowledged(kind)
        }
    }

    private fun defaultHealAmount(item: TuxemonItem, target: EchoformInstance): Int {
        // Tier the heal by item slug. Aetherbound's pilot tunings:
        val maxHp = target.maxVigor
        return when {
            "super" in item.slug || "great" in item.slug -> (maxHp * 0.5).toInt()
            "max" in item.slug || "ultra" in item.slug -> maxHp
            "revive" in item.slug -> maxHp / 2
            else -> 30  // baseline potion
        }.coerceIn(1, maxHp)
    }
}
