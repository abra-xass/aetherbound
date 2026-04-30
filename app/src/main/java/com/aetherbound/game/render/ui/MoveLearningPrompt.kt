package com.aetherbound.game.render.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherbound.game.core.EchoformInstance
import com.aetherbound.game.core.Technique
import com.aetherbound.game.render.theme.AetherColors
import com.aetherbound.game.render.theme.aspectColors

/**
 * Modal that fires when a level-up unlocks a new move and the
 * Echoform's 4 slots are full. Player picks which existing move to
 * forget, or skips the new move.
 *
 *   header     "Vulkid wants to learn Ember Lance!"
 *   newMove    [chip]   power 55 acc 95
 *
 *   "But it can't learn more than 4 moves.
 *    Forget one and make space?"
 *
 *   slot1 [Ember Snap]  pwr 40 acc 100
 *   slot2 [Spark Hit]   pwr 40 acc 100 prio +1
 *   slot3 [Tide Surge]  pwr 50 acc 95
 *   slot4 [Frost Pulse] pwr 45 acc 100
 *   [→ Tap to forget that one]
 *
 *   [Skip — don't learn it]
 */
@Composable
fun MoveLearningPrompt(
    instance: EchoformInstance,
    newMove: Technique,
    onForget: (slotIndex: Int) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
    ) {
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(20.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AetherColors.Obsidian)
                .border(2.dp, AetherColors.GoldBright, RoundedCornerShape(12.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "${instance.species.name} wants to learn",
                color = AetherColors.ParchmentText,
                fontSize = 14.sp,
            )
            MoveCard(newMove, highlight = true)

            Spacer(Modifier.height(4.dp))
            Text(
                text = "But it can only know 4 moves. Forget one?",
                color = AetherColors.MutedText,
                fontSize = 12.sp,
            )

            Spacer(Modifier.height(4.dp))
            instance.techniques.forEachIndexed { i, tech ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onForget(i) },
                ) { MoveCard(tech, highlight = false) }
            }

            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AetherColors.Slate)
                    .clickable { onSkip() }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Skip — don't learn ${newMove.name}",
                    color = AetherColors.MutedText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun MoveCard(tech: Technique, highlight: Boolean) {
    val (primary, _) = aspectColors(tech.aspect)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (highlight) primary.copy(alpha = 0.25f) else AetherColors.Slate)
            .border(
                width = if (highlight) 2.dp else 1.dp,
                color = if (highlight) AetherColors.GoldBright else primary.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(primary.copy(alpha = 0.95f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                text = tech.aspect.slug.uppercase(),
                color = AetherColors.Obsidian,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = tech.name,
                color = AetherColors.ParchmentText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "pwr ${tech.power} · acc ${tech.accuracy}% · prio ${tech.priority}",
                color = AetherColors.MutedText,
                fontSize = 10.sp,
            )
        }
    }
}
