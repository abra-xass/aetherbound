#!/usr/bin/env python3
"""
Tuxemon → Aetherbound Phase-1 Importer.

Translates Tuxemon's content (411 monsters, 274 techniques, 223 items,
13 elements, 35 statuses, 235 maps, 76 tilesets, all sprites) into the
Aetherbound asset-structure (assets/game/<category>/{tuxemon,overrides}/).

Reads:
    C:/project/Neu/Tuxemon/mods/tuxemon/db/      (YAML files)
    C:/project/Neu/Tuxemon/mods/tuxemon/gfx/     (PNG sprites + tilesets)
    C:/project/Neu/Tuxemon/mods/tuxemon/maps/    (Tiled .tmx)
    C:/project/Neu/Tuxemon/mods/tuxemon/animations/  (move FX)

Writes:
    C:/project/Neu/Aetherbound/app/src/main/assets/game/
        ├── data/<category>.json   (parsed YAML → flat JSON)
        ├── echoforms/tuxemon/<slug>/{front,back,menu}.png
        ├── techniques/tuxemon/<slug>/sheet.png + meta.json
        ├── items/tuxemon/<slug>.png
        ├── tilesets/tuxemon/*.png
        ├── maps/tuxemon/*.tmx
        ├── characters/tuxemon/<name>/sheet.png
        ├── ui/tuxemon/...
        └── manifest.json

License compliance: every Tuxemon-sourced file lands in `tuxemon/` subdir.
User-polished overrides go to `overrides/` (created empty during import).

Usage:
    py -3 tools/ai/import_tuxemon.py            # full import
    py -3 tools/ai/import_tuxemon.py --phase 1  # only data (no sprites)
    py -3 tools/ai/import_tuxemon.py --dry-run  # show plan, no writes
"""
from __future__ import annotations
import argparse
import json
import shutil
import sys
from pathlib import Path

# Force UTF-8 stdout/stderr on Windows so unicode in print() doesn't crash cp1252.
try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

try:
    import yaml
except ImportError:
    print("PyYAML required. Run: py -3 -m pip install PyYAML Pillow", file=sys.stderr)
    sys.exit(2)

try:
    from PIL import Image
except ImportError:
    print("Pillow required. Run: py -3 -m pip install Pillow", file=sys.stderr)
    sys.exit(2)


REPO = Path(__file__).resolve().parents[2]
# Tuxemon source path — configurable via env so CI can clone next to the repo.
# Local default = the absolute path on the original author's dev machine.
_DEFAULT_TUXEMON = "C:/project/Neu/Tuxemon/mods/tuxemon"
import os as _os
TUXEMON = Path(_os.environ.get("TUXEMON_SRC", _DEFAULT_TUXEMON))
# CI convenience: if TUXEMON_SRC isn't set but a sibling Tuxemon clone exists,
# auto-resolve it. This makes `git clone ... && python import_tuxemon.py` JustWork.
if not TUXEMON.is_dir():
    sibling = REPO.parent / "Tuxemon" / "mods" / "tuxemon"
    if sibling.is_dir(): TUXEMON = sibling
OUT = REPO / "app" / "src" / "main" / "assets" / "game"
AUDIO_OUT = REPO / "app" / "src" / "main" / "assets" / "audio"


# ── Phase 1: Parse YAML databases → JSON ──────────────────────────────
DB_CATEGORIES = {
    "monster": "monsters",
    "technique": "techniques",
    "item": "items",
    "element": "elements",
    "status": "statuses",
}


def import_data(dry: bool) -> dict[str, int]:
    """Parse all YAML db/<cat>/*.yaml files into one JSON list per cat."""
    out_dir = OUT / "data"
    if not dry: out_dir.mkdir(parents=True, exist_ok=True)
    counts = {}
    for src_cat, out_name in DB_CATEGORIES.items():
        src_dir = TUXEMON / "db" / src_cat
        files = sorted(src_dir.glob("*.yaml"))
        all_records = []
        for f in files:
            try:
                with open(f, "r", encoding="utf-8") as h:
                    record = yaml.safe_load(h)
                if record is None: continue
                # use filename slug if not in record
                if "slug" not in record:
                    record["slug"] = f.stem
                all_records.append(record)
            except Exception as e:
                print(f"  X {f.name}: {e}")
        counts[out_name] = len(all_records)
        if not dry:
            out_path = out_dir / f"{out_name}.json"
            out_path.write_text(json.dumps(all_records, indent=2, ensure_ascii=False), encoding="utf-8")
    return counts


