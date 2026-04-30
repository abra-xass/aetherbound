package com.aetherbound.game.core.data

import android.content.Context
import com.aetherbound.game.core.EchoformInstance
import com.aetherbound.game.core.Technique
import kotlin.random.Random

/**
 * NPC trainer battle definition. A trainer carries a fixed team of
 * Echoforms (built via [TuxemonBattleSetup.build]) and a money/XP reward
 * for defeat. AI behaviour is delegated to [TrainerAI].
 */
data class TrainerSpec(
    val id: String,
    val displayName: String,
    val sprite: String,
    /** Fixed team — slug + level pairs, max 6. */
    val team: List<TeamMember>,
    val moneyReward: Int = 100,
    /** Pre-battle line shown above the trainer sprite. */
    val intro: String = "Let's battle!",
    /** Post-battle line on win/loss. */
    val outroVictory: String = "I lost!",
    val outroDefeat: String = "Easy win.",
)

data class TeamMember(
    val slug: String,
    val level: Int,
    val nickname: String? = null,
)

/**
 * Materialises a [TrainerSpec] into 6 [EchoformInstance]s — call once at
 * battle start.
 */
fun TrainerSpec.buildTeam(ctx: Context): List<EchoformInstance> {
    return team.mapNotNull { TuxemonBattleSetup.build(ctx, it.slug, it.level) }
}

/**
 * Decision-making for trainer-controlled Echoforms.
 *
 * Three difficulty tiers:
 *   - WILD:   pure random move pick (used for wild encounters too)
 *   - GRUNT:  prefers super-effective moves, otherwise random
 *   - ACE:    super-effective + considers HP threshold for finisher prioritisation
 */
object TrainerAI {

    enum class Difficulty { WILD, GRUNT, ACE }

    fun pickMove(
        attacker: EchoformInstance,
        defender: EchoformInstance,
        difficulty: Difficulty,
        rng: Random,
    ): Technique? {
        if (attacker.techniques.isEmpty()) return null
        if (difficulty == Difficulty.WILD) return attacker.techniques.random(rng)

        val scored = attacker.techniques.map { tech -> tech to scoreMove(tech, defender, difficulty) }
        val best = scored.maxByOrNull { it.second } ?: return null
        // ACE picks the best deterministically; GRUNT adds a 25% randomness fall-back.
        return if (difficulty == Difficulty.ACE || rng.nextInt(100) < 75) best.first
        else attacker.techniques.random(rng)
    }

    private fun scoreMove(
        tech: Technique,
        defender: EchoformInstance,
        difficulty: Difficulty,
    ): Double {
        val typeMult = com.aetherbound.game.core.AspectAffinity.multiplier(
            attack = tech.aspect,
            defenderPrimary = defender.species.primaryAspect,
            defenderSecondary = defender.species.secondaryAspect,
        )
        val rawDamage = tech.power * typeMult
        if (difficulty == Difficulty.ACE) {
            // Bonus when this move would finish the defender.
            val finisher = if (rawDamage >= defender.currentVigor) 100.0 else 0.0
            return rawDamage + finisher
        }
        return rawDamage
    }
}

/**
 * In-memory directory of Aetherbound trainers. Populated at game-start from
 * a JSON file that we can hand-author or extract from Tuxemon NPC YAMLs.
 *
 * Pilot: ships with two trainers so the encounter system has someone to
 * spawn against. Add more by extending [Pilot].
 */
object TrainerRegistry {
    private val trainers: MutableMap<String, TrainerSpec> = mutableMapOf()

    fun register(spec: TrainerSpec) { trainers[spec.id] = spec }
    fun get(id: String): TrainerSpec? = trainers[id]
    fun all(): Collection<TrainerSpec> = trainers.values
    fun count(): Int = trainers.size

    object Pilot {
        val ROUTE1_HIKER = TrainerSpec(
            id = "route1_hiker",
            displayName = "Hiker Bert",
            sprite = "game/characters/tuxemon/hiker/walk_south_0.png",
            team = listOf(
                TeamMember("rockitten", 8),
                TeamMember("agnidon", 10),
            ),
            moneyReward = 240,
            intro = "Mountains forge strength!",
            outroVictory = "I trained the wrong rocks.",
            outroDefeat = "Granite never bends.",
        )

        val PORT_FISHER = TrainerSpec(
            id = "port_fisher",
            displayName = "Fisher Mira",
            sprite = "game/characters/tuxemon/fisherman/walk_south_0.png",
            team = listOf(
                TeamMember("nudimind", 9),
                TeamMember("loiter", 11),
            ),
            moneyReward = 280,
            intro = "I caught these myself.",
            outroVictory = "Net came back empty today.",
            outroDefeat = "Hooked you good.",
        )

        fun installAll() {
            register(ROUTE1_HIKER)
            register(PORT_FISHER)
        }
    }
}
