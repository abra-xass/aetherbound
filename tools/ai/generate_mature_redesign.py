#!/usr/bin/env python3
"""
For each Echoform front-PNG:
  1. Backup original (irreversible safety net)
  2. Pollinations vision describes the creature in 1 sentence (identity anchor)
  3. Pollinations img2img generates a "less childish, more mature" FRONT,
     overwriting the original PNG
  4. Pollinations img2img generates a back-view with the same maturity
     direction, saved as `<id>@back.png`

The img2img call uses the front PNG as conditioning + the description as
anchor. Strength 0.6 = noticeable redesign while still preserving body
silhouette and palette.

Usage:
    py -3 tools/ai/generate_mature_redesign.py --ids E001,E002      # test
    py -3 tools/ai/generate_mature_redesign.py --skip-existing-back # safer rerun
    py -3 tools/ai/generate_mature_redesign.py                       # all 300
"""
from __future__ import annotations
import argparse
import http.server
import json
import os
import shutil
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
BACKUP = REPO / "tools" / "ai" / "echoforms-backup-pre-mature"

VISION_URL = "https://text.pollinations.ai/openai"
IMAGE_BASE = "https://image.pollinations.ai/prompt/"
LOCAL_PORT = 7777


# ── Local HTTP server so Pollinations can fetch the front-PNGs ──
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
        s.connect(("8.8.8.8", 80)); return s.getsockname()[0]
    finally:
        s.close()


