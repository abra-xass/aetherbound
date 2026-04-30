package com.aetherbound.game.render.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Compose-friendly handle on a singleton [AudioEngine] for the current
 * Activity. Acquires on first composition, releases when the composable
 * leaves the tree. SoundPool/MediaPlayer are heavy — never instantiate
 * one per scene; instead, hoist this once at the navigation root.
 *
 *     val audio = rememberAudioEngine()
 *     LaunchedEffect(scene) { audio.playMusic(AudioCatalog.bgmFor(...)) }
 */
@Composable
fun rememberAudioEngine(): AudioEngine {
    val ctx = LocalContext.current
    val engine = remember { AudioEngine(ctx) }
    DisposableEffect(engine) {
        onDispose { engine.release() }
    }
    return engine
}
