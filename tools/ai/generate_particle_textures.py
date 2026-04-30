#!/usr/bin/env python3
"""
Generate 144 small particle textures (64×64 PNG, transparent black bg) used by
the upgraded ParticleSystem renderer. Output goes into:

    app/src/main/assets/game/particles/<element>_<type>.png    (12 × 10 = 120)
    app/src/main/assets/game/particles/universal_<type>.png    (24)

Free via Pollinations Flux. Run once; assets become available immediately.

Usage:
    py -3 tools/ai/generate_particle_textures.py
    py -3 tools/ai/generate_particle_textures.py --skip-existing
"""
from __future__ import annotations
import argparse
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
DEST = REPO / "app" / "src" / "main" / "assets" / "game" / "particles"

ELEMENTS: dict[str, tuple[str, str]] = {
    # name -> (primary color, descriptor)
    "ember":    ("#F05A28", "warm orange-red flame, fire ember"),
    "tide":     ("#2F9EEA", "cool teal-blue water"),
    "verdance": ("#43A047", "lush deep green leaves"),
    "stone":    ("#8D6E4D", "earthy brown rock"),
    "gale":     ("#B8E0F6", "pale wind-white"),
    "spark":    ("#FFD43B", "electric yellow lightning"),
    "frost":    ("#8FCBE1", "icy pale blue"),
    "metal":    ("#B0BEC5", "silver chrome metallic"),
    "shade":    ("#311B92", "deep dark purple shadow"),
    "radiant":  ("#FFE082", "warm gold sun light"),
    "mind":     ("#A77CFF", "lavender psychic"),
    "echo":     ("#55D6C2", "cyan turquoise resonance"),
}

TYPES = [
    ("spark",       "tiny pointed spark, sharp edges"),
    ("droplet",     "small round droplet shape"),
    ("shard",       "angular crystalline shard fragment"),
    ("aura_ring",   "circular aura ring with hollow center"),
    ("beam_segment","narrow horizontal beam segment with bright core"),
    ("impact_ring", "expanding shockwave ring frame"),
    ("background_glow","soft circular glow blob"),
    ("orb",         "compact glowing orb sphere"),
    ("trail_wisp",  "elongated wisp tail trail"),
    ("sub_particle","tiny diffuse fleck"),
]

UNIVERSAL = [
    ("shockwave",      "white expanding shockwave ring"),
    ("slash",          "diagonal sword slash arc"),
    ("explosion",      "round explosion burst frame"),
    ("speed_lines",    "horizontal motion blur lines"),
    ("lightning_bolt", "jagged lightning bolt single frame"),
    ("glow_orb_white", "pure white soft glow orb"),
    ("glow_orb_gold",  "warm gold soft glow orb"),
    ("smoke_puff",     "soft grey smoke puff"),
    ("ash_fleck",      "tiny dark ash fleck"),
    ("crit_flash",     "bright star burst flash"),
    ("frame_burst",    "cinematic burst frame ring"),
    ("dust_cloud",     "ground dust cloud puff"),
    ("crack_line",     "jagged crack line single stroke"),
    ("droplet_white",  "neutral water droplet"),
    ("petal_light",    "abstract light petal shape"),
    ("rune_mark",      "abstract glowing rune symbol"),
    ("gear_cog",       "small mechanical cog silhouette"),
    ("vortex_swirl",   "compact spiral vortex"),
    ("crystal_chip",   "small angular crystal chip"),
    ("ribbon_arc",     "thin curved ribbon arc"),
    ("pulse_ring",     "double-ring pulse"),
    ("shard_burst",    "starburst of small shards"),
    ("scatter_dot",    "scattered tiny dots"),
    ("halo_thin",      "thin halo ring"),
]

POLLINATIONS = "https://image.pollinations.ai/prompt/"

NEG = (
    "scenery, background details, multiple subjects, text, UI, logo, watermark, "
    "frame, border, photo, 3D render, anime character, complex composition"
)


def gen(prompt: str, dest: Path, seed: int, retries: int = 3) -> bool:
    full = f"{prompt}. Avoid: {NEG}."
    quoted = urllib.parse.quote(full, safe="")
    params = urllib.parse.urlencode({
        "width": 256, "height": 256,        # generated big, downscaled at runtime
        "model": "flux", "seed": seed,
        "nologo": "true", "enhance": "false",
    })
    url = f"{POLLINATIONS}{quoted}?{params}"
    req = urllib.request.Request(url, headers={"User-Agent": "Aetherbound-Particles/1.0"})
    for attempt in range(1, retries + 1):
        try:
            with urllib.request.urlopen(req, timeout=180) as resp:
                data = resp.read()
            if len(data) < 512:
                raise ValueError(f"size {len(data)}B")
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_bytes(data)
            return True
        except Exception as e:
            print(f"    retry {attempt}: {type(e).__name__}: {e}")
            time.sleep(2 * attempt)
    return False


def style_anchor() -> str:
    return (
        "single isolated game-effect particle texture, centered on solid pure "
        "black background, sharp edges, high contrast, additive-blend ready, "
        "soft falloff at edges, abstract VFX texture, no scenery, single subject"
    )


def main(skip_existing: bool) -> int:
    DEST.mkdir(parents=True, exist_ok=True)
    jobs: list[tuple[Path, str, int]] = []
    style = style_anchor()

    # element × type
    for elem, (color, desc) in ELEMENTS.items():
        for type_name, type_desc in TYPES:
            fname = f"{elem}_{type_name}.png"
            prompt = f"{type_desc}, {color} colour, {desc}, {style}"
            seed = abs(hash((elem, type_name))) % 1_000_000
            jobs.append((DEST / fname, prompt, seed))

    # universal
    for type_name, type_desc in UNIVERSAL:
        fname = f"universal_{type_name}.png"
        prompt = f"{type_desc}, {style}"
        seed = abs(hash(("universal", type_name))) % 1_000_000
        jobs.append((DEST / fname, prompt, seed))

    print(f">> {len(jobs)} particle textures queued")
    ok, failed = 0, []
    for i, (dest, prompt, seed) in enumerate(jobs, 1):
        if skip_existing and dest.exists():
            print(f"[{i}/{len(jobs)}] {dest.name} already exists")
            ok += 1; continue
        print(f"[{i}/{len(jobs)}] {dest.name}")
        if gen(prompt, dest, seed):
            print(f"  OK {dest.stat().st_size // 1024} KB")
            ok += 1
        else:
            failed.append(dest.name)
        time.sleep(0.4)

    print(f"-- done | {ok}/{len(jobs)} ok --")
    if failed:
        print(f"  failed: {', '.join(failed[:10])}{'...' if len(failed) > 10 else ''}")
    return 0 if not failed else 1


def parse_args():
    p = argparse.ArgumentParser(description="Generate 144 particle textures via Pollinations")
    p.add_argument("--skip-existing", action="store_true")
    return p.parse_args()


if __name__ == "__main__":
    sys.exit(main(parse_args().skip_existing))
