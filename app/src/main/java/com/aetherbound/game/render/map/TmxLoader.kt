package com.aetherbound.game.render.map

import android.content.Context
import android.util.Base64
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.Inflater

/**
 * Minimal Tiled .tmx + .tsx loader for Tuxemon-imported maps.
 *
 * Supports:
 *   - orthogonal tile layers, base64 + zlib/gzip/none encoding
 *   - external .tsx tilesets referenced via firstgid/source
 *   - object layers (extracted as [TmxObject] for warps, NPCs, signs)
 *   - per-tile properties (read but not interpreted)
 *
 * Skipped (not needed for Tuxemon's pilot rendering):
 *   - isometric / hexagonal orientations
 *   - infinite maps, chunked layer data
 *   - tile flipping flags (top 3 bits of gid stripped on decode)
 *   - animations
 */

data class TmxMap(
    val width: Int,
    val height: Int,
    val tileWidth: Int,
    val tileHeight: Int,
    val tilesets: List<TmxTilesetRef>,
    val tileLayers: List<TmxTileLayer>,
    val objectLayers: List<TmxObjectLayer>,
)

data class TmxTilesetRef(
    val firstGid: Int,
    /** Path relative to the .tmx, OR an embedded inline tileset's image path. */
    val source: String?,
    /** Inline tileset metadata when [source] is null. */
    val inline: TmxTileset? = null,
)

data class TmxTileset(
    val name: String,
    val tileWidth: Int,
    val tileHeight: Int,
    val tileCount: Int,
    val columns: Int,
    /** Image path relative to the .tsx (or .tmx for inline tilesets). */
    val imageSource: String,
    val imageWidth: Int,
    val imageHeight: Int,
)

data class TmxTileLayer(
    val id: Int,
    val name: String,
    val width: Int,
    val height: Int,
    val visible: Boolean,
    val opacity: Float,
    /** Global tile ids per cell, length = width * height. Zero = empty. */
    val data: IntArray,
)

data class TmxObjectLayer(
    val name: String,
    val objects: List<TmxObject>,
)

data class TmxObject(
    val id: Int,
    val name: String,
    val type: String,         // class/type — "warp", "npc", "sign", etc
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val properties: Map<String, String>,
)

object TmxLoader {

    private val GID_FLIP_MASK = 0x1FFFFFFF
    // Tile-flip bits live in the top 3; we strip them since rendering ignores flips for now.

    /** Loads and parses a .tmx map from assets (pack dir or APK). */
    fun loadMap(ctx: Context, assetPath: String): TmxMap {
        val stream = com.aetherbound.game.core.data.AssetPackDownloader.resolveStream(ctx, assetPath)
            ?: throw java.io.FileNotFoundException("tmx not found: $assetPath")
        stream.use { return parseMap(it) }
    }

    /** Loads and parses a .tsx external tileset from assets (pack dir or APK). */
    fun loadTileset(ctx: Context, assetPath: String): TmxTileset {
        val stream = com.aetherbound.game.core.data.AssetPackDownloader.resolveStream(ctx, assetPath)
            ?: throw java.io.FileNotFoundException("tsx not found: $assetPath")
        stream.use { return parseTileset(it) }
    }

    // ───────────────────────────────────────────────────────────────
    // .tmx parsing
    // ───────────────────────────────────────────────────────────────

