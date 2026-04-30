#!/bin/sh
# Phase-4 production-sprite & Phase-2 production-arena generation
# (single-subject, isolated, drop-in for the runtime asset resolver)
set -e
cd "$(dirname "$0")/../.."

NEG_CREATURE="Pokemon clone, Digimon clone, anime, chibi, 3D render, photo, scenery background, text, logo, multiple subjects, frame, watermark, signature, multiple creatures, comic panel, collage, sheet, grid"
NEG_ARENA="characters, creatures, monsters, people, text, UI, HUD, logo, watermark, frame, multiple scenes, comic panel, collage, sheet"

# E001 Vulkid (Ember starter)
python tools/ai/generate_wave01.py --inline \
  --bucket echoforms --filename E001.png \
  --width 1024 --height 1024 --seed 142 \
  --prompt "Vulkid, original ember-aspect collectible creature for a hybrid 2D monster RPG, salamander silhouette with a glowing tail ember and brow flame plume, full-body three-quarter view facing right, single isolated subject centered on solid pure black background, dark elegant fantasy art direction, premium creature concept art, strong silhouette, painted texture, warm rim light from upper-right, no scenery, no text" \
  --negative "$NEG_CREATURE"

# E002 Reeva (Tide starter)
python tools/ai/generate_wave01.py --inline \
  --bucket echoforms --filename E002.png \
  --width 1024 --height 1024 --seed 242 \
  --prompt "Reeva, original tide-aspect collectible creature for a hybrid 2D monster RPG, river-guardian silhouette with finned cheeks and a single luminous water-droplet crest, full-body three-quarter view facing left, single isolated subject centered on solid pure black background, dark elegant fantasy art direction, premium creature concept art, strong silhouette, painted texture, cool rim light from upper-left, no scenery, no text" \
  --negative "$NEG_CREATURE"

# Namaris Harbor (single battle arena, 16:9)
python tools/ai/generate_wave01.py --inline \
  --bucket cities --filename namaris_harbor.png \
  --width 1920 --height 1080 --seed 342 \
  --prompt "Namaris Harbor battle arena, single dramatic stage scene, wet black stone foreground platform, teal storm-tinted water reflections in middle distance, distant brass relay towers and dock silhouettes against stormy sky, hanging lanterns glowing warm gold, atmospheric depth, painted RPG battle background, no characters, no UI, single composed scene only, premium hybrid 2D game environment painting" \
  --negative "$NEG_ARENA"

echo "DONE"
