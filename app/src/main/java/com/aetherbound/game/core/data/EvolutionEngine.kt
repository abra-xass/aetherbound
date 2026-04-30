package com.aetherbound.game.core.data

import android.content.Context
import com.aetherbound.game.core.EchoformInstance
import com.aetherbound.game.core.StatFormula
import com.aetherbound.game.core.StatKey

/**
 * Evolution checker — evaluates whether an [EchoformInstance] qualifies to
 * evolve into one of its declared `evolves_into` slugs and produces a new
 * instance after evolution.
 *
 * Tuxemon's `evolutions[]` JSON entries carry per-evolution conditions
 * (level threshold, item, location, etc). Our [TuxemonMonster] stores
 * only the flat slug list extracted from `history`; for full conditions
 * the caller must consult the raw JSON. This engine ships with the
 * common cases:
 *
 *   - LEVEL_UP   → reach target level
 *   - STONE      → use evolution stone item
 *   - TRADE      → trade with another player (Aetherbound will use Matrix)
 *   - FRIENDSHIP → high happiness
 */
object EvolutionEngine {

    enum class Trigger { LEVEL_UP, STONE, TRADE, FRIENDSHIP, MOVE_LEARNED }

    data class Condition(
        val targetSlug: String,
        val trigger: Trigger,
        val levelThreshold: Int? = null,
        val itemSlug: String? = null,
    )

    /**
     * Evaluate whether [instance] is ready to evolve given [trigger] context.
     *
     * @param itemSlug item being used (only relevant for STONE trigger)
     * @param happiness 0..255 friendship counter (only for FRIENDSHIP)
     * @return target species slug if eligible, else null
     */
    fun checkReady(
        ctx: Context,
        instance: EchoformInstance,
        trigger: Trigger,
        itemSlug: String? = null,
        happiness: Int = 0,
    ): String? {
        val mon = TuxemonDex.bySlug(ctx, instance.species.id) ?: return null
        if (mon.evolvesInto.isEmpty()) return null

        // Heuristic: pick the first declared evolution and apply trigger-specific
        // gating. Tuxemon's per-evolution conditions live in the raw JSON which
        // we don't currently retain — extend [TuxemonMonster] if you need richer
        // gating (e.g. multiple evolution paths).
        val target = mon.evolvesInto.first()

        return when (trigger) {
            Trigger.LEVEL_UP -> if (instance.level >= guessEvolutionLevel(mon.stage)) target else null
            Trigger.STONE -> if (itemSlug != null && itemSlug.contains("stone")) target else null
            Trigger.TRADE -> target  // any trade triggers evolution if the species has one
            Trigger.FRIENDSHIP -> if (happiness >= 220) target else null
            Trigger.MOVE_LEARNED -> null  // requires per-mon move-evolution table
        }
    }

    /**
     * Default level threshold by Tuxemon stage. "basic" → 16, "stage1" → 32.
     * Override per species when richer evolution data lands.
     */
    private fun guessEvolutionLevel(stage: String): Int = when (stage.lowercase()) {
        "basic" -> 16
        "stage1" -> 32
        "stage2" -> 100  // already final
        "standalone" -> 100
        else -> 16
    }

    /**
     * Transform [instance] into its evolved form. Recomputes [EchoformSpecies]
     * via [TuxemonEchoformDex], scales current HP proportionally, retains
     * level/training/potential/temperament/techniques.
     */
    fun evolve(ctx: Context, instance: EchoformInstance, targetSlug: String): EchoformInstance? {
        val newSpecies = TuxemonEchoformDex.bySlug(ctx, targetSlug) ?: return null
        val oldMaxHp = instance.maxVigor.coerceAtLeast(1)
        val newInstance = instance.copy(species = newSpecies)
        val newMaxHp = StatFormula.calc(
            stat = StatKey.VIGOR,
            base = newSpecies.baseStats,
            level = instance.level,
            potential = instance.potential,
            training = instance.training,
            temperament = instance.temperament,
        )
        val hpRatio = instance.currentVigor.toDouble() / oldMaxHp
        val newCurrent = (newMaxHp * hpRatio).toInt().coerceAtLeast(1)
        return newInstance.copy(currentVigor = newCurrent)
    }
}
