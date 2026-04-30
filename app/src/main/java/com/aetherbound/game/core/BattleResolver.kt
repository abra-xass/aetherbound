package com.aetherbound.game.core

import com.aetherbound.game.core.data.MoveStatusEffects
import com.aetherbound.game.core.data.StatusEngine
import com.aetherbound.game.core.data.StatusInstance
import com.aetherbound.game.core.data.StatusTickResult
import com.aetherbound.game.core.data.TuxemonStatus
import kotlin.random.Random

/**
 * Pure deterministic turn resolver. Same inputs + seed = same outputs on every device.
 * Required so future Matrix PvP can replay event logs without server.
 *
 * Turn order:
 *   1. **Status tick (start of turn)** — poison/burn damage, regen heal,
 *      sleep/freeze/confusion proc rolls. Sets `skip<side>` flag if the
 *      status blocks the turn.
 *   2. **Action resolution** — pick first-mover by priority/tempo, then
 *      run each side's chosen technique.
 *   3. **Hit consequences** — damage applied, status conditions rolled
 *      via [MoveStatusEffects.rollStatus] for each landed move.
 */
object BattleResolver {

    fun resolveTurn(
        state: BattleState,
        playerAction: BattleAction,
        opponentAction: BattleAction,
    ): BattleState {
        val rng = Random(state.rngSeed xor (state.turn.toLong() * 0x9E3779B97F4A7C15uL.toLong()))
        val log = mutableListOf<BattleEvent>()
        log += BattleEvent.TurnStart(state.turn)

        var player = state.player
        var opponent = state.opponent
        var playerStatuses = state.playerStatuses
        var opponentStatuses = state.opponentStatuses

        // ── Phase 1: status tick (start of turn) ────────────────────
        val (newPlayer, newPlayerStatuses, skipPlayer) = tickStatuses(player, playerStatuses, Side.PLAYER, state.turn, rng, log)
        player = newPlayer
        playerStatuses = newPlayerStatuses

        val (newOpponent, newOpponentStatuses, skipOpponent) = tickStatuses(opponent, opponentStatuses, Side.OPPONENT, state.turn, rng, log)
        opponent = newOpponent
        opponentStatuses = newOpponentStatuses

        // ── Phase 2: technique resolution ───────────────────────────
        val playerTech = (playerAction as? BattleAction.UseTechnique)?.let { player.techniques.getOrNull(it.techniqueIndex) }
        val opponentTech = (opponentAction as? BattleAction.UseTechnique)?.let { opponent.techniques.getOrNull(it.techniqueIndex) }

        val playerPriority = playerTech?.priority ?: 0
        val opponentPriority = opponentTech?.priority ?: 0
        // Effective tempo = stat × multiplier from active statuses.
        val playerTempo = StatusEngine.effectiveStat(player, StatKey.TEMPO, playerStatuses)
        val opponentTempo = StatusEngine.effectiveStat(opponent, StatKey.TEMPO, opponentStatuses)

        val playerFirst = when {
            playerPriority != opponentPriority -> playerPriority > opponentPriority
            playerTempo != opponentTempo -> playerTempo > opponentTempo
            else -> rng.nextBoolean()
        }

        val acts: List<Triple<Side, Technique?, Boolean>> = if (playerFirst) {
            listOf(
                Triple(Side.PLAYER, playerTech, skipPlayer),
                Triple(Side.OPPONENT, opponentTech, skipOpponent),
            )
        } else {
            listOf(
                Triple(Side.OPPONENT, opponentTech, skipOpponent),
                Triple(Side.PLAYER, playerTech, skipPlayer),
            )
        }

        for ((side, tech, skip) in acts) {
            if (player.isFainted || opponent.isFainted) break
            if (skip) continue
            if (tech == null) continue
            log += BattleEvent.TechniqueDeclared(state.turn, side, tech.id, tech.name)
            val (att, def) = if (side == Side.PLAYER) player to opponent else opponent to player
            val result = DamageFormula.resolve(att, def, tech, rng)
            log += BattleEvent.TechniqueResolved(
                turn = state.turn,
                side = side,
                techniqueId = tech.id,
                damage = result.damage,
                effectiveness = result.effectiveness,
                crit = result.crit,
                missed = result.missed,
                stab = result.stab,
            )
            if (!result.missed) {
                if (side == Side.PLAYER) {
                    opponent = opponent.copy(currentVigor = (opponent.currentVigor - result.damage).coerceAtLeast(0))
                } else {
                    player = player.copy(currentVigor = (player.currentVigor - result.damage).coerceAtLeast(0))
                }
                // Roll status application via the move's mapping table.
                val newStatusSlug = MoveStatusEffects.rollStatus(tech.id, rng)
                if (newStatusSlug != null) {
                    val statusObj = stubStatus(newStatusSlug)
                    if (side == Side.PLAYER) {
                        if (opponentStatuses.none { it.status.slug == newStatusSlug }) {
                            opponentStatuses = StatusEngine.apply(opponentStatuses, statusObj)
                            log += BattleEvent.StatusApplied(state.turn, Side.OPPONENT, newStatusSlug)
                        }
                    } else {
                        if (playerStatuses.none { it.status.slug == newStatusSlug }) {
                            playerStatuses = StatusEngine.apply(playerStatuses, statusObj)
                            log += BattleEvent.StatusApplied(state.turn, Side.PLAYER, newStatusSlug)
                        }
                    }
                }
            }
            if (player.isFainted) log += BattleEvent.Faint(state.turn, Side.PLAYER)
            if (opponent.isFainted) log += BattleEvent.Faint(state.turn, Side.OPPONENT)
        }

        val nextState = state.copy(
            player = player,
            opponent = opponent,
            turn = state.turn + 1,
            log = state.log + log,
            playerStatuses = playerStatuses,
            opponentStatuses = opponentStatuses,
        )
        return if (nextState.isOver) {
            nextState.copy(log = nextState.log + BattleEvent.MatchEnd(state.turn, nextState.winner!!))
        } else nextState
    }

