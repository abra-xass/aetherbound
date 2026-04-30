package com.aetherbound.game.render.world

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherbound.game.core.EchoformInstance
import com.aetherbound.game.core.ScreenTheme
import com.aetherbound.game.core.data.AmbientTime
import com.aetherbound.game.core.data.DayNightPhase
import com.aetherbound.game.core.data.EncounterEngine
import com.aetherbound.game.core.data.rememberAmbientTint
import com.aetherbound.game.render.map.CollisionMap
import com.aetherbound.game.render.map.MovementController
import com.aetherbound.game.render.map.ObjectDispatcher
import com.aetherbound.game.render.map.TmxLoader
import com.aetherbound.game.render.map.TmxRenderer
import com.aetherbound.game.render.map.WorldEvent
import com.aetherbound.game.render.theme.AetherColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Full Tuxemon-driven world scene. Loads a `.tmx` map asynchronously,
 * resolves tilesets, sets up [MovementController] + [CollisionMap], steps
 * the player on dpad input, rolls [EncounterEngine] on grass tiles, and
 * applies [AmbientTime] real-clock day/night tint.
 *
 *   tmxAssetPath — `"game/maps/tuxemon/spyder_shores.tmx"`
 *   biome        — bias for the encounter pool when no zone overrides
 *   onEncounter  — battle bridge (caller usually pushes BattleScene)
 *   onMenu       — opens the bag/party menu
 */
