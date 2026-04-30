package com.aetherbound.game.render.world

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.aetherbound.game.core.crossEdge
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.aetherbound.game.content.PilotMap
import com.aetherbound.game.content.TileMap
import com.aetherbound.game.render.shader.TimeOfDay
import com.aetherbound.game.render.shader.TimeOfDayOverlay
import com.aetherbound.game.render.theme.AetherColors
import com.aetherbound.game.render.theme.LocalQualityPreset
import kotlin.math.sin

// Authoring tile size from Visual Bible (32-48 px). Used for collision math.
// At RUNTIME we compute a viewport-relative pixelsPerTile so the map always
// fills the screen — the authoring tile size is the unit, not the render size.
private const val TILE_PX = 48f

/** Show ~7 tiles horizontally and ~11 tiles vertically on a phone (Pokemon-classic feel). */
private fun viewportPixelsPerTile(viewW: Float, viewH: Float): Float =
    maxOf(viewW / 7f, viewH / 11f, TILE_PX)
// Lighthouse anchor in tile coordinates — north-east of the new 33×24 map
// (above the harbor pier, at the eastern grass-strip edge).
private const val LIGHTHOUSE_COL = 27
private const val LIGHTHOUSE_ROW = 4

@Composable
fun WorldScene(
    onEncounter: (speciesId: String) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Prefer the Abraxas Outside-E atlas if present; fall back to the
    // CoastalTile/Tiny-Town renderer otherwise.
    val ctxOuter = androidx.compose.ui.platform.LocalContext.current
    val atlasBitmap = remember(ctxOuter) {
        val asset = "game/tilesets/outside_e.png"
        if (com.aetherbound.game.render.asset.AssetCache.exists(
                ctxOuter, com.aetherbound.game.render.asset.AssetSpec(asset))
        ) loadAssetBitmap(ctxOuter, asset) else null
    }
    // World grid (3×3 of 32×32 screens) when atlas is present;
    // single-screen fallback when atlas is missing.
    val worldGrid = remember(atlasBitmap) {
        if (atlasBitmap != null) com.aetherbound.game.content.PilotWorld.build() else null
    }
    var currentScreenPos by remember(worldGrid) {
        mutableStateOf(worldGrid?.startScreen ?: (1 to 1))
    }
    val currentScreen = worldGrid?.screenAt(currentScreenPos.first, currentScreenPos.second)
    val atlasMap = currentScreen?.map
    val map = remember { PilotMap.build() }
    // Player spawn: from world grid start, or fallback for single-map mode.
    var playerCol by remember(worldGrid) {
        mutableFloatStateOf(worldGrid?.startLocalCol?.toFloat() ?: 15f)
    }
    var playerRow by remember(worldGrid) {
        mutableFloatStateOf(worldGrid?.startLocalRow?.toFloat() ?: 17f)
    }
    var facing by remember { mutableStateOf(Facing.Down) }
    var animPhase by remember { mutableFloatStateOf(0f) }
    var timeOfDay by remember { mutableStateOf(TimeOfDay.Day) }
    var encounterTriggered by remember { mutableStateOf(false) }
    var heldDir by remember { mutableStateOf<Facing?>(null) }
    /** Step counter — used by Roaming-Legendary positioning. Persists across
     *  screen transitions; resets only on app restart. */
    var stepCount by remember { mutableLongStateOf(0L) }

    // Hold-to-walk loop: while a direction button is pressed, step every ~140ms
    // Collision view: prefer the atlas map's solid set when active, otherwise
    // the CoastalTile map's. Same idea for cols/rows bounds.
    val effCols = atlasMap?.cols ?: map.cols
    val effRows = atlasMap?.rows ?: map.rows
    val effSolid = atlasMap?.solid ?: map.solid
    val effEncounter = atlasMap?.encounter ?: map.encounter

    /** Steps the player by one tile, with optional cross-screen edge handling. */
    fun stepOnce(d: Facing) {
        facing = d
        val before = playerCol.toInt() to playerRow.toInt()
        // Edge-crossing first: if walking off this screen and a neighbor exists, transition
        if (worldGrid != null && currentScreen != null) {
            val edge: com.aetherbound.game.core.Edge? = when {
                d == Facing.Up && playerRow.toInt() <= 0 -> com.aetherbound.game.core.Edge.North
                d == Facing.Down && playerRow.toInt() >= currentScreen.rows - 1 -> com.aetherbound.game.core.Edge.South
                d == Facing.Left && playerCol.toInt() <= 0 -> com.aetherbound.game.core.Edge.West
                d == Facing.Right && playerCol.toInt() >= currentScreen.cols - 1 -> com.aetherbound.game.core.Edge.East
                else -> null
            }
            if (edge != null) {
                val crossed = worldGrid.crossEdge(
                    currentScreen, edge, playerCol.toInt(), playerRow.toInt()
                )
                if (crossed != null) {
                    val next = crossed.first
                    val nLocalC = crossed.second
                    val nLocalR = crossed.third
                    currentScreenPos = next.gridCol to next.gridRow
                    playerCol = nLocalC.toFloat()
                    playerRow = nLocalR.toFloat()
                    return
                }
                // No neighbor → blocked at edge
                return
            }
        }
        val (c2, r2) = tryStep(effCols, effRows, effSolid, playerCol, playerRow, d)
        playerCol = c2; playerRow = r2
    }

    LaunchedEffect(heldDir) {
        val d = heldDir ?: return@LaunchedEffect
        stepOnce(d)
        delay(180)
        while (heldDir == d) {
            stepOnce(d)
            delay(140)
        }
    }

    val transition = rememberInfiniteTransition(label = "world")
    val lightPulse by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val lightSweep by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(5400, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )

    // Walk anim: continuous while user is stepping; we just nudge animPhase per step
    LaunchedEffect(Unit) {
        var lastNs = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (lastNs == 0L) 16f else ((now - lastNs) / 1_000_000f).coerceAtMost(50f)
                lastNs = now
                animPhase = (animPhase + dt / 380f) % 1f
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Solid obsidian fallback in case the AI map doesn't fully cover
            // (it now does — see viewportPixelsPerTile — but this is a safety
            // net that doesn't visually fight the painting).
            .background(AetherColors.ObsidianDeep)
    ) {
        // Camera-tracked tilemap + lighthouse + player on a single Canvas (one layer = one GPU pass)
        WorldCanvas(
            map = map,
            atlasMap = atlasMap,
            atlasBitmap = atlasBitmap,
            playerCol = playerCol,
            playerRow = playerRow,
            facing = facing,
            animPhase = animPhase,
            lightPulse = lightPulse,
            lightSweep = lightSweep,
            modifier = Modifier.fillMaxSize(),
        )

        TimeOfDayOverlay(time = timeOfDay)

        // top hud
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(AetherColors.Onyx.copy(alpha = 0.78f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    "Namaris Harbor",
                    style = MaterialTheme.typography.titleMedium,
                    color = AetherColors.GoldBright,
                )
            }
            Spacer(Modifier.weight(1f))
            TodToggle(timeOfDay) { timeOfDay = it }
            Spacer(Modifier.size(8.dp))
            ExitChip(onExit)
        }

        // D-Pad bottom-start (hold-to-walk)
        DirectionPad(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            onHoldStart = { dir -> heldDir = dir },
            onHoldEnd = { heldDir = null },
        )

        // Encounter roll: 25% chance per step into a tall-grass cell. The
        // flag resets when the player leaves the cell so re-entering can roll
        // again. Species comes from biome-aware pool, with a sub-roll for the
        // 3 Roaming Legendaries when the player happens to be on a Legendary's
        // current screen.
        LaunchedEffect(playerCol, playerRow) {
            stepCount++
            val key = playerCol.toInt() to playerRow.toInt()
            val onGrass = key in effEncounter
            if (onGrass && !encounterTriggered) {
                encounterTriggered = true
                if (kotlin.random.Random.nextDouble() < 0.25) {
                    val rng = kotlin.random.Random(stepCount * 0x9E3779B97F4A7C15uL.toLong())
                    val theme = currentScreen?.theme ?: com.aetherbound.game.core.ScreenTheme.Plains
                    // Step 1: Legendary roll — 5% if a Legendary is on this screen
                    val species = if (worldGrid != null && currentScreen != null) {
                        com.aetherbound.game.core.RoamingLegendary.rollLegendaryEncounter(
                            gridCol = currentScreen.gridCol,
                            gridRow = currentScreen.gridRow,
                            gridCols = worldGrid.cols,
                            gridRows = worldGrid.rows,
                            stepCount = stepCount,
                            rng = rng,
                        ) ?: com.aetherbound.game.core.EchoformDex.rollEncounter(theme, rng)
                    } else {
                        com.aetherbound.game.core.EchoformDex.rollEncounter(theme, rng)
                    }
                    species?.let { onEncounter(it.id) }
                }
            } else if (!onGrass) {
                encounterTriggered = false
            }
        }
    }
}

