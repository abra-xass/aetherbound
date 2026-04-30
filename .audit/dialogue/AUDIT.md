# Tuxemon-Dialog-Audit

**1.267 einzigartige NPC-msgids** in 235 TMX-Maps · 1.607 Aufrufe · 1.171 mit Upstream-en_US-Text · 196 mit de_DE-Text

## TL;DR — Policy-Verteilung

| Policy | msgids | % |
|---|---:|---:|
| **REWRITE_VOICE** | 634 | 50.0 % |
| **PRESERVE_MEANING** | 530 | 41.8 % |
| **INVESTIGATE** | 76 | 6.0 % |
| **KEEP_STRUCTURE** | 27 | 2.1 % |
| Σ | 1267 | 100 % |

→ ~50 % darfst du frei in unsere Voice umschreiben.  
→ ~42 % muss Story-/Quest-Inhalt erhalten bleiben (Stil aber polierbar).  
→ ~8 % strukturell stabil halten (Mechanik + Choice-Dividers).

## Kategorien-Aufschlüsselung

| Kategorie | Anzahl | Policy | Was zu tun ist |
|---|---:|---|---|
| 📜 **QUEST_LORE** | 518 | `PRESERVE_MEANING` | Story-Sequenzen, Lore-Dumps, Item-Übergaben, mehrteilige Quests. Inhalt darf NICHT verloren gehen. |
| 🗣️ **NPC_FLAVOR** | 551 | `REWRITE_VOICE` | Generische Villager-Linien, einmalige Statements. Frei in unsere Voice umschreiben. |
| 📋 **WORLD_SIGN** | 42 | `REWRITE_VOICE` | Schilder, Anschläge, Notizen in der Welt. Frei umschreibbar. |
| ⚔️ **TRAINER_PRE** | 41 | `REWRITE_VOICE` | Pre-Battle-Taunts. Trash-Talk in unsere Voice. |
| 🏆 **TRAINER_OUTCOME** | 7 | `PRESERVE_MEANING` | Win/Loss/Redo-Linien. Outcome-Hinweise erhalten. |
| 🔁 **HIGH_REUSE** | 7 | `KEEP_STRUCTURE` | In vielen Maps recycelt — Heal-Center-Standardphrasen. Strukturell stabil bleiben. |
| 🎁 **ITEM_LABEL** | 5 | `PRESERVE_MEANING` | Item-Namen + Beschreibungen. Funktion erhalten. |
| 🔘 **CHOICE_OPTION** | 20 | `KEEP_STRUCTURE` | Multiple-Choice-Optionsdividers (yes:no, ja:nein). 1:1 strukturell. |
| ❓ **ORPHAN_NO_TEXT** | 76 | `INVESTIGATE` | Im TMX referenziert, aber kein Upstream-Text. Vermutlich obsolete Choice-Bezeichner. |

## Stichproben pro Kategorie

### 📜 QUEST_LORE — 518 msgids · Policy: `PRESERVE_MEANING`

Story-Sequenzen, Lore-Dumps, Item-Übergaben, mehrteilige Quests. Inhalt darf NICHT verloren gehen.

- **`37707_computer`**  ·  used 1× in 1 map(s)
  - en: Developer console: This is intended for Tuxemon developers only, for testing an experimental data loading system.\nThe variable for the new loader will now be enabled. Please report any issues you find at https://tuxemon
- **`37707_villager_female_1`**  ·  used 1× in 1 map(s)
  - en: Wish: Oh... a newcomer! How did you get here? We haven't seen anyone new in... oh, never mind.\nWish: Welcome! Welcome to... oh my: We never got around to giving our town a name! Just call us whatever you'd like I guess.
- **`37707_villager_female_2`**  ·  used 1× in 1 map(s)
  - en: Wish: Please: Do not enter the tower. It hasn't been built yet.
- **`37707_villager_male_1`**  ·  used 1× in 1 map(s)
  - en: Blank: Hmph. Hi. Sorry: We're usually weary of outsiders. We only get visited... sometimes.\nBlank: But since you're here, maybe you can do me a favor. It might sound strange of me to ask you this, yet I must know...\nBl
- **`37707_villager_male_2`**  ·  used 1× in 1 map(s)
  - en: Blank: You can't help any more. We are not allowed to talk about it. You cannot understand. Leave.
- **`37707_villager_female_3`**  ·  used 1× in 1 map(s)
  - en: Wish: No, no, no: Stop doing this! Do you not understand it's not there?

*…und 512 weitere*

### 🗣️ NPC_FLAVOR — 551 msgids · Policy: `REWRITE_VOICE`

Generische Villager-Linien, einmalige Statements. Frei in unsere Voice umschreiben.

