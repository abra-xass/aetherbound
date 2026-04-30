# Aetherbound — Distribution ohne Play Store

Da Thots Threat-Model Tor-Mandatory ist und User typischerweise Play-
Store-frei betreiben, hier die saubere Distribution-Architektur ohne
Google. Fünf Kanäle, drei davon Tor-nativ.

## Matrix der Kanäle

| Kanal | Tor-nativ | Auto-Update | FOSS-konform | Aufwand |
|---|---|---|---|---|
| **Thot In-App-Installer** | ✅ | ✅ via Capability-Sync | ✅ | mittel |
| **Tor Hidden Service** (.onion) | ✅ | ⚠️ manuell | ✅ | klein |
| **F-Droid eigener Repo** | ⚠️ via Bridge | ✅ | ✅ | mittel |
| **GitHub Releases + Obtainium** | ⚠️ via Tor-Browser | ✅ | ✅ | klein |
| **IzzyOnDroid** | ⚠️ | ✅ | ✅ | klein |

## Kanal 1: Thot In-App-Installer (empfohlen für Thot-Nutzer)

**Aetherbound wird durch Thot selbst verteilt.** Logischste Wahl, weil:
- Thot-User sind die Zielgruppe
- Thot hat bereits eine Tor-Verbindung
- Capability-Advertise-Mechanismus lebt schon in Matrix

### Wie's funktioniert

```
1. Alice hat Aetherbound, Bob nicht
2. Alice's Aetherbound broadcastet capability.present
   {version: "0.1", apk_url: "matrix://...", apk_hash: "<sha256>"}
3. Alice tippt im Thot-Chat ⚔️ → State B (Invite-Mode)
4. Tap → Thot fragt: "APK direkt schicken?"
5. Yes → Thot sendet die APK als Matrix-File-Attachment
   (verschlüsselt, via Tor, end-to-end)
6. Bob bekommt File-Card im DM: "Aetherbound 0.1.apk · 280 MB"
7. Bob tippt → Android-Installer öffnet sich
   (braucht "Install from unknown sources" Permission)
8. Aetherbound installiert, broadcastet capability.present
9. Alice's Button wechselt zu State A (Challenge-Mode)
```

### Update-Mechanismus

Aetherbound's `capability.present` carries a version string. Thot kann beim nächsten
Sync vergleichen — wenn Peer eine neuere Version hat, prompt: "Update verfügbar via DM".

### Vorteile

- **Kein externer Server** — Matrix-File-Hosting reicht
- **Tor-end-to-end** — APK durchquert nie clearnet
- **Keine User-Action** — wer Thot installiert hat, kann sofort empfangen
- **Cryptographic verifiable** — sha256 im capability.present matched APK-Hash

### Nachteile

- Erste Install muss Aetherbound-Distributor manuell anstoßen (capability mit APK seeden)
- Matrix-Server-Storage-Belastung (~280MB pro Distribution-Event, aber nur einmal seedend)

## Kanal 2: Tor Hidden Service (.onion)

Eigener Onion-Service als primäre Download-Quelle:

```
http://aetherboundXXXX.onion/
├── /releases/0.1/aetherbound-0.1-release.apk
├── /releases/0.1/aetherbound-0.1-release.apk.sig
├── /releases/latest.json    {"version": "0.1", "url": "...", "sha256": "..."}
└── /repo/                    F-Droid-kompatibler Repo (siehe Kanal 3)
```

### Setup

- Tor-Daemon auf einem VPS (oder Home-Pi)
- nginx servt statische APKs
- `/etc/tor/torrc`:
  ```
  HiddenServiceDir /var/lib/tor/aetherbound/
  HiddenServicePort 80 127.0.0.1:8080
  ```
- onion-URL fest in Aetherbound's `BuildConfig.UPDATE_URL`
- Aetherbound prüft beim Start `latest.json` (über Thots Tor-Connection
  oder Orbot wenn Thot nicht installiert), prompt für Update

### Vorteile

- Server-IP unbekannt (Tor v3-onion, keine DNS-Leaks)
- Server kennt User-IPs nicht
- Censorship-resistant
- Keine Abhängigkeit von Drittanbietern

### Nachteile

- Hosting-Kosten (kleiner VPS reicht ~5€/Monat)
- User braucht Tor-Browser oder Orbot wenn er ohne Thot draufzugreift

## Kanal 3: F-Droid eigener Repo

F-Droid ist der Standard für FOSS-Android. Du kannst einen eigenen Repo
hosten (kein Verlassen auf das offizielle f-droid.org).

### Setup

