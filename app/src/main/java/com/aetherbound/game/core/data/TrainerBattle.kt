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

        val FOREST_RANGER = TrainerSpec(
            id = "forest_ranger",
            displayName = "Ranger Voss",
            sprite = "game/characters/tuxemon/adventurer_green/walk_south_0.png",
            team = listOf(
                TeamMember("eyenemy", 12),
                TeamMember("bumbleboar", 14),
                TeamMember("aardart", 13),
            ),
            moneyReward = 380,
            intro = "These woods listen back. Hear them?",
            outroVictory = "The grove rests, for now.",
            outroDefeat = "Roots remember the wrong trail.",
        )

        val DESERT_NOMAD = TrainerSpec(
            id = "desert_nomad",
            displayName = "Nomad Iska",
            sprite = "game/characters/tuxemon/adventurer_yellow/walk_south_0.png",
            team = listOf(
                TeamMember("agnidon", 16),
                TeamMember("dunsparrow", 17),
                TeamMember("vivipere", 18),
            ),
            moneyReward = 540,
            intro = "Sand teaches patience. Show me yours.",
            outroVictory = "The dunes fold over my pride.",
            outroDefeat = "Some lessons sting like the noon sun.",
        )

        val CAVE_HERMIT = TrainerSpec(
            id = "cave_hermit",
            displayName = "Hermit Solas",
            sprite = "game/characters/tuxemon/alchemist/walk_south_0.png",
            team = listOf(
                TeamMember("sumchon", 22),
                TeamMember("nudimind", 22),
                TeamMember("anu", 24),
                TeamMember("rockitten", 23),
            ),
            moneyReward = 720,
            intro = "Down here, light is a luxury. So is mercy.",
            outroVictory = "The dark welcomes you. Rest a while.",
            outroDefeat = "Echoes always return.",
        )

        val GYM_LEADER = TrainerSpec(
            id = "gym_leader_aether",
            displayName = "Adept Lumin",
            sprite = "game/characters/tuxemon/adventurer/walk_south_0.png",
            team = listOf(
                TeamMember("agnidon", 28),
                TeamMember("nudimind", 30),
                TeamMember("sumchon", 30),
                TeamMember("memnomnom", 32),
                TeamMember("vivipere", 32),
                TeamMember("eyenemy", 34),
            ),
            moneyReward = 1500,
            intro = "Welcome, challenger. Show me the bond you've forged.",
            outroVictory = "You've earned the Aether-Sigil. The path opens.",
            outroDefeat = "Return when your echoes ring true.",
        )

        fun installAll() {
            register(ROUTE1_HIKER)
            register(PORT_FISHER)
            register(FOREST_RANGER)
            register(DESERT_NOMAD)
            register(CAVE_HERMIT)
            register(GYM_LEADER)
        }
    }
}
