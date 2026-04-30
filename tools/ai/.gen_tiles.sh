#!/bin/sh
# Phase-2 production tileset — GBA-era pixel-art style (per user reference:
# Pokemon Ruby/Emerald look). Sharp pixels, limited palette, top-down,
# tile-friendly, no painterly anti-aliasing. Each tile 256x256 (rendered
# down to ~48px on phone for crisp pixel feel).
set -e
cd "$(dirname "$0")/../.."

STYLE="classic GBA-era 48x48 pixel art top-down tile, polished 16-bit RPG aesthetic in the late SNES/GBA tradition, limited 32-color palette, sharp clean pixels, flat shading with subtle dithering, seamless tileable edges that blend with neighbours, no border, no outline frame, no UI text, no logo, no watermark, original art not derived from any existing IP"
NEG="painterly, 3D render, smooth gradients, blur, soft shadows, anti-aliasing artifacts, photo, modern illustration, watercolor, oil painting, perspective view, cell shading, vector illustration, AI smooth, character, monster, person, text, UI, logo, watermark, frame, multiple tiles, grid, perspective"

gen() {
  python tools/ai/generate_wave01.py --inline \
    --bucket tiles --filename "$1.png" \
    --width 256 --height 256 --seed "$2" \
    --prompt "$3, $STYLE" \
    --negative "$NEG"
}

# Per Visual Bible coastal-biome tile dictionary (matches CoastalTile enum)
gen sand              201 "warm sandy beach floor"
gen grass             202 "lush green grass field"
gen grass_tall        203 "tall green grass with darker tufts as wild encounter zone"
gen path              204 "packed dirt walking path"
gen path_stone        205 "grey cobblestone path"
gen pier              206 "weathered wooden pier planks running horizontally"
gen pier_edge         207 "wooden pier plank meeting dark teal water"
gen sea_deep          208 "deep teal blue sea water with subtle wave dithering"
gen sea_shallow       209 "shallow turquoise sea water with light pixel sparkles"
gen shore_foam        210 "white wave foam edge meeting beach"
gen crate             211 "single brown wooden crate centered on grass background"
gen barrel_lamp       212 "single dark wooden barrel with hanging brass lantern centered on grass background"
gen bollard_gold      213 "single golden brass mooring bollard centered on stone path"
gen roof_tile         214 "dark red slate roof shingles row"
gen wall              215 "cream stucco wall with dark wood timber framing"
gen door              216 "single dark wooden double door set into cream stucco wall, centered"
gen window            217 "single window with brass frame and blue glass set into cream stucco wall, centered"
gen flag_pole         218 "single wooden flagpole with golden pennant flag centered on grass background"
gen hedge             219 "dense dark green hedge bushes"
gen stair             220 "weathered grey stone steps top-down"
gen lantern           221 "single tall brass lantern with warm yellow flame centered on stone background"
gen bench             222 "single brown wooden bench centered on grass background"
gen sign              223 "single dark wooden signboard on a post centered on grass background"
gen seal_stone        224 "carved obsidian disc with glowing golden glyph set into stone pavement"
gen empty_shadow      225 "deep black shadow void"

echo "DONE"
