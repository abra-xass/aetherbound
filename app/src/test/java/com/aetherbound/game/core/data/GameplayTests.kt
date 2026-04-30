package com.aetherbound.game.core.data

import com.aetherbound.game.core.Aspect
import com.aetherbound.game.core.AspectAffinity
import com.aetherbound.game.core.BaseStats
import com.aetherbound.game.core.EchoformInstance
import com.aetherbound.game.core.EchoformSpecies
import com.aetherbound.game.core.Potential
import com.aetherbound.game.core.Rarity
import com.aetherbound.game.core.Technique
import com.aetherbound.game.core.TechniqueCategory
import com.aetherbound.game.core.Temperament
import com.aetherbound.game.core.TrainingPoints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Engine-level smoke tests — every mechanic the player will hit
 * dozens of times per session. These don't require an Android Context,
 * so they run on the JVM in <500ms.
 */
class GameplayTests {

    // ── ExperienceCurve ─────────────────────────────────────────────

    @Test fun `xp curves agree on level 1 boundary`() {
        ExperienceCurve.values().forEach { curve ->
            assertEquals(0L, curve.xpForLevel(1))
        }
    }

    @Test fun `medium-fast curve matches L^3 formula`() {
        val curve = ExperienceCurve.MEDIUM_FAST
        assertEquals(8L, curve.xpForLevel(2))      // 2^3 = 8
        assertEquals(125L, curve.xpForLevel(5))    // 5^3 = 125
        assertEquals(1000L, curve.xpForLevel(10))  // 10^3 = 1000
    }

    @Test fun `level for xp is monotonic`() {
        val curve = ExperienceCurve.MEDIUM_FAST
        var lastLevel = 1
        for (xp in 0L..50000L step 250L) {
            val l = curve.levelForXp(xp)
            assertTrue("level decreased at xp=$xp", l >= lastLevel)
            lastLevel = l
        }
    }

    @Test fun `addXp triggers level-up when threshold crossed`() {
        val rookie = sampleInstance(level = 5)
        val curve = ExperienceCurve.MEDIUM_FAST
        val baseXp = curve.xpForLevel(5)        // 125
        val needed = curve.xpForLevel(6) - baseXp  // 91
        val result = ExperienceEngine.addXp(rookie, baseXp, needed.toInt(), curve)
        assertTrue(result.didLevelUp)
        assertEquals(6, result.newLevel)
    }

    // ── CaptureMath ─────────────────────────────────────────────────

    @Test fun `master ball captures every mon`() {
        val target = sampleInstance(level = 50, currentVigorPercent = 1.0)
        val rng = Random(42)
        val outcome = CaptureMath.attempt(
            target = target,
            ballMultiplier = CaptureMath.ballMultiplier("tuxeball_master"),
            statusMultiplier = 1.0,
            rng = rng,
        )
        assertTrue("master ball should always capture", outcome.captured)
        assertEquals(4, outcome.shakes)
    }

    @Test fun `low hp raises capture probability`() {
        val full = sampleInstance(level = 20, currentVigorPercent = 1.0)
        val low = sampleInstance(level = 20, currentVigorPercent = 0.05)
        var fullCaught = 0
        var lowCaught = 0
        repeat(500) {
            val rng = Random(it.toLong())
            val a = CaptureMath.attempt(full, 1.0, 1.0, rng)
            val b = CaptureMath.attempt(low, 1.0, 1.0, Random(it.toLong()))
            if (a.captured) fullCaught++
            if (b.captured) lowCaught++
        }
        assertTrue("low-HP must capture more often: full=$fullCaught low=$lowCaught", lowCaught > fullCaught)
    }

    @Test fun `status bonus stacks with sleep over paralyse`() {
        val sleepInst = listOf(StatusInstance(stubStatus("sleeping")))
        val paralyseInst = listOf(StatusInstance(stubStatus("paralyzed")))
        assertEquals(2.5, CaptureMath.statusMultiplier(sleepInst), 0.001)
        assertEquals(1.5, CaptureMath.statusMultiplier(paralyseInst), 0.001)
        assertEquals(1.0, CaptureMath.statusMultiplier(emptyList()), 0.001)
    }

    // ── AspectAffinity (canonical Tuxemon chart) ───────────────────

    @Test fun `fire is super effective on wood and weak to water`() {
        assertEquals(2.0, AspectAffinity.pairMultiplier(Aspect.FIRE, Aspect.WOOD), 0.001)
        assertEquals(0.5, AspectAffinity.pairMultiplier(Aspect.FIRE, Aspect.WATER), 0.001)
    }

    @Test fun `dual-type defender stacks multipliers`() {
        // Fire vs Wood/Metal: 2.0 × 2.0 = 4.0
        val mult = AspectAffinity.multiplier(Aspect.FIRE, Aspect.WOOD, Aspect.METAL)
        assertEquals(4.0, mult, 0.001)
    }

    @Test fun `same secondary type collapses to single multiplier`() {
        val mult = AspectAffinity.multiplier(Aspect.WATER, Aspect.FIRE, Aspect.FIRE)
        assertEquals(2.0, mult, 0.001)
    }

