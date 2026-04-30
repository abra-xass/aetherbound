package com.aetherbound.game.render.audio

import com.aetherbound.game.core.ScreenTheme
import com.aetherbound.game.core.data.DayNightPhase

/**
 * Slug catalog for the imported Tuxemon audio. The importer (Phase 9)
 * flattens the `mods/tuxemon/sounds/<subdir>/<file>` tree into
 * `assets/audio/sfx/<subdir>_<file>.ogg`, so the slugs here are
 * subdir-prefixed (`combat_*`, `interface_*`, `monster_*`).
 *
 * Music files are named by track title — slugs match the original Tuxemon
 * filename (lower_snake_case, spaces→underscores). The full list is
 * 111 tracks; we curate the most useful ones below and the rest can be
 * looked up by slug at runtime.
 */
object AudioCatalog {

    // ── Interface SFX ────────────────────────────────────────────────
    const val SFX_MENU_CONFIRM = "interface_confirm"
    const val SFX_MENU_SELECT = "interface_menu_select"
    const val SFX_MENU_CLICK = "interface_nenadsimic_click"
    const val SFX_BLIP = "ding"

    // ── Combat SFX ───────────────────────────────────────────────────
    const val SFX_BATTLE_HIT = "combat_falling_macro"
    const val SFX_FAINT = "monster_birdsad"

    // ── Monster cries (slugs follow `monster_<basename>` convention) ─
    fun monsterCry(slug: String): String? = when {
        slug.contains("dragon") -> "monster_growl"
        slug.contains("bird") -> "monster_birdsad"
        slug.contains("bug") -> "monster_bug_03"
        slug.contains("baby") -> "monster_babyanimal"
        slug.contains("pony") -> "monster_babypony"
        else -> null
    }

    // ── Music by ScreenTheme + DayNightPhase ─────────────────────────
    /**
     * Pick a BGM slug for the current biome and time-of-day. Falls back
     * to a generic overworld track if nothing more specific exists.
     */
    fun bgmFor(theme: ScreenTheme, phase: DayNightPhase): String {
        // Night-only tracks
        if (phase == DayNightPhase.NIGHT || phase == DayNightPhase.DUSK) {
            return when (theme) {
                ScreenTheme.Forest, ScreenTheme.Marsh -> "06_rebels_be"
                ScreenTheme.Cave -> "10_the_empire"
                else -> "01_opening"
            }
        }
        return when (theme) {
            ScreenTheme.Beach, ScreenTheme.Harbor, ScreenTheme.WaterSurface -> "08_overworld"
            ScreenTheme.Forest, ScreenTheme.Marsh -> "07_town"
            ScreenTheme.Mountain, ScreenTheme.Cave -> "10_the_empire"
            ScreenTheme.Desert -> "11_ostrich_"
            ScreenTheme.Castle, ScreenTheme.Sanctum -> "04_sanctuary"
            ScreenTheme.Town -> "07_town"
            ScreenTheme.Plains -> "08_overworld"
        }
    }

    /** BGM for a battle scene. */
    fun bgmForBattle(isLegendary: Boolean = false, isTrainer: Boolean = false): String = when {
        isLegendary -> "boss__introduction"
        isTrainer -> "05_reunion"
        else -> "02_bwv_1007_prelude"
    }
}
