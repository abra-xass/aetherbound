# Aetherbound ↔ Thot Multiplayer-Bindung

## Kernidee: **Thot ist die soziale Schicht, Aetherbound ist eine Linse darauf**

Aetherbound bekommt **keinen eigenen Account-Service, keine Friend-Codes,
keinen separaten Server**. Stattdessen wird die existierende Thot-Matrix-
Infrastruktur als Trägerschicht zweckentfremdet: jeder Thot-DM ist
automatisch ein Multiplayer-Channel.

Eine bestehende Chat-Beziehung in Thot = eine bestehende
Multiplayer-Beziehung in Aetherbound. Kein zusätzlicher Schritt, kein
extra Login, kein extra Friend-Add.

```
┌──────────────────────────────────────────┐
│  THOT (Matrix client, E2E encrypted)     │
│  · DMs                                   │
│  · Rooms                                  │
│  · Identity (matrix-id @user:server)     │
│  · Olm/Megolm encryption                  │
│  · Push notifications                     │
└────────────────┬─────────────────────────┘
                 │  Custom event types
                 ▼
┌──────────────────────────────────────────┐
│  AETHERBOUND (Matrix-aware game)         │
│  · Trade requests                        │
│  · Battle invites                        │
│  · Move actions                          │
│  · Battle replays                        │
└──────────────────────────────────────────┘
```

## Identität: Matrix-ID = Aetherbound-Trainer-ID

Kein zusätzliches Account-System nötig. Der Matrix-User-ID
`@alice:matrix.thot.io` ist gleichzeitig die globale Trainer-ID. Trainer-Cards
zeigen den Matrix-Display-Name + Avatar.

**Vorteile:**
- Single Sign-On
- Identity-Reuse → Aetherbound erbt Thots Sicherheits-Eigenschaften (E2EE, Forward Secrecy)
- Block in Thot = Block in Aetherbound (kostenlos)

## Transport: Matrix-Custom-Event-Types

Matrix erlaubt beliebige Event-Typen jenseits von `m.room.message`. Wir
definieren einen Namespace `io.aether.*`:

| Event Type | Body Schema | Semantik |
|---|---|---|
| `io.aether.trade.offer` | `{partyMember: <EchoformInstance JSON>, expiresAt: <epoch_ms>}` | Trade-Angebot |
| `io.aether.trade.accept` | `{originalEventId: <ref>}` | Trade angenommen |
| `io.aether.trade.cancel` | `{originalEventId: <ref>, reason: <str>}` | Trade abgesagt |
| `io.aether.battle.invite` | `{teamHash: <sha256>, ruleset: "tuxemon-pilot", expiresAt: <epoch>}` | Battle-Anfrage |
| `io.aether.battle.start` | `{rngSeed: <i64>, hostTeam: [...], guestTeam: [...]}` | Beide Teams revealed |
| `io.aether.battle.move` | `{turn: <int>, side: "host"\|"guest", moveIdx: <int>, stateHash: <sha256>}` | Move ausgeführt |
| `io.aether.battle.switch` | `{turn: <int>, side: <str>, partyIdx: <int>}` | Mon gewechselt |
| `io.aether.battle.faint` | `{turn: <int>, side: <str>, index: <int>}` | KO-Bestätigung |
| `io.aether.battle.end` | `{winner: "host"\|"guest"\|"draw", finalLog: [...]}` | Match vorbei |
| `io.aether.spectate.subscribe` | `{battleEventId: <ref>}` | Spectator joined |
| `io.aether.replay.share` | `{log: [BattleEvent], rngSeed: <i64>, teams: [...]}` | Sharable Replay |

**Verschlüsselung kostenlos:** Matrix's `m.room.encrypted` umhüllt alle
diese Events automatisch in olm/megolm. Server sieht nur Ciphertext.

## Synchronisation: Pure Deterministic Resolver = Trivial Sync

Aetherbounds `BattleResolver` ist **bewusst pure**:
```
resolveTurn(state, playerAction, opponentAction) -> nextState
```
Same inputs + same rngSeed = same outputs auf jedem Device.

