#!/usr/bin/env python3
"""
Import 1000 echoform PNGs from a source folder into the runtime asset path.
Source filenames `00000.png .. 00999.png` -> `E001.png .. E1000.png`.

Usage:
    py -3 tools/ai/import_echoforms.py "C:/Users/Amir/Downloads/Neuer Ordner"
"""
from __future__ import annotations
import sys
import shutil
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
DEST = REPO / "app" / "src" / "main" / "assets" / "game" / "echoforms"


def target_name(ordinal_1based: int) -> str:
    return f"E{ordinal_1based:03d}.png" if ordinal_1based < 1000 else f"E{ordinal_1based}.png"


def main(source: str) -> int:
    src = Path(source)
    if not src.is_dir():
        print(f"FATAL: source not a directory: {src}", file=sys.stderr)
        return 2
    DEST.mkdir(parents=True, exist_ok=True)

    pngs = sorted(src.glob("*.png"))
    if not pngs:
        print(f"FATAL: no PNGs in {src}", file=sys.stderr)
        return 2

    n = 0
    skipped = 0
    for i, src_file in enumerate(pngs):
        ordinal = i + 1
        dst_file = DEST / target_name(ordinal)
        try:
            shutil.copy2(src_file, dst_file)
            n += 1
            if n % 100 == 0:
                print(f"  {n}/{len(pngs)} copied")
        except Exception as e:
            print(f"  X {src_file.name}: {e}")
            skipped += 1

    print(f"-- done | copied {n}, skipped {skipped} --")
    return 0 if skipped == 0 else 1


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("usage: py -3 tools/ai/import_echoforms.py <source-dir>", file=sys.stderr)
        sys.exit(2)
    sys.exit(main(sys.argv[1]))
