#!/usr/bin/env python3
"""
Generate AI back-views via Pollinations img2img.

How it works:
  1. Local HTTP server hosts assets/game/echoforms/ (so Pollinations can fetch front-PNGs).
  2. For each E001.png … E300.png:
        a. Optional vision pass (text.pollinations.ai) for a 1-sentence description
           of the creature — used as the prompt anchor.
        b. Image-gen call to image.pollinations.ai with `image=<front_url>` and
           `strength` ~0.55 — Pollinations conditions the generation on the
           actual front-view pixels and re-renders from behind.
  3. Save as `<id>@back.png` next to the front.

Pollinations img2img preserves body/colour/silhouette much better than
text-only generation. Expected fidelity: ~70-85% (vs 50-70% for text-only).
For pixel-perfect 100%, you need a hand-drawn back-view per creature.

Usage:
    py -3 tools/ai/generate_backviews.py                 # all 300
    py -3 tools/ai/generate_backviews.py --ids E001,E002 # one or several
    py -3 tools/ai/generate_backviews.py --skip-existing # resume after failures
    py -3 tools/ai/generate_backviews.py --strength 0.45 # stronger fidelity
    py -3 tools/ai/generate_backviews.py --no-vision     # skip vision step
"""
from __future__ import annotations
import argparse
import http.server
import json
import os
import socket
import socketserver
import sys
import threading
import time
import urllib.parse
import urllib.request
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
ASSETS = REPO / "app" / "src" / "main" / "assets" / "game" / "echoforms"

VISION_URL = "https://text.pollinations.ai/openai"
IMAGE_BASE = "https://image.pollinations.ai/prompt/"
LOCAL_PORT = 7777


# ── HTTP server: makes ASSETS/ reachable as http://<lan-ip>:7777/E001.png ──
class _Quiet(http.server.SimpleHTTPRequestHandler):
    def log_message(self, *_): pass


def start_local_server(directory: Path) -> tuple[socketserver.TCPServer, int]:
    os.chdir(directory)
    port = LOCAL_PORT
    while True:
        try:
            srv = socketserver.TCPServer(("0.0.0.0", port), _Quiet)
            break
        except OSError:
            port += 1
    threading.Thread(target=srv.serve_forever, daemon=True).start()
    return srv, port


def get_lan_ip() -> str:
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("8.8.8.8", 80))
        return s.getsockname()[0]
    finally:
        s.close()


