# Thot Fusion / Aetherbound Preparation Pack

This folder is a non-production preparation workspace for building the Thot in-game creature RPG described in `docs/superpowers/specs/2026-04-25-thot-fusion-rpg-design.md`.

It intentionally lives outside the normal Kotlin source set because the folder name contains a space and the current request is to prepare content and systems, not wire the feature into the app build yet.

## Contents

- `content/` - source-of-truth game content seeds: aspects, cities, creatures, items, NPC archetypes, story, maps, graphics manifests, and animation bindings.
- `engine/` - Kotlin-like pure-domain templates for later extraction into a real game module.
- `graphics/` - SVG placeholder art and generated atlases for all 300 Echoforms, 180 Items, NPC variants, world tiles, the region map, and battle backgrounds.
- `tests/` - behavior contracts for the future deterministic game core.
- `docs/` - implementation notes and import order.
- `docs/graphics-visual-bible.md` - replacement visual direction for hybrid world art, Echoforms, VFX, and first production asset priorities.
- `docs/graphics-production-briefings-and-prompts.md` - concrete production briefings and AI prompt seeds derived from the visual bible.
- `docs/graphics-wave-01-prompt-pack.json` - first executable-style prompt wave for style boards, region concepts, starter concepts, and initial VFX packs.

## Build Direction

1. Move pure files from `engine/` into a future package such as `app/src/main/java/com/thot/messenger/game/core`.
2. Move JSON content into `app/src/main/assets/game/` once the runtime loader exists.
3. Move SVGs either into Android vector drawables or convert them into raster sprites.
4. Keep battle and fusion logic pure Kotlin so Matrix PvP can replay event logs deterministically.

## Generated Asset Sheets

- `graphics/echoform_sprite_atlas.svg` - 300 ID-keyed creature placeholders from `E001` through `E300`.
- `graphics/item_catalog_atlas.svg` - 180 ID-keyed inventory icons from `IT001` through `IT180`.
- `graphics/npc_binder_sprite_atlas.svg` - 240 reusable NPC overworld variants; the 2,000 NPC rows map to them deterministically.
- `graphics/world_map_overview.svg` - first Asterfall region overview with all 30 cities and leaders.
- `content/techniques_450.csv` - 450 technique rows across the 12 Elements.
- `content/echoform_learnsets_300.csv` - 3,600 level-up learn rows, 12 per Echoform, restricted to matching Elements.
- `content/encounter_tables_seed.csv` - 300 non-legendary encounter seed rows across 30 city areas.
- `content/npc_teams_2000.csv` - battle teams for the full 2,000 NPC pool with legendary Echoforms excluded.
- `content/balance_rules.json` - NPC population split, legendary rules, difficulty curve, XP pacing, and balance guardrails.
- `content/animations/technique_animation_map.csv` - 450 technique rows mapped to unique animation IDs.
- `content/animations/attack_animation_manifest.json` - 450 attack animation definitions.
- `content/animations/creature_animation_manifest.json` - shared animation states for all 300 Echoforms.
- `content/animations/world_animation_manifest.json` - animated world tile, weather, puzzle, and NPC state definitions.
- `content/world_movement.json` - player movement speeds, rendering layers, Signal Wheel, Loom Jump, and 30 city Seal rewards.

## Non-Negotiables

- Full roster target: 300 Echoforms.
- Legendary target: 15 total; 12 legendary and 3 very legendary.
- World target: 30 cities and 30 Arenas/Sanctums.
- NPC target: 2,000 battle-capable Binders as a pool, split into 700 fixed world battles, 200 optional rematch/postgame battles, and 1,100 simulation/Matrix-season opponents.
- NPC teams never use legendary or very legendary Echoforms.
- Levels: 1 to 100.
- Party size: 6 active Echoforms.
- Multiplayer identity: Matrix ID.
- Campaign: offline-first.
- PvP: direct Matrix-ID challenge, deterministic turns, commit/reveal.
