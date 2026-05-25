package com.example.padeltracker.presentation.service

import com.example.padeltracker.shared.MatchRules
import com.example.padeltracker.shared.MatchSetup
import com.example.padeltracker.shared.MatchSetupDataKeys
import com.example.padeltracker.shared.PlayerSetup
import com.example.padeltracker.shared.TeamSetup
import com.example.padeltracker.shared.communication.WearPaths
import com.example.padeltracker.shared.debug.DebugLogger
import com.example.padeltracker.presentation.data.PendingMatchSetupStore
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

/**
 * Receives match setup data sent from the phone (teams and players)
 */
class MatchSetupListenerService : WearableListenerService() {

    // Called when a match setup is received from the phone
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)

        DebugLogger.d(TAG, "onDataChanged called")

        dataEvents.forEach { event ->
            val dataItem = event.dataItem
            val path = dataItem.uri.path

            DebugLogger.d(TAG, "Data event received. type=${event.type}, path=$path")

            if (event.type == DataEvent.TYPE_CHANGED &&
                path == WearPaths.MATCH_SETUP
            ) {
                // Convert the data received into a MatchSetup object
                val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                val setup = dataMap.toMatchSetup()

                DebugLogger.d(TAG, "Match setup received successfully")
                PendingMatchSetupStore(applicationContext).save(setup)
                DebugLogger.d(TAG, "Match setup saved as pending setup")

                DebugLogger.d(TAG, "matchId=${setup.matchId}")
                DebugLogger.d(TAG, "createdAt=${setup.createdAt}")
                DebugLogger.d(TAG, "teamA=${setup.teamA.name}: ${setup.teamA.players.joinToString { it.name }}")
                DebugLogger.d(TAG, "teamB=${setup.teamB.name}: ${setup.teamB.players.joinToString { it.name }}")
                DebugLogger.d(
                    TAG,
                    "rules=setsToWin:${setup.rules.setsToWin}, " +
                        "gamesToWinSet:${setup.rules.gamesToWinSet}, " +
                        "tieBreakAt:${setup.rules.tieBreakAt}, " +
                        "tieBreakPointsToWin:${setup.rules.tieBreakPointsToWin}, " +
                        "minimumAdvantage:${setup.rules.minimumAdvantage}"
                )
            }
        }
    }

    // Take the data packet sent by the phone and convert it into a structured object (MatchSetup) that can be used by the watch module
    private fun DataMap.toMatchSetup(): MatchSetup {
        val defaultRules = MatchRules()

        return MatchSetup(
            matchId = getString(MatchSetupDataKeys.MATCH_ID) ?: "unknown_match",
            tournamentName = getString(MatchSetupDataKeys.TOURNAMENT_NAME) ?: "",
            createdAt = getLong(MatchSetupDataKeys.CREATED_AT),
            teamA = TeamSetup(
                id = getString(MatchSetupDataKeys.TEAM_A_ID) ?: "team_a",
                name = getString(MatchSetupDataKeys.TEAM_A_NAME) ?: "Team A",
                players = buildPlayers(
                    ids = getStringArrayList(MatchSetupDataKeys.TEAM_A_PLAYER_IDS),
                    names = getStringArrayList(MatchSetupDataKeys.TEAM_A_PLAYER_NAMES),
                    fallbackPrefix = "a"
                )
            ),
            teamB = TeamSetup(
                id = getString(MatchSetupDataKeys.TEAM_B_ID) ?: "team_b",
                name = getString(MatchSetupDataKeys.TEAM_B_NAME) ?: "Team B",
                players = buildPlayers(
                    ids = getStringArrayList(MatchSetupDataKeys.TEAM_B_PLAYER_IDS),
                    names = getStringArrayList(MatchSetupDataKeys.TEAM_B_PLAYER_NAMES),
                    fallbackPrefix = "b"
                )
            ),
            rules = MatchRules(
                setsToWin = getIntOrDefault(MatchSetupDataKeys.SETS_TO_WIN, defaultRules.setsToWin),
                gamesToWinSet = getIntOrDefault(MatchSetupDataKeys.GAMES_TO_WIN_SET, defaultRules.gamesToWinSet),
                tieBreakAt = getIntOrDefault(MatchSetupDataKeys.TIE_BREAK_AT, defaultRules.tieBreakAt),
                tieBreakPointsToWin = getIntOrDefault(
                    MatchSetupDataKeys.TIE_BREAK_POINTS_TO_WIN,
                    defaultRules.tieBreakPointsToWin
                ),
                minimumAdvantage = getIntOrDefault(
                    MatchSetupDataKeys.MINIMUM_ADVANTAGE,
                    defaultRules.minimumAdvantage
                )
            )
        )
    }

    private fun DataMap.getIntOrDefault(key: String, defaultValue: Int): Int {
        return if (containsKey(key)) getInt(key) else defaultValue
    }

    private fun buildPlayers(
        ids: ArrayList<String>?,
        names: ArrayList<String>?,
        fallbackPrefix: String
    ): List<PlayerSetup> {
        val safeNames = names.orEmpty()

        return safeNames.mapIndexed { index, name ->
            PlayerSetup(
                id = ids?.getOrNull(index) ?: "$fallbackPrefix${index + 1}",
                name = name
            )
        }
    }

    companion object {
        private const val TAG = "WATCH_SETUP_RECEIVER"
    }
}
