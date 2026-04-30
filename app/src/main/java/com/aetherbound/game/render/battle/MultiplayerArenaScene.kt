package com.aetherbound.game.render.battle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherbound.game.content.PilotTechniques
import com.aetherbound.game.core.BattleAction
import com.aetherbound.game.core.BattleEvent
import com.aetherbound.game.core.BattleResolver
import com.aetherbound.game.core.BattleState
import com.aetherbound.game.core.EchoformInstance
import com.aetherbound.game.core.Side
import com.aetherbound.game.core.Technique
import com.aetherbound.game.core.data.BattleTimeouts
import com.aetherbound.game.core.data.MultiplayerRewards
import com.aetherbound.game.core.data.MultiplayerSnapshot
import com.aetherbound.game.render.animation.AnimationRecipe
import com.aetherbound.game.render.animation.AttackAnimationState
import com.aetherbound.game.render.animation.CasterMotion
import com.aetherbound.game.render.animation.ImpactVisual
import com.aetherbound.game.render.animation.ProjectilePath
import com.aetherbound.game.render.animation.ShaderEffect
import com.aetherbound.game.render.animation.TimingEnvelope
import com.aetherbound.game.render.animation.advance
import com.aetherbound.game.render.animation.casterOffset
import com.aetherbound.game.render.animation.defenderImpactOffset
import com.aetherbound.game.render.animation.drawHitFlash
import com.aetherbound.game.render.animation.drawProjectileHead
import com.aetherbound.game.render.particle.ParticleSystem
import com.aetherbound.game.render.particle.rememberParticleTextures
import com.aetherbound.game.render.shader.BloomLayer
import com.aetherbound.game.render.theme.AetherColors
import com.aetherbound.game.render.theme.LocalQualityPreset
import com.aetherbound.game.render.ui.EchoformSpriteImage
import com.aetherbound.game.render.ui.EchoformSpriteVariant
import com.aetherbound.game.render.ui.MultiplayerStatusIndicator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Live multiplayer-battle scene driven by:
 *
 *   - **Local input** — player taps a move-index; we route it to
 *     [onLocalAction] which the activity ships via the Matrix bridge.
 *   - **Remote input** — `peerActions` Flow emits the opponent's
 *     move-index when their `BATTLE_MOVE` Matrix-event arrives.
 *
 * Both clients run [BattleResolver.resolveTurn] with the SAME `rngSeed`
 * and the same paired actions, so output state is bit-identical.
 */
