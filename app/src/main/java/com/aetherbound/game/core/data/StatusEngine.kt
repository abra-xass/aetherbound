package com.aetherbound.game.core.data

import com.aetherbound.game.core.EchoformInstance
import com.aetherbound.game.core.StatKey

/**
 * Status-effect engine for Tuxemon-imported conditions.
 *
 * Each [TuxemonStatus] carries `stat_modifiers` like:
 *   { "speed": { "value": 0.5, "operation": "*" },
 *     "dodge": { "value": 0.5, "operation": "*" } }
 *
 * The engine applies these multiplicatively to combat stats while the
 * status is active, ticks per turn for damage/heal categories, and
 * supports stacking rules (positive/negative replace each other per
 * Tuxemon's `on_positive_status` / `on_negative_status` flags).
 *
 * 35 imported statuses cover all canonical Pokémon-like conditions:
 *   poisoned, burned, frozen, paralyzed, sleeping, confused, blinded,
 *   wild, hardshell, charging, recharging, focused, etc.
 */

/** Map Tuxemon stat slugs to Aetherbound's [StatKey]. */
private val STAT_SLUG_TO_KEY: Map<String, StatKey> = mapOf(
    "hp"       to StatKey.VIGOR,
    "vigor"    to StatKey.VIGOR,
    "armour"   to StatKey.GUARD,
    "armor"    to StatKey.GUARD,
    "guard"    to StatKey.GUARD,
    "melee"    to StatKey.FORCE,
    "force"    to StatKey.FORCE,
    "ranged"   to StatKey.FOCUS,
    "focus"    to StatKey.FOCUS,
    "dodge"    to StatKey.WARD,
    "ward"     to StatKey.WARD,
    "speed"    to StatKey.TEMPO,
    "tempo"    to StatKey.TEMPO,
)

data class StatusInstance(
    val status: TuxemonStatus,
    /** Turns remaining; 0 = expires this turn, -1 = permanent until cured. */
    val turnsRemaining: Int = -1,
    /** Stack count (poison/burn intensity). */
    val stacks: Int = 1,
)

sealed class StatusTickResult {
    /** Status persists for another turn. */
    data class Continue(val instance: StatusInstance) : StatusTickResult()

    /** Status expires after this tick (timer ran out, cure, etc). */
    object Expired : StatusTickResult()

    /** Status deals damage this turn (poison/burn). */
    data class DamageDealt(val instance: StatusInstance, val damage: Int) : StatusTickResult()

    /** Status restores HP this turn (regen). */
    data class HpRestored(val instance: StatusInstance, val amount: Int) : StatusTickResult()

    /** Holder cannot act (paralyze proc, sleep, freeze, confuse). */
    data class Skipped(val instance: StatusInstance, val reason: String) : StatusTickResult()
}

object StatusEngine {

    /**
     * Combine all active statuses into a per-stat multiplier map.
     * stat-mod operation `"*"` is multiplicative, `"+"` additive.
     */
    fun statMultipliers(active: List<StatusInstance>): Map<StatKey, Double> {
        val out = StatKey.values().associateWith { 1.0 }.toMutableMap()
        for (inst in active) {
            for ((slug, mod) in inst.status.statModifiers) {
                val key = STAT_SLUG_TO_KEY[slug.lowercase()] ?: continue
                val current = out.getValue(key)
                out[key] = when (mod.operation) {
                    "*" -> current * mod.value
                    "+" -> current + mod.value
                    else -> current
                }
            }
        }
        return out
    }

    /** Effective stat after applying all status multipliers. */
    fun effectiveStat(instance: EchoformInstance, key: StatKey, statuses: List<StatusInstance>): Int {
        val raw = instance.stat(key)
        val mult = statMultipliers(statuses)[key] ?: 1.0
        return (raw * mult).toInt().coerceAtLeast(1)
    }

    /**
     * Tick each status forward by one turn. Returns the result-per-status
     * (in input order). Caller is responsible for applying damage / clearing
     * statuses based on the returned [StatusTickResult]s.
     */
    fun tick(holder: EchoformInstance, statuses: List<StatusInstance>): List<StatusTickResult> {
        val results = ArrayList<StatusTickResult>(statuses.size)
        for (inst in statuses) {
            val slug = inst.status.slug.lowercase()
            // 1) per-turn damage / heal
            when {
                slug == "poisoned" || slug == "poison" -> {
                    val dmg = (holder.maxVigor / 8).coerceAtLeast(1) * inst.stacks
                    results += StatusTickResult.DamageDealt(inst, dmg)
                    decrement(inst, results) ?: continue
                }
                slug == "burned" || slug == "burn" -> {
                    val dmg = (holder.maxVigor / 16).coerceAtLeast(1)
                    results += StatusTickResult.DamageDealt(inst, dmg)
                    decrement(inst, results) ?: continue
                }
                slug == "regenerated" || slug == "regen" || slug == "healing" -> {
                    val heal = (holder.maxVigor / 16).coerceAtLeast(1)
                    results += StatusTickResult.HpRestored(inst, heal)
                    decrement(inst, results) ?: continue
                }
                slug == "frozen" || slug == "freeze" -> {
                    results += StatusTickResult.Skipped(inst, "frozen")
                }
                slug == "sleeping" || slug == "sleep" -> {
                    results += StatusTickResult.Skipped(inst, "asleep")
                }
                slug == "paralyzed" || slug == "paralyze" -> {
                    // 25% proc chance — caller decides; we just signal continuation.
                    results += StatusTickResult.Continue(inst)
                }
                slug == "confused" || slug == "confusion" -> {
                    // 33% self-hit chance — caller decides.
                    results += StatusTickResult.Continue(inst)
                }
                else -> {
                    results += StatusTickResult.Continue(inst)
                }
            }
            // 2) timer
            if (inst.turnsRemaining in 0..0) {
                // Was 0 — now expires
                results[results.lastIndex] = StatusTickResult.Expired
            }
        }
        return results
    }

    /**
     * Stack rule: Tuxemon's `on_positive_status` / `on_negative_status` flags
     * default to "replaced" (only one status of each polarity). For the pilot
     * we keep at most one positive + one negative.
     */
    fun apply(active: List<StatusInstance>, incoming: TuxemonStatus): List<StatusInstance> {
        val polarity = incoming.category.lowercase()
        val filtered = active.filter { it.status.category.lowercase() != polarity }
        return filtered + StatusInstance(incoming)
    }

    /** Decrement turn timer; returns null when the status has expired. */
    private fun decrement(inst: StatusInstance, sink: MutableList<StatusTickResult>): Unit? {
        if (inst.turnsRemaining < 0) return Unit            // permanent
        if (inst.turnsRemaining == 0) {
            // Last index just got DamageDealt/HpRestored — overwrite to Expired
            sink[sink.lastIndex] = StatusTickResult.Expired
            return null
        }
        return Unit
    }
}
