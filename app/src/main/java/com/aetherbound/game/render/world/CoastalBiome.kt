package com.aetherbound.game.render.world

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import com.aetherbound.game.render.theme.AetherColors

/** Coastal-biome tile glyphs — a small vector vocabulary the world is built from. */
enum class CoastalTile { Sand, Grass, GrassTall, Path, PathStone, Pier, PierEdge,
    SeaShallow, SeaDeep, ShoreFoam, Crate, BarrelLamp, BollardGold, RoofTile,
    Wall, Door, Window, FlagPole, Hedge, Stair, Lantern, Bench, Sign, SealStone, EmptyShadow }

/** Render one tile within the given size at (0,0) (caller translates). */
fun DrawScope.drawCoastalTile(tile: CoastalTile, tileSize: Float, accent: Color = AetherColors.GoldCore) {
    when (tile) {
        CoastalTile.Sand -> {
            drawRect(Color(0xFFD7B58A), size = Size(tileSize, tileSize))
            drawCircle(Color(0x55B58A55), tileSize * 0.06f, Offset(tileSize * 0.3f, tileSize * 0.6f))
            drawCircle(Color(0x55B58A55), tileSize * 0.04f, Offset(tileSize * 0.7f, tileSize * 0.3f))
        }
        CoastalTile.Grass -> {
            drawRect(Color(0xFF2F5E37), size = Size(tileSize, tileSize))
            for (i in 0..3) {
                val x = tileSize * (0.1f + i * 0.25f)
                drawLine(Color(0xFF59B66B), Offset(x, tileSize * 0.85f), Offset(x, tileSize * 0.55f), 3f)
            }
        }
        CoastalTile.GrassTall -> {
            drawRect(Color(0xFF285030), size = Size(tileSize, tileSize))
            for (i in 0..5) {
                val x = tileSize * (0.08f + i * 0.16f)
                drawLine(Color(0xFF6BC97A), Offset(x, tileSize * 0.9f), Offset(x, tileSize * 0.30f), 3.5f)
            }
        }
        CoastalTile.Path -> {
            drawRect(Color(0xFFB89773), size = Size(tileSize, tileSize))
            drawCircle(Color(0x77836547), tileSize * 0.08f, Offset(tileSize * 0.5f, tileSize * 0.5f))
        }
        CoastalTile.PathStone -> {
            drawRect(Color(0xFF8E7B5F), size = Size(tileSize, tileSize))
            drawRect(Color(0xFFB89773), Offset(tileSize * 0.12f, tileSize * 0.12f), Size(tileSize * 0.34f, tileSize * 0.34f))
            drawRect(Color(0xFFB89773), Offset(tileSize * 0.54f, tileSize * 0.54f), Size(tileSize * 0.34f, tileSize * 0.34f))
            drawRect(Color(0xFFB89773), Offset(tileSize * 0.54f, tileSize * 0.12f), Size(tileSize * 0.30f, tileSize * 0.28f))
        }
        CoastalTile.Pier -> {
            drawRect(Color(0xFF6B4D2F), size = Size(tileSize, tileSize))
            drawLine(Color(0xFF45301C), Offset(0f, tileSize * 0.5f), Offset(tileSize, tileSize * 0.5f), 4f)
        }
        CoastalTile.PierEdge -> {
            drawRect(Color(0xFF45301C), size = Size(tileSize, tileSize))
            drawRect(Color(0xFF6B4D2F), Offset(0f, 0f), Size(tileSize, tileSize * 0.7f))
        }
        CoastalTile.SeaShallow -> {
            drawRect(Color(0xFF2C82B7), size = Size(tileSize, tileSize))
            drawArc(Color(0x77B5E6F2), 200f, 140f, false,
                topLeft = Offset(tileSize * 0.1f, tileSize * 0.4f),
                size = Size(tileSize * 0.8f, tileSize * 0.4f),
                style = Stroke(2f))
        }
        CoastalTile.SeaDeep -> {
            drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF1B4F71), Color(0xFF0E2E48))),
                size = Size(tileSize, tileSize))
        }
        CoastalTile.ShoreFoam -> {
            drawRect(Color(0xFF2C82B7), size = Size(tileSize, tileSize))
            drawArc(Color(0xCCFFFFFF), 0f, 180f, false,
                topLeft = Offset(0f, tileSize * 0.55f),
                size = Size(tileSize, tileSize * 0.45f),
                style = Stroke(4f))
        }
        CoastalTile.Crate -> {
            drawRect(Color(0xFF785134), Offset(tileSize * 0.18f, tileSize * 0.20f),
                Size(tileSize * 0.64f, tileSize * 0.6f))
            drawLine(Color(0xFF4F351F), Offset(tileSize * 0.18f, tileSize * 0.5f),
                Offset(tileSize * 0.82f, tileSize * 0.5f), 3f)
        }
        CoastalTile.BarrelLamp -> {
            drawCircle(Color(0xFF4A3018), tileSize * 0.30f, Offset(tileSize * 0.5f, tileSize * 0.6f))
            drawCircle(Color(0xFFFFD78A), tileSize * 0.16f, Offset(tileSize * 0.5f, tileSize * 0.30f))
            drawCircle(Color(0x66FFD78A), tileSize * 0.32f, Offset(tileSize * 0.5f, tileSize * 0.30f))
        }
        CoastalTile.BollardGold -> {
            drawRect(brush = Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.5f))),
                topLeft = Offset(tileSize * 0.36f, tileSize * 0.30f),
                size = Size(tileSize * 0.28f, tileSize * 0.55f))
            drawCircle(accent, tileSize * 0.18f, Offset(tileSize * 0.5f, tileSize * 0.30f))
        }
        CoastalTile.RoofTile -> {
            drawRect(Color(0xFF7A2C28), size = Size(tileSize, tileSize))
            for (i in 0..2) {
                drawLine(Color(0xFF4F1A18),
                    Offset(0f, tileSize * (0.25f + i * 0.25f)),
                    Offset(tileSize, tileSize * (0.25f + i * 0.25f)), 3f)
            }
        }
        CoastalTile.Wall -> {
            drawRect(Color(0xFFD7CDB8), size = Size(tileSize, tileSize))
            drawLine(Color(0xFF8C7E62), Offset(0f, tileSize * 0.5f), Offset(tileSize, tileSize * 0.5f), 2f)
            drawLine(Color(0xFF8C7E62), Offset(tileSize * 0.4f, 0f), Offset(tileSize * 0.4f, tileSize * 0.5f), 2f)
            drawLine(Color(0xFF8C7E62), Offset(tileSize * 0.7f, tileSize * 0.5f), Offset(tileSize * 0.7f, tileSize), 2f)
        }
        CoastalTile.Door -> {
            drawRect(Color(0xFFD7CDB8), size = Size(tileSize, tileSize))
            drawRect(Color(0xFF45301C),
                topLeft = Offset(tileSize * 0.25f, tileSize * 0.25f),
                size = Size(tileSize * 0.5f, tileSize * 0.75f))
            drawCircle(accent, tileSize * 0.05f, Offset(tileSize * 0.66f, tileSize * 0.62f))
        }
        CoastalTile.Window -> {
            drawRect(Color(0xFFD7CDB8), size = Size(tileSize, tileSize))
            drawRect(Color(0xFF1F4A6C),
                topLeft = Offset(tileSize * 0.22f, tileSize * 0.22f),
                size = Size(tileSize * 0.56f, tileSize * 0.40f))
            drawLine(Color(0xFF8C7E62), Offset(tileSize * 0.5f, tileSize * 0.22f),
                Offset(tileSize * 0.5f, tileSize * 0.62f), 3f)
        }
        CoastalTile.FlagPole -> {
            drawRect(Color(0xFFD7CDB8), size = Size(tileSize, tileSize))
            drawLine(Color(0xFF8C7E62), Offset(tileSize * 0.5f, tileSize),
                Offset(tileSize * 0.5f, tileSize * 0.05f), 3f)
            val flag = Path().apply {
                moveTo(tileSize * 0.5f, tileSize * 0.10f)
                lineTo(tileSize * 0.85f, tileSize * 0.18f)
                lineTo(tileSize * 0.5f, tileSize * 0.30f)
                close()
            }
            drawPath(flag, accent)
        }
        CoastalTile.Hedge -> {
            drawRect(Color(0xFF1F3F25), size = Size(tileSize, tileSize))
            drawCircle(Color(0xFF356B41), tileSize * 0.20f, Offset(tileSize * 0.30f, tileSize * 0.45f))
            drawCircle(Color(0xFF356B41), tileSize * 0.20f, Offset(tileSize * 0.70f, tileSize * 0.45f))
            drawCircle(Color(0xFF356B41), tileSize * 0.18f, Offset(tileSize * 0.50f, tileSize * 0.30f))
        }
        CoastalTile.Stair -> {
            drawRect(Color(0xFF8E7B5F), size = Size(tileSize, tileSize))
            for (i in 0..3) drawLine(Color(0xFF45301C),
                Offset(0f, tileSize * (0.20f + i * 0.20f)),
                Offset(tileSize, tileSize * (0.20f + i * 0.20f)), 2f)
        }
        CoastalTile.Lantern -> {
            drawRect(Color(0xFFD7CDB8), size = Size(tileSize, tileSize))
            drawLine(Color(0xFF45301C), Offset(tileSize * 0.5f, tileSize),
                Offset(tileSize * 0.5f, tileSize * 0.4f), 3f)
            drawCircle(Color(0xFFFFD78A), tileSize * 0.12f, Offset(tileSize * 0.5f, tileSize * 0.30f))
            drawCircle(Color(0x55FFD78A), tileSize * 0.24f, Offset(tileSize * 0.5f, tileSize * 0.30f))
        }
        CoastalTile.Bench -> {
            drawRect(Color(0xFFD7CDB8), size = Size(tileSize, tileSize))
            drawRect(Color(0xFF6B4D2F),
                topLeft = Offset(tileSize * 0.10f, tileSize * 0.40f),
                size = Size(tileSize * 0.80f, tileSize * 0.18f))
        }
        CoastalTile.Sign -> {
            drawRect(Color(0xFFD7CDB8), size = Size(tileSize, tileSize))
            drawRect(Color(0xFF45301C),
                topLeft = Offset(tileSize * 0.20f, tileSize * 0.30f),
                size = Size(tileSize * 0.60f, tileSize * 0.40f))
            for (i in 0..1) drawLine(accent,
                Offset(tileSize * 0.28f, tileSize * (0.42f + i * 0.14f)),
                Offset(tileSize * 0.72f, tileSize * (0.42f + i * 0.14f)), 2f)
        }
        CoastalTile.SealStone -> {
            drawRect(Color(0xFF101725), size = Size(tileSize, tileSize))
            drawCircle(accent, tileSize * 0.32f, Offset(tileSize * 0.5f, tileSize * 0.5f), style = Stroke(3f))
            drawCircle(accent, tileSize * 0.18f, Offset(tileSize * 0.5f, tileSize * 0.5f))
        }
        CoastalTile.EmptyShadow -> {
            drawRect(Color(0x44000000), size = Size(tileSize, tileSize))
        }
    }
}

/** Place a tile relative to the canvas origin. */
fun DrawScope.placeTile(col: Int, row: Int, tileSize: Float, tile: CoastalTile, accent: Color = AetherColors.GoldCore) {
    translate(col * tileSize, row * tileSize) {
        drawCoastalTile(tile, tileSize, accent)
    }
}
