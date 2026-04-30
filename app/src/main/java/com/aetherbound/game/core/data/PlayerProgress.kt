package com.aetherbound.game.core.data

/**
 * Aggregated meta-progress for the player. Lives separately from
 * [SaveGame] so the UI can read it without rebuilding the whole save
 * payload, and so a future "any-save Bestiary" feature works (caught
 * across all slots).
 *
 *   playerName   — Trainer name shown in HUD / save slots.
 *   seenSlugs    — every slug the player has ever seen in battle.
 *   caughtSlugs  — every slug the player has captured.
 *   badgesEarned — list of trainer-IDs whose battle was won.
 *   playtimeSec  — running total in-game-time across all sessions.
 *
 * Audio settings live here too because they're meta-progress in the
 * save-data sense (persist between runs).
 */
data class PlayerProgress(
    val playerName: String = "Player",
    val seenSlugs: Set<String> = emptySet(),
    val caughtSlugs: Set<String> = emptySet(),
    val badgesEarned: Set<String> = emptySet(),
    val playtimeSec: Long = 0,
    val audio: AudioSettings = AudioSettings(),
    val controls: ControlSettings = ControlSettings(),
    /**
     * One-shot world flags — used by [com.aetherbound.game.render.map.WorldEvent.ItemDrop]
     * pickups and any future story flags so they don't re-trigger on revisit.
     * Examples: `"item_pokeball_route1"`, `"story_intro_done"`, `"trainer_hiker_beaten"`.
     */
    val collectedFlags: Set<String> = emptySet(),
) {
    fun see(slug: String): PlayerProgress = copy(seenSlugs = seenSlugs + slug)
    fun capture(slug: String): PlayerProgress = copy(
        seenSlugs = seenSlugs + slug,
        caughtSlugs = caughtSlugs + slug,
    )
    fun earnBadge(trainerId: String): PlayerProgress =
        copy(badgesEarned = badgesEarned + trainerId)
    fun setFlag(flag: String): PlayerProgress = copy(collectedFlags = collectedFlags + flag)
    fun hasFlag(flag: String): Boolean = flag in collectedFlags

    /** Pokédex-style completion percentage out of [totalSpecies]. */
    fun completionPercent(totalSpecies: Int = 411): Int =
        if (totalSpecies <= 0) 0 else (caughtSlugs.size * 100 / totalSpecies)
}

data class AudioSettings(
    /** 0..1 master multiplier on SFX. */
    val sfxVolume: Float = 0.85f,
    /** 0..1 master multiplier on BGM. */
    val musicVolume: Float = 0.6f,
    /** When false, [com.aetherbound.game.render.audio.AudioEngine.playMusic] no-ops. */
    val musicEnabled: Boolean = true,
    val sfxEnabled: Boolean = true,
)

data class ControlSettings(
    /** Step size in ms — higher = slower walking. */
    val stepDurationMs: Int = 220,
    /** When true, hardware-back returns to overworld; else exits app. */
    val backOpensMenu: Boolean = true,
    /** Override the real-clock day/night cycle with a fixed phase (debug). */
    val forcedPhase: DayNightPhase? = null,
)