private fun tryStep(
    cols: Int, rows: Int, solid: Set<Pair<Int, Int>>,
    col: Float, row: Float, dir: Facing,
): Pair<Float, Float> {
    val (dc, dr) = when (dir) {
        Facing.Up -> 0 to -1
        Facing.Down -> 0 to 1
        Facing.Left -> -1 to 0
        Facing.Right -> 1 to 0
    }
    val nc = (col + dc).coerceIn(0f, (cols - 1).toFloat())
    val nr = (row + dr).coerceIn(0f, (rows - 1).toFloat())
    val solidKey = nc.toInt() to nr.toInt()
    return if (solidKey in solid) col to row else nc to nr
}

@Composable
private fun WorldCanvas(
    map: TileMap,
    atlasMap: com.aetherbound.game.core.AtlasMap?,
    atlasBitmap: ImageBitmap?,
    playerCol: Float,
    playerRow: Float,
    facing: Facing,
    animPhase: Float,
    lightPulse: Float,
    lightSweep: Float,
    modifier: Modifier,
) {
    val preset = LocalQualityPreset.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    // Per-tile PNG bitmaps loaded once. If a tile has a PNG in
    // assets/game/tiles/<key>.png the renderer uses it; otherwise vector.
    val tileBitmaps = remember(ctx) { loadTileBitmaps(ctx) }
    val useAtlas = atlasMap != null && atlasBitmap != null

    Canvas(modifier) {
        val viewW = size.width
        val viewH = size.height
        val tilePx = viewportPixelsPerTile(viewW, viewH)
        val camX = playerCol * tilePx - viewW / 2f + tilePx / 2f
        val camY = playerRow * tilePx - viewH / 2f + tilePx / 2f

        // Render path 1: atlas-based (preferred when Outside_E.png + AtlasMap present).
        // Two passes per cell: ground layer + optional overlay (buildings/props).
        if (useAtlas && atlasMap != null && atlasBitmap != null) {
            val atlasTilePx = com.aetherbound.game.core.ATLAS_TILE_PX
            val atlasCols = atlasBitmap.width / atlasTilePx
            val atlasRows = atlasBitmap.height / atlasTilePx
            for (r in 0 until atlasMap.rows) {
                for (c in 0 until atlasMap.cols) {
                    val tx = c * tilePx - camX
                    val ty = r * tilePx - camY
                    if (tx + tilePx < 0 || tx > viewW) continue
                    if (ty + tilePx < 0 || ty > viewH) continue
                    val ground = atlasMap.ground[r][c]
                    drawAtlasCell(atlasBitmap, ground.col, ground.row, atlasCols, atlasRows, atlasTilePx, tx, ty, tilePx)
                    val ov = atlasMap.overlay[r][c]
                    if (ov != null) {
                        drawAtlasCell(atlasBitmap, ov.col, ov.row, atlasCols, atlasRows, atlasTilePx, tx, ty, tilePx)
                    }
                }
            }
        } else {
            // Render path 2: legacy per-tile PNG / vector fallback.
            for (r in 0 until map.rows) {
                for (c in 0 until map.cols) {
                    val tx = c * tilePx - camX
                    val ty = r * tilePx - camY
                    if (tx + tilePx < 0 || tx > viewW) continue
                    if (ty + tilePx < 0 || ty > viewH) continue
                    val tile = map.tiles[r][c]
                    val bmp = tileBitmaps[tile]
                    if (bmp != null) {
                        drawImage(
                            image = bmp,
                            srcOffset = androidx.compose.ui.unit.IntOffset.Zero,
                            srcSize = androidx.compose.ui.unit.IntSize(bmp.width, bmp.height),
                            dstOffset = androidx.compose.ui.unit.IntOffset(tx.toInt(), ty.toInt()),
                            dstSize = androidx.compose.ui.unit.IntSize(tilePx.toInt(), tilePx.toInt()),
                            filterQuality = androidx.compose.ui.graphics.FilterQuality.None,
                        )
                    } else {
                        translate(tx, ty) {
                            drawCoastalTile(tile, tilePx)
                        }
                    }
                }
            }
        }

        // 2) lighthouse landmark — always drawn, layered on top of the tile grid
        run {
            val ax = LIGHTHOUSE_COL * tilePx + tilePx / 2f - camX
            val ay = LIGHTHOUSE_ROW * tilePx + tilePx / 2f - camY
            translate(ax - tilePx, ay - tilePx * 3f) {
                drawLighthouse(width = tilePx * 2f, height = tilePx * 4f, pulse = lightPulse, sweepDeg = lightSweep, glow = preset.bloomRadiusPx > 0f)
            }
        }

        // 3) player drawn at screen center (camera follows player). Player
        // sprite scales with tilePx so it looks proportional on big screens.
        val playerSize = tilePx * 1.6f
        val px = viewW / 2f - playerSize / 2f
        val py = viewH / 2f - playerSize / 2f
        translate(px, py) {
            drawPlayer(playerSize, facing, animPhase)
        }
    }
}

