# Aetherbound — Economy + Multiplayer-Reward Design

## Ziel

Geld und XP sollen sich anfühlen — Sieg ist süß, Verlust ärgert, Self-
Farming auf zwei Geräten ist möglich aber langwierig (skaliert quadratisch
mit Level-Differenz). Trainer-Battles + Multiplayer + Wild-Encounters
fühlen sich wie unterschiedliche Belohnungs-Curves an, nicht alle gleich.

## Geld-Skala (Pokemon-Ballpark, kalibriert)

### Verdienen

| Quelle | Betrag |
|---|---:|
| Wild-Encounter besiegen | 0 (nur XP) |
| Trainer-Sieg (1-Mon-Team) | 200-300 |
| Trainer-Sieg (2-3-Mon-Team) | 400-700 |
| Trainer-Sieg (4-5-Mon-Team) | 800-1200 |
| Gym Leader | 1500-3500 |
| Multiplayer-Sieg | max(5,000, 5% des Verlierer-Wallets) — kein Upper-Cap |
| Story-Quest-Reward | 500-3000 |
| Hidden-Pickup (Item-Drop) | 200-1500 |

**Multiplayer-Pot-Logik (flat-floor, debt-cap):**
```
pot = max(5000, loser.money * 0.05)
loser.money clamped at -5000 floor
winner.money += actually_paid
```
- Bob mit $10.000: zahlt $5.000 (5% wäre $500, aber Floor zieht hoch)
- Bob mit $50.000: zahlt $2.500 → Floor greift, **$5.000 zahlt er**
- Bob mit $200.000: zahlt $10.000 (5% schlägt durch, kein Cap mehr)
- Bob mit $200: zahlt $200 + geht auf -$4.800 Schulden
- Bob mit -$5.000 (Schulden-Floor): zahlt $0, Match-Pot leer aber er kann
  weiterspielen, nur Winner bekommt nichts mehr in Cash

**Eintritts-Schwelle:** Spieler braucht effektiv keine Mindestsumme um zu
matchen — auch ein neuer Spieler mit $1.500 Startgeld kann sofort
multiplayer-fechten. Ein Verlust setzt ihn auf -$3.500, ein zweiter auf
-$5.000 (Floor). Dritter Verlust kostet ihn nichts mehr in Geld
(Schulden-Floor erreicht), aber er bleibt verschuldet bis er Trainer-Wins
sammelt um aus dem Minus zu kommen.

**Schmerz-Curve im Detail:**
| Verlierer-Wallet | Pot (was Winner kriegt) | Verlierer-Resultat |
|---:|---:|---|
| $200 | $5.000 | $200 → -$4.800 |
| $1.500 | $5.000 | $1.500 → -$3.500 |
| $5.000 | $5.000 | $5.000 → $0 |
| $50.000 | $5.000 | -10% Wallet (Floor greift) |
| $200.000 | $10.000 | -5% Wallet |
| $1.000.000 | $50.000 | -5% Wallet (kein Cap mehr) |
| -$5.000 (Floor) | $0 | bleibt -$5.000, kein Cash-Flow |

### Ausgeben

Item-Pricing folgt Pokemon-Konvention (innere Spanne, balanciert mit
Trainer-Rewards):

| Kategorie | Item | Preis |
|---|---|---:|
| **Heal** | Potion | 200 |
|  | Super Potion | 700 |
|  | Hyper Potion | 1500 |
|  | Max Potion | 2500 |
|  | Revive | 1500 |
|  | Max Revive | 4000 |
|  | Cureall | 600 |
| **Balls** | Tuxeball | 200 |
|  | Tuxeball Great | 600 |
|  | Tuxeball Ultra | 1200 |
|  | Tuxeball Lure | 1000 |
|  | Tuxeball Quick | 1000 |
|  | Tuxeball Timer | 1000 |
|  | Tuxeball Master | unkaufbar (story-only) |
| **Stones** | Fire/Water/Wood Stone | 2100 |
|  | Sun/Moon Stone | 3000 |
|  | Cosmic Stone | 5000 |
| **TMs** | TM Common (acid, ember) | 1000 |
|  | TM Rare (psybeam, ice_beam) | 3000 |
|  | TM Epic (hyper_beam) | 7500 |
| **Berries** | Single-effect berry | 100 |
|  | Held-stat berry | 300 |
| **Repels** | Standard | 350 |
|  | Super | 500 |
|  | Max | 700 |
| **Story** | Bicycle, Aether-Lens | 5000-10000 |

**Zwei-Bahn-Spending:**
- Konsumgüter (Potions, Balls): konstanter "drain", sodass Geld immer fließt
- Investitionen (Stones, TMs, Story): seltene große Käufe, Spieler spart drauf

## XP-Formel mit Level-Differenz

Aktuelle Formel:
```kotlin
fun xpFromVictory(loserLevel, loserBaseStatSum, isTrainer): Int {
    val baseYield = (loserBaseStatSum * 0.4).coerceAtLeast(40)
    val raw = baseYield * loserLevel / 7
    return if (isTrainer) raw * 3 / 2 else raw
}
```

Neue Formel mit Level-Diff-Skalierung:

