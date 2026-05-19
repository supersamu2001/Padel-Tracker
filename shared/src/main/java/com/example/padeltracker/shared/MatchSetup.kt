package com.example.padeltracker.shared

/**
 * Shared data contract for initializing a match from the phone to the watch.
 */
data class MatchSetup(
    val matchId: String,
    val teamA: TeamSetup,
    val teamB: TeamSetup,
    val rules: MatchRules = MatchRules(),
    val createdAt: Long
)

data class TeamSetup(
    val id: String,
    val name: String,
    val players: List<PlayerSetup>
)

data class PlayerSetup(
    val id: String,
    val name: String
)

data class MatchRules(
    val setsToWin: Int = 2,
    val gamesToWinSet: Int = 6,
    val tieBreakAt: Int = 6,
    val tieBreakPointsToWin: Int = 7,
    val minimumAdvantage: Int = 2
)

object MatchSetupDataKeys {
    const val MATCH_ID = "match_id"
    const val CREATED_AT = "created_at"
    const val SENT_AT = "sent_at"

    const val TEAM_A_ID = "team_a_id"
    const val TEAM_A_NAME = "team_a_name"
    const val TEAM_A_PLAYER_IDS = "team_a_player_ids"
    const val TEAM_A_PLAYER_NAMES = "team_a_player_names"

    const val TEAM_B_ID = "team_b_id"
    const val TEAM_B_NAME = "team_b_name"
    const val TEAM_B_PLAYER_IDS = "team_b_player_ids"
    const val TEAM_B_PLAYER_NAMES = "team_b_player_names"

    const val SETS_TO_WIN = "sets_to_win"
    const val GAMES_TO_WIN_SET = "games_to_win_set"
    const val TIE_BREAK_AT = "tie_break_at"
    const val TIE_BREAK_POINTS_TO_WIN = "tie_break_points_to_win"
    const val MINIMUM_ADVANTAGE = "minimum_advantage"
}