private fun DrawScope.drawLighthouse(width: Float, height: Float, pulse: Float, sweepDeg: Float, glow: Boolean) {
    val w = width
    val h = height
    drawRect(Color(0xFF6B4D2F), Offset(w * 0.35f, h * 0.85f), Size(w * 0.30f, h * 0.10f))
    val shaft = Brush.verticalGradient(0f to Color(0xFFEFE5C8), 1f to Color(0xFFCDC1A2))
    drawRect(shaft, Offset(w * 0.32f, h * 0.18f), Size(w * 0.36f, h * 0.67f))
    drawRect(Color(0xFF7A2C28), Offset(w * 0.32f, h * 0.45f), Size(w * 0.36f, h * 0.06f))
    drawRect(Color(0xFF7A2C28), Offset(w * 0.32f, h * 0.62f), Size(w * 0.36f, h * 0.06f))
    drawRect(Color(0xFF1A2233), Offset(w * 0.28f, h * 0.10f), Size(w * 0.44f, h * 0.10f))
    drawRect(AetherColors.GoldDeep, Offset(w * 0.28f, h * 0.10f), Size(w * 0.44f, h * 0.10f), style = Stroke(2f))
    val dome = Path().apply {
        moveTo(w * 0.25f, h * 0.10f)
        quadraticBezierTo(w * 0.50f, -h * 0.02f, w * 0.75f, h * 0.10f)
        close()
    }
    drawPath(dome, Color(0xFF7A2C28))
    if (glow) {
        drawCircle(
            color = AetherColors.GoldHighlight.copy(alpha = 0.4f + 0.3f * pulse),
            radius = w * (0.18f + 0.12f * pulse),
            center = Offset(w * 0.5f, h * 0.155f),
            blendMode = BlendMode.Plus,
        )
    }
    drawCircle(AetherColors.GoldBright, w * 0.06f, Offset(w * 0.5f, h * 0.155f))

    // sweep cone
    val len = w * 1.4f
    val ang = Math.toRadians(sweepDeg.toDouble())
    val ex = (w * 0.5 + Math.cos(ang) * len).toFloat()
    val ey = (h * 0.155 + Math.sin(ang) * len).toFloat()
    val cone = Path().apply {
        moveTo(w * 0.5f, h * 0.155f)
        lineTo(ex + sin(ang + 0.20).toFloat() * 30f, ey + sin(ang + 0.20).toFloat() * 30f)
        lineTo(ex - sin(ang - 0.20).toFloat() * 30f, ey - sin(ang - 0.20).toFloat() * 30f)
        close()
    }
    drawPath(cone, Brush.radialGradient(
        listOf(AetherColors.GoldHighlight.copy(alpha = 0.30f), Color.Transparent),
        center = Offset(w * 0.5f, h * 0.155f), radius = len,
    ), blendMode = BlendMode.Plus)
}

