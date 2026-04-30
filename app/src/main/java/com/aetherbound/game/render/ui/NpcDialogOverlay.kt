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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherbound.game.render.theme.AetherColors
import kotlinx.coroutines.delay

/**
 * Pokémon-style NPC dialog overlay. Shows speaker name + portrait
 * placeholder + typewriter-animated text. Tap to either complete the
 * current line instantly (if still typing) or advance to the next line.
 *
 * @param speakerName name shown above the box (e.g. "Hiker Bert")
 * @param lines       list of text pages; tap advances through them
 * @param onClose     fires after the last page is dismissed
 * @param speakerSlug optional sprite-asset slug for portrait
 *                    (resolves via CharacterSprite naming convention)
 */
@Composable
fun NpcDialogOverlay(
    speakerName: String,
    lines: List<String>,
    onClose: () -> Unit,
    speakerSlug: String? = null,
    modifier: Modifier = Modifier,
) {
    var pageIndex by remember { mutableIntStateOf(0) }
    var typedChars by remember { mutableIntStateOf(0) }
    val currentPage = lines.getOrNull(pageIndex) ?: ""

    LaunchedEffect(pageIndex) {
        typedChars = 0
        for (i in 0..currentPage.length) {
            typedChars = i
            delay(28)
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(enabled = lines.isNotEmpty()) {
                if (typedChars < currentPage.length) {
                    typedChars = currentPage.length
                } else {
                    if (pageIndex >= lines.lastIndex) onClose() else pageIndex++
                }
            },
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 18.dp),
        ) {
            // Speaker header (name only — sprite optional)
            if (speakerName.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(AetherColors.GoldGradient)
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                    ) {
                        Text(
                            speakerName,
                            color = AetherColors.Obsidian,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            // Dialog box
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AetherColors.Obsidian.copy(alpha = 0.95f))
                    .border(2.dp, AetherColors.GoldCore, RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Column {
                    Text(
                        text = currentPage.take(typedChars),
                        color = AetherColors.ParchmentText,
                        fontSize = 14.sp,
                    )
                    if (typedChars >= currentPage.length) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (pageIndex >= lines.lastIndex) "▶ Tap to close" else "▶ Tap to continue",
                            color = AetherColors.GoldBright,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }
    }
}
