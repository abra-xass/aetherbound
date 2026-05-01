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

    enum class Trigger {
        LEVEL_UP, STONE, TRADE, FRIENDSHIP, MOVE_LEARNED,
        // Aetherbound-only triggers driven by real-clock + DailyPack data.
        MOON_FULL, MOON_NEW, WEATHER_STORM, WEATHER_RAIN,
    }

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
            // Aetherbound moon/weather triggers — handled via the new
            // chain-based path in [checkChainTrigger]. Legacy Tuxemon
            // path doesn't know about them, so they always fail here.
            Trigger.MOON_FULL, Trigger.MOON_NEW,
            Trigger.WEATHER_STORM, Trigger.WEATHER_RAIN -> null
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

    // ── Aetherbound 152-chain evaluation ────────────────────────────
    //
    // Replaces the heuristic Tuxemon-stage logic for the 1000-monster
    // matrix. Reads chain definitions from EvolutionChainRegistry and
    // applies the right trigger:
    //
    //   level         → P(level) ramp, must roll on level-up
    //   stone         → instant if matching stone item is consumed
    //   moon_full     → instant on full-moon nights (real-clock)
    //   moon_new      → instant on new-moon nights
    //   weather_storm → instant when DailyPack reports STORM
    //   weather_rain  → instant when DailyPack reports RAIN
    //
    // All checks pass instance.species.id (the Echoform's name slug)
    // through to the chain registry.

    /**
     * Outcome of a chain-evaluation step. The caller (BattleScene /
     * level-up handler / item-use handler) acts on `nextStageName`.
     */
    data class ChainResult(
        val triggered: Boolean,
        val nextStageName: String?,
        val reason: String,
    )

    /**
     * Decide whether [instance] should evolve right now given the trigger
     * context. Pass only the inputs that matter for the trigger type
     * (e.g. for STONE pass [stoneItemSlug]; for level-up pass nothing
     * beyond the instance, which already carries its current level).
     *
     * Returns [ChainResult.triggered] = true and [nextStageName] when
     * the evolution fires. Caller then applies [evolveTo] to swap species.
     */
    fun checkChainTrigger(
        ctx: Context,
        instance: EchoformInstance,
        trigger: Trigger,
        stoneItemSlug: String? = null,
        currentWeather: com.aetherbound.game.core.Weather =
            com.aetherbound.game.core.Weather.CLEAR,
        rng: kotlin.random.Random = kotlin.random.Random.Default,
    ): ChainResult {
        val name = instance.species.id
        val pair = EvolutionChainRegistry.forMember(ctx, name)
            ?: return ChainResult(false, null, "$name is standalone (not in any chain)")
        val (chain, idx) = pair
        if (idx + 1 >= chain.members.size) {
            return ChainResult(false, null, "$name is already at final stage")
        }
        val nextName = chain.members[idx + 1]

        // Match the instance's chain trigger to the requested trigger.
        // Mismatched triggers are silently rejected (returns false, no error).
        val match = when (chain.triggerType) {
            "level" -> trigger == Trigger.LEVEL_UP
            "stone" -> trigger == Trigger.STONE && stoneItemSlug == chain.stoneItem
            "moon_full" -> trigger == Trigger.MOON_FULL && com.aetherbound.game.core.MoonPhase.isFullMoonNight()
            "moon_new" -> trigger == Trigger.MOON_NEW && com.aetherbound.game.core.MoonPhase.isNewMoonNight()
            "weather_storm" -> trigger == Trigger.WEATHER_STORM &&
                currentWeather == com.aetherbound.game.core.Weather.STORM
            "weather_rain" -> trigger == Trigger.WEATHER_RAIN &&
                currentWeather == com.aetherbound.game.core.Weather.RAIN
            else -> false
        }
        if (!match) {
            return ChainResult(false, null, "trigger mismatch: chain wants ${chain.triggerType}")
        }

        // For LEVEL trigger, roll probability. Other triggers fire instantly.
        if (chain.triggerType == "level") {
            val p = EvolutionChainRegistry.levelEvolutionProbability(ctx, name, instance.level)
            if (p <= 0f) return ChainResult(false, null, "level too low for $name")
            if (rng.nextFloat() > p) return ChainResult(false, null, "rolled ${rng.nextFloat()} > p=$p")
        }
        return ChainResult(true, nextName, "evolution fired: $name → $nextName via ${chain.triggerType}")
    }

    /** Force-evolve to the next chain stage. Wraps [evolve] with name lookup. */
    fun evolveToNextStage(ctx: Context, instance: EchoformInstance): EchoformInstance? {
        val nextName = EvolutionChainRegistry.nextStage(ctx, instance.species.id) ?: return null
        return evolve(ctx, instance, nextName)
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
