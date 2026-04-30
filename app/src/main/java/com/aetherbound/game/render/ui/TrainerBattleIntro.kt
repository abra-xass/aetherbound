package com.aetherbound.game.render.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherbound.game.core.data.TrainerSpec
import com.aetherbound.game.render.theme.AetherColors
import kotlinx.coroutines.delay

/**
 * Pre-battle trainer intro screen:
 *
 *   [trainer sprite slides in from right]
 *   [intro dialog typewrites in]
 *   [6 ball indicators light up one at a time]
 *   [tap to start battle]
 *
 * Replays the iconic Pokémon "Trainer wants to fight!" beat. Caller
 * dispatches the actual battle once [onStart] fires.
 */
@Composable
fun TrainerBattleIntro(
    trainer: TrainerSpec,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var slideIn by remember { mutableStateOf(false) }
    val slideOffset by animateFloatAsState(
        targetValue = if (slideIn) 0f else 1f,
        animationSpec = tween(durationMillis = 500, easing = LinearEasing),
        label = "trainer-slide",
    )

    var typedChars by remember { mutableStateOf(0) }
    var ballsLit by remember { mutableStateOf(0) }
    var canStart by remember { mutableStateOf(false) }

    LaunchedEffect(trainer.id) {
        slideIn = true
        delay(500)
        // Type the dialog character-by-character
        for (i in 0..trainer.intro.length) {
            typedChars = i
            delay(35)
        }
        // Light up balls one at a time
        for (i in 1..trainer.team.size.coerceAtMost(6)) {
            delay(180)
            ballsLit = i
        }
        delay(400)
        canStart = true
    }

    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1A0E2A),
                        AetherColors.Obsidian,
                        AetherColors.ObsidianDeep,
                    )
                )
            )
            .clickable(enabled = canStart, onClick = onStart),
    ) {
        // Diagonal slash line decor (Pokémon-style)
        Box(
            Modifier
                .fillMaxSize()
                .alpha(0.10f)
                .background(
                    Brush.linearGradient(
                        listOf(Color.Transparent, AetherColors.GoldCore, Color.Transparent)
                    )
                ),
        )

        // Trainer sprite slides in from right
        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AetherColors.Slate)
                    .border(2.dp, AetherColors.GoldBright, RoundedCornerShape(12.dp))
                    .alpha(1f - slideOffset),
            ) {
                Text(
                    text = trainer.displayName.first().uppercase(),
                    color = AetherColors.GoldBright,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = trainer.displayName,
                color = AetherColors.GoldBright,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(1f - slideOffset),
            )
        }

        // Intro dialog box bottom
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
                .fillMaxSize(0.65f),
            verticalArrangement = Arrangement.Bottom,
        ) {
            // Ball indicators row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in 0 until trainer.team.size.coerceAtMost(6)) {
                    BallIndicator(lit = i < ballsLit)
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(AetherColors.Obsidian.copy(alpha = 0.92f))
                    .border(1.dp, AetherColors.GoldCore, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    text = "${trainer.displayName} says:\n${trainer.intro.take(typedChars)}",
                    color = AetherColors.ParchmentText,
                    fontSize = 15.sp,
                )
            }
            if (canStart) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "▶ Tap to begin battle",
                    color = AetherColors.GoldBright,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun BallIndicator(lit: Boolean) {
    Box(
        Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(if (lit) AetherColors.GoldBright else AetherColors.Slate)
            .border(
                width = 1.dp,
                color = if (lit) AetherColors.GoldHighlight else AetherColors.SlateLight,
                shape = CircleShape,
            ),
    )
}
