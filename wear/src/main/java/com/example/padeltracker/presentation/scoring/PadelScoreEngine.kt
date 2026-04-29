package com.example.padeltracker.presentation.scoring

import com.example.padeltracker.presentation.model.*

/**
 * Pure padel scoring engine. Independent from Android UI.
 */
class PadelScoreEngine {

    /**
     * Creates a default ScoreTrackerState with placeholder players.
     */
    fun createDefaultMatch(): ScoreTrackerState {
        val teamA = Team(
            id = TeamId.TEAM_A,
            name = "Team A",
            players = listOf(Player("1", "Player 1"), Player("2", "Player 2"))
        )
        val teamB = Team(
            id = TeamId.TEAM_B,
            name = "Team B",
            players = listOf(Player("3", "Player 3"), Player("4", "Player 4"))
        )
        val match = MatchState(teamA = teamA, teamB = teamB)
        return ScoreTrackerState(
            initialMatch = match,
            currentMatch = match,
            pointHistory = emptyList()
        )
    }

    /**
     * Starts the match, moving status from NOT_STARTED to SELECTING_SERVER.
     */
    fun startMatch(state: ScoreTrackerState): ScoreTrackerState {
        if (state.currentMatch.status != MatchStatus.NOT_STARTED) return state
        val updatedMatch = state.currentMatch.copy(status = MatchStatus.SELECTING_SERVER)
        return state.copy(
            initialMatch = updatedMatch,
            currentMatch = updatedMatch
        )
    }

    /**
     * Selects the initial serving team and starts the match progress.
     * Only allowed if the match status is NOT_STARTED or SELECTING_SERVER.
     */
    fun selectInitialServer(state: ScoreTrackerState, teamId: TeamId): ScoreTrackerState {
        if (state.currentMatch.status != MatchStatus.NOT_STARTED &&
            state.currentMatch.status != MatchStatus.SELECTING_SERVER
        ) return state

        val updatedMatch = state.currentMatch.copy(
            servingTeam = teamId,
            initialServingTeam = teamId,
            status = MatchStatus.IN_PROGRESS
        )
        return state.copy(
            initialMatch = updatedMatch,
            currentMatch = updatedMatch,
            pointHistory = emptyList()
        )
    }

    /**
     * Adds a point to the winner team and updates the match state.
     */
    fun addPoint(state: ScoreTrackerState, winnerId: TeamId): ScoreTrackerState {
        if (state.currentMatch.status != MatchStatus.IN_PROGRESS) return state
        
        val newHistory = state.pointHistory + winnerId
        val nextMatch = applyPoint(state.currentMatch, winnerId)
        
        return state.copy(
            currentMatch = nextMatch,
            pointHistory = newHistory
        )
    }

    /**
     * Undoes the last point by replaying the history minus the last item.
     */
    fun undo(state: ScoreTrackerState): ScoreTrackerState {
        if (state.pointHistory.isNotEmpty()) {
            val newHistory = state.pointHistory.dropLast(1)
            val recalculatedMatch = replayHistory(state.initialMatch, newHistory)
            
            return state.copy(
                currentMatch = recalculatedMatch,
                pointHistory = newHistory
            )
        } else if (state.currentMatch.status == MatchStatus.IN_PROGRESS) {
            // If no points played, return to server selection
            val updatedMatch = state.currentMatch.copy(
                status = MatchStatus.SELECTING_SERVER,
                servingTeam = null,
                initialServingTeam = null
            )
            return state.copy(
                initialMatch = updatedMatch,
                currentMatch = updatedMatch,
                pointHistory = emptyList()
            )
        }
        
        return state
    }

    /**
     * Replays a history of points starting from an initial match state.
     */
    private fun replayHistory(initialMatch: MatchState, history: List<TeamId>): MatchState {
        var current = initialMatch
        for (winnerId in history) {
            current = applyPoint(current, winnerId)
        }
        return current
    }

