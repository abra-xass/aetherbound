# Aetherbound — Gap-Analyse (Stand: heute)

Vollständige Bestandsaufnahme: was ist fertig, was fehlt für ein
veröffentlichungsfähiges Spiel.

## ✅ Komplett (87 Files, ~12.000 Zeilen Code)

### Engine-Layer (`com.aetherbound.game.core` + `core.data`)
- 13-Element Type-Chart (Aspect.kt)
- 14 Tuxemon Shapes mit Stat-Curves (MonsterShape)
- 12 Tastes mit Temperament-Adapter
- BattleResolver mit voller Status-Engine (poison/burn/sleep/freeze/paralyze/confuse/regen/blinded)
- DamageVariance (crit-tier, STAB ×1.5, 85-100% range)
- StatusEngine + MoveStatusEffects (30 Move→Status Mappings)
- CaptureMath (8 Ball-Multipliers, status-bonus, shake animation)
- ExperienceEngine (4 XP-Curves) + EvolutionEngine (5 Trigger)
- TrainerBattle (3 AI-Tiers, 2 Pilot-Trainers) + TrainerRegistry
- Party (max 6 + auto-failover) + PcStorage (30er Boxen)
- Inventory (stacks + money + grouping)
- ItemEngine (20 Effekt-Kinds, payload-driven)
- EncounterEngine (Step-Counter, Repellent, Cooldown)
- AmbientTime + DayNightCycle (real-clock 6 Phasen)
- BattleTimeouts (Tor/Clearnet/Replay defaults)

### Render-Layer
- TmxLoader / TmxRenderer / TmxScene (komplett pack-aware)
- CollisionMap + MovementController + ObjectInteraction
- NpcEntity + NpcSpawner (static/wander/patrol)
- AudioEngine + AudioCatalog + LocalAudioEngine (pack-aware)
- TuxemonMoveFx (frame-by-frame player, pack-aware)

### Daten-Layer
- TuxemonDex / TechniqueDex / ItemDex / StatusDex (lazy JSON-Loader)
- TuxemonAdapter + TuxemonEchoformDex
- TuxemonBattleSetup (factory builds 2 EchoformInstances)
- SaveGame + SaveGameIO (5 Slots, JSON-Snapshot)
- PlayerProgress (caught/seen/badges/audio/controls)

### UI-Layer (Compose)
- TitleScreen, TuxemonWorldScene, BattleScene
- PartyScreen + EchoformDetailScreen
- BagScreen + ItemTargetPicker + ItemSpriteImage
- BestiaryScreen (411-Grid, caught/seen/unknown)
- SaveLoadMenu (5 Slots), SettingsScreen, PlayerStatusCard
- TrainerBattleHost + TrainerBattleIntro
- CaptureAnimation, MultiplayerStatusIndicator
- AssetPackPrompt + EchoformSpriteImage + CharacterSprite

### Multiplayer-Skeleton
- MultiplayerBridge interface + ConnectionInfo + NetworkTransport
- MatrixWireFormat (13 Event-Types + Encoder/Decoder + hashState/hashTeam)
- NoOpMultiplayerBridge default
- Tor-aware BattleTimeouts

### Distribution
- 9-Phase Tuxemon-Importer (411 mons + 274 moves + 223 items + 235 maps + 192 SFX + 111 BGM)
- AssetPackDownloader (Manifest fetch, sha256 verify, ZIP extract, transparent resolve)
- build_asset_packs.py (Core/Audio/Graphics/Maps split + Stripper)

---

## ❌ Hard Gaps (kritisch für Release)

### Multiplayer (Aetherbound-Seite ~80% fertig, Thot-Seite 0%)
1. **Bridge-Implementation in Thot-Modul** — `ThotMatrixBridge.kt` muss `MultiplayerBridge` implementieren
2. **MultiplayerScreen UI** — Roster-Liste + Invite-Flow + Inbound-Invite-Inbox
3. **Multiplayer-aware BattleScene** — sync moves via Bridge statt local AI
4. **Intent-Handler `aetherbound.battle.invite`** — AndroidManifest deep-link entry, parse room_id/peer_id
5. **Capability-Advertise-Logik** — Aetherbound broadcastet `io.aether.capability.present` einmal pro Install

### Distribution
6. **Release-Signing-Config** — derzeit nur Debug-Keystore; Production-Keystore + Backup-Strategie fehlt
7. **App-Icon + Launcher-Metadata** — derzeit Android-Default
8. **UpdateChecker** — pollt `latest.json` von GitHub Release, prompt bei Version-Mismatch
9. **AssetPackPrompt im Menü erreichbar** — Settings-Screen-Eintrag fehlt
10. **README.md** für GitHub-Release-Page

### Build-Pipeline
11. **GitHub Actions Workflow** — auto-build APK + asset-packs bei Tag-Push
12. **Reproducible Build Config** — für F-Droid-Aufnahme

---

## ⚠️ Soft Gaps (UX-Defizite, kein Showstopper)

