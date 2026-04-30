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

    /** Encounter pool filtered by biome. Returns at most [count] species. */
    fun encounterPool(
        ctx: Context,
        theme: ScreenTheme,
        regionSeed: Long,
        count: Int = 8,
    ): List<EchoformSpecies> {
        val candidates = load(ctx).values.filter {
            !it.isLegendary && (theme in it.biomes || it.biomes.isEmpty())
        }
        if (candidates.isEmpty()) return emptyList()
        val rng = Random(regionSeed)
        val picks = mutableListOf<EchoformSpecies>()
        repeat(count * 4) {
            val sp = candidates.random(rng)
            if (rng.nextInt(100) < sp.rarity.poolWeight) {
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
