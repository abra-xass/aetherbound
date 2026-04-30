#!/usr/bin/env python3
"""
Inspect tilepack ZIPs and report contents + routing recommendation.

Usage: py -3 tools/ai/inspect_zips.py <zip1> <zip2> ...
"""
from __future__ import annotations
import sys, zipfile, struct
from pathlib import Path
from collections import Counter, defaultdict


def png_size_from_bytes(b: bytes):
    if len(b) < 24 or b[:8] != b"\x89PNG\r\n\x1a\n":
        return None
    return struct.unpack(">II", b[16:24])


def inspect(zip_path: str) -> dict:
    p = Path(zip_path)
    out = {"path": str(p), "size_kb": p.stat().st_size // 1024, "ok": False}
    if not p.is_file():
        out["error"] = "missing"
        return out
    try:
        with zipfile.ZipFile(p) as z:
            names = z.namelist()
            ext_count = Counter()
            png_dims = Counter()
            top_dirs = Counter()
            png_paths = []
            for n in names:
                ext = Path(n).suffix.lower().lstrip(".")
                if ext:
                    ext_count[ext] += 1
                top = n.split("/", 1)[0] if "/" in n else "(root)"
                top_dirs[top] += 1
                if ext == "png":
                    try:
                        with z.open(n) as f:
                            head = f.read(24)
                        d = png_size_from_bytes(head)
                        if d:
                            png_dims[d] += 1
                            png_paths.append(n)
                    except Exception:
                        pass
            out["ok"] = True
            out["total_files"] = len(names)
            out["ext_count"] = dict(ext_count.most_common(10))
            out["top_dirs"] = dict(top_dirs.most_common(8))
            out["png_count"] = sum(png_dims.values())
            out["png_dims"] = {f"{w}x{h}": c for (w, h), c in png_dims.most_common(10)}
            out["png_samples"] = png_paths[:5] + (png_paths[-3:] if len(png_paths) > 8 else [])
    except Exception as e:
        out["error"] = f"{type(e).__name__}: {e}"
    return out


def categorise(report: dict) -> str:
    if not report.get("ok"):
        return f"ERROR: {report.get('error')}"
    ext = report["ext_count"]
    has_2d = ext.get("png", 0) > 0
    has_3d = any(ext.get(e, 0) > 0 for e in ("fbx", "obj", "glb", "gltf", "blend"))
    if has_3d and not has_2d:
        return "3D-only — not usable for 2D top-down RPG. Skip."
    if has_3d and has_2d:
        return "3D primary + PNG previews — PNGs may be marketing renders, low usability"
    if has_2d:
        # 2D analysis
        dims = report["png_dims"]
        if not dims:
            return "PNGs but no readable dimensions"
        biggest = max((int(k.split("x")[0]) * int(k.split("x")[1]), k) for k in dims)
        single_big_pct = report["png_dims"][biggest[1]] / max(report["png_count"], 1)
        # Heuristic: many small same-size PNGs => individual tiles; one big PNG => atlas
        sizes_set = set(dims.keys())
        small_uniform = all(int(k.split("x")[0]) <= 128 for k in sizes_set)
        if small_uniform and len(sizes_set) <= 3:
            return f"2D individual tiles ({report['png_count']} PNGs, sizes {sorted(sizes_set)}). USABLE as primary tile source."
        if biggest[0] >= 512 * 512 and report["png_count"] <= 5:
            return f"2D atlas-style ({biggest[1]}). Needs slicing."
        return f"2D mixed ({report['png_count']} PNGs across {len(sizes_set)} sizes). Manual review."
    return "Unknown contents."


def main(zips: list[str]) -> int:
    for z in zips:
        r = inspect(z)
        print("=" * 78)
        print(f"PACK: {Path(z).name}  ({r.get('size_kb', 0)} KB)")
        if not r.get("ok"):
            print(f"  ERROR: {r.get('error')}")
            continue
        print(f"  total files: {r['total_files']}")
        print(f"  extensions:  {r['ext_count']}")
        print(f"  top dirs:    {r['top_dirs']}")
        if r["png_count"]:
            print(f"  PNG count:   {r['png_count']}")
            print(f"  PNG dims:    {r['png_dims']}")
            print(f"  PNG samples: {r['png_samples'][:4]}")
        print(f"  -> {categorise(r)}")
    return 0


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("usage: py -3 inspect_zips.py <zip1> ...", file=sys.stderr)
        sys.exit(2)
    sys.exit(main(sys.argv[1:]))
