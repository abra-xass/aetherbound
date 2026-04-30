package com.aetherbound.game

import com.aetherbound.game.content.AtlasPilotMap
import com.aetherbound.game.content.PilotEchoforms
import com.aetherbound.game.content.PilotTechniques
import com.aetherbound.game.core.AspectAffinity
import com.aetherbound.game.core.BattleAction
import com.aetherbound.game.core.BattleEvent
import com.aetherbound.game.core.BattleResolver
import com.aetherbound.game.core.BattleState
import com.aetherbound.game.core.CaptureMechanic
import com.aetherbound.game.core.DamageFormula
import com.aetherbound.game.core.EchoformDex
import com.aetherbound.game.core.Aspect
import com.aetherbound.game.core.Side
import com.aetherbound.game.core.StatFormula
import com.aetherbound.game.core.StatKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import kotlin.random.Random

/**
 * Pure-Kotlin tests for the game preview's deterministic core. No Android
 * runtime, no Compose — runs in `./gradlew :app:testDebugUnitTest` in seconds.
 */
class GamePreviewLogicTest {

    // ─────────────────────────────────────────────────────────────────
    // BattleResolver determinism (required for Matrix PvP replay)
    // ─────────────────────────────────────────────────────────────────

    @Test fun `resolveTurn is deterministic for same seed and actions`() {
        val state = BattleState(
            player = PilotEchoforms.playerStarter(),
            opponent = PilotEchoforms.opponent(),
            rngSeed = 0x1234567890ABCDEFL,
        )
        val a = BattleAction.UseTechnique(0)
        val b = BattleAction.UseTechnique(0)
        val r1 = BattleResolver.resolveTurn(state, a, b)
        val r2 = BattleResolver.resolveTurn(state, a, b)
        assertEquals(r1.player.currentVigor, r2.player.currentVigor)
        assertEquals(r1.opponent.currentVigor, r2.opponent.currentVigor)
        assertEquals(r1.turn, r2.turn)
        assertEquals(r1.log.size, r2.log.size)
    }

    @Test fun `resolveTurn produces different outcomes for different seeds`() {
        val a = BattleAction.UseTechnique(0)
        val b = BattleAction.UseTechnique(0)
        val s1 = BattleState(
            player = PilotEchoforms.playerStarter(),
            opponent = PilotEchoforms.opponent(),
            rngSeed = 1L,
        )
        val s2 = s1.copy(rngSeed = 999_999L)
        val r1 = BattleResolver.resolveTurn(s1, a, b)
        val r2 = BattleResolver.resolveTurn(s2, a, b)
        // At least one stat should differ across many distinct seeds
        val differs = r1.player.currentVigor != r2.player.currentVigor ||
                r1.opponent.currentVigor != r2.opponent.currentVigor
        assertTrue("RNG should diverge on different seeds", differs)
    }

    @Test fun `priority technique resolves first regardless of tempo`() {
        // Spark Hit (priority 1) should beat Tide Surge (priority 0)
        val state = BattleState(
            player = PilotEchoforms.playerStarter(),
            opponent = PilotEchoforms.opponent(),
            rngSeed = 0L,
        )
        val playerSparkIdx = state.player.techniques.indexOf(PilotTechniques.SparkHit)
        val oppTideIdx = state.opponent.techniques.indexOf(PilotTechniques.TideSurge)
        val result = BattleResolver.resolveTurn(
            state,
            BattleAction.UseTechnique(playerSparkIdx),
            BattleAction.UseTechnique(oppTideIdx),
        )
        // First TechniqueDeclared event must be PLAYER (priority 1 > 0)
        val first = result.log.firstOrNull { it is BattleEvent.TechniqueDeclared } as? BattleEvent.TechniqueDeclared
        assertNotNull(first)
        assertEquals(Side.PLAYER, first!!.side)
    }

    // ─────────────────────────────────────────────────────────────────
    // Damage formula — Bulbapedia-documented Gen-3 math
    // ─────────────────────────────────────────────────────────────────

    @Test fun `damage is at least 1 even on small inputs`() {
        val tech = PilotTechniques.VerdanceBind  // power 35, low
        val player = PilotEchoforms.playerStarter()
        val opp = PilotEchoforms.opponent()
        val rng = Random(42)
        val res = DamageFormula.resolve(player, opp, tech, rng)
        if (!res.missed) {
            assertTrue("damage must be >=1 when not missed", res.damage >= 1)
        }
    }

    @Test fun `super-effective doubles the damage tier`() {
        // Lightning vs Water should be 2x effective per Tuxemon's affinity chart
        val mult = AspectAffinity.multiplier(Aspect.LIGHTNING, Aspect.WATER, null)
        assertEquals(2.0, mult, 0.0001)
    }

    @Test fun `not very effective halves the damage tier`() {
        // Fire vs Water should be 0.5x
        val mult = AspectAffinity.multiplier(Aspect.FIRE, Aspect.WATER, null)
        assertEquals(0.5, mult, 0.0001)
    }

    // ─────────────────────────────────────────────────────────────────
    // Stat formula — Gen-3 with full IV/EV
    // ─────────────────────────────────────────────────────────────────

