#!/usr/bin/env python3
"""
generate_wave01.py — fetch Wave-01 graphics from Pollinations.ai (Flux).

Reads `app/src/test/Thot Fusion/docs/graphics-wave-01-prompt-pack.json`
and runs each job through the free Pollinations endpoint, saving PNGs
into the appropriate `app/src/main/assets/game/<bucket>/` folder.

No API key. No signup. Free.

Usage:
    python tools/ai/generate_wave01.py                      # all 13 jobs
    python tools/ai/generate_wave01.py --types style_board  # only boards
    python tools/ai/generate_wave01.py --ids wave01_05      # single job
    python tools/ai/generate_wave01.py --seed 1337          # reproducible

Each PNG is saved with its canonical `target_filename` from the pack.
Re-running overwrites — that's intentional, you want fresh tries cheap.
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

# Force UTF-8 stdout/stderr regardless of Windows console codepage,
# so we don't crash on common box-drawing chars when output is redirected.
try:
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")
except Exception:
    pass

REPO_ROOT = Path(__file__).resolve().parents[2]
PACK_PATH = REPO_ROOT / "app" / "src" / "test" / "Thot Fusion" / "docs" / "graphics-wave-01-prompt-pack.json"
ASSETS_ROOT = REPO_ROOT / "app" / "src" / "main" / "assets" / "game"

# asset_type -> (subfolder, width, height)
BUCKETS = {
    "style_board":     ("style-boards", 1536, 1024),
    "region_concept":  ("concepts",     1536, 1024),
    "starter_concept": ("concepts",     1024, 1024),
    "vfx_sheet":       ("vfx",          1536,  768),
    # default fallback
    "default":         ("concepts",     1024, 1024),
}

POLLINATIONS_BASE = "https://image.pollinations.ai/prompt/"


def build_url(prompt: str, negative: str | None, width: int, height: int, seed: int, model: str = "flux") -> str:
    # Combine negative into the prompt as a "do not include" clause — Flux
    # responds better to descriptive avoidance than to a separate negative prompt.
    full_prompt = prompt
    if negative:
        full_prompt = f"{prompt}. Do not include: {negative}."
    quoted = urllib.parse.quote(full_prompt, safe="")
    params = urllib.parse.urlencode({
        "width": width,
        "height": height,
        "model": model,
        "seed": seed,
        "nologo": "true",
        "enhance": "false",
    })
    return f"{POLLINATIONS_BASE}{quoted}?{params}"


def download(url: str, dest: Path, timeout: int = 240) -> int:
    """Returns bytes written or 0 on failure."""
    req = urllib.request.Request(url, headers={"User-Agent": "Aetherbound-Wave01/1.0"})
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            data = resp.read()
        if len(data) < 1024:
            print(f"  ! suspicious size {len(data)}B - probably an error placeholder")
            return 0
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(data)
        return len(data)
    except Exception as e:
        print(f"  X {type(e).__name__}: {e}")
        return 0


def run(types: set[str] | None, ids: set[str] | None, seed: int, model: str, retries: int) -> int:
    if not PACK_PATH.exists():
        print(f"FATAL: prompt pack not found at {PACK_PATH}", file=sys.stderr)
        return 2

    with PACK_PATH.open("r", encoding="utf-8") as f:
        pack = json.load(f)

    jobs = pack.get("jobs", [])
    if types:
        jobs = [j for j in jobs if j.get("asset_type") in types]
    if ids:
        jobs = [j for j in jobs if j.get("id") in ids]

    if not jobs:
        print("No jobs match the filter.")
        return 1

    print(f">> {len(jobs)} job(s) queued | model={model} | seed={seed}")
    print(f"  pack: {PACK_PATH.name}")
    print(f"  out:  {ASSETS_ROOT.relative_to(REPO_ROOT)}")
    print()

    ok = 0
    failed = []
    for i, job in enumerate(jobs, 1):
        jid = job.get("id", f"job_{i}")
        atype = job.get("asset_type", "default")
        bucket, w, h = BUCKETS.get(atype, BUCKETS["default"])
        fname = job.get("target_filename") or f"{jid}.png"
        dest = ASSETS_ROOT / bucket / fname

        prompt = job["prompt"]
        negative = job.get("negative_prompt", "")

        print(f"[{i}/{len(jobs)}] {jid}  →  {bucket}/{fname}  ({w}×{h})")
        url = build_url(prompt, negative, w, h, seed, model)
        # show short prompt preview
        preview = prompt[:90].replace("\n", " ")
        print(f"  prompt: {preview}…")

        size = 0
        for attempt in range(1, retries + 1):
            t0 = time.time()
            size = download(url, dest)
            dt = time.time() - t0
            if size > 0:
                print(f"  OK {size//1024} KB in {dt:.1f}s")
                ok += 1
                break
            print(f"  retry {attempt}/{retries}…")
            time.sleep(2 * attempt)
        else:
            failed.append(jid)
        # gentle rate limit
        time.sleep(1)

    print()
    print(f"-- done | {ok}/{len(jobs)} ok --")
    if failed:
        print(f"  failed: {', '.join(failed)}")
        return 1
    return 0


def run_inline(bucket: str, filename: str, prompt: str, negative: str,
               width: int, height: int, seed: int, model: str, retries: int) -> int:
    dest = ASSETS_ROOT / bucket / filename
    print(f">> inline | {bucket}/{filename} ({width}x{height}) seed={seed} model={model}")
    print(f"  prompt: {prompt[:90]}...")
    url = build_url(prompt, negative or None, width, height, seed, model)
    for attempt in range(1, retries + 1):
        t0 = time.time()
        size = download(url, dest)
        dt = time.time() - t0
        if size > 0:
            print(f"  OK {size//1024} KB in {dt:.1f}s")
            return 0
        print(f"  retry {attempt}/{retries}...")
        time.sleep(2 * attempt)
    print(f"  X failed")
    return 1


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Generate Aetherbound graphics via Pollinations.ai")
    p.add_argument("--types", default="",
                   help="comma-separated asset_type filter (style_board,region_concept,starter_concept,vfx_sheet)")
    p.add_argument("--ids", default="",
                   help="comma-separated job-id filter (e.g. wave01_05_namaris_harbor)")
    p.add_argument("--seed", type=int, default=42, help="seed for reproducibility (default 42)")
    p.add_argument("--model", default="flux", choices=["flux", "flux-realism", "flux-anime", "turbo"],
                   help="Pollinations model (default flux)")
    p.add_argument("--retries", type=int, default=3, help="retry attempts per job")
    # Inline (one-off) production-asset mode — bypasses the JSON pack
    p.add_argument("--inline", action="store_true", help="run a single ad-hoc generation, ignores the JSON pack")
    p.add_argument("--bucket", default="echoforms", help="(inline) target subfolder under assets/game/")
    p.add_argument("--filename", default="", help="(inline) output filename, e.g. E001.png")
    p.add_argument("--prompt", default="", help="(inline) positive prompt")
    p.add_argument("--negative", default="", help="(inline) negative prompt")
    p.add_argument("--width", type=int, default=1024, help="(inline) image width")
    p.add_argument("--height", type=int, default=1024, help="(inline) image height")
    return p.parse_args()


def main() -> int:
    args = parse_args()
    if args.inline:
        if not args.filename or not args.prompt:
            print("--inline requires --filename and --prompt", file=sys.stderr)
            return 2
        return run_inline(args.bucket, args.filename, args.prompt, args.negative,
                          args.width, args.height, args.seed, args.model, args.retries)
    types = {s.strip() for s in args.types.split(",") if s.strip()} or None
    ids = {s.strip() for s in args.ids.split(",") if s.strip()} or None
    return run(types, ids, args.seed, args.model, args.retries)


if __name__ == "__main__":
    sys.exit(main())
