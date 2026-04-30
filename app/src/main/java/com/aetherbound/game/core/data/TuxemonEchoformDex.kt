package com.aetherbound.game.core.data

import android.content.Context
import com.aetherbound.game.core.EchoformSpecies
import com.aetherbound.game.core.ScreenTheme
import com.aetherbound.game.core.Technique
import kotlin.random.Random

/**
 * JSON-driven Echoform dex. Loads all 411 Tuxemon monsters lazily from
 * `assets/game/data/monsters.json`, converts each to an [EchoformSpecies]
 * via [TuxemonAdapter], and serves O(1) lookups by slug.
 *
 * Coexists with the procedural [com.aetherbound.game.core.EchoformDex]
 * (E001..E300 generated species) — that dex remains the "pilot" content
 * for now. Switch to this dex once the Tuxemon-derived stat curves and
 * movesets are wired to the battle scene.
 *
 * Usage:
 *   val mon = TuxemonEchoformDex.bySlug(ctx, "agnidon")
 *   val moves = TuxemonEchoformDex.movesetAt(ctx, "agnidon", level = 12)
 */
object TuxemonEchoformDex {
    private var cached: Map<String, EchoformSpecies>? = null

    fun load(ctx: Context): Map<String, EchoformSpecies> {
        cached?.let { return it }
        val mons = TuxemonDex.load(ctx)
        val map = LinkedHashMap<String, EchoformSpecies>(mons.size)
        for ((slug, mon) in mons) {
            map[slug] = TuxemonAdapter.echoformFromTuxemon(mon)
        }
        cached = map
        return map
    }

    fun bySlug(ctx: Context, slug: String): EchoformSpecies? = load(ctx)[slug]
    fun all(ctx: Context): Collection<EchoformSpecies> = load(ctx).values
    fun count(ctx: Context): Int = load(ctx).size

    // ───────────────────────────────────────────────────────────────
    // Moveset queries. The Tuxemon moveset is a list of (level, slug)
    // pairs. We resolve each to a Technique through TuxemonAdapter.
    // ───────────────────────────────────────────────────────────────

    /** All techniques known by [slug] at or below [level], sorted by learn level. */
    fun movesetAt(ctx: Context, slug: String, level: Int): List<Technique> {
        val mon = TuxemonDex.bySlug(ctx, slug) ?: return emptyList()
        val techDex = TuxemonTechniqueDex.load(ctx)
        return mon.moveset
            .filter { it.levelLearned <= level }
            .sortedBy { it.levelLearned }
            .mapNotNull { entry ->
                techDex[entry.technique]?.let { TuxemonAdapter.techniqueFromTuxemon(it) }
            }
    }

    /**
     * The "current four" — last 4 techniques learned at or below [level],
     * mirroring Pokémon's 4-move slot rule.
     */
    fun activeMovesetAt(ctx: Context, slug: String, level: Int, slots: Int = 4): List<Technique> =
        movesetAt(ctx, slug, level).takeLast(slots)

    /**
     * Full level-keyed moveset map for [slug]. Used by [ExperienceEngine.addXp]
     * to detect which moves unlock between an old level and a new level after
     * an XP gain. Each pair is `(levelLearned, technique)`.
     */
    fun levelMovesetMap(ctx: Context, slug: String): List<Pair<Int, Technique>> {
        val mon = TuxemonDex.bySlug(ctx, slug) ?: return emptyList()
        val techDex = TuxemonTechniqueDex.load(ctx)
        return mon.moveset
            .sortedBy { it.levelLearned }
            .mapNotNull { entry ->
                techDex[entry.technique]?.let { entry.levelLearned to TuxemonAdapter.techniqueFromTuxemon(it) }
            }
    }

    /**
     * Encounter pool filtered by biome **and** spawn-conditions
     * (time-of-day / weekday / weather). Returns at most [count] species.
     *
     * Each species' [com.aetherbound.game.core.SpawnConditions.spawnRateBoost]
     * multiplies its rarity poolWeight when conditions match — so a
     * weather-affinity species gets ~2× spawn-rate when it rains.
     */
    fun encounterPool(
        ctx: Context,
        theme: ScreenTheme,
        regionSeed: Long,
        count: Int = 8,
    ): List<EchoformSpecies> {
        // One snapshot for the whole roll — keeps the pool internally
        // consistent (a species either passes all conditions or none).
        val snapshot = AmbientTime.snapshotNow()
        val candidates = load(ctx).values.filter {
            !it.isLegendary &&
                (theme in it.biomes || it.biomes.isEmpty()) &&
                it.spawn.matches(snapshot)
        }
        if (candidates.isEmpty()) return emptyList()
        val rng = Random(regionSeed)
        val picks = mutableListOf<EchoformSpecies>()
        repeat(count * 4) {
            val sp = candidates.random(rng)
            // Effective weight = base rarity poolWeight × effectiveBoost
            // (boost handles weather as soft multiplier — weather never
            // excludes, only multiplies the spawn rate when it matches).
            val boost = sp.spawn.effectiveBoost(snapshot)
            val weight = (sp.rarity.poolWeight * boost).toInt().coerceIn(1, 100)
            if (rng.nextInt(100) < weight) {
                picks += sp
                if (picks.size >= count) return picks.distinctBy { it.id }
            }
        }
        return picks.distinctBy { it.id }.take(count).ifEmpty {
            (1..count.coerceAtMost(candidates.size))
                .map { candidates.random(rng) }
                .distinctBy { it.id }
        }
    }
}
