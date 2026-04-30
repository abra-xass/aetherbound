# Aetherbound — Pragmatische Distribution (250 MB Realität)

## Problem

- APK ~250 MB → für Tor unbrauchbar (Stunden Download)
- Matrix-File-Hosting (besonders nope.chat) hält 250 MB nicht
- Catbox-Limit 200 MB
- Brauchen persistenten Hoster + schmale APK

## Sofort-Lösung: APK schrumpfen

### Größenanalyse

| Asset | Größe | Reduktion möglich |
|---|---|---|
| Audio (151 MB) | 192 SFX + 111 BGM | **→ 15-25 MB** (10 BGM + 50 SFX reichen) |
| Echoform-Sprites | ~20 MB | bleibt — Kernspielinhalt |
| Move-FX-Frames | ~30 MB | **→ 5 MB** (nur Pilot-Moves) |
| Tilesets + Maps | ~30 MB | bleibt |
| UI + Items + Chars | ~15 MB | bleibt |
| Code + Libs | ~10 MB | bleibt |
| **Total** | **~250 MB** | **→ ~85 MB Core-APK** |

### Asset-Pack-Strategie

```
Aetherbound-Core.apk        85 MB    Play Store / direkt
   ↓ optional download
Aetherbound-Audio.zip      130 MB    nur bei Bedarf, on-demand
Aetherbound-FullFx.zip      25 MB    optional, polished animations
Aetherbound-AllMaps.zip     20 MB    optional, alle 235 Maps
                          ─────────
                           ~260 MB total wenn alles drauf
```

User installiert 85 MB Core, lädt Audio später wenn er WLAN hat. Tor wird
nicht für 250 MB belastet, sondern für 85 MB einmalig.

## Filehoster-Optionen (persistent, kein Auto-Delete)

| Hoster | Limit | Tor-tauglich | Kosten | Bewertung |
|---|---|---|---|---|
| **GitHub Releases** | 2 GB/file | ⚠️ via Tor-Browser | gratis | ⭐ Empfohlen |
| **Backblaze B2** | unbegrenzt | ✅ HTTPS | $0.005/GB/Mon | gut für Skalierung |
| **MEGA** | 20 GB free | ✅ via Web | gratis | E2E-encrypted, gut |
| **Codeberg Releases** | 2 GB/file | ✅ FOSS-Server | gratis | FOSS-konform |
| **Self-hosted nginx + .onion** | unbegrenzt | ✅ nativ | ~5€/Monat | volle Kontrolle |
| **IPFS (mit Pinning)** | unbegrenzt | ✅ via Gateway | Pinning ~$1-5/Mon | dezentral, hip |
| Google Drive | 15 GB | ❌ braucht Google Account | gratis | Privacy-Leak |

**Empfehlung:** GitHub Releases als primärer Hoster. 2 GB/file reicht für
Core+Asset-Packs. Persistent. Kostenlos. Hat eine API für `latest.json`.
User muss nicht GitHub-Account haben um zu downloaden.

Backup: Codeberg-Mirror für FOSS-Konformität, plus eigener .onion für
Privacy-Bewusste.

## Play Store — Konkreter Aufwand

### Setup-Kosten

- **Google Play Developer Account**: $25 einmalig, lebenslang
- **Privacy-Policy-URL**: muss öffentlich erreichbar sein (z.B. eigene Webseite oder GitHub Pages, kostenlos)
- **App-Signing-Key**: einmalig erzeugen, sicher backupen

### Erste Publishing-Phase

| Schritt | Aufwand |
|---|---|
| Developer-Account anmelden + verifizieren | 30 min |
| App-Listing erstellen (Titel, Beschreibung, Screenshots, Icons) | 4-8h |
| Privacy Policy schreiben + hosten | 2-3h |
| Content Rating IARC-Fragebogen | 30 min |
| Data Safety Section ausfüllen | 1h |
| AAB statt APK bauen + signieren | 2h |
| Test-Track (Internal Testing) → Closed → Open → Production | mehrere Tage |
| **Gesamt vor Submission** | **1-2 Arbeitstage** |
| Google Review (Erst-Einreichung) | **3-21 Tage warten** (post-2024 strenger) |
| Updates nach erstem Release | meist <24h |

### Aetherbound-spezifische Hürden

