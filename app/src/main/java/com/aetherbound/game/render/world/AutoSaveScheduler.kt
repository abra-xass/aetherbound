package com.aetherbound.game.render.world

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.aetherbound.game.core.data.SaveGame
import com.aetherbound.game.core.data.SaveGameIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Drives the 10-minute auto-save tick from inside [TuxemonWorldScene].
 *
 * Each tick:
 *   1. Wakes after [SaveGameIO.AUTO_SAVE_INTERVAL_MS] (= 10 min)
 *   2. Builds a [SaveGame] from the current world state via [snapshotProvider]
 *   3. Calls [SaveGameIO.pushAutoRing] — overwrites the oldest of 6 slots
 *   4. Toggles [savingFlash] briefly so the UI gold-diskette indicator
 *      flashes for ~600 ms, then auto-resets.
 *
 * The scheduler is killed when the world scene leaves composition, so it
 * doesn't fire in the title screen or during battles. That's intentional —
 * battles have their own post-match auto-save (`saveAuto`), and the title
 * screen has nothing to save.
 *
 * Usage in TuxemonWorldScene:
 * ```
 * val savingFlash = remember { mutableStateOf(false) }
 * AutoSaveScheduler(
 *     savingFlash = savingFlash,
 *     snapshotProvider = { buildSaveGameFromCurrentState() },
 * )
 * SaveDiskIndicator(visible = savingFlash.value, ...)
 * ```
 */
@Composable
fun AutoSaveScheduler(
    savingFlash: MutableState<Boolean>,
    onAutoSaveRequested: suspend () -> Unit,
) {
    LaunchedEffect(Unit) {
        // First save: 10 min after entering the world (not immediately —
        // gives the player time to actually do something worth saving).
        while (true) {
            delay(SaveGameIO.AUTO_SAVE_INTERVAL_MS)
            onAutoSaveRequested()
            // Flash the gold-disk indicator briefly.
            savingFlash.value = true
            delay(600)
            savingFlash.value = false
        }
    }
}

/**
 * One-shot helper — used by manual "Save Now" buttons + the auto-tick.
 * Runs disk I/O off the main thread, then flashes the indicator briefly.
 */
suspend fun performAutoSave(
    ctx: Context,
    snapshotProvider: () -> SaveGame,
    savingFlash: MutableState<Boolean>,
) {
    val snap = snapshotProvider()
    withContext(Dispatchers.IO) {
        SaveGameIO.pushAutoRing(ctx, snap)
    }
    // Flash the indicator: 600 ms visible, then off.
    savingFlash.value = true
    delay(600)
    savingFlash.value = false
}

/**
 * Same as [performAutoSave] but writes the persistent manual slot. Used
 * when the player taps "Speichern jetzt" in the in-game menu.
 */
suspend fun performManualSave(
    ctx: Context,
    snapshotProvider: () -> SaveGame,
    savingFlash: MutableState<Boolean>,
): Boolean {
    val snap = snapshotProvider()
    val ok = withContext(Dispatchers.IO) {
        SaveGameIO.saveManual(ctx, snap)
    }
    savingFlash.value = true
    delay(900)    // longer for manual — feels more deliberate
    savingFlash.value = false
    return ok
}
