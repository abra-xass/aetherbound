#!/usr/bin/env python3
"""
Inspect a folder of free tile/autotile packs and report contents + licensing.

Walks every subfolder, identifies pack format (Wang autotile / single tileset
atlas / individual-tile pack / Tiled .tmx project), reads LICENSE/README files,
and recommends usage per pack.

Usage: py -3 inspect_graphics_folder.py "C:/Users/Amir/Desktop/Graphics"
"""
from __future__ import annotations
import sys, struct, re
from pathlib import Path
from collections import Counter


def png_size(path: Path):
    try:
        with open(path, "rb") as f:
            head = f.read(24)
        if head[:8] != b"\x89PNG\r\n\x1a\n":
            return None
        return struct.unpack(">II", head[16:24])
    except Exception:
        return None


WANG_HINTS = re.compile(r"_(N|S|E|W|NE|NW|SE|SW|inner|outer|corner|edge|cap)\b", re.I)
TILED_EXTS = {".tmx", ".tsx", ".tilesetx", ".world"}
LICENSE_FILE_HINTS = re.compile(r"(license|readme|credits|attribution|copyright)", re.I)


def license_summary(pack_dir: Path) -> str:
    snippets = []
    for f in pack_dir.rglob("*"):
        if f.is_file() and LICENSE_FILE_HINTS.search(f.name) and f.suffix.lower() in {".txt", ".md", ".html", ""}:
            try:
                text = f.read_text(encoding="utf-8", errors="ignore")[:600]
                snippets.append(f"  [{f.relative_to(pack_dir)}]\n    " + text.strip().replace("\n", "\n    ")[:500])
            except Exception:
                pass
    return "\n".join(snippets) if snippets else "  (no license/readme file detected)"


def inspect_pack(pack_dir: Path) -> dict:
    files = list(pack_dir.rglob("*"))
    pngs = [f for f in files if f.is_file() and f.suffix.lower() == ".png"]
    tiled = [f for f in files if f.is_file() and f.suffix.lower() in TILED_EXTS]
    dim_counter = Counter()
    wang_hits = 0
    for p in pngs:
        d = png_size(p)
        if d:
            dim_counter[d] += 1
        if WANG_HINTS.search(p.stem):
            wang_hits += 1

    layout = "unknown"
    if tiled:
        layout = f"Tiled project ({len(tiled)} .tmx/.tsx files)"
    elif wang_hits >= len(pngs) * 0.5 and wang_hits >= 8:
        layout = f"Wang autotile set (~{wang_hits}/{len(pngs)} files have edge hints)"
    elif dim_counter and len(dim_counter) <= 2 and len(pngs) >= 20:
        sample = dim_counter.most_common(1)[0][0]
        layout = f"individual tile pack ({len(pngs)} × {sample[0]}x{sample[1]})"
    elif dim_counter:
        biggest = max(dim_counter, key=lambda d: d[0] * d[1])
        if biggest[0] >= 256 and dim_counter[biggest] <= 3:
            layout = f"single atlas-style ({biggest[0]}x{biggest[1]})"
        else:
            layout = f"mixed ({len(pngs)} PNGs across {len(dim_counter)} sizes)"

    return {
        "name": pack_dir.name,
        "path": pack_dir,
        "png_count": len(pngs),
        "tiled_count": len(tiled),
        "dims_top": dim_counter.most_common(5),
        "wang_hits": wang_hits,
        "layout": layout,
    }


def find_packs(root: Path) -> list[Path]:
    """A 'pack' is a leaf-ish folder containing PNGs (and maybe .tmx)."""
    packs = []
    for d in sorted(root.rglob("*")):
        if not d.is_dir():
            continue
        # is this dir a pack?  i.e. contains PNGs and isn't just a parent of subpacks
        own_pngs = [f for f in d.iterdir() if f.is_file() and f.suffix.lower() == ".png"]
        if own_pngs:
            packs.append(d)
    # also include the root folders Autotiles/Tilesets if they have direct PNGs
    for d in (root,):
        own_pngs = [f for f in d.iterdir() if f.is_file() and f.suffix.lower() == ".png"]
        if own_pngs:
            packs.append(d)
    return packs


def main(root_str: str) -> int:
    root = Path(root_str)
    if not root.is_dir():
        print(f"FATAL: {root} not a directory", file=sys.stderr)
        return 2

    print(f"Scanning {root}\n")
    packs = find_packs(root)
    if not packs:
        print("No PNG-bearing folders found.")
        return 1

    for d in packs:
        info = inspect_pack(d)
        rel = d.relative_to(root) if d != root else Path(".")
        print("=" * 78)
        print(f"PACK: {rel}")
        print(f"  PNGs: {info['png_count']}  |  Tiled files: {info['tiled_count']}")
        if info["dims_top"]:
            dim_str = ", ".join(f"{w}x{h}={c}" for (w, h), c in info["dims_top"])
            print(f"  dimensions: {dim_str}")
        if info["wang_hits"]:
            print(f"  wang/edge filenames: {info['wang_hits']}")
        print(f"  layout: {info['layout']}")
        print("  license:")
        print(license_summary(d))
    return 0


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("usage: py -3 inspect_graphics_folder.py <folder>", file=sys.stderr)
        sys.exit(2)
    sys.exit(main(sys.argv[1]))
