#!/bin/sh
# Pixel-art replacement for production sprites + battle backdrop.
# Style: GBA-era top-down RPG (Pokemon Ruby/Emerald aesthetic but original IP).
set -e
cd "$(dirname "$0")/../.."

NEG_CREATURE="painterly, 3D render, smooth gradients, blur, photo, watercolor, modern illustration, anime, chibi, multiple subjects, comic panel, sheet, grid, scenery background, text, UI, logo, watermark, signature, Pokemon-clone, Digimon-clone, copyrighted character"
NEG_ARENA="characters, creatures, monsters, people, painterly, 3D, photo, smooth gradients, modern illustration, text, UI, logo, watermark, frame, comic panel, sheet"

# Vulkid: ember salamander
python tools/ai/generate_wave01.py --inline \
  --bucket echoforms --filename E001.png \
  --width 512 --height 512 --seed 142 \
  --prompt "Vulkid, original ember-aspect collectible monster sprite, lithe quadruped salamander silhouette with glowing tail ember and brow flame plume, single creature centered on solid pure black background, polished 16-bit GBA-era pixel art battle sprite at 96x96 effective resolution, sharp limited 32-color palette, side-view facing right, retro 2D monster RPG creature art, original character not derived from any IP, bold readable silhouette, flat shading with subtle dithering" \
  --negative "$NEG_CREATURE"

# Reeva: tide guardian
python tools/ai/generate_wave01.py --inline \
  --bucket echoforms --filename E002.png \
  --width 512 --height 512 --seed 242 \
  --prompt "Reeva, original tide-aspect collectible monster sprite, river-guardian creature with finned cheeks and a single luminous water-droplet crest above its head, single creature centered on solid pure black background, polished 16-bit GBA-era pixel art battle sprite at 96x96 effective resolution, sharp limited 32-color palette, side-view facing left, retro 2D monster RPG creature art, original character not derived from any IP, bold readable silhouette, flat shading with subtle dithering" \
  --negative "$NEG_CREATURE"

# Battle arena: coastal harbor scene
python tools/ai/generate_wave01.py --inline \
  --bucket cities --filename namaris_harbor.png \
  --width 1024 --height 768 --seed 342 \
  --prompt "Classic GBA-era pixel art battle scene background, dark stormy coastal harbor arena, wet black stone foreground platform with golden brass mooring bollards, teal sea reflection in middle distance, distant wooden dock buildings and a tall lighthouse silhouette against a stormy sky, hanging brass lanterns glowing warm gold, retro 2D RPG battle stage in classic 16-bit pixel art aesthetic, limited palette, sharp pixels, no characters, no UI, single composed scene only, cohesive Asterfall coastal world tone" \
  --negative "$NEG_ARENA"

echo "DONE"