```kotlin
fun xpFromVictory(
    winnerLevel: Int,
    loserLevel: Int,
    loserBaseStatSum: Int = 360,
    sourceMode: SourceMode = SourceMode.WILD,
): Int {
    val baseYield = (loserBaseStatSum * 0.4).coerceAtLeast(40)
    val raw = baseYield * loserLevel / 7

    // Level-differential scaler — quadratic falloff when winner is much
    // higher level. Identical level: 1.0×. Winner double level: 0.25×.
    val ratio = (loserLevel.toDouble() / winnerLevel.coerceAtLeast(1).toDouble())
                  .coerceIn(0.05, 1.0)
    val diffScaler = ratio * ratio

    val modeMult = when (sourceMode) {
        SourceMode.WILD -> 1.0
        SourceMode.TRAINER -> 1.5
        SourceMode.MULTIPLAYER -> 1.2
    }

    return (raw * diffScaler * modeMult).toInt().coerceAtLeast(1)
}
```

### Beispiele

| Winner-Level | Loser-Level | Diff-Scaler | Mode | Final XP |
|---:|---:|---:|---|---:|
| 20 | 20 | 1.00 | WILD | 411 |
| 20 | 20 | 1.00 | TRAINER | 616 |
| 20 | 20 | 1.00 | MULTIPLAYER | 493 |
| 20 | 25 | 1.00 (capped) | MULTIPLAYER | 616 |
| 70 | 20 | 0.082 | MULTIPLAYER | **40** |
| 70 | 70 | 1.00 | MULTIPLAYER | 1727 |
| 70 | 50 | 0.510 | MULTIPLAYER | 879 |
| 70 | 100 | 1.00 (capped) | MULTIPLAYER | 2470 |

**Self-Farming-Limit:** Wenn Alice mit Level 70 ihre eigene Level 20 Echoform
auf einem zweiten Gerät besiegt → 40 XP pro Match. Bei Level 70 braucht
sie für ein Level-Up ~50000 XP — also **1250 Matches**. Bei realistischen
3 min/Match = 60 Stunden ununterbrochen. Komplett unrealistisch als
Exploit.

Echte Multiplayer auf gleichem Level: 1727 XP/Match → 30 Matches bis
Level-Up bei Level 70. Spürbar aber nicht Sprint-fast.

## Reward bei Verlust

```
WIN:
  + win_money (5% of opponent's wallet, clamped 50-2000)
  + win_xp (per formula above) → applied to MVP-mon (most kills)
  + multiplayerStats.wins++
  + match record in recent-10
  + first-win badge if applicable

LOSS:
  - loss_money (5% of own wallet, clamped 50-2000)
  + 0 XP
  + multiplayerStats.losses++
  + match record in recent-10

DRAW:
  - 0 money
  + 0 XP
  + counts in recent-10 but not in win/loss totals
```

## Anti-Exploit-Safeguards

| Regel | Wert |
|---|---|
| Min-Match-Length | 3 Turns (kürzere = "no result" für Stats) |
| Same-peer cooldown | 60s zwischen Matches |
| Daily-cap pro Peer-Pair | 20 Ranked, danach unranked (kein Money-Flow, nur XP) |
| Money-Floor | Spieler kann nie unter $0 (clamp at 0 — overflow goes to "debt" stat) |
| Level-Cap-Format | Lobby kann level-cap setzen (10 / 30 / 50 / unlimited) |

## Statistik-Anzeige

Trainer Card erweitert um:

```
┌────────────────────────────────┐
│  AETHER BATTLE RECORD          │
│  ┌─────────────────────────┐   │
│  │ Wins      32            │   │
│  │ Losses    18            │   │
│  │ Win-Rate  64%           │   │
│  │ Streak    +5  (max +12) │   │
│  │ Biggest Upset  Lv 35→62 │   │
│  │ Total Pot Won  $14,200  │   │
│  └─────────────────────────┘   │
│                                │
│  Last 10 matches:              │
│  • W vs Bob          5 turns   │
│  • L vs Charlie      8 turns   │
│  • W vs Alice (sweep) 6 turns  │
│  ...                           │
└────────────────────────────────┘
```

## Ranked vs Casual

Lobby toggle:

- **Ranked:** Money + XP + stats wie oben
- **Casual:** keine Money/XP-Bewegung, nur "spar match" für Skill-Test
  und Bestiary-Achievement (sehen-Mark wird gesetzt — keine Anti-Exploit
  weil keine ressourcen-rewards)

## Implementations-Schritte

1. **`ExperienceEngine.xpFromVictory()`** Signatur erweitern:
   - Neuer Parameter `winnerLevel`
   - Neue Enum `SourceMode { WILD, TRAINER, MULTIPLAYER }`
   - Backward-compat: alter `isTrainer: Boolean` als deprecated overload
2. **`ShopCatalog.kt`** als statische Daten-Datei mit allen Preisen
3. **`MultiplayerRewards.kt`** mit `computePotAnte(loserMoney): Int`
4. **`MultiplayerStats`** erweitern: `currentStreak`, `longestStreak`,
   `biggestUpset`, `totalPotWon`
5. **`PlayerStatusCard`** UI erweitern um den Battle-Record-Block
6. **`ShopScreen.kt`** + Shop-NPC-Object-Type für die Welt
