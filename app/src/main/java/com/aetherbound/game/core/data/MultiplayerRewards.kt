package com.aetherbound.game.core.data

import com.aetherbound.game.core.EchoformInstance

/**
 * Computes monetary + XP rewards for a finished multiplayer match,
 * applying the anti-exploit guardrails from `docs/economy-design.md`.
 *
 * Two flavours, picked at lobby time:
 *
 *   - **Ranked** — full money + XP exchange, counts toward stats
 *   - **Casual** — no money, no XP, only "seen" Bestiary marks (still
 *     anti-exploit-safe because no resources move)
 */
object MultiplayerRewards {

    enum class Mode { RANKED, CASUAL }

    data class Result(
        val winnerMoneyDelta: Int,
        val loserMoneyDelta: Int,        // negative
        val winnerXpToActive: Int,        // applied to the most-active winner mon
        val isPotPaid: Boolean,
        val ranked: Boolean,
        val achievementHints: List<String>,
    )

    /**
     * Flat pot floor — every match has a 5,000 stake. Wealthier players
     * pay proportionally more (5% of wallet) but never less than 5,000.
     * No upper cap any more — late-game whales can lose big, which keeps
     * pots meaningful even when wallets balloon.
     *
     * Pairs with [MAX_DEBT]: a broke player still owes 5,000, but their
     * wallet floors at −5,000 instead of running into infinite negatives.
     */
    fun computePotAnte(loserMoney: Int): Int {
        val proportional = (loserMoney * 0.05).toInt()
        return maxOf(MIN_POT, proportional)
    }

    /** Minimum match stake — every win/loss moves at least this much. */
    const val MIN_POT = 5_000

    /** Player's wallet can go this far below zero before debt clamps. */
    const val MAX_DEBT = -5_000

    /**
     * Apply a pot to a wallet, respecting the [MAX_DEBT] floor.
     *
     * Returns (newBalance, paidAmount). The paid amount is what actually
     * left the loser's wallet — equal to [potAmount] when they could
     * afford it, less when the debt-floor clipped them.
     */
    fun applyLoss(currentMoney: Int, potAmount: Int): Pair<Int, Int> {
        val targetBalance = currentMoney - potAmount
        return if (targetBalance >= MAX_DEBT) {
            targetBalance to potAmount
        } else {
            // Floor at MAX_DEBT — only pay what brings us to the floor.
            val actuallyPaid = currentMoney - MAX_DEBT
            MAX_DEBT to actuallyPaid.coerceAtLeast(0)
        }
    }

    /**
     * Build the [Result] for a finished match. Caller applies the deltas
     * to its `Inventory.money` + active member's XP.
     *
     * @param winnerMVP the winner-side Echoform that scored the most KOs
     *                  in the match (or last alive, as tie-break)
     * @param loserActive the loser-side mon at the moment of last KO —
     *                   feeds the level-diff formula and base-stat-sum
     * @param winnerMoney current wallet of the winner
     * @param loserMoney current wallet of the loser
     * @param mode RANKED vs CASUAL
     * @param matchTurns number of turns resolved — < 3 disqualifies the
     *                   match from any rewards (spam-forfeit guard)
     */
    fun compute(
        winnerMVP: EchoformInstance,
        loserActive: EchoformInstance,
        winnerMoney: Int,
        loserMoney: Int,
        mode: Mode,
        matchTurns: Int,
    ): Result {
        val achievements = mutableListOf<String>()
        // Guardrails: too-short matches don't pay out.
        if (matchTurns < 3) {
            return Result(
                winnerMoneyDelta = 0,
                loserMoneyDelta = 0,
                winnerXpToActive = 0,
                isPotPaid = false,
                ranked = false,
                achievementHints = listOf("mp_no_reward_short_match"),
            )
        }

        if (mode == Mode.CASUAL) {
            return Result(
                winnerMoneyDelta = 0,
                loserMoneyDelta = 0,
                winnerXpToActive = 0,
                isPotPaid = false,
                ranked = false,
                achievementHints = emptyList(),
            )
        }

        // Ranked: pot + XP.
        val pot = computePotAnte(loserMoney)
        val baseStatSum = with(loserActive.species.baseStats) {
            vigor + force + focus + guard + ward + tempo
        }
        val xp = ExperienceEngine.xpFromVictory(
            winnerLevel = winnerMVP.level,
            loserLevel = loserActive.level,
            loserBaseStatSum = baseStatSum,
            sourceMode = ExperienceEngine.SourceMode.MULTIPLAYER,
        )

        // Big-upset achievement: winning when at least 10 levels lower.
        if (loserActive.level - winnerMVP.level >= 10) {
            achievements += "mp_upset_${loserActive.level - winnerMVP.level}"
        }

        return Result(
            winnerMoneyDelta = pot,
            loserMoneyDelta = -pot,
            winnerXpToActive = xp,
            isPotPaid = true,
            ranked = true,
            achievementHints = achievements,
        )
    }
}
