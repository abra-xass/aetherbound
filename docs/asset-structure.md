# Aetherbound — Asset Structure

## Design-Prinzip: **lookup mit fallback**

Jedes Asset hat zwei mögliche Quellen:

1. **`overrides/<id>/`** — von dir/Künstler aufpoliert (höchste Priorität)
2. **`tuxemon/<id>/`** — Original Tuxemon-Asset (CC-BY-SA-3.0, Fallback)

Der Engine-Loader prüft zuerst overrides, fällt dann auf tuxemon zurück. Drop-In-Replacement: einzelne Sprites verbessern ohne Original anzufassen.

## Folder-Layout

```
app/src/main/assets/game/
├── echoforms/                       # 411 Monster
│   ├── tuxemon/
│   │   ├── agnidon/
│   │   │   ├── front.png            # battle front-view
│   │   │   ├── back.png             # battle back-view
│   │   │   ├── menu.png             # 32×32 inventory icon
│   │   │   └── portrait.png         # codex/dex art (optional)
│   │   ├── rockitten/...
│   │   └── ... (411 ordner)
│   └── overrides/
│       └── agnidon/                 # nur wenn poliert
│           └── front.png            # höchste Prio
│
├── techniques/                      # 274 Move-Animationen
│   ├── tuxemon/
│   │   ├── fire_ball/
│   │   │   ├── frame_0001.png … frame_0024.png
│   │   │   └── meta.json            # { duration_ms, hit_frame, loop, sfx_key }
│   │   └── ... (274 ordner)
│   └── overrides/
│
├── items/                           # 223 Items
│   ├── tuxemon/
│   │   ├── potion.png               # 32×32 inventory icon
│   │   ├── ancient_egg.png
│   │   └── ... (223 dateien)
│   └── overrides/
│
├── tilesets/                        # World-Tile-Atlanten
│   ├── tuxemon/
│   │   ├── core.png                 # Haupt-Atlas
│   │   ├── interior.png
│   │   ├── caves.png
│   │   └── manifest.json            # { tile_px, columns, rows, terrain_rules }
│   └── overrides/
│
├── maps/                            # 235 Tiled-Karten
│   ├── tuxemon/
│   │   ├── route_1.tmx              # Tiled XML
│   │   ├── route_1.tsx              # Tileset-Reference
│   │   └── ... (235 maps)
│   └── overrides/                   # eigene Karten (höchste Prio)
│
├── characters/                      # Player + NPCs
│   ├── tuxemon/
│   │   ├── player_male/
│   │   │   ├── walk_north_0.png … walk_north_3.png
│   │   │   ├── walk_south_0.png …
│   │   │   ├── walk_east_0.png …
│   │   │   ├── walk_west_0.png …
│   │   │   └── meta.json            # { frame_duration_ms }
│   │   ├── npc_doctor/
│   │   └── ...
│   └── overrides/
│
├── ui/                              # HUD-Grafiken
│   ├── tuxemon/
│   │   ├── hp_bar_frame.png
│   │   ├── dialog_box.png
│   │   ├── menu_cursor.png
│   │   └── ...
│   └── overrides/
│
├── particles/                       # bestehende 144 (bleibt)
└── data/                            # importierte JSON-Datenbanken
    ├── monsters.json                # 411 Monster-Stats
    ├── techniques.json              # 274 Move-Daten
    ├── items.json                   # 223 Item-Effekte
    ├── elements.json                # 13 Type-Chart
    └── statuses.json                # 35 Status-Effekte
```

## Asset-Resolver in Kotlin

Eine zentrale Klasse `AssetResolver` mit override-zuerst-fallback-zuletzt Lookup:

```kotlin
fun assetPath(category: String, id: String, file: String): String {
    val override = "game/$category/overrides/$id/$file"
    val tuxemon = "game/$category/tuxemon/$id/$file"
    return if (assetExists(override)) override else tuxemon
}

// Usage:
PngEchoform("agnidon")           → game/echoforms/overrides/agnidon/front.png OR
                                   game/echoforms/tuxemon/agnidon/front.png
```

## Manifest-Tracking

Pro Kategorie eine `manifest.json` die festhält welche Assets poliert sind:

```json
{
  "echoforms": {
    "agnidon": { "source": "tuxemon", "overridden": [], "license": "CC-BY-SA-3.0" },
    "rockitten": { "source": "tuxemon", "overridden": ["front", "back"], "license": "CC-BY-SA-3.0" }
  }
}
```

Damit kann ich automatisch:
- Credits-Screen generieren (welche Künstler beigesteuert)
- „Polish-Backlog" anzeigen (was ist noch original-Tuxemon?)
- Asset-Pack-DLC schnüren (alle overrides zusammen = Premium-Pack)

## Naming-Konvention

- **Slugs aus Tuxemon übernehmen** — `agnidon`, `rockitten`, `fire_ball`, `potion` (lower_snake_case)
- Aetherbound-Code nutzt diese Slugs als Primärschlüssel
- Original-Tuxemon-Sprites behalten ihre Datei-Namen → 1:1 Mapping

## Lizenz-Hygiene

- **`tuxemon/` Ordner**: CC-BY-SA-3.0 Pflicht, Attribution in Credits-Screen
- **`overrides/` Ordner**: deine Lizenz (eigene Werke)
- Mischung pro Asset wird im Manifest erfasst

## Asset-Größen-Einschätzung

- echoforms tuxemon: ~80 MB (411 × 4 × ~50 KB)
- techniques tuxemon: ~120 MB (274 × 20 frames × ~22 KB)
- tilesets: ~30 MB
- maps + UI + chars: ~30 MB
- **Total Tuxemon-Originals**: **~260 MB**
- Mit DLC-Splitting (Core 80 MB + DLC-Packs) shippbar