@Composable
fun MultiplayerArenaScene(
    initialPlayer: EchoformInstance,
    initialOpponent: EchoformInstance,
    rngSeed: Long,
    snapshot: MultiplayerSnapshot,
    selfDisplayName: String,
    peerDisplayName: String,
    selfWins: Int,
    selfLosses: Int,
    peerWins: Int,
    peerLosses: Int,
    mode: MultiplayerRewards.Mode,
    peerActions: Flow<PeerMove>,
    onLocalAction: (moveIdx: Int, stateHashAfter: String) -> Unit,
    onMatchEnd: (MatchEndResult) -> Unit,
    onConcede: () -> Unit,
    /**
     * Player's full party — used for mid-match switching. When the active
     * mon faints, the scene auto-swaps to the first non-fainted member.
     * When the user taps the Switch button, the [showSwitchPicker] state
     * opens an inline picker over the move-buttons.
     */
    party: com.aetherbound.game.core.data.Party = com.aetherbound.game.core.data.Party(
        members = listOf(initialPlayer)
    ),
    /** Fires when the player picks a new active member — index into [party.members]. */
    onSwitchTo: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var state by remember(rngSeed) {
        mutableStateOf(
            BattleState(
                player = initialPlayer,
                opponent = initialOpponent,
                turn = 1,
                rngSeed = rngSeed,
            )
        )
    }
    var statusLine by remember { mutableStateOf("Pick a move.") }
    var phase by remember { mutableStateOf(Phase.Choosing) }
    var playerFaints by remember { mutableIntStateOf(0) }
    var showSwitchPicker by remember { mutableStateOf(false) }
    var activeMemberIdx by remember { mutableIntStateOf(party.activeIndex) }

    // ── Full SP-recipe attack animation state ──────────────────────
    // Same renderer the SP BattleScene uses: caster motion + projectile
    // path + impact burst + bloom layer + cinematic letterbox/slow-mo for
    // Storm/Legendary tier. Recipes come from PilotTechniques.recipes
    // when the technique has a hand-tuned recipe, otherwise we derive a
    // sensible default from the technique's aspect + power.
    val preset = LocalQualityPreset.current
    val particleTextures = rememberParticleTextures()
    val particles = remember(preset, particleTextures) {
        ParticleSystem(preset.maxParticles, particleTextures)
    }
    var current by remember { mutableStateOf<AttackAnimationState?>(null) }
    var attackerSide by remember { mutableStateOf<Side?>(null) }
    var frameTick by remember { mutableLongStateOf(0L) }
    var playerAnchorState by remember { mutableStateOf(Offset.Zero) }
    var opponentAnchorState by remember { mutableStateOf(Offset.Zero) }

    /**
     * Recipe for [techId]. Returns the hand-tuned PilotTechniques recipe
     * if available; otherwise builds a generic LUNGE/BEAM/RING_POP recipe
     * from the technique itself, scaled by its power tier. This means
     * every Tuxemon-derived MP technique gets a real visual, not silence.
     */
    fun recipeFor(techId: String, tech: Technique?): AnimationRecipe? {
        PilotTechniques.recipes[techId]?.let { return it }
        if (tech == null) return null
        return AnimationRecipe(
            techniqueId = tech.id,
            techniqueName = tech.name,
            aspect = tech.aspect,
            casterMotion = CasterMotion.LUNGE,
            projectilePath = ProjectilePath.BEAM,
            impactVisual = ImpactVisual.RING_POP,
            shader = ShaderEffect.NONE,
            envelope = TimingEnvelope.SNAP,
            power = tech.power,
        )
    }

    /**
     * Run the recipe-driven attack animation for [attacker] and suspend
     * until it finishes (incl. cinematic post-hit pause). Mirrors the
     * frame-pump pattern from SP BattleScene's LaunchedEffect(current).
     */
    suspend fun playAttack(recipe: AnimationRecipe, attacker: Side) {
        val origin = if (attacker == Side.PLAYER) playerAnchorState else opponentAnchorState
        val target = if (attacker == Side.PLAYER) opponentAnchorState else playerAnchorState
        val seed = state.rngSeed xor state.turn.toLong()
        val st = AttackAnimationState(recipe, origin, target, particles, seed)
        attackerSide = attacker
        current = st
        var lastNs = 0L
        while (!st.finished) {
            withFrameNanos { now ->
                val dt = if (lastNs == 0L) 16f
                else ((now - lastNs) / 1_000_000f).coerceAtMost(50f)
                lastNs = now
                val timeScale = cinematicTimeScale(st)
                st.advance(dt * timeScale)
                frameTick = now
            }
        }
        val pause = st.recipe.cinematic.postHitPauseMs
        if (pause > 0) delay(pause.toLong())
        current = null
        attackerSide = null
    }

    /** Helper: replace state.player with party.members[idx], reset statuses. */
    fun swapToMember(newIdx: Int) {
        val newMember = party.members.getOrNull(newIdx) ?: return
        if (newMember.isFainted) return
        activeMemberIdx = newIdx
        state = state.copy(player = newMember, playerStatuses = emptyList())
        onSwitchTo(newIdx)
        statusLine = "${newMember.species.name} stepped into the ring."
    }

    // Channel for local move picks — the resolver loop suspends on receive().
    val pickGate = remember { Channel<Int>(Channel.CONFLATED) }

    // Resolver loop — keyed by rngSeed so a new match restarts cleanly.
    LaunchedEffect(rngSeed) {
        while (!state.isOver) {
            phase = Phase.Choosing
            statusLine = "Pick a move."
            val mine = pickGate.receive()
            // We don't yet know what state-hash will result — we resolve
            // first, then publish the post-resolution hash to the peer so
            // they can verify against their own. That mirrors how the
            // peer's hash is also "after their resolution", so the two
            // claims must be byte-equal on a deterministic resolver.
            statusLine = "Waiting for $peerDisplayName…"
            phase = Phase.Waiting
            val peer = withTimeoutOrNull(BattleTimeouts.Tor.moveTimeout) {
                peerActions.first()
            }
            if (peer == null) {
                statusLine = "$peerDisplayName timed out. Match forfeit."
                onMatchEnd(
                    MatchEndResult(
                        won = true,
                        isDraw = false,
                        potDelta = 0,    // forfeit doesn't pay pot — anti-grief
                        xpDelta = 0,
                        finalTurn = state.turn,
                        playerSweep = false,
                    )
                )
                return@LaunchedEffect
            }
            val resolved = BattleResolver.resolveTurn(
                state = state,
                playerAction = BattleAction.UseTechnique(mine),
                opponentAction = BattleAction.UseTechnique(peer.moveIdx),
            )

            // ── State-hash desync check ────────────────────────────────
            // Both peers ran the same resolver with the same seed + same
            // actions. If we disagree on the resulting state, someone
            // cheated or the resolver isn't truly deterministic. Abort
            // cleanly with no rewards in either direction.
            val ourHash = com.aetherbound.game.core.data.MatrixWireFormat.hashState(
                playerHp = resolved.player.currentVigor,
                opponentHp = resolved.opponent.currentVigor,
                turn = resolved.turn,
            )
            if (peer.stateHashAfter.isNotEmpty() && peer.stateHashAfter != ourHash) {
                statusLine = "Desync detected — match aborted."
                onMatchEnd(
                    MatchEndResult(
                        won = false,
                        isDraw = true,    // both sides treated as draw
                        potDelta = 0,
                        xpDelta = 0,
                        finalTurn = resolved.turn,
                        playerSweep = false,
                    )
                )
                return@LaunchedEffect
            }
            // Tell the activity to ship our move + post-resolution hash on the wire.
            onLocalAction(mine, ourHash)
            phase = Phase.Animating
            val tail = resolved.log.drop(state.log.size)
            // Play attack animations in chronological order — usually one
            // hit per side per turn (host first then guest, or vice versa).
            // Each recipe runs through the SP-quality renderer (caster motion +
            // projectile + impact burst + bloom + cinematic letterbox).
            tail.filterIsInstance<BattleEvent.TechniqueResolved>().forEach { ev ->
                statusLine = if (ev.missed) "${nameForSide(state, ev.side)} missed!"
                else "${nameForSide(state, ev.side)} dealt ${ev.damage}" +
                    (if (ev.crit) " CRIT" else "") +
                    (if (ev.stab) " STAB" else "")
                val tech = state.player.techniques.firstOrNull { it.id == ev.techniqueId }
                    ?: state.opponent.techniques.firstOrNull { it.id == ev.techniqueId }
                val recipe = recipeFor(ev.techniqueId, tech)
                if (recipe != null) playAttack(recipe, ev.side)
            }
            tail.filterIsInstance<BattleEvent.Faint>().forEach { f ->
                if (f.side == Side.PLAYER) playerFaints++
            }

            // Mid-match auto-switch: if our active fainted and the party
            // still has non-fainted members, hot-swap before next turn.
            // The swap clears statuses on the slot (Pokemon convention).
            if (resolved.player.isFainted) {
                // Update the party member to reflect its current HP/status before swapping.
                val nextAliveIdx = party.members.indexOfFirst { idx ->
                    idx == idx
                }.let {
                    party.members.indexOfFirst { it != resolved.player && !it.isFainted }
                }
                if (nextAliveIdx >= 0) {
                    val next = party.members[nextAliveIdx]
                    state = resolved.copy(player = next, playerStatuses = emptyList())
                    activeMemberIdx = nextAliveIdx
                    onSwitchTo(nextAliveIdx)
                    statusLine = "${resolved.player.species.name} fainted! ${next.species.name} steps in."
                    delay(900)
                    continue
                }
            }
            state = resolved
            delay(700)
        }
        // Match over — compute reward (activity supplies real wallet figures
        // via its onMatchEnd handler; here we send the structural deltas).
        val won = state.winner == Side.PLAYER
        val sweep = won && playerFaints == 0
        val rewards = MultiplayerRewards.compute(
            winnerMVP = if (won) state.player else state.opponent,
            loserActive = if (won) state.opponent else state.player,
            winnerMoney = 0,
            loserMoney = 0,
            mode = mode,
            matchTurns = state.turn - 1,
        )
        onMatchEnd(
            MatchEndResult(
                won = won,
                isDraw = state.winner == null,
                potDelta = if (won) rewards.winnerMoneyDelta else rewards.loserMoneyDelta,
                xpDelta = if (won) rewards.winnerXpToActive else 0,
                finalTurn = state.turn,
                playerSweep = sweep,
            )
        )
    }

    Box(modifier.fillMaxSize()) {
        AetherArenaBackdrop(Modifier.fillMaxSize())

        // Top banner
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BannerSlot(selfDisplayName, selfWins, selfLosses, AetherColors.GoldBright, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text("VS", color = AetherColors.GoldBright, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                BannerSlot(peerDisplayName, peerWins, peerLosses, AetherColors.GoldCore, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
                MultiplayerStatusIndicator()
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Center) {
                Text(
                    if (mode == MultiplayerRewards.Mode.RANKED)
                        "RANKED · pot ≥ \$${MultiplayerRewards.MIN_POT}"
                    else "CASUAL · sparring",
                    color = AetherColors.MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                )
            }
        }

        // Echoform sprites — opponent at top-right, player bottom-left.
        // Reuses the same PNG-loader the SP BattleScene + Party UI use.
        // Caster lunge + defender hit-bounce come from the recipe-driven
        // AttackAnimationState; sprite Boxes are positioned via graphicsLayer
        // translation in raw pixels for sub-pixel accuracy and so the
        // particle Canvas above (in raw pixels too) lines up exactly.
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wPx = constraints.maxWidth.toFloat()
            val hPx = constraints.maxHeight.toFloat()
            // Anchor = sprite center. SP uses the same sized-Box pattern.
            val playerAnchor = Offset(wPx * 0.20f, hPx * 0.62f)
            val opponentAnchor = Offset(wPx * 0.72f, hPx * 0.30f)
            playerAnchorState = playerAnchor
            opponentAnchorState = opponentAnchor

            // Subscribe this composition to per-frame state.
            @Suppress("UNUSED_VARIABLE") val tick = frameTick
            val playerOffset = current?.let { st ->
                if (attackerSide == Side.PLAYER) st.casterOffset(facingRight = true)
                else st.defenderImpactOffset()
            } ?: Offset.Zero
            val opponentOffset = current?.let { st ->
                if (attackerSide == Side.OPPONENT) st.casterOffset(facingRight = false)
                else st.defenderImpactOffset()
            } ?: Offset.Zero

            SpriteAt(anchorPx = playerAnchor + playerOffset, sizeDp = 200.dp) {
                BloomLayer(tint = Color(0x33F05A28), intensity = 0.5f) {
                    EchoformSpriteImage(
                        slug = state.player.species.id,
                        variant = EchoformSpriteVariant.BACK,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            SpriteAt(anchorPx = opponentAnchor + opponentOffset, sizeDp = 160.dp) {
                BloomLayer(tint = Color(0x332F9EEA), intensity = 0.5f) {
                    EchoformSpriteImage(
                        slug = state.opponent.species.id,
                        variant = EchoformSpriteVariant.FRONT,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // Particle + projectile + hit-flash overlay. Reading frameTick
            // forces a redraw every animation tick so particles animate.
            Canvas(Modifier.fillMaxSize()) {
                @Suppress("UNUSED_VARIABLE") val _t = frameTick
                particles.draw(this)
                current?.let { st ->
                    drawProjectileHead(st)
                    val anchor = if (attackerSide == Side.PLAYER) opponentAnchor else playerAnchor
                    drawHitFlash(st, Size(wPx, hPx), anchor)
                }
            }

            // Cinematic letterbox + full-screen flash for Storm/Legendary tier.
            CinematicLayer(state = current, modifier = Modifier.fillMaxSize())
        }

        // HP bars + status line
        Column(Modifier.align(Alignment.Center).fillMaxWidth().padding(20.dp)) {
            HpReadout(
                "${state.opponent.species.name} Lv.${state.opponent.level}",
                state.opponent.currentVigor, state.opponent.maxVigor, AetherColors.GoldCore,
            )
            Spacer(Modifier.height(40.dp))
            HpReadout(
                "${state.player.species.name} Lv.${state.player.level}",
                state.player.currentVigor, state.player.maxVigor, AetherColors.GoldBright,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Turn ${state.turn}  ·  $statusLine",
                color = AetherColors.ParchmentText, fontSize = 12.sp,
            )
        }

        // Bottom: move selector / waiting / animating overlay
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp)) {
            when {
                showSwitchPicker -> SwitchPicker(
                    party = party,
                    currentIdx = activeMemberIdx,
                    onPick = { idx ->
                        showSwitchPicker = false
                        swapToMember(idx)
                    },
                    onCancel = { showSwitchPicker = false },
                )
                phase == Phase.Choosing -> MoveButtons(
                    techniques = state.player.techniques,
                    enabled = !state.isOver,
                    onPick = { idx -> pickGate.trySend(idx) },
                )
                phase == Phase.Waiting -> InfoBox("Waiting for ${peerDisplayName}'s move…", AetherColors.GoldBright)
                phase == Phase.Animating -> InfoBox(statusLine, AetherColors.ParchmentText)
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.align(Alignment.End)) {
                if (party.members.size > 1) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AetherColors.Slate)
                            .clickable(enabled = phase == Phase.Choosing && !state.isOver) {
                                showSwitchPicker = true
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text("Switch", color = AetherColors.GoldBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AetherColors.Slate)
                        .clickable(onClick = onConcede)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text("Concede", color = AetherColors.WarningRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private enum class Phase { Choosing, Waiting, Animating }

@Composable
private fun MoveButtons(
    techniques: List<com.aetherbound.game.core.Technique>,
    enabled: Boolean,
    onPick: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        techniques.forEachIndexed { i, t ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AetherColors.Slate)
                    .clickable(enabled = enabled) { onPick(i) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    "${t.name}  ·  pwr ${t.power}  ·  acc ${t.accuracy}%",
                    color = if (enabled) AetherColors.GoldBright else AetherColors.MutedText,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SwitchPicker(
    party: com.aetherbound.game.core.data.Party,
    currentIdx: Int,
    onPick: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AetherColors.Obsidian.copy(alpha = 0.95f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Switch active", color = AetherColors.GoldBright, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        party.members.forEachIndexed { i, m ->
            val available = !m.isFainted && i != currentIdx
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (available) AetherColors.Slate else AetherColors.Slate.copy(alpha = 0.4f))
                    .clickable(enabled = available) { onPick(i) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    "${i + 1}. ${m.species.name}  Lv.${m.level}  HP ${m.currentVigor}/${m.maxVigor}" +
                        (if (i == currentIdx) "  · (active)" else "") +
                        (if (m.isFainted) "  · FAINTED" else ""),
                    color = if (available) AetherColors.ParchmentText else AetherColors.MutedText,
                    fontSize = 12.sp,
                )
            }
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(AetherColors.Slate)
                .clickable(onClick = onCancel)
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .align(Alignment.End),
        ) {
            Text("Cancel", color = AetherColors.MutedText, fontSize = 11.sp)
        }
    }
}

@Composable
private fun InfoBox(text: String, accent: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AetherColors.Obsidian)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = accent, fontSize = 13.sp)
    }
}

@Composable
private fun HpReadout(name: String, current: Int, max: Int, accent: Color) {
    val ratio = (current.toFloat() / max.coerceAtLeast(1)).coerceIn(0f, 1f)
    Column {
        Text(name, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(AetherColors.ObsidianDeep)
                .padding(1.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(ratio)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accent),
            )
        }
        Text("$current / $max", color = AetherColors.MutedText, fontSize = 10.sp)
    }
}

private fun nameForSide(state: BattleState, side: Side): String = when (side) {
    Side.PLAYER -> state.player.species.name
    Side.OPPONENT -> state.opponent.species.name
}

/**
 * One peer move as the arena consumes it. Includes the claimed state-hash
 * the peer says it computed AFTER applying its own action — we recompute
 * locally with the same deterministic resolver and abort on mismatch.
 */
data class PeerMove(val moveIdx: Int, val stateHashAfter: String)

/** Match-end snapshot delivered via [onMatchEnd]. Activity applies it. */
data class MatchEndResult(
    val won: Boolean,
    val isDraw: Boolean = false,
    val potDelta: Int,
    val xpDelta: Int,
    val finalTurn: Int,
    val playerSweep: Boolean,
)

@Composable
private fun SpriteAt(
    anchorPx: Offset,
    sizeDp: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(sizeDp)
            .graphicsLayer {
                translationX = anchorPx.x - this.size.width / 2f
                translationY = anchorPx.y - this.size.height / 2f
            },
    ) {
        content()
    }
}

@Composable
private fun BannerSlot(
    name: String,
    wins: Int,
    losses: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .background(AetherColors.Obsidian.copy(alpha = 0.78f))
            .padding(8.dp),
    ) {
        Text(name, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("$wins W / $losses L", color = AetherColors.MutedText, fontSize = 9.sp)
    }
}
