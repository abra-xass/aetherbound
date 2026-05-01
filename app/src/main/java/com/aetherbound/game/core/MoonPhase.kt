package com.aetherbound.game.core

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Real-clock moon-phase calculator. Drives the rare evolution triggers
 * `moon_full` and `moon_new` declared in `assets/content/evolution_chains.json`.
 *
 * Algorithm: synodic month ≈ 29.53059 days. Reference new moon: 2000-01-06.
 * For each in-game day we compute days-since-reference, modulo synodic
 * month, then bin into 8 phases (Pokémon convention).
 *
 * **Tolerance:** the evolution check accepts ±1 day around exact
 * full/new so the player has a wider window to trigger it.
 */
object MoonPhase {

    enum class Phase {
        NEW,            // 0% illumination
        WAXING_CRESCENT,
        FIRST_QUARTER,
        WAXING_GIBBOUS,
        FULL,           // 100% illumination
        WANING_GIBBOUS,
        LAST_QUARTER,
        WANING_CRESCENT,
    }

    private const val SYNODIC_MONTH_DAYS = 29.530588853
    private val REFERENCE_NEW_MOON: LocalDate = LocalDate.of(2000, 1, 6)

    /** Current moon phase from device clock. */
    fun phaseToday(): Phase = phaseOn(LocalDate.now())

    /** Phase on a specific date — testable / determinism for save replay. */
    fun phaseOn(date: LocalDate): Phase {
        val daysSinceRef = ChronoUnit.DAYS.between(REFERENCE_NEW_MOON, date).toDouble()
        val phase = ((daysSinceRef % SYNODIC_MONTH_DAYS) + SYNODIC_MONTH_DAYS) % SYNODIC_MONTH_DAYS
        val idx = ((phase / SYNODIC_MONTH_DAYS) * 8.0).toInt().coerceIn(0, 7)
        return Phase.values()[idx]
    }

    /**
     * True if today is within ±1 day of FULL moon. Used by the
     * `moon_full` evolution trigger as a soft window.
     */
    fun isFullMoonNight(date: LocalDate = LocalDate.now()): Boolean {
        val phase = phaseOn(date)
        return phase == Phase.FULL ||
            (phase == Phase.WAXING_GIBBOUS && phaseOn(date.plusDays(1)) == Phase.FULL) ||
            (phase == Phase.WANING_GIBBOUS && phaseOn(date.minusDays(1)) == Phase.FULL)
    }

    /** True if today is within ±1 day of NEW moon. */
    fun isNewMoonNight(date: LocalDate = LocalDate.now()): Boolean {
        val phase = phaseOn(date)
        return phase == Phase.NEW ||
            (phase == Phase.WAXING_CRESCENT && phaseOn(date.minusDays(1)) == Phase.NEW) ||
            (phase == Phase.WANING_CRESCENT && phaseOn(date.plusDays(1)) == Phase.NEW)
    }

    /** German display name for UI tooltip. */
    fun displayDe(phase: Phase): String = when (phase) {
        Phase.NEW -> "Neumond"
        Phase.WAXING_CRESCENT -> "zunehmender Sichelmond"
        Phase.FIRST_QUARTER -> "erstes Viertel"
        Phase.WAXING_GIBBOUS -> "zunehmender Gibbous"
        Phase.FULL -> "Vollmond"
        Phase.WANING_GIBBOUS -> "abnehmender Gibbous"
        Phase.LAST_QUARTER -> "letztes Viertel"
        Phase.WANING_CRESCENT -> "abnehmender Sichelmond"
    }

    /** Emoji for HUD / dialogue. */
    fun emoji(phase: Phase): String = when (phase) {
        Phase.NEW -> "🌑"
        Phase.WAXING_CRESCENT -> "🌒"
        Phase.FIRST_QUARTER -> "🌓"
        Phase.WAXING_GIBBOUS -> "🌔"
        Phase.FULL -> "🌕"
        Phase.WANING_GIBBOUS -> "🌖"
        Phase.LAST_QUARTER -> "🌗"
        Phase.WANING_CRESCENT -> "🌘"
    }
}
