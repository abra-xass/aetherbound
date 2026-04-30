package com.aetherbound.game.render.battle

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.aetherbound.game.ai.SimpleBattleAi
import com.aetherbound.game.content.PilotEchoforms
import com.aetherbound.game.content.PilotTechniques
import com.aetherbound.game.core.BattleAction
import com.aetherbound.game.core.BattleEvent
import com.aetherbound.game.core.BattleMenu
import com.aetherbound.game.core.BattleResolver
import com.aetherbound.game.core.BattleState
import com.aetherbound.game.core.CaptureMechanic
import com.aetherbound.game.core.EchoformInstance
import com.aetherbound.game.core.RunMechanic
import com.aetherbound.game.core.Side
import com.aetherbound.game.render.animation.AttackAnimationState
import com.aetherbound.game.render.animation.advance
import com.aetherbound.game.render.animation.casterOffset
import com.aetherbound.game.render.animation.defenderImpactOffset
import com.aetherbound.game.render.animation.drawHitFlash
import com.aetherbound.game.render.animation.drawProjectileHead
import com.aetherbound.game.render.asset.AssetSpecs
import com.aetherbound.game.render.asset.PngEchoform
import com.aetherbound.game.render.asset.assetExists
import com.aetherbound.game.render.particle.ParticleSystem
import com.aetherbound.game.render.shader.BloomLayer
import com.aetherbound.game.render.theme.AetherColors
import com.aetherbound.game.render.theme.LocalQualityPreset
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

private enum class BattlePhase { Choosing, Animating, Ended }
private enum class Submenu { Top, Fight }
private enum class EndKind { Victory, Defeat, Captured, Escaped }

