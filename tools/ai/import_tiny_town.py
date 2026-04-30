#!/usr/bin/env python3
"""
Import Kenney Tiny Town tiles (16×16, CC0) into our 25 CoastalTile slots.

The mapping below is a best-guess based on the typical Kenney Tiny Town v1
layout (132 tiles in a 12-row × 11-col grid). After importing, run the game
and visually verify each tile; if wrong, edit MAPPING and re-run.

Usage: py -3 tools/ai/import_tiny_town.py "C:/Users/Amir/Downloads/kenney_tiny-town.zip"
"""
from __future__ import annotations
import sys, zipfile, shutil
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
DEST = REPO / "app" / "src" / "main" / "assets" / "game" / "tiles"

# Slot name (matches CoastalTile enum, lower_snake_case) -> tile index (0..131)
# These are best-guesses based on standard Tiny Town layout. Refine after seeing in-game.
MAPPING: dict[str, int] = {
    # Ground
    "grass":        0,    # plain grass
    "grass_tall":   3,    # grass with flowers / detail (encounter zone)
    "sand":         24,   # beach sand
    "path":         30,   # dirt path
    "path_stone":   36,   # stone road
    # Water
    "sea_deep":     14,   # deep water
    "sea_shallow":  12,   # shallow water
    "shore_foam":   13,   # water edge / foam
    # Pier (use stone path for pier surface, wood for pier_edge)
    "pier":         84,   # wood / floor
    "pier_edge":    85,   # wood edge
    # Decoration / props
    "crate":        96,   # crate or barrel
    "barrel_lamp":  98,   # barrel
    "bollard_gold": 102,  # post / pillar
    "lantern":      99,   # lamp / lantern
    "bench":        97,   # bench
    "sign":         103,  # sign
    "flag_pole":    100,  # pole / flag
    "hedge":        60,   # bush
    "seal_stone":   66,   # rock / stone
    # Buildings
    "roof_tile":    72,   # roof
    "wall":         84,   # wall
    "door":         90,   # door
    "window":       91,   # window
    # Misc
    "stair":        42,   # stone steps / path detail
    "empty_shadow": 1,    # plain grass (shadow tiles will sit on top)
}


def main(zip_path: str) -> int:
    src = Path(zip_path)
    if not src.is_file():
        print(f"FATAL: missing zip {src}", file=sys.stderr)
        return 2
    DEST.mkdir(parents=True, exist_ok=True)

    with zipfile.ZipFile(src) as z:
        names = z.namelist()
        # Build lookup: index -> internal zip path
        tile_paths: dict[int, str] = {}
        for n in names:
            stem = Path(n).stem  # e.g. "tile_0042"
            if stem.startswith("tile_") and n.endswith(".png"):
                idx = int(stem.split("_")[1])
                tile_paths[idx] = n

        print(f"found {len(tile_paths)} tile_*.png files in zip")

        ok, missing = 0, []
        for slot, idx in MAPPING.items():
            zip_entry = tile_paths.get(idx)
            if zip_entry is None:
                missing.append((slot, idx))
                continue
            target = DEST / f"{slot}.png"
            with z.open(zip_entry) as src_f, open(target, "wb") as dst_f:
                shutil.copyfileobj(src_f, dst_f)
            ok += 1

        # Also copy the full tilemap.png for reference
        tilemap_entry = next((n for n in names if n.endswith("tilemap_packed.png")), None) \
                     or next((n for n in names if n.endswith("tilemap.png")), None)
        if tilemap_entry:
            with z.open(tilemap_entry) as src_f, open(DEST / "_tilemap_reference.png", "wb") as dst_f:
                shutil.copyfileobj(src_f, dst_f)
            print(f"  (also saved {tilemap_entry} as _tilemap_reference.png for visual mapping)")

        print(f"-- done | mapped {ok}/{len(MAPPING)} slots --")
        if missing:
            print(f"  missing tile indices: {missing}")
        return 0 if not missing else 1


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("usage: py -3 import_tiny_town.py <zip>", file=sys.stderr)
        sys.exit(2)
    sys.exit(main(sys.argv[1]))
