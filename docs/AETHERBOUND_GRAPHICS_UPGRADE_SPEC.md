# Aetherbound — Graphics Upgrade Spec

**Audience:** AI image-generation pipeline that will upgrade ALL game art.
**Source-of-truth-status:** generated 2026-05-01 from a fresh asset audit.

This single document contains everything the AI needs. Read top-to-bottom.

---

## 0. Global Hard-Rules (apply to EVERY image, every category)

| Rule | Why |
|---|---|
| **Never rename a file.** Always write to the EXACT existing path + filename. | The engine resolves assets by hard-coded paths — a rename = broken sprite. |
| **Match existing dimensions exactly.** If the original is 96×96, the upgrade must be 96×96. | Tile-grid + sprite-anchor math depends on it. |
| **Preserve transparent backgrounds** where the original has alpha. | Every monster / NPC / particle is alpha-cropped onto game scenes. |
| **Stay below the per-file size cap** listed per category. | Total APK budget. |
| **Never add text, watermarks, signatures.** | Game has its own font system. |
| **Style: Pokemon / classic-RPG-mobile aesthetic.** Clean line-art, vibrant saturated colors, readable silhouette at thumbnail size. | Aetherbound's design pillar. |
| **Aetherbound palette accent**: when using gold tones, prefer the 4-stop metallic-gold ramp `#FFF3C4 → #F5D37A → #D4A043 → #8B5F1A`. | Brand identity. |

---

## 1. ATTACK / TECHNIQUE ANIMATIONS

### NEW canonical source: **`app/src/main/assets/content/techniques_v2.json`**

Contains all **600 attacks** Aetherbound ships with — evenly distributed
across all 19 elements + 10 unique legendary signatures.

The legacy `assets/game/data/techniques.json` (274 Tuxemon-imported entries)
is now **deprecated**. Engine will gradually migrate to v2.

### Distribution (600 total)

```
Per element (regular moves):    31-32 attacks
Total regular moves:            590  (590 / 19 ≈ 31 each)
Legendary signatures:            10  (1 per legendary monster)
                                ──────
Total:                          600
```

**All 19 elements covered evenly** — including the 6 new (Dream, Mind,
Sound, Time, Crystal, Blood).

### Power tier distribution (target counts in techniques_v2.json)

| Tier | Aetherbound power | Count |
|---|---:|---:|
| Tiny (priority snap) | 15–35 | 172 |
| Low | 25–50 | 19 |
| Mid (Strike base) | 50–89 | 209 |
| High (Pulse) | 90–139 | 133 |
| Huge (Storm) | 140–220 | 57 |
| **Legendary signature** | 250–320 | 10 |

### 1.5 Legendary Signature Moves (UNIQUE — 1 per legendary, no duplicates)

Each of the 10 legendary echoforms owns one attack **no other monster
in the entire game can learn — not even other legendaries**.

| Owner (Echoform) | Element | Signature | Power |
|---|---|---|---:|
| **Orgasdam** | Cosmic | **Galaktische Explosion** | **320** ⭐ unique top |
| Poton | Shadow | Abyssal-Verschlingung | 305 |
| Indrusdet | Storm | Sturm-Apokalypse | 300 |
| Xazzyrth | Metal | Mithril-Zerschlag | 295 |
| Orru | Ice | Glaziale Singularität | 285 |
| Froizainteo | Wind | Hyperion-Hurrikan | 280 |
| Xundraintu | Earth | Tektonische Verwerfung | 275 |
| Chekyntom | Light | Sternenkrönung | 275 |
| Resosdienth | Wood | Weltbaum-Wurzelschlag | 270 |
| Grutin | Venom | Welken-Plage | 270 |

→ Only **one** attack does 320 dmg in the entire game: "Galaktische Explosion".
   The other 9 spread between 270–305.

In `echoform_matrix.json` each legendary now has a `signature_move` field
that locks ownership — the engine will enforce that no other monster's
moveset can ever include these slugs.

### 1.1 Path & Filename Convention

```
app/src/main/assets/game/techniques/tuxemon/<slug>/frame_0001.png
                                                 frame_0002.png
                                                 …
                                                 frame_NNNN.png
```

`<slug>` = lowercase technique-id (e.g. `bite`, `beam`, `fire_breath`).
**The slug folder name is fixed** — never change it.

### 1.2 Dimensions