# ── Phase 2: Monster sprites ──────────────────────────────────────────
# Tuxemon battle sheets are 128×88 — a 2-frame horizontal layout with
#   front_rect = (0,  0, 64, 64)  → left half  (player-facing portrait)
#   back_rect  = (64, 0, 64, 64)  → right half (back view, what you see in battle)
# Source: tests/tuxemon/test_monster_sprite_handler.py and db.py MonsterSpritesModel.
# The 88px height adds padding below the 64×64 frames for taller monsters.
def import_monster_sprites(dry: bool) -> int:
    src_dir = TUXEMON / "gfx" / "sprites" / "battle"
    out_root = OUT / "echoforms" / "tuxemon"
    if not dry: out_root.mkdir(parents=True, exist_ok=True)
    count = 0
    for sheet_path in sorted(src_dir.glob("*-sheet.png")):
        slug = sheet_path.stem.removesuffix("-sheet")
        if slug == "missing": continue
        target_dir = out_root / slug
        if not dry:
            target_dir.mkdir(parents=True, exist_ok=True)
            try:
                with Image.open(sheet_path) as img:
                    w, h = img.size
                    # Standard Tuxemon layout: two frames side-by-side, frame_w = w/2.
                    # Sheet height (typically 88) is taller than frame (64) for tall monsters;
                    # we keep the full frame strip so nothing gets clipped.
                    if w % 2 == 0:
                        frame_w = w // 2
                        front = img.crop((0, 0, frame_w, h))
                        back  = img.crop((frame_w, 0, w, h))
                    else:
                        # malformed sheet — duplicate as fallback
                        front = img.copy()
                        back  = img.copy()
                    front.save(target_dir / "front.png", "PNG")
                    back.save(target_dir / "back.png", "PNG")
                    # menu icon = 32x32 thumbnail of front
                    menu = front.copy()
                    menu.thumbnail((32, 32), Image.NEAREST)
                    menu.save(target_dir / "menu.png", "PNG")
            except Exception as e:
                print(f"  X {slug}: {e}")
                continue
        count += 1
    # create empty overrides/ scaffold
    overrides = OUT / "echoforms" / "overrides"
    if not dry: overrides.mkdir(parents=True, exist_ok=True)
    return count


# ── Phase 3: Item icons ───────────────────────────────────────────────
def import_item_icons(dry: bool) -> int:
    src_dir = TUXEMON / "gfx" / "items"
    out_dir = OUT / "items" / "tuxemon"
    if not dry: out_dir.mkdir(parents=True, exist_ok=True)
    count = 0
    for png in sorted(src_dir.glob("*.png")):
        if not dry:
            shutil.copy2(png, out_dir / png.name)
        count += 1
    if not dry:
        (OUT / "items" / "overrides").mkdir(parents=True, exist_ok=True)
    return count


# ── Phase 4: Tilesets ─────────────────────────────────────────────────
def import_tilesets(dry: bool) -> int:
    src_dir = TUXEMON / "gfx" / "tilesets"
    out_dir = OUT / "tilesets" / "tuxemon"
    if not dry: out_dir.mkdir(parents=True, exist_ok=True)
    count = 0
    for png in sorted(src_dir.rglob("*.png")):
        rel = png.relative_to(src_dir)
        target = out_dir / rel
        if not dry:
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(png, target)
        count += 1
    if not dry:
        (OUT / "tilesets" / "overrides").mkdir(parents=True, exist_ok=True)
    return count


# ── Phase 5: Maps ─────────────────────────────────────────────────────
def import_maps(dry: bool) -> int:
    src_dir = TUXEMON / "maps"
    out_dir = OUT / "maps" / "tuxemon"
    if not dry: out_dir.mkdir(parents=True, exist_ok=True)
    count = 0
    for f in sorted(src_dir.rglob("*.*")):
        if f.suffix.lower() in (".tmx", ".tsx", ".json"):
            rel = f.relative_to(src_dir)
            target = out_dir / rel
            if not dry:
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(f, target)
            count += 1
    if not dry:
        (OUT / "maps" / "overrides").mkdir(parents=True, exist_ok=True)
    return count


# ── Phase 6: Move FX animations ───────────────────────────────────────
def import_move_fx(dry: bool) -> int:
    src_dir = TUXEMON / "animations" / "technique"
    out_dir = OUT / "techniques" / "tuxemon"
    if not dry: out_dir.mkdir(parents=True, exist_ok=True)
    count = 0
    for png in sorted(src_dir.glob("*.png")):
        # Tuxemon names them like fireball_114.png — group by prefix
        slug = png.stem
        target = out_dir / slug / "sheet.png"
        if not dry:
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(png, target)
            # placeholder meta
            (target.parent / "meta.json").write_text(
                json.dumps({"source": "tuxemon", "filename": png.name}, indent=2),
                encoding="utf-8",
            )
        count += 1
    if not dry:
        (OUT / "techniques" / "overrides").mkdir(parents=True, exist_ok=True)
    return count


# ── Phase 7: Character sprites (player + NPCs) ────────────────────────
def import_characters(dry: bool) -> int:
    src_dir = TUXEMON / "gfx" / "sprites" / "player"
    out_dir = OUT / "characters" / "tuxemon"
    if not dry: out_dir.mkdir(parents=True, exist_ok=True)
    count = 0
    for png in sorted(src_dir.glob("*.png")):
        slug = png.stem
        target_dir = out_dir / slug
        if not dry:
            target_dir.mkdir(parents=True, exist_ok=True)
            shutil.copy2(png, target_dir / "sheet.png")
        count += 1
    if not dry:
        (OUT / "characters" / "overrides").mkdir(parents=True, exist_ok=True)
    return count