    // ── Helpers ──────────────────────────────────────────────────

    /**
     * Apply status ticks at the start of [side]'s turn. Returns:
     *   - updated [EchoformInstance] (HP after poison/burn/regen)
     *   - updated active-status list (expired removed)
     *   - skipTurn flag (true when sleep/freeze blocks the action)
     */
    private fun tickStatuses(
        instance: EchoformInstance,
        statuses: List<StatusInstance>,
        side: Side,
        turn: Int,
        rng: Random,
        log: MutableList<BattleEvent>,
    ): Triple<EchoformInstance, List<StatusInstance>, Boolean> {
        if (statuses.isEmpty()) return Triple(instance, statuses, false)

        var hp = instance.currentVigor
        val survivors = mutableListOf<StatusInstance>()
        var skip = false
        val results = StatusEngine.tick(instance, statuses)

        for ((i, result) in results.withIndex()) {
            val statusInst = statuses[i]
            when (result) {
                is StatusTickResult.DamageDealt -> {
                    hp = (hp - result.damage).coerceAtLeast(0)
                    log += BattleEvent.StatusTickDamage(turn, side, statusInst.status.slug, result.damage)
                    survivors += statusInst
                }
                is StatusTickResult.HpRestored -> {
                    hp = (hp + result.amount).coerceAtMost(instance.maxVigor)
                    log += BattleEvent.StatusTickDamage(turn, side, statusInst.status.slug, -result.amount)
                    survivors += statusInst
                }
                is StatusTickResult.Skipped -> {
                    skip = true
                    log += BattleEvent.StatusBlockedTurn(turn, side, statusInst.status.slug, result.reason)
                    survivors += statusInst
                }
                is StatusTickResult.Continue -> {
                    val slug = statusInst.status.slug.lowercase()
                    when (slug) {
                        "paralyzed", "paralyze" -> if (rng.nextDouble() < 0.25) {
                            skip = true
                            log += BattleEvent.StatusBlockedTurn(turn, side, slug, "paralysed")
                        }
                        "confused", "confusion" -> if (rng.nextDouble() < 0.33) {
                            skip = true
                            log += BattleEvent.StatusBlockedTurn(turn, side, slug, "confusion-self-hit")
                        }
                    }
                    survivors += statusInst
                }
                is StatusTickResult.Expired -> {
                    log += BattleEvent.StatusExpired(turn, side, statusInst.status.slug)
                    // dropped from survivors list
                }
            }
        }

        return Triple(
            instance.copy(currentVigor = hp),
            survivors,
            skip,
        )
    }

    /**
     * Stub a [TuxemonStatus] for a status slug. Real status records carry
     * stat_modifiers from JSON; we rebuild minimal ones here so the
     * resolver doesn't require Context.
     */
    private fun stubStatus(slug: String): TuxemonStatus = TuxemonStatus(
        slug = slug,
        condId = 0,
        sort = "meta",
        category = if (slug in NEGATIVE) "negative" else if (slug in POSITIVE) "positive" else "neutral",
        icon = null,
        statModifiers = STAT_MODS[slug] ?: emptyMap(),
    )

    private val NEGATIVE = setOf(
        "poisoned", "burned", "frozen", "sleeping", "paralyzed", "confused", "blinded",
    )
    private val POSITIVE = setOf("regenerated", "healing", "focused", "hardshell")

    /** Hand-curated stat modifiers for the most common statuses. */
    private val STAT_MODS: Map<String, Map<String, com.aetherbound.game.core.data.StatModifier>> = mapOf(
        "paralyzed" to mapOf(
            "speed" to com.aetherbound.game.core.data.StatModifier("speed", 0.5, "*"),
        ),
        "burned" to mapOf(
            "melee" to com.aetherbound.game.core.data.StatModifier("melee", 0.5, "*"),
        ),
        "blinded" to mapOf(
            "speed" to com.aetherbound.game.core.data.StatModifier("speed", 0.5, "*"),
            "dodge" to com.aetherbound.game.core.data.StatModifier("dodge", 0.5, "*"),
        ),
    )
}
