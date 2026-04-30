# Aetherbound Import Plan

## Purpose

This plan turns the preparation pack into app code without mixing large game systems into existing chat/wallet code too early.

## Target Runtime Structure

```text
app/src/main/java/com/thot/messenger/game/
  core/
    GameModels.kt
    ExperienceSystem.kt
    FusionSystem.kt
    BattleSystem.kt
  content/
    GameContentRepository.kt
    JsonGameContentRepository.kt
  matrix/
    MatrixGameProtocol.kt
    MatrixGameTransport.kt
  ui/
    GameHomeScreen.kt
    OverworldScreen.kt
    BattleScreen.kt
    FusionLabScreen.kt
```

## Import Order

1. Import `engine/GameModels.kt`.
2. Import `engine/ExperienceSystem.kt` and add unit tests for XP curves.
3. Import `engine/FusionSystem.kt` and add unit tests for compatibility and stat blending.
4. Import `engine/BattleSystem.kt` and add golden battle fixtures.
5. Import `content/*.json` into app assets.
6. Build a content loader that validates 300 Echoform IDs, 30 cities, and item/category references.
7. Add a debug-only content browser before adding gameplay screens.
8. Add local save and player profile creation.
9. Add overworld renderer and first maps.
10. Add Matrix game event transport after local battles are deterministic.

## First Playable Build Scope

- All 300 Echoform IDs/names exist in content.
- Encounter-enabled subset for first maps: E001-E020 plus three starters.
- Maps: Namaris Harbor, Harbor Road, Glassreed Marsh, Copperleaf Terrace.
- Battles: wild, offline Binder, first Arena leader.
- Capture: Prisms and Attunement outcomes.
- Items: early healing, status cures, basic Prisms.
- Multiplayer: direct Matrix-ID challenge protocol stubs and deterministic local action logs.
