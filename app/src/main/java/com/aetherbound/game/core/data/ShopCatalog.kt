package com.aetherbound.game.core.data

/**
 * Item-pricing catalogue — Pokémon-Gen-3-balanced relative to Aetherbound's
 * trainer-reward curve ($240–$1500). See `docs/economy-design.md` for the
 * full rationale.
 *
 * Two tiers of progression-gating:
 *
 *   - **Consumables** (Potions, Balls, Berries) — small, frequent drains
 *     that keep money flowing back into the economy.
 *   - **Investments** (Stones, TMs, Story-items) — rare big-ticket buys
 *     the player saves toward.
 *
 * Items not in this table are unsellable / unbuyable (story-only). The
 * `buy` price is what you pay at shops; `sell` is the trade-in value
 * (typically ½ of buy, like classic Pokémon).
 */
object ShopCatalog {

    data class Listing(
        val slug: String,
        val buyPrice: Int,
        val sellPrice: Int = buyPrice / 2,
        val category: ShopCategory,
    )

    enum class ShopCategory(val label: String) {
        Heal("Healing"),
        Balls("Capture"),
        Stones("Evolution"),
        TMs("Techniques"),
        Berries("Berries"),
        Repels("Repellents"),
        Story("Story"),
    }

    /** Indexed by slug for O(1) price lookup. */
    val ALL: Map<String, Listing> = buildMap {
        // ── Heal ────────────────────────────────────────────
        listing("potion",          200, ShopCategory.Heal)
        listing("super_potion",    700, ShopCategory.Heal)
        listing("hyper_potion",   1500, ShopCategory.Heal)
        listing("max_potion",     2500, ShopCategory.Heal)
        listing("revive",         1500, ShopCategory.Heal)
        listing("max_revive",     4000, ShopCategory.Heal)
        listing("cureall",         600, ShopCategory.Heal)
        listing("antidote",        300, ShopCategory.Heal)
        listing("burn_heal",       300, ShopCategory.Heal)
        listing("ice_heal",        300, ShopCategory.Heal)
        listing("paralyze_heal",   300, ShopCategory.Heal)
        listing("awakening",       300, ShopCategory.Heal)

        // ── Balls ───────────────────────────────────────────
        listing("tuxeball",            200, ShopCategory.Balls)
        listing("tuxeball_great",      600, ShopCategory.Balls)
        listing("tuxeball_ultra",     1200, ShopCategory.Balls)
        listing("tuxeball_lure",      1000, ShopCategory.Balls)
        listing("tuxeball_quick",     1000, ShopCategory.Balls)
        listing("tuxeball_timer",     1000, ShopCategory.Balls)
        listing("tuxeball_dive",      1500, ShopCategory.Balls)
        listing("tuxeball_dusk",      1500, ShopCategory.Balls)
        listing("tuxeball_park",      1500, ShopCategory.Balls)
        // tuxeball_master — story-only, NOT in catalog

        // ── Stones (evolution triggers) ─────────────────────
        listing("fire_stone",     2100, ShopCategory.Stones)
        listing("water_stone",    2100, ShopCategory.Stones)
        listing("wood_stone",     2100, ShopCategory.Stones)
        listing("frost_stone",    2100, ShopCategory.Stones)
        listing("earth_stone",    2100, ShopCategory.Stones)
        listing("sky_stone",      2100, ShopCategory.Stones)
        listing("sun_stone",      3000, ShopCategory.Stones)
        listing("moon_stone",     3000, ShopCategory.Stones)
        listing("cosmic_stone",   5000, ShopCategory.Stones)
        listing("shadow_stone",   3500, ShopCategory.Stones)
        listing("metal_stone",    2500, ShopCategory.Stones)

        // ── Berries / held items ────────────────────────────
        listing("oran_berry",      100, ShopCategory.Berries)
        listing("sitrus_berry",    300, ShopCategory.Berries)
        listing("leppa_berry",     300, ShopCategory.Berries)
        listing("cheri_berry",     200, ShopCategory.Berries)
        listing("chesto_berry",    200, ShopCategory.Berries)
        listing("rawst_berry",     200, ShopCategory.Berries)

        // ── Repellents ──────────────────────────────────────
        listing("repel",            350, ShopCategory.Repels)
        listing("super_repel",      500, ShopCategory.Repels)
        listing("max_repel",        700, ShopCategory.Repels)

        // ── Generic TM tiers — exact tech slugs filled by content team ──
        // We expose three tiers and the actual slug lookup comes from
        // TuxemonTechniqueDex when the user opens the TM section.
        listing("tm_common",       1000, ShopCategory.TMs)
        listing("tm_rare",         3000, ShopCategory.TMs)
        listing("tm_epic",         7500, ShopCategory.TMs)

        // ── Story-only big-ticket (only sold by specific NPCs) ──
        listing("bicycle",         5000, ShopCategory.Story)
        listing("aether_lens",    10000, ShopCategory.Story)
    }

    fun priceOf(slug: String): Int? = ALL[slug]?.buyPrice
    fun sellPriceOf(slug: String): Int = ALL[slug]?.sellPrice ?: 0
    fun isBuyable(slug: String): Boolean = ALL.containsKey(slug)
    fun byCategory(): Map<ShopCategory, List<Listing>> =
        ALL.values.groupBy { it.category }

    private fun MutableMap<String, Listing>.listing(
        slug: String,
        buy: Int,
        cat: ShopCategory,
        sellOverride: Int? = null,
    ) {
        put(slug, Listing(slug = slug, buyPrice = buy, sellPrice = sellOverride ?: (buy / 2), category = cat))
    }
}
