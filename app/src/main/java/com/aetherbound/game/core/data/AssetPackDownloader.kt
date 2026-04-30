package com.aetherbound.game.core.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * Downloads + extracts on-demand asset packs from a GitHub Release.
 *
 * Each pack lives at `<baseUrl>/<pack>.zip` and is described in
 * `<baseUrl>/manifest.json`. After extraction, files land in
 *
 *     getExternalFilesDir(null)/packs/<pack-id>/...
 *
 * which is auto-cleared on uninstall and needs no Storage permission.
 *
 * The asset-resolver helpers ([resolveStream], [resolveFile]) check the
 * pack directory first, then fall back to APK assets — so any code that
 * already reads `ctx.assets.open("game/...")` keeps working unchanged.
 */
object AssetPackDownloader {

    enum class Pack(val id: String) {
        AUDIO("audio"),
        GRAPHICS("graphics"),
        MAPS("maps"),
    }

    /** Where pack manifest + .zip files live. Default: GitHub Releases of this repo. */
    var baseUrl: String = "https://github.com/abra-xass/Aetherbound/releases/latest/download"

    // ── Public API ────────────────────────────────────────────────

    /** True when [pack] has been downloaded and unzipped on this device. */
    fun isInstalled(ctx: Context, pack: Pack): Boolean {
        val markerFile = File(packDir(ctx, pack), ".installed")
        return markerFile.exists()
    }

    fun installedSizeBytes(ctx: Context, pack: Pack): Long {
        val dir = packDir(ctx, pack)
        if (!dir.exists()) return 0
        return dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    }

    /** Remove a pack from disk. */
    fun uninstall(ctx: Context, pack: Pack): Boolean {
        return packDir(ctx, pack).deleteRecursively()
    }

    /** Fetch + cache the manifest. Returns null on network failure. */
    suspend fun fetchManifest(): Manifest? {
        return runCatching {
            val txt = download("$baseUrl/manifest.json")
            val obj = JSONObject(txt.toString(Charsets.UTF_8))
            val packs = mutableMapOf<String, PackEntry>()
            val packsObj = obj.optJSONObject("packs") ?: return@runCatching Manifest("?", emptyMap())
            val keys = packsObj.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val po = packsObj.getJSONObject(id)
                packs[id] = PackEntry(
                    url = po.optString("url"),
                    size = po.optLong("size"),
                    sha256 = po.optString("sha256"),
                )
            }
            Manifest(obj.optString("appVersion"), packs)
        }.getOrNull()
    }

    /**
     * Download [pack].zip, verify sha256, extract into [packDir]. Streams
     * progress 0.0..1.0 into [onProgress]. Returns true on success.
     */
    suspend fun downloadAndExtract(
        ctx: Context,
        pack: Pack,
        manifest: Manifest,
        onProgress: (Float) -> Unit,
    ): Boolean {
        val entry = manifest.packs[pack.id] ?: return false
        val targetZip = File(ctx.cacheDir, "${pack.id}.zip")
        if (!downloadTo(targetZip, "$baseUrl/${entry.url}", entry.size, onProgress)) {
            return false
        }
        val actualSha = sha256(targetZip)
        if (entry.sha256.isNotEmpty() && actualSha != entry.sha256) {
            targetZip.delete()
            return false
        }
        val dir = packDir(ctx, pack).apply { deleteRecursively(); mkdirs() }
        if (!extractZip(targetZip, dir)) return false
        targetZip.delete()
        File(dir, ".installed").writeText(actualSha)
        return true
    }

    // ── Asset resolver ────────────────────────────────────────────

    /**
     * Open an asset by its original assets-relative path. Looks first in
     * the pack directory of the matching pack, falls back to APK assets.
     */
    fun resolveStream(ctx: Context, assetPath: String): java.io.InputStream? {
        val pack = packForAsset(assetPath)
        if (pack != null) {
            val f = File(packDir(ctx, pack), assetPath)
            if (f.exists()) return f.inputStream()
        }
        return runCatching { ctx.assets.open(assetPath) }.getOrNull()
    }

    fun resolveFile(ctx: Context, assetPath: String): File? {
        val pack = packForAsset(assetPath) ?: return null
        val f = File(packDir(ctx, pack), assetPath)
        return f.takeIf { it.exists() }
    }

    /**
     * List entries inside an asset directory. Pack dir takes priority; if
     * empty/missing, falls back to APK-bundled `ctx.assets.list(dir)`.
     */
    fun resolveListing(ctx: Context, dirPath: String): List<String> {
        val pack = packForAsset("$dirPath/")
        if (pack != null) {
            val packSub = File(packDir(ctx, pack), dirPath)
            if (packSub.isDirectory) {
                val entries = packSub.list()?.toList()
                if (!entries.isNullOrEmpty()) return entries
            }
        }
        return runCatching { ctx.assets.list(dirPath)?.toList() ?: emptyList() }
            .getOrDefault(emptyList())
    }

    private fun packForAsset(assetPath: String): Pack? = when {
        assetPath.startsWith("audio/") -> Pack.AUDIO
        assetPath.startsWith("game/techniques/tuxemon/") -> Pack.GRAPHICS
        assetPath.startsWith("game/maps/") -> Pack.MAPS
        else -> null
    }

    // ── Internals ─────────────────────────────────────────────────

    fun packDir(ctx: Context, pack: Pack): File {
        val base = ctx.getExternalFilesDir(null) ?: ctx.filesDir
        return File(base, "packs/${pack.id}")
    }

    private fun download(url: String): ByteArray {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 60_000
            instanceFollowRedirects = true
        }
        conn.inputStream.use { return it.readBytes() }
    }

    private fun downloadTo(
        target: File,
        url: String,
        expectedSize: Long,
        onProgress: (Float) -> Unit,
    ): Boolean = runCatching {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 60_000
            instanceFollowRedirects = true
        }
        conn.inputStream.use { input ->
            FileOutputStream(target).use { output ->
                val buf = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    output.write(buf, 0, n)
                    total += n
                    if (expectedSize > 0) {
                        onProgress((total.toFloat() / expectedSize.toFloat()).coerceIn(0f, 1f))
                    }
                }
            }
        }
        true
    }.getOrDefault(false)

    private fun extractZip(zip: File, dir: File): Boolean = runCatching {
        ZipInputStream(zip.inputStream()).use { zin ->
            var entry = zin.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val out = File(dir, entry.name)
                    out.parentFile?.mkdirs()
                    out.outputStream().use { os ->
                        zin.copyTo(os)
                    }
                }
                zin.closeEntry()
                entry = zin.nextEntry
            }
        }
        true
    }.getOrDefault(false)

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    data class Manifest(val appVersion: String, val packs: Map<String, PackEntry>)
    data class PackEntry(val url: String, val size: Long, val sha256: String)
}
