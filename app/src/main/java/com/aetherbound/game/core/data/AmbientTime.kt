package com.aetherbound.game.core.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import java.time.LocalTime

/**
 * "Bound to Aether" — the in-game day/night cycle is tied to the real
 * device clock. Instead of a fast in-game clock that loops every X
 * minutes, Aetherbound's world brightens and darkens with the player's
 * actual day. Encounter pools and ambient mood follow.
 *
 * This is the single hook the rest of the engine should call:
 *
 *     val tint = rememberAmbientTint()           // Compose-recomposed every minute
 *     // → Pair<Color, Float> for screen-overlay blend
 *
 *     val phase = AmbientTime.phaseNow()         // for pool gating in EncounterEngine
 */
object AmbientTime {

    /**
     * When non-null, [phaseNow] / [tintNow] return this phase regardless
     * of the device clock. Used by `SettingsScreen` to let the user pin
     * a phase for screenshots / preview / debug.
     */
    var forcedPhase: DayNightPhase? = null

    /** Phase based on current device clock, unless [forcedPhase] is set. */
    fun phaseNow(): DayNightPhase = forcedPhase ?: DayNightCycle.phaseNow()

    /** Smooth tint based on current device clock minute, unless overridden. */
    fun tintNow(): Pair<Color, Float> {
        forcedPhase?.let { return it.tint to it.tintAlpha }
        val now = LocalTime.now()
        return DayNightCycle.smoothTint(hour = now.hour, minute = now.minute)
    }

    /** Encounter-pool predicate: returns true when [species] may spawn now. */
    fun isAllowedToSpawn(speciesPrimary: com.aetherbound.game.core.Aspect): Boolean {
        val phase = phaseNow()
        // Shadow Echoforms only at night/dusk; Heroic only at noon/morning.
        return when (speciesPrimary) {
            com.aetherbound.game.core.Aspect.SHADOW ->
                phase == DayNightPhase.NIGHT || phase == DayNightPhase.DUSK
            com.aetherbound.game.core.Aspect.HEROIC ->
                phase == DayNightPhase.MORNING || phase == DayNightPhase.NOON || phase == DayNightPhase.DAWN
            com.aetherbound.game.core.Aspect.COSMIC ->
                phase == DayNightPhase.NIGHT || phase == DayNightPhase.DUSK || phase == DayNightPhase.DAWN
            else -> true
        }
    }
}

/**
 * Compose state that recomposes every minute with the current ambient tint.
 * Use inside any composable that wants to paint a TimeOfDayOverlay:
 *
 *     val (color, alpha) by rememberAmbientTint()
 *     Box(Modifier.background(color.copy(alpha = alpha)))
 */
@Composable
fun rememberAmbientTint(): State<Pair<Color, Float>> {
    val state = remember { mutableStateOf(AmbientTime.tintNow()) }
    LaunchedEffect(Unit) {
        while (true) {
            state.value = AmbientTime.tintNow()
            // Re-evaluate every 30 seconds to pick up phase changes promptly.
            delay(30_000)
        }
    }
    return state
}

/**
 * Compose state that recomposes when the day/night phase changes.
 * Cheaper than [rememberAmbientTint] because it only recomposes on phase
 * boundaries (6 times per day).
 */
@Composable
fun rememberAmbientPhase(): State<DayNightPhase> {
    val state = remember { mutableStateOf(AmbientTime.phaseNow()) }
    LaunchedEffect(Unit) {
        while (true) {
            val next = AmbientTime.phaseNow()
            if (next != state.value) state.value = next
            delay(60_000)
        }
    }
    return state
}