@Composable
fun BattleScene(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    /** Optional wild-encounter species; null = uses the fixed Reeva story battle. */
    opponentSpeciesId: String? = null,
    /**
     * If non-null, the *player* Echoform is built from this Tuxemon slug via
     * [com.aetherbound.game.core.data.TuxemonBattleSetup] instead of the hand-tuned
     * [PilotEchoforms.playerStarter]. JSON-driven full pipeline.
     */
    tuxemonPlayerSlug: String? = null,
    /** Same as [tuxemonPlayerSlug] but for the opponent. */
    tuxemonOpponentSlug: String? = null,
    /** Levels for the Tuxemon-derived combatants (ignored when slugs are null). */
    tuxemonPlayerLevel: Int = 18,
    tuxemonOpponentLevel: Int = 16,
    /**
     * Fires once when the battle starts, with the opponent species slug.
     * Caller updates `PlayerProgress.seenSlugs` so the Bestiary tracks it.
     */
    onSpeciesSeen: (slug: String) -> Unit = {},
    /**
     * Fires when the player successfully captures the wild Echoform.
     * Caller appends the new instance to the Party (if room) or PcStorage,
     * and marks the slug caught in Bestiary.
     */
    onSpeciesCaptured: (slug: String, captured: com.aetherbound.game.core.EchoformInstance) -> Unit = { _, _ -> },
    /** Fires when the player wins (any non-capture victory). */
    onVictory: (loser: com.aetherbound.game.core.EchoformInstance, xpGained: Int) -> Unit = { _, _ -> },
    /** Fires when player taps "Switch" — caller opens PartyScreen as modal. */
    onSwitchRequest: () -> Unit = {},
    /** Fires when player taps "Bag" — caller opens BagScreen as modal. */
    onBagRequest: () -> Unit = {},
    /**
     * Source-of-truth override for the player Echoform. When non-null,
     * the scene replaces its internal state.player whenever this value
     * changes — this is how Switch and Bag-heal feed back into the
     * running battle. Behaviour:
     *
     *   - Same speciesId + same nickname → treat as HP/PP/status update
     *     (potion-heal mid-battle).
     *   - Different speciesId → treat as Switch: clear playerStatuses,
     *     reset attack-animation, return turn to Choosing phase.
     */
    playerOverride: com.aetherbound.game.core.EchoformInstance? = null,
) {
    val preset = LocalQualityPreset.current
    val ctx = androidx.compose.ui.platform.LocalContext.current

    var state by remember(opponentSpeciesId, tuxemonPlayerSlug, tuxemonOpponentSlug) {
        val tuxPlayer = tuxemonPlayerSlug?.let {
            com.aetherbound.game.core.data.TuxemonBattleSetup.build(ctx, it, tuxemonPlayerLevel)
        }
        val tuxOpponent = tuxemonOpponentSlug?.let {
            com.aetherbound.game.core.data.TuxemonBattleSetup.build(ctx, it, tuxemonOpponentLevel)
        }
        val opponent = tuxOpponent
            ?: opponentSpeciesId
                ?.let { id -> com.aetherbound.game.core.EchoformDex.byId(id) }
                ?.let { species -> PilotEchoforms.wildInstance(species) }
            ?: PilotEchoforms.opponent()
        mutableStateOf(
            BattleState(
                player = tuxPlayer ?: PilotEchoforms.playerStarter(),
                opponent = opponent,
                rngSeed = -0x5C84C2A361E0BFD7,
            )
        )
    }
    // Bestiary "seen" trigger — fires once per battle start.
    androidx.compose.runtime.LaunchedEffect(state.opponent.species.id) {
        onSpeciesSeen(state.opponent.species.id)
    }

    // Player-side state override: when the activity changes the active party
    // member or heals via Bag, propagate it into the running battle state.
    androidx.compose.runtime.LaunchedEffect(playerOverride) {
        val incoming = playerOverride ?: return@LaunchedEffect
        val current = state.player
        val isSameMon = incoming.species.id == current.species.id
        state = if (isSameMon) {
            // Heal / item / minor mutation — keep statuses + log + turn.
            state.copy(player = incoming)
        } else {
            // Real switch — Pokémon convention: switching clears status conditions
            // on the *outgoing* mon's slot. Statuses on the opponent stay.
            state.copy(
                player = incoming,
                playerStatuses = emptyList(),
            )
        }
    }

    // Battle SFX observer — every time the event log grows we scan the
    // recent tail for hits/faints and fire matching audio cues. This stays
    // out of the resolver (which is pure logic) and out of the choose/animate
    // branches (which already have plenty going on).
    val battleAudio = com.aetherbound.game.render.audio.LocalAudioEngine.current
    var lastLogSize by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(state.log.size) {
        if (state.log.size > lastLogSize) {
            for (i in lastLogSize until state.log.size) {
                when (val ev = state.log[i]) {
                    is com.aetherbound.game.core.BattleEvent.TechniqueResolved ->
                        battleAudio?.playSfx(com.aetherbound.game.render.audio.AudioCatalog.SFX_BATTLE_HIT, volume = 0.65f)
                    is com.aetherbound.game.core.BattleEvent.Faint ->
                        battleAudio?.playSfx(com.aetherbound.game.render.audio.AudioCatalog.SFX_FAINT, volume = 0.85f)
                    else -> Unit
                }
            }
            lastLogSize = state.log.size
        }
    }

    var phase by remember { mutableStateOf(BattlePhase.Choosing) }
    var submenu by remember { mutableStateOf(Submenu.Top) }
    var endKind by remember { mutableStateOf<EndKind?>(null) }
    var captureFlash by remember { mutableStateOf(0f) }
    var statusLine by remember { mutableStateOf("What will you do?") }
    val particleTextures = com.aetherbound.game.render.particle.rememberParticleTextures()
    val particles = remember(preset, particleTextures) { ParticleSystem(preset.maxParticles, particleTextures) }
    var current by remember { mutableStateOf<AttackAnimationState?>(null) }
    var attackerSide by remember { mutableStateOf<Side?>(null) }
    var queuedSecondTechId by remember { mutableStateOf<String?>(null) }
    var queuedSecondSide by remember { mutableStateOf<Side?>(null) }
    var playerAnchorState by remember { mutableStateOf(Offset.Zero) }
    var opponentAnchorState by remember { mutableStateOf(Offset.Zero) }
    // Frame tick: incremented every frame during attack animation. Anything
    // that depends on `current.elapsedMs` (sprite offsets, particles, projectile
    // head) reads frameTick to subscribe to per-frame recomposition.
    var frameTick by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    val aiRng = remember { Random(0x12345678ABCDEFL) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(AetherColors.Onyx, AetherColors.Obsidian, AetherColors.ObsidianDeep)
                )
            )
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = constraints.maxWidth.toFloat()
            val h = constraints.maxHeight.toFloat()
            val playerAnchor = Offset(w * 0.27f, h * 0.66f)
            val opponentAnchor = Offset(w * 0.73f, h * 0.40f)
            playerAnchorState = playerAnchor
            opponentAnchorState = opponentAnchor

            BattleArenaBackdrop(Modifier.fillMaxSize())

            // Compute per-frame offsets. Reading frameTick subscribes this
            // composition to per-frame updates while an attack is animating.
            @Suppress("UNUSED_VARIABLE")
            val tick = frameTick
            val playerOffset = current?.let { st ->
                if (attackerSide == Side.PLAYER) st.casterOffset(facingRight = true)
                else st.defenderImpactOffset()
            } ?: Offset.Zero
            val opponentOffset = current?.let { st ->
                if (attackerSide == Side.OPPONENT) st.casterOffset(facingRight = false)
                else st.defenderImpactOffset()
            } ?: Offset.Zero

            // Sprites positioned by graphicsLayer translation in raw pixels
            SpriteAt(
                anchorPx = playerAnchor + playerOffset,
                sizeDp = 220.dp,
            ) {
                BloomLayer(tint = Color(0x33F05A28), intensity = 0.5f) {
                    val playerPose = poseFor(state.player, current, isPlayer = true, attackerSide = attackerSide)
                    val playerHasPng = assetExists(AssetSpecs.echoformBase(state.player.species.id))
                    val playerVector = PilotEchoforms.visualMap[state.player.species.id]
                    when {
                        playerHasPng -> PngEchoform(
                            speciesId = state.player.species.id,
                            pose = playerPose.name.lowercase(),
                            modifier = Modifier.fillMaxSize(),
                        )
                        playerVector != null -> EchoformSprite(
                            visualId = playerVector,
                            pose = playerPose,
                            facingRight = true,
                            modifier = Modifier.fillMaxSize(),
                        )
                        // No PNG, no vector: render an obsidian silhouette so the
                        // battle still reads. This path only triggers if asset
                        // loading fails for an unmapped species.
                        else -> Unit
                    }
                }
            }
            SpriteAt(
                anchorPx = opponentAnchor + opponentOffset,
                sizeDp = 220.dp,
            ) {
                BloomLayer(tint = Color(0x332F9EEA), intensity = 0.5f) {
                    val opponentPose = poseFor(state.opponent, current, isPlayer = false, attackerSide = attackerSide)
                    val opponentHasPng = assetExists(AssetSpecs.echoformBase(state.opponent.species.id))
                    val opponentVector = PilotEchoforms.visualMap[state.opponent.species.id]
                    when {
                        opponentHasPng -> PngEchoform(
                            speciesId = state.opponent.species.id,
                            pose = opponentPose.name.lowercase(),
                            modifier = Modifier.fillMaxSize(),
                        )
                        opponentVector != null -> EchoformSprite(
                            visualId = opponentVector,
                            pose = opponentPose,
                            facingRight = false,
                            modifier = Modifier.fillMaxSize(),
                        )
                        else -> Unit
                    }
                }
            }

            // Particle + projectile + hit-flash overlay. Reads frameTick so
            // the canvas redraws every animation frame (otherwise particles
            // wouldn't appear — Compose only redraws on observed-state changes).
            Canvas(Modifier.fillMaxSize()) {
                @Suppress("UNUSED_VARIABLE") val _t = frameTick
                particles.draw(this)
                current?.let { st ->
                    drawProjectileHead(st)
                    val anchor = if (attackerSide == Side.PLAYER) opponentAnchor else playerAnchor
                    drawHitFlash(st, Size(w, h), anchor)
                }
            }

            // Cinematic overlay: letterbox bars + full-screen flash for Storm/Legendary
            CinematicLayer(state = current, modifier = Modifier.fillMaxSize())
        }

        // Top hud (opponent stats)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 12.dp, end = 12.dp),
        ) {
            HpBar(
                name = state.opponent.species.name,
                level = state.opponent.level,
                currentVigor = state.opponent.currentVigor,
                maxVigor = state.opponent.maxVigor,
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AetherColors.Onyx.copy(alpha = 0.75f)),
            )
        }

        // Bottom hud (player stats + controls)
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HpBar(
                name = state.player.species.name,
                level = state.player.level,
                currentVigor = state.player.currentVigor,
                maxVigor = state.player.maxVigor,
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AetherColors.Onyx.copy(alpha = 0.75f)),
            )
            Text(
                text = statusLine,
                style = MaterialTheme.typography.titleMedium,
                color = AetherColors.GoldBright,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            when (submenu) {
                Submenu.Top -> ActionMenu(
                    enabled = phase == BattlePhase.Choosing,
                    onPick = { menu ->
                        if (phase != BattlePhase.Choosing) return@ActionMenu
                        when (menu) {
                            BattleMenu.Fight -> {
                                submenu = Submenu.Fight
                                statusLine = "Choose a technique."
                            }
                            BattleMenu.Switch -> {
                                statusLine = "Pick a party member to send out."
                                onSwitchRequest()
                            }
                            BattleMenu.Bag -> {
                                statusLine = "Open the Bag…"
                                onBagRequest()
                            }
                            BattleMenu.Capture -> {
                                val chance = CaptureMechanic.successChance(state.opponent)
                                val roll = aiRng.nextDouble()
                                phase = BattlePhase.Animating
                                statusLine = "You hurl a Glass Prism…"
                                captureFlash = 1f
                                if (roll < chance) {
                                    endKind = EndKind.Captured
                                    statusLine = "${state.opponent.species.name} bound to the Prism!"
                                    phase = BattlePhase.Ended
                                    onSpeciesCaptured(state.opponent.species.id, state.opponent)
                                } else {
                                    statusLine = "${state.opponent.species.name} broke free!"
                                    // Capture failure forfeits the player's turn — opponent attacks
                                    val aiAction = SimpleBattleAi.pickAction(state, aiRng)
                                    val resolved = BattleResolver.resolveTurn(state, BattleAction.Wait, aiAction)
                                    val freshEvents = resolved.log.drop(state.log.size)
                                    state = resolved
                                    val declared = freshEvents.filterIsInstance<BattleEvent.TechniqueDeclared>().firstOrNull()
                                    if (declared != null) {
                                        attackerSide = declared.side
                                        val recipe = PilotTechniques.recipes[declared.techniqueId]
                                        if (recipe != null) {
                                            val origin = opponentAnchorState
                                            val target = playerAnchorState
                                            current = AttackAnimationState(
                                                recipe = recipe, origin = origin, target = target,
                                                particles = particles,
                                                seed = resolved.rngSeed xor resolved.turn.toLong(),
                                            )
                                        }
                                    } else {
                                        // No counterattack queued — return to menu
                                        phase = BattlePhase.Choosing
                                        submenu = Submenu.Top
                                        statusLine = "What will you do?"
                                    }
                                }
                            }
                            BattleMenu.Run -> {
                                val chance = RunMechanic.successChance(state.player, state.opponent)
                                val roll = aiRng.nextDouble()
                                if (roll < chance) {
                                    endKind = EndKind.Escaped
                                    statusLine = "Got away safely."
                                    phase = BattlePhase.Ended
                                } else {
                                    statusLine = "Couldn't escape!"
                                    phase = BattlePhase.Animating
                                    val aiAction = SimpleBattleAi.pickAction(state, aiRng)
                                    val resolved = BattleResolver.resolveTurn(state, BattleAction.Wait, aiAction)
                                    val freshEvents = resolved.log.drop(state.log.size)
                                    state = resolved
                                    val declared = freshEvents.filterIsInstance<BattleEvent.TechniqueDeclared>().firstOrNull()
                                    if (declared != null) {
                                        attackerSide = declared.side
                                        val recipe = PilotTechniques.recipes[declared.techniqueId]
                                        if (recipe != null) {
                                            current = AttackAnimationState(
                                                recipe = recipe,
                                                origin = opponentAnchorState,
                                                target = playerAnchorState,
                                                particles = particles,
                                                seed = resolved.rngSeed xor resolved.turn.toLong(),
                                            )
                                        }
                                    } else {
                                        phase = BattlePhase.Choosing
                                        submenu = Submenu.Top
                                    }
                                }
                            }
                        }
                    },
                )
                Submenu.Fight -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MoveSelector(
                        techniques = state.player.techniques,
                        enabled = phase == BattlePhase.Choosing,
                        target = state.opponent,
                        onPick = { idx ->
                            if (phase != BattlePhase.Choosing) return@MoveSelector
                            val playerAction = BattleAction.UseTechnique(idx)
                            val aiAction = SimpleBattleAi.pickAction(state, aiRng)
                            val resolved = BattleResolver.resolveTurn(state, playerAction, aiAction)
                            val freshEvents = resolved.log.drop(state.log.size)
                            state = resolved
                            phase = BattlePhase.Animating
                            submenu = Submenu.Top

                            val declared = freshEvents.filterIsInstance<BattleEvent.TechniqueDeclared>()
                            val first = declared.firstOrNull()
                            val second = declared.getOrNull(1)
                            if (first != null) {
                                statusLine = "${nameForSide(state, first.side)} uses ${first.techniqueName}!"
                                attackerSide = first.side
                                val recipe = PilotTechniques.recipes[first.techniqueId]
                                if (recipe != null) {
                                    val origin = if (first.side == Side.PLAYER) playerAnchorState else opponentAnchorState
                                    val target = if (first.side == Side.PLAYER) opponentAnchorState else playerAnchorState
                                    current = AttackAnimationState(
                                        recipe = recipe,
                                        origin = origin,
                                        target = target,
                                        particles = particles,
                                        seed = resolved.rngSeed xor resolved.turn.toLong(),
                                    )
                                }
                            }
                            queuedSecondTechId = second?.takeIf { !state.isOver }?.techniqueId
                            queuedSecondSide = second?.takeIf { !state.isOver }?.side
                        },
                    )
                    BackChip(
                        label = "← Back",
                        onBack = { submenu = Submenu.Top; statusLine = "What will you do?" },
                    )
                }
            }
        }

        if (phase == BattlePhase.Ended) {
            EndOverlay(endKind = endKind, winner = state.winner, onExit = onExit)
        }
    }

    // Animation pump driven off frame nanos
    LaunchedEffect(current) {
        val st = current ?: return@LaunchedEffect
        var lastNs = 0L
        while (!st.finished) {
            withFrameNanos { now ->
                val dt = if (lastNs == 0L) 16f else ((now - lastNs) / 1_000_000f).coerceAtMost(50f)
                lastNs = now
                // Slow-motion during Storm/Legendary cinematic windows
                val timeScale = cinematicTimeScale(st)
                st.advance(dt * timeScale)
                frameTick = now      // <- triggers Canvas + sprite-offset recomposition every frame
            }
        }
        // Cinematic post-hit pause (Storm = 240ms, Legendary = 600ms)
        val pauseMs = st.recipe.cinematic.postHitPauseMs
        if (pauseMs > 0) delay(pauseMs.toLong())
        // sequence: play queued second action
        if (queuedSecondTechId != null && !state.isOver) {
            val side = queuedSecondSide
            val recipe = PilotTechniques.recipes[queuedSecondTechId!!]
            queuedSecondTechId = null; queuedSecondSide = null
            if (side != null && recipe != null) {
                statusLine = "${nameForSide(state, side)} uses ${recipe.techniqueName}!"
                attackerSide = side
                delay(220)
                val origin = if (side == Side.PLAYER) playerAnchorState else opponentAnchorState
                val target = if (side == Side.PLAYER) opponentAnchorState else playerAnchorState
                current = AttackAnimationState(
                    recipe = recipe,
                    origin = origin,
                    target = target,
                    particles = particles,
                    seed = state.rngSeed xor (state.turn.toLong() + 7L),
                )
                return@LaunchedEffect
            }
        }
        // turn complete
        delay(160)
        if (state.isOver) {
            phase = BattlePhase.Ended
            endKind = if (state.winner == Side.PLAYER) EndKind.Victory else EndKind.Defeat
            statusLine = if (state.winner == Side.PLAYER) "Victory." else "Defeat."
            if (state.winner == Side.PLAYER) {
                val loser = state.opponent
                val baseStatSum = with(loser.species.baseStats) { vigor + force + focus + guard + ward + tempo }
                // Wild encounter (single-player). Trainer + Multiplayer override
                // the source-mode at the activity level via dedicated callbacks.
                val xp = com.aetherbound.game.core.data.ExperienceEngine.xpFromVictory(
                    winnerLevel = state.player.level,
                    loserLevel = loser.level,
                    loserBaseStatSum = baseStatSum,
                    sourceMode = com.aetherbound.game.core.data.ExperienceEngine.SourceMode.WILD,
                )
                onVictory(loser, xp)
            }
        } else {
            phase = BattlePhase.Choosing
            submenu = Submenu.Top
            statusLine = "What will you do?"
            current = null
            attackerSide = null
        }
    }
}

