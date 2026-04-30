package com.aetherbound.game.core.data

import android.content.Context
import com.aetherbound.game.core.Weather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL

/**
 * Pulls the [Open-Meteo-driven daily pack](../../../../../../scripts/daily_pack_build.py)
 * once per game launch (or once per hour, whichever comes first).
 *
 * Source: `https://raw.githubusercontent.com/abra-xass/aetherbound/main/content/daily-pack.json`
 *
 * Schema (matches `scripts/daily_pack_build.py`):
 * ```
 * {
 *   "ts_utc": "2026-04-30T15:00:00Z",
 *   "weather": "RAIN",            // matches Weather enum
 *   "weather_de": "Regen",
 *   "temp_c": 14.2,
 *   "humidity": 76,
 *   "wind_kmh": 8.4,
 *   "city": "Zurich",
 *   "world_event": { "tag": "rain_burst", "text": "Regen ueber Zuerich — Aether-Otter raus." }
 * }
 * ```
 *
 * **Effects when fetched successfully:**
 *   - [AmbientTime.forcedWeather] is set → encounter pool boosts shift
 *     to match real Zurich weather
 *   - The "world_event" text becomes a Tracery slot any NPC can drop
 *     into dialogue (`#world_event_text#`)
 *
 * **Privacy / Tor compliance:**
 *   - Pulls a static file from GitHub's CDN — no telemetry, no
 *     user-identifying request.
 *   - User-Agent is generic.
 *   - Cached locally for 1 hour so we don't hammer GitHub.
 */
object DailyPackFetcher {

    private const val PACK_URL =
        "https://raw.githubusercontent.com/abra-xass/aetherbound/main/content/daily-pack.json"
    private const val CACHE_FILE = "daily-pack-cache.json"
    private const val MAX_CACHE_AGE_MS = 60 * 60 * 1000L    // 1 hour

    /** What the resolver hands back. */
    data class Pack(
        val weather: Weather,
        val weatherDe: String,
        val tempC: Double,
        val humidity: Int,
        val city: String,
        val worldEventTag: String?,
        val worldEventText: String?,
        val timestampMs: Long,
    )

    /**
     * Fetch + cache. If the network is unreachable or returns junk, falls
     * back to the last cached pack on disk; if there's no cache either,
     * returns a CLEAR-weather default so the game still works.
     */
    suspend fun refresh(ctx: Context): Pack = withContext(Dispatchers.IO) {
        val cached = loadCache(ctx)
        // If cache is fresh enough, skip network entirely — saves Tor/data.
        if (cached != null && System.currentTimeMillis() - cached.timestampMs < MAX_CACHE_AGE_MS) {
            applyToWorld(cached)
            return@withContext cached
        }
        val networked = runCatching {
            URL(PACK_URL).openConnection().apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "aetherbound-android/1.0")
            }.getInputStream().use { it.readBytes().toString(Charsets.UTF_8) }
        }.getOrNull()

        val pack = if (networked != null) {
            val parsed = parse(networked)
            saveCache(ctx, networked)
            parsed
        } else {
            cached ?: defaultClear()
        }
        applyToWorld(pack)
        pack
    }

    /** Convenience: get the most recent pack from cache without networking. */
    fun cached(ctx: Context): Pack? = loadCache(ctx)

    // ── plumbing ────────────────────────────────────────────────────

    private fun applyToWorld(pack: Pack) {
        // Live-set the world's weather. Encounter pool re-rolls match
        // this on next encounter. NPC dialog Tracery picks it up via
        // DialogueContext snapshot.
        AmbientTime.forcedWeather = pack.weather
    }

    private fun parse(text: String): Pack {
        val o = JSONObject(text)
        val we = o.optJSONObject("world_event")
        val ts = runCatching {
            // ISO-8601 -> epoch millis
            val iso = o.optString("ts_utc", "")
            if (iso.isBlank()) System.currentTimeMillis()
            else java.time.Instant.parse(iso).toEpochMilli()
        }.getOrDefault(System.currentTimeMillis())
        return Pack(
            weather = Weather.fromName(o.optString("weather", "CLEAR")),
            weatherDe = o.optString("weather_de", "klar"),
            tempC = o.optDouble("temp_c", 12.0),
            humidity = o.optInt("humidity", 70),
            city = o.optString("city", "Zurich"),
            worldEventTag = we?.optString("tag")?.takeIf { it.isNotBlank() },
            worldEventText = we?.optString("text")?.takeIf { it.isNotBlank() },
            timestampMs = ts,
        )
    }

    private fun defaultClear(): Pack = Pack(
        weather = Weather.CLEAR,
        weatherDe = "klar",
        tempC = 12.0,
        humidity = 70,
        city = "Zurich",
        worldEventTag = null,
        worldEventText = null,
        timestampMs = System.currentTimeMillis(),
    )

    private fun cacheFile(ctx: Context): File =
        File(ctx.filesDir, "aetherbound").apply { mkdirs() }.let { File(it, CACHE_FILE) }

    private fun loadCache(ctx: Context): Pack? {
        val f = cacheFile(ctx)
        if (!f.exists()) return null
        return runCatching { parse(f.readText(Charsets.UTF_8)) }.getOrNull()
    }

    private fun saveCache(ctx: Context, raw: String) {
        runCatching {
            val f = cacheFile(ctx)
            val tmp = File(f.parentFile, "${f.name}.tmp")
            tmp.writeText(raw, Charsets.UTF_8)
            f.delete()
            tmp.renameTo(f)
        }
    }
}
