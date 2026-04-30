# Aetherbound — Multiplayer-Arena-Design

## Kernregeln

| Regel | Wert |
|---|---|
| Arena | "Aether-Arena" (eigene Scene, nicht TMX-Map) |
| Party-Snapshot | Deep-copy beim Match-Start, Restore nach Match-Ende |
| XP-Gain | ❌ keiner |
| Money | ❌ keines |
| Bestiary-Track | ❌ kein "seen"-Mark |
| Capture-Button | ❌ ausgeblendet |
| Run-Away | ❌ ausgeblendet (stattdessen "Concede" via Menü) |
| Items in Battle | ❌ MVP-default; spätere Toggleable per Match-Settings |
| Overworld-Avatar | eingefroren am Pre-Match-Tile |
| Status-Effekte | persistieren *innerhalb* des Matches, weg nach Restore |
| Disconnect-Toleranz | 5 min Reconnect-Window vor Forfeit |
| Achievements | "first_mp_win", "first_mp_loss", "first_perfect_sweep" |
| Trainer-Card-Stats | Win/Loss-Counter + Letzte-10-Match-Liste |

## Match-Flow

```
1. Alice in Thot-DM mit Bob
2. Beide haben Aetherbound (Capability-State == Challenge)
3. Alice tippt ⚔️ → AetherChallengeSheet → "Challenge to Battle"
4. AetherBridge.challengeToBattle()
     ├─ sendet io.aether.battle.invite via Matrix
     └─ AetherbroundLauncher startet Aetherbound bei Alice
5. Bob's Thot bekommt invite-Event, zeigt Notification + ⚔️-Banner im Chat
6. Bob tippt → Aetherbound startet bei Bob
7. Beide Aetherbound-Instanzen sind in Scene.MultiplayerLobby
   - Zeigen Peer-Trainer-Card (Name, Bestiary %, Badges)
   - Beide tippen "Ready"
8. Beide Clients berechnen rngSeed = sha256(host.id + guest.id + invite.event_id)
9. Pre-Match Snapshot:
     snapshot = MultiplayerSnapshot(
         partyDeepCopy = party,
         worldMapPath = currentMapPath,
         worldTileX = ..., worldTileY = ...,
     )
10. Scene.MultiplayerArena
     - Aether-Arena Hintergrund (cosmic void + gold ring)
     - Beide Echoforms-Sprites in der Arena
     - HP-Bars für beide
     - Thot Connection-Indicator (TOR · 3 hops · 850ms)
11. Turn-Loop:
     - Beide picken Move
     - Beide senden BATTLE_MOVE über Matrix mit state-hash
     - Beide Clients runen BattleResolver mit selbem rngSeed
     - State-Hash-Compare → match continue or desync abort
12. Match-Ende:
     - winner = side mit allen 6 mons standing
     - oder: forfeit (concede / disconnect-timeout)
13. Restore:
     party = snapshot.partyDeepCopy
     currentMapPath = snapshot.worldMapPath
     spawnTileX = snapshot.worldTileX, spawnTileY = snapshot.worldTileY
     progress.multiplayerWins/Losses++
     scene = Scene.TuxemonWorld
     → Spieler wacht am exakt selben Tile auf
```

## Arena-Visualisierung

Wiederverwendet Aetherbounds existierende Battle-Render-Stack
(`BattleScene`, `BattleArenaBackdrop`, particle system) mit ausgetauschtem
Hintergrund:

- **Backdrop:** dunkler Sternenfeld-Gradient (Obsidian → Cosmic-Purple),
  langsam rotierende Sternen-Particles (10-20 Stück, niedrige Last)
- **Bodenring:** hexagonaler Gold-Aether-Ring, 4-stop-Gold-Gradient,
  pulsiert leise (1 Hz, ±10% Alpha)
- **Match-Banner:** oben — beide Trainer-Namen, Bestiary-Completion-%,
  Badge-Count, Connection-Indicator rechts
- **Sprites:** Echoforms größer dargestellt (1.2× normaler Battle-Size)
  weil's ein Showcase-Match ist