private fun nameForSide(state: BattleState, side: Side): String =
    if (side == Side.PLAYER) state.player.species.name else state.opponent.species.name

private fun poseFor(
    form: EchoformInstance,
    current: AttackAnimationState?,
    isPlayer: Boolean,
    attackerSide: Side?,
): SpritePose {
    if (form.isFainted) return SpritePose.Fainted
    if (current == null) return SpritePose.Idle
    val isAttacking = (isPlayer && attackerSide == Side.PLAYER) ||
        (!isPlayer && attackerSide == Side.OPPONENT)
    if (isAttacking && !current.hitFired) return SpritePose.Attacking
    if (!isAttacking && current.hitFired) return SpritePose.Hit
    return SpritePose.Idle
}

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
private fun BattleArenaBackdrop(modifier: Modifier) {
    // Per Visual Bible: battle backdrop is a Phase-2 production asset
    // (cities/<region>.png), NOT a Phase-1 style board. The style board is
    // an approval artifact only; using it here produces a 3-scene concept
    // sheet instead of a single arena. Fall through to vector if the
    // production city PNG is not yet generated.
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val arena = AssetSpecs.city("namaris_harbor")
    if (com.aetherbound.game.render.asset.AssetCache.exists(ctx, arena)) {
        Box(modifier) {
            coil.compose.AsyncImage(
                model = coil.request.ImageRequest.Builder(ctx)
                    .data("file:///android_asset/${arena.path}")
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // dark vignette so sprites pop
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                AetherColors.ObsidianDeep.copy(alpha = 0.10f),
                                AetherColors.ObsidianDeep.copy(alpha = 0.70f),
                            ),
                        )
                    ),
            )
        }
        return
    }
    // Fallback: vector backdrop (no PNG yet)
    BattleArenaBackdropVector(modifier)
}

