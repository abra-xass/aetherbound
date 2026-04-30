# Aetherbound

Pokémon-style monster RPG for Android, built on **Tuxemon's** open-source
content layer (411 creatures, 274 techniques, 223 items, 235 maps) and
integrated with the **Thot encrypted messenger** for end-to-end
encrypted multiplayer over Tor.

```
                ╔══════════════════════════════════╗
                ║   AETHERBOUND  · pilot 0.1.0      ║
                ╚══════════════════════════════════╝
```

## What it is

- **Single-player JRPG** — capture monsters, level them up, evolve them,
  fight trainer battles, save progress across 5 slots
- **Multiplayer via Thot** — every Thot DM is a battle channel; tap ⚔️
  in chat to challenge, no friend codes
- **Tor-mandatory networking** — battles ride on Thot's encrypted
  Matrix transport; structurally impossible to bypass
- **Real-time day/night cycle** — encounter pools shift with your wall
  clock (shadow Echoforms only at night, etc.)
- **Aetherbound theme** — obsidian + 4-stop gold, Cinzel typography,
  no Material default leak

## Install

### From inside the game (recommended)

If you already have Aetherbound, open MENU → **Check for Updates** —
APK pulls directly from this repo, one tap to install. No browser
detour.

### Fresh install

Download `aetherbound-<version>.apk` from the
[latest release](https://github.com/abra-xass/Aetherbound/releases/latest)
and tap to install. You'll need to allow "Install from unknown sources"
once.

### Asset Packs (optional)

Core APK is ~80 MB. Open MENU → **Asset Packs** to download:

- `audio.zip` (~145 MB) — 192 SFX + 111 BGM tracks
- `graphics.zip` (~25 MB) — full move-FX animations for 274 techniques
- `maps.zip` (~6 MB) — all 235 imported Tuxemon maps

Pack downloads land in app-private storage, auto-cleared on uninstall.

## Build from source

```bash
git clone https://github.com/abra-xass/Aetherbound.git
cd Aetherbound
git clone --depth 1 https://github.com/Tuxemon/Tuxemon.git ../Tuxemon
python tools/ai/import_tuxemon.py
./gradlew :app:assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

For release builds + signing config, see
[`docs/release-process.md`](docs/release-process.md).

## Architecture

```
TITLE
  ↓
TUXEMON-WORLD     (.tmx maps, real-clock day/night, biome BGM)
  ├─ Movement+Collision (TmxLoader → TmxRenderer → CollisionMap)
  ├─ Encounter (411 mons via TuxemonEchoformDex.encounterPool)
  └─ Battle (JSON-driven, deterministic resolver)
        ├─ DamageVariance (crit/STAB/range)
        ├─ StatusEngine (poison/burn/sleep/freeze/paralyze/confuse)
        ├─ CaptureMath (8 ball types) → CaptureAnimation
        ├─ ExperienceEngine → LevelUpResult
        └─ EvolutionEngine

MENU (8 entries)
  ├─ Party · Bag · Bestiary · Trainer Card · Settings
  ├─ Asset Packs · Check for Updates
  └─ Save · Load (5 slots, JSON)

AUDIO (192 SFX + 111 BGM, pack-aware)
  └─ Auto-fires on: scene change, menu click, encounter trigger
```

## Multiplayer

Aetherbound multiplayer works exclusively through the Thot Messenger
(Matrix). Three button states in the Thot composer:

| Button | Condition | Tap behaviour |
|---|---|---|
| ⚔️ gold | Both have Aetherbound | Battle invite |
| ⚔️➕ | Only sender has it | "Lust mitzuspielen?" + install link |
| (hidden) | Neither has it | No button shown |

See [`docs/multiplayer-thot-binding.md`](docs/multiplayer-thot-binding.md)
for the full Matrix custom-event-type specification.

## License + Attribution

- **Aetherbound code** — your license terms TBD; Aetherbound contributors
- **Tuxemon assets** — CC-BY-SA-3.0, Tuxemon project ([credits](docs/asset-credits.md))
- **Tuxemon engine code (referenced for stat/shape/element formulas)** — GPL-3.0

The asset import layer (anything under `assets/{game,audio}/.../tuxemon/`)
is therefore CC-BY-SA-3.0. Original Aetherbound code (engine, render, UI)
remains under our own license. See [`docs/asset-credits.md`](docs/asset-credits.md).

## Status

Pilot release. The engine layer is complete; UI polish + multiplayer
bridge implementation in Thot are the next milestones. See
[`docs/gap-analysis.md`](docs/gap-analysis.md) for the full feature
inventory.
