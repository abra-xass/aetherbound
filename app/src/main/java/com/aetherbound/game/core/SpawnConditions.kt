package com.aetherbound.game.core

import com.aetherbound.game.core.data.AmbientTime
import com.aetherbound.game.core.data.DayNightPhase
import java.time.DayOfWeek

/**
 * Per-species spawn restrictions.
 *
 * **Hard-lock fields** (block spawn when condition fails):
 *   timeOfDay   — limits when of day (Dawn/Day/Dusk/Night)
 *   weekdays    — limits which weekdays (e.g. only Wednesdays)
 *
 * **Soft-boost field** (never blocks, only multiplies spawn rate):
 *   weather     — preferred weather; when current weather is in this set,
 *                 [spawnRateBoost] is applied. Other weather → normal rate.
 *
 *   spawnRateBoost — multiplier on spawn weight when weather matches.
 *
 * Examples:
 *   - hard-lock:  `timeOfDay = setOf(NIGHT)`             → only at night, never day
 *   - hard-lock:  `weekdays = setOf(WEDNESDAY)`          → only on Wednesdays
 *   - soft-buff:  `weather = setOf(RAIN), boost = 2f`    → spawns always, ×2 in rain
 *   - combined:   timeOfDay=NIGHT + weather=RAIN+boost   → night-only AND ×2 in rain
 */
data class SpawnConditions(
    val timeOfDay: Set<DayNightPhase>? = null,
    val weekdays: Set<DayOfWeek>? = null,
    val weather: Set<Weather>? = null,
    val spawnRateBoost: Float = 1.0f,
) {
    /**
     * True when [snapshot] satisfies the **hard-lock** fields (timeOfDay +
     * weekdays). Weather is intentionally NOT checked here — weather is
     * always a soft boost via [effectiveBoost], never a lock-out.
     */
    fun matches(snapshot: AmbientTime.Snapshot): Boolean {
        if (timeOfDay != null && snapshot.phase !in timeOfDay) return false
        if (weekdays != null && snapshot.weekday !in weekdays) return false
        return true
    }

    /**
     * Effective weight multiplier given [snapshot]. Returns 0 if any
     * hard-lock condition fails. Otherwise returns [spawnRateBoost] if
     * the current weather is in [weather] (or weather is null), and
     * 1.0 if a weather preference exists but doesn't match today.
     */
    fun effectiveBoost(snapshot: AmbientTime.Snapshot): Float {
        if (!matches(snapshot)) return 0f
        val weatherMatches = weather == null || snapshot.weather in weather
        return if (weatherMatches) spawnRateBoost else 1.0f
    }

    val isUnrestricted: Boolean
        get() = timeOfDay == null && weekdays == null && weather == null && spawnRateBoost == 1.0f

    companion object {
        /** No restrictions — most species use this. */
        val ANYTIME = SpawnConditions()

        // Convenience presets the auto-curator in [TuxemonAdapter] uses.
        val NIGHT_ONLY = SpawnConditions(timeOfDay = setOf(DayNightPhase.NIGHT, DayNightPhase.DUSK))
        val DAY_ONLY = SpawnConditions(timeOfDay = setOf(DayNightPhase.DAWN, DayNightPhase.MORNING, DayNightPhase.NOON))
        val DAWN_DUSK = SpawnConditions(timeOfDay = setOf(DayNightPhase.DAWN, DayNightPhase.DUSK))

        val RAIN_BOOST = SpawnConditions(weather = setOf(Weather.RAIN, Weather.STORM), spawnRateBoost = 2.0f)
        val FOG_ONLY = SpawnConditions(weather = setOf(Weather.FOG))
        val STORM_ONLY = SpawnConditions(weather = setOf(Weather.STORM))
        val SNOW_BOOST = SpawnConditions(weather = setOf(Weather.SNOW), spawnRateBoost = 2.5f)

        /** Weekday-locked — for that "Mittwoch-Cosmic" feel. */
        fun onlyOnWeekday(day: DayOfWeek) = SpawnConditions(weekdays = setOf(day))
    }
}

/**
 * Maps a weekday to a "biased aspect" — used by NPC dialogue to give
 * tips like *"An Mittwochen sind Cosmic-Echoforms aktiver."*
 *
 * Lore: each day of the Aether-week resonates with a different element.
 */
object WeekdayLore {
    private val MAP: Map<DayOfWeek, Aspect> = mapOf(
        DayOfWeek.MONDAY    to Aspect.METAL,
        DayOfWeek.TUESDAY   to Aspect.HEROIC,
        DayOfWeek.WEDNESDAY to Aspect.COSMIC,
        DayOfWeek.THURSDAY  to Aspect.LIGHTNING,
        DayOfWeek.FRIDAY    to Aspect.SKY,
        DayOfWeek.SATURDAY  to Aspect.FIRE,
        DayOfWeek.SUNDAY    to Aspect.WOOD,
    )

    fun biasedAspect(day: DayOfWeek): Aspect = MAP[day] ?: Aspect.NORMAL

    /** German weekday name. */
    fun displayDe(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY    -> "Montag"
        DayOfWeek.TUESDAY   -> "Dienstag"
        DayOfWeek.WEDNESDAY -> "Mittwoch"
        DayOfWeek.THURSDAY  -> "Donnerstag"
        DayOfWeek.FRIDAY    -> "Freitag"
        DayOfWeek.SATURDAY  -> "Samstag"
        DayOfWeek.SUNDAY    -> "Sonntag"
    }

    /** German "an Mittwochen / an Sonntagen" form. */
    fun displayDePlural(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY    -> "Montagen"
        DayOfWeek.TUESDAY   -> "Dienstagen"
        DayOfWeek.WEDNESDAY -> "Mittwochen"
        DayOfWeek.THURSDAY  -> "Donnerstagen"
        DayOfWeek.FRIDAY    -> "Freitagen"
        DayOfWeek.SATURDAY  -> "Samstagen"
        DayOfWeek.SUNDAY    -> "Sonntagen"
    }

    fun displayDeAspect(aspect: Aspect): String = when (aspect) {
        Aspect.COSMIC -> "Kosmische"
        Aspect.EARTH -> "Erde-"
        Aspect.FIRE -> "Feuer-"
        Aspect.FROST -> "Eis-"
        Aspect.HEROIC -> "Licht-"
        Aspect.LIGHTNING -> "Sturm-"
        Aspect.METAL -> "Metall-"
        Aspect.NORMAL -> "Aether-"
        Aspect.SHADOW -> "Schatten-"
        Aspect.SKY -> "Wind-"
        Aspect.VENOM -> "Gift-"
        Aspect.WATER -> "Wasser-"
        Aspect.WOOD -> "Wald-"
        Aspect.DREAM -> "Traum-"
        Aspect.MIND -> "Geist-"
        Aspect.SOUND -> "Klang-"
        Aspect.TIME -> "Zeit-"
        Aspect.CRYSTAL -> "Kristall-"
        Aspect.BLOOD -> "Blut-"
    }
}