**Tuxemon-Assets (CC-BY-SA-3.0):**
- Attribution im App-Listing + im Spiel zwingend
- Derivat muss auch CC-BY-SA-3.0 lizenziert werden (für Asset-Layer)
- Source-Code-Pflicht für GPL-3.0-derivierte Engine-Teile
- Play Store akzeptiert das, aber Listing muss klar formuliert sein

**Pokémon-Ähnlichkeit:**
- Niemals "Pokémon" / "Catch them all" / "Gotta catch" im Listing
- Beschreibung: "Monster-RPG basierend auf Tuxemon (CC-BY-SA-3.0)"
- Tuxemon selbst ist legal Pokémon-frei → wir auch

**APK-Größe:**
- Play Store: 200 MB max APK, aber AAB unterstützt bis 4 GB via Asset Packs
- → AAB-Migration ist Voraussetzung (sowieso seit 2021 Pflicht)

**Account-Verifikation:**
- Seit 2024 strenger: Personal Account braucht 12 verschiedene Test-User
- Organization Account braucht D-U-N-S-Nummer (3 Wochen Wartezeit für DUNS)
- Bypass: Personal Account direkt mit verified ID

### Zeit-Schätzung Komplett-Pfad

```
Tag 0:    $25 zahlen, Account anlegen
Tag 1-2:  Listing + Screenshots + Privacy Policy
Tag 3:    AAB-Build + Internal-Test-Track Upload
Tag 4-7:  Test-Phase mit Freunden (Closed Testing 14 Tage minimum
          für neue Accounts!)
Tag 21:   Production-Submission
Tag 22-42: Google Review (kann variieren 3-21 Tage)
Tag 42:   Public Release
```

**Realistisch: 3-6 Wochen vom Tag 0 bis Public Release.** Updates danach
sind schnell (24-48h).

## Empfohlener Pfad

### Phase 1 — Sofort (1 Tag Arbeit)

1. APK auf 85 MB schrumpfen (Audio-Subset bündeln)
2. Asset-Pack-Downloader in Aetherbound einbauen
3. GitHub Releases anlegen, Core-APK + Asset-ZIPs hochladen
4. In Thot: Capability advertise mit `apk_url: github.com/...`
5. → Bob bekommt Link, lädt 85 MB direkt, Audio später bei Bedarf

### Phase 2 — Beta (1 Woche)

1. Codeberg-Mirror anlegen (FOSS-Sichtbarkeit)
2. Tor Hidden Service mit `latest.json` für Auto-Update-Check
3. Update-Checker in App: pollt `/latest.json` über Tor

### Phase 3 — Mainstream (3-6 Wochen)

1. Google Play Developer Account anlegen ($25)
2. AAB statt APK bauen (Asset-Pack-Splitting durch Play Store automatisch)
3. Listing + Privacy Policy + Closed Testing
4. Production Release nach Review
5. F-Droid eigener Repo parallel für FOSS-User

## Asset-Pack-Implementation in Aetherbound

```kotlin
// AssetPackDownloader.kt (zu bauen)
object AssetPackDownloader {
    enum class Pack(val id: String, val sizeMb: Int, val url: String) {
        AUDIO("audio", 130, "https://github.com/.../v0.1/audio.zip"),
        FULL_FX("fullfx", 25, "https://github.com/.../v0.1/fullfx.zip"),
        ALL_MAPS("allmaps", 20, "https://github.com/.../v0.1/allmaps.zip"),
    }

    suspend fun downloadAndExtract(ctx: Context, pack: Pack, onProgress: (Float) -> Unit)
    fun isInstalled(ctx: Context, pack: Pack): Boolean
}
```

User-Flow: Beim ersten Battle prompt: "Audio aktivieren? (130 MB
Download)" → bei "Ja" → Tor-Download mit Progressbar → entpackt nach
`getExternalFilesDir()`.

## Fazit

- **APK von 250 MB → 85 MB** durch Audio-Auslagerung in Asset-Pack
- **Persistent gehostete Asset-Packs** auf GitHub Releases (gratis, 2 GB/file)
- **Play Store = $25 + 3-6 Wochen Arbeit** mit Reviews, danach <48h Updates
- **Beste Strategie: alles parallel** — GitHub für tech-savvy, Play Store für Mainstream, .onion für Privacy