    private fun parseMap(stream: InputStream): TmxMap {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(stream, "UTF-8")

        var mapWidth = 0
        var mapHeight = 0
        var tileWidth = 0
        var tileHeight = 0
        val tilesets = mutableListOf<TmxTilesetRef>()
        val tileLayers = mutableListOf<TmxTileLayer>()
        val objectLayers = mutableListOf<TmxObjectLayer>()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "map" -> {
                        mapWidth = parser.requireIntAttr("width")
                        mapHeight = parser.requireIntAttr("height")
                        tileWidth = parser.requireIntAttr("tilewidth")
                        tileHeight = parser.requireIntAttr("tileheight")
                    }
                    "tileset" -> tilesets += parseTilesetRef(parser)
                    "layer" -> tileLayers += parseTileLayer(parser)
                    "objectgroup" -> objectLayers += parseObjectLayer(parser)
                }
            }
            event = parser.next()
        }

        return TmxMap(
            width = mapWidth,
            height = mapHeight,
            tileWidth = tileWidth,
            tileHeight = tileHeight,
            tilesets = tilesets,
            tileLayers = tileLayers,
            objectLayers = objectLayers,
        )
    }

    private fun parseTilesetRef(parser: XmlPullParser): TmxTilesetRef {
        val firstGid = parser.requireIntAttr("firstgid")
        val source = parser.getAttributeValue(null, "source")
        if (source != null) {
            // External .tsx — skip past </tileset> and return a lazy reference.
            skipUntilEndOf(parser, "tileset")
            return TmxTilesetRef(firstGid = firstGid, source = source, inline = null)
        }
        // Inline tileset: parse name/dims and the <image> child.
        val name = parser.getAttributeValue(null, "name") ?: "inline"
        val tileWidth = parser.requireIntAttr("tilewidth")
        val tileHeight = parser.requireIntAttr("tileheight")
        val tileCount = parser.optIntAttr("tilecount", 0)
        val columns = parser.optIntAttr("columns", 0)
        var imageSource = ""
        var imageWidth = 0
        var imageHeight = 0
        var depth = 1
        while (depth > 0) {
            val event = parser.next()
            if (event == XmlPullParser.START_TAG) {
                if (parser.name == "image") {
                    imageSource = parser.getAttributeValue(null, "source") ?: ""
                    imageWidth = parser.optIntAttr("width", 0)
                    imageHeight = parser.optIntAttr("height", 0)
                }
                if (parser.name != "image") depth++
            } else if (event == XmlPullParser.END_TAG) {
                depth--
            } else if (event == XmlPullParser.END_DOCUMENT) {
                break
            }
        }
        val inline = TmxTileset(
            name = name, tileWidth = tileWidth, tileHeight = tileHeight,
            tileCount = tileCount, columns = columns,
            imageSource = imageSource, imageWidth = imageWidth, imageHeight = imageHeight,
        )
        return TmxTilesetRef(firstGid = firstGid, source = null, inline = inline)
    }

    private fun parseTileLayer(parser: XmlPullParser): TmxTileLayer {
        val id = parser.optIntAttr("id", 0)
        val name = parser.getAttributeValue(null, "name") ?: "layer"
        val width = parser.requireIntAttr("width")
        val height = parser.requireIntAttr("height")
        val visible = parser.optIntAttr("visible", 1) != 0
        val opacity = parser.getAttributeValue(null, "opacity")?.toFloatOrNull() ?: 1f

        var encoding: String? = null
        var compression: String? = null
        var rawData = ""

        // Walk to the <data> child.
        var event = parser.next()
        while (event != XmlPullParser.END_TAG || parser.name != "layer") {
            if (event == XmlPullParser.START_TAG && parser.name == "data") {
                encoding = parser.getAttributeValue(null, "encoding")
                compression = parser.getAttributeValue(null, "compression")
                // Read text content
                val sb = StringBuilder()
                event = parser.next()
                while (!(event == XmlPullParser.END_TAG && parser.name == "data")) {
                    if (event == XmlPullParser.TEXT) sb.append(parser.text)
                    event = parser.next()
                }
                rawData = sb.toString()
            }
            event = parser.next()
            if (event == XmlPullParser.END_DOCUMENT) break
        }

        val data = decodeLayerData(rawData, encoding, compression, width * height)

        return TmxTileLayer(
            id = id, name = name, width = width, height = height,
            visible = visible, opacity = opacity, data = data,
        )
    }

    private fun parseObjectLayer(parser: XmlPullParser): TmxObjectLayer {
        val name = parser.getAttributeValue(null, "name") ?: "objects"
        val objects = mutableListOf<TmxObject>()

        var event = parser.next()
        while (event != XmlPullParser.END_TAG || parser.name != "objectgroup") {
            if (event == XmlPullParser.START_TAG && parser.name == "object") {
                objects += parseObject(parser)
            }
            event = parser.next()
            if (event == XmlPullParser.END_DOCUMENT) break
        }

        return TmxObjectLayer(name = name, objects = objects)
    }

    private fun parseObject(parser: XmlPullParser): TmxObject {
        val id = parser.optIntAttr("id", 0)
        val name = parser.getAttributeValue(null, "name") ?: ""
        // Tiled 1.9+ uses "class"; pre-1.9 uses "type".
        val type = parser.getAttributeValue(null, "class")
            ?: parser.getAttributeValue(null, "type") ?: ""
        val x = parser.getAttributeValue(null, "x")?.toFloatOrNull() ?: 0f
        val y = parser.getAttributeValue(null, "y")?.toFloatOrNull() ?: 0f
        val width = parser.getAttributeValue(null, "width")?.toFloatOrNull() ?: 0f
        val height = parser.getAttributeValue(null, "height")?.toFloatOrNull() ?: 0f

        val props = mutableMapOf<String, String>()
        var depth = 1
        while (depth > 0) {
            val event = parser.next()
            if (event == XmlPullParser.START_TAG) {
                if (parser.name == "property") {
                    val pName = parser.getAttributeValue(null, "name") ?: ""
                    val pVal = parser.getAttributeValue(null, "value") ?: ""
                    props[pName] = pVal
                } else {
                    depth++
                }
            } else if (event == XmlPullParser.END_TAG) {
                depth--
            } else if (event == XmlPullParser.END_DOCUMENT) break
        }

        return TmxObject(
            id = id, name = name, type = type,
            x = x, y = y, width = width, height = height,
            properties = props,
        )
    }

    // ───────────────────────────────────────────────────────────────
    // .tsx parsing — external tilesets
    // ───────────────────────────────────────────────────────────────

    private fun parseTileset(stream: InputStream): TmxTileset {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(stream, "UTF-8")

        var name = ""
        var tileWidth = 0
        var tileHeight = 0
        var tileCount = 0
        var columns = 0
        var imageSource = ""
        var imageWidth = 0
        var imageHeight = 0

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "tileset" -> {
                        name = parser.getAttributeValue(null, "name") ?: ""
                        tileWidth = parser.optIntAttr("tilewidth", 16)
                        tileHeight = parser.optIntAttr("tileheight", 16)
                        tileCount = parser.optIntAttr("tilecount", 0)
                        columns = parser.optIntAttr("columns", 0)
                    }
                    "image" -> {
                        imageSource = parser.getAttributeValue(null, "source") ?: ""
                        imageWidth = parser.optIntAttr("width", 0)
                        imageHeight = parser.optIntAttr("height", 0)
                    }
                }
            }
            event = parser.next()
        }

        return TmxTileset(
            name = name, tileWidth = tileWidth, tileHeight = tileHeight,
            tileCount = tileCount, columns = columns,
            imageSource = imageSource, imageWidth = imageWidth, imageHeight = imageHeight,
        )
    }

    // ───────────────────────────────────────────────────────────────
    // Layer-data decoding
    // ───────────────────────────────────────────────────────────────

    private fun decodeLayerData(
        raw: String,
        encoding: String?,
        compression: String?,
        expectedSize: Int,
    ): IntArray {
        if (encoding == "csv" || encoding == null) {
            // CSV — split, parse to int. (encoding=null also CSV in old tilesets.)
            val out = IntArray(expectedSize)
            var i = 0
            for (token in raw.split(',', '\n', '\r', ' ', '\t')) {
                if (token.isBlank()) continue
                if (i >= out.size) break
                out[i++] = (token.trim().toLongOrNull() ?: 0L).toInt() and GID_FLIP_MASK
            }
            return out
        }
        if (encoding != "base64") {
            // Unsupported encoding — return empty layer
            return IntArray(expectedSize)
        }

        val bytes = Base64.decode(raw.trim(), Base64.DEFAULT)
        val decompressed = when (compression) {
            null, "" -> bytes
            "zlib" -> inflateZlib(bytes)
            "gzip" -> GZIPInputStream(bytes.inputStream()).use { it.readBytes() }
            "zstd" -> bytes // not supported, treat as raw — will produce broken layer
            else -> bytes
        }

        val out = IntArray(expectedSize)
        // Each gid is 4 bytes, little-endian.
        var i = 0
        var b = 0
        while (i < expectedSize && b + 3 < decompressed.size) {
            val gid = (decompressed[b].toInt() and 0xFF) or
                ((decompressed[b + 1].toInt() and 0xFF) shl 8) or
                ((decompressed[b + 2].toInt() and 0xFF) shl 16) or
                ((decompressed[b + 3].toInt() and 0xFF) shl 24)
            out[i] = gid and GID_FLIP_MASK
            i++
            b += 4
        }
        return out
    }

    private fun inflateZlib(data: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(data)
        val out = java.io.ByteArrayOutputStream(data.size * 4)
        val buf = ByteArray(4096)
        while (!inflater.finished()) {
            val n = inflater.inflate(buf)
            if (n == 0) {
                if (inflater.needsInput() || inflater.needsDictionary()) break
            } else {
                out.write(buf, 0, n)
            }
        }
        inflater.end()
        return out.toByteArray()
    }

    // ───────────────────────────────────────────────────────────────
    // XmlPullParser helpers
    // ───────────────────────────────────────────────────────────────

    private fun XmlPullParser.requireIntAttr(name: String): Int =
        getAttributeValue(null, name)?.toIntOrNull()
            ?: throw IllegalStateException("missing int attribute '$name' on <$this.name>")

    private fun XmlPullParser.optIntAttr(name: String, default: Int): Int =
        getAttributeValue(null, name)?.toIntOrNull() ?: default

    private fun skipUntilEndOf(parser: XmlPullParser, tag: String) {
        var depth = 1
        while (depth > 0) {
            val event = parser.next()
            when (event) {
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> if (parser.name == tag) depth--
                XmlPullParser.END_DOCUMENT -> return
            }
        }
    }
}
