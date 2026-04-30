package com.aetherbound.game.render.battle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.aetherbound.game.core.data.TrainerSpec
import com.aetherbound.game.render.audio.AudioCatalog
import com.aetherbound.game.render.audio.LocalAudioEngine
import com.aetherbound.game.render.ui.TrainerBattleIntro

/**
 * Wraps a trainer-battle: shows [TrainerBattleIntro] as a cinematic
 * pre-roll, then hands off to [BattleScene] with the trainer's first
 * Echoform as the opponent.
 *
 * Audio:
 *   - intro plays nothing (lets the typewriter SFX feel quiet)
 *   - on START → switches BGM to `bgmForBattle(isTrainer = true)`
 *   - on EXIT  → caller's parent restores world BGM via its own LaunchedEffect
 */
@Composable
fun TrainerBattleHost(
    trainer: TrainerSpec,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var introDone by remember(trainer.id) { mutableStateOf(false) }
    val audio = LocalAudioEngine.current

    LaunchedEffect(introDone) {
        if (introDone) {
            audio?.playMusic(AudioCatalog.bgmForBattle(isTrainer = true))
        }
    }

    if (!introDone) {
        TrainerBattleIntro(
            trainer = trainer,
            onStart = { introDone = true },
            modifier = modifier,
        )
    } else {
        // Hand off to the Tuxemon-driven BattleScene with the trainer's
        // first Echoform as the opponent.
        val first = trainer.team.firstOrNull()
        BattleScene(
            onExit = onExit,
            modifier = modifier,
            tuxemonOpponentSlug = first?.slug,
            tuxemonOpponentLevel = first?.level ?: 5,
            tuxemonPlayerSlug = "agnidon",
            tuxemonPlayerLevel = first?.level?.plus(2) ?: 5,
        )
    }
}