| Spec | Value |
|---|---|
| Width × Height | **96 × 96 px** |
| Background | transparent (RGBA PNG) |
| Format | PNG, indexed-256 palette |
| Max file-size per frame | **3 KB** |
| Frame count per technique | **8–24 frames** (min 8 for snap, 24 for Storm/Legendary) |
| Default playback | 24 fps (~42 ms / frame) |

### 1.3 AI Prompt Template — Existing Techniques

For each technique slug, look up in `techniques.json`:

```
{
  "slug": "<slug>",
  "types": ["fire"],          ← element
  "power": 1.4,               ← Tuxemon scale, ×50 ≈ Aetherbound
  "category": "STRIKE",       ← STRIKE | PULSE | BIND | …
  "accuracy": 95
}
```

Prompt template:

```
Pokemon-style attack animation for "<slug>".
PRIMARY ELEMENT: <type> — use that element's canonical palette
  (Fire = orange/red/yellow flame; Water = teal/blue wave; Frost = pale-cyan
  crystalline; Earth = brown/ochre stone; Wood = forest-green leaf;
  Metal = steel-grey + brass; Lightning = electric-violet/yellow;
  Sky = pale-blue + white wind-streaks; Shadow = deep-purple + black-smoke;
  Heroic/Light = radiant gold + ivory; Venom = sickly green + bile-yellow;
  Cosmic = nebula-purple + starlight pinpricks; Normal/Aether = iridescent gold).
POWER TIER: <tier from table above>. Tiny = single quick burst, 8 frames.
  Mid = clear strike + impact, 12-16 frames. Huge = dramatic
  charge + arc + screen-filling impact + after-glow, 20-24 frames.
CATEGORY:
  STRIKE = focused projectile or melee impact
  PULSE = wave / breath / area effect
  BIND = constraining vines / chains / coils
DIMENSIONS: 96×96 PNG per frame, transparent background, indexed-256 palette,
  ≤ 3 KB per frame. CRITICAL: keep slug folder name and frame_XXXX.png
  numbering EXACT — engine reads them in order.
NEVER include text, signature, or watermark.
```

### 1.4 NEW Techniques for the 6 New Elements (~50 to author)

Create new technique animations + JSON entries for the 6 new aspects.
**Suggested split** (~50 new total, to fill the table):

| New Element | # New Moves | Mood / Motif |
|---|---:|---|
| **Dream** | 8 | misty pastels, butterfly-wing arcs, sleep-Z's, dream-bubble pop |
| **Mind** | 9 | psychic-violet ripples, fractal-iris pulse, telekinetic-spoon-bend |
| **Sound** | 8 | concentric blue-pink shock-rings, tympanum-ripple, tuning-fork resonance |
| **Time** | 8 | sand-streams, hourglass markings, sepia-tint, slow-mo trail |
| **Crystal** | 9 | prismatic refractions, geode-burst, faceted shard arcs |
| **Blood** | 8 | crimson-pulse, vein-trace, drip-dagger, heart-shockwave |

Suggested slugs to introduce: `dream_haze`, `nightmare_call`, `mind_lance`,
`fractal_pulse`, `sonic_burst`, `chronos_echo`, `prism_break`, `blood_pact`,
`vein_strike`, … (full list in `docs/element-chart.json` for reference).

---

## 2. NPC / CHARACTER SPRITES

### Counts

- **357 character variants** in `assets/game/characters/tuxemon/<archetype>[_variant]/`
- **119 distinct archetypes** (barmaid, alchemist, aviator, knight, alien, …)

### 2.1 Path & Filename Convention

```
app/src/main/assets/game/characters/tuxemon/<archetype>/sheet.png
                                                       sheet_<variant>.png
```

E.g. `barmaid/sheet.png`, `barmaid_alt2/sheet.png`, `barmaid_blonde/sheet.png`.
**Variant suffixes are part of the archetype identity** — keep them.

### 2.2 Dimensions

| Spec | Value |
|---|---|
| Sprite-sheet layout | **4 cols × 4 rows** (4 facings × 4 walk-frames) |
| Frame size | **16 × 24 px** |
| Total sheet size | **64 × 96 px** |
| Background | transparent |
| Max file-size | **6 KB** per sheet |

### 2.3 AI Prompt Template — Existing NPCs

