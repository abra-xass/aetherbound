# Aetherbound — Asset Credits & License Attribution

Aetherbound ships content imported from **Tuxemon**, an open-source
Pokémon-style monster RPG. We use Tuxemon's monsters, techniques, items,
sprites, maps, tilesets, and UI graphics as our base content layer.

## Tuxemon

- **Project**: <https://github.com/Tuxemon/Tuxemon>
- **Code license**: GPL-3.0
- **Asset license**: CC-BY-SA-3.0
- **Attribution**: The Tuxemon project and its many contributors. See
  Tuxemon's `AUTHORS` and `CREDITS.md` files for the full contributor list.

### What we import

Imported on **2026-04-29** via `tools/ai/import_tuxemon.py` from a clone of
the upstream `Tuxemon/Tuxemon` repository. The importer covers 8 phases:

| Phase | Category | Count | Source path | Aetherbound destination |
|------:|---------------------|------:|---------------------------------------------|----------------------------------------------|
| 1 | Data (YAML→JSON) | 411 monsters / 274 techniques / 223 items / 13 elements / 35 statuses | `tuxemon/db/{monster,technique,item,element,condition}/*.yaml` | `app/src/main/assets/game/data/*.json` |
| 2 | Monster sprites (front/back/menu) | 412 | `tuxemon/gfx/sprites/battle/*-sheet.png` (128×88, 2-frame layout — left=front 64×64, right=back 64×64 per Tuxemon's `MonsterSpritesModel.front_rect`/`back_rect`) | `app/src/main/assets/game/echoforms/tuxemon/<slug>/{front,back,menu}.png` |
| 3 | Item icons | 177 | `tuxemon/gfx/items/*.png` | `app/src/main/assets/game/items/tuxemon/` |
| 4 | Tilesets | 77 | `tuxemon/resources/gfx/tilesets/*.png` | `app/src/main/assets/game/tilesets/tuxemon/` |
| 5 | Maps (.tmx/.tsx) | 235 | `tuxemon/resources/maps/*.tmx` + `*.tsx` | `app/src/main/assets/game/maps/tuxemon/` |
| 6 | Move FX animations | 190 | `tuxemon/gfx/animations/technique/<slug>_NNN.png` | `app/src/main/assets/game/techniques/tuxemon/<slug>/frame_NNNN.png` |
| 7 | Character sprites | 357 | `tuxemon/gfx/sprites/player/*.png` + NPC sets | `app/src/main/assets/game/characters/tuxemon/<name>/` |
| 8 | UI graphics | 252 | `tuxemon/gfx/ui/**/*.png` | `app/src/main/assets/game/ui/tuxemon/` |

### What we modify or override

Aetherbound uses an **override-with-fallback** asset resolver
(`app/src/main/java/com/aetherbound/game/render/asset/AssetResolver.kt`).
For each asset id, it checks `overrides/<slug>/` first (our own work, our
license) then falls back to `tuxemon/<slug>/` (CC-BY-SA-3.0 originals).

See `docs/asset-structure.md` for the full directory layout. The
folder-level split makes attribution unambiguous:

- Anything under any `tuxemon/` subdirectory → CC-BY-SA-3.0, attribution
  required (this file).
- Anything under any `overrides/` subdirectory → © Aetherbound contributors,
  separate license terms (TBD).

### CC-BY-SA-3.0 obligations

Per the Creative Commons Attribution-ShareAlike 3.0 Unported license
(<https://creativecommons.org/licenses/by-sa/3.0/>):

1. **Attribution**: this file plus the in-game Credits screen will list
   "Tuxemon project — CC-BY-SA-3.0" as the source for all assets under
   `tuxemon/` directories.
2. **ShareAlike**: any derivative we publish that includes Tuxemon assets
   must itself be licensed CC-BY-SA-3.0 (or a compatible later version).
   Aetherbound's Tuxemon-derived content layer is therefore CC-BY-SA-3.0.
   Original Aetherbound code (`app/src/main/java/...`) and `overrides/`
   art are under our own license.
3. **No DRM**: the license forbids us from imposing technical measures
   that prevent users from exercising their rights under the license on
   the CC-BY-SA-3.0 portion of the assets.

### Element-system attribution

Aetherbound's 13-element type system (`Aspect.kt`: cosmic, earth, fire,
frost, heroic, lightning, metal, normal, shadow, sky, venom, water, wood)
and the full 13×13 multiplier chart in `AspectAffinity` are a 1:1 port of
Tuxemon's element database. Element slugs match Tuxemon's
`db/element/*.yaml` exactly so JSON-driven dex loaders can use the same
keys.

### Tuxemon code we did **not** import

- The Tuxemon Python game engine itself (GPL-3.0). Aetherbound is an
  independent Kotlin / Compose Android engine; we only import the asset
  layer and JSON schemas.
- Tuxemon's pygame/UI code, save-game format, or networking.

## Other content

- Aetherbound original art (concepts/, style-boards/, portraits/,
  overrides/) — © Aetherbound contributors.
- Particle textures generated via AI tooling under
  `tools/ai/generate_particle_textures.py` — see that file's header for
  per-asset license terms.
- Coastal pilot biome assets (`PilotMap.kt` etc.) — © Aetherbound.

## Updating this file

Whenever a new batch of upstream assets is imported, re-run
`tools/ai/import_tuxemon.py` and update the count column in the table
above. The importer also writes `app/src/main/assets/game/manifest.json`
with the per-phase tally for cross-reference.