    @Test fun `vulkid HP at level 18 with max IV plus 252 EV vigor passes one-shot threshold`() {
        // Bulbapedia formula: floor((2*65 + 31 + 63) * 18 / 100) + 18 + 10 = 68
        // Pre-rebalance was 51 (one-shot bug). 68 survives a 38-damage hit.
        val v = PilotEchoforms.playerStarter()
        assertEquals(68, v.maxVigor)
    }

    @Test fun `stat formula matches Bulbapedia reference for known input`() {
        // Reference: base=100, IV=31, EV=252, level=100, neutral nature
        // expected non-HP stat: floor(((2*100 + 31 + 63) * 100 / 100) + 5) = 299
        val base = com.aetherbound.game.core.BaseStats(50, 100, 50, 50, 50, 50)
        val pot = com.aetherbound.game.core.Potential(force = 31)
        val tp = com.aetherbound.game.core.TrainingPoints(force = 252)
        val v = StatFormula.calc(StatKey.FORCE, base, 100, pot, tp)
        assertEquals(299, v)
    }

    // ─────────────────────────────────────────────────────────────────
    // EchoformDex — 1000-species deterministic generator
    // ─────────────────────────────────────────────────────────────────

    @Test fun `dex has 300 species`() {
        assertEquals(300, EchoformDex.all().size)
    }

    @Test fun `byId is deterministic`() {
        val a = EchoformDex.byId("E150")
        val b = EchoformDex.byId("E150")
        assertNotNull(a)
        assertNotNull(b)
        assertEquals(a!!.id, b!!.id)
        assertEquals(a.name, b.name)
        assertEquals(a.primaryAspect, b.primaryAspect)
        assertEquals(a.baseStats, b.baseStats)
        assertEquals(a.catchRate, b.catchRate)
    }

    @Test fun `dex covers all 12 aspects`() {
        val aspects = EchoformDex.all().map { it.primaryAspect }.toSet()
        assertTrue("every non-null aspect must appear at least once",
            aspects.size >= 12)
    }

    @Test fun `encounter pool returns common-tier species`() {
        val pool = EchoformDex.encounterPool(regionSeed = 1L, count = 8)
        assertEquals(8, pool.size)
        // All in common-tier band
        assertTrue(pool.all { it.catchRate >= 150 })
    }

    // ─────────────────────────────────────────────────────────────────
    // Capture mechanic — Gen-3-style
    // ─────────────────────────────────────────────────────────────────

    @Test fun `capture is hardest at full HP`() {
        val opp = PilotEchoforms.opponent()
        val full = CaptureMechanic.successChance(opp.copy(currentVigor = opp.maxVigor))
        val critical = CaptureMechanic.successChance(opp.copy(currentVigor = 1))
        assertTrue("low HP should be easier to capture: full=$full critical=$critical",
            critical > full)
    }

    @Test fun `legendary catch rate is much harder than common`() {
        val common = EchoformDex.all().first { it.catchRate >= 150 }
        val maybeLegendary = EchoformDex.all().firstOrNull { it.catchRate <= 30 }
        if (maybeLegendary != null) {
            val state = PilotEchoforms.playerStarter() // any L18 instance shape
            // ad-hoc: build instances at 1 HP for max catch chance
            val cInst = com.aetherbound.game.core.EchoformInstance(
                species = common, level = 14, techniques = state.techniques,
                currentVigor = 1,
            )
            val lInst = com.aetherbound.game.core.EchoformInstance(
                species = maybeLegendary, level = 14, techniques = state.techniques,
                currentVigor = 1,
            )
            val pCommon = CaptureMechanic.successChance(cInst)
            val pLegendary = CaptureMechanic.successChance(lInst)
            assertTrue("common should be easier than legendary at 1HP: c=$pCommon L=$pLegendary",
                pCommon > pLegendary)
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // AtlasMap — Pilot map structure
    // ─────────────────────────────────────────────────────────────────

    @Test fun `atlas pilot map has expected dimensions`() {
        val m = AtlasPilotMap.build()
        assertEquals(33, m.cols)
        assertEquals(24, m.rows)
        assertEquals(33, m.ground.size.let { m.ground[0].size })
        assertEquals(24, m.ground.size)
    }

    @Test fun `atlas pilot map contains encounter cells`() {
        val m = AtlasPilotMap.build()
        assertFalse("pilot map must define at least one encounter cell",
            m.encounter.isEmpty())
    }

    @Test fun `atlas pilot map contains solid cells for water and buildings`() {
        val m = AtlasPilotMap.build()
        // Top rows (deep sea) should be solid
        for (c in 0 until m.cols) {
            assertTrue("(${c}, 0) should be solid (deep sea)", c to 0 in m.solid)
        }
    }

    @Test fun `atlas pilot map building doors are walkable`() {
        val m = AtlasPilotMap.build()
        // SmallHouse at (4, 8): doorCol=1 doorRow=2 -> world cell (5, 10)
        val doorCells = listOf(5 to 10, 23 to 10, 5 to 19, 22 to 19)
        for (door in doorCells) {
            assertFalse("door cell $door must be walkable, but was solid",
                door in m.solid)
        }
    }
}