@Composable
fun TuxemonWorldScene(
    tmxAssetPath: String,
    biome: ScreenTheme,
    onExit: () -> Unit,
    onEncounter: (EchoformInstance) -> Unit,
    onMenu: () -> Unit = {},
    onTrainerEncounter: (trainerId: String) -> Unit = {},
    /** Fires when player walks onto a warp tile. Caller swaps tmxAssetPath. */
    onWarp: (destMap: String, destTileX: Int, destTileY: Int) -> Unit = { _, _, _ -> },
    /** Fires when player walks onto a HealZone. Caller restores party. */
    onHealRequest: () -> Unit = {},
    /**
     * Fires when player walks onto an ItemDrop (and the flag wasn't yet set).
     * Caller adds item, sets the flag, returns true if handled (suppresses
     * future re-trigger on this map session).
     */
    onItemPickup: (itemSlug: String, flagId: String) -> Boolean = { _, _ -> false },
    /** Where the player should spawn when this map loads (tile coords). */
    spawnTileX: Int = 8,
    spawnTileY: Int = 8,
    /** Set of flags already collected — suppresses re-triggers. */
    collectedFlags: Set<String> = emptySet(),
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val audio = com.aetherbound.game.render.audio.LocalAudioEngine.current
    var renderer by remember(tmxAssetPath) { mutableStateOf<TmxRenderer?>(null) }
    var collision by remember(tmxAssetPath) { mutableStateOf<CollisionMap?>(null) }
    var npcs by remember(tmxAssetPath) { mutableStateOf<List<com.aetherbound.game.render.map.NpcEntity>>(emptyList()) }
    @Suppress("UNUSED_VARIABLE") val npcTick by androidx.compose.runtime.produceState(initialValue = 0L, npcs) {
        if (npcs.isEmpty()) return@produceState
        var last = System.nanoTime()
        while (true) {
            val now = System.nanoTime()
            val dt = ((now - last) / 1_000_000L).coerceAtMost(64)
            last = now
            for (npc in npcs) npc.tick(dt, collision, kotlin.random.Random.Default)
            value++
            kotlinx.coroutines.delay(16)
        }
    }

    val movement = remember(tmxAssetPath) { MovementController(initialTileX = spawnTileX, initialTileY = spawnTileY) }
    val encounter = remember(tmxAssetPath) { EncounterEngine(ctx) }
    val rng = remember { Random(System.nanoTime()) }

    val tint by rememberAmbientTint()

    var statusLine by remember { mutableStateOf("") }
    var activeDialog by remember { mutableStateOf<DialogPayload?>(null) }

    // Async map load
    LaunchedEffect(tmxAssetPath) {
        val map = withContext(Dispatchers.IO) { TmxLoader.loadMap(ctx, tmxAssetPath) }
        renderer = TmxRenderer.build(ctx, map, tmxAssetPath)
        collision = CollisionMap.fromMap(map)
        npcs = com.aetherbound.game.render.map.NpcSpawner.fromMap(map)
    }

    // Frame loop — advance MovementController, sample encounter check on each completed step
    LaunchedEffect(tmxAssetPath) {
        var lastFrame = System.nanoTime()
        while (true) {
            val now = System.nanoTime()
            val dtMs = ((now - lastFrame) / 1_000_000L).coerceAtMost(64)
            lastFrame = now

            val stepCompleted = movement.tick(dtMs, collision)
            if (stepCompleted) {
                val r = renderer
                val event = if (r != null) ObjectDispatcher.eventAt(r.map, movement.tileX, movement.tileY) else null
                when (event) {
                    is WorldEvent.HealZone -> {
                        onHealRequest()
                        activeDialog = DialogPayload(
                            speakerName = "Aether Pulse",
                            lines = listOf("Your Echoforms recover their full Vigor."),
                        )
                    }
                    is WorldEvent.Sign -> {
                        // Route signs through DialogueResolver — its world_sign
                        // category-grammar produces fresh sign text for any
                        // msgid not in the literal map. Fallback line is the
                        // old static one.
                        val resolved = com.aetherbound.game.dialogue.DialogueResolver
                            .get(ctx)
                            .resolve(
                                msgid = event.text,
                                spriteArchetype = null,
                                fallback = "Hier steht eine alte Markierung.",
                            )
                        activeDialog = DialogPayload(
                            speakerName = "",
                            lines = paginate(resolved),
                        )
                    }
                    is WorldEvent.Warp -> {
                        if (event.destMap.isNotBlank()) {
                            statusLine = "Warp → ${event.destMap.substringAfterLast('/').removeSuffix(".tmx")}"
                            onWarp(event.destMap, event.destTileX, event.destTileY)
                        }
                    }
                    is WorldEvent.ItemDrop -> {
                        if (event.flagId !in collectedFlags) {
                            val handled = onItemPickup(event.itemSlug, event.flagId)
                            if (handled) {
                                activeDialog = DialogPayload(
                                    speakerName = "Item Found",
                                    lines = listOf("You found a ${event.itemSlug.replace('_', ' ')}!"),
                                )
                            }
                        }
                    }
                    is WorldEvent.Npc -> {
                        if (event.trainerId != null) {
                            statusLine = "Trainer ${event.trainerId} challenges you!"
                            onTrainerEncounter(event.trainerId)
                        } else {
                            // Resolve through the dialogue pipeline:
                            //   1. literal msgid lookup (quest/lore/keep_structure)
                            //   2. sprite-archetype Tracery grammar (barmaid/alchemist/…)
                            //   3. category fallback (npc_flavor)
                            //   4. final TextNormalizer scrub (no Tuxemon leaks)
                            val resolved = com.aetherbound.game.dialogue.DialogueResolver
                                .get(ctx)
                                .resolve(
                                    msgid = event.dialog,
                                    spriteArchetype = event.sprite,
                                )
                            activeDialog = DialogPayload(
                                speakerName = "Reisender",
                                lines = paginate(resolved),
                            )
                        }
                    }
                    else -> {
                        // Encounter roll only on plain ground / grass
                        val onGrass = event !is WorldEvent.Generic || event.type.contains("grass")
                        encounter.onStepCompleted(
                            theme = biome,
                            onEncounterTile = onGrass,
                            level = (rng.nextInt(3) + biomeLevelFloor(biome)),
                            rng = rng,
                        )?.let { wild ->
                            statusLine = "A wild ${wild.species.name} appeared!"
                            audio?.playSfx(com.aetherbound.game.render.audio.AudioCatalog.SFX_BATTLE_HIT, volume = 0.8f)
                            onEncounter(wild)
                        }
                    }
                }
            }
            delay(16)
        }
    }

    Box(modifier.fillMaxSize().background(AetherColors.ObsidianDeep)) {
        // Map render with camera centered on player tile
        renderer?.let { r ->
            // Centre camera on player tile (with sub-tile offset)
            val viewportTilesX = 12
            val viewportTilesY = 9
            val tileW = r.map.tileWidth
            val tileH = r.map.tileHeight
            val cameraX = (movement.tileX * tileW - viewportTilesX * tileW / 2 + movement.pixelOffset.x)
                .coerceIn(0, (r.widthPx - viewportTilesX * tileW).coerceAtLeast(0))
            val cameraY = (movement.tileY * tileH - viewportTilesY * tileH / 2 + movement.pixelOffset.y)
                .coerceIn(0, (r.heightPx - viewportTilesY * tileH).coerceAtLeast(0))

            com.aetherbound.game.render.map.TmxScene(
                tmxAssetPath = tmxAssetPath,
                cameraPx = IntOffset(cameraX, cameraY),
                zoom = 2.5f,
                background = AetherColors.ObsidianDeep,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Day/night tint overlay
        Box(
            Modifier
                .fillMaxSize()
                .background(tint.first.copy(alpha = tint.second)),
        )

        // HUD
        Column(Modifier.align(Alignment.TopStart).padding(12.dp)) {
            Text(
                text = AmbientTime.phaseNow().displayName.uppercase(),
                color = AetherColors.GoldBright,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Tile (${movement.tileX}, ${movement.tileY})",
                color = AetherColors.MutedText,
                fontSize = 11.sp,
            )
        }

        if (statusLine.isNotEmpty()) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AetherColors.Obsidian.copy(alpha = 0.85f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(statusLine, color = AetherColors.ParchmentText, fontSize = 12.sp)
            }
        }

        // NPC dialog overlay (above HUD, below DPad — captures taps until dismissed)
        activeDialog?.let { d ->
            com.aetherbound.game.render.ui.NpcDialogOverlay(
                speakerName = d.speakerName,
                lines = d.lines,
                onClose = { activeDialog = null },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // DPad
        DPadOverlay(
            collision = collision,
            controller = movement,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
        )

        // Right-side action buttons
        Column(
            Modifier.align(Alignment.BottomEnd).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onMenu,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AetherColors.Slate,
                    contentColor = AetherColors.GoldBright,
                ),
            ) { Text("MENU") }
            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AetherColors.Slate,
                    contentColor = AetherColors.WarningRed,
                ),
            ) { Text("EXIT") }
        }
    }
}

