package com.aetherbound.game.core.data

import androidx.compose.ui.graphics.Color
import java.time.LocalTime

/**
 * Tuxemon-style day/night-cycle, optionally tied to the player's wall-clock
 * for the Aetherbound "bound to ambient time" feeling.
 *
 * Six phases on a 24-hour wheel:
 *
 *   night    00:00 – 04:59
 *   dawn     05:00 – 06:59  (sun rising, magenta tint)
 *   morning  07:00 – 11:59  (cool-bright)
 *   noon     12:00 – 15:59  (peak warm)
 *   evening  16:00 – 19:59  (orange tint)
 *   dusk     20:00 – 23:59  (deep blue settling into night)
 *
 * Encounters can use [Phase] as an extra dimension on the encounter pool
 * (e.g. only spawn shadow Echoforms at night).
 */
enum class DayNightPhase(
    val displayName: String,
    /** Tint applied as a screen-overlay multiplier on the world. */
    val tint: Color,
    val tintAlpha: Float,
) {
    NIGHT("Night",     Color(0xFF1A1F4F), 0.45f),
    DAWN("Dawn",       Color(0xFFFF9DA8), 0.30f),
    MORNING("Morning", Color(0xFFFFF5C2), 0.10f),
    NOON("Noon",       Color(0xFFFFFAE0), 0.00f),
    EVENING("Evening", Color(0xFFFFB060), 0.30f),
    DUSK("Dusk",       Color(0xFF3A2F6A), 0.40f);
}

object DayNightCycle {

    /** Map any 24-hour time to a [DayNightPhase]. */
    fun phaseAt(hour: Int): DayNightPhase = when (hour.coerceIn(0, 23)) {
        in 0..4 -> DayNightPhase.NIGHT
        in 5..6 -> DayNightPhase.DAWN
        in 7..11 -> DayNightPhase.MORNING
        in 12..15 -> DayNightPhase.NOON
        in 16..19 -> DayNightPhase.EVENING
        else -> DayNightPhase.DUSK
    }

    /** Fetch the phase for the device's current local time. */
    fun phaseNow(): DayNightPhase = phaseAt(LocalTime.now().hour)

    /**
     * Linear interpolation between [a] and [b] colours by [t] (0..1). Used
     * by [TimeOfDayOverlay] to blend tints around phase boundaries.
     */
    fun lerp(a: Color, b: Color, t: Float): Color {
        val u = t.coerceIn(0f, 1f)
        return Color(
            red = a.red + (b.red - a.red) * u,
            green = a.green + (b.green - a.green) * u,
            blue = a.blue + (b.blue - a.blue) * u,
            alpha = a.alpha + (b.alpha - a.alpha) * u,
        )
    }

    /**
     * Tinting *by minute-of-day* — interpolates between the surrounding
     * phases so transitions are smooth. Returns (color, alpha).
     */
    fun smoothTint(hour: Int, minute: Int): Pair<Color, Float> {
        val phaseHere = phaseAt(hour)
        val phaseNext = phaseAt((hour + 1) % 24)
        if (phaseHere == phaseNext) return phaseHere.tint to phaseHere.tintAlpha
        val t = minute / 60f
        val color = lerp(phaseHere.tint, phaseNext.tint, t)
        val alpha = phaseHere.tintAlpha + (phaseNext.tintAlpha - phaseHere.tintAlpha) * t
        return color to alpha
    }
}
