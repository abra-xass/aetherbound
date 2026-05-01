# Aetherbound — 19-Element-System + 1000-Echoform-Matrix

**Status:** Proposal — bitte abnicken, dann baue ich die 1000-Zeilen-Matrix.

---

## 1. Seltenheits-Verteilung (auf 1000 skaliert)

| Tier | Anteil | Anzahl | Catch-Rate | Stat-Budget | Notiz |
|---|---:|---:|---:|---:|---|
| **Common** | 60 % | 600 | 190 | 420 | überall fangbar, einfach |
| **Uncommon** | 25 % | 250 | 90 | 480 | Routen-Encounter |
| **Rare** | 10 % | 100 | 45 | 540 | wettlebedingt, Time-Lock |
| **VeryRare** | 4 % | 40 | 15 | 580 | Wochentag + Tageszeit-Lock |
| **Legendary** | 1 % | 10 | 3 | 640 | Roaming, 5 % Encounter-Chance |
| **Σ** | 100 % | **1000** | — | — | — |

→ Vorher waren 3 Legendaries; jetzt **10** für reichere Endgame-Sammler-Belohnung.

---

## 2. Die 19 Elemente (von 13 erweitert)

### Behaltene 13 (mit Umbenennungen)

| # | Name (final) | War vorher | Bedeutung |
|---:|---|---|---|
| 1 | **Fire** | Fire | Feuer, Hitze, Verbrennung |
| 2 | **Water** | Water | Wasser, Ozean, Strömung |
| 3 | **Ice** | Frost | Eis, Kälte, Kristallstruktur |
| 4 | **Earth** | Earth | Erde, Stein, Berg |
| 5 | **Wood** | Wood | Pflanze, Wachstum, Wurzeln |
| 6 | **Metal** | Metal | Erz, Stahl, Industrie |
| 7 | **Storm** | Lightning | Blitz + atmosphärische Energie |
| 8 | **Wind** | Sky | Luft, Strömung, Vögel |
| 9 | **Shadow** | Shadow | Dunkelheit, Heimlichkeit |
| 10 | **Light** | Heroic | Strahlung, Reinheit (gameplay-stärker als "heroisch") |
| 11 | **Venom** | Venom | Gift, Säure, Korruption |
| 12 | **Cosmic** | Cosmic | Sterne, Schwerkraft, Universum |
| 13 | **Aether** | Normal | reine Lebensessenz (Aetherbound-Markenkern) |

### 6 NEUE Elemente

| # | Name | Bedeutung | Beispiel-Look |
|---:|---|---|---|
| 14 | **Dream** | Illusion, Schlaf, Phantasma | weiche Konturen, pastel, Schmetterlings-Anhängsel |
| 15 | **Mind** | Psychisch, Telepathie | offene Schädelkronen, Augen mit fraktalen Iris |
| 16 | **Sound** | Sonik, Vibration | Trommelfellscheiben, Resonanz-Hörner |
| 17 | **Time** | Chronos, Alterung | Sanduhr-Motive, Sand-Strömung am Körper |
| 18 | **Crystal** | Prismatisch, Gemmen | facettierte Glieder, regenbogenfarbene Reflexe |
| 19 | **Blood** | Vitalität, Opfer | rote Aderzeichnung, Herz-Symbolik |

---

## 3. Verteilung der Echoforms auf 19 Elemente × 5 Rarities

```
1000 / 19 ≈ 53 pro Element  (mit Tier-Splittung wie folgt)

Pro Element:
  Common      32  (~60 %)
  Uncommon    13  (~25 %)
  Rare         5  (~10 %)
  VeryRare     2  (~ 4 %)
  Legendary    0.5 → 10 Legendaries verteilt auf ausgewählte Elemente
                   (Fire, Water, Aether, Cosmic, Time, Dream,
                    Light, Shadow, Crystal, Blood)
```

---

## 4. Stärke-Schwäche-Kreis (19×19 Type-Chart)

Vollständige Matrix als JSON in `docs/element-chart.json`.

### Übersichtsstruktur

Jedes Element hat:
- **3 starke Treffer** (×2 Schaden) → was es schlägt
- **3 schwache Treffer** (×0,5 Schaden) → wogegen es kämpft
- **0–1 Immunität** (×0 Schaden) → seltene Spezial-Konter

### Beispiel-Zeile: **Fire**
| | Stärken (×2) | Schwächen (×0.5) | Immun |
|---|---|---|---|
| Fire | Wood · Ice · Metal | Water · Earth · Storm | — |

### Beispiel-Zeile: **Aether** (Aetherbound-Markenkern)
| | Stärken (×2) | Schwächen (×0.5) | Immun |
|---|---|---|---|
| Aether | Shadow · Venom · Decay-tendenzen | Cosmic · Time | Mind |

→ Vollchart wird in nächstem Commit angelegt sobald du das 19-Set absegnest.

---

## 5. 1000-Echoform-Matrix — Zeilenformat

Jede der 1000 Zeilen wird:

```json
{
  "ordinal": 0,
  "name": "Lobstepi",
  "primary": "Water",
  "secondary": "Crystal",
  "rarity": "Common",
  "biome": ["Beach", "Harbor"],
  "spawn_time": "any",
  "spawn_weather": ["Rain"],
  "description_de": "Eisbergartiger Krebs mit kristallinen Scherenklingen,
    hellblau leuchtend. Setzt auf Härte und Reflektion. Zwei Augenstiele
    aus Eis, kleine Kiemen am Hals.",
  "ai_prompt_seed": "icy crab creature, crystalline pincers, glowing
    light-blue, two ice-stalk eyes, small gills, pokemon-style monster
    art, clean white background"
}
```

---

## 6. Bild-Generator-Skript (kommt nach Approval)

Ich schreibe `tools/generate_echoform_art.py`, das:

1. liest die Matrix-JSON
2. baut pro Echoform 3 separate Prompts:
   - **Front view** — straight-on, full body, Pokemon-style
   - **Three-quarter back view** — leicht von hinten, Tuxemon-Standard
   - **Profile (Side)** — pure Seitenansicht
3. ruft eine AI-Image-API (du wählst Modell)
4. speichert als `Monsters/<name>/front.png`, `back.png`, `profile.png`

---

## Frage an dich

1. **Bist du mit den 19 Elementen einverstanden?** (insbesondere die Umbenennungen Frost→Ice, Lightning→Storm, Sky→Wind, Heroic→Light, Normal→Aether)
2. **Sind die 6 neuen Elemente gut?** (Dream, Mind, Sound, Time, Crystal, Blood)
3. **OK mit 10 Legendaries auf 1000?**
4. **Soll ich gleich die volle Type-Chart bauen** und committen, dann die 1000-Echoform-Matrix?

Sag mir was du anpassen willst, dann baue ich's in einem Schritt durch.
