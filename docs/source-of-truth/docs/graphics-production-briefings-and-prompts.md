# Thot Fusion Graphics Production Briefings and Prompts

This file turns the visual bible into production-ready briefings and AI prompt seeds. It is intentionally focused on the first shippable art families instead of trying to prompt all 300 Echoforms at once.

## How To Use This File

Each asset family below contains:

- `Purpose`: what the asset is for in-game.
- `Must keep`: non-negotiable visual rules.
- `Avoid`: failure cases that create weak or unusable outputs.
- `Output target`: what kind of image or sheet should be produced.
- `Prompt seed`: a strong first-generation prompt.
- `Negative prompt`: what to suppress.

These prompts are not meant to be pasted blindly forever. They are baseline production prompts that should be iterated once good visual direction appears.

## Global Style Lock

Apply these rules to almost every generation:

- hybrid 2D monster RPG art direction
- modern handheld-scale readability
- darker Asterfall identity, not cheerful generic anime fantasy
- strong silhouette priority
- readable material contrast
- no flat SVG icon look
- no cheap retro imitation
- no washed-out default fantasy palette
- no over-rendered painterly mud for gameplay assets

Base quality line:

- overworld assets: clear, modular, readable, tile-aware
- battle assets: more dramatic, richer lighting, stronger anatomy
- portraits: premium layer, more illustrative, more expressive

## Prompt Rules By Asset Class

### 1. Overworld Style Board

Purpose:
Define the final world rendering language before tilesets are built.

Must keep:

- top-down or slight 3/4 top-down RPG readability
- dark coastal/relay fantasy tone
- wet stone, brass, weathered wood, signal glass, reeds, storm light
- higher quality than classic GBA monster RPGs while still tile-friendly

Avoid:

- fake 8-bit nostalgia
- flat vector mockup look
- open-world 3D realism
- generic JRPG town concept art that ignores tile logic

Output target:

- one concept sheet showing 3 to 5 environment slices
- no text baked into image
- intended for style approval, not final in-game tile use

Prompt seed:

```text
style board for a modern 2D monster-RPG overworld, hybrid pixel-art and HD-2D direction, top-down and slight three-quarter top-down camera references, dark coastal fantasy region called Asterfall, wet black stone streets, brass relay machines, glowing tide reflections, hanging lanterns, reeds, storm clouds, signal towers, modular town layout logic, readable tile-based exploration, strong atmosphere, handcrafted game environment concept sheet, cohesive color direction, premium handheld RPG quality
```

Negative prompt:

```text
cheap retro pixel filter, flat vector icons, mobile game asset slop, generic anime town, 3D realistic render, chibi toy proportions, blurry painterly mess, purple neon cyberpunk, empty white background
```

### 2. Battle Style Board

Purpose:
Set the quality bar for combat scenes, creature framing, lighting, and VFX density.

Must keep:

- creature remains readable during attack effects
- layered arena background
- impact-oriented VFX
- stronger contrast than overworld

Avoid:

- fighting-game UI mockups
- overstuffed cinematic explosions
- muddy dark scenes where creature silhouette disappears

Output target:

- one concept sheet with 3 battle scenes
- one starter, one midgame creature, one legendary-scale scene

Prompt seed:

```text
battle presentation style board for a modern 2D monster RPG, hybrid sprite and painted battle look, large readable creature sprites, layered arena backdrops, impact-driven elemental effects, clean combat staging, dramatic but controlled lighting, premium handheld RPG battle scenes, one ember creature scene, one tide or verdance creature scene, one legendary ritual battle scene, strong silhouette clarity, not nostalgic mockup, not generic mobile RPG
```

Negative prompt:

```text
fighting game HUD, MMO raid clutter, overexposed spell spam, tiny unreadable creatures, retro parody, simplistic flash animation, diagram arrows, card game layout
```

### 3. Echoform Style Board

Purpose:
Lock the anatomical and silhouette language for collectible creatures.

Must keep:

- collectible-creature appeal
- anatomy or fantasy anatomy with internal logic
- elemental traits integrated into body design
- three tiers visible: starter, route, legendary

Avoid:

- obvious Pokemon clones
- mascot-only design language
- symbols with eyes
- random horn and palette variants pretending to be new species

Output target:

- one sheet with 9 to 12 creature design explorations
- include side or rear hints if possible

Prompt seed:

```text
creature design style board for original collectible monsters called Echoforms, hybrid RPG art direction, strong silhouettes, believable fantasy anatomy, elemental traits integrated into body form, dark elegant world tone, designs for starter creatures, early route creatures, midgame predators, and legendary entities, premium creature concept art sheet, readable shapes, game-ready monster family design language, not derivative of existing monster franchises
```

Negative prompt:

```text
Pokemon clone, Digimon clone, mascot overload, rubber toy style, random spikes, flat symbols, cutesy mobile pet game, repetitive horn variants, generic fantasy beast collage
```

### 4. VFX Style Board

Purpose:
Define how the 12 elements move and hit.

Must keep:

- readable hit timing
- short, punchy shapes
- distinct motion language by element
- usable as game VFX reference

Avoid:

- abstract infographic arrows
- giant magic circles everywhere
- noisy particles without shape hierarchy

Output target:

- one sheet with 12 effect studies or grouped subsets

Prompt seed:

```text
elemental combat VFX style board for a 2D monster RPG, twelve distinct effect languages: ember, tide, verdance, stone, gale, spark, frost, metal, shade, radiant, mind, echo, each shown as a short impact or projectile study, clean readable game VFX, high contrast, premium 2D battle effects, not infographic, not UI icons, not generic fantasy spell spam
```

Negative prompt:

```text
flat vector arrows, random sparkles, over-rendered smoke, MMO spell clutter, soft blurry effects, logo marks, minimalist symbols, fake retro effect strips
```

## Region Briefings

### Namaris Harbor

Purpose:
First major city, must sell the whole world immediately.

Must keep:

- black wet stone
- harbor ropes and moorings
- tide pools
- storm lantern gold
- teal reflections
- first-lab feeling

Avoid:

- bright tropical port
- medieval market cliché
- generic anime fishing village

Prompt seed:

```text
Namaris Harbor, original monster-RPG city, dark stormy coastal hub, wet black stone streets, tide pools, rope docks, brass lanterns, teal reflected water light, small harbor lab, relay beacon technology mixed with old masonry, top-down and slight three-quarter game environment concept, highly readable modular RPG town, premium hybrid pixel-art and painted reference
```

Negative prompt:

```text
sunny tropical beach town, cartoon pirate village, steampunk overload, bright cheerful vacation port, realistic 3D render, empty plaza
```

### Glassreed Crossing

Purpose:
Early marsh biome with instability and atmosphere.

Must keep:

- reeds
- dark water
- boardwalks
- low fog
- faint signal glow
- dangerous but beautiful mood

Avoid:

- swamp horror cliché with no elegance
- bright jungle
- flat green mud with no structure

Prompt seed:

```text
Glassreed Crossing, marsh settlement in an original monster RPG, unstable boardwalk town above dark reflective water, tall reeds, low fog, muted green and black palette, small signal lamps, bioluminescent plants, elegant but dangerous marsh atmosphere, modular top-down RPG environment concept, handcrafted worldbuilding, high readability for exploration
```

### Myr Vault City

Purpose:
Mind/Echo region anchor and archive city benchmark.

Must keep:

- vertical archive architecture
- suspended bridges
- stone and metal stacks
- glyph light
- mind/echo signal identity

Avoid:

- generic wizard library
- sci-fi spaceship interior
- plain city with books pasted on top

Prompt seed:

```text
Myr Vault City, archive metropolis for an original monster RPG, vertical stone archives, suspended bridges, metallic library structures, glowing glyph elevators, memory and signal technology, mind and echo visual identity, top-down RPG city concept, premium hybrid 2D game environment, dense readable architecture, not fantasy cliché
```

## Core World Asset Briefings

### Base Ground Tileset

Purpose:
Core world walking surfaces shared across early zones.

Must keep:

- tileability
- material variation without noise overload
- dark fantasy tone
- readable collision boundaries

Output target:

- seamless tile sheet
- clean square presentation
- 32x32 or 48x48 production intent

Prompt seed:

```text
game asset sheet, seamless RPG ground tiles for a dark coastal fantasy monster game, wet stone, packed earth, worn path edges, clean top-down tile readability, modular handcrafted texture, premium 2D tileset concept, designed for 32x32 or 48x48 production, not flat icons
```

### Water Tileset

Purpose:
Tide identity is critical to the world.

Must keep:

- layered water motion
- readable shore edge
- darker reflective water, not bright tropical blue

Prompt seed:

```text
top-down RPG water tileset, dark reflective tidal water, teal highlights, believable shoreline edges, subtle wave motion design, modular handcrafted 2D game tiles, premium monster-RPG environment asset concept
```

### Grass Encounter Tileset

Purpose:
Wild encounter surfaces must be visually inviting and mechanically readable.

Must keep:

- clearly denser encounter grass
- contrast with normal ground
- natural edge transitions

Prompt seed:

```text
top-down RPG encounter grass tileset, dark verdant field grass with readable density change, natural edge transitions, premium 2D monster-RPG asset concept, modular tile sheet, not cartoon flat shapes
```

## Character Briefings

### Player Sprite Sheet

Purpose:
Main overworld avatar for walking, turning, and standing.

Must keep:

- readable at small scale
- neutral enough to fit all early environments
- stronger silhouette than generic RPG villager

Avoid:

- overdesigned coat clutter
- giant anime hair silhouette that breaks sprite readability

Prompt seed:

```text
player character sprite sheet concept for a modern 2D monster RPG, top-down and slight three-quarter sprite logic, readable walk cycle poses, dark fantasy explorer outfit, practical boots, layered travel clothing, subtle relay-tech details, premium sprite design reference, clean silhouette, built for 32x48 or 48x64 production
```

### Story Character Portrait Pack

Use this structure for `Dr. Ilya Voss`, `Mara Keir`, `Senn Vale`, and `Archivist Orun`.

Must keep:

- personality first
- same world and clothing language as Asterfall
- portrait should upscale perceived production value

Portrait prompt template:

```text
character portrait for an original monster RPG, {character_name}, {role_summary}, dark elegant fantasy science world, strong personality, practical layered clothing, subtle relay-tech details, premium illustrated game portrait, clean face readability, strong color control, not generic anime template
```

Examples:

- `Dr. Ilya Voss`: calm ethical fusion researcher, harbor lab mentor, practical and severe
- `Mara Keir`: talented rival binder, impatient, intense, athletic, morally evolving
- `Senn Vale`: explorer and mapmaker, warm, curious, field-worn gear
- `Archivist Orun`: old archive keeper, secretive, ritual-intellectual presence

## Echoform Production Briefings

### Starter Set

The first three complete creature families should define the quality bar.

#### Vulkid

Purpose:
Ember starter, aggressive but not villainous.

Must keep:

- salamander or ember-lizard logic
- compact body with explosive posture
- heat pressure motif, not generic dragon clone

Prompt seed:

```text
original starter monster concept, Vulkid, ember element, compact salamander-like creature with pressure-heat anatomy, volcanic vents along back, bright ember glands, aggressive but appealing posture, collectible creature silhouette, premium battle-sprite concept for a modern 2D monster RPG, not a dragon clone, not a Pokemon imitation
```

#### Reeva

Purpose:
Tide starter, defensive and fluid.

Must keep:

- river guardian logic
- elegant waterline curves
- calm but capable expression

Prompt seed:

```text
original starter monster concept, Reeva, tide element, river guardian creature with smooth aquatic anatomy, elegant fins or crest shapes, calm and resilient expression, collectible creature silhouette, premium battle-sprite concept for a modern 2D monster RPG, not fish mascot, not Pokemon imitation
```

#### Mosslyn

Purpose:
Verdance starter, adaptive and clever.

Must keep:

- fox-deer or woodland hybrid logic
- growth textures integrated into body
- agile grounded silhouette

Prompt seed:

```text
original starter monster concept, Mosslyn, verdance element, woodland fox-deer hybrid creature with living moss and root textures, agile body plan, clever expression, premium collectible creature design for a modern 2D monster RPG, strong silhouette, not generic grass mascot, not Pokemon imitation
```

### Early Route Echoform Template

Purpose:
Generate the first 10 to 15 common creatures that define exploration variety.

Template:

```text
original collectible creature concept for a modern 2D monster RPG, {echoform_name}, {element_or_elements}, early-route species, clear silhouette, simple but memorable anatomy, strong collectible appeal, suitable for overworld sprite and battle sprite adaptation, world tone is dark elegant fantasy science, not derivative of existing monster franchises
```

## VFX Production Briefings

### Ember Base Pack

Must keep:

- ignition
- ember spray
- pressure burst
- quick impact flash

Prompt seed:

```text
2D battle VFX concept sheet for ember element attacks in a monster RPG, ignition arcs, ember bursts, ash trails, pressure flashes, readable short combat timing, premium game VFX, high contrast, not infographic, not logo design
```

### Tide Base Pack

Prompt seed:

```text
2D battle VFX concept sheet for tide element attacks in a monster RPG, surge ribbons, water lashes, splash curtains, droplets, rippling impact rings, readable short combat timing, premium game VFX, not generic fantasy spell art
```

### Verdance Base Pack

Prompt seed:

```text
2D battle VFX concept sheet for verdance element attacks in a monster RPG, root bursts, leaf arcs, spore sprays, growth surges, grasping vines, readable game combat effects, premium 2D VFX, not decorative botanical wallpaper
```

## Battle Background Briefings

### Coast Battle Background

Purpose:
Standard early battle scene near Namaris.

Must keep:

- black stone foreground
- water reflection
- distant harbor structures
- weather mood

Prompt seed:

```text
2D monster RPG battle background, stormy coast arena near a dark harbor city, wet black stone foreground, teal water reflections, distant relay towers and dock silhouettes, dramatic but readable combat staging, premium hybrid 2D game background
```

### Marsh Battle Background

Prompt seed:

```text
2D monster RPG battle background, dark marsh combat scene, reflective water patches, reeds, fog, weak signal lamps, unstable boardwalk remains, eerie but elegant atmosphere, readable combat foreground, premium hybrid 2D game background
```

## Recommended Prompt Workflow

Use this order:

1. generate one style board per category
2. approve direction
3. generate one premium target asset
4. lock silhouette and palette rules
5. only then generate production variants

Do not:

- generate 300 creatures before approving 10
- generate tiles before locking material language
- generate 450 attack effects before locking 12 base element packs

## Immediate Next Batch

The most useful next prompts to run are:

1. overworld style board
2. battle style board
3. echoform style board
4. Namaris Harbor concept
5. Glassreed Crossing concept
6. Myr Vault City concept
7. Vulkid premium concept
8. Reeva premium concept
9. Mosslyn premium concept
10. Ember VFX base pack
11. Tide VFX base pack
12. Verdance VFX base pack

That set is enough to tell whether the new art direction is genuinely better than the discarded placeholder generation.
