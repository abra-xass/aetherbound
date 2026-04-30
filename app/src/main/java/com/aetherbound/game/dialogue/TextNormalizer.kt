package com.aetherbound.game.dialogue

/**
 * Final-pass text scrubber. Every line of dialogue that leaves the
 * [DialogueResolver] is run through here so we never leak Tuxemon-brand
 * vocabulary into the player-visible UI, even if a content-author or
 * upstream-translation slipped through.
 *
 * Order matters: longer/more-specific patterns run before shorter ones
 * (e.g. "Tuxemon Center" → "Aether-Zentrum" before bare "Tuxemon" → "Echoform").
 *
 * Substitutions are case-aware: we keep capitalization style on the
 * replacement so "TUXEMON" doesn't become "Echoform" mid-sentence.
 */
object TextNormalizer {

    /**
     * (regex, replacement-fn) pairs. The replacement-fn receives the matched
     * substring so it can preserve casing. Run top-to-bottom.
     */
    private val rules: List<Pair<Regex, (String) -> String>> = listOf(
        // Compound nouns first (longest wins)
        Regex("""\bTuxemon\s+Center\b""")  to { _ -> "Aether-Zentrum" },
        Regex("""\bTuxecenter\b""")        to { _ -> "Aether-Zentrum" },
        Regex("""\bTuxepedia\b""")         to { _ -> "Aether-Kompendium" },
        Regex("""\bTuxeball\b""")          to { _ -> "Aether-Prisma" },
        Regex("""\bTuxeballs?\b""")        to { _ -> "Aether-Prismen" },

        // Bare word — case-aware
        Regex("""\bTUXEMON\b""")           to { _ -> "ECHOFORM" },
        Regex("""\bTuxemon\b""")           to { _ -> "Echoform" },
        Regex("""\btuxemon\b""")           to { _ -> "echoform" },

        // Plural forms (English upstream often pluralizes "Tuxemon" as itself,
        // so handle the bare-word rule above; explicit "Tuxemons" rare but possible)
        Regex("""\bTuxemons\b""")          to { _ -> "Echoforms" },
        Regex("""\btuxemons\b""")          to { _ -> "echoforms" },
    )

    /**
     * Run [text] through every rule, returning the scrubbed string.
     * No-op for null/blank inputs.
     */
    fun scrub(text: String?): String {
        if (text.isNullOrBlank()) return text ?: ""
        var s: String = text
        for ((re, repl) in rules) {
            s = re.replace(s) { m -> repl(m.value) }
        }
        return s
    }
}
