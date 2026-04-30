# Aetherbound — Release-Prozess

**Komplett automatisiert.** Du pushst einen Git-Tag, GitHub Actions
baut + signiert + uploadet alles. Kein manueller Schritt mehr.

## One-time Setup (genau einmal nötig)

### 1. Release-Keystore erzeugen

```bash
mkdir -p keys
keytool -genkey -v \
  -keystore keys/aetherbound-release.keystore \
  -alias aetherbound \
  -keyalg RSA -keysize 4096 -validity 10000
```

Beantworte die Fragen (CN/OU/Land egal, kann frei wählen). Notiere dir
**Keystore-Passwort** und **Key-Passwort** — getrennt aufbewahren!

⚠️ **CRITICAL:** Diesen Keystore mehrfach verschlüsselt sichern (Cold-
Storage USB, age-encrypted Backup, Password-Manager-Vault). **Verlust =
du kannst nie wieder Updates ausliefern, die existierende Installs
überschreiben.**

### 2. Lokales Build aktivieren (optional)

```bash
cp keystore.properties.template keystore.properties
# Edit keystore.properties mit deinen Pfaden + Passwörtern
./gradlew :app:assembleRelease
```

`keystore.properties` ist gitignored.

### 3. GitHub Secrets eintragen

Auf https://github.com/abra-xass/Aetherbound → Settings → Secrets and
Variables → Actions → "New repository secret":

| Secret-Name | Wert |
|---|---|
| `AETHER_KEYSTORE_BASE64` | Base64 vom Keystore-File |
| `AETHER_KEYSTORE_PASS` | Keystore-Passwort |
| `AETHER_KEY_ALIAS` | `aetherbound` (oder dein Alias) |
| `AETHER_KEY_PASS` | Key-Passwort |

Keystore base64-encoden:

```bash
# Linux / Mac:
base64 -w0 keys/aetherbound-release.keystore > kb64.txt

# Windows PowerShell:
[Convert]::ToBase64String([IO.File]::ReadAllBytes("keys/aetherbound-release.keystore")) > kb64.txt
```

Inhalt von `kb64.txt` als `AETHER_KEYSTORE_BASE64`-Secret einfügen.

## Release ausliefern (jedes Mal)

Genau **3 Befehle**:

```bash
git tag v0.2.0
git push origin v0.2.0
# Done. GitHub Actions macht den Rest.
```

Was automatisch passiert:

1. ✅ GitHub Actions checkt repo aus
2. ✅ Setzt Java 17 + Android SDK + Python auf
3. ✅ Lädt Keystore aus Base64-Secret
4. ✅ Berechnet `versionCode` aus Tag (`v0.2.0` → `200`)
5. ✅ Baut Asset-Packs (`audio.zip`, `graphics.zip`, `maps.zip` + `manifest.json`)
6. ✅ Strippt Pack-Files aus APK-Tree → Core-APK ~80 MB
7. ✅ Baut signierten Release-APK
8. ✅ Erstellt GitHub Release `v0.2.0`
9. ✅ Uploadet APK + 3 Pack-ZIPs + manifest.json als Release-Assets
10. ✅ Generiert Release-Notes aus Git-Log (commits seit letztem Tag)
11. ✅ Tagged Pre-Release wenn Tag ein `-` enthält (`v0.2.0-beta1`)

## Dauer

Erster Release: ~10-15 min CI-Zeit. Folgende ~6-8 min (Gradle-Cache).

## Was Spieler sehen

Sekunden nach dem Release-Build:

- Spieler öffnen Aetherbound → MENU → "Check for Updates"
- App pollt `api.github.com/.../releases/latest` → `0.1.0 < 0.2.0`
- Update-Dialog zeigt Release-Notes + DOWNLOAD-Button
- Tap → APK lädt direkt von GitHub Release
- Tap INSTALL → Android-Systeminstaller-Prompt → fertig

Asset-Packs sind separat verfügbar im selben Release: Spieler tippt
"Asset Packs" → wählt Audio/Graphics/Maps → lädt nach Belieben.

## Hotfix-Release

Wenn ein Bug-Fix raus muss:

```bash
git commit -m "fix: capture-math integer overflow"
git tag v0.2.1
git push origin v0.2.1
```

Patch-Tags (`v0.2.1`) verhalten sich genau wie Minor-Releases.

## Pre-Release / Beta

Tags mit `-` werden als GitHub-Pre-Release markiert (zeigen "Pre-release"
Badge, werden vom default `releases/latest`-Endpoint NICHT zurückgegeben):

```bash
git tag v0.3.0-beta1
git push origin v0.3.0-beta1
```

Beta-User können in der UpdateChecker-Konfiguration `includePrerelease=true`
setzen, dann pollen sie das Listing-Endpoint statt /latest.

## Versionscode-Schema

Tag → `versionCode`:

- `v0.1.0` → 100
- `v0.2.0` → 200
- `v0.2.5` → 205
- `v1.0.0` → 10000
- `v1.2.3` → 10203

Formel: `major * 10000 + minor * 100 + patch`. Ein versionCode darf
**nie** kleiner sein als der vorherige, sonst lehnt Android das Update ab.
Patches inkrementieren immer den letzten Teil.

## Asset-Pack-Updates ohne APK-Update

Falls du nur Audio/Maps-Inhalt ändern willst ohne neue APK-Logik:

1. Lokal `py -3 tools/build/build_asset_packs.py` laufen lassen
2. Manuell auf den existierenden GitHub-Release uploaden (API oder Web-UI)
3. Spieler bekommen automatisch beim nächsten "Asset Packs"-Tap die neue
   Version (manifest.json wird neu gefetcht)

Oder: kleinen Patch-Tag drauf-pushen — die Workflow läuft alles neu durch.

## Was du als Maintainer NIE machen sollst

- ❌ Keystore-File commiten (gitignored, aber doppelt prüfen)
- ❌ `keystore.properties` commiten (gitignored)
- ❌ Tags löschen die schon publiziert sind
- ❌ Force-Push auf publizierte Tags
- ❌ Release-Assets manuell editieren nach Build (würde Hash-Verifikation brechen für Asset-Packs)
- ❌ Mit anderem Keystore signieren (würde Updates für existierende User unmöglich machen)