# ── Vision: 1-sentence identity anchor ──
def describe(image_url: str, retries: int = 3) -> str | None:
    body = {
        "model": "openai",
        "messages": [
            {"role": "system", "content":
                "You are a pixel-art art director. Reply with ONE concise sentence "
                "describing the creature's body shape, colour palette, and signature "
                "physical features. No naming, under 28 words."},
            {"role": "user", "content": [
                {"type": "text", "text": "Describe this monster sprite."},
                {"type": "image_url", "image_url": {"url": image_url}},
            ]},
        ],
        "max_tokens": 70,
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


# ── Img2img: front conditioning + maturity / view prompt ──
def gen(
    front_url: str, description: str, view: str,
    dest: Path, seed: int, strength: float, retries: int = 3,
) -> bool:
    """view = 'front' or 'back'."""
    if view == "back":
        view_clause = (
            # Classic GBA Pokemon back-sprite framing: not a pure rear shot
            # (would just be a silhouette) — instead camera sits behind the
            # creature and rotated ~45° to the right, so you see the rear of
            # the head plus one shoulder profile. This is the "three-quarter
            # rear view" in art-direction terms.
            "three-quarter rear view rotated 45 degrees to the right, the "
            "creature is facing away from the viewer with its body angled to "
            "the right, you see the back of the head, one shoulder silhouette, "
            "and the rear posture of the body, classic back-sprite framing as "
            "in 16-bit RPG battle scenes"
        )
    else:
        view_clause = (
            "FRONT view facing the viewer, like the reference but redesigned "
            "with more mature aesthetic"
        )
    prompt = (
        f"{description}, redesigned to look less childish and more mature for "
        f"adult players, weathered serious dark-fantasy aesthetic, sharper "
        f"angular features, less round and cute, less candy-coloured, "
        f"{view_clause}, single isolated subject centered on solid pure black "
        f"background, polished 16-bit GBA-era pixel art battle sprite, sharp "
        f"limited 32-color palette, retro 2D monster RPG creature art, "
        f"preserve the same body shape and primary colour palette as the "
        f"reference, original character not derived from any IP"
    )
    negative = (
        "childish, cute, kawaii, chibi, big-eyes anime, soft pastel colours, "
        "cartoonish, sticker style, multiple subjects, scenery, text, UI, "
        "logo, watermark, photo, 3D render, painterly, Pokemon clone, Digimon clone"
    )
    full = f"{prompt}. Avoid: {negative}."
    quoted = urllib.parse.quote(full, safe="")
    params = urllib.parse.urlencode({
        "width": 512, "height": 512,
        "model": "flux", "seed": seed,
        "image": front_url,
        "strength": f"{strength:.2f}",
        "nologo": "true", "enhance": "false",
    })
    url = f"{IMAGE_BASE}{quoted}?{params}"
    req = urllib.request.Request(url, headers={"User-Agent": "Aetherbound-Mature/1.0"})
    for attempt in range(1, retries + 1):
        try:
            with urllib.request.urlopen(req, timeout=180) as resp:
                data = resp.read()
            if len(data) < 1024:
                raise ValueError(f"size {len(data)}B")
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_bytes(data)
            return True
        except Exception as e:
            print(f"    {view} retry {attempt}: {type(e).__name__}: {e}")
            time.sleep(2 * attempt)
    return False


def main(ids_filter: set[str] | None, strength: float, skip_existing_back: bool) -> int:
    if not ASSETS.is_dir():
        print(f"FATAL: {ASSETS} missing", file=sys.stderr); return 2

    fronts = sorted(p for p in ASSETS.glob("E*.png") if "@" not in p.stem)
    if ids_filter:
        fronts = [p for p in fronts if p.stem in ids_filter]
    if not fronts:
        print("no fronts to process"); return 1

    BACKUP.mkdir(parents=True, exist_ok=True)
    print(f">> hosting {ASSETS} on local HTTP for img2img...")
    srv, port = start_local_server(ASSETS)
    ip = get_lan_ip()
    base = f"http://{ip}:{port}"
    print(f"   serving at {base}")
    print(f">> {len(fronts)} echoforms | strength={strength}")

    ok, failed = 0, []
    try:
        for i, front in enumerate(fronts, 1):
            sid = front.stem
            back = front.with_name(f"{sid}@back.png")

            # 1. Backup (only if not already backed up)
            backup_path = BACKUP / front.name
            if not backup_path.exists():
                shutil.copy2(front, backup_path)

            # 2. Vision anchor
            front_url = f"{base}/{front.name}"
            desc = describe(front_url) or "original 16-bit pixel-art monster sprite"
            print(f"[{i}/{len(fronts)}] {sid}  desc: {desc[:60]}…")

            seed_front = abs(hash((sid, "front"))) % 1_000_000
            seed_back = abs(hash((sid, "back"))) % 1_000_000

            # 3. Mature front (overwrites original)
            front_ok = gen(front_url, desc, "front", front, seed_front, strength)
            if front_ok:
                print(f"  OK front ({front.stat().st_size // 1024} KB)")
            else:
                print(f"  X front failed — restoring backup")
                shutil.copy2(backup_path, front)
                failed.append(f"{sid}-front")
                continue

            # Re-host updated front for back generation? No — Pollinations
            # already has the original cached as conditioning. Use BACKUP
            # PNG as conditioning for the back to keep consistency.
            backup_url = f"{base}/{front.name}"  # the dir hosts updated; backup is elsewhere
            # Actually use original via local HTTP — to do that, we'd need to
            # also host BACKUP. Simpler: pass the (just-overwritten) mature
            # front as conditioning, since back should match the mature design.

            # 4. Mature back
            if skip_existing_back and back.exists():
                print(f"  back already exists, skipping")
            else:
                back_ok = gen(front_url, desc, "back", back, seed_back, strength)
                if back_ok:
                    print(f"  OK back ({back.stat().st_size // 1024} KB)")
                else:
                    failed.append(f"{sid}-back")
            ok += 1
            time.sleep(0.6)
    finally:
        srv.shutdown()

    print(f"-- done | {ok}/{len(fronts)} ok --")
    if failed:
        print(f"  failed: {', '.join(failed[:10])}{'...' if len(failed) > 10 else ''}")
    print(f"\nBackups in: {BACKUP}")
    print("Restore with: cp tools/ai/echoforms-backup-pre-mature/E001.png app/src/main/assets/game/echoforms/")
    return 0 if not failed else 1


def parse_args():
    p = argparse.ArgumentParser(description="Mature redesign + back-view via Pollinations img2img")
    p.add_argument("--ids", default="", help="comma-separated id filter, e.g. E001,E002")
    p.add_argument("--strength", type=float, default=0.60,
                   help="img2img strength (0.4=conservative, 0.7=aggressive)")
    p.add_argument("--skip-existing-back", action="store_true")
    return p.parse_args()


if __name__ == "__main__":
    a = parse_args()
    ids = {s.strip() for s in a.ids.split(",") if s.strip()} or None
    sys.exit(main(ids, a.strength, a.skip_existing_back))
