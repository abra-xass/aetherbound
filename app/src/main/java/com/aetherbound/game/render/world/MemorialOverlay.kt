package com.aetherbound.game.render.world

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aetherbound.game.render.map.WorldEvent
import com.aetherbound.game.render.theme.AetherColors

/**
 * Full-screen overlay shown when the player walks onto a memorial tile.
 * Renders the image with a metallic-gold bordered frame, caption,
 * pull-quote, and attribution. Tap anywhere to dismiss.
 *
 * The image is loaded from `app/src/main/assets/<imageAsset>` via Coil's
 * `file:///android_asset/` scheme. The whole overlay fades + scales in
 * to set a respectful tone — not abrupt.
 *
 * Stylistically follows the rest of the game: Cinzel display, obsidian
 * backdrop with metallic-gold frame and quotes in Cormorant-italic.
 */
@Composable
fun MemorialOverlay(
    memorial: WorldEvent.Memorial?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = memorial != null,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut() + scaleOut(targetScale = 0.92f),
        modifier = modifier,
    ) {
        memorial ?: return@AnimatedVisibility
        val ctx = LocalContext.current
        Box(
            Modifier
                .fillMaxSize()
                .background(AetherColors.ObsidianDeep.copy(alpha = 0.94f))
                .clickable(onClick = onDismiss),    // tap anywhere = dismiss
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Caption — small, gold, atop the frame
                Text(
                    text = memorial.caption,
                    color = AetherColors.GoldBright,
                    fontSize = 14.sp,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))

                // The image — gold-bordered frame on obsidian backdrop
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AetherColors.Onyx)
                        .border(
                            width = 2.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    AetherColors.GoldBright,
                                    AetherColors.GoldDeep,
                                ),
                            ),
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(2.dp),
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(ctx)
                            .data("file:///android_asset/${memorial.imageAsset}")
                            .crossfade(true)
                            .build(),
                        contentDescription = memorial.caption,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp)),
                    )
                }

                Spacer(Modifier.height(28.dp))

                // The quote — italic, larger, in parchment ink
                Text(
                    text = "\"${memorial.quote}\"",
                    color = AetherColors.ParchmentText,
                    fontSize = 17.sp,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(10.dp))

                // Attribution — muted, smaller
                Text(
                    text = memorial.attribution,
                    color = AetherColors.GoldDeep,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(40.dp))

                // Dismiss hint
                Text(
                    text = "tippe, um zu schließen",
                    color = AetherColors.MutedText.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                )
            }
        }
    }
}
