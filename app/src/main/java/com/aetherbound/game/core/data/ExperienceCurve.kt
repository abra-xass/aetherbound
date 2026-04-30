package com.aetherbound.game.core.data

import com.aetherbound.game.core.EchoformInstance
import com.aetherbound.game.core.Technique

/**
 * XP curves and level-up logic, ported from Tuxemon's experience system
 * (`tuxemon/monster/experience.py`). Each species follows one of 4 growth
 * curves. We default to MEDIUM_FAST since Tuxemon's monster.json doesn't
 * always declare it explicitly.
 *
 * Curve formulas at level L:
 *   FAST        : 4 * L³ / 5
 *   MEDIUM_FAST : L³
 *   MEDIUM_SLOW : (6/5) * L³ - 15 * L² + 100 * L - 140
 *   SLOW        : 5 * L³ / 4
 */
enum class ExperienceCurve {
    FAST, MEDIUM_FAST, MEDIUM_SLOW, SLOW;

    /** Total XP required to *be* at [level]. (Level 1 = 0 XP.) */
    fun xpForLevel(level: Int): Long {
        if (level <= 1) return 0
        val l = level.toDouble()
        return when (this) {
            FAST -> (4.0 * l * l * l / 5.0).toLong()
            MEDIUM_FAST -> (l * l * l).toLong()
            MEDIUM_SLOW -> ((6.0 / 5.0) * l * l * l - 15.0 * l * l + 100.0 * l - 140.0).toLong().coerceAtLeast(0)
            SLOW -> (5.0 * l * l * l / 4.0).toLong()
        }
    }

    /** Highest level whose threshold ≤ [totalXp]. */
    fun levelForXp(totalXp: Long): Int {
        if (totalXp <= 0) return 1
        var lvl = 1
        while (lvl < 100 && xpForLevel(lvl + 1) <= totalXp) lvl++
        return lvl
    }
}

/**
 * Tracks accumulated XP and exposes a [LevelUpResult] that callers apply
 * to an [EchoformInstance]. Stateless — every call returns a new result.
 */
object ExperienceEngine {

    /** Source-of-XP context — different multipliers + anti-exploit gates per mode. */
    enum class SourceMode { WILD, TRAINER, MULTIPLAYER }

    /**
     * XP awarded when an Echoform faints. Pokémon Gen-3-style formula plus a
     * **level-differential scaler** that quadratically dampens XP when the
     * winner is significantly higher level than the loser. Self-farming is
     * still possible but auto-limited: a Lv 70 winner against its own Lv 20
     * mon gets only ~8% of the XP it would get against an equal-level
     * opponent — making the grind-loop unattractive without ever banning it.
     *
     *   xp = baseYield * loserLevel * (loserLevel/winnerLevel)^2 * modeMult / 7
     *
     * @param winnerLevel level of the active Echoform that delivered the KO
     * @param loserLevel level of the fainted opponent
     * @param loserBaseStatSum sum of the loser's six base stats (drives baseYield)
     * @param sourceMode WILD → 1.0×, TRAINER → 1.5×, MULTIPLAYER → 1.2×
     */
    fun xpFromVictory(
        winnerLevel: Int,
        loserLevel: Int,
        loserBaseStatSum: Int = 360,
        sourceMode: SourceMode = SourceMode.WILD,
    ): Int {
        val baseYield = (loserBaseStatSum * 0.4).toInt().coerceAtLeast(40)
        val raw = baseYield * loserLevel / 7

        // Quadratic level-differential scaler. Capped at 1.0 (no bonus for
        // beating higher-level mons) and floored at 0.05 (some XP always).
        val ratio = (loserLevel.toDouble() / winnerLevel.coerceAtLeast(1).toDouble())
            .coerceIn(0.05, 1.0)
        val diffScaler = ratio * ratio

        val modeMult = when (sourceMode) {
            SourceMode.WILD -> 1.0
            SourceMode.TRAINER -> 1.5
            SourceMode.MULTIPLAYER -> 1.2
        }

        return (raw * diffScaler * modeMult).toInt().coerceAtLeast(1)
    }

    /** Legacy overload kept for source-compat — delegates with assumed equal levels. */
    @Deprecated(
        message = "Pass winnerLevel + sourceMode for proper level-diff scaling",
        replaceWith = ReplaceWith(
            "xpFromVictory(winnerLevel = loserLevel, loserLevel = loserLevel, " +
                "loserBaseStatSum = loserBaseStatSum, sourceMode = if (isTrainer) " +
                "ExperienceEngine.SourceMode.TRAINER else ExperienceEngine.SourceMode.WILD)"
        ),
    )
    fun xpFromVictory(loserLevel: Int, loserBaseStatSum: Int = 360, isTrainer: Boolean = false): Int =
        xpFromVictory(
            winnerLevel = loserLevel,
            loserLevel = loserLevel,
            loserBaseStatSum = loserBaseStatSum,
            sourceMode = if (isTrainer) SourceMode.TRAINER else SourceMode.WILD,
        )

    data class LevelUpResult(
        val oldLevel: Int,
        val newLevel: Int,
        val totalXp: Long,
        /** Newly-unlocked techniques (sorted by learn level). Empty if no level-up. */
        val newlyLearned: List<Technique> = emptyList(),
    ) {
        val didLevelUp: Boolean get() = newLevel > oldLevel
    }

    /**
     * Award [xpDelta] XP and return the resulting level + newly-learned moves.
     * Caller is responsible for replacing the [EchoformInstance] with a level-
     * incremented copy (and optionally inserting moves into its slot list).
     */
    fun addXp(
        instance: EchoformInstance,
        currentTotalXp: Long,
        xpDelta: Int,
        curve: ExperienceCurve = ExperienceCurve.MEDIUM_FAST,
        movesetByLevel: List<Pair<Int, Technique>> = emptyList(),
    ): LevelUpResult {
        val oldLevel = instance.level
        val total = (currentTotalXp + xpDelta).coerceAtLeast(0)
        val newLevel = curve.levelForXp(total).coerceAtMost(100)
        val newMoves = if (newLevel > oldLevel) {
            movesetByLevel
                .filter { it.first in (oldLevel + 1)..newLevel }
                .sortedBy { it.first }
                .map { it.second }
        } else emptyList()
        return LevelUpResult(
            oldLevel = oldLevel,
            newLevel = newLevel,
            totalXp = total,
            newlyLearned = newMoves,
        )
    }
}