@Composable
private fun BattleArenaBackdropVector(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "arena")
    val drift by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift",
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        // back wall ambient
        drawRect(
            brush = Brush.radialGradient(
                listOf(Color(0xFF1A2233), AetherColors.Obsidian),
                center = Offset(w * 0.5f, h * 0.30f),
                radius = w * 0.7f,
            ),
            size = Size(w, h * 0.55f),
        )
        // floor disc
        drawOval(
            brush = Brush.verticalGradient(
                listOf(AetherColors.Slate.copy(alpha = 0.9f), AetherColors.Onyx),
                startY = h * 0.48f, endY = h,
            ),
            topLeft = Offset(-w * 0.1f, h * 0.55f),
            size = Size(w * 1.2f, h * 0.55f),
        )
        // gold ring
        drawOval(
            color = AetherColors.GoldDeep.copy(alpha = 0.5f),
            topLeft = Offset(w * 0.10f, h * 0.66f),
            size = Size(w * 0.80f, h * 0.18f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(3f),
        )
        // distant gold motes
        repeat(12) { i ->
            val px = ((i * 73 + (drift * 320).toInt()) % w.toInt().coerceAtLeast(1)).toFloat()
            val py = h * 0.20f + sin((i + drift * 6f).toDouble()).toFloat() * 12f
            drawCircle(
                color = AetherColors.GoldBright.copy(alpha = 0.18f),
                radius = 2f,
                center = Offset(px, py),
            )
        }
    }
}

@Composable
private fun EndOverlay(endKind: EndKind?, winner: Side?, onExit: () -> Unit) {
    val k = endKind ?: when (winner) { Side.PLAYER -> EndKind.Victory; Side.OPPONENT -> EndKind.Defeat; null -> EndKind.Defeat }
    val (title, subtitle, color) = when (k) {
        EndKind.Victory -> Triple("VICTORY", "Tempo holds.", AetherColors.GoldBright)
        EndKind.Defeat -> Triple("DEFEAT", "The Echo fades.", AetherColors.WarningRed)
        EndKind.Captured -> Triple("BOUND", "The Prism keeps it.", AetherColors.GoldHighlight)
        EndKind.Escaped -> Triple("ESCAPED", "You slip away.", AetherColors.GoldCore)
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(AetherColors.ObsidianDeep.copy(alpha = 0.88f))
            .clickable { onExit() },
    ) {
        Column(
            Modifier.align(Alignment.Center).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.displayLarge, color = color)
            Spacer(Modifier.size(8.dp))
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = AetherColors.ParchmentText)
            Spacer(Modifier.size(16.dp))
            Text("tap to return", style = MaterialTheme.typography.labelMedium, color = AetherColors.MutedText)
        }
    }
}