# ── Optional: 1-sentence anchor description from vision endpoint ──
def describe(image_url: str, retries: int = 3) -> str | None:
    body = {
        "model": "openai",
        "messages": [
            {"role": "system", "content":
                "You are a pixel-art art director. Reply with ONE concise sentence "
                "describing the creature's body shape, colour palette, and signature "
                "features. No naming, under 25 words."},
            {"role": "user", "content": [
                {"type": "text", "text": "Describe this monster sprite."},
                {"type": "image_url", "image_url": {"url": image_url}},
            ]},
        ],
        "max_tokens": 60,
    }
    req = urllib.request.Request(
        VISION_URL,
        data=json.dumps(body).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    for attempt in range(1, retries + 1):
        try:
            with urllib.request.urlopen(req, timeout=120) as resp:
                payload = json.loads(resp.read().decode("utf-8"))
            return payload["choices"][0]["message"]["content"].strip()
        except Exception as e:
            print(f"    vision retry {attempt}: {type(e).__name__}: {e}")
            time.sleep(2 * attempt)
    return None


# ── img2img: front PNG URL + back-view prompt → back PNG ─────────
def gen_backview(
    front_url: str,
    description: str | None,
    dest: Path,
    seed: int,
    strength: float,
    retries: int = 3,
) -> bool:
    anchor = (description or "original 16-bit pixel-art monster sprite") + ", "
    prompt = (
        anchor +
        "BACK view facing away from the viewer, single isolated subject centered "
        "on solid pure black background, polished 16-bit GBA-era pixel art battle "
        "sprite, sharp limited 32-color palette, retro 2D monster RPG creature art, "
        "preserve the same body shape and colour palette as the reference, "
        "show the back of the head and rear silhouette, "
        "original character not derived from any IP"
    )
    negative = (
        "front view, face visible, eyes visible, mirrored front-view, multiple subjects, "
        "scenery, text, UI, logo, watermark, photo, 3D render, anime, chibi, painterly, "
        "Pokemon clone, Digimon clone"
    )
    full = f"{prompt}. Avoid: {negative}."
    quoted = urllib.parse.quote(full, safe="")
    params = urllib.parse.urlencode({
        "width": 512,
        "height": 512,
        "model": "flux",
        "seed": seed,
        "image": front_url,
        "strength": f"{strength:.2f}",
        "nologo": "true",
        "enhance": "false",
    })
    url = f"{IMAGE_BASE}{quoted}?{params}"
    req = urllib.request.Request(url, headers={"User-Agent": "Aetherbound-Backview/2.0"})
    for attempt in range(1, retries + 1):
        try:
            with urllib.request.urlopen(req, timeout=180) as resp:
                data = resp.read()
            if len(data) < 1024:
                raise ValueError(f"suspicious size {len(data)}B")
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_bytes(data)
            return True
        except Exception as e:
            print(f"    image retry {attempt}: {type(e).__name__}: {e}")
            time.sleep(2 * attempt)
    return False


def main(ids_filter: set[str] | None, skip_existing: bool, strength: float, use_vision: bool) -> int:
    if not ASSETS.is_dir():
        print(f"FATAL: {ASSETS} missing", file=sys.stderr); return 2

    fronts = sorted(p for p in ASSETS.glob("E*.png") if "@" not in p.stem)
    if ids_filter:
        fronts = [p for p in fronts if p.stem in ids_filter]
    if not fronts:
        print("no front PNGs match filter"); return 1

    print(f">> hosting {ASSETS} on local HTTP for Pollinations img2img...")
    srv, port = start_local_server(ASSETS)
    ip = get_lan_ip()
    base = f"http://{ip}:{port}"
    print(f"   serving at {base}")
    print(f">> {len(fronts)} echoforms queued | strength={strength} | vision={use_vision}")

    ok, failed = 0, []
    try:
        for i, front in enumerate(fronts, 1):
            sid = front.stem
            back = front.with_name(f"{sid}@back.png")
            if skip_existing and back.exists():
                print(f"[{i}/{len(fronts)}] {sid} already done")
                ok += 1; continue
            front_url = f"{base}/{front.name}"
            desc = describe(front_url) if use_vision else None
            if use_vision and desc:
                print(f"[{i}/{len(fronts)}] {sid}  desc: {desc[:70]}")
            else:
                print(f"[{i}/{len(fronts)}] {sid}")
            seed = abs(hash(sid)) % 1_000_000
            if gen_backview(front_url, desc, back, seed, strength):
                size = back.stat().st_size // 1024
                print(f"  OK {size} KB -> {back.name}")
                ok += 1
            else:
                failed.append(sid)
            time.sleep(0.5)
    finally:
        srv.shutdown()

    print(f"-- done | {ok}/{len(fronts)} ok --")
    if failed:
        print(f"  failed: {', '.join(failed[:10])}{'...' if len(failed) > 10 else ''}")
    return 0 if not failed else 1


def parse_args():
    p = argparse.ArgumentParser(description="Generate Echoform back-views via Pollinations img2img")
    p.add_argument("--ids", default="", help="comma-separated id filter")
    p.add_argument("--skip-existing", action="store_true")
    p.add_argument("--strength", type=float, default=0.55,
                   help="img2img strength 0.0-1.0 (lower = closer to front)")
    p.add_argument("--no-vision", action="store_true",
                   help="skip the vision-description anchor step")
    return p.parse_args()


if __name__ == "__main__":
    a = parse_args()
    ids = {s.strip() for s in a.ids.split(",") if s.strip()} or None
    sys.exit(main(ids, a.skip_existing, a.strength, not a.no_vision))
