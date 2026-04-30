package com.aetherbound.game.render.sandbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.aetherbound.game.content.PilotEchoforms
import com.aetherbound.game.content.PilotTechniques
import com.aetherbound.game.core.EchoformDex
import com.aetherbound.game.core.PowerTier
import com.aetherbound.game.render.animation.AttackAnimationState
import com.aetherbound.game.render.animation.advance
import com.aetherbound.game.render.animation.casterOffset
import com.aetherbound.game.render.animation.defenderImpactOffset
import com.aetherbound.game.render.animation.drawHitFlash
import com.aetherbound.game.render.animation.drawProjectileHead
import com.aetherbound.game.render.asset.AssetSpecs
import com.aetherbound.game.render.asset.PngEchoform
import com.aetherbound.game.render.asset.assetExists
import com.aetherbound.game.render.battle.CinematicLayer
import com.aetherbound.game.render.particle.ParticleSystem
import com.aetherbound.game.render.theme.AetherColors
import com.aetherbound.game.render.theme.LocalQualityPreset

/**
 * Attack Sandbox: pick any two echoforms, trigger any attack, watch it play.
 * No HP, no damage, no battle logic — pure animation preview for tuning.
 *
 * Use to tune cinematic feel: does a Storm attack look as big as you want?
 * Does the Legendary tier slow-mo land at the right moment?
 */
@Composable
fun SandboxScene(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preset = LocalQualityPreset.current

    var playerOrdinal by remember { mutableLongStateOf(1L) }      // E001
    var opponentOrdinal by remember { mutableLongStateOf(2L) }    // E002
    var currentAttack by remember { mutableStateOf<AttackAnimationState?>(null) }
    var attackerIsPlayer by remember { mutableStateOf(true) }
    // Anchor positions are computed inside BoxWithConstraints (we need the
    // viewport size). Hoisted as state so the AttackChip onClick handler
    // (outside the BoxWithConstraints scope) can read the latest values.
    var playerAnchorState by remember { mutableStateOf(Offset.Zero) }
    var opponentAnchorState by remember { mutableStateOf(Offset.Zero) }
    val particleTextures = com.aetherbound.game.render.particle.rememberParticleTextures()
    val particles = remember(preset, particleTextures) { ParticleSystem(preset.maxParticles, particleTextures) }
    var frameTick by remember { mutableLongStateOf(0L) }

    val playerSpecies = EchoformDex.byId(EchoformDex.id(playerOrdinal.toInt()))
    val opponentSpecies = EchoformDex.byId(EchoformDex.id(opponentOrdinal.toInt()))

    // All available attacks in the pilot. Sorted by power so the tier
    // progression is obvious.
    val attacks = remember {
        listOf(
            PilotTechniques.VerdanceBind,
            PilotTechniques.EmberSnap,
            PilotTechniques.SparkHit,
            PilotTechniques.FrostPulse,
            PilotTechniques.TideSurge,
            PilotTechniques.EmberLance,
            PilotTechniques.SolarPyre,
            PilotTechniques.AetherCascade,
        ).sortedBy { it.power }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(AetherColors.Onyx, AetherColors.Obsidian, AetherColors.ObsidianDeep)
                )
            ),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = constraints.maxWidth.toFloat()
            val h = constraints.maxHeight.toFloat()
            val playerAnchor = Offset(w * 0.27f, h * 0.55f)
            val opponentAnchor = Offset(w * 0.73f, h * 0.35f)
            playerAnchorState = playerAnchor
            opponentAnchorState = opponentAnchor

            // Background — simple obsidian
            // (no full battle backdrop in sandbox to keep focus on the FX)

            // Sprites
            val playerOffset = currentAttack?.let { st ->
                if (attackerIsPlayer) st.casterOffset(facingRight = true) else st.defenderImpactOffset()
            } ?: Offset.Zero
            val opponentOffset = currentAttack?.let { st ->
                if (!attackerIsPlayer) st.casterOffset(facingRight = false) else st.defenderImpactOffset()
            } ?: Offset.Zero

            @Suppress("UNUSED_VARIABLE") val tickRead = frameTick

            SpriteAt(playerAnchor + playerOffset, sizeDp = 200.dp) {
                playerSpecies?.let {
                    val hasBack = assetExists(AssetSpecs.echoform(it.id, "back"))
                    PngEchoform(
                        speciesId = it.id,
                        pose = "idle",
                        modifier = Modifier.fillMaxSize(),
                        facingBack = hasBack,  // use real back if generated, else front
                    )
                }
            }
            SpriteAt(opponentAnchor + opponentOffset, sizeDp = 200.dp) {
                opponentSpecies?.let {
                    PngEchoform(
                        speciesId = it.id, pose = "idle",
                        modifier = Modifier.fillMaxSize(),
                        facingBack = false,
                    )
                }
            }

            // Particle + projectile + hit-flash overlay
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                particles.draw(this)
                currentAttack?.let { st ->
                    drawProjectileHead(st)
                    val anchor = if (attackerIsPlayer) opponentAnchor else playerAnchor
                    drawHitFlash(st, androidx.compose.ui.geometry.Size(w, h), anchor)
                }
            }

            // Cinematic overlay (letterbox + flash for Storm/Legendary)
            CinematicLayer(state = currentAttack, modifier = Modifier.fillMaxSize())
        }

        // Top bar: title + filter-toggle + exit
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Attack Sandbox",
                style = MaterialTheme.typography.titleLarge,
                color = AetherColors.GoldBright,
            )
            Spacer(Modifier.weight(1f))
            FilterToggle()
            Spacer(Modifier.size(8.dp))
            ExitChip(onExit)
        }

        // Echoform pickers
        Column(
            Modifier
                .align(Alignment.TopStart)
                .padding(top = 56.dp, start = 12.dp, end = 12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            EchoformPicker(
                label = "Player (back-view)",
                selectedOrdinal = playerOrdinal.toInt(),
                onPick = { playerOrdinal = it.toLong() },
            )
            EchoformPicker(
                label = "Opponent (front)",
                selectedOrdinal = opponentOrdinal.toInt(),
                onPick = { opponentOrdinal = it.toLong() },
            )
        }

        // Attack picker bottom
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "Tap an attack to play",
                style = MaterialTheme.typography.labelMedium,
                color = AetherColors.MutedText,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(attacks) { tech ->
                    AttackChip(
                        name = tech.name,
                        power = tech.power,
                        tier = PowerTier.forPower(tech.power),
                        onClick = {
                            val recipe = PilotTechniques.recipes[tech.id] ?: return@AttackChip
                            attackerIsPlayer = true
                            currentAttack = AttackAnimationState(
                                recipe = recipe,
                                origin = playerAnchorState,
                                target = opponentAnchorState,
                                particles = particles,
                                seed = System.nanoTime(),
                            )
                        },
                    )
                }
            }
        }
    }

    // Animation pump
    LaunchedEffect(currentAttack) {
        val st = currentAttack ?: return@LaunchedEffect
        var lastNs = 0L
        while (!st.finished) {
            withFrameNanos { now ->
                val dt = if (lastNs == 0L) 16f else ((now - lastNs) / 1_000_000f).coerceAtMost(50f)
                lastNs = now
                val timeScale = com.aetherbound.game.render.battle.cinematicTimeScale(st)
                st.advance(dt * timeScale)
                frameTick = now
            }
        }
        // post-hit pause for cinematic
        val pauseMs = st.recipe.cinematic.postHitPauseMs
        if (pauseMs > 0) kotlinx.coroutines.delay(pauseMs.toLong())
        currentAttack = null
    }
}

