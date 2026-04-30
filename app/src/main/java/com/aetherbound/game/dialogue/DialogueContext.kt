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
     * Daily-pack slot overrides — e.g. `today_weather="Föhn"`,
     * `btc_price="47230"`, `iran_oil_event="Hormuz-Blockade"`. Empty by
     * default; fed by DailyPackFetcher (next session).
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
        out.putAll(dailySlots)
        return out
    }
}
