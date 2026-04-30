package com.thot.fusion.prep.engine

object FusionCompatibility {
    fun score(
        coreAspects: List<Aspect>,
        mantleAspects: List<Aspect>,
        coreLevel: Int,
        mantleLevel: Int,
        coreBond: Int,
        mantleBond: Int,
        sameBodyFamily: Boolean,
    ): Int {
        val sharedAspectBonus = if (coreAspects.any { it in mantleAspects }) 20 else 0
        val nullPenalty = if (Aspect.NULL in coreAspects || Aspect.NULL in mantleAspects) -25 else 0
        val levelGapPenalty = -kotlin.math.min(25, kotlin.math.abs(coreLevel - mantleLevel))
        val bondBonus = ((coreBond.coerceIn(0, 100) + mantleBond.coerceIn(0, 100)) / 10)
        val bodyBonus = if (sameBodyFamily) 10 else 0
        return (55 + sharedAspectBonus + nullPenalty + levelGapPenalty + bondBonus + bodyBonus).coerceIn(0, 100)
    }

    fun stabilityTier(score: Int): String = when (score) {
        in 90..100 -> "perfect"
        in 70..89 -> "stable"
        in 50..69 -> "strained"
        in 30..49 -> "unstable"
        else -> "unsafe"
    }
}

object FusionStats {
    fun blend(core: BaseStats, mantle: BaseStats, compatibilityScore: Int): BaseStats {
        val compatibilityBonus = when (compatibilityScore) {
            in 90..100 -> 8
            in 70..89 -> 4
            in 50..69 -> 0
            in 30..49 -> -6
            else -> -12
        }

        fun mix(stat: StatKey): Int {
            return kotlin.math.round(core.get(stat) * 0.65 + mantle.get(stat) * 0.35 + compatibilityBonus).toInt().coerceAtLeast(1)
        }

        return BaseStats(
            vigor = mix(StatKey.VIGOR),
            force = mix(StatKey.FORCE),
            focus = mix(StatKey.FOCUS),
            guard = mix(StatKey.GUARD),
            ward = mix(StatKey.WARD),
            tempo = mix(StatKey.TEMPO),
        )
    }
}
