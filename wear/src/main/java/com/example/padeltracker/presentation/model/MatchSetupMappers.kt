package com.example.padeltracker.presentation.model

import com.example.padeltracker.shared.MatchRules
import com.example.padeltracker.shared.MatchSetup
import com.example.padeltracker.shared.PlayerSetup
import com.example.padeltracker.shared.TeamSetup

/**
 * Mappers to convert shared setup models into wear-specific domain models.
 *
 * So it takes data structures of the shared module and converts them in order to be used in the wear module.
 *  Input: object of the shared module
 *  Output: new object of the wear module that will be used by other files of the wear module
 */
fun PlayerSetup.toDomain(): Player {
    return Player(id = this.id, name = this.name)
}

fun TeamSetup.toDomain(teamId: TeamId): Team {
    return Team(
        id = teamId,
        name = this.name,
        players = this.players.map { it.toDomain() }
    )
}

fun MatchRules.toDomain(): MatchConfig {
    return MatchConfig(
        setsToWin = this.setsToWin,
        gamesToWinSet = this.gamesToWinSet,
        tieBreakAt = this.tieBreakAt,
        tieBreakPointsToWin = this.tieBreakPointsToWin,
        minimumAdvantage = this.minimumAdvantage
    )
}

fun MatchSetup.toDomain(): MatchState {
    return MatchState(
        teamA = this.teamA.toDomain(TeamId.TEAM_A),
        teamB = this.teamB.toDomain(TeamId.TEAM_B),
        tournamentName = this.tournamentName,
        config = this.rules.toDomain(),
        status = MatchStatus.NOT_STARTED
    )
}
