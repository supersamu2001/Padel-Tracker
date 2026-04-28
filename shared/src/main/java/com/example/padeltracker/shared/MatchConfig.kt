package com.example.padeltracker.shared

data class MatchConfig(
    val teamAPlayer1: String = "Player 1",
    val teamAPlayer2: String = "Player 2",
    val teamBPlayer1: String = "Player 1",
    val teamBPlayer2: String = "Player 2"
) {
    companion object {
        const val PATH = "/match_config"
        const val KEY_TEAM_A_P1 = "team_a_p1"
        const val KEY_TEAM_A_P2 = "team_a_p2"
        const val KEY_TEAM_B_P1 = "team_b_p1"
        const val KEY_TEAM_B_P2 = "team_b_p2"
    }
}
