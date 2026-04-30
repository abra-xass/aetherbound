# Classic Monster RPG Gap Check and Mechanics Adaptation

## Sources Checked

- Bulbapedia statistic/stat formula reference: https://bulbapedia.bulbagarden.net/wiki/Statistic
- Bulbapedia experience reference: https://bulbapedia.bulbagarden.net/wiki/Experience
- Bulbapedia damage formula reference: https://bulbapedia.bulbagarden.net/wiki/Damage_formula
- Bulbapedia effort values reference: https://bulbapedia.bulbagarden.net/wiki/Effort_values

## What To Adapt

Classic Pokemon-style depth comes from several hidden or semi-hidden layers:

- Species base stats.
- Individual values per creature.
- Effort/training values earned through battle.
- Nature modifying one stat up and one stat down.
- Move categories using physical or special offense/defense.
- Same-type attack bonus.
- Type effectiveness.
- Level-scaled damage.
- Experience growth groups.
- Evolution conditions.
- Held items.
- Abilities.
- Status effects.
- Trainer AI tiers.

Aetherbound should adopt the system shape, not the protected names or exact theme language.

## Aetherbound Terms

| Classic Concept | Aetherbound Term | Target |
|---|---|---|
| HP | Vigor | current/max battle health |
| Attack | Force | physical technique offense |
| Defense | Guard | physical technique defense |
| Special Attack | Focus | pulse/special technique offense |
| Special Defense | Ward | pulse/special technique defense |
| Speed | Tempo | action order |
| Nature | Temperament | one stat up, one stat down |
| IV | Potential | 0-31 per stat |
| EV | Training Points | max 510 total, 252 per stat |
| STAB | Aspect Affinity | bonus when technique matches Aspect |
| Type | Element / Aspect | 12-element chart; Aspect remains internal data term |
| Ability | Instinct | passive trait |
| Move | Technique | battle command |

## Adopted Stat Formula

Use a Pokemon-like deterministic formula with Aetherbound terms:

```text
Vigor =
  floor(((2 * BaseVigor + Potential + floor(TrainingPoints / 4)) * Level) / 100)
  + Level
  + 10

OtherStat =
  floor(
    (
      floor(((2 * BaseStat + Potential + floor(TrainingPoints / 4)) * Level) / 100)
      + 5
    )
    * TemperamentModifier
  )
```

Rules:

- Potential range: 0-31.
- Training Points per stat max: 252.
- Training Points total max: 510.
- Temperament modifier: 1.1, 1.0, or 0.9.
- Vigor is not modified by Temperament.
- Legendary Echoforms have higher base stat budgets but still use the same formula.

## Adopted Damage Formula

Use a Pokemon-like damage skeleton with Aetherbound terms:

```text
baseDamage =
  floor(
    floor(
      floor((2 * AttackerLevel) / 5 + 2)
      * TechniquePower
      * AttackStat
      / DefenseStat
    )
    / 50
  )
  + 2

finalDamage =
  floor(baseDamage * targets * weather * critical * random * aspectAffinity * effectiveness * burn * otherModifiers)
```

Rules:

- Strike techniques use Force vs Guard.
- Pulse techniques use Focus vs Ward.
- Guard/Field/Recovery techniques usually do not use damage formula.
- Random modifier: integer 85-100 percent for non-ranked/casual play.
- Ranked deterministic play derives the random roll from match seed and turn input hash.
- Element Affinity: 1.5 when the Technique matches one of the user's Elements.
- Effectiveness uses Aetherbound's Aspect chart, not Pokemon's type chart.
- Critical default: 1.5, not 2.0, to reduce swinginess.
- Burn halves Force-based damage unless an Instinct says otherwise.

## Adopted XP Shape

Use two modes:

- Story/offline: mostly flat XP by opponent level and species yield.
- Matrix PvP and high-level rematches: scaled XP by level difference to prevent farming weak opponents.

```text
baseXp = floor((opponentBaseYield * opponentLevel) / 5)

scaledLevelFactor =
  ((2 * opponentLevel + 10) ^ 2.5)
  / ((opponentLevel + participantLevel + 10) ^ 2.5)

finalXp =
  floor(baseXp * battleTypeMultiplier * participationMultiplier * scaledLevelFactor)
```

This is intentionally inspired by the level-scaling idea, not copied as an exact runtime contract.

## Current Element Rules

- The game has 12 Elements: Ember, Tide, Verdance, Stone, Gale, Spark, Frost, Metal, Shade, Radiant, Mind, Echo.
- Each Echoform has one or two Elements.
- Each Technique has exactly one Element.
- A normal Echoform can only equip Techniques matching one of its Elements.
- A fused form can use the Core and Mantle Elements, so fusion expands legal Techniques without creating off-element spam.
- Super-effective damage is 2.0x, not-very-effective damage is 0.5x.
- Dual-Element defenders can create 4.0x, 0.25x, or canceling 1.0x outcomes.
- Same-Element Affinity is 1.5x.
- Null is a story corruption state, not a thirteenth Element.
- The technique target is 450. Every technique needs its own animation ID and PvP-stable animation metadata.

## Missing Compared To Pokemon-Like Scope

Still to prepare:

- Full 300 Echoform detailed stat budgets.
- Full 300 Echoform Element assignments, including dual-Element forms and legendary exceptions.
- Learn tables for all 300 Echoforms that enforce the Element-lock rule.
- Final hand-balancing for all 450 Techniques: power, accuracy, category, Flux cost/gain, status chance, animation timing, and unlock level.
- Evolution/Ascension conditions for all evolving species.
- Held item catalog.
- Full NPC AI difficulty tiers.
- Storage/vault UI rules.
- Codex completion rewards.
- Weather and day/night encounter tables.
- Audio/music plan.
- Full map count and dungeon list.
- Trade protocol.
- Ranked validator.
- Full status-condition set with clear immunities, durations, cures, and PvP legality.
- Encounter methods beyond walking: water travel, fishing-equivalent, caves, overworld ambushes, swarms, static bosses, and legendary rituals.
- Breeding/nursery-equivalent decision: either omit it, replace it with Echo incubation, or keep it post-game only.
- Shiny/rare-variant system with odds, visual rules, and trading restrictions.
- Ability/Instinct catalog for all 300 species.
- Held-item interaction rules, including restrictions for ranked Matrix PvP.
- Battle formats beyond 1v1: double battles, tag battles, boss shields, raids, and arena challenge rules.
- Field progression tools that avoid HM clones but still unlock water travel, cliff travel, darkness, heavy obstacles, sealed doors, and signal puzzles.
- Trainer rematch system, level-scaling policy, money rewards, and anti-farming caps.
- Box/storage management UX, party swap rules, release rules, and favorite/locked creature protection.
- Move relearning, Technique Script economy, and limits on replacing learned Techniques.
- Endgame loop: master arenas, battle tower equivalent, legendary rematches, daily swarms, and Matrix seasons.
- Final art production beyond generated SVG placeholders: animated creature sheets, overworld tile animation sheets, attack VFX sprites, battle backgrounds, UI motion, and sound hooks.
