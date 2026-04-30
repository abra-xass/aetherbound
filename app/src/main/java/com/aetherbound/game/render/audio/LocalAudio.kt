package com.aetherbound.game.render.audio

import androidx.compose.runtime.compositionLocalOf

/**
 * Composition-scoped access to the activity's [AudioEngine]. The activity
 * provides this once at the top of its scene tree:
 *
 *     val engine = rememberAudioEngine()
 *     CompositionLocalProvider(LocalAudioEngine provides engine) {
 *         GamePreviewRoot(...)
 *     }
 *
 * Inside any descendant composable:
 *
 *     val audio = LocalAudioEngine.current
 *     audio.playSfx(AudioCatalog.SFX_MENU_CONFIRM)
 *
 * Returns null when no engine is provided (e.g. in unit tests / previews).
 */
val LocalAudioEngine = compositionLocalOf<AudioEngine?> { null }