# ── Phase 8: UI graphics ──────────────────────────────────────────────
def import_ui(dry: bool) -> int:
    src_dir = TUXEMON / "gfx" / "ui"
    out_dir = OUT / "ui" / "tuxemon"
    if not dry: out_dir.mkdir(parents=True, exist_ok=True)
    count = 0
    for f in sorted(src_dir.rglob("*.*")):
        if f.suffix.lower() in (".png", ".jpg"):
            rel = f.relative_to(src_dir)
            target = out_dir / rel
            if not dry:
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(f, target)
            count += 1
    if not dry:
        (OUT / "ui" / "overrides").mkdir(parents=True, exist_ok=True)
    return count


# ── Phase 9: Audio (SFX + Music) ──────────────────────────────────────
def import_audio(dry: bool) -> dict:
    """Copy Tuxemon SFX and music into assets/audio/.

    Folder layout produced:
      assets/audio/sfx/<slug>.ogg     (flat — slug taken from filename without ext)
      assets/audio/music/<slug>.ogg

    Tuxemon ships .ogg primarily, with a few .mp3 fallbacks for legacy tracks.
    Both formats are copied as-is; the AudioEngine probes both extensions.
    """
    sfx_src = TUXEMON / "sounds"
    music_src = TUXEMON / "music"
    sfx_out = AUDIO_OUT / "sfx"
    music_out = AUDIO_OUT / "music"
    if not dry:
        sfx_out.mkdir(parents=True, exist_ok=True)
        music_out.mkdir(parents=True, exist_ok=True)

    sfx_count = 0
    if sfx_src.is_dir():
        for f in sorted(sfx_src.rglob("*.*")):
            if f.suffix.lower() not in (".ogg", ".wav", ".mp3"): continue
            # Slug = stem in lower_snake_case, prefix with subdir for namespacing
            # (combat/, interface/, monster/, item/) to avoid name collisions.
            rel = f.relative_to(sfx_src)
            parts = list(rel.parts[:-1]) + [rel.stem]
            slug = "_".join(parts).lower().replace(" ", "_").replace("-", "_")
            target = sfx_out / f"{slug}{f.suffix.lower()}"
            if not dry: shutil.copy2(f, target)
            sfx_count += 1

    music_count = 0
    if music_src.is_dir():
        for f in sorted(music_src.rglob("*.*")):
            if f.suffix.lower() not in (".ogg", ".mp3"): continue
            slug = f.stem.lower().replace(" ", "_").replace("-", "_")
            # Strip duplicate underscores from sloppy filenames
            while "__" in slug: slug = slug.replace("__", "_")
            target = music_out / f"{slug}{f.suffix.lower()}"
            if not dry: shutil.copy2(f, target)
            music_count += 1

    return {"sfx": sfx_count, "music": music_count}


# ── Manifest ──────────────────────────────────────────────────────────
def write_manifest(stats: dict[str, int], dry: bool) -> None:
    manifest = {
        "source": "Tuxemon (https://github.com/Tuxemon/Tuxemon)",
        "license_code": "GPL-3.0",
        "license_assets": "CC-BY-SA-3.0",
        "import_stats": stats,
        "structure_version": 1,
        "categories": {
            cat: { "source": "tuxemon", "overridable": True }
            for cat in ["echoforms", "techniques", "items", "tilesets", "maps", "characters", "ui"]
        },
    }
    if not dry:
        (OUT / "manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")


def parse_args():
    p = argparse.ArgumentParser(description="Tuxemon → Aetherbound asset import")
    p.add_argument("--dry-run", action="store_true", help="report only, no writes")
    p.add_argument("--phase", type=int, default=0,
                   help="run only one phase (1=data, 2=monsters, 3=items, 4=tilesets, "
                        "5=maps, 6=move-fx, 7=chars, 8=ui). 0=all")
    return p.parse_args()


def main(args) -> int:
    if not TUXEMON.is_dir():
        print(f"FATAL: {TUXEMON} not found", file=sys.stderr); return 2

    stats = {}
    phases = [
        (1, "data (YAML->JSON)", import_data),
        (2, "monster sprites (front/back/menu)", import_monster_sprites),
        (3, "item icons", import_item_icons),
        (4, "tilesets", import_tilesets),
        (5, "maps (.tmx/.tsx)", import_maps),
        (6, "move FX", import_move_fx),
        (7, "character sprites", import_characters),
        (8, "UI graphics", import_ui),
        (9, "audio (SFX + music)", import_audio),
    ]
    print(f">> Tuxemon -> Aetherbound import | dry={args.dry_run}")
    for num, name, fn in phases:
        if args.phase != 0 and args.phase != num:
            continue
        print(f"\n[{num}] {name}")
        result = fn(args.dry_run)
        if isinstance(result, dict):
            stats.update(result)
            for k, v in result.items():
                print(f"  {k}: {v}")
        else:
            stats[name] = result
            print(f"  copied: {result}")

    write_manifest(stats, args.dry_run)
    print(f"\n-- done --")
    print(f"Output: {OUT}")
    return 0


if __name__ == "__main__":
    sys.exit(main(parse_args()))