**Konsequenz:** Spieler müssen nur ihre **Actions** (move-index) austauschen,
nicht den vollen Battle-State. Die `rngSeed` wird beim Match-Start einmal
festgelegt (z.B. `sha256(host_id + guest_id + match_id)`) und beide
Clients berechnen dieselbe Sequenz. Der Matrix-Timestamp ordnet die Events
für uns.

**Anti-Cheat:** Jeder Action-Event enthält den `state_hash` *nach*
Anwendung der Action. Client B vergleicht — wenn die Hashes divergieren,
wurde manipuliert oder es gibt einen Bug → Battle wird abgebrochen mit
`battle.end {winner: "draw", reason: "desync"}`.

## UI-Flow: "Aetherbound-Linse" auf einem Thot-Chat

### Variante A — Aus Aetherbound heraus

```
Aetherbound-Hauptmenü
  ├─ Single Player
  └─ Multiplayer
       ├─ "Connect via Thot"  (öffnet Bridge)
       ├─ Thot-Roster anzeigen (alle DM-Partner)
       │    [Alice    🟢 online   Aetherbound ✓]
       │    [Bob      ⚫ offline  Aetherbound ✓]
       │    [Charlie  🟢 online   Aetherbound ✗ → "Invite to install"]
       └─ Tap auf Alice
            ├─ ⚔️ Challenge to Battle
            ├─ 🔄 Propose Trade
            ├─ 👁️ Spectate ongoing
            └─ 📦 Send Replay
```

### Variante B — Aus Thot heraus (die *interessantere*)

In jedem Thot-Chat erscheint unten ein "🌟 Aetherbound"-Tab:

```
Thot-DM mit Alice
┌─────────────────────────┐
│ Alice: Hey, was geht?   │
│ Du: Lust auf Battle?    │
├─────────────────────────┤  ← Thot-Standard-Composer
│ [Aa] [📎] [😀]    [↑]   │
└─────────────────────────┘
                      ⚔️    ← floating Aetherbound-Linse
                            (nur sichtbar wenn beide Aetherbound haben)
```

Tap auf ⚔️ öffnet einen Bottom-Sheet:
- Quick-Battle-Button → erzeugt direkt `battle.invite`
- Trade → öffnet Echoform-Picker
- View Trainer Card → zeigt Alice's PlayerStatusCard

**"Bindung über Thot":** Genau das. Eine bestehende Vertrauensbeziehung
wird nahtlos zur Game-Beziehung.

### Variante C — Reines Aufruf-via-Thot ("Aetherbound startet sich aus Thot")

Aetherbound ist als Service registriert (Android Intent `aetherbound.battle`).
Aus Thot heraus per Button → Intent löst Aetherbound aus, übergibt den
Matrix-Room-ID + Peer-ID → Aetherbound öffnet sich direkt im
Multiplayer-Battle-Modus mit dem konkreten Peer.

Wenn Aetherbound noch nicht installiert ist → Play-Store-Link mit
Deeplink-Parameter (Room-ID), so dass nach Install der Battle-Invite
sofort fortgesetzt werden kann.

## Drei Button-States (Kerndesign)

Der ⚔️-Button im Thot-Composer hat **drei** Zustände, abhängig vom letzten
gespeicherten `capability.present` des Gegenübers im Room-State:

| State | Bedingung | UI | Tap-Verhalten |
|---|---|---|---|
| **A — Challenge** | beide haben capability.present | ⚔️ gold | öffnet Aetherbound-Battle-Invite |
| **B — Invite** | nur ich habe Aetherbound | ⚔️➕ mit Plus-Badge | sendet Chat-Nachricht "Lust mitzuspielen?" + Play-Store-Deeplink |
| **C — Hidden** | keiner hat Aetherbound | kein Button | (Thot lädt die Game-Integration gar nicht erst) |