private fun DrawScope.drawPlayer(s: Float, facing: Facing, animPhase: Float) {
    val swing = sin(animPhase * 6.2832f) * (s * 0.06f)
    drawOval(Color(0x55000000), Offset(s * 0.14f, s * 0.78f), Size(s * 0.72f, s * 0.10f))
    when (facing) {
        Facing.Down -> drawPlayerDown(s, swing)
        Facing.Up -> drawPlayerUp(s, swing)
        Facing.Left -> drawPlayerSide(s, swing, flip = true)
        Facing.Right -> drawPlayerSide(s, swing, flip = false)
    }
}

private fun DrawScope.drawPlayerDown(s: Float, swing: Float) {
    drawRect(AetherColors.Obsidian, Offset(s * 0.36f, s * 0.55f + swing), Size(s * 0.10f, s * 0.20f))
    drawRect(AetherColors.Obsidian, Offset(s * 0.54f, s * 0.55f - swing), Size(s * 0.10f, s * 0.20f))
    val cape = Path().apply {
        moveTo(s * 0.20f, s * 0.30f); lineTo(s * 0.80f, s * 0.30f)
        lineTo(s * 0.72f, s * 0.62f); lineTo(s * 0.28f, s * 0.62f); close()
    }
    drawPath(cape, AetherColors.ObsidianDeep)
    drawRect(AetherColors.Slate, Offset(s * 0.34f, s * 0.36f), Size(s * 0.32f, s * 0.22f))
    drawRect(AetherColors.GoldCore, Offset(s * 0.34f, s * 0.36f), Size(s * 0.32f, s * 0.04f))
    drawRect(AetherColors.ObsidianDeep, Offset(s * 0.20f, s * 0.36f + swing * 0.7f), Size(s * 0.08f, s * 0.18f))
    drawRect(AetherColors.ObsidianDeep, Offset(s * 0.72f, s * 0.36f - swing * 0.7f), Size(s * 0.08f, s * 0.18f))
    drawCircle(Color(0xFFE5C39E), s * 0.13f, Offset(s * 0.5f, s * 0.22f))
    val hair = Path().apply {
        moveTo(s * 0.36f, s * 0.18f)
        quadraticBezierTo(s * 0.5f, s * 0.05f, s * 0.64f, s * 0.18f)
        lineTo(s * 0.64f, s * 0.24f); lineTo(s * 0.36f, s * 0.24f); close()
    }
    drawPath(hair, Color(0xFF1F1B17))
    drawCircle(Color(0xFF101217), s * 0.012f, Offset(s * 0.46f, s * 0.225f))
    drawCircle(Color(0xFF101217), s * 0.012f, Offset(s * 0.54f, s * 0.225f))
}

