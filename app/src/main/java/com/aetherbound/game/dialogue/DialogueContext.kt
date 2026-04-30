package com.aetherbound.game.dialogue

import com.aetherbound.game.core.data.Gender

/**
 * Runtime state passed into [DialogueResolver.resolve] — used to inject
 * Tracery variables that templates can reference, like `#playerName#`,
 * `#playerGender#`, `#today_weather#`, `#current_rumor#`.
 *
 * Most fields are optional / nullable so callers can pass only what they
 * have. The resolver fills in safe defaults for absent slots so a missing
 * piece of context never crashes a template expansion.
 */
data class DialogueContext(
    /** Player's chosen name (from NameInputScreen). Empty → "Reisender". */
    val playerName: String = "",
    /** Player's chosen gender (from NameInputScreen). Null → "nichtbinär". */
    val playerGender: Gender? = null,
    /** In-game days played — lets time-aware NPCs ("lange nicht gesehen") work. */
    val daysPlayed: Int = 0,
    /**
     * Snapshot of the world's time/weather/weekday — feeds the
     * `#dayPhase#`, `#weekday#`, `#weather#`, `#weekdayAspect#` Tracery
     * slots used by spawn-hint NPC dialogue. Defaults to a generic clear
     * Wednesday-noon if not set.
     */
    val world: com.aetherbound.game.core.data.AmbientTime.Snapshot? = null,
    /**
     * Daily-pack slot overrides — e.g. `btc_price="47230"`,
     * `iran_oil_event="Hormuz-Blockade"`. Empty by default; fed by the
     * DailyPackFetcher (separate session).
     */
    val dailySlots: Map<String, String> = emptyMap(),
) {
    /**
     * Convert to the initial Tracery scope. Keys here become directly
     * referenceable as `#name#` inside grammar templates.
     */
    fun toTraceryScope(): Map<String, String> {
        val out = mutableMapOf<String, String>()
        out["playerName"] = playerName.ifBlank { "Reisender" }
        out["playerGender"] = playerGender?.tracerySlot ?: "n"
        out["playerPronoun"] = when (playerGender) {
            Gender.MASCULINE -> "er"
            Gender.FEMININE -> "sie"
            Gender.NEUTRAL, null -> "they"
        }
        out["daysPlayed"] = daysPlayed.toString()

        // World snapshot → time/weather/weekday slots for spawn-hint
        // templates. Each is filled with a sensible default if no world
        // snapshot was passed.
        val snapshot = world ?: com.aetherbound.game.core.data.AmbientTime.snapshotNow()
        out["dayPhase"] = phaseDe(snapshot.phase)
        out["weekday"] = com.aetherbound.game.core.WeekdayLore.displayDe(snapshot.weekday)
        out["weekdayPlural"] = com.aetherbound.game.core.WeekdayLore.displayDePlural(snapshot.weekday)
        out["weather"] = snapshot.weather.displayDe
        out["weatherAdj"] = snapshot.weather.tracerySlot
        out["weatherEmoji"] = snapshot.weather.emoji
        val biasAspect = com.aetherbound.game.core.WeekdayLore.biasedAspect(snapshot.weekday)
        out["weekdayAspect"] = com.aetherbound.game.core.WeekdayLore.displayDeAspect(biasAspect)

        out.putAll(dailySlots)
        return out
    }

    private fun phaseDe(phase: com.aetherbound.game.core.data.DayNightPhase): String = when (phase) {
        com.aetherbound.game.core.data.DayNightPhase.DAWN    -> "Morgendämmerung"
        com.aetherbound.game.core.data.DayNightPhase.MORNING -> "Morgen"
        com.aetherbound.game.core.data.DayNightPhase.NOON    -> "Mittag"
        com.aetherbound.game.core.data.DayNightPhase.EVENING -> "Abend"
        com.aetherbound.game.core.data.DayNightPhase.DUSK    -> "Abenddämmerung"
        com.aetherbound.game.core.data.DayNightPhase.NIGHT   -> "Nacht"
    }
}