**State B ist der Wachstums-Hebel.** Statt nur Mutual-Installs spielen zu
lassen, werden bestehende Thot-Beziehungen zum Onboarding-Kanal: jeder
Aetherbound-Spieler kann ohne externe Reklame seine Kontakte einladen,
und der Empfänger sieht die Einladung in seinem Standard-Messenger,
nicht in Spam-Email oder Werbung.

**Auto-Upgrade von B → A.** Wenn der Eingeladene Aetherbound installiert,
broadcastet seine App `capability.present` in alle DMs. Der Sender's
Thot-Client syncrt das automatisch und der Button wechselt sofort von
B (Plus-Badge) zu A (Challenge). Kein Neustart, kein User-Refresh nötig.

## Onboarding: Opportunistische Discovery

**Capability-Advertise:** Wenn Aetherbound installiert ist, sendet es
einmalig in jeden Thot-Room einen `io.aether.capability.present`-Event
mit `{version: "0.1", features: ["battle", "trade"]}`. Andere Clients
erkennen den Event und können den ⚔️-Button zeigen.

```
Alice installiert Aetherbound
  ↓ Aetherbound Bridge.broadcastCapability(allRooms)
  ↓ Sendet io.aether.capability.present in jeden DM
  ↓ Bob's Thot empfängt
  ↓ Bob's UI zeigt "🌟 Alice plays Aetherbound!"
```

Variante: **Stiller Empfang.** Aetherbound prüft beim Boot die letzten
30 Tage Events in jedem Room. Findet es ein `capability.present` →
markiert den Peer als spielfähig.

## Match-Flow im Detail

```
HOST (Alice)                          GUEST (Bob)
─────────                              ──────────
1. Alice tippt ⚔️ Challenge
2. Aetherbound bridge.inviteBattle()
   └─ Matrix sendet:
      {type: "io.aether.battle.invite",
       teamHash: sha256(alice.team),
       expiresAt: now+5min}        →   Empfangen via observeActions()
                                       3. Bob's UI: "Alice will fight!"
                                          [Accept] [Decline]
                                       4. Bob tippt Accept
                                       5. Bob's bridge.acceptBattleInvite()
                                          └─ sendet:
                                             {type: "io.aether.battle.start",
                                              rngSeed: <derived>,
                                              hostTeam: [...] (encrypted),
                                              guestTeam: [...]}
6. Alice empfängt start
7. BEIDE öffnen BattleScene
   mit hostInstance, guestInstance,
   sharedRngSeed
8. Alice tippt Move 0
   └─ sendet:
      {type: "io.aether.battle.move",
       turn: 1, side: "host", moveIdx: 0,
       stateHash: <after-resolution>}  →  Bob empfängt
                                          9. Bob's resolver: simuliert
                                             selben turn mit selben Seed
                                             Vergleicht stateHash
                                             ✓ matches → animiert
10. Bob tippt seinen Move
    (gleicher Tausch in Gegenrichtung)
...
N. Final Faint → battle.end → beide
   zurück ins Menü
```

## Spectator-Mode (sehr cool)

Charlie ist in einem 3er-Room mit Alice & Bob. Alice & Bob fechten ein
Battle. Charlie sieht:
```
[Battle in progress: Alice vs Bob]
[👁️ Spectate]
```
Tap → Charlie's Aetherbound öffnet die selbe BattleScene, hört auf
`io.aether.battle.move`-Events mit, replayt jede Aktion mit dem geteilten
Seed, sieht alles live mit. Kein zusätzliches Protokoll nötig — nur
read-only Subscription auf den Room.

## Replay-Sharing

Battle-Log + rngSeed + Teams in einem JSON-File →
`io.aether.replay.share`-Event. Empfänger tippt → Aetherbound spielt das
Battle deterministisch nach. Perfekt für "schau mal was für ein epischer
Comeback":
```
Alice → Bob: "schau mal" + 📎 battle_replay.json
Bob: tap → Aetherbound öffnet sich im Replay-Mode
```

## Trade-Flow