### Battle-System
13. **Move-Learning-UI** — wenn XP einen neuen Move triggert, Spieler muss in 4-Slot-Setup auswählen
14. **In-Battle-Switch-UI** — PartyScreen kennt switch, aber BattleScene's "Switch"-Action ist nicht gewired
15. **Battle-Item-Use** — BagScreen kann nicht aus dem Battle aufgerufen werden
16. **Run-Away-Mechanik** — kein Escape-Button im Wild-Battle

### World
17. **Map-Warp-Logik** — `WorldEvent.Warp` wird klassifiziert aber nicht ausgeführt (kein Map-Wechsel)
18. **NPC-Dialog-UI** — derzeit nur abgeschnittener Status-Line-Text, kein vollständiger Dialog-Overlay
19. **Sign-Reading-UI** — gleiches Problem
20. **Healing-Pad** — recognized, aber heilt Party nicht
21. **Item-Drop-Pickup** — Flag-Tracking fehlt

### Inventory / Party
22. **PC-Storage-Screen** — Overflow-Echoforms beyond 6 nicht ansehbar/abrufbar
23. **Echoform-Release** — kein Release-from-PC
24. **Item-Description-Modal** — beim Use-Tap einfach Effekt, keine Erklärung
25. **Bag-Tab-Filter** — alle Items in einer Liste, keine Heal/Balls/Berries-Tabs

### Trainer / Story
26. **Mehr Trainer** — nur 2 Pilot (Hiker + Fisher); Tuxemon-NPCs nicht importiert
27. **Story-Beats** — keine Narrative
28. **Region-Map / Overview** — kein Welt-Übersichtsbild

### Settings
29. **Volume-Slider funktional** — derzeit nur kosmetisch (AudioEngine ignoriert die Werte)
30. **Day/Night-Override aktiv** — Settings-Toggle existiert, aber AmbientTime ignoriert `forcedPhase`

---

## 🧪 Testing-Lücken

31. **Unit-Tests** — nur 1 Test-File (GamePreviewLogicTest); BattleResolver/StatusEngine/CaptureMath haben keine Tests
32. **Integration-Tests** — kein End-to-End-Battle-Run
33. **Snapshot-Tests** — kein UI-Regressionstest
34. **MultiplayerBridge-Tests** — Wire-Format-Round-Trip nicht getestet

---

## 📚 Dokumentations-Lücken

35. **README.md** — Projekt hat interne Docs, aber keine User-Facing-Readme
36. **CONTRIBUTING.md** — für FOSS-Distribution
37. **CHANGELOG.md** — Versions-Historie
38. **API-Doc** — KDoc gibt's, aber kein generierter Dokku-Output
39. **In-Game-Help** — keine Tutorial-Overlays für First-Time-User

---

## Priorisierung — was als nächstes

### Stage A: "Spielbar als Single-Player" (1-2 Tage)
- #13 Move-Learning-UI
- #14 In-Battle-Switch-UI
- #15 Battle-Item-Use
- #18 NPC-Dialog-Overlay
- #29 Volume-Slider funktional
- #30 Day/Night-Override aktiv

### Stage B: "Verteilbar via GitHub" (1 Tag)
- #6 Release-Signing-Config
- #7 App-Icon
- #8 UpdateChecker
- #9 AssetPackPrompt im Menü
- #10 README.md
- #11 GitHub Actions Workflow

### Stage C: "Multiplayer aktiv" (3-5 Tage, Thot-Modul-Arbeit)
- #1 Bridge-Implementation in Thot-Modul
- #2 MultiplayerScreen UI
- #3 Multiplayer-aware BattleScene
- #4 Intent-Handler
- #5 Capability-Advertise

### Stage D: "Storymode + Content" (5-10 Tage)
- #17 Map-Warps
- #22 PC-Storage-Screen
- #25 Bag-Tabs
- #26 Mehr Trainer
- #27 Story-Beats

### Stage E: "Production-Ready" (3-5 Tage)
- #31-34 Tests
- #12 Reproducible Build
- #35-39 Docs

---

## Bewertung in Zahlen

| Kategorie | Fertig | Offen | Kritisch | Total |
|---|---:|---:|---:|---:|
| Engine | 100% | 0 | 0 | ✅ |
| Daten | 100% | 0 | 0 | ✅ |
| Render | 95% | 2 | 0 | ✅ |
| UI | 80% | 13 | 4 | ⚠️ |
| Multiplayer | 50% | 5 | 5 | ⚠️ |
| Distribution | 60% | 7 | 6 | ⚠️ |
| Tests | 5% | 4 | 1 | ❌ |
| Docs | 30% | 5 | 1 | ❌ |

**Gesamt: ~75% fertig.** Was noch fehlt ist 80% UI-Polish + 100%
Multiplayer-Wiring + Build-Pipeline. Die schwere Engine-Arbeit ist
durch — der Rest ist "Wirings auf existierende APIs".
