package com.aetherbound.game.render.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Lightweight audio engine. Two channels:
 *
 *   - **SFX** via [SoundPool] for short clips (≤5s): move sounds, UI clicks,
 *     monster cries, capture jingles, hits.
 *   - **Music** via [MediaPlayer] for streaming BGM (looped overworld music,
 *     battle themes).
 *
 * SFX assets live in `assets/audio/sfx/<slug>.ogg` (or .wav), music in
 * `assets/audio/music/<slug>.ogg`. Tuxemon doesn't ship these in our import
 * yet — when it does, drop them in those folders and the engine resolves
 * by slug automatically.
 */
class AudioEngine(private val ctx: Context) {

    /**
     * Active audio settings. Updated from [com.aetherbound.game.core.data.PlayerProgress.audio]
     * whenever the user changes them in [com.aetherbound.game.render.ui.SettingsScreen].
     * Both volume sliders are pre-multiplied with the per-call volume parameter.
     */
    var settings: com.aetherbound.game.core.data.AudioSettings =
        com.aetherbound.game.core.data.AudioSettings()
        set(value) {
            field = value
            // If music was playing and toggle just flipped to disabled → silence it.
            if (!value.musicEnabled) stopMusic()
            // Live-update current music volume.
            music?.runCatching {
                val v = (value.musicVolume).coerceIn(0f, 1f)
                setVolume(v, v)
            }
        }


    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(attrs)
        .build()

    private val sfxIds = ConcurrentHashMap<String, Int>()
    private var music: MediaPlayer? = null
    private var currentMusicSlug: String? = null

    /** Play an SFX. Loads on first use, then plays from the in-memory pool. */
    fun playSfx(slug: String, volume: Float = 1f) {
        if (!settings.sfxEnabled) return
        val id = sfxIds.getOrPut(slug) { loadSfx(slug) }
        if (id == 0) return  // load failed, fail silently
        val v = (volume * settings.sfxVolume).coerceIn(0f, 1f)
        pool.play(id, v, v, /* priority */ 0, /* loop */ 0, /* rate */ 1f)
    }

    private fun loadSfx(slug: String): Int {
        // Pack-aware: prefer external file (audio pack), fall back to APK assets.
        for (ext in listOf("ogg", "wav", "mp3")) {
            val path = "audio/sfx/$slug.$ext"
            // External pack file first
            val packFile = com.aetherbound.game.core.data.AssetPackDownloader.resolveFile(ctx, path)
            if (packFile != null) {
                runCatching { return pool.load(packFile.absolutePath, 1) }
            }
            // APK asset fallback
            try {
                val fd = ctx.assets.openFd(path)
                val id = pool.load(fd, 1)
                fd.close()
                return id
            } catch (_: Exception) { /* try next extension */ }
        }
        Log.w(TAG, "SFX not found: $slug")
        return 0
    }

    /**
     * Stream BGM, replacing any current track. No-op if [slug] is already
     * the active track.
     */
    fun playMusic(slug: String, loop: Boolean = true, volume: Float = 0.6f) {
        if (!settings.musicEnabled) return
        if (slug == currentMusicSlug) return
        stopMusic()
        val effectiveVolume = (volume * settings.musicVolume).coerceIn(0f, 1f)
        for (ext in listOf("ogg", "mp3")) {
            val path = "audio/music/$slug.$ext"
            // Pack-aware: prefer external file, fall back to APK asset FD.
            val packFile = com.aetherbound.game.core.data.AssetPackDownloader.resolveFile(ctx, path)
            try {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(attrs)
                    if (packFile != null) {
                        setDataSource(packFile.absolutePath)
                    } else {
                        val fd = ctx.assets.openFd(path)
                        setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                        fd.close()
                    }
                    isLooping = loop
                    setVolume(effectiveVolume, effectiveVolume)
                    prepare()
                    start()
                }
                music = mp
                currentMusicSlug = slug
                return
            } catch (_: Exception) { /* try next extension */ }
        }
        Log.w(TAG, "Music not found: $slug")
    }

    fun stopMusic() {
        music?.runCatching { stop(); release() }
        music = null
        currentMusicSlug = null
    }

    fun release() {
        stopMusic()
        pool.release()
        sfxIds.clear()
    }

    companion object { private const val TAG = "AudioEngine" }
}
