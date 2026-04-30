# Aetherbound Content Contracts

These contracts must pass before moving prep content into app assets.

## Roster

- `content/echoforms_300.csv` has exactly 300 rows.
- Echoform IDs are unique.
- Echoform names are unique.
- IDs cover E001 through E300 without gaps.
- Exactly 15 Echoforms are legendary or very legendary.
- Exactly 3 Echoforms are very legendary.
- Legendary and very legendary Echoforms are marked `npc_legal=false`.
- No Echoform has a base stat below 25.
- Every Echoform has one or two of the 12 legal Elements.
- `content/echoform_learnsets_300.csv` has exactly 3,600 rows, 12 level-up rows per Echoform.

## Cities

- `content/cities.json` has exactly 30 cities.
- Every city has a leader.
- Every city has an Arena/Sanctum name.
- Every city has a level band and signature Aspect.
- `content/world_movement.json` has exactly 30 city Seal rewards.
- The visible reward term is `Seal`; `badge` is reserved as a forbidden visible term.

## NPC Binders

- `content/npc_binders_2000.csv` has at least 2,000 rows.
- `content/npc_binders_2000.csv` is a full opponent pool, not 2,000 visible world placements.
- Exactly 700 NPCs are `fixed_world_battle`.
- Exactly 200 NPCs are `optional_rematch_postgame`.
- Exactly 1,100 NPCs are `simulation_matrix_pool`.
- All visible battle starts default to `talk_to_battle`.
- Every NPC has a city, archetype, level policy, team theme, reward, and dialogue key.
- `content/npc_teams_2000.csv` has exactly 2,000 teams.
- NPC teams must never include legendary or very legendary Echoforms.

## Items

- `content/items_180.csv` has exactly 180 rows.
- Every item has a category, price, currency, effect, target, and unlock city.
- Crown/legendary capture items must not be normal purchasable Marks-only items in final production data.

## Techniques

- `content/techniques_450.csv` has exactly 450 rows.
- Every technique has an Aspect, category, power, accuracy, Flux delta, priority, effect, animation ID, and unlock level.
- Every technique animation ID is unique.
- `content/animations/technique_animation_map.csv` has exactly 450 rows.
- `content/animations/attack_animation_manifest.json` has exactly 450 animation entries.
- Each normal Echoform may only learn techniques matching one of its Elements; fused forms may use Core and Mantle Elements.
- Non-damaging categories can have power 0.

## Graphics

- SVG sheets exist for world tiles, starter creatures, items/UI, NPC archetypes, attack animations, and battle backgrounds.
- `graphics/echoform_sprite_atlas.svg` contains one ID-keyed placeholder sprite for every Echoform from E001 through E300.
- `graphics/item_catalog_atlas.svg` contains one ID-keyed icon for every item from IT001 through IT180.
- `graphics/npc_binder_sprite_atlas.svg` contains 240 reusable NPC overworld variants.
- `graphics/world_map_overview.svg` contains all 30 city markers.
- SVG IDs match future animation/content manifests where possible.
- `content/animations/technique_animation_map.csv` maps every technique to an animation primitive.
- `content/animations/creature_animation_manifest.json` defines the stable creature state list for all 300 Echoforms.
- `content/animations/world_animation_manifest.json` defines animated world tiles and NPC animation states.

## Mechanics

- Stats use Base + Potential + Training Points + Level + Temperament.
- Potential range is 0-31.
- Training Points cap at 252 per stat and 510 total.
- Level range is 1-100.
- Party size is 6.
- PvP uses Matrix IDs and deterministic commit/reveal events.
- Player world movement uses tile collision with smooth interpolation.
- Signal Wheel and Loom Jump are traversal unlocks, not starting abilities.
