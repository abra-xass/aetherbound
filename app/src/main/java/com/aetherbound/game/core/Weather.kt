package com.aetherbound.game.core

/**
 * Current overworld weather. Drives spawn-condition filtering so certain
 * Echoforms only appear under specific weather (e.g. Aether-Otter only
 * during RAIN, Storm-Sprites only during STORM).
 *
 * In future: the [Weather] value gets overridden daily by the cloud-AI
 * Daily-Pack (real Swiss meteo data, fed via GitHub Actions). Until that
 * lands, the world stays on [CLEAR] permanently.
 */
enum class Weather(val displayDe: String, val emoji: String, val tracerySlot: String) {
    CLEAR("klar", "☀", "klar"),
    CLOUDY("bewölkt", "☁", "bewölkt"),
    RAIN("Regen", "🌧", "regnerisch"),
    STORM("Sturm", "⛈", "stürmisch"),
    SNOW("Schnee", "❄", "verschneit"),
    FOG("Nebel", "🌫", "neblig"),
    /** Schweizer Spezial-Wetter — warmer Fallwind aus den Alpen, Echoforms unruhig. */
    FOEHN("Föhn", "🌬", "föhnig"),
    HEAT("Sommerhitze", "🔥", "heiß"),
    ;

    companion object {
        fun fromName(name: String?): Weather =
            values().firstOrNull { it.name.equals(name, ignoreCase = true) } ?: CLEAR
    }
}
