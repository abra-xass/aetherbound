package com.thot.fusion.prep.engine

object MatrixGameEventTypes {
    const val CHALLENGE = "com.thot.game.challenge.v1"
    const val CHALLENGE_RESPONSE = "com.thot.game.challenge_response.v1"
    const val MATCH_INIT = "com.thot.game.match_init.v1"
    const val TEAM_COMMIT = "com.thot.game.team_commit.v1"
    const val TEAM_REVEAL = "com.thot.game.team_reveal.v1"
    const val TURN_COMMIT = "com.thot.game.turn_commit.v1"
    const val TURN_REVEAL = "com.thot.game.turn_reveal.v1"
    const val TURN_RESULT = "com.thot.game.turn_result.v1"
    const val MATCH_END = "com.thot.game.match_end.v1"
    const val DISPUTE = "com.thot.game.dispute.v1"
}

data class MatrixChallengeDraft(
    val challengeId: String,
    val fromUserId: String,
    val toUserId: String,
    val mode: String,
    val rulesetId: String,
    val createdAtMillis: Long,
    val expiresAtMillis: Long,
)

data class TurnCommitDraft(
    val matchId: String,
    val turn: Int,
    val side: String,
    val commitmentSha256: String,
)

data class TurnRevealDraft(
    val matchId: String,
    val turn: Int,
    val side: String,
    val actionJsonCanonical: String,
    val nonce: String,
)