```
Alice picks Mosslyn from her party
→ tap "Trade with Bob"
→ Aetherbound bridge.proposeTrade(bob_id, mosslyn)
→ Matrix sendet trade.offer event
   {partyMember: <Mosslyn JSON snapshot>}

Bob's UI: "Alice offers Mosslyn (Lv.18)"
         [Accept] [Counter-offer]

Bob tippt Accept → bridge.acceptTrade()
→ Matrix sendet trade.accept

Alice's Aetherbound: removes Mosslyn from party
Bob's Aetherbound: adds Mosslyn to party
```

**Tradevolution:** Tuxemon hat Evolutions die nur durch Trade triggern.
EvolutionEngine.checkReady(TRIGGER.TRADE, ...) wird auf beiden Seiten
nach erfolgreichem Trade gefeuert.

## Tor-Compliance (Thot's "Tor mandatory" Doktrin)

Thot routet **alle** Matrix-Traffic verpflichtend über Tor. Aetherbound
erfüllt diese Doktrin automatisch und ohne Sonderlogik, weil:

1. Aetherbound macht **null eigene Network-Calls** — kein STUN/TURN, keine
   eigenen Sockets, kein WebRTC, kein direktes P2P. Alles geht durch
   Thots Matrix-Client.
2. `MultiplayerBridge` ist abstrakt — die Impl liegt im Thot-Modul, das
   Thots existierenden (Tor-mandanten) Matrix-Client wiederverwendet.
3. Es existiert **kein Code-Pfad in Aetherbound, der Tor umgehen könnte.**
   Single-Player ist 100% offline; Multiplayer hängt zwingend an Thots Bridge.

### Tor-bedingte Latenz: warum es egal ist

| Aspekt | Clearnet | Tor (3 Hops) |
|---|---|---|
| Round-trip pro Move | ~150ms | ~800-1500ms |
| Spielergefühl | sofort | "Move sent…" 1-2s sichtbar |
| Pokémon-Konvention | Spieler tippt ~3-10s pro Move sowieso | unbemerkbar |
| Move-Größe | ~80 bytes | ~80 bytes |

Turn-based + winzige Payloads + bereits-vorhandene-Wartezeit pro Move =
Tor-Latenz fällt im Tippzeit-Rauschen unter.

### Tor-aware Defaults: `BattleTimeouts.Tor`

```kotlin
val Tor = BattleTimeouts(
    moveTimeout      = 45.seconds,    // statt 30s — schluckt Circuit-Flaps
    inviteExpiry     = 5.minutes,     // genug Zeit für Push via ntfy + Tor
    syncTolerance    = 30.seconds,    // großzügig für state-hash exchange
    captureWaitTimeout = 20.seconds,
)
```

`BattleTimeouts.Clearnet` existiert für Debug/Preview, `BattleTimeouts.Replay`
schaltet alle Timeouts ab.

### Connection-State-UI

`MultiplayerStatusIndicator` ist der UI-Indikator für die Battle-Scene.
Pollt `bridge.connectionInfo()` alle 4s, zeigt:

```
🟢 CLEARNET · 120ms              (extrem selten)
🟡 TOR · 3 hops · 850ms          (Standardfall in Thot-Verbindung)
🔴 RECONNECTING                  (Circuit-Rebuild oder push delay)
```

Bei Disconnect pulsiert die Komponente (alpha-fade) damit der Spieler weiß
"die App arbeitet noch, wartet nur auf neue Tor-Circuit". Wichtig fürs
mentale Modell — sonst denkt der Spieler die App sei eingefroren.

### Anti-Fingerprinting-Bonus

Battle-Move-Events haben dieselbe Traffic-Signatur wie Text-Messages:
- Selber `m.room.encrypted` Wrapper
- ähnliche Payload-Größe (Move ~80 byte ciphertext, kurze Nachricht ~50-300 byte)
- Burst-Frequenz während aktivem Battle ähnlich aktivem Chat

Ein Tor-Exit-Sniffer kann **nicht** unterscheiden zwischen "Alice schreibt
Bob" und "Alice tippt Move gegen Bob" — Aetherbound erweitert Thots
Cover-Traffic kostenlos.

### Push-Notifications (ntfy clearnet)