- **`37707_town01`**  ·  used 1× in 1 map(s)
  - en: You got a Fruitera for testing purposes. This will be removed later!
- **`37707_house`**  ·  used 5× in 1 map(s)
  - en: Strange: It's locked. No one seems to be using this building.
- **`37707_mail`**  ·  used 5× in 1 map(s)
  - en: It's empty and full of spider webs.
- **`37707_flower`**  ·  used 1× in 1 map(s)
  - en: You sense a warm energy emanating from this flower. It's the only thing in this town your Tuxemon seem to enjoy.
- **`37707_villager_female_missing`**  ·  used 1× in 1 map(s)
  - en: @*!^: 0H... @ n3wc0m3r! H0vv p1d y0v g3t he7e? <NONE> h3llen\"T $33N %^&one new 1n... ()h, never n3ver m3V3t N3W37 MIIIIIIIIND\n@*!^: WElcoME! W3lcome t0... 0H N/A: We never g0t ar0und to gi^ing 0ur town {<1} We never g0
- **`37707_villager_male_missing`**  ·  used 1× in 1 map(s)
  - en: 00000: i W@$ n3v3R B0rN\n${{name}} ${{name}} ${{name}} ${{name}} ${{name}} ${{name}} ${{name}} ${{name}} ${{name}} ${{name}}
  - de: 00000: i W@$ n3v3R B0rN\n${{name}} ${{name}} ${{name}} ${{name}} ${{name}} ${{name}} ${{name}} ${{name}} ${{name}} ${{name}}

*…und 545 weitere*

### 📋 WORLD_SIGN — 42 msgids · Policy: `REWRITE_VOICE`

Schilder, Anschläge, Notizen in der Welt. Frei umschreibbar.

- **`37707_sign_welcome`**  ·  used 1× in 1 map(s)
  - en: Welcome to town 37707! We are...
- **`37707_sign_tower`**  ·  used 1× in 1 map(s)
  - en: Town 37707 tower. This structure is highly unstable: Do not enter!
- **`37707_sign_foreign`**  ·  used 1× in 1 map(s)
  - en: This sign is written in a foreign language you can't understand.
- **`37707_sign_hidden`**  ·  used 1× in 1 map(s)
  - en: Get out while you still can...
- **`37707_sign_missing_welcome`**  ·  used 1× in 1 map(s)
  - en: We11c0me T0 t0wn ERROR! We ne^er weR3
- **`37707_sign_missing_tower`**  ·  used 1× in 1 map(s)
  - en: Town N/A <undefined_structure>. Denetsil evah dluohs uoy

*…und 36 weitere*

### ⚔️ TRAINER_PRE — 41 msgids · Policy: `REWRITE_VOICE`

Pre-Battle-Taunts. Trash-Talk in unsere Voice.

- **`challenger`**  ·  used 1× in 1 map(s)
  - en: Challenger!\nStep to the center and face me!
- **`challenger2`**  ·  used 1× in 1 map(s)
  - en: You have done well to make it this far!
- **`challenger3`**  ·  used 1× in 1 map(s)
  - en: ... .... ...
- **`challenger4`**  ·  used 1× in 1 map(s)
  - en: Wait, this is your first battle?\nI was told I would be the third battle...
- **`challenger5`**  ·  used 1× in 1 map(s)
  - en: No matter!
- **`challenger6`**  ·  used 1× in 1 map(s)
  - en: This just means it will be easier to crush you!

*…und 35 weitere*

### 🏆 TRAINER_OUTCOME — 7 msgids · Policy: `PRESERVE_MEANING`

Win/Loss/Redo-Linien. Outcome-Hinweise erhalten.

- **`spyder_papertown_firstfight_win`**  ·  used 1× in 1 map(s)
  - en: Huh, must be a fluke. There's no way the new model would be worse than the old!
- **`spyder_papertown_firstfight_lose`**  ·  used 1× in 1 map(s)
  - en: As expected! Old models can't compare to new ones!
- **`acolyte1loss`**  ·  used 1× in 1 map(s)
  - en: Ha! Some Challenger you are. You'll have to do better than that to beat me!\nTry and level up your Tuxemon before facing me again!
- **`acolyte1redo`**  ·  used 1× in 1 map(s)
  - en: So, you actually crawled back... Let's see how powerful you really are!
- **`acolyte2challengerwin`**  ·  used 1× in 1 map(s)
  - en: ... Well done. I put the blame of this loss entirely on Chris though.\nIf he hadn't missed his mark, I'm positive I could have won...\nIf you exit out the entrance behind me, you'll teleport back to the main room.
- **`acolyte2challengerloss`**  ·  used 1× in 1 map(s)
  - en: Ha! You put up a good fight, but as always, no one can defeat the MIGHTY GARTH!\nFeel free to try again when you think you actually have a chance!

