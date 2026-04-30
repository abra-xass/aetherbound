#!/usr/bin/env python3
"""Restore E301..E1000 from the original 1000-PNG Downloads folder into a
pool directory (outside assets/) so they don't bloat the APK but stay
available for the future scale-up to 1000 echoforms.

Usage: py -3 tools/ai/restore_pool.py
"""
from __future__ import annotations
import shutil
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
SRC = Path(r"C:/Users/Amir/Downloads/Neuer Ordner")
POOL = REPO / "tools" / "ai" / "echoforms-pool"


def target_name(ordinal_1based: int) -> str:
    return f"E{ordinal_1based:03d}.png" if ordinal_1based < 1000 else f"E{ordinal_1based}.png"


def main() -> int:
    if not SRC.is_dir():
        print(f"missing source: {SRC}", file=sys.stderr); return 2
    POOL.mkdir(parents=True, exist_ok=True)
    n = 0
    # E301..E1000 corresponds to 00300.png..00999.png (0-indexed source)
    for i in range(300, 1000):
        src = SRC / f"{i:05d}.png"
        if not src.is_file():
            continue
        dst = POOL / target_name(i + 1)
        shutil.copy2(src, dst)
        n += 1
        if n % 100 == 0:
            print(f"  {n} restored")
    print(f"-- done | {n} echoforms in pool/ --")
    return 0


if __name__ == "__main__":
    sys.exit(main())