```
Pokemon-style character walking sprite for archetype "<archetype>".
4-direction × 4-frame walking-cycle on a 4×4 grid:
  ROW 1: facing south (toward camera)
  ROW 2: facing north (away)
  ROW 3: facing west (left)
  ROW 4: facing east (right)
  Each row: 4 frames (idle, step-left, idle, step-right).
ARCHETYPE FLAVOR: see below.
DIMENSIONS: 64×96 px PNG total (each frame 16×24), transparent background,
  indexed palette, ≤ 6 KB. NEVER change filename or grid order.
STYLE: clean Pokemon-overworld pixel-art, readable silhouette at small size,
  no text or watermarks.
```

### 2.4 Archetype-Flavor Cheat-Sheet

| Archetype | Visual brief |
|---|---|
| `barmaid` | apron, holding a mug or cloth, hair tied back, friendly |
| `alchemist` | robe, vial-belt, wild eyes, slightly singed cuffs |
| `aviator` | leather jacket, cap with goggles, scarf trailing |
| `knight` / `paladin` | plate armor, plumed helm, sheathed sword |
| `merchant` | ledger or coin-pouch, vest, waist-bag |
| `monk` / `buddha-style` | simple robes, bare feet or sandals, prayer beads |
| `alien` | thin-limbed, oversized eyes, exotic head-shape |
| `child` / `kid` | shorter (3/4 height), oversized backpack or toy |
| `beachgoer` | swimsuit, towel, sunglasses |
| `fisherman` | rolled-up trousers, rod or net |
| `coolDude` / `baller` | streetwear, snapback or shades |
| `chrome_robo` | metallic chassis, glowing eye-bar |
| `cochini` | unique character — keep folkloric folk-style outfit |

When `_alt1`/`_alt2`/`_blonde`/`_red` etc. variants exist, change ONLY hair
or palette — keep silhouette + outfit identical to base.

---

## 3. WORLD TILESETS

### Counts

- **78 tileset PNG sheets** in `assets/game/tilesets/[tuxemon/]`
- **25 autotile sheets** in `assets/game/autotiles/`

### 3.1 Path & Filename Convention

```
app/src/main/assets/game/tilesets/<sheet>.png
app/src/main/assets/game/tilesets/tuxemon/<sheet>.png
app/src/main/assets/game/autotiles/<autotile>.png
```

**Filenames are referenced by every TMX map** — never rename.

### 3.2 Dimensions

| Spec | Value |
|---|---|
| Tile cell size | **16 × 16 px** (always) |
| Sheet width | **divisible by 16** (most are 256 or 512 wide) |
| Sheet height | **divisible by 16** |
| Background | transparent for object/decoration tiles; solid for terrain |
| Max file-size | **350 KB** per sheet (typical 50–200 KB) |
| Format | PNG (32-bit ok for terrain, indexed-256 for objects) |

### 3.3 AI Prompt Template — Tilesets

```
Pokemon-style top-down 16×16 tile sheet for "<biome>".
LAYOUT: keep the EXACT tile-grid as the source — same number of columns
  and rows, same tile-positions. Each 16×16 cell = one game-world tile.
BIOME: <biome> (Forest / Desert / Cave / Town / Mountain / Beach / Marsh /
  Sanctum / Castle / Plains / Harbor / WaterSurface).
PERSPECTIVE: top-down, slight south-east light, no parallax.
STYLE: clean Pokemon-DS pixel-art, vibrant saturated colors, readable
  shapes at full zoom AND when stitched into a 32-tile-wide map.
TRANSITIONS: tiles meant to autotile (water-edge, grass-edge, sand-edge)
  must stay symmetric — top-edge, bottom-edge, corners must match
  the original cell positions.
DIMENSIONS: same width × height as source PNG, 16×16 cells, ≤ 350 KB.
NEVER change file dimensions, never reorder tiles within the sheet.
```

### 3.4 Top-priority sheets to upgrade (largest = most-used)

1. `tuxemon/core_outdoor_nature.png` — 355 KB, forest/grass/trees
2. `outside_e.png` — 273 KB, generic outdoor terrain
3. `tuxemon/core_outdoor.png` — 194 KB, paths/roads
4. `tuxemon/core_outdoor_water.png` — 192 KB, water variants
5. `tuxemon/oceanset_outside.tiles.png` — 145 KB, beach/ocean
6. `tuxemon/Interiors_16x16.png` — 116 KB, indoor floors/walls
7. `tuxemon/core_set pieces.png` — 110 KB, props/furniture
8. `tuxemon/core_indoor_floors.png` — 108 KB
9. `tuxemon/rubberduck_outdoor.png` — 94 KB
10. `tuxemon/core_indoor_stairs.png` — 78 KB

