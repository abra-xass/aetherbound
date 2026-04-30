package com.aetherbound.game.core.data

import android.content.Context
import com.aetherbound.game.core.EchoformInstance
import com.aetherbound.game.core.Potential
import com.aetherbound.game.core.StatFormula
import com.aetherbound.game.core.StatKey
import com.aetherbound.game.core.Technique
import com.aetherbound.game.core.TrainingPoints

/**
 * Factory that builds battle-ready [EchoformInstance]s straight from
 * Tuxemon JSON content. Replaces the hand-authored
 * [com.aetherbound.game.content.PilotEchoforms] starters when callers want
 * a randomised / data-driven battle.
 *
 * Example:
 *   val (player, enemy) = TuxemonBattleSetup.makeBattle(
 *       ctx = ctx,
 *       playerSlug = "agnidon",
 *       enemySlug = "rockitten",
 *       playerLevel = 18,
 *       enemyLevel = 16,
 *   )
 *   battleScene.start(player, enemy)
 */
object TuxemonBattleSetup {

    /** Default "competitive" IVs/EVs spread for a battle-ready Echoform. */
    private val MAX_POTENTIAL = Potential(31, 31, 31, 31, 31, 31)
    private val PHYS_TRAINING = TrainingPoints(vigor = 252, force = 252, tempo = 6)
    private val SPEC_TRAINING = TrainingPoints(vigor = 252, focus = 252, tempo = 6)

    /**
     * Build one [EchoformInstance] from a Tuxemon slug.
     *
     * @param slug Tuxemon monster slug (e.g. "agnidon").
     * @param level 1..100. Stats are computed via Aetherbound's Gen-3 formula.
     * @param trainingBias "physical" picks Force EVs, anything else picks Focus.
     * @return null if the slug is unknown to [TuxemonEchoformDex].
     */
    fun build(
        ctx: Context,
        slug: String,
        level: Int,
        trainingBias: String = "auto",
    ): EchoformInstance? {
        val species = TuxemonEchoformDex.bySlug(ctx, slug) ?: return null
        val techs = TuxemonEchoformDex.activeMovesetAt(ctx, slug, level)
            .ifEmpty { fallbackTechniques() }
        val training = when (trainingBias) {
            "physical" -> PHYS_TRAINING
            "special" -> SPEC_TRAINING
            else -> if (species.baseStats.force >= species.baseStats.focus) PHYS_TRAINING else SPEC_TRAINING
        }
        val maxHp = StatFormula.calc(
            stat = StatKey.VIGOR,
            base = species.baseStats,
            level = level,
            potential = MAX_POTENTIAL,
            training = training,
        )
        return EchoformInstance(
            species = species,
            level = level,
            techniques = techs.take(4),
            potential = MAX_POTENTIAL,
            training = training,
            currentVigor = maxHp,
        )
    }

    /**
     * Build a (player, enemy) pair. Useful for piping straight into a battle
     * scene that takes two [EchoformInstance]s.
     */
    fun makeBattle(
        ctx: Context,
        playerSlug: String,
        enemySlug: String,
        playerLevel: Int,
        enemyLevel: Int,
    ): Pair<EchoformInstance, EchoformInstance>? {
        val player = build(ctx, playerSlug, playerLevel) ?: return null
        val enemy = build(ctx, enemySlug, enemyLevel) ?: return null
        return player to enemy
    }

    /** Fallback move when the species has no learnt moves at the chosen level. */
    private fun fallbackTechniques(): List<Technique> = listOf(
        Technique(
            id = "struggle",
            name = "Struggle",
            aspect = com.aetherbound.game.core.Aspect.NORMAL,
            category = com.aetherbound.game.core.TechniqueCategory.STRIKE,
            power = 30,
            accuracy = 100,
        )
    )
}
