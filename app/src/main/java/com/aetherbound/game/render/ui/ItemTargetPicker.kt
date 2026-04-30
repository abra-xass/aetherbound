package com.aetherbound.game.render.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.aetherbound.game.core.data.Party
import com.aetherbound.game.core.data.TuxemonItem
import com.aetherbound.game.render.theme.AetherColors

/**
 * Dim-overlay modal that prompts the player to pick which party member an
 * item should be applied to. Used after [BagScreen] taps a heal-style item.
 *
 * Tap on a member → [onPick]. Tap on the dim background → [onCancel].
 */
@Composable
fun ItemTargetPicker(
    item: TuxemonItem,
    party: Party,
    onPick: (memberIndex: Int) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable(onClick = onCancel),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(20.dp),
        ) {
            Text(
                text = "USE ${item.slug.replace('_', ' ').uppercase()}",
                color = AetherColors.GoldBright,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Pick a party member.",
                color = AetherColors.MutedText,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(12.dp))

            party.members.forEachIndexed { index, member ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AetherColors.Slate)
                        .border(1.dp, AetherColors.GoldCore, RoundedCornerShape(8.dp))
                        .clickable { onPick(index) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(AetherColors.Onyx),
                        ) {
                            EchoformSpriteImage(
                                slug = member.species.id,
                                variant = EchoformSpriteVariant.FRONT,
                                modifier = Modifier.matchParentSize(),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "${index + 1}. ${member.species.name}  Lv.${member.level}",
                                color = AetherColors.ParchmentText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "HP ${member.currentVigor}/${member.maxVigor}" +
                                    if (member.isFainted) "  · FAINTED" else "",
                                color = if (member.isFainted) AetherColors.WarningRed else AetherColors.MutedText,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tap outside to cancel.",
                color = AetherColors.MutedText,
                fontSize = 11.sp,
            )
        }
    }
}
