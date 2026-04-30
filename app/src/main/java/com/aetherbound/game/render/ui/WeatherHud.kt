package com.aetherbound.game.render.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherbound.game.core.WeekdayLore
import com.aetherbound.game.core.data.AmbientTime
import com.aetherbound.game.core.data.DayNightPhase
import com.aetherbound.game.core.data.rememberAmbientPhase
import com.aetherbound.game.render.theme.AetherColors

/**
 * Compact HUD widget showing current weekday + day-phase + weather.
 * Sits at the top-right of [com.aetherbound.game.render.world.TuxemonWorldScene].
 *
 * Layout (~100 dp wide):
 *
 *   ┌────────────────────────┐
 *   │ Mi · 14:23 · ☀ klar   │
 *   │ #weekdayAspect#-Tag    │
 *   └────────────────────────┘
 *
 * Helps the player plan: "Heute Mittwoch + klar → suche Cosmic im Sanctum."
 *
 * Reads [AmbientTime.snapshotNow] every minute via [rememberAmbientPhase]
 * — cheap, recomposes only on phase boundaries.
 */
@Composable
fun WeatherHud(modifier: Modifier = Modifier) {
    // The phase-tick triggers recomposition; we snapshot the full state below.
    val phase by rememberAmbientPhase()
    val snapshot = AmbientTime.snapshotNow()

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AetherColors.Obsidian.copy(alpha = 0.85f))
            .border(1.dp, AetherColors.GoldDeep.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                WeekdayLore.displayDe(snapshot.weekday).take(2),
                color = AetherColors.GoldBright,
                fontSize = 11.sp,
            )
            Text(
                phaseShort(phase),
                color = AetherColors.GoldCore,
                fontSize = 11.sp,
            )
            Text(
                "${snapshot.weather.emoji} ${snapshot.weather.displayDe}",
                color = AetherColors.ParchmentText,
                fontSize = 11.sp,
            )
        }
        // Sub-line: which aspect is biased today (passive hint to player)
        val biasAspect = WeekdayLore.biasedAspect(snapshot.weekday)
        Text(
            "${WeekdayLore.displayDeAspect(biasAspect)}-Tag",
            color = AetherColors.MutedText,
            fontSize = 9.sp,
        )
    }
}

private fun phaseShort(p: DayNightPhase): String = when (p) {
    DayNightPhase.DAWN    -> "Morgen-D."
    DayNightPhase.MORNING -> "Morgen"
    DayNightPhase.NOON    -> "Mittag"
    DayNightPhase.EVENING -> "Abend"
    DayNightPhase.DUSK    -> "Abend-D."
    DayNightPhase.NIGHT   -> "Nacht"
}
