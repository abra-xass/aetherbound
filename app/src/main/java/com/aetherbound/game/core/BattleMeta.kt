package com.aetherbound.game.core

/**
 * Top-level battle action menu — like Pokemon's Fight/Bag/Form/Run,
 * adapted to Aetherbound terminology.
 */
enum class BattleMenu { Fight, Capture, Switch, Bag, Run }

/**
 * Capture roll — Gen-3-style. Bulbapedia-documented math, freely usable.
 * a = ((3*MaxHP - 2*CurHP) * speciesCatchRate * ballMod) / (3*MaxHP)
 * Then 4 shake checks each with probability sqrt(a/255).
 * For the pilot we collapse to a single-roll probability for simplicity.
 *
 * Glass Prism = ballMod 1.0 (basic). Future Prisms can use 1.5, 2.0, 3.5.
 */
object CaptureMechanic {
    fun successChance(target: EchoformInstance, ballMod: Double = 1.0): Double {
        val maxHp = target.maxVigor
        val curHp = target.currentVigor.coerceAtLeast(1)
        val rate = target.species.catchRate.coerceIn(3, 255)
        val a = ((3 * maxHp - 2 * curHp).toDouble() * rate * ballMod) / (3 * maxHp)
        // 4 shake checks of probability sqrt(a/255), so p = (a/255)^2
        val perShake = (a / 255.0).coerceIn(0.0, 1.0)
        return kotlin.math.sqrt(perShake).pow(4).coerceIn(0.02, 0.98)
    }
    private fun Double.pow(n: Int): Double { var r = 1.0; repeat(n) { r *= this }; return r }
}

/**
 * Run roll: like Pokemon's escape formula. Succeeds if random < (player.tempo * 32) / opp.tempo + 30 escape attempts so far.
 * Pilot keeps it simple: succeeds 80% if player tempo >= opp tempo, else 50%.
 */
object RunMechanic {
    fun successChance(player: EchoformInstance, opponent: EchoformInstance): Double {
        return if (player.stat(StatKey.TEMPO) >= opponent.stat(StatKey.TEMPO)) 0.80 else 0.50
    }
}