private fun DrawScope.drawPlayerUp(s: Float, swing: Float) {
    drawRect(AetherColors.Obsidian, Offset(s * 0.36f, s * 0.55f + swing), Size(s * 0.10f, s * 0.20f))
    drawRect(AetherColors.Obsidian, Offset(s * 0.54f, s * 0.55f - swing), Size(s * 0.10f, s * 0.20f))
    val cape = Path().apply {
        moveTo(s * 0.18f, s * 0.28f); lineTo(s * 0.82f, s * 0.28f)
        lineTo(s * 0.74f, s * 0.66f); lineTo(s * 0.26f, s * 0.66f); close()
    }
    drawPath(cape, AetherColors.ObsidianDeep)
    drawRect(AetherColors.GoldCore, Offset(s * 0.48f, s * 0.30f), Size(s * 0.04f, s * 0.34f))
    drawRect(AetherColors.ObsidianDeep, Offset(s * 0.20f, s * 0.36f + swing * 0.7f), Size(s * 0.08f, s * 0.18f))
    drawRect(AetherColors.ObsidianDeep, Offset(s * 0.72f, s * 0.36f - swing * 0.7f), Size(s * 0.08f, s * 0.18f))
    drawCircle(Color(0xFF1F1B17), s * 0.13f, Offset(s * 0.5f, s * 0.22f))
}

private fun DrawScope.drawPlayerSide(s: Float, swing: Float, flip: Boolean) {
    val mirror = if (flip) -1f else 1f
    val ox = s * 0.5f
    fun fx(x: Float): Float = ox + mirror * (x - s * 0.5f)
    drawRect(AetherColors.Obsidian, Offset(fx(s * 0.40f) - s * 0.05f, s * 0.55f + swing), Size(s * 0.10f, s * 0.20f))
    drawRect(AetherColors.Obsidian, Offset(fx(s * 0.55f) - s * 0.05f, s * 0.55f - swing), Size(s * 0.10f, s * 0.20f))
    val cape = Path().apply {
        moveTo(fx(s * 0.30f), s * 0.30f); lineTo(fx(s * 0.70f), s * 0.30f)
        lineTo(fx(s * 0.66f), s * 0.62f); lineTo(fx(s * 0.34f), s * 0.62f); close()
    }
    drawPath(cape, AetherColors.ObsidianDeep)
    drawRect(AetherColors.Slate, Offset(fx(s * 0.36f), s * 0.36f), Size(s * 0.28f, s * 0.22f))
    drawRect(AetherColors.GoldCore, Offset(fx(s * 0.36f), s * 0.36f), Size(s * 0.28f, s * 0.04f))
    drawRect(AetherColors.ObsidianDeep, Offset(fx(s * 0.40f), s * 0.36f + swing), Size(s * 0.08f, s * 0.20f))
    drawCircle(Color(0xFFE5C39E), s * 0.13f, Offset(fx(s * 0.5f), s * 0.22f))
    val hair = Path().apply {
        moveTo(fx(s * 0.36f), s * 0.18f)
        quadraticBezierTo(fx(s * 0.5f), s * 0.05f, fx(s * 0.64f), s * 0.18f)
        lineTo(fx(s * 0.64f), s * 0.24f); lineTo(fx(s * 0.36f), s * 0.24f); close()
    }
    drawPath(hair, Color(0xFF1F1B17))
    drawCircle(Color(0xFF101217), s * 0.014f, Offset(fx(s * 0.56f), s * 0.225f))
}

