package com.aetherbound.game.render.world

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherbound.game.core.data.Gender
import com.aetherbound.game.render.theme.AetherColors

/**
 * Character-creation entry screen — first thing the player sees on a fresh
 * boot (when [com.aetherbound.game.core.data.PlayerProgress.playerName] is
 * blank). They pick a name + an optional gender. NPCs read this for
 * reactive dialogue (`#playerName#` slot).
 *
 * Stylistically matches the rest of the title — Cinzel display font on
 * a deep obsidian backdrop with metallic-gold accents and a breathing
 * gold-shimmer on the title (mirrors [com.aetherbound.game.render.shader.LivingEyeSplash]).
 *
 * Tone: a single cryptic Don-Juan line beneath the title — sets the
 * Aetherbound voice from the very first interaction.
 *
 * @param onConfirm called with the chosen [name] and [gender] when the
 *        player taps "Beginnen"
 * @param onSkip optional escape hatch — defaults `name="Reisender"` and
 *        `gender=NEUTRAL` so users can blast through. Useful for
 *        repeated test-builds.
 */
@Composable
fun NameInputScreen(
    onConfirm: (name: String, gender: Gender) -> Unit,
    onSkip: () -> Unit = { onConfirm("Reisender", Gender.NEUTRAL) },
    modifier: Modifier = Modifier,
) {
    var typedName by remember { mutableStateOf("") }
    var chosenGender by remember { mutableStateOf<Gender?>(null) }

    // Subtle breathing on the title gold — same motif as the splash.
    val breathing = rememberInfiniteTransition(label = "title-breath")
    val glow by breathing.animateFloat(
        initialValue = 0.7f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(AetherColors.ObsidianDeep, AetherColors.Obsidian, AetherColors.Onyx),
                ),
            ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // ── Title ─────────────────────────────────────────────────
            Text(
                text = "WER BIST DU?",
                color = AetherColors.GoldBright.copy(alpha = glow),
                fontSize = 36.sp,
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))

            // ── Don Juan opener ───────────────────────────────────────
            Text(
                text = "\"Bevor du entscheidest, was du willst,\nfrag dich: Hat dieser Pfad ein Herz?\"",
                color = AetherColors.GoldDeep,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "— don juan, durch carlos castaneda",
                color = AetherColors.MutedText,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(48.dp))

            // ── Name input ────────────────────────────────────────────
            Text(
                "DEIN NAME",
                color = AetherColors.GoldCore,
                fontSize = 11.sp,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AetherColors.Onyx.copy(alpha = 0.85f))
                    .border(1.dp, AetherColors.GoldDeep.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                BasicTextField(
                    value = typedName,
                    onValueChange = { v -> typedName = v.take(20) },    // cap at 20 chars
                    singleLine = true,
                    cursorBrush = SolidColor(AetherColors.GoldBright),
                    textStyle = TextStyle(
                        color = AetherColors.GoldBright,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                            if (typedName.isEmpty()) {
                                Text(
                                    "(hier tippen)",
                                    color = AetherColors.MutedText,
                                    fontSize = 14.sp,
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(36.dp))

            // ── Gender row ────────────────────────────────────────────
            Text(
                "ICH BIN…",
                color = AetherColors.GoldCore,
                fontSize = 11.sp,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (g in Gender.values()) {
                    GenderChip(
                        label = g.displayDe,
                        selected = chosenGender == g,
                        onTap = { chosenGender = g },
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            // ── Action row ────────────────────────────────────────────
            val canConfirm = typedName.trim().length in 2..20 && chosenGender != null
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(AetherColors.Slate.copy(alpha = 0.6f))
                        .clickable(onClick = onSkip)
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                ) {
                    Text("Überspringen", color = AetherColors.MutedText, fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (canConfirm) AetherColors.GoldDeep
                            else AetherColors.Slate.copy(alpha = 0.4f),
                        )
                        .clickable(enabled = canConfirm) {
                            onConfirm(typedName.trim(), chosenGender ?: Gender.NEUTRAL)
                        }
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                ) {
                    Text(
                        "Beginnen",
                        color = if (canConfirm) AetherColors.Onyx else AetherColors.MutedText,
                        fontSize = 13.sp,
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Footer voice line — Watts wink ────────────────────────
            Text(
                text = "\"Du jagst Echoforms, aber das Universum jagt sich nur selbst. Auch dich.\"",
                color = AetherColors.MutedText.copy(alpha = 0.7f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "— alan watts, paraphrasiert",
                color = AetherColors.MutedText.copy(alpha = 0.5f),
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun GenderChip(
    label: String,
    selected: Boolean,
    onTap: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) AetherColors.GoldDeep
                else AetherColors.Onyx.copy(alpha = 0.7f),
            )
            .border(
                1.dp,
                if (selected) AetherColors.GoldBright else AetherColors.GoldDeep.copy(alpha = 0.4f),
                RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            color = if (selected) AetherColors.Onyx else AetherColors.GoldCore,
            fontSize = 12.sp,
        )
    }
}
