# Thot Fusion Graphics Visual Bible

This document replaces the earlier weak placeholder direction for creatures, world, and attack visuals. The existing SVG atlases and boards in `graphics/` remain useful only as rough experiments, IDs, and content maps. They are not the target final look.

## Goal

Build a hybrid presentation style for the game:

- Overworld and exploration: high-quality pixel art or HD-2D tile/sprite presentation.
- Battles: richer, more detailed 2D battle sprites and stronger VFX.
- Portraits, leaders, legendaries, loading art: more illustrative premium layer.
- UI and VFX: modern and clean, readable first, stylish second.

The target is not "retro because old Pokemon used pixels". The target is a modern, cohesive 2D monster RPG with strong silhouettes, readable combat, and a darker Asterfall identity.

## Current Asset Review

Files already present in `graphics/` fall into three buckets.

Good direction:

- `creature_starters.svg`: best current shape language; the only file that shows recognizable creature personality.
- `world_tiles.svg`: useful for biome/material direction, not a final tileset.
- `attack_animations.svg`: useful as an element moodboard, not as final in-game animation.

Useful but not final:

- `battle_backgrounds.svg`
- `world_map_overview.svg`

These work as concept boards, not production-ready game assets.

Placeholder only:

- `echoform_sprite_atlas.svg`
- `npc_binder_sprite_atlas.svg`
- most icon/UI placeholder sheets

These should not define final art quality.

## Hybrid Style Rules

### 1. Overworld

The world uses a top-down or slight three-quarter-top-down RPG camera with tile-based navigation and smooth movement.

Visual rules:

- Final world art should be readable at play speed, not painterly noise.
- Tiles must feel hand-built, not icon-like.
- Surfaces need visible material identity: wet stone, worn wood, brass, oxidized metal, crystal ice, black sand, signal glass.
- Light sources matter. Lanterns, relay beacons, sanctum seals, and storm flashes should shape the mood of each scene.
- Each city must be visually recognizable from screenshots without reading text.

Do not use:

- flat color blocks as final surfaces
- generic vector icons as tiles
- overly tiny decorative detail that disappears in motion

### 2. Echoforms

Echoforms must look like collectible creatures, not abstract badges.

Visual rules:

- Each species needs a strong silhouette first.
- Each species needs two or three defining physical cues.
- Anatomy should feel intentional even when fantastical.
- Element identity should be expressed through form, posture, texture, and accent effects, not only palette swaps.
- Evolutions must visibly belong to the same lineage.

For each species family, the final graphics stack should support:

- overworld sprite
- battle sprite
- optional portrait/codex art
- faint/hit/idle animation hooks

### 3. Battles

Battle scenes are a premium layer over the overworld.

Rules:

- Battle sprites are larger and more detailed than overworld sprites.
- Backgrounds have depth and atmosphere, not just a flat stage.
- VFX must be high contrast and fast to parse.
- Impacts should feel physical: recoil, flash, burst, cut, shatter, ripple, pulse.
- The creature remains readable during effects.

### 4. Portrait Layer

Important Echoforms, leaders, rival characters, and legendary scenes should use a richer illustration layer.

Use cases:

- codex hero entries
- leader introduction splash
- legendary ritual scenes
- chapter cards
- loading art

This layer can be more painted and dramatic than the main game sprites, as long as proportions and identity remain consistent.

## Element VFX Language

Each element needs a distinct motion language, not only a different color.

- `Ember`: heat bloom, ash, ignition arcs, pressure flashes
- `Tide`: surge lines, droplets, splash curtains, flow ribbons
- `Verdance`: roots, spores, fronds, growth bursts
- `Stone`: fracture lines, dust plumes, debris bursts, ground heave
- `Gale`: cutting arcs, stream lines, spiral gusts, lift effects
- `Spark`: chain bolts, jitter flicker, discharge snaps, node jumps
- `Frost`: crystal bloom, vapor, shard bursts, freeze crawl
- `Metal`: clang sparks, plate slides, cutting glints, resonance rings
- `Shade`: smoke folds, pull-in darkness, veil trails, soft distortion
- `Radiant`: beam lines, halos, lens flare shards, cleansing light
- `Mind`: sigils, pulse rings, refraction, prediction overlays
- `Echo`: resonance rings, waveform trails, vibration pulses, signal noise