Improve those 10 first → biggest visual impact for ~80% of map area.

---

## 4. AUTOTILES (water-edge, grass-edge, etc.)

### 4.1 Path

```
app/src/main/assets/game/autotiles/<name>.png
```

### 4.2 Dimensions

| Spec | Value |
|---|---|
| Format | RM2K-style 96 × 128 px (4 cols × 6 rows of 8 × 8 px sub-tiles) OR Tuxemon-flat 16-tile grid |
| Background | transparent corners |
| Max file-size | **20 KB** |

### 4.3 Prompt template

Keep the source layout EXACT. Only repaint the visual style.
Autotile generators slice these by hard-coded position — any layout shift
breaks ALL water/grass/sand transitions on every map.

---

## 5. ITEMS (potion icons, evolution stones, …)

- **177 item icon PNGs** in `assets/game/items/`

### 5.1 Path & Filenames

```
app/src/main/assets/game/items/<slug>.png
```

`<slug>` is referenced from `ShopCatalog.kt` + inventory IDs — DO NOT rename.

### 5.2 Dimensions

| Spec | Value |
|---|---|
| Width × Height | **64 × 64 px** |
| Background | transparent |
| Max file-size | **3 KB** |
| Style | clean inventory-icon, single object centered |

### 5.3 Categories needing love (priority order)

1. **19 evolution stones** (`fire_stone`, `water_stone`, `frost_stone`, `earth_stone`,
   `leaf_stone`, `metal_stone`, `thunder_stone`, `wind_stone`, `dusk_stone`,
   `sun_stone`, `venom_stone`, `cosmic_stone`, `aether_stone`, `dream_stone`,
   `psychic_stone`, `echo_stone`, `chronos_stone`, `prism_stone`, `blood_stone`)
   — each should LOOK like its element (fire_stone = molten ember inside red gem;
   blood_stone = pulsing crimson with vein-traces; chronos_stone = sand drifting
   inside an hourglass-cut diamond, etc.)
2. **Potions** (`potion`, `super_potion`, `imperial_potion`, `mega_potion`)
3. **Echoform-Prismen** (was Tuxeballs — `aether_prism`, `glass_prism`, …)
4. **Berries** (13 elemental, one per legacy aspect)
5. **Repels, Mystery-Tea, etc.**

### 5.4 Prompt template

```
Pokemon-style inventory icon for "<slug>".
SUBJECT: single item, centered in frame, slight diagonal lighting top-left.
DIMENSIONS: 64×64 PNG, transparent background, indexed-256 palette, ≤ 3 KB.
STYLE: clean line-art, vibrant colors, readable at thumbnail size,
  no text or watermark, NEVER change the filename.
```

---

## 6. UI ELEMENTS

- **252 UI PNGs** in `assets/game/ui/`

These include: dialog frames, button backgrounds, menu corners, HUD chips,
type-icon badges, status-effect glyphs.

### 6.1 Hard rule

UI assets are tied to specific layout-pixels in Compose. **Match dimensions
EXACTLY** — even a 1-pixel difference breaks rounded-corner overlays.

### 6.2 Prompt template

```
Pokemon-style UI element: "<asset name>".
DIMENSIONS: same as source PNG (do NOT modify width/height), transparent
  background where source is transparent.
STYLE: Aetherbound theme — obsidian-dark base, metallic-gold accents
  (4-stop ramp #FFF3C4 → #F5D37A → #D4A043 → #8B5F1A), clean line-art,
  high contrast, readable on small phone screens.
NO text — Compose draws labels separately.
```

---

## 7. PARTICLES

- **144 particle PNGs** in `assets/game/particles/`

Sparks, embers, frost shards, mist puffs, leaf flutter, etc. Used as
animated overlays during attack effects (see § 1).

### 7.1 Hard rule

Particle textures are loaded by exact key string from
`ParticleTextures.kt`. **Match filename + dimensions exactly.**

### 7.2 Dimensions

| Spec | Value |
|---|---|
| Width × Height | usually **8 × 8** or **16 × 16** px |
| Background | transparent |
| Max file-size | **1.5 KB** per texture |

### 7.3 Prompt template

