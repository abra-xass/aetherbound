# Visual-Audit — Phase 1 (19 Bilder gescannt)

## Was ich gesehen habe

### Cluster `Allig*` (12 Monster, 6 gescannt)
| Folder | Was wirklich abgebildet ist |
|---|---|
| Alligaros | blau-violetter Frost-Drache, vierbeinig |
| Alligasaur | glitzernder Sternen-Drache mit Mond-Gem |
| Alligatar | flammender Triceratops mit Pflanzen-Akzent |
| Alligette | insektoides geflügeltes Wesen (Bug) |
| Alligoth | violetter Geist-Drache mit Flügeln |
| Alligotto | violetter dornenbedeckter Geist mit Edelstein |

**Verdikt:** 4 von 6 sind drachenartig → mittleres Cluster-Risiko, aber Variation OK.

### Cluster `Aqua*` (9 Monster, 3 gescannt)
| Folder | Was wirklich abgebildet ist |
|---|---|
| Aquataros | erdiger Triceratops mit Lehm-Akzenten — NICHT Water! |
| Aquatopus | blau-cyanes Wasser-Drache mit Tentakel |
| Aquatile | weiß-goldener Krystall-Schmetterling — NICHT Water! |

**Verdikt:** Element-Mismatch häufig. 2 von 3 passen NICHT zur Water-Klassifikation.

### Cluster `Arma*` (7 Monster, 4 gescannt)
| Folder | Was wirklich abgebildet ist |
|---|---|
| Armadion | dornenbedeckter violet Drache (Shadow) |
| Armadaza | cyaner Krabben-/Krustazeen-Drache |
| Armanium | violet-blauer kleiner geflügelter Drache |
| Armaslash | goldener Drachen-Krieger (Light) |

**Verdikt:** Alle drachenartig. Cluster-Risiko REAL.

### Cluster `Drago*` (3 Monster, alle gescannt)
| Folder | Was wirklich abgebildet ist |
|---|---|
| Dragodrill | Krystall-Igel — KEIN Drache |
| Dragonightmare | Frosch mit Hut — KEIN Drache |
| Dragoron | schwarz-roter Drache mit Stacheln (Drache, ja) |

**Verdikt:** AI hat den Namen großteils ignoriert — gut! Nur 1 echter Drache.

### Cluster `Eleph*` (3 Monster, alle gescannt) ⚠️
| Folder | Was wirklich abgebildet ist |
|---|---|
| Elephia | stahl-grauer Mecha-Elefant |
| Elephoal | erdbeer-roter Elefant mit Edelstein |
| Elephuzz | brauner Mammut mit Flecken |

**Verdikt:** **ALLE 3 sind Elefanten.** Klassischer AI-Names-Bias. Visuell zu ähnlich.

## Schlüssel-Erkenntnisse

1. **AI-Names-Bias EXISTIERT** aber selektiv (Eleph* total, Drago* gar nicht)
2. **Element-Mismatch häufig** — Aquataros wirkt erdig, Alligatar feurig — Original-Klassifikation aus dem Matrix passt nicht mehr zum Bild
3. **Echte Duplikate gering** — Eleph* ist der einzige stark homogene Cluster der Phase-1-Stichprobe
4. **Visual-Diversität insgesamt gut** trotz des Names-Drucks

## Empfehlung

**Strategischer Schwenk** wie vom User vorgeschlagen:

1. **Stop bei 533 + 79 = 612** (Rare + VeryRare + Legendary komplett, Common/Uncommon einfrieren bei aktuellem Stand)
2. **Re-Classification Pass** für die 533 fertigen Bilder:
   - Phase 2-N: ich scanne alle 533 in Batches à 20-30
   - Pro Bild: derive `actual_archetype` + `actual_element` + `actual_complexity` aus dem Bild
   - Output: `assets/content/visual_taxonomy.json` (1 Eintrag pro fertiges Bild)
3. **Re-Naming**: bei den 533 prüfen ob Name zum Bild passt. Falls nicht, neuen Namen vorschlagen
4. **Re-Chaining**: anhand visueller Familienähnlichkeit Chains neu definieren (statt namens-basiert wie aktuell)
