package com.aetherbound.game.content

import com.aetherbound.game.core.EchoformDex
import com.aetherbound.game.core.EchoformInstance
import com.aetherbound.game.core.EchoformSpecies
import com.aetherbound.game.core.Potential
import com.aetherbound.game.core.StatFormula
import com.aetherbound.game.core.StatKey
import com.aetherbound.game.core.Temperament
import com.aetherbound.game.core.TrainingPoints
import com.aetherbound.game.render.battle.EchoformVisualId

/**
 * Pilot battle roster. The full dex of 1000 species lives in [EchoformDex];
 * this file only pins the two starters used in the Pilot battle.
 */
object PilotEchoforms {

    val Vulkid: EchoformSpecies = EchoformDex.byId("E001")!!
    val Reeva: EchoformSpecies = EchoformDex.byId("E002")!!

    /**
     * Vector-fallback map. Only species that have a hand-authored vector
     * sprite get an entry here. Species without an entry rely on PNG (which
     * exists for all 1000 imported species) — the runtime checks PNG first.
     */
    val visualMap: Map<String, EchoformVisualId> = mapOf(
        Vulkid.id to EchoformVisualId.Vulkid,
        Reeva.id to EchoformVisualId.Reeva,
    )

    /** Max IVs + 252 EVs split between Vigor and main offensive stat. */
    private val starterPotential = Potential(31, 31, 31, 31, 31, 31)
    private val playerTraining = TrainingPoints(vigor = 252, force = 252)
    private val opponentTraining = TrainingPoints(vigor = 252, focus = 252)

    fun playerStarter(): EchoformInstance {
        val maxHp = StatFormula.calc(StatKey.VIGOR, Vulkid.baseStats, 18, starterPotential, playerTraining)
        return EchoformInstance(
            species = Vulkid,
            level = 18,
            techniques = listOf(
                PilotTechniques.EmberSnap,
                PilotTechniques.EmberLance,
                PilotTechniques.SparkHit,
            ),
            potential = starterPotential,
            training = playerTraining,
            currentVigor = maxHp,
        )
    }

    fun opponent(): EchoformInstance {
        val maxHp = StatFormula.calc(StatKey.VIGOR, Reeva.baseStats, 18, starterPotential, opponentTraining)
        return EchoformInstance(
            species = Reeva,
            level = 18,
            techniques = listOf(
                PilotTechniques.TideSurge,
                PilotTechniques.FrostPulse,
                PilotTechniques.VerdanceBind,
            ),
            potential = starterPotential,
            training = opponentTraining,
            currentVigor = maxHp,
        )
    }

    /**
     * Build a wild encounter instance for any of the 1000 species. Moves are
     * picked from the pilot pool — one matching the species' primary aspect
     * if available, plus 1–2 neutrals. Wilds are L14, slightly weaker than
     * the L18 player starter.
     */
    fun wildInstance(species: EchoformSpecies, level: Int = 14): EchoformInstance {
        val maxHp = StatFormula.calc(StatKey.VIGOR, species.baseStats, level)
        return EchoformInstance(
            species = species,
            level = level,
            techniques = pickWildMoves(species),
            currentVigor = maxHp,
        )
    }

    private fun pickWildMoves(species: EchoformSpecies): List<com.aetherbound.game.core.Technique> {
        val pool = listOf(
            PilotTechniques.EmberSnap, PilotTechniques.EmberLance, PilotTechniques.SparkHit,
            PilotTechniques.TideSurge, PilotTechniques.FrostPulse, PilotTechniques.VerdanceBind,
        )
        val matchingPrimary = pool.filter { it.aspect == species.primaryAspect }
        val matchingSecondary = species.secondaryAspect?.let { sec -> pool.filter { it.aspect == sec } } ?: emptyList()
        val firstMove = matchingPrimary.firstOrNull() ?: matchingSecondary.firstOrNull() ?: PilotTechniques.SparkHit
        val rest = pool.filter { it != firstMove }.shuffled().take(2)
        return listOf(firstMove) + rest
    }
}
