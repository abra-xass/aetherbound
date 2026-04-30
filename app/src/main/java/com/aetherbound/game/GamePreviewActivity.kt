package com.aetherbound.game

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aetherbound.game.perf.PerformanceOverlay
import com.aetherbound.game.render.asset.AssetCache
import com.aetherbound.game.render.asset.AssetSpecs
import com.aetherbound.game.render.battle.BattleScene
import com.aetherbound.game.render.theme.AetherColors
import com.aetherbound.game.render.theme.AetherboundTheme
import com.aetherbound.game.render.theme.QualityPreset
import com.aetherbound.game.render.world.WorldScene

/**
 * Standalone debug Activity that hosts the Aetherbound pilot.
 * Launched via the debug-only `<activity-alias>` defined in
 * `app/src/debug/AndroidManifest.xml`.
 *
 * Three scenes, internally routed: Title → World → Battle → World.
 */
class GamePreviewActivity : ComponentActivity() {
    /**
     * Incoming intent state — observed by the Compose tree. Set in
     * [onCreate] from the launching intent, refreshed in [onNewIntent]
     * when Thot launches us while we're already running. The composable
     * reads it via [androidx.compose.runtime.mutableStateOf] reference
     * passed through the local-composition.
     */
    private val incoming = mutableStateOf<com.aetherbound.game.core.data.IncomingIntent>(
        com.aetherbound.game.core.data.IncomingIntent.None
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incoming.value = com.aetherbound.game.core.data.IncomingIntent.from(intent)
        setContent {
            var quality by remember { mutableStateOf(QualityPreset.Balanced) }
            AetherboundTheme(quality = quality) {
                GamePreviewRoot(
                    quality = quality,
                    onCycleQuality = {
                        quality = when (quality) {
                            QualityPreset.Quality -> QualityPreset.Balanced
                            QualityPreset.Balanced -> QualityPreset.Battery
                            QualityPreset.Battery -> QualityPreset.Quality
                        }
                    },
                    onExit = { finish() },
                    incomingIntent = incoming.value,
                    consumeIntent = {
                        incoming.value = com.aetherbound.game.core.data.IncomingIntent.None
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        incoming.value = com.aetherbound.game.core.data.IncomingIntent.from(intent)
    }
}

private enum class Scene {
    Title, World, TuxemonWorld, Battle, TrainerBattle, Sandbox,
    Menu, Party, Bag, SaveMenu, LoadMenu,
    Detail, Bestiary, Settings, StatusCard, ItemTarget,
    AssetPacks, Updates, MoveLearning, PcStorage,
    MultiplayerLobby, MultiplayerArena,
}

/** Pending move-learning event from a level-up. Queue may hold several
 *  if multiple level-ups happened (e.g. exp share boost). */
private data class MoveLearnEvent(
    val partyIndex: Int,
    val newMove: com.aetherbound.game.core.Technique,
)

@Composable
private fun GamePreviewRoot(
    quality: QualityPreset,
    onCycleQuality: () -> Unit,
    onExit: () -> Unit,
    incomingIntent: com.aetherbound.game.core.data.IncomingIntent =
        com.aetherbound.game.core.data.IncomingIntent.None,
    consumeIntent: () -> Unit = {},
) {
    var scene by remember { mutableStateOf(Scene.Title) }
    var sceneBeforeMenu by remember { mutableStateOf(Scene.Title) }
    var encounterSpeciesId by remember { mutableStateOf<String?>(null) }
    var tuxemonWildSlug by remember { mutableStateOf<String?>(null) }
    var tuxemonWildLevel by remember { mutableStateOf(5) }

    // Persisted player state — shared between Party/Bag/Save screens.
    var party by remember {
        mutableStateOf(
            com.aetherbound.game.core.data.Party(
                members = listOf(
                    com.aetherbound.game.content.PilotEchoforms.playerStarter(),
                ),
                activeIndex = 0,
            )
        )
    }
    var inventory by remember {
        mutableStateOf(
            com.aetherbound.game.core.data.Inventory(
                stacks = mapOf("potion" to 5, "tuxeball" to 8, "super_potion" to 2),
                money = 1500,
            )
        )
    }
    val ctx = LocalContext.current
    var saveSlotMessage by remember { mutableStateOf("") }
    var progress by remember {
        mutableStateOf(
            com.aetherbound.game.core.data.PlayerProgress(
                playerName = "Aether",
                seenSlugs = setOf("agnidon", "rockitten", "nudimind"),
                caughtSlugs = setOf("agnidon"),
            )
        )
    }
    var detailIndex by remember { mutableStateOf(0) }
    var bagSelectedItem by remember { mutableStateOf<com.aetherbound.game.core.data.TuxemonItem?>(null) }
    var cameFromBattle by remember { mutableStateOf(false) }
    var moveLearnQueue by remember { mutableStateOf<List<MoveLearnEvent>>(emptyList()) }
    var pcStorage by remember { mutableStateOf(com.aetherbound.game.core.data.PcStorage()) }

    // Thot ForegroundService bind + heartbeat — keeps Tor connection alive
    // while a multiplayer match is running. Auto-released on dispose.
    val thotKeeper = remember { com.aetherbound.game.core.data.ThotConnectionKeeper(ctx) }
    var showBatteryPrompt by remember { mutableStateOf(false) }
    androidx.compose.runtime.DisposableEffect(thotKeeper) {
        onDispose { thotKeeper.stop() }
    }

    // ── Multiplayer match state ────────────────────────────────────
    var mpRoomId by remember { mutableStateOf("") }
    var mpPeerMatrixId by remember { mutableStateOf("") }
    var mpInviteEventId by remember { mutableStateOf("") }
    var mpRngSeed by remember { mutableStateOf(0L) }
    var mpMode by remember { mutableStateOf(com.aetherbound.game.core.data.MultiplayerRewards.Mode.RANKED) }
    var mpLevelCap by remember { mutableStateOf(50) }
    var mpSnapshot by remember { mutableStateOf<com.aetherbound.game.core.data.MultiplayerSnapshot?>(null) }
    var mpIAmReady by remember { mutableStateOf(false) }
    var mpPeerReady by remember { mutableStateOf(false) }
    // Channel that the bridge feeds with peer move-indices when the
    // BATTLE_MOVE Matrix event lands. ArenaScene's resolver-loop suspends on it.
    val mpPeerActionsChannel = remember {
        kotlinx.coroutines.channels.Channel<com.aetherbound.game.render.battle.PeerMove>(
            kotlinx.coroutines.channels.Channel.CONFLATED,
        )
    }
    val mpPeerActionsFlow = remember(mpPeerActionsChannel) {
        kotlinx.coroutines.flow.flow {
            for (a in mpPeerActionsChannel) emit(a)
        }
    }

    // Cross-app relay: Thot broadcasts decoded battle.* events here; we
    // funnel battle.move into the per-turn channel that ArenaScene reads.
    androidx.compose.runtime.LaunchedEffect(scene) {
        if (scene != Scene.MultiplayerArena && scene != Scene.MultiplayerLobby) return@LaunchedEffect
        com.aetherbound.game.core.data.BattleRelayChannel.events.collect { ev ->
            when (ev.type) {
                com.aetherbound.game.core.data.MatrixWireFormat.EventType.BATTLE_MOVE -> {
                    val moveIdx = ev.body.optInt("moveIdx", 0)
                    val claimedHash = ev.body.optString("stateHash", "")
                    mpPeerActionsChannel.trySend(
                        com.aetherbound.game.render.battle.PeerMove(moveIdx, claimedHash)
                    )
                }
                com.aetherbound.game.core.data.MatrixWireFormat.EventType.BATTLE_INVITE_REPLY -> {
                    if (ev.body.optBoolean("accepted", false)) mpPeerReady = true
                }
                com.aetherbound.game.core.data.MatrixWireFormat.EventType.BATTLE_END -> {
                    saveSlotMessage = "Opponent ended the match: ${ev.body.optString("winner")}"
                }
                else -> { /* trade / capability / spectate ignored in arena */ }
            }
        }
    }

    // If we were killed mid-match, surface a banner offering to roll back
    // the player to their pre-match party state. The match itself can't
    // resume cleanly (peer's state is gone) so we just clear gracefully.
    var pendingResume by remember { mutableStateOf<com.aetherbound.game.core.data.ActiveMatchPersist.Snapshot?>(null) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        pendingResume = com.aetherbound.game.core.data.ActiveMatchPersist.load(ctx)
    }

    var pendingStoryBeat by remember { mutableStateOf<com.aetherbound.game.core.data.StoryBeats.Beat?>(null) }
    fun fireBeat(beat: com.aetherbound.game.core.data.StoryBeats.Beat) {
        if (!progress.hasFlag(beat.flag)) pendingStoryBeat = beat
    }
    var currentMapPath by remember { mutableStateOf("game/maps/tuxemon/spyder_shores.tmx") }
    var spawnTileX by remember { mutableStateOf(8) }
    var spawnTileY by remember { mutableStateOf(8) }

    // ── Continuous auto-save — never lose progress ──────────────────────
    //
    // Reacts to ANY change in the critical save state and writes atomically
    // to the auto-slot 500ms after the last change. Debouncing avoids
    // hammering disk during compound state mutations (e.g. capture →
    // bestiary update → inventory deduct → party add all fire in one tick).
    //
    // Coverage:
    //   - capture, faint, level-up, evolution, item pickup, item use
    //   - money change (battle pot, trainer reward, shop)
    //   - map warp, healing pad, sign read
    //   - bestiary seen/caught marks, badges earned
    //   - multiplayer match results (W/L, streak, pot)
    //   - PC storage deposit/withdraw/release
    //
    // This sits ON TOP of the explicit pre/post-battle saves below — both
    // run independently. Worst-case data loss: 500ms of activity (single
    // tick of compose state). Practically zero.
    androidx.compose.runtime.LaunchedEffect(
        party, inventory, progress, pcStorage, currentMapPath, spawnTileX, spawnTileY,
    ) {
        kotlinx.coroutines.delay(500)
        com.aetherbound.game.core.data.SaveGameIO.saveAuto(
            ctx,
            com.aetherbound.game.core.data.SaveGame(
                playerName = progress.playerName,
                currentMap = currentMapPath,
                playerTileX = spawnTileX,
                playerTileY = spawnTileY,
                party = party,
                pcStorage = pcStorage,
                inventory = inventory,
            ),
        )
    }

    // Auto-save checkpoint helper for explicit pre/post-battle commits
    // (synchronous, no 500ms debounce — used at boundaries that must
    // commit immediately for anti-exploit reasons).
    fun autoSaveCheckpoint() {
        com.aetherbound.game.core.data.SaveGameIO.saveAuto(
            ctx,
            com.aetherbound.game.core.data.SaveGame(
                playerName = progress.playerName,
                currentMap = currentMapPath,
                playerTileX = spawnTileX,
                playerTileY = spawnTileY,
                party = party,
                pcStorage = pcStorage,
                inventory = inventory,
            ),
        )
    }

    // Pre/post-battle explicit checkpoint — fires on every transition into
    // and out of a battle scene. Anti-exploit: post-battle save closes the
    // window where a force-stop could undo a loss.
    var lastScene by remember { mutableStateOf(scene) }
    androidx.compose.runtime.LaunchedEffect(scene) {
        val isBattleScene: (Scene) -> Boolean = { s ->
            s == Scene.Battle || s == Scene.TrainerBattle ||
                s == Scene.MultiplayerLobby || s == Scene.MultiplayerArena
        }
        val entering = isBattleScene(scene) && !isBattleScene(lastScene)
        val leaving = !isBattleScene(scene) && isBattleScene(lastScene)
        if (entering || leaving) autoSaveCheckpoint()
        lastScene = scene
    }

    // Bind to Thot's ForegroundService whenever a multiplayer scene is up.
    // Heartbeat every 15s while bound. DisposableEffect releases on scene leave.
    androidx.compose.runtime.LaunchedEffect(scene) {
        val isMp = scene == Scene.MultiplayerLobby || scene == Scene.MultiplayerArena
        if (isMp) {
            thotKeeper.start()
            // Battery-optim prompt — fire only once per save (flag-tracked).
            if (!progress.hasFlag("mp_battery_prompt_seen") &&
                !com.aetherbound.game.render.ui.isBatteryOptimisationOk(ctx)) {
                showBatteryPrompt = true
            }
            // Heartbeat loop.
            while (scene == Scene.MultiplayerLobby || scene == Scene.MultiplayerArena) {
                thotKeeper.heartbeat()
                kotlinx.coroutines.delay(15_000)
            }
            thotKeeper.stop()
        } else {
            thotKeeper.stop()
        }
    }

    // Route incoming Thot intents (BATTLE_INVITE, TRADE_OFFER, OPEN_TITLE) into scenes.
    androidx.compose.runtime.LaunchedEffect(incomingIntent) {
        when (val ii = incomingIntent) {
            is com.aetherbound.game.core.data.IncomingIntent.BattleInvite -> {
                mpRoomId = ii.roomId
                mpPeerMatrixId = ii.peerMatrixId
                mpInviteEventId = ii.inviteEventId
                // Pre-match snapshot — restored verbatim post-match.
                mpSnapshot = com.aetherbound.game.core.data.MultiplayerSnapshot.capture(
                    party = party,
                    worldMapPath = currentMapPath,
                    worldTileX = spawnTileX,
                    worldTileY = spawnTileY,
                )
                // Both peers derive the same seed from the invite event-id.
                mpRngSeed = ii.inviteEventId.hashCode().toLong()
                scene = Scene.MultiplayerLobby
                consumeIntent()
            }
            is com.aetherbound.game.core.data.IncomingIntent.TradeOffer -> {
                saveSlotMessage = "Trade offer from ${ii.peerMatrixId} (TBD)"
                consumeIntent()
            }
            com.aetherbound.game.core.data.IncomingIntent.OpenTitle -> {
                scene = Scene.Title
                consumeIntent()
            }
            com.aetherbound.game.core.data.IncomingIntent.None -> Unit
        }
    }

    // Hoisted audio engine — single instance per activity, auto-released on dispose.
    val audio = com.aetherbound.game.render.audio.rememberAudioEngine()

    // Sync engine settings with PlayerProgress whenever audio/controls change.
    androidx.compose.runtime.LaunchedEffect(progress.audio, progress.controls) {
        audio.settings = progress.audio
        com.aetherbound.game.core.data.AmbientTime.forcedPhase = progress.controls.forcedPhase
    }

    // BGM follows scene + biome + day/night phase.
    val ambientPhase by com.aetherbound.game.core.data.rememberAmbientPhase()

    // Trainer-intro flag: when set, Scene.Battle plays the intro overlay first.
    var pendingTrainer by remember { mutableStateOf<com.aetherbound.game.core.data.TrainerSpec?>(null) }
    var trainerIntroDone by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(scene, ambientPhase) {
        when (scene) {
            Scene.TuxemonWorld -> audio.playMusic(
                com.aetherbound.game.render.audio.AudioCatalog.bgmFor(
                    com.aetherbound.game.core.ScreenTheme.Beach, ambientPhase,
                )
            )
            Scene.Battle -> audio.playMusic(
                com.aetherbound.game.render.audio.AudioCatalog.bgmForBattle(
                    isLegendary = false, isTrainer = false,
                )
            )
            Scene.Title, Scene.Menu, Scene.Party, Scene.Bag,
            Scene.SaveMenu, Scene.LoadMenu -> {
                // Keep music playing in menus over the world
            }
            else -> Unit
        }
    }

    // Hardware-back handling: if a sub-screen is open, route back through the
    // scene stack instead of finishing the activity. Toggles the in-game menu
    // when the player is in the world (matches Pokémon-style START behaviour).
    androidx.activity.compose.BackHandler(enabled = true) {
        when (scene) {
            Scene.Party, Scene.Bag, Scene.Bestiary, Scene.Settings,
            Scene.StatusCard, Scene.SaveMenu, Scene.LoadMenu,
            Scene.AssetPacks, Scene.Updates, Scene.PcStorage -> scene = Scene.Menu
            Scene.Detail -> scene = Scene.Party
            Scene.ItemTarget -> { bagSelectedItem = null; scene = Scene.Bag }
            Scene.Menu -> scene = sceneBeforeMenu
            Scene.TuxemonWorld -> if (progress.controls.backOpensMenu) {
                sceneBeforeMenu = Scene.TuxemonWorld
                scene = Scene.Menu
            } else {
                scene = Scene.Title
            }
            Scene.World -> scene = Scene.Title
            Scene.Sandbox -> scene = Scene.Title
            Scene.Battle, Scene.TrainerBattle, Scene.MultiplayerArena,
            Scene.MoveLearning -> { /* modal — user must pick an option, no back */ }
            Scene.MultiplayerLobby -> {
                mpIAmReady = false
                mpPeerReady = false
                mpSnapshot = null
                scene = Scene.TuxemonWorld
            }
            Scene.Title -> onExit()
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        com.aetherbound.game.render.audio.LocalAudioEngine provides audio,
    ) {
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = scene,
            transitionSpec = {
                fadeIn(tween(380)) togetherWith fadeOut(tween(280))
            },
            label = "scene",
        ) { current ->
            when (current) {
                Scene.Title -> TitleScreen(
                    onStart = { scene = Scene.TuxemonWorld },
                    onSandbox = { scene = Scene.Sandbox },
                    onExit = onExit,
                )
                Scene.World -> WorldScene(
                    onEncounter = { speciesId ->
                        encounterSpeciesId = speciesId
                        scene = Scene.Battle
                    },
                    onExit = { scene = Scene.Title },
                )
                Scene.TuxemonWorld -> {
                    // Install pilot trainers once + fire intro beat on first entry.
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        com.aetherbound.game.core.data.TrainerRegistry.Pilot.installAll()
                        fireBeat(com.aetherbound.game.core.data.StoryBeats.INTRO)
                    }
                    com.aetherbound.game.render.world.TuxemonWorldScene(
                        tmxAssetPath = currentMapPath,
                        biome = com.aetherbound.game.core.ScreenTheme.Beach,
                        spawnTileX = spawnTileX,
                        spawnTileY = spawnTileY,
                        collectedFlags = progress.collectedFlags,
                        onExit = { scene = Scene.Title },
                        onEncounter = { wild ->
                            tuxemonWildSlug = wild.species.id
                            tuxemonWildLevel = wild.level
                            scene = Scene.Battle
                        },
                        onTrainerEncounter = { trainerId ->
                            val spec = com.aetherbound.game.core.data.TrainerRegistry.get(trainerId)
                                ?: com.aetherbound.game.core.data.TrainerRegistry.Pilot.ROUTE1_HIKER
                            pendingTrainer = spec
                            scene = Scene.TrainerBattle
                        },
                        onMenu = {
                            sceneBeforeMenu = Scene.TuxemonWorld
                            scene = Scene.Menu
                        },
                        onWarp = { destMap, dx, dy ->
                            // If the path is relative ("foo.tmx"), keep the
                            // tuxemon prefix so it resolves under assets/.
                            val resolved = when {
                                destMap.startsWith("game/maps/") -> destMap
                                destMap.endsWith(".tmx") -> "game/maps/tuxemon/$destMap"
                                else -> "game/maps/tuxemon/$destMap.tmx"
                            }
                            currentMapPath = resolved
                            spawnTileX = dx
                            spawnTileY = dy
                            saveSlotMessage = "Entered ${destMap.substringAfterLast('/').removeSuffix(".tmx")}"
                        },
                        onHealRequest = {
                            // Restore HP + clear faint flag on every party member.
                            val healed = party.members.map { it.copy(currentVigor = it.maxVigor) }
                            party = party.copy(members = healed)
                            saveSlotMessage = "Party fully restored."
                        },
                        onItemPickup = { itemSlug, flagId ->
                            inventory = inventory.add(itemSlug, 1)
                            progress = progress.setFlag(flagId)
                            saveSlotMessage = "Picked up ${itemSlug.replace('_', ' ')}."
                            true
                        },
                    )
                }
                Scene.TrainerBattle -> {
                    val spec = pendingTrainer
                    if (spec == null) scene = Scene.TuxemonWorld
                    else com.aetherbound.game.render.battle.TrainerBattleHost(
                        trainer = spec,
                        onExit = {
                            progress = progress.earnBadge(spec.id)
                            inventory = inventory.earn(spec.moneyReward)
                            saveSlotMessage = "Defeated ${spec.displayName}! +$${spec.moneyReward}"
                            // Story beats — fire once per save.
                            fireBeat(com.aetherbound.game.core.data.StoryBeats.FIRST_TRAINER)
                            if (spec.id == com.aetherbound.game.core.data.TrainerRegistry.Pilot.GYM_LEADER.id) {
                                fireBeat(com.aetherbound.game.core.data.StoryBeats.GYM_LEADER)
                            }
                            pendingTrainer = null
                            scene = Scene.TuxemonWorld
                        },
                    )
                }
                Scene.Battle -> {
                    val tuxSlug = tuxemonWildSlug
                    BattleScene(
                        onExit = {
                            tuxemonWildSlug = null
                            // After battle ends, surface any queued move-learn prompts.
                            scene = when {
                                moveLearnQueue.isNotEmpty() -> Scene.MoveLearning
                                sceneBeforeMenu == Scene.TuxemonWorld -> Scene.TuxemonWorld
                                else -> Scene.World
                            }
                        },
                        opponentSpeciesId = if (tuxSlug == null) encounterSpeciesId else null,
                        tuxemonOpponentSlug = tuxSlug,
                        tuxemonOpponentLevel = tuxemonWildLevel,
                        tuxemonPlayerSlug = if (tuxSlug != null) "agnidon" else null,
                        tuxemonPlayerLevel = if (tuxSlug != null) 8 else 18,
                        // Live-sync to current active party member: switch / heal flow back here.
                        playerOverride = party.active,
                        onSpeciesSeen = { slug -> progress = progress.see(slug) },
                        onSpeciesCaptured = { slug, instance ->
                            progress = progress.capture(slug)
                            if (!party.isFull) {
                                party = (party.add(instance) as? com.aetherbound.game.core.data.Party.AddResult.Added)
                                    ?.party ?: party
                                saveSlotMessage = "${instance.species.name} caught!"
                            } else {
                                // Party full → deposit to PC.
                                pcStorage = pcStorage.deposit(instance)
                                saveSlotMessage = "${instance.species.name} sent to PC."
                            }
                            fireBeat(com.aetherbound.game.core.data.StoryBeats.FIRST_CAPTURE)
                        },
                        onSwitchRequest = {
                            cameFromBattle = true
                            scene = Scene.Party
                        },
                        onBagRequest = {
                            cameFromBattle = true
                            scene = Scene.Bag
                        },
                        onVictory = { loser, xp ->
                            // XP applied to active party member; level-up triggers stat refresh
                            // and may unlock new moves via the species moveset table.
                            val active = party.active
                            val activeIdx = party.activeIndex
                            if (active != null) {
                                val curve = com.aetherbound.game.core.data.ExperienceCurve.MEDIUM_FAST
                                val baseLevelXp = curve.xpForLevel(active.level)
                                // Build the move table for level-range detection.
                                val movesetByLevel: List<Pair<Int, com.aetherbound.game.core.Technique>> =
                                    com.aetherbound.game.core.data.TuxemonEchoformDex
                                        .levelMovesetMap(ctx, active.species.id)
                                val result = com.aetherbound.game.core.data.ExperienceEngine.addXp(
                                    instance = active,
                                    currentTotalXp = baseLevelXp,
                                    xpDelta = xp,
                                    curve = curve,
                                    movesetByLevel = movesetByLevel,
                                )
                                if (result.didLevelUp) {
                                    val newLevel = result.newLevel
                                    val updated = active.copy(level = newLevel)
                                    party = party.replace(activeIdx, updated)
                                    saveSlotMessage = "${active.species.name} grew to Lv.$newLevel!"
                                    // Distribute newly-learned moves: auto-append if a
                                    // slot is free, otherwise queue the prompt to let
                                    // the player choose which old move to forget.
                                    if (result.newlyLearned.isNotEmpty()) {
                                        var current = updated
                                        val toPrompt = mutableListOf<MoveLearnEvent>()
                                        for (newMove in result.newlyLearned) {
                                            if (current.techniques.size < 4) {
                                                current = current.copy(techniques = current.techniques + newMove)
                                                saveSlotMessage = "${current.species.name} learned ${newMove.name}!"
                                            } else {
                                                toPrompt += MoveLearnEvent(activeIdx, newMove)
                                            }
                                        }
                                        if (current !== updated) {
                                            party = party.replace(activeIdx, current)
                                        }
                                        if (toPrompt.isNotEmpty()) moveLearnQueue = moveLearnQueue + toPrompt
                                    }
                                }
                            }
                        },
                    )
                }
                Scene.Sandbox -> com.aetherbound.game.render.sandbox.SandboxScene(
                    onExit = { scene = Scene.Title },
                )
                Scene.Menu -> MenuOverlay(
                    progress = progress,
                    onParty = { scene = Scene.Party },
                    onBag = { scene = Scene.Bag },
                    onBestiary = { scene = Scene.Bestiary },
                    onStatus = { scene = Scene.StatusCard },
                    onSettings = { scene = Scene.Settings },
                    onPcStorage = { scene = Scene.PcStorage },
                    onAssetPacks = { scene = Scene.AssetPacks },
                    onUpdates = { scene = Scene.Updates },
                    onSave = { scene = Scene.SaveMenu },
                    onLoad = { scene = Scene.LoadMenu },
                    onClose = { scene = sceneBeforeMenu },
                    saveMessage = saveSlotMessage,
                )
                Scene.Party -> com.aetherbound.game.render.ui.PartyScreen(
                    party = party,
                    onBack = {
                        if (cameFromBattle) { cameFromBattle = false; scene = Scene.Battle }
                        else scene = Scene.Menu
                    },
                    onSwitch = { idx ->
                        party = party.switch(idx)
                        if (cameFromBattle) { cameFromBattle = false; scene = Scene.Battle }
                    },
                    onDetail = { idx -> detailIndex = idx; scene = Scene.Detail },
                )
                Scene.Bag -> com.aetherbound.game.render.ui.BagScreen(
                    inventory = inventory,
                    onBack = {
                        if (cameFromBattle) { cameFromBattle = false; scene = Scene.Battle }
                        else scene = Scene.Menu
                    },
                    onUseItem = { item ->
                        bagSelectedItem = item
                        scene = Scene.ItemTarget
                    },
                )
                Scene.ItemTarget -> {
                    val pickedItem = bagSelectedItem
                    if (pickedItem == null) {
                        scene = Scene.Bag
                    } else {
                        com.aetherbound.game.render.ui.ItemTargetPicker(
                            item = pickedItem,
                            party = party,
                            onPick = { idx ->
                                val target = party.members.getOrNull(idx)
                                if (target != null) {
                                    val itemCtx = com.aetherbound.game.core.data.ItemContext(
                                        kind = com.aetherbound.game.core.data.ItemContextKind.World,
                                        targetFainted = target.isFainted,
                                    )
                                    val results = com.aetherbound.game.core.data.ItemEngine.use(
                                        item = pickedItem, target = target, ctx = itemCtx,
                                    )
                                    val healed = results.filterIsInstance<com.aetherbound.game.core.data.ItemResult.HpRestored>().sumOf { it.amount }
                                    if (healed > 0) {
                                        val healed2 = (target.currentVigor + healed).coerceAtMost(target.maxVigor)
                                        party = party.replace(idx, target.copy(currentVigor = healed2))
                                    }
                                    inventory.remove(pickedItem.slug)?.let { inventory = it }
                                }
                                bagSelectedItem = null
                                scene = Scene.Bag
                            },
                            onCancel = {
                                bagSelectedItem = null
                                scene = Scene.Bag
                            },
                        )
                    }
                }
                Scene.Detail -> {
                    val mon = party.members.getOrNull(detailIndex)
                    if (mon == null) scene = Scene.Party
                    else com.aetherbound.game.render.ui.EchoformDetailScreen(
                        instance = mon,
                        onBack = { scene = Scene.Party },
                    )
                }
                Scene.Bestiary -> com.aetherbound.game.render.ui.BestiaryScreen(
                    progress = progress,
                    onBack = { scene = Scene.Menu },
                )
                Scene.Settings -> com.aetherbound.game.render.ui.SettingsScreen(
                    progress = progress,
                    onChange = { progress = it },
                    onBack = { scene = Scene.Menu },
                )
                Scene.StatusCard -> com.aetherbound.game.render.ui.PlayerStatusCard(
                    progress = progress,
                    party = party,
                    inventory = inventory,
                    onBack = { scene = Scene.Menu },
                )
                Scene.PcStorage -> com.aetherbound.game.render.ui.PcStorageScreen(
                    storage = pcStorage,
                    party = party,
                    onBack = { scene = Scene.Menu },
                    onWithdraw = { boxIdx, slotIdx ->
                        val box = pcStorage.boxes.getOrNull(boxIdx) ?: return@PcStorageScreen
                        val mon = box.slots.getOrNull(slotIdx) ?: return@PcStorageScreen
                        if (party.isFull) return@PcStorageScreen
                        val newSlots = box.slots.toMutableList().also { it.removeAt(slotIdx) }
                        val newBoxes = pcStorage.boxes.toMutableList().also { it[boxIdx] = box.copy(slots = newSlots) }
                        pcStorage = pcStorage.copy(boxes = newBoxes)
                        party = (party.add(mon) as? com.aetherbound.game.core.data.Party.AddResult.Added)?.party ?: party
                        saveSlotMessage = "Withdrew ${mon.species.name}."
                    },
                    onRelease = { boxIdx, slotIdx ->
                        val box = pcStorage.boxes.getOrNull(boxIdx) ?: return@PcStorageScreen
                        val mon = box.slots.getOrNull(slotIdx) ?: return@PcStorageScreen
                        val newSlots = box.slots.toMutableList().also { it.removeAt(slotIdx) }
                        val newBoxes = pcStorage.boxes.toMutableList().also { it[boxIdx] = box.copy(slots = newSlots) }
                        pcStorage = pcStorage.copy(boxes = newBoxes)
                        saveSlotMessage = "${mon.species.name} released into the wild."
                    },
                )
                Scene.MultiplayerLobby -> {
                    com.aetherbound.game.render.battle.MultiplayerLobbyScene(
                        selfDisplayName = progress.playerName,
                        selfStats = progress.multiplayer,
                        selfMoney = inventory.money,
                        peerDisplayName = mpPeerMatrixId.substringBefore(":").removePrefix("@")
                            .ifEmpty { "Opponent" },
                        peerStats = null,    // peer Matrix-state-event reveal pending
                        peerMoney = null,
                        iAmReady = mpIAmReady,
                        peerReady = mpPeerReady,
                        onReady = { m, cap ->
                            mpMode = m
                            mpLevelCap = cap
                            mpIAmReady = true
                            // Persist the active-match snapshot so a kill mid-match
                            // can be detected on next launch and the player at least
                            // gets their pre-match party restored.
                            mpSnapshot?.let { snap ->
                                com.aetherbound.game.core.data.ActiveMatchPersist.save(
                                    ctx,
                                    com.aetherbound.game.core.data.ActiveMatchPersist.Snapshot(
                                        roomId = mpRoomId,
                                        peerMatrixId = mpPeerMatrixId,
                                        rngSeed = mpRngSeed,
                                        mode = m.name,
                                        turn = 0,
                                        matchSnapshot = snap,
                                    ),
                                )
                            }
                            scene = Scene.MultiplayerArena
                        },
                        onDecline = {
                            mpIAmReady = false
                            mpPeerReady = false
                            mpSnapshot = null
                            scene = Scene.TuxemonWorld
                        },
                    )
                }
                Scene.MultiplayerArena -> {
                    val snap = mpSnapshot
                    val player = party.active
                    if (snap == null || player == null) {
                        scene = Scene.TuxemonWorld
                    } else {
                        // Build a placeholder opponent — real wire would receive
                        // it via BATTLE_START event payload.
                        val opponent = com.aetherbound.game.core.data.TuxemonBattleSetup
                            .build(ctx, "rockitten", player.level)
                            ?: player
                        com.aetherbound.game.render.battle.MultiplayerArenaScene(
                            initialPlayer = player,
                            initialOpponent = opponent,
                            rngSeed = mpRngSeed,
                            snapshot = snap,
                            selfDisplayName = progress.playerName,
                            peerDisplayName = mpPeerMatrixId.substringBefore(":").removePrefix("@")
                                .ifEmpty { "Opponent" },
                            selfWins = progress.multiplayer.wins,
                            selfLosses = progress.multiplayer.losses,
                            peerWins = 0, peerLosses = 0,
                            mode = mpMode,
                            peerActions = mpPeerActionsFlow,
                            onLocalAction = { idx, stateHashAfter ->
                                // Ship our move + post-resolution state-hash to Thot,
                                // which posts it as an io.aether.battle.move Matrix
                                // event into the room. The peer's Aetherbound picks
                                // it up via BattleRelayChannel.
                                com.aetherbound.game.core.data.AetherSendBridge.sendBattleMove(
                                    ctx = ctx,
                                    roomId = mpRoomId,
                                    turn = 0,
                                    moveIdx = idx,
                                    stateHash = stateHashAfter,
                                )
                            },
                            onMatchEnd = { result ->
                                // Apply rewards. Use real wallet figures here.
                                if (result.won && mpMode == com.aetherbound.game.core.data.MultiplayerRewards.Mode.RANKED) {
                                    val pot = com.aetherbound.game.core.data.MultiplayerRewards.computePotAnte(
                                        loserMoney = inventory.money,    // opponent-money proxy for MVP
                                    )
                                    inventory = inventory.copy(money = inventory.money + pot)
                                    val record = com.aetherbound.game.core.data.MatchRecord(
                                        opponentDisplayName = mpPeerMatrixId.substringBefore(":").removePrefix("@"),
                                        opponentMatrixId = mpPeerMatrixId,
                                        won = true,
                                        finalTurn = result.finalTurn,
                                        playerSweep = result.playerSweep,
                                    )
                                    progress = progress.copy(
                                        multiplayer = progress.multiplayer.recordMatch(record, potDelta = pot),
                                    )
                                    saveSlotMessage = "Victory! +\$$pot"
                                } else if (!result.won && mpMode == com.aetherbound.game.core.data.MultiplayerRewards.Mode.RANKED) {
                                    val pot = com.aetherbound.game.core.data.MultiplayerRewards.computePotAnte(inventory.money)
                                    val (newBalance, _) = com.aetherbound.game.core.data.MultiplayerRewards
                                        .applyLoss(inventory.money, pot)
                                    inventory = inventory.copy(money = newBalance)
                                    val record = com.aetherbound.game.core.data.MatchRecord(
                                        opponentDisplayName = mpPeerMatrixId.substringBefore(":").removePrefix("@"),
                                        opponentMatrixId = mpPeerMatrixId,
                                        won = false,
                                        finalTurn = result.finalTurn,
                                        playerSweep = false,
                                    )
                                    progress = progress.copy(
                                        multiplayer = progress.multiplayer.recordMatch(record, potDelta = -pot),
                                    )
                                    saveSlotMessage = "Defeat. -\$$pot"
                                }
                                // Restore party + world position from snapshot.
                                party = snap.partyDeepCopy
                                currentMapPath = snap.worldMapPath
                                spawnTileX = snap.worldTileX
                                spawnTileY = snap.worldTileY
                                mpSnapshot = null
                                mpIAmReady = false
                                mpPeerReady = false
                                // Atomic auto-save: persist W/L + pot delta IMMEDIATELY
                                // so a crash/force-stop after the match can't revert them.
                                // Cleans active-match-persist file too.
                                com.aetherbound.game.core.data.SaveGameIO.saveAuto(
                                    ctx,
                                    com.aetherbound.game.core.data.SaveGame(
                                        playerName = progress.playerName,
                                        currentMap = currentMapPath,
                                        playerTileX = spawnTileX,
                                        playerTileY = spawnTileY,
                                        party = party,
                                        pcStorage = pcStorage,
                                        inventory = inventory,
                                    ),
                                )
                                com.aetherbound.game.core.data.ActiveMatchPersist.clear(ctx)
                                scene = Scene.TuxemonWorld
                            },
                            party = party,
                            onSwitchTo = { newIdx -> party = party.switch(newIdx) },
                            onConcede = {
                                // Concede counts as a loss for ranked matches —
                                // record before restore so stats persist.
                                if (mpMode == com.aetherbound.game.core.data.MultiplayerRewards.Mode.RANKED) {
                                    val pot = com.aetherbound.game.core.data.MultiplayerRewards.computePotAnte(inventory.money)
                                    val (newBal, _) = com.aetherbound.game.core.data.MultiplayerRewards
                                        .applyLoss(inventory.money, pot)
                                    inventory = inventory.copy(money = newBal)
                                    progress = progress.copy(
                                        multiplayer = progress.multiplayer.recordMatch(
                                            com.aetherbound.game.core.data.MatchRecord(
                                                opponentDisplayName = mpPeerMatrixId.substringBefore(":").removePrefix("@"),
                                                opponentMatrixId = mpPeerMatrixId,
                                                won = false,
                                                finalTurn = 0,
                                                playerSweep = false,
                                            ),
                                            potDelta = -pot,
                                        ),
                                    )
                                }
                                party = snap.partyDeepCopy
                                currentMapPath = snap.worldMapPath
                                spawnTileX = snap.worldTileX
                                spawnTileY = snap.worldTileY
                                mpSnapshot = null
                                mpIAmReady = false
                                saveSlotMessage = "Conceded the match."
                                // Auto-save the loss + restored party so concede
                                // can't be undone by force-stop.
                                com.aetherbound.game.core.data.SaveGameIO.saveAuto(
                                    ctx,
                                    com.aetherbound.game.core.data.SaveGame(
                                        playerName = progress.playerName,
                                        currentMap = currentMapPath,
                                        playerTileX = spawnTileX,
                                        playerTileY = spawnTileY,
                                        party = party,
                                        pcStorage = pcStorage,
                                        inventory = inventory,
                                    ),
                                )
                                com.aetherbound.game.core.data.ActiveMatchPersist.clear(ctx)
                                scene = Scene.TuxemonWorld
                            },
                        )
                    }
                }
                Scene.AssetPacks -> com.aetherbound.game.render.ui.AssetPackPrompt(
                    onClose = { scene = Scene.Menu },
                )
                Scene.Updates -> com.aetherbound.game.render.ui.UpdatePromptScreen(
                    onBack = { scene = Scene.Menu },
                )
                Scene.MoveLearning -> {
                    val head = moveLearnQueue.firstOrNull()
                    val target = head?.let { party.members.getOrNull(it.partyIndex) }
                    if (head == null || target == null) {
                        // Queue drained or party shifted — bail back to world.
                        scene = if (sceneBeforeMenu == Scene.TuxemonWorld) Scene.TuxemonWorld else Scene.World
                    } else {
                        com.aetherbound.game.render.ui.MoveLearningPrompt(
                            instance = target,
                            newMove = head.newMove,
                            onForget = { slotIdx ->
                                // Replace move at [slotIdx] with the new one.
                                val newTechs = target.techniques.toMutableList()
                                if (slotIdx in newTechs.indices) {
                                    newTechs[slotIdx] = head.newMove
                                } else if (newTechs.size < 4) {
                                    newTechs += head.newMove
                                }
                                party = party.replace(head.partyIndex, target.copy(techniques = newTechs))
                                saveSlotMessage = "${target.species.name} learned ${head.newMove.name}!"
                                moveLearnQueue = moveLearnQueue.drop(1)
                                if (moveLearnQueue.isEmpty()) {
                                    scene = if (sceneBeforeMenu == Scene.TuxemonWorld) Scene.TuxemonWorld else Scene.World
                                }
                            },
                            onSkip = {
                                saveSlotMessage = "${target.species.name} did not learn ${head.newMove.name}."
                                moveLearnQueue = moveLearnQueue.drop(1)
                                if (moveLearnQueue.isEmpty()) {
                                    scene = if (sceneBeforeMenu == Scene.TuxemonWorld) Scene.TuxemonWorld else Scene.World
                                }
                            },
                        )
                    }
                }
                Scene.SaveMenu -> com.aetherbound.game.render.ui.SaveLoadMenu(
                    mode = com.aetherbound.game.render.ui.SaveLoadMode.SAVE,
                    currentSave = com.aetherbound.game.core.data.SaveGame(
                        playerName = "Player",
                        currentMap = "game/maps/tuxemon/spyder_shores.tmx",
                        party = party,
                        inventory = inventory,
                    ),
                    onBack = { scene = Scene.Menu },
                    onSaved = { slot -> saveSlotMessage = "Saved to slot ${slot + 1}" },
                )
                Scene.LoadMenu -> com.aetherbound.game.render.ui.SaveLoadMenu(
                    mode = com.aetherbound.game.render.ui.SaveLoadMode.LOAD,
                    onBack = { scene = Scene.Menu },
                    onLoaded = { save ->
                        party = save.party
                        inventory = save.inventory
                        saveSlotMessage = "Loaded ${save.playerName}"
                        scene = Scene.Menu
                    },
                )
            }
        }
        PerformanceOverlay(
            quality = quality,
            onCycleQuality = onCycleQuality,
            activeParticles = 0, // TODO wire from current scene's ParticleSystem
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp),
        )

        // Story-beat overlay — fires above any scene when a one-shot
        // narrative trigger is queued. Closing it sets the flag so the
        // beat never repeats this save.
        pendingStoryBeat?.let { beat ->
            com.aetherbound.game.render.ui.NpcDialogOverlay(
                speakerName = beat.speaker,
                lines = beat.lines,
                onClose = {
                    progress = progress.setFlag(beat.flag)
                    pendingStoryBeat = null
                },
            )
        }

        // First-multiplayer-match battery-optimisation prompt.
        if (showBatteryPrompt) {
            com.aetherbound.game.render.ui.BatteryOptimizationPrompt(
                onDismiss = {
                    progress = progress.setFlag("mp_battery_prompt_seen")
                    showBatteryPrompt = false
                },
            )
        }

        // Interrupted-match recovery prompt — restores party to pre-match state.
        pendingResume?.let { snap ->
            com.aetherbound.game.render.ui.NpcDialogOverlay(
                speakerName = "Aether-Vision",
                lines = listOf(
                    "An Aether bond was interrupted while you were away.",
                    "Your party is restored to the state before the match. The opponent's outcome stands.",
                ),
                onClose = {
                    party = snap.matchSnapshot.partyDeepCopy
                    currentMapPath = snap.matchSnapshot.worldMapPath
                    spawnTileX = snap.matchSnapshot.worldTileX
                    spawnTileY = snap.matchSnapshot.worldTileY
                    com.aetherbound.game.core.data.ActiveMatchPersist.clear(ctx)
                    pendingResume = null
                },
            )
        }
    }
    }
}

@Composable
private fun MenuOverlay(
    progress: com.aetherbound.game.core.data.PlayerProgress,
    onParty: () -> Unit,
    onBag: () -> Unit,
    onBestiary: () -> Unit,
    onStatus: () -> Unit,
    onSettings: () -> Unit,
    onPcStorage: () -> Unit,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onAssetPacks: () -> Unit,
    onUpdates: () -> Unit,
    onClose: () -> Unit,
    saveMessage: String,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(AetherColors.ObsidianDeep, AetherColors.Obsidian, AetherColors.Onyx)
                )
            ),
    ) {
        Column(
            Modifier.align(Alignment.Center).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("MENU", color = AetherColors.GoldBright, fontSize = 28.sp)
            Text(
                "${progress.playerName}  ·  ${progress.caughtSlugs.size}/411 caught",
                color = AetherColors.MutedText,
                fontSize = 11.sp,
            )
            Spacer(Modifier.size(8.dp))
            MenuButton("Party", onParty)
            MenuButton("Bag", onBag)
            MenuButton("Bestiary", onBestiary)
            MenuButton("Trainer Card", onStatus)
            MenuButton("PC Storage", onPcStorage)
            MenuButton("Settings", onSettings)
            MenuButton("Asset Packs", onAssetPacks)
            MenuButton("Check for Updates", onUpdates)
            MenuButton("Save Game", onSave)
            MenuButton("Load Game", onLoad)
            MenuButton("Close", onClose)
            if (saveMessage.isNotEmpty()) {
                Text(saveMessage, color = AetherColors.GoldBright, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun MenuButton(label: String, onClick: () -> Unit) {
    val audio = com.aetherbound.game.render.audio.LocalAudioEngine.current
    Box(
        Modifier
            .size(width = 200.dp, height = 44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(AetherColors.Slate)
            .clickable {
                audio?.playSfx(com.aetherbound.game.render.audio.AudioCatalog.SFX_MENU_CONFIRM, volume = 0.7f)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = AetherColors.GoldBright, fontSize = 16.sp)
    }
}

@Composable
private fun TitleScreen(onStart: () -> Unit, onSandbox: () -> Unit, onExit: () -> Unit) {
    val ctx = LocalContext.current
    val styleBoard = AssetSpecs.styleBoardOverworld()
    val hasBoard = AssetCache.exists(ctx, styleBoard)

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(AetherColors.Onyx, AetherColors.Obsidian, AetherColors.ObsidianDeep)
                )
            ),
    ) {
        if (hasBoard) {
            AsyncImage(
                model = ImageRequest.Builder(ctx)
                    .data("file:///android_asset/${styleBoard.path}")
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // gold-toned overlay so the board doesn't fight the title
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                AetherColors.ObsidianDeep.copy(alpha = 0.25f),
                                AetherColors.ObsidianDeep.copy(alpha = 0.85f),
                            )
                        )
                    ),
            )
        }
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                "AETHERBOUND",
                style = MaterialTheme.typography.displayLarge,
                color = AetherColors.GoldHighlight,
            )
            Text(
                "Pilot · Namaris Harbor",
                style = MaterialTheme.typography.labelMedium,
                color = AetherColors.MutedText,
            )
            Spacer(Modifier.size(20.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush = AetherColors.GoldGradient)
                    .clickable { onStart() }
                    .padding(horizontal = 32.dp, vertical = 14.dp),
            ) {
                Text(
                    "Begin",
                    style = MaterialTheme.typography.labelLarge,
                    color = AetherColors.Obsidian,
                )
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AetherColors.SlateLight)
                    .clickable { onSandbox() }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
            ) {
                Text(
                    "Attack Sandbox",
                    style = MaterialTheme.typography.labelLarge,
                    color = AetherColors.GoldBright,
                )
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AetherColors.Slate)
                    .clickable { onExit() }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
            ) {
                Text(
                    "Exit",
                    style = MaterialTheme.typography.labelLarge,
                    color = AetherColors.MutedText,
                )
            }
        }
    }
}