@Composable
private fun SpriteAt(anchorPx: Offset, sizeDp: androidx.compose.ui.unit.Dp, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(sizeDp)
            .graphicsLayer {
                translationX = anchorPx.x - this.size.width / 2f
                translationY = anchorPx.y - this.size.height / 2f
            },
    ) { content() }
}

@Composable
private fun EchoformPicker(
    label: String,
    selectedOrdinal: Int,
    onPick: (Int) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AetherColors.Onyx.copy(alpha = 0.78f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = AetherColors.GoldBright)
        Spacer(Modifier.size(8.dp))
        // Cycle buttons (-10, -1, current, +1, +10)
        StepBtn("◀◀") { onPick(((selectedOrdinal - 10 - 1 + 300) % 300) + 1) }
        Spacer(Modifier.size(4.dp))
        StepBtn("◀") { onPick(((selectedOrdinal - 1 - 1 + 300) % 300) + 1) }
        Spacer(Modifier.size(8.dp))
        Text(
            EchoformDex.id(selectedOrdinal),
            style = MaterialTheme.typography.titleMedium,
            color = AetherColors.ParchmentText,
        )
        Spacer(Modifier.size(8.dp))
        StepBtn("▶") { onPick((selectedOrdinal % 300) + 1) }
        Spacer(Modifier.size(4.dp))
        StepBtn("▶▶") { onPick(((selectedOrdinal + 10 - 1) % 300) + 1) }
    }
}

@Composable
private fun StepBtn(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AetherColors.Slate)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) { Text(label, color = AetherColors.GoldBright, style = MaterialTheme.typography.labelLarge) }
}

@Composable
private fun AttackChip(
    name: String,
    power: Int,
    tier: PowerTier,
    onClick: () -> Unit,
) {
    val (label, color) = when (tier) {
        PowerTier.Snap -> "SNAP" to Color(0xFF8FCBE1)
        PowerTier.Strike -> "STRIKE" to Color(0xFF59B66B)
        PowerTier.Pulse -> "PULSE" to Color(0xFFE8B460)
        PowerTier.Storm -> "STORM" to Color(0xFFFF8A3D)
        PowerTier.Legendary -> "LGD" to AetherColors.GoldHighlight
    }
    Column(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AetherColors.Slate.copy(alpha = 0.92f))
            .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(name, style = MaterialTheme.typography.labelLarge, color = AetherColors.ParchmentText)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(color.copy(alpha = 0.85f))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = AetherColors.Obsidian)
            }
            Spacer(Modifier.size(6.dp))
            Text("Pwr $power", style = MaterialTheme.typography.bodyMedium, color = AetherColors.MutedText)
        }
    }
}

@Composable
private fun FilterToggle() {
    val active = com.aetherbound.game.render.theme.SpriteFilter.matureMood
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (active) AetherColors.GoldDeep else AetherColors.Slate
            )
            .clickable {
                com.aetherbound.game.render.theme.SpriteFilter.matureMood = !active
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            if (active) "Filter: ON" else "Filter: OFF",
            color = if (active) AetherColors.Obsidian else AetherColors.MutedText,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun ExitChip(onExit: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AetherColors.WarningRed.copy(alpha = 0.6f))
            .clickable { onExit() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text("Exit", color = AetherColors.ParchmentText, style = MaterialTheme.typography.labelLarge)
    }
}
