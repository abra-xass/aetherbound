package com.aetherbound.game.dialogue

import android.content.Context
import kotlin.random.Random

/**
 * Single entry-point for turning a Tuxemon-style msgid (e.g. `"okaythen"`,
 * `"cotton_breeder0"`, `"37707_villager_female_1"`) into player-visible
 * Aetherbound dialogue text.
 *
 * Resolution order — first match wins:
 *
 *   1. **Literal lookup** — msgid in the loaded ContentPack literals map.
 *      Used for hand-translated quest/lore/mechanic content.
 *
 *   2. **Sprite-archetype grammar** — if [spriteArchetype] is given (e.g.
 *      `"barmaid"`, `"alchemist"`, `"aviator"`) and its grammar exists,
 *      Tracery generates a line.
 *
 *   3. **Category fallback grammar** — any msgid that contains "sign"
 *      routes to the world_sign grammar; "challenger"/"trainer" → trainer_pre;
 *      everything else → npc_flavor.
 *
 *   4. **Last-resort** — a humble bootstrap line so the player never sees
 *     a raw msgid. Useful while the pack is being populated.
 *
 * Every output is run through [TextNormalizer] so no "Tuxemon" leaks even
 * if a content-author missed a substitution.
 *
 * The resolver is a long-lived singleton; load the pack once at app boot.
 */
class DialogueResolver private constructor(
    private val pack: ContentPack,
    seed: Long = System.currentTimeMillis(),
) {
    private val rng = Random(seed)

    /**
     * Resolve a msgid into a final, player-visible string.
     *
     * @param msgid the upstream Tuxemon dialogue id (or empty)
     * @param spriteArchetype optional sprite folder name from the NPC
     *        (`"barmaid"`, `"alchemist"`, …) — improves grammar selection
     * @param fallback shown when no grammar/literal matches at all
     */
    fun resolve(
        msgid: String,
        spriteArchetype: String? = null,
        fallback: String = "Sie schweigt einen Moment, dann nickt sie höflich.",
    ): String {
        // 1) Literal lookup (handles KEEP_STRUCTURE, HIGH_REUSE, QUEST_LORE, …)
        pack.literals[msgid]?.let { return TextNormalizer.scrub(it) }

        // 2) Sprite-archetype grammar (if NPC sprite suggests a profession)
        if (spriteArchetype != null) {
            val archetypeKey = normalizeArchetype(spriteArchetype)
            pack.grammars[archetypeKey]?.let { rules ->
                val tracery = Tracery(rules, rng)
                return TextNormalizer.scrub(tracery.expand("origin"))
            }
        }

        // 3) Category fallback by msgid pattern
        val category = categoryForMsgid(msgid)
        pack.grammars[category]?.let { rules ->
            val tracery = Tracery(rules, rng)
            return TextNormalizer.scrub(tracery.expand("origin"))
        }

        // 4) Bootstrap line so player never sees the raw msgid
        return TextNormalizer.scrub(fallback)
    }

    private fun categoryForMsgid(msgid: String): String {
        val m = msgid.lowercase()
        return when {
            "sign" in m || "notice" in m -> "world_sign"
            "challenger" in m || "trainer" in m || "battle_intro" in m -> "trainer_pre"
            else -> "npc_flavor"
        }
    }

    /**
     * Tuxemon's sprite folder names include colour variants
     * (`"barmaid_alt1"`, `"barmaid_blonde"`, …). Strip them down to the
     * base archetype so a single grammar covers all variants.
     */
    private fun normalizeArchetype(sprite: String): String {
        val s = sprite.lowercase().substringBefore('/')
        // Common Tuxemon variants: alt1/alt2/alt3, colour suffixes
        val stripped = s
            .replace(Regex("""_alt\d+"""), "")
            .replace(Regex("""_(black|brown|blue|green|red|yellow|violet|rose|fiery|blonde|lapi)$"""), "")
        return stripped
    }

    companion object {

        @Volatile
        private var instance: DialogueResolver? = null

        /** Get the singleton resolver — lazily loads the pack on first call. */
        fun get(ctx: Context): DialogueResolver {
            instance?.let { return it }
            synchronized(this) {
                instance?.let { return it }
                val pack = ContentPack.load(ctx.applicationContext)
                val r = DialogueResolver(pack)
                instance = r
                return r
            }
        }

        /** Test-only constructor that bypasses asset loading. */
        internal fun forTesting(pack: ContentPack, seed: Long = 0L) =
            DialogueResolver(pack, seed)
    }
}
