package com.example.padeltracker.presentation.service

import android.util.Log
import com.example.padeltracker.shared.MatchRules
import com.example.padeltracker.shared.MatchSetup
import com.example.padeltracker.shared.MatchSetupDataKeys
import com.example.padeltracker.shared.PlayerSetup
import com.example.padeltracker.shared.TeamSetup
import com.example.padeltracker.shared.communication.WearPaths
import com.example.padeltracker.presentation.data.PendingMatchSetupStore
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

/**
 * Receives match setup data sent from the phone through the Wear OS Data Layer.
 */
class MatchSetupListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)

        Log.d(TAG, "onDataChanged called")

        dataEvents.forEach { event ->
            val dataItem = event.dataItem
            val path = dataItem.uri.path

            Log.d(TAG, "Data event received. type=${event.type}, path=$path")

            if (event.type == DataEvent.TYPE_CHANGED &&
                path == WearPaths.MATCH_SETUP
            ) {
                val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                val setup = dataMap.toMatchSetup()

                Log.d(TAG, "Match setup received successfully")
                PendingMatchSetupStore(applicationContext).save(setup)
                Log.d(TAG, "Match setup saved as pending setup")

                Log.d(TAG, "matchId=${setup.matchId}")
                Log.d(TAG, "createdAt=${setup.createdAt}")
                Log.d(TAG, "teamA=${setup.teamA.name}: ${setup.teamA.players.joinToString { it.name }}")
                Log.d(TAG, "teamB=${setup.teamB.name}: ${setup.teamB.players.joinToString { it.name }}")
                Log.d(
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

    private fun DataMap.toMatchSetup(): MatchSetup {
        val defaultRules = MatchRules()

        return MatchSetup(
            matchId = getString(MatchSetupDataKeys.MATCH_ID) ?: "unknown_match",
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