    /**
     * Internal logic to apply a single point to a MatchState.
     */
    private fun applyPoint(match: MatchState, winnerId: TeamId): MatchState {
        if (match.status == MatchStatus.FINISHED) return match
        
        val currentSet = match.currentSet
        val currentGame = currentSet.currentGame
        
        val nextGame = incrementPoints(currentGame, winnerId)
        
        return when {
            isGameWon(nextGame, match.config) -> {
                val nextSet = incrementGames(currentSet.copy(currentGame = nextGame), winnerId, match.config)
                val newCompletedSets = match.completedSets + nextSet
                val matchWinner = getMatchWinner(newCompletedSets, match.config)

                if (matchWinner != null) {
                    match.copy(
                        currentSet = SetState(),
                        completedSets = newCompletedSets,
                        status = MatchStatus.FINISHED,
                        winner = matchWinner
                    )
                } else {
                    val nextServingTeam = if (currentGame.type == GameType.TIE_BREAK) {
                        val tieBreakStarter = inferTieBreakStartingServer(match.servingTeam, currentGame)
                        switchServer(tieBreakStarter)
                    } else {
                        switchServer(match.servingTeam)
                    }

                    if (isSetWon(nextSet, match.config)) {
                        match.copy(
                            currentSet = SetState(),
                            completedSets = newCompletedSets,
                            servingTeam = nextServingTeam
                        )
                    } else {
                        match.copy(
                            currentSet = nextSet,
                            servingTeam = nextServingTeam
                        )
                    }
                }
            }
            else -> {
                val nextServingTeam = if (nextGame.type == GameType.TIE_BREAK) {
                    val totalPoints = nextGame.teamAPoints + nextGame.teamBPoints
                    if (totalPoints % 2 != 0) switchServer(match.servingTeam) else match.servingTeam
                } else {
                    match.servingTeam
                }
                match.copy(
                    currentSet = currentSet.copy(currentGame = nextGame),
                    servingTeam = nextServingTeam
                )
            }
        }
    }

    private fun inferTieBreakStartingServer(currentServer: TeamId?, gameBeforePoint: GameState): TeamId? {
        val totalPoints = gameBeforePoint.teamAPoints + gameBeforePoint.teamBPoints
        // pattern: A BB AA BB ...
        // total 0: A
        // total 1: B
        // total 2: B
        // total 3: A
        // total 4: A
        // total 5: B
        // total 6: B
        // total 7: A
        // total 8: A
        // so if mod 4 is 0 or 3, currentServer is the starter.
        // if mod 4 is 1 or 2, currentServer is the opposite of the starter.
        return if (totalPoints % 4 == 0 || totalPoints % 4 == 3) {
            currentServer
        } else {
            switchServer(currentServer)
        }
    }

    private fun incrementPoints(game: GameState, winnerId: TeamId): GameState {
        return if (winnerId == TeamId.TEAM_A) {
            game.copy(teamAPoints = game.teamAPoints + 1)
        } else {
            game.copy(teamBPoints = game.teamBPoints + 1)
        }
    }

    private fun isGameWon(game: GameState, config: MatchConfig): Boolean {
        val p1 = game.teamAPoints
        val p2 = game.teamBPoints
        return if (game.type == GameType.TIE_BREAK) {
            (p1 >= config.tieBreakPointsToWin || p2 >= config.tieBreakPointsToWin) && 
                kotlin.math.abs(p1 - p2) >= config.minimumAdvantage
        } else {
            (p1 >= 4 || p2 >= 4) && kotlin.math.abs(p1 - p2) >= config.minimumAdvantage
        }
    }

    private fun incrementGames(set: SetState, winnerId: TeamId, config: MatchConfig): SetState {
        val nextSet = if (winnerId == TeamId.TEAM_A) {
            set.copy(teamAGames = set.teamAGames + 1)
        } else {
            set.copy(teamBGames = set.teamBGames + 1)
        }
        
        val gamesA = nextSet.teamAGames
        val gamesB = nextSet.teamBGames
        
        val nextType = if (gamesA == config.tieBreakAt && gamesB == config.tieBreakAt) {
            GameType.TIE_BREAK
        } else {
            GameType.NORMAL
        }
        
        return nextSet.copy(currentGame = GameState(type = nextType))
    }

    private fun isSetWon(set: SetState, config: MatchConfig): Boolean {
        val g1 = set.teamAGames
        val g2 = set.teamBGames
        
        // Normal set win
        val isNormalWin = (g1 >= config.gamesToWinSet || g2 >= config.gamesToWinSet) && 
            kotlin.math.abs(g1 - g2) >= config.minimumAdvantage
            
        // Tie-break set win (e.g., 7-6)
        val isTieBreakWin = (g1 == config.tieBreakAt + 1 && g2 == config.tieBreakAt) ||
                            (g2 == config.tieBreakAt + 1 && g1 == config.tieBreakAt)
                            
        return isNormalWin || isTieBreakWin
    }

    /**
     * Helper function to calculate the match winner from completed sets.
     */
    private fun getMatchWinner(completedSets: List<SetState>, config: MatchConfig): TeamId? {
        val winsA = completedSets.count { it.teamAGames > it.teamBGames }
        val winsB = completedSets.count { it.teamBGames > it.teamAGames }
        return when {
            winsA >= config.setsToWin -> TeamId.TEAM_A
            winsB >= config.setsToWin -> TeamId.TEAM_B
            else -> null
        }
    }

    private fun switchServer(current: TeamId?): TeamId? {
        if (current == null) return null
        return if (current == TeamId.TEAM_A) TeamId.TEAM_B else TeamId.TEAM_A
    }

}
