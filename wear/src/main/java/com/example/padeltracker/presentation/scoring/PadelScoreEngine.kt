package com.example.padeltracker.presentation.scoring

import com.example.padeltracker.presentation.model.*

/**
 * Pure padel scoring logic. Independent from Android UI.
 */
class PadelScoreEngine {

    /**
     * Creates a default ScoreTrackerState with placeholder players.
     * Useful for testing or initializing a match with default settings.
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
            status = MatchStatus.IN_PROGRESS,
            servingPlayerIndex = 0,
            teamANextServerIndex = 0,
            teamBNextServerIndex = 0
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
     * Undoes the last action.
     *
     * Behavior:
     * - if the match was ended early, Undo reopens the match without changing the score;
     * - otherwise, if there is point history, Undo removes the last point;
     * - if no points were played and the match is in progress, Undo goes back to server selection.
     */
    fun undo(state: ScoreTrackerState): ScoreTrackerState {
        // Case 1: the match was manually ended early.
        // Reopen it without changing score, sets, games, serving team, or point history.
        if (state.currentMatch.status == MatchStatus.FINISHED && state.currentMatch.endedEarly) {
            val reopenedMatch = state.currentMatch.copy(
                status = MatchStatus.IN_PROGRESS,
                winner = null,
                endedEarly = false
            )

            return state.copy(
                currentMatch = reopenedMatch
            )
        }

        // Case 2: if at least one point was played, remove the last point
        // and rebuild the match from the initial match state.
        if (state.pointHistory.isNotEmpty()) {
            val newHistory = state.pointHistory.dropLast(1)
            val recalculatedMatch = replayHistory(state.initialMatch, newHistory)

            return state.copy(
                currentMatch = recalculatedMatch,
                pointHistory = newHistory
            )
        }

        // Case 3: if the match is in progress but no point was played,
        // go back to the initial server selection screen.
        if (state.currentMatch.status == MatchStatus.IN_PROGRESS) {
            val updatedMatch = state.currentMatch.copy(
                status = MatchStatus.SELECTING_SERVER,
                servingTeam = null,
                initialServingTeam = null,
                servingPlayerIndex = null
            )

            return state.copy(
                initialMatch = updatedMatch,
                currentMatch = updatedMatch,
                pointHistory = emptyList()
            )
        }

        // Default case: if Undo is called in a state where it should not do anything,
        // return the state unchanged.
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
     * Ends the match early before a natural winner is reached.
     */
    fun endMatchEarly(state: ScoreTrackerState): ScoreTrackerState {
        if (state.currentMatch.status != MatchStatus.IN_PROGRESS) return state

        val updatedMatch = state.currentMatch.copy(
            status = MatchStatus.FINISHED,
            winner = null,
            endedEarly = true
        )
        return state.copy(currentMatch = updatedMatch)
    }

    /**
     * Internal logic to apply a single point to a MatchState.
     * It manages what happen after a point e.g. game won/set won
     */
    private fun applyPoint(match: MatchState, winnerId: TeamId): MatchState {
        if (match.status == MatchStatus.FINISHED) return match
        
        val currentSet = match.currentSet
        val currentGame = currentSet.currentGame
        
        val nextGame = incrementPoints(currentGame, winnerId)
        
        return when {
            isGameWon(nextGame, match.config) -> {
                // the point causes the game to finish -> we need to check if also the set/match is finish
                val nextSet = incrementGames(currentSet.copy(currentGame = nextGame), winnerId, match.config)
                
                if (isSetWon(nextSet, match.config)) {
                    val newCompletedSets = match.completedSets + nextSet
                    val matchWinner = getMatchWinner(newCompletedSets, match.config)

                    if (matchWinner != null) { // someone win
                        match.copy(
                            currentSet = SetState(),
                            completedSets = newCompletedSets,
                            status = MatchStatus.FINISHED,
                            winner = matchWinner
                        )
                    } else { // match not finish
                        // Set ended, Match continues
                        val nextMatch = if (currentGame.type == GameType.TIE_BREAK) {
                            /*
                             * The tie-break has just ended.
                             *
                             * The next set must start with the next server in the normal serving cycle
                             * after the player who started the tie-break.
                             *
                             * Important:
                             * We do not use the player who served the final tie-break point.
                             */
                            val nextSetServer = getNextSetServerAfterTieBreak(match)

                            if (nextSetServer != null) {
                                match.copy(
                                    servingTeam = nextSetServer.team,
                                    servingPlayerIndex = nextSetServer.playerIndex,
                                    tieBreakStartingTeam = null,
                                    tieBreakStartingPlayerIndex = null
                                )
                            } else { //fallback
                                match.copy(
                                    tieBreakStartingTeam = null,
                                    tieBreakStartingPlayerIndex = null
                                )
                            }
                        } else {
                            rotateService(match)
                        }
                        
                        nextMatch.copy(
                            currentSet = SetState(),
                            completedSets = newCompletedSets
                        )
                    }
                } else {
                    // Game ended, Set continues
                    val rotatedMatch = rotateService(match)

                    val nextMatch = if (nextSet.currentGame.type == GameType.TIE_BREAK) {
                        //Tie-break just started, we need to set which player is serving

                        rotatedMatch.copy(
                            currentSet = nextSet,
                            tieBreakStartingTeam = rotatedMatch.servingTeam,
                            tieBreakStartingPlayerIndex = rotatedMatch.servingPlayerIndex
                        )
                    } else {
                        rotatedMatch.copy(currentSet = nextSet)
                    }

                    nextMatch
                }
            }
            else -> {
                // Point won, Game continues
                if (nextGame.type == GameType.TIE_BREAK) {
                    // extra check because in the tiebreak service can change mid-game
                    val updatedServerMatch = updateTieBreakServingDisplay(
                        match = match,
                        gameAfterPoint = nextGame
                    )

                    updatedServerMatch.copy(
                        currentSet = currentSet.copy(currentGame = nextGame)
                    )
                } else {
                    match.copy(
                        currentSet = currentSet.copy(currentGame = nextGame)
                    )
                }
            }
        }
    }

    /**
     * Represents one serving slot in the doubles serving cycle.
     *
     * The simplified service cycle used in this project is:
     * Team A player 0 -> Team B player 0 -> Team A player 1 -> Team B player 1 -> repeat.
     */
    private data class ServingSlot(
        val team: TeamId,
        val playerIndex: Int
    )

    /**
     * Converts a serving team/player into its position in the simplified serving cycle.
     *
     * Cycle:
     * 0 = Team A player 0
     * 1 = Team B player 0
     * 2 = Team A player 1
     * 3 = Team B player 1
     */
    private fun servingSlotIndex(team: TeamId, playerIndex: Int): Int {
        return when (team) {
            TeamId.TEAM_A -> if (playerIndex == 0) 0 else 2
            TeamId.TEAM_B -> if (playerIndex == 0) 1 else 3
        }
    }

    /**
     * Converts a serving cycle index back into a serving team/player.
     */
    private fun servingSlotFromIndex(index: Int): ServingSlot {
        return when (index.mod(4)) {
            0 -> ServingSlot(TeamId.TEAM_A, 0)
            1 -> ServingSlot(TeamId.TEAM_B, 0)
            2 -> ServingSlot(TeamId.TEAM_A, 1)
            else -> ServingSlot(TeamId.TEAM_B, 1)
        }
    }

    /**
     * Returns the serving slot for the next tie-break point.
     *
     * Tie-break rule:
     * - first point: starting server
     * - next two points: next server in the normal cycle
     * - next two points: next server in the normal cycle
     * - and so on
     */
    private fun getTieBreakServingSlotAfterPoints(
        startingTeam: TeamId?,
        startingPlayerIndex: Int?,
        pointsPlayed: Int
    ): ServingSlot? {
        if (startingTeam == null || startingPlayerIndex == null) return null

        val startSlotIndex = servingSlotIndex(startingTeam, startingPlayerIndex)

        val serverChanges = if (pointsPlayed == 0) {
            0
        } else {
            (pointsPlayed + 1) / 2
        }

        return servingSlotFromIndex(startSlotIndex + serverChanges)
    }

    /**
     * Updates only the displayed serving team/player during a tie-break.
     *
     * Important:
     * This function must NOT update teamANextServerIndex or teamBNextServerIndex.
     * The tie-break has its own temporary serving sequence.
     */
    private fun updateTieBreakServingDisplay(
        match: MatchState,
        gameAfterPoint: GameState
    ): MatchState {
        val totalPoints = gameAfterPoint.teamAPoints + gameAfterPoint.teamBPoints

        val nextSlot = getTieBreakServingSlotAfterPoints(
            startingTeam = match.tieBreakStartingTeam,
            startingPlayerIndex = match.tieBreakStartingPlayerIndex,
            pointsPlayed = totalPoints
        ) ?: return match

        return match.copy(
            servingTeam = nextSlot.team,
            servingPlayerIndex = nextSlot.playerIndex
        )
    }

    /**
     * Returns the serving slot that should start the next set after a tie-break.
     */
    private fun getNextSetServerAfterTieBreak(match: MatchState): ServingSlot? {
        val startingTeam = match.tieBreakStartingTeam ?: return null
        val startingPlayerIndex = match.tieBreakStartingPlayerIndex ?: return null

        val startSlotIndex = servingSlotIndex(startingTeam, startingPlayerIndex)

        return servingSlotFromIndex(startSlotIndex + 1)
    }

    private fun rotateService(match: MatchState): MatchState {
        val oldTeam = match.servingTeam ?: return match
        val rotatedMatch = rotatePlayerWithinTeam(match, oldTeam)
        
        val nextTeam = switchServer(oldTeam)
        val nextPlayerIndex = if (nextTeam == TeamId.TEAM_A) rotatedMatch.teamANextServerIndex else rotatedMatch.teamBNextServerIndex
        
        return rotatedMatch.copy(
            servingTeam = nextTeam,
            servingPlayerIndex = nextPlayerIndex
        )
    }

    private fun rotatePlayerWithinTeam(match: MatchState, teamId: TeamId): MatchState {
        return if (teamId == TeamId.TEAM_A) {
            match.copy(teamANextServerIndex = (match.teamANextServerIndex + 1) % 2)
        } else {
            match.copy(teamBNextServerIndex = (match.teamBNextServerIndex + 1) % 2)
        }
    }

    private fun incrementPoints(game: GameState, winnerId: TeamId): GameState {
        return if (winnerId == TeamId.TEAM_A) {
            game.copy(teamAPoints = game.teamAPoints + 1)
        } else {
            game.copy(teamBPoints = game.teamBPoints + 1)
        }
    }

    /**
     * Determines if a game has been won based on the current points and match configuration.
     * Handles both normal games (40-40/Deuce) and tie-breaks.
     */
    fun isGameWon(game: GameState, config: MatchConfig): Boolean {
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
