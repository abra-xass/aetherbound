package com.aetherbound.game.core

enum class Side { PLAYER, OPPONENT }

sealed interface BattleAction {
    data class UseTechnique(val techniqueIndex: Int) : BattleAction
    data object Wait : BattleAction
}

data class BattleState(
    val player: EchoformInstance,
    val opponent: EchoformInstance,
    val turn: Int = 1,
    val rngSeed: Long,
    val log: List<BattleEvent> = emptyList(),
    /** Status conditions on the player's active Echoform. Mutated by the resolver. */
    val playerStatuses: List<com.aetherbound.game.core.data.StatusInstance> = emptyList(),
    val opponentStatuses: List<com.aetherbound.game.core.data.StatusInstance> = emptyList(),
) {
    val isOver: Boolean get() = player.isFainted || opponent.isFainted
    val winner: Side? get() = when {
        opponent.isFainted && !player.isFainted -> Side.PLAYER
        player.isFainted && !opponent.isFainted -> Side.OPPONENT
        else -> null
    }
}

sealed interface BattleEvent {
    val turn: Int
    data class TurnStart(override val turn: Int) : BattleEvent
    data class TechniqueDeclared(
        override val turn: Int,
        val side: Side,
        val techniqueId: String,
        val techniqueName: String,
    ) : BattleEvent
    data class TechniqueResolved(
        override val turn: Int,
        val side: Side,
        val techniqueId: String,
        val damage: Int,
        val effectiveness: Double,
        val crit: Boolean,
        val missed: Boolean,
        val stab: Boolean,
    ) : BattleEvent
    data class Faint(override val turn: Int, val side: Side) : BattleEvent
    data class MatchEnd(override val turn: Int, val winner: Side) : BattleEvent
    /** A new status condition was applied. */
    data class StatusApplied(
        override val turn: Int,
        val side: Side,
        val statusSlug: String,
    ) : BattleEvent
    /** Per-turn damage/heal from an active status (poison, burn, regen). */
    data class StatusTickDamage(
        override val turn: Int,
        val side: Side,
        val statusSlug: String,
        val damage: Int,
    ) : BattleEvent
    /** Holder lost a turn to sleep/freeze/paralyze proc. */
    data class StatusBlockedTurn(
        override val turn: Int,
        val side: Side,
        val statusSlug: String,
        val reason: String,
    ) : BattleEvent
    /** Status expired this turn. */
    data class StatusExpired(
        override val turn: Int,
        val side: Side,
        val statusSlug: String,
    ) : BattleEvent
}