*…und 1 weitere*

### 🔁 HIGH_REUSE — 7 msgids · Policy: `KEEP_STRUCTURE`

In vielen Maps recycelt — Heal-Center-Standardphrasen. Strukturell stabil bleiben.

- **`okaythen`**  ·  used 5× in 5 map(s)
  - en: Okay! Just give me a second and I'll heal your Tuxemon!
  - de: Okay! Gib mir nur ein paar Sekunden und ich werde dein Tuxemon heilen!
- **`okaythen2`**  ·  used 5× in 5 map(s)
  - en: Well, it looks like your Tuxemon are all better now!\nHave a wonderful day!
  - de: Gut, ich denke deine Tuxemon sind wieder bei voller Energie! Hier hast du sie wieder!
- **`spyder_scoop_welcome`**  ·  used 13× in 5 map(s)
  - en: Welcome!
- **`spyder_multi_martsign`**  ·  used 6× in 6 map(s)
  - en: Buy things here
- **`spyder_generic_nurse`**  ·  used 7× in 7 map(s)
  - en: Hey, you look tired. Let me help you feel better!
- **`water_nurse1`**  ·  used 5× in 5 map(s)
  - en: Let me heal your Tuxemon for you.

*…und 1 weitere*

### 🎁 ITEM_LABEL — 5 msgids · Policy: `PRESERVE_MEANING`

Item-Namen + Beschreibungen. Funktion erhalten.

- **`potion`**  ·  used 5× in 4 map(s)
  - en: Potion
  - de: Trank
- **`tea`**  ·  used 3× in 3 map(s)
  - en: Tea
- **`mystery_tea`**  ·  used 1× in 1 map(s)
  - en: Mystery Tea
- **`metal_berry`**  ·  used 1× in 1 map(s)
  - en: Metal Berry
- **`super_potion`**  ·  used 2× in 2 map(s)
  - en: Super Potion
  - de: Super Trank

### 🔘 CHOICE_OPTION — 20 msgids · Policy: `KEEP_STRUCTURE`

Multiple-Choice-Optionsdividers (yes:no, ja:nein). 1:1 strukturell.

- **`mistaken:thats_me`**  ·  used 1× in 1 map(s)
- **`yes:no`**  ·  used 55× in 26 map(s)
- **`somebody:nobody`**  ·  used 1× in 1 map(s)
- **`sure_i_do:not_really`**  ·  used 1× in 1 map(s)
- **`0floor:2floor:no`**  ·  used 4× in 1 map(s)
- **`0floor:1floor:no`**  ·  used 4× in 1 map(s)

*…und 14 weitere*

### ❓ ORPHAN_NO_TEXT — 76 msgids · Policy: `INVESTIGATE`

Im TMX referenziert, aber kein Upstream-Text. Vermutlich obsolete Choice-Bezeichner.

- **`cityparkchoice`**  ·  used 1× in 1 map(s)
- **`chooses`**  ·  used 8× in 6 map(s)
- **`grampsask`**  ·  used 2× in 1 map(s)
- **`happy`**  ·  used 1× in 1 map(s)
- **`shady`**  ·  used 1× in 1 map(s)
- **`maplecotton`**  ·  used 1× in 1 map(s)

*…und 70 weitere*

## Empfohlene Verarbeitungsschritte

1. **PRESERVE_MEANING** (~530 msgids): Ich bekomme den englischen Originaltext, schreibe ihn in unsere deutsche Voice um — ohne Quest-/Item-Information zu verlieren. Pro Eintrag eine 1:1-Korrespondenz im Pack: `dialog/<msgid>.de`.
2. **REWRITE_VOICE** (~634 msgids): Tracery-Templates mit Pool-Slots. Pro Archetyp 5-10 Schablonen × Slot-Variationen → tausende Outputs. Original-msgid mappt auf Kategorie + Sub-Kategorie (z. B. `cotton_villager_male` → `villager.adult.male`).
3. **KEEP_STRUCTURE** (~103 msgids): 1:1-Mapping in eine kleine Lookup-Tabelle. `yes:no` → `"Ja:Nein"`. `okaythen` → ein etwas neutralerer Heal-Center-Text. Funktional stabil.
4. **ORPHAN_NO_TEXT** (76 msgids): Investigieren — das sind fast alle Choice-Option-Bezeichner ohne separaten Übersetzungstext. Vermutlich genug, sie als KEEP_STRUCTURE mit Default-`Ja:Nein` zu behandeln.

## Output-Artefakte (.audit/dialogue/)

- `referenced.json` — alle 1.267 msgids mit Original-en/de-Texten + Map-Refs
- `classified.json` — selbe Daten + Auto-Klassifizierung pro msgid
- `AUDIT.md` — diese Datei