@Composable
private fun DirectionPad(
    modifier: Modifier = Modifier,
    onHoldStart: (Facing) -> Unit,
    onHoldEnd: () -> Unit,
) {
    Box(modifier.size(180.dp)) {
        DirButton(Modifier.align(Alignment.TopCenter), Facing.Up, onHoldStart, onHoldEnd)
        DirButton(Modifier.align(Alignment.BottomCenter), Facing.Down, onHoldStart, onHoldEnd)
        DirButton(Modifier.align(Alignment.CenterStart), Facing.Left, onHoldStart, onHoldEnd)
        DirButton(Modifier.align(Alignment.CenterEnd), Facing.Right, onHoldStart, onHoldEnd)
    }
}

@Composable
private fun DirButton(
    modifier: Modifier,
    dir: Facing,
    onHoldStart: (Facing) -> Unit,
    onHoldEnd: () -> Unit,
) {
    Box(
        modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(AetherColors.Onyx.copy(alpha = 0.85f))
            .pointerInput(dir) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onHoldStart(dir)
                    waitForUpOrCancellation()
                    onHoldEnd()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            when (dir) {
                Facing.Up -> "▲"; Facing.Down -> "▼"; Facing.Left -> "◀"; Facing.Right -> "▶"
            },
            color = AetherColors.GoldBright,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun TodToggle(current: TimeOfDay, onPick: (TimeOfDay) -> Unit) {
    val order = listOf(TimeOfDay.Dawn, TimeOfDay.Day, TimeOfDay.Dusk, TimeOfDay.Night)
    val next by remember(current) { derivedStateOf { order[(order.indexOf(current) + 1) % order.size] } }
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AetherColors.Onyx.copy(alpha = 0.78f))
            .clickable { onPick(next) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(current.name, color = AetherColors.GoldBright, style = MaterialTheme.typography.labelLarge)
    }
}

/** Draw one cell of an atlas at screen pos (tx, ty). Bounds-safe. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAtlasCell(
    atlas: ImageBitmap,
    col: Int, row: Int,
    atlasCols: Int, atlasRows: Int,
    atlasTilePx: Int,
    tx: Float, ty: Float, dstPx: Float,
) {
    if (col !in 0 until atlasCols || row !in 0 until atlasRows) return
    drawImage(
        image = atlas,
        srcOffset = androidx.compose.ui.unit.IntOffset(col * atlasTilePx, row * atlasTilePx),
        srcSize = androidx.compose.ui.unit.IntSize(atlasTilePx, atlasTilePx),
        dstOffset = androidx.compose.ui.unit.IntOffset(tx.toInt(), ty.toInt()),
        dstSize = androidx.compose.ui.unit.IntSize(dstPx.toInt(), dstPx.toInt()),
        filterQuality = androidx.compose.ui.graphics.FilterQuality.None,
    )
}

/** Load a single asset PNG into a Compose ImageBitmap. Returns null if missing. */
private fun loadAssetBitmap(ctx: android.content.Context, path: String): ImageBitmap? = try {
    ctx.assets.open(path).use { stream ->
        android.graphics.BitmapFactory.decodeStream(stream)?.asImageBitmap()
    }
} catch (e: Exception) {
    null
}

/**
 * Map every CoastalTile to its asset PNG (if generated) or null (vector fallback).
 * The filename convention is `assets/game/tiles/<lower_snake>.png` matching the
 * enum constant in lower_snake_case.
 */
private fun loadTileBitmaps(ctx: android.content.Context): Map<CoastalTile, ImageBitmap?> {
    val out = HashMap<CoastalTile, ImageBitmap?>(CoastalTile.values().size)
    for (t in CoastalTile.values()) {
        val key = t.name.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
        out[t] = loadAssetBitmap(ctx, "game/tiles/$key.png")
    }
    return out
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