```
Pokemon-style particle texture: "<element>_<kind>".
KIND vocabulary:
  spark = bright pinpoint with 4 short radial streaks
  orb = soft glowing ball
  shard = angular fragment / shrapnel
  glow = soft radial gradient
  background_glow = wider radial glow
  sub_particle = small accent dot
ELEMENT palette per § 1.3.
DIMENSIONS: 8×8 (or 16×16 if source is 16×16) PNG, transparent background,
  ≤ 1.5 KB. Preserve exact filename.
```

---

## 8. CITIES / STYLE-BOARDS / CONCEPTS

### Counts

- `cities/` — 1 PNG (Phase-2 production city)
- `style-boards/` — 4 PNGs (approval artifacts, NOT in-game)
- `concepts/` — 0 PNGs

These are big production-render assets used as battle-arena backdrops.

### 8.1 Path

```
app/src/main/assets/game/cities/<region>.png
app/src/main/assets/game/style-boards/<board>.png
```

### 8.2 Dimensions

| Spec | Value |
|---|---|
| City backdrop | **1024 × 768 px** (landscape, full-bleed) |
| Format | JPG ok (large file), PNG for transparency |
| Max file-size | **300 KB** |
| Style | painterly Pokemon-route concept-art, atmospheric |

### 8.3 Prompt template

```
Painterly Pokemon-style top-down-isometric city / region backdrop for
"<region>". 1024×768 px, atmospheric perspective, slight south-east
lighting, vibrant saturated colors, no text.
SCENE: <one-sentence description of the city — see Aetherbound lore in
docs/, e.g. "Namaris Harbor — coastal town with lighthouse, fishing boats,
gold-trimmed obsidian rooftops, cliff backdrop">.
```

---

## 9. PLAYER / PORTRAITS / VFX (currently empty)

These directories exist but contain 0 PNGs:

- `player/` — needs the player avatar walking-sprite (same spec as § 2)
- `portraits/` — needs dialog face-shots (256×256, transparent BG)
- `vfx/` — needs full-screen effect overlays (1024×768 PNG, transparent)

**Priority: low.** These are nice-to-have once core monster art is done.

---

## 10. Workflow Recommendation

1. **Phase 1 — Echoforms** (already started): see `docs/echoform_matrix.csv`
2. **Phase 2 — Top 10 tilesets** (§ 3.4) → biggest visual impact
3. **Phase 3 — NPCs** (priority archetypes: barmaid / alchemist / aviator / monk / merchant / kid / alien)
4. **Phase 4 — 19 evolution stones** (§ 5.3 priority 1)
5. **Phase 5 — Existing 274 attack animations** + 50 new for Dream/Mind/Sound/Time/Crystal/Blood
6. **Phase 6 — Items / particles / UI**
7. **Phase 7 — City backdrops / portraits / vfx**

---

## 11. Validation Script (recommended)

After each batch the AI generates, run:

```python
import os, struct
from pathlib import Path
for p in Path("assets/game").rglob("*.png"):
    with open(p, "rb") as f:
        sig = f.read(8); assert sig[:8] == b"\\x89PNG\\r\\n\\x1a\\n", p
    size_kb = p.stat().st_size / 1024
    # category-specific size checks (see per-category table above)
```

If any file:
- has been **renamed** → fail
- exceeds **size cap** → fail
- has **wrong dimensions** → fail
- has **opaque BG** where transparent expected → fail

---

## 12. SUMMARY — What the AI processes

| Category | Files | Per-file cap | Total cap |
|---|---:|---:|---:|
| Echoforms (§ existing csv) | 3 000 (1000 × 3 views) | 5 KB | 15 MB |
| Attack animations | 274 × ~16 frames ≈ 4 400 | 3 KB | 13 MB |
| NPCs | 357 sheets | 6 KB | 2 MB |
| Tilesets | 78 sheets | 350 KB | 25 MB |
| Autotiles | 25 sheets | 20 KB | 0.5 MB |
| Items | 177 icons | 3 KB | 0.5 MB |
| UI | 252 elements | varies | 5 MB |
| Particles | 144 textures | 1.5 KB | 0.2 MB |
| Cities + boards | ~5 | 300 KB | 1.5 MB |
| **Σ asset budget** | ~8 700 files | — | **~62 MB** |

**Comfortable for an Android APK** (typical RPG ships 50–150 MB).
