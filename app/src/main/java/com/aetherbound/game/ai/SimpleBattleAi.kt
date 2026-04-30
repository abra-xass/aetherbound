package com.aetherbound.game.ai

import com.aetherbound.game.core.AspectAffinity
import com.aetherbound.game.core.BattleAction
import com.aetherbound.game.core.BattleState
import kotlin.random.Random

/**
 * Pilot-grade AI: picks move with best expected effectiveness × power, with
 * tiny exploration noise so it doesn't feel deterministic.
 */
object SimpleBattleAi {
    fun pickAction(state: BattleState, rng: Random): BattleAction {
        val opp = state.opponent
        val target = state.player
        val scored = opp.techniques.mapIndexed { i, tech ->
            val eff = AspectAffinity.multiplier(
                tech.aspect, target.species.primaryAspect, target.species.secondaryAspect,
            )
            val noise = rng.nextDouble() * 0.15
            i to (tech.power * eff + noise)
        }
        val best = scored.maxByOrNull { it.second }!!.first
        return BattleAction.UseTechnique(best)
    }
}
