package com.aetherbound.game.core.data

/**
 * Canonical story-flag identifiers. Tracked in [PlayerProgress.collectedFlags]
 * so each beat fires exactly once across the whole save.
 *
 * Convention: `<chapter>_<beat>` lower_snake_case.
 */
object StoryFlags {
    const val INTRO_SEEN          = "story_intro_seen"
    const val FIRST_CAPTURE_SEEN  = "story_first_capture"
    const val FIRST_FAINT_SEEN    = "story_first_faint"
    const val FIRST_TRAINER_BEAT  = "story_first_trainer"
    const val LEFT_STARTING_TOWN  = "story_left_starting_town"
    const val GYM_LEADER_BEAT     = "story_gym_leader_beat"

    /** All known story flags — useful for debug "show all flags" lists. */
    val ALL: Set<String> = setOf(
        INTRO_SEEN, FIRST_CAPTURE_SEEN, FIRST_FAINT_SEEN,
        FIRST_TRAINER_BEAT, LEFT_STARTING_TOWN, GYM_LEADER_BEAT,
    )
}

/**
 * Story-beat dispatch — when a triggering event fires, look up the matching
 * intro/outro lines and surface them via dialog overlay if the flag wasn't
 * yet seen.
 */
object StoryBeats {
    data class Beat(val flag: String, val speaker: String, val lines: List<String>)

    val INTRO = Beat(
        flag = StoryFlags.INTRO_SEEN,
        speaker = "Aether-Vision",
        lines = listOf(
            "Welcome, traveler.",
            "The Aether between worlds is fraying. Echoforms — the souls of the wild — are slipping through.",
            "Bind to one. Catch others. Become the bridge that holds the worlds together.",
        ),
    )
    val FIRST_CAPTURE = Beat(
        flag = StoryFlags.FIRST_CAPTURE_SEEN,
        speaker = "Aether-Vision",
        lines = listOf(
            "Your first bond. The Echoform's resonance hums in your pocket now.",
            "Open the Bestiary anytime to see who you've met. The MENU has a Trainer Card too — track your journey.",
        ),
    )
    val FIRST_FAINT = Beat(
        flag = StoryFlags.FIRST_FAINT_SEEN,
        speaker = "Aether-Vision",
        lines = listOf(
            "Even Echoforms can fall.",
            "Find a healing pad — they shimmer pale-gold — to restore your party. Or use a Potion.",
        ),
    )
    val FIRST_TRAINER = Beat(
        flag = StoryFlags.FIRST_TRAINER_BEAT,
        speaker = "Aether-Vision",
        lines = listOf(
            "A trainer's badge etched on your card.",
            "Six leaders walk the world. Defeat them all to earn the Aether-Sigil.",
        ),
    )
    val GYM_LEADER = Beat(
        flag = StoryFlags.GYM_LEADER_BEAT,
        speaker = "Adept Lumin",
        lines = listOf(
            "The Aether-Sigil is yours.",
            "What lies beyond is for you to discover. The threads between worlds are stable, for now.",
            "But Echoforms still slip through. Keep walking.",
        ),
    )

    /** All beats indexed by their fire-condition flag. */
    val ALL: Map<String, Beat> = listOf(
        INTRO, FIRST_CAPTURE, FIRST_FAINT, FIRST_TRAINER, GYM_LEADER,
    ).associateBy { it.flag }
}
