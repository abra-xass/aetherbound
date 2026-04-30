package com.aetherbound.game

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                )
            }
        }
    }
}

private enum class Scene {
    Title, World, TuxemonWorld, Battle, TrainerBattle, Sandbox,
    Menu, Party, Bag, SaveMenu, LoadMenu,
    Detail, Bestiary, Settings, StatusCard, ItemTarget,
    AssetPacks, Updates, MoveLearning,
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
            Scene.AssetPacks, Scene.Updates -> scene = Scene.Menu
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
            Scene.Battle, Scene.TrainerBattle,
            Scene.MoveLearning -> { /* modal — user must pick an option, no back */ }
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
                    // Install pilot trainers once.
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        com.aetherbound.game.core.data.TrainerRegistry.Pilot.installAll()
                    }
                    com.aetherbound.game.render.world.TuxemonWorldScene(
                        tmxAssetPath = "game/maps/tuxemon/spyder_shores.tmx",
                        biome = com.aetherbound.game.core.ScreenTheme.Beach,
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
                            // Add to party if room, else PC storage stub.
                            if (!party.isFull) {
                                party = (party.add(instance) as? com.aetherbound.game.core.data.Party.AddResult.Added)
                                    ?.party ?: party
                            }
                            saveSlotMessage = "${instance.species.name} caught!"
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
