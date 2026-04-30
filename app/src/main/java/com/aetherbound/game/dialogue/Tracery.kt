package com.aetherbound.game.dialogue

import kotlin.random.Random

/**
 * Minimal Tracery-style grammar engine — Kotlin port of the public
 * [Tracery](https://tracery.io) algorithm by Kate Compton, scoped down
 * to the features Aetherbound needs.
 *
 * **Supported syntax:**
 *
 *   - **Symbol references**: `#greeting#` → expand by picking a random rule
 *     from the symbol's rule-list and recursively expanding it.
 *   - **Inline alternation**: `#weather|mood#` picks one of the listed
 *     symbols at random *before* expansion.
 *   - **Variables (push/pop)**: `[hero:#hero_name#]` evaluates the rhs
 *     once and binds it to `hero` for the rest of this expansion. Useful
 *     for keeping a name consistent across multiple references in one
 *     sentence (e.g. *"#hero# said hello. #hero# smiled."* should reuse
 *     the same name on both occurrences).
 *   - **Modifiers**: `#word.capitalize#`, `#word.upper#`, `#word.lower#`
 *     transform the expanded result. Chainable: `#word.capitalize.s#`.
 *
 * **Not supported (intentionally):**
 *
 *   - Action grammars / pushPop with rule-list rhs (`[hero:name1,name2]`)
 *     — adds parsing complexity for marginal benefit. Use a separate
 *     symbol with multiple rules instead.
 *   - JS-engine-style modifiers (`.a`, `.ed`, etc.) — we have no English
 *     pluralization needs; German content is hand-formatted.
 *
 * Output is always passed through [TextNormalizer] at the end so no
 * Tuxemon-brand vocabulary leaks to the UI — the engine itself does
 * NOT scrub, callers do (or use [DialogueResolver]).
 */
class Tracery(
    private val rules: Map<String, List<String>>,
    private val rng: Random = Random.Default,
) {

    /**
     * Expand [start] — either a literal template like `"#greeting# #obs#"`
     * or a bare symbol name like `"villager.adult.female"`. The latter
     * is automatically wrapped in `#…#` for convenience.
     */
    fun expand(start: String): String {
        val template = if (start.contains('#')) start else "#$start#"
        return Expander(rules, rng).expand(template, mutableMapOf())
    }

    /** Convenience: expand many times — useful for tests / dumps. */
    fun expandN(start: String, n: Int): List<String> = List(n) { expand(start) }

    private class Expander(
        val rules: Map<String, List<String>>,
        val rng: Random,
    ) {
        // Recursion guard — stops broken grammars from blowing the stack
        private var depth = 0
        private val maxDepth = 32

        fun expand(template: String, scope: MutableMap<String, String>): String {
            depth++
            if (depth > maxDepth) {
                depth--
                return "[grammar-depth-exceeded]"
            }
            val out = StringBuilder()
            var i = 0
            while (i < template.length) {
                val c = template[i]
                when (c) {
                    '\\' -> {
                        // Escape next char (so authors can write literal '#' or '[')
                        if (i + 1 < template.length) {
                            out.append(template[i + 1])
                            i += 2
                        } else { out.append(c); i++ }
                    }
                    '[' -> {
                        // Variable bind: [name:#rhs#]
                        val close = findMatching(template, i, '[', ']')
                        if (close < 0) { out.append(c); i++; continue }
                        val inner = template.substring(i + 1, close)
                        val sep = inner.indexOf(':')
                        if (sep > 0) {
                            val varName = inner.substring(0, sep).trim()
                            val rhs = inner.substring(sep + 1)
                            scope[varName] = expand(rhs, scope)
                        }
                        i = close + 1
                    }
                    '#' -> {
                        // Symbol reference: #name# or #name.modifier#
                        val close = template.indexOf('#', i + 1)
                        if (close < 0) { out.append(c); i++; continue }
                        val token = template.substring(i + 1, close)
                        out.append(expandToken(token, scope))
                        i = close + 1
                    }
                    else -> { out.append(c); i++ }
                }
            }
            depth--
            return out.toString()
        }

        private fun expandToken(token: String, scope: MutableMap<String, String>): String {
            // Modifier chain: split on '.'
            val parts = token.split('.')
            val sym = parts[0]
            val mods = parts.drop(1)

            // Inline alternation: name|other
            val candidates = sym.split('|').map { it.trim() }.filter { it.isNotEmpty() }
            val chosen = candidates.random(rng)

            // Resolve: scope first (variable), then rules (symbol)
            val raw = scope[chosen]
                ?: rules[chosen]?.let { it[rng.nextInt(it.size)] }
                ?: return "{$chosen?}"

            val expanded = expand(raw, scope)
            return mods.fold(expanded, ::applyModifier)
        }

        private fun applyModifier(s: String, mod: String): String = when (mod) {
            "capitalize" -> s.replaceFirstChar { it.uppercase() }
            "upper", "uppercase" -> s.uppercase()
            "lower", "lowercase" -> s.lowercase()
            else -> s    // unknown modifier: pass-through (don't error in production)
        }

        private fun findMatching(s: String, openIdx: Int, open: Char, close: Char): Int {
            var depth = 0
            for (j in openIdx until s.length) {
                when (s[j]) {
                    open -> depth++
                    close -> { depth--; if (depth == 0) return j }
                }
            }
            return -1
        }
    }

    companion object {
        /** Merge multiple grammar maps into one — later wins on duplicate keys. */
        fun merge(vararg grammars: Map<String, List<String>>): Map<String, List<String>> {
            val out = mutableMapOf<String, List<String>>()
            for (g in grammars) for ((k, v) in g) out[k] = v
            return out
        }
    }
}