- **Move-Animationen:** identisch zu SP, nutzen die existierenden
  `AnimationRecipe` + `TuxemonMoveFx`-Player

## Match-Settings (Future-Toggles)

Diese werden im Lobby-Screen verhandelt und dann mit `BATTLE_START` Event
beidseitig festgelegt:

| Setting | Default | Future-Toggleable |
|---|---|---|
| Items erlaubt | aus | ja |
| Level-Cap | 50 | ja (10 / 30 / 50 / unlimited) |
| Status-Conditions | ein | nein (ist core-mechanic) |
| Same-Mon-Ban | ein | nein |
| Sleep-Clause | aus | ja |
| Switch-Speed | normal | nein |
| Time-per-Move | 45s (Tor-default) | ja (10s / 30s / 60s) |

## State-Hash-Validierung (Anti-Cheat)

Pro Move sendet jede Seite einen `stateHash` mit:

```
stateHash = sha256(
    "$turn|" +
    "${player.currentVigor}/${player.maxVigor}|" +
    "${opponent.currentVigor}/${opponent.maxVigor}|" +
    "${playerStatuses.joinSlugs}|" +
    "${opponentStatuses.joinSlugs}"
)
```

Empfänger berechnet seinen eigenen und vergleicht. Wenn unterschiedlich:
- 1× Toleranz (race-condition-buffer): nochmal vergleichen nach 200ms
- 2× ungleich → Match-Abort mit `BATTLE_END {winner: "draw", reason: "desync"}`

Pure deterministischer `BattleResolver` macht's möglich: gleiche Inputs +
gleicher Seed → bit-identischer State auf jedem Device.

## Connection-Verlust

```
Tor-Circuit-Drop während Match
   ↓
Lokaler Client zeigt 🔴 RECONNECTING
   ↓ wartet 5 min
   ├─ Recovered → Match continues seamlessly
   └─ Timeout → BATTLE_END forfeit, der disconnecting-side bekommt Loss
```

Save bleibt während des Matches im Pre-Match-Zustand (kein Disk-I/O), also
kein Risiko für korrupte Saves.

## Achievements

In `PlayerProgress` neue Felder:

```kotlin
val multiplayerWins: Int
val multiplayerLosses: Int
val recentMatches: List<MatchRecord>   // last 10
```

`MatchRecord(opponentName, won, finalTurn, timestamp)` für die Trainer-Card.

Erste 3 Achievements:
- `mp_first_win` — erster MP-Sieg
- `mp_first_loss` — erste MP-Niederlage (nicht-shamefully framed)
- `mp_perfect_sweep` — Match gewonnen ohne dass eigene Echoform fainted

## Spectator-Mode

Dritte Person im Thot-Room (nicht Alice/Bob) kann tap "Spectate":
- Aetherbound öffnet sich, Scene.MultiplayerArena im read-only-Modus
- Hört auf alle io.aether.battle.* Events
- Replay des Matches in Echtzeit (deterministisch via shared seed)
- Kein eigener Move-Selektor, keine Pause-Funktion
- Verlässt durch Tippen "Stop spectating" → zurück zu Aetherbound-Title

## Implementations-Status

| Komponente | Stand |
|---|---|
| MultiplayerBridge interface | ✅ existiert |
| MatrixWireFormat (13 events) | ✅ existiert |
| BattleResolver determinismus | ✅ existiert |
| MultiplayerStatusIndicator | ✅ existiert |
| BattleTimeouts.Tor | ✅ existiert |
| **MultiplayerSnapshot** | TBD — siehe nächster Schritt |
| **MultiplayerLobbyScene** | TBD |
| **MultiplayerArenaScene** | TBD (~80% reuse von BattleScene) |
| **AetherArenaBackdrop** | TBD (cosmic void + gold ring) |
| **MatchSettings** lobby UI | TBD |
| **State-hash-validation** in Resolver | TBD (existiert teilweise) |
| **Spectator-Mode** | TBD |

Nächster konkreter Schritt: `MultiplayerSnapshot.kt` als Datenstruktur, dann
`MultiplayerArenaScene` als Skelett (kann 80% von BattleScene erben).
