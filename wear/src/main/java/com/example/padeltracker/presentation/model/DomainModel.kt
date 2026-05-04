package com.example.padeltracker.presentation.model

enum class TeamId {
    TEAM_A, TEAM_B
}

enum class GameType {
    NORMAL, TIE_BREAK
}

enum class MatchStatus {
    NOT_STARTED, SELECTING_SERVER, IN_PROGRESS, FINISHED
}

data class Player(
    val id: String,
    val name: String
)

data class Team(
    val id: TeamId,
    val name: String,
    val players: List<Player>
)

data class MatchConfig(
    val setsToWin: Int = 2,
    val gamesToWinSet: Int = 6,
    val tieBreakAt: Int = 6,
    val tieBreakPointsToWin: Int = 7,
    val minimumAdvantage: Int = 2
)

data class GameState(
    val teamAPoints: Int = 0,
    val teamBPoints: Int = 0,
    val type: GameType = GameType.NORMAL
)

data class SetState(
    val teamAGames: Int = 0,
    val teamBGames: Int = 0,
    val currentGame: GameState = GameState()
)

data class MatchState(
    val teamA: Team,
    val teamB: Team,
    val config: MatchConfig = MatchConfig(),
    val currentSet: SetState = SetState(),
    val completedSets: List<SetState> = emptyList(),
    val servingTeam: TeamId? = null,
    val initialServingTeam: TeamId? = null,
    val status: MatchStatus = MatchStatus.NOT_STARTED,
    val winner: TeamId? = null,
    val servingPlayerIndex: Int? = null,
    val teamANextServerIndex: Int = 0,
    val teamBNextServerIndex: Int = 0,
    val tieBreakStartingTeam: TeamId? = null,
    val tieBreakStartingPlayerIndex: Int? = null
)

data class ScoreTrackerState(
    val initialMatch: MatchState,
    val currentMatch: MatchState,
    val pointHistory: List<TeamId> = emptyList()
)