```bash
# Auf deinem Server:
fdroidserver init
# konfiguriert config.yml
fdroidserver build
fdroidserver update
# Output: ein /repo/ Verzeichnis mit signed metadata
nginx servt: https://aetherbound.example.org/fdroid/repo
# (oder auf .onion: http://aetherboundXXXX.onion/fdroid/repo)
```

### User-Flow

1. User installiert F-Droid Client (von f-droid.org direkt, signed APK)
2. In F-Droid → "Repos hinzufügen" → URL einfügen
3. F-Droid pullt Manifest + APK
4. Auto-Updates wenn neue Versionen kommen

### Vorteile

- Etabliertes Format, viele User kennen F-Droid
- Auto-Update-Infrastruktur sofort
- Reproducible-Build-Standards
- Kann auf .onion gehostet werden (Tor-nativ)

### Nachteile

- F-Droid-Build-Server-Anforderungen sind streng (must build from source)
- Erste Einrichtung 1-2 Tage Arbeit

## Kanal 4: GitHub Releases + Obtainium

Für User die GitHub mögen aber kein F-Droid:

- Release Tag pushen → APK als Asset hochladen
- User installiert **Obtainium** (selbst auf F-Droid)
- In Obtainium: "App hinzufügen" → GitHub-Repo-URL
- Obtainium pollt Releases-API, bietet Update an

### Vorteile

- Null Setup-Aufwand wenn du eh GitHub nutzt
- Obtainium ist sehr User-freundlich
- Pro Repo: nur APK + signed-asset hochladen

### Nachteile

- GitHub clearnet (kein Tor-nativ, aber Obtainium kann SOCKS-Proxy)
- GitHub kennt Download-Statistiken (Privacy-Leak)

## Kanal 5: IzzyOnDroid

Inoffizieller F-Droid-kompatibler Repo, weniger streng als f-droid.org.
Reicht aus wenn du nur Binaries publizieren willst.

- URL: https://apt.izzysoft.de/fdroid/repo
- Maintainer (Izzy) reviewt manuell, listet dann
- Nutzer fügen den Repo in F-Droid hinzu, gleicher Flow wie Kanal 3

### Vorteile

- Etablierte Reichweite (viele FOSS-Apps)
- Weniger Build-Anforderungen

### Nachteile

- Drittanbieter-Vertrauen (Izzy als Gatekeeper)
- Keine Reproducible-Build-Garantie

## Empfehlung für deinen Stack

**Stage 1 (jetzt):**
1. **Kanal 1 (Thot In-App)** — primär für Thot-User
2. **Kanal 2 (.onion)** — Tor-Backup für Zugriff ohne Thot

**Stage 2 (nach 0.1-Release):**
3. **Kanal 3 (F-Droid eigener Repo)** für FOSS-Sichtbarkeit
4. **Kanal 4 (GitHub + Obtainium)** für tech-savvy User

**Stage 3 (Optional):**
5. **Kanal 5 (IzzyOnDroid)** für extra Reichweite

## Signing-Setup (gilt für alle Kanäle)

```bash
keytool -genkey -v -keystore aetherbound-release.keystore \
  -alias aetherbound -keyalg RSA -keysize 4096 -validity 10000
```

```kotlin
// app/build.gradle.kts
signingConfigs {
    create("release") {
        storeFile = file("../keys/aetherbound-release.keystore")
        storePassword = System.getenv("AETHER_KEYSTORE_PASS")
        keyAlias = "aetherbound"
        keyPassword = System.getenv("AETHER_KEY_PASS")
    }
}
```

**Kritisch:** Selber Keystore für **alle** Kanäle. Wenn du mit verschiedenen
Keys signierst, kann ein User von Kanal A nicht zu Kanal B updaten ohne
Uninstall+Reinstall (Daten weg).

Backup den Keystore an mehrere Orte (verschlüsselt, z.B. age + cold-storage).
Verlust = du kannst nie wieder updaten.

## Build-Output

`./gradlew :app:assembleRelease` produziert
`app/build/outputs/apk/release/app-release.apk` — das ist die einzige
Datei, die in alle 5 Kanäle gleich gepushed wird.

## Auto-Update-Architektur

```
Aetherbound App (running)
    │
    ├─ Capability-Sync (über Thot Matrix) → Thot In-App-Installer
    │     "Peer Alice meldet 0.2, du hast 0.1 → Update?"
    │
    ├─ HTTPS-Poll (über Tor-SOCKS) → /latest.json on .onion
    │     "{version: 0.2, url: ..., sha256: ...}" → Update-Prompt
    │
    └─ F-Droid Client (separater App) → automatisch im Hintergrund
```

Aetherbound braucht selbst nur **einen** Update-Check, der zur
verfügbaren Quelle dispatcht.