    // ── Party ───────────────────────────────────────────────────────

    @Test fun `party respects max 6 cap`() {
        var p = Party()
        for (i in 1..6) {
            val r = p.add(sampleInstance(level = i))
            assertTrue(r is Party.AddResult.Added)
            p = (r as Party.AddResult.Added).party
        }
        val overflow = p.add(sampleInstance(level = 7))
        assertTrue(overflow is Party.AddResult.PartyFull)
    }

    @Test fun `switch skips fainted slots`() {
        val alive = sampleInstance(level = 5)
        val dead = sampleInstance(level = 5, currentVigorPercent = 0.0)
        val p = Party(members = listOf(alive, dead, alive), activeIndex = 0)
        val withSwitch = p.switch(1)   // request fainted
        // Convention: cannot switch to fainted — stays at original.
        assertEquals(0, withSwitch.activeIndex)
        val viable = p.switch(2)
        assertEquals(2, viable.activeIndex)
    }

    @Test fun `pcStorage spills into next box when first fills`() {
        var s = PcStorage()
        repeat(31) { i -> s = s.deposit(sampleInstance(level = 1)) }
        assertEquals(2, s.boxes.size)
        assertEquals(30, s.boxes[0].slots.size)
        assertEquals(1, s.boxes[1].slots.size)
        assertEquals(31, s.totalCount)
    }

    // ── Inventory ───────────────────────────────────────────────────

    @Test fun `inventory remove returns null on insufficient stock`() {
        val inv = Inventory(stacks = mapOf("potion" to 2), money = 0)
        assertNotNull(inv.remove("potion", 1))
        assertNull(inv.remove("potion", 5))
    }

    @Test fun `money cannot go negative`() {
        val inv = Inventory(stacks = emptyMap(), money = 50)
        assertNull(inv.spend(100))
        assertNotNull(inv.spend(50))
    }

    // ── PlayerProgress ──────────────────────────────────────────────

    @Test fun `seenSlugs accumulate, capture sets seen`() {
        var p = PlayerProgress()
        p = p.see("agnidon")
        p = p.capture("rockitten")
        assertTrue("agnidon" in p.seenSlugs)
        assertTrue("rockitten" in p.seenSlugs)
        assertTrue("rockitten" in p.caughtSlugs)
        assertFalse("agnidon" in p.caughtSlugs)
    }

    @Test fun `flags are one-shot`() {
        var p = PlayerProgress()
        p = p.setFlag(StoryFlags.INTRO_SEEN)
        assertTrue(p.hasFlag(StoryFlags.INTRO_SEEN))
        // Re-setting is idempotent, doesn't error.
        p = p.setFlag(StoryFlags.INTRO_SEEN)
        assertEquals(1, p.collectedFlags.size)
    }

    // ── DamageVariance ──────────────────────────────────────────────

    @Test fun `STAB applies when move type matches attacker primary`() {
        val attacker = sampleInstance(level = 10, primary = Aspect.FIRE)
        val tech = Technique(
            id = "ember",
            name = "Ember",
            aspect = Aspect.FIRE,
            category = TechniqueCategory.STRIKE,
            power = 40, accuracy = 100, priority = 0, critRate = 0,
        )
        val rng = Random(1)
        val r = DamageVariance.roll(attacker, tech, rng)
        assertTrue("STAB should apply", r.stab)
    }

    @Test fun `crit chance progresses with crit-rate tier`() {
        val t0 = stubTech(critRate = 0)
        val t4 = stubTech(critRate = 4)
        assertEquals(0.0625, DamageVariance.critChance(t0), 0.001)
        assertEquals(0.5, DamageVariance.critChance(t4), 0.001)
    }

    // ── helpers ─────────────────────────────────────────────────────

    private fun sampleInstance(
        level: Int,
        currentVigorPercent: Double = 1.0,
        primary: Aspect = Aspect.NORMAL,
    ): EchoformInstance {
        val species = EchoformSpecies(
            id = "test",
            name = "Test",
            primaryAspect = primary,
            secondaryAspect = null,
            baseStats = BaseStats(60, 60, 60, 60, 60, 60),
            catchRate = 100,
            rarity = Rarity.Common,
            biomes = emptyList(),
        )
        val instance = EchoformInstance(
            species = species,
            level = level,
            techniques = listOf(stubTech()),
            potential = Potential(),
            training = TrainingPoints(),
            temperament = Temperament(),
            currentVigor = 1,
        )
        val maxHp = instance.maxVigor.coerceAtLeast(1)
        return instance.copy(currentVigor = (maxHp * currentVigorPercent).toInt().coerceIn(0, maxHp))
    }

    private fun stubTech(critRate: Int = 0): Technique = Technique(
        id = "stub", name = "Stub", aspect = Aspect.NORMAL,
        category = TechniqueCategory.STRIKE,
        power = 40, accuracy = 100, priority = 0, critRate = critRate,
    )

    private fun stubStatus(slug: String): TuxemonStatus = TuxemonStatus(
        slug = slug, condId = 0, sort = "meta", category = "negative",
        icon = null, statModifiers = emptyMap(),
    )
}
