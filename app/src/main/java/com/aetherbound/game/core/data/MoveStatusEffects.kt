package com.aetherbound.game.core.data

/**
 * Maps Tuxemon technique slugs to the status they apply on hit.
 *
 * Tuxemon's `effects[]` on technique JSON includes entries like
 * `{type: "status", parameters: ["status_burned"]}`. Our [TuxemonTechnique]
 * data class doesn't currently retain the parsed effects (focus has been
 * the visual + damage layer first), so we provide a static lookup table
 * keyed by move slug — covers the canonical Pokémon-style moves.
 *
 * Extend the table as more moves get wired up. The function returns null
 * when a move has no status effect.
 */
object MoveStatusEffects {

    /** Move-slug → (status-slug, application-chance 0..1). */
    private val TABLE: Map<String, Pair<String, Double>> = mapOf(
        // ── Burn ────────────────────────────────────────────────
        "fire_punch" to ("burned" to 0.10),
        "flame_strike" to ("burned" to 0.20),
        "ember" to ("burned" to 0.10),
        "flamethrower" to ("burned" to 0.10),
        "fire_blast" to ("burned" to 0.30),

        // ── Freeze ──────────────────────────────────────────────
        "ice_punch" to ("frozen" to 0.10),
        "ice_beam" to ("frozen" to 0.10),
        "blizzard" to ("frozen" to 0.10),
        "freeze_tag" to ("frozen" to 0.30),

        // ── Paralyze ────────────────────────────────────────────
        "thunder_punch" to ("paralyzed" to 0.10),
        "thunder_shock" to ("paralyzed" to 0.10),
        "thunderbolt" to ("paralyzed" to 0.10),
        "thunder" to ("paralyzed" to 0.30),
        "static_shock" to ("paralyzed" to 0.30),

        // ── Poison ──────────────────────────────────────────────
        "poison_sting" to ("poisoned" to 0.30),
        "poison_jab" to ("poisoned" to 0.30),
        "acid" to ("poisoned" to 0.10),
        "sludge_bomb" to ("poisoned" to 0.30),
        "venom_drench" to ("poisoned" to 1.00),

        // ── Sleep ───────────────────────────────────────────────
        "sleep_powder" to ("sleeping" to 0.75),
        "sing" to ("sleeping" to 0.55),
        "lullaby" to ("sleeping" to 0.85),
        "hypnosis" to ("sleeping" to 0.60),

        // ── Confusion ───────────────────────────────────────────
        "confuse_ray" to ("confused" to 1.00),
        "psybeam" to ("confused" to 0.10),
        "swagger" to ("confused" to 1.00),

        // ── Blind / Misc ────────────────────────────────────────
        "sand_attack" to ("blinded" to 1.00),
        "smokescreen" to ("blinded" to 1.00),
        "flash" to ("blinded" to 1.00),
    )

    /**
     * Returns (status-slug, chance) for [moveSlug], or null if the move has
     * no status effect.
     */
    fun statusFor(moveSlug: String): Pair<String, Double>? = TABLE[moveSlug.lowercase()]

    /**
     * Roll a status application for a move that just hit. Returns the
     * status slug to apply (caller looks it up in [TuxemonStatusDex]) or
     * null if the move has no status or the roll missed.
     */
    fun rollStatus(moveSlug: String, rng: kotlin.random.Random): String? {
        val (slug, chance) = statusFor(moveSlug) ?: return null
        return if (rng.nextDouble() < chance) slug else null
    }
}