## Production Sizes

Recommended production baseline:

- world tiles: `32x32` or `48x48`
- player/NPC sprites: `32x48` or `48x64`
- small overworld Echoforms: `32x32` to `48x48`
- battle Echoforms: authored around `128x128` to `256x256`
- portrait art: free-size premium layer

Recommended direction:

- use `32x32` or `48x48` for world production
- do not upscale overworld sprites into battle art
- do not derive final portraits from tiny sprite assets

## Region Look Targets

Three world anchors should define the final look before full asset production starts.

### Namaris Harbor

- wet black stone
- teal tide reflections
- gold lantern accents
- tide pools and ropes
- storm harbor atmosphere

### Glassreed Crossing

- marsh greens and dark water
- reed silhouettes
- reflective mud
- fog and bioluminescent signal plants
- unstable boardwalk mood

### Myr Vault City

- archive stone
- suspended bridges
- glyph light
- metallic library structures
- mind/echo visual identity

## Asset Production Priority

Do not attempt 300 creatures or full world coverage first. Production should be layered.

### Phase 1: Style Master

1. Overworld style board
2. Battle style board
3. Echoform style board
4. VFX style board
5. Color and light boards for coast, marsh, archive
6. Proportion sheets for player/NPC/Echoforms

### Phase 2: World Base

1. base ground tiles
2. path and edge tiles
3. water tiles
4. grass/encounter tiles
5. coast biome set
6. marsh biome set
7. archive biome set
8. three city building kits
9. four core interior kits
10. one arena interior base

### Phase 3: Character Base

1. player sprite sheet
2. player battle/stand sprite
3. reusable NPC archetype system
4. first five story character sheets

### Phase 4: Echoform Base

1. three starters complete
2. ten early route Echoforms
3. ten midgame Echoforms
4. five elite/boss Echoforms
5. three very legendary Echoforms

### Phase 5: VFX Base

1. twelve element base packs
2. hit, projectile, aura, field, and status variants per element
3. reusable battle hit language for 450 techniques

## First 50 Priority Assets

The first 50 assets to build are:

1. Overworld style board
2. Battle style board
3. Echoform style board
4. VFX style board
5. Coast color/light board
6. Marsh color/light board
7. Archive color/light board
8. Material board
9. Player/NPC proportion sheet
10. Echoform proportion sheet
11. Base ground tileset
12. Path and edge tileset
13. Water tileset
14. Grass encounter tileset
15. Coast decor set
16. Marsh decor set
17. Archive decor set
18. Namaris building set
19. Glassreed building set
20. Myr Vault building set
21. Lab interior set
22. Shop interior set
23. Heilstation interior set
24. Standard house interior set
25. Arena interior base
26. Player walk sprite sheet
27. Player battle/stand sprite
28. NPC archetype 1
29. NPC archetype 2
30. NPC archetype 3
31. NPC archetype 4
32. Dr. Ilya Voss sprite/portrait
33. Mara Keir sprite/portrait
34. Senn Vale sprite/portrait
35. Archivist Orun sprite/portrait
36. Vulkid overworld sprite
37. Vulkid battle sprite
38. Reeva overworld sprite
39. Reeva battle sprite
40. Mosslyn overworld sprite
41. Mosslyn battle sprite
42. Early route Echoform 1
43. Early route Echoform 2
44. Early route Echoform 3
45. Early route Echoform 4
46. Ember VFX base pack
47. Tide VFX base pack
48. Verdance VFX base pack
49. Coast battle background
50. Marsh battle background

## Practical Conclusion

The old graphics should not be refined. They should be treated as discarded experiments plus metadata helpers.

The final production path is:

1. lock the hybrid style
2. lock three reference biomes
3. lock starter-quality Echoforms
4. lock twelve VFX element packs
5. scale from that base

Anything else will recreate the same inconsistency problem.