Thots Push-Gateway läuft per Doktrin clearnet (ntfy). Aetherbound erbt das:
- Battle-Invites kommen über denselben Push-Kanal wie DM-Messages
- Topic leakt clearnet als z.B. "thot-push-alice" — keine Inhalte
- Inhalt (`io.aether.battle.invite`) wird **erst nach App-Wakeup** über
  Tor-Matrix entschlüsselt, kein Clearnet-Leak von Battle-Daten
- Akzeptabler Tradeoff (existiert in Thot bereits)

### Hidden-Service-Homeserver

Falls die Thot-HS-Instanz `.onion` ist: end-to-end Tor, Server-IP
unbekannt. Aetherbound funktioniert **identisch** — die Bridge sieht nur
Matrix-API-Calls.

---

## Sicherheit & Privacy

- ✅ **E2EE für alles:** Matrix-Olm/Megolm umhüllt jeden custom Event
- ✅ **Kein Server kennt Spielzustand:** alle Logik am Edge
- ✅ **Block-respect:** wenn Alice Bob in Thot blockiert, sind keine
  game-events sichtbar
- ✅ **Forward Secrecy:** Trade-Logs aus Sept. sind im Okt. unentschlüsselbar wenn Schlüssel rotieren
- ⚠️ **Cheat-Vector:** Client kann manipulierte Battle-States senden →
  state_hash-Validierung deckt das auf, Match wird gecancelt
- ⚠️ **Replay-Attacks:** jeder Event hat unique `event_id` von Matrix →
  Doppelaktionen werden ignoriert

## Implementierungsphasen

| Phase | Aufgabe | Größe | Status |
|---|---|---:|---|
| 1 | `MultiplayerBridge`-Interface (✓ existiert in `core.data`) | — | ✅ |
| 2 | `MatrixWireFormat.kt`: Event-Type-Strings + JSON-Schemas | S | TBD |
| 3 | Aetherbound-Side: Roster-Screen mit `bridge.listOpponents()` | M | TBD |
| 4 | Thot-Side: `ThotMatrixBridge.kt` im Thot-Modul (impl von `MultiplayerBridge`) | L | TBD |
| 5 | Capability-Advertise + Discovery | S | TBD |
| 6 | Battle-PvP-Screen mit Sync | M | TBD |
| 7 | Trade-UI | S | TBD |
| 8 | Spectator + Replay | M | TBD |
| 9 | Thot-Side ⚔️-Button im Chat-Composer | M | TBD |

## Empfehlung für nächsten konkreten Schritt

**Phase 2 jetzt bauen:** `MatrixWireFormat.kt` als Single-Source-of-Truth
für alle Event-Type-Konstanten und JSON-Encoder/Decoder. Damit kann der
Thot-Module-Entwickler genau wissen welche Strings er senden muss.

Phase 3 (Roster) wäre der zweite konkrete Schritt — Aetherbound bekommt
einen `MultiplayerScreen.kt` der eine Liste der Thot-DMs zeigt. Solange
`Multiplayer.bridge` der `NoOpMultiplayerBridge` ist, ist die Liste leer
mit "No bridge connected" Hinweis. Das ist ein klarer Test-Punkt.

## Langfrist-Visionen (jenseits Pokémon-Konvention)

- **Daily Co-Op Raids:** Matrix-Push triggert "Heute 19:00 Legendary Raid",
  4 Spieler aus dem selben Thot-Group-Chat müssen koordiniert kämpfen
- **Echoform-Mailing:** Echoform per Thot-Push verschicken wie eine WhatsApp-Sticker
- **Battle-Tournaments-via-Group-Chat:** 8-Spieler-Bracket organisiert sich selbst über einen Thot-Room
- **Cross-Game-Events:** Aetherbound nutzt Thot-Daten (Anzahl Chats, Online-Stunden) für In-Game-Boni
- **"Aether-Bonds":** persistente Multiplayer-Beziehungen die sich aus Trade-/Battle-Statistiken ableiten — wer hat mit wem am meisten getradet wird zum "Soulmate-Trainer-Pair" mit Special-Move
