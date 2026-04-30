package com.aetherbound.game.core.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * One-tap in-app update flow against the public GitHub Releases page.
 *
 * Pulls `https://api.github.com/repos/<owner>/<repo>/releases/latest`,
 * compares `tag_name` vs the installed `versionName`, downloads the APK
 * asset, and hands it to Android's package installer via FileProvider.
 *
 * The user still has to tap "Install" once on Android's installer prompt
 * (a hard system requirement on stock Android, can't be silently bypassed
 * without root). After the first time the user grants `REQUEST_INSTALL_PACKAGES`
 * for Aetherbound, subsequent updates are one-tap from inside the game.
 */
object UpdateChecker {

    /** GitHub repo path. */
    var repoOwner: String = "abra-xass"
    var repoName: String = "Aetherbound"

    /** Asset filename pattern to look for in releases. */
    var apkAssetSuffix: String = ".apk"

    data class ReleaseInfo(
        val tagName: String,           // e.g. "v0.2.0"
        val versionLabel: String,      // e.g. "0.2.0"
        val publishedAt: String,
        val notes: String,
        val apkUrl: String,
        val apkSize: Long,
        val isPrerelease: Boolean,
    )

    sealed class CheckResult {
        data class UpToDate(val installed: String) : CheckResult()
        data class UpdateAvailable(val installed: String, val latest: ReleaseInfo) : CheckResult()
        data class Error(val message: String) : CheckResult()
    }

    /** Read the installed app version from the manifest. */
    fun installedVersion(ctx: Context): String {
        return try {
            @Suppress("DEPRECATION")
            val pi: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ctx.packageManager.getPackageInfo(ctx.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            }
            pi.versionName ?: "0.0.0"
        } catch (e: Exception) { "0.0.0" }
    }

    /**
     * Hit the GitHub Releases API and return the latest stable release.
     * Returns null on network or parse error (caller can show a toast).
     */
    suspend fun fetchLatest(includePrerelease: Boolean = false): ReleaseInfo? = runCatching {
        val url = if (includePrerelease)
            "https://api.github.com/repos/$repoOwner/$repoName/releases?per_page=1"
        else
            "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
        val text = httpGet(url) ?: return@runCatching null
        val obj = if (includePrerelease) {
            // listing → take first
            val arr = org.json.JSONArray(text)
            if (arr.length() == 0) return@runCatching null
            arr.getJSONObject(0)
        } else {
            JSONObject(text)
        }

        val tag = obj.optString("tag_name", "")
        val name = obj.optString("name", tag).ifEmpty { tag }
        val notes = obj.optString("body", "")
        val publishedAt = obj.optString("published_at", "")
        val prerelease = obj.optBoolean("prerelease", false)

        // Pick the .apk asset.
        val assets = obj.optJSONArray("assets") ?: return@runCatching null
        var apkUrl = ""
        var apkSize = 0L
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            val n = a.optString("name", "")
            if (n.endsWith(apkAssetSuffix, ignoreCase = true)) {
                apkUrl = a.optString("browser_download_url", "")
                apkSize = a.optLong("size", 0)
                break
            }
        }
        if (apkUrl.isEmpty()) return@runCatching null

        ReleaseInfo(
            tagName = tag,
            versionLabel = sanitiseTag(tag).ifEmpty { name },
            publishedAt = publishedAt,
            notes = notes,
            apkUrl = apkUrl,
            apkSize = apkSize,
            isPrerelease = prerelease,
        )
    }.getOrNull()

    /**
     * Combine [installedVersion] + [fetchLatest] into one checkable result
     * suitable for direct UI consumption.
     */
    suspend fun check(ctx: Context, includePrerelease: Boolean = false): CheckResult {
        val installed = installedVersion(ctx)
        val latest = fetchLatest(includePrerelease)
            ?: return CheckResult.Error("Could not reach GitHub")
        return if (compareVersions(installed, latest.versionLabel) >= 0)
            CheckResult.UpToDate(installed)
        else
            CheckResult.UpdateAvailable(installed, latest)
    }

    /**
     * Download the APK to `<external-files>/updates/<tag>.apk` and stream
     * progress 0..1. Returns the file or null on failure.
     */
    suspend fun downloadApk(
        ctx: Context,
        info: ReleaseInfo,
        onProgress: (Float) -> Unit,
    ): File? {
        val dir = File(ctx.getExternalFilesDir(null) ?: ctx.filesDir, "updates").apply { mkdirs() }
        val out = File(dir, "${info.tagName.ifEmpty { "latest" }}.apk")
        return runCatching {
            val conn = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 60_000
                instanceFollowRedirects = true
            }
            conn.inputStream.use { input ->
                FileOutputStream(out).use { output ->
                    val buf = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        output.write(buf, 0, n)
                        total += n
                        if (info.apkSize > 0) {
                            onProgress((total.toFloat() / info.apkSize.toFloat()).coerceIn(0f, 1f))
                        }
                    }
                }
            }
            out
        }.getOrNull()
    }

    /**
     * Hand [apk] to Android's package installer. The user sees the system
     * "Install Aetherbound v0.2.0?" prompt and confirms once. If the user
     * has not yet granted `REQUEST_INSTALL_PACKAGES` for this app, Android
     * routes them through that permission flow first.
     */
    fun startInstall(ctx: Context, apk: File): Boolean {
        return runCatching {
            val authority = "${ctx.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(ctx, authority, apk)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            ctx.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    // ── Helpers ──────────────────────────────────────────────────

    /** "v0.2.0" → "0.2.0". Strips a leading 'v' and any trailing prerelease tag. */
    private fun sanitiseTag(tag: String): String {
        val core = if (tag.startsWith("v") || tag.startsWith("V")) tag.substring(1) else tag
        return core.takeWhile { it.isDigit() || it == '.' }
    }

    /**
     * Compare semver-ish strings: returns negative if [a] < [b], zero if
     * equal, positive if [a] > [b]. Non-numeric components default to 0.
     */
    private fun compareVersions(a: String, b: String): Int {
        val pa = a.split('.').map { it.toIntOrNull() ?: 0 }
        val pb = b.split('.').map { it.toIntOrNull() ?: 0 }
        val n = maxOf(pa.size, pb.size)
        for (i in 0 until n) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return x - y
        }
        return 0
    }

    private fun httpGet(url: String): String? = runCatching {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Aetherbound-UpdateChecker")
        }
        conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
    }.getOrNull()
}
