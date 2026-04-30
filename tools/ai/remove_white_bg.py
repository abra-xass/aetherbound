#!/usr/bin/env python3
"""
Remove white / near-white backgrounds from all Echoform PNGs by converting
matching pixels to transparent (alpha=0). Uses corner-sampling so it works
on white, off-white, or any solid-color BG — not hardcoded to pure white.

Strategy:
  1. Sample 4 corner pixels — if at least 3 match (within tolerance), that's
     the BG colour
  2. Walk every pixel; if it's within `tolerance` of the BG colour AND
     reachable from a corner (flood-fill), set alpha=0
  3. Anti-alias edge: also fade alpha proportionally for near-BG pixels in
     the flood-filled region

Usage:
    py -3 tools/ai/remove_white_bg.py                  # all 300, in-place
    py -3 tools/ai/remove_white_bg.py --tolerance 30   # stricter / looser
    py -3 tools/ai/remove_white_bg.py --backup         # writes to backup/ first
"""
from __future__ import annotations
import argparse
import shutil
import sys
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    print("Pillow not installed. Run: py -3 -m pip install Pillow", file=sys.stderr)
    sys.exit(2)

REPO = Path(__file__).resolve().parents[2]
ASSETS = REPO / "app" / "src" / "main" / "assets" / "game" / "echoforms"
BACKUP = REPO / "tools" / "ai" / "echoforms-backup-pre-keying"


def detect_bg_color(img: Image.Image, tolerance: int = 20) -> tuple[int, int, int] | None:
    """Returns the BG colour if 3 of 4 corners agree (within tolerance), else None."""
    w, h = img.size
    corners = [
        img.getpixel((0, 0)),
        img.getpixel((w - 1, 0)),
        img.getpixel((0, h - 1)),
        img.getpixel((w - 1, h - 1)),
    ]
    # Take first 3 channels (RGB) of each corner
    rgbs = [c[:3] for c in corners]
    # Count corners matching the first one within tolerance
    base = rgbs[0]
    matching = [c for c in rgbs if all(abs(c[i] - base[i]) <= tolerance for i in range(3))]
    return base if len(matching) >= 3 else None


def flood_fill_remove(img: Image.Image, bg: tuple[int, int, int], tolerance: int) -> Image.Image:
    """Flood-fills from all 4 corners, removing pixels within tolerance of BG."""
    img = img.convert("RGBA")
    w, h = img.size
    pixels = img.load()
    seen = bytearray(w * h)
    stack = [(0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)]

    def matches(c: tuple[int, ...]) -> bool:
        return all(abs(c[i] - bg[i]) <= tolerance for i in range(3))

    while stack:
        x, y = stack.pop()
        if x < 0 or x >= w or y < 0 or y >= h:
            continue
        idx = y * w + x
        if seen[idx]:
            continue
        c = pixels[x, y]
        if not matches(c):
            continue
        seen[idx] = 1
        # Soft alpha based on distance to BG (anti-alias edge)
        diff = max(abs(c[0] - bg[0]), abs(c[1] - bg[1]), abs(c[2] - bg[2]))
        alpha = int((diff / tolerance) * 255) if tolerance > 0 else 0
        pixels[x, y] = (c[0], c[1], c[2], alpha)
        stack.extend([(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)])
    return img


def process(path: Path, tolerance: int) -> bool:
    img = Image.open(path).convert("RGBA")
    bg = detect_bg_color(img.convert("RGB"), tolerance=20)
    if bg is None:
        return False  # no consistent BG — skip
    out = flood_fill_remove(img, bg, tolerance)
    out.save(path, "PNG")
    return True


def main(tolerance: int, backup: bool, only: set[str] | None) -> int:
    fronts = sorted(p for p in ASSETS.glob("E*.png") if "@" not in p.stem)
    if only:
        fronts = [p for p in fronts if p.stem in only]
    if not fronts:
        print("no fronts to process"); return 1

    if backup:
        BACKUP.mkdir(parents=True, exist_ok=True)
        for p in fronts:
            dst = BACKUP / p.name
            if not dst.exists():
                shutil.copy2(p, dst)
        print(f"backup written to {BACKUP}")

    print(f">> processing {len(fronts)} PNGs | tolerance={tolerance}")
    ok, skipped = 0, 0
    for i, p in enumerate(fronts, 1):
        try:
            if process(p, tolerance):
                ok += 1
            else:
                skipped += 1
            if i % 50 == 0:
                print(f"  {i}/{len(fronts)} ({ok} keyed, {skipped} skipped)")
        except Exception as e:
            print(f"  X {p.name}: {type(e).__name__}: {e}")
    print(f"-- done | keyed {ok}, skipped {skipped} --")
    return 0


def parse_args():
    p = argparse.ArgumentParser(description="Remove white/near-uniform background from Echoform PNGs")
    p.add_argument("--tolerance", type=int, default=24, help="RGB distance tolerance (8=strict, 40=loose)")
    p.add_argument("--backup", action="store_true", help="copy originals to tools/ai/echoforms-backup-pre-keying/")
    p.add_argument("--ids", default="", help="comma-separated id filter (e.g. E001,E002)")
    return p.parse_args()


if __name__ == "__main__":
    a = parse_args()
    only = {s.strip() for s in a.ids.split(",") if s.strip()} or None
    sys.exit(main(a.tolerance, a.backup, only))