/** Active dialog payload — driven by ObjectDispatcher events. */
private data class DialogPayload(val speakerName: String, val lines: List<String>)

/** Splits long text into ~80-char pages so the dialog box stays compact. */
private fun paginate(text: String, maxPerPage: Int = 80): List<String> {
    if (text.length <= maxPerPage) return listOf(text)
    val pages = mutableListOf<String>()
    var i = 0
    while (i < text.length) {
        val end = minOf(i + maxPerPage, text.length)
        // Try to break at the last whitespace before the limit for nicer wrapping.
        val cut = if (end < text.length) {
            val ws = text.lastIndexOf(' ', end)
            if (ws > i) ws else end
        } else end
        pages += text.substring(i, cut).trim()
        i = cut
    }
    return pages
}

/** Hard-coded baseline level per biome. Replace once route table is real. */
private fun biomeLevelFloor(biome: ScreenTheme): Int = when (biome) {
    ScreenTheme.Beach, ScreenTheme.Plains, ScreenTheme.Harbor -> 4
    ScreenTheme.Forest, ScreenTheme.Marsh -> 8
    ScreenTheme.Mountain, ScreenTheme.Cave -> 14
    ScreenTheme.Desert -> 18
    ScreenTheme.Castle, ScreenTheme.Sanctum -> 22
    else -> 6
}

@Composable
private fun DPadOverlay(
    collision: CollisionMap?,
    controller: MovementController,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DPadButton("▲") { controller.requestStep(MovementController.Facing.NORTH, collision) }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DPadButton("◀") { controller.requestStep(MovementController.Facing.WEST, collision) }
            DPadButton("●") { /* center / interact */ }
            DPadButton("▶") { controller.requestStep(MovementController.Facing.EAST, collision) }
        }
        DPadButton("▼") { controller.requestStep(MovementController.Facing.SOUTH, collision) }
    }
}

@Composable
private fun DPadButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = AetherColors.Slate,
            contentColor = AetherColors.GoldBright,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        modifier = Modifier.size(48.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
