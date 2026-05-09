package com.example.padeltracker.wear

import android.content.Context
import android.util.Log
import com.example.padeltracker.shared.MatchSetup
import com.example.padeltracker.shared.MatchSetupDataKeys
import com.example.padeltracker.shared.WearCommunicationConstants
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

class WearMatchSetupSender(private val context: Context) {

    fun sendMatchSetup(
        setup: MatchSetup,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val request = PutDataMapRequest.create(
                WearCommunicationConstants.MATCH_SETUP_PATH
            ).apply {
                dataMap.putString(MatchSetupDataKeys.MATCH_ID, setup.matchId)
                dataMap.putLong(MatchSetupDataKeys.CREATED_AT, setup.createdAt)
                dataMap.putLong(MatchSetupDataKeys.SENT_AT, System.currentTimeMillis())

                dataMap.putString(MatchSetupDataKeys.TEAM_A_ID, setup.teamA.id)
                dataMap.putString(MatchSetupDataKeys.TEAM_A_NAME, setup.teamA.name)
                dataMap.putStringArrayList(
                    MatchSetupDataKeys.TEAM_A_PLAYER_IDS,
                    ArrayList(setup.teamA.players.map { it.id })
                )
                dataMap.putStringArrayList(
                    MatchSetupDataKeys.TEAM_A_PLAYER_NAMES,
                    ArrayList(setup.teamA.players.map { it.name })
                )

                dataMap.putString(MatchSetupDataKeys.TEAM_B_ID, setup.teamB.id)
                dataMap.putString(MatchSetupDataKeys.TEAM_B_NAME, setup.teamB.name)
                dataMap.putStringArrayList(
                    MatchSetupDataKeys.TEAM_B_PLAYER_IDS,
                    ArrayList(setup.teamB.players.map { it.id })
                )
                dataMap.putStringArrayList(
                    MatchSetupDataKeys.TEAM_B_PLAYER_NAMES,
                    ArrayList(setup.teamB.players.map { it.name })
                )

                dataMap.putInt(MatchSetupDataKeys.SETS_TO_WIN, setup.rules.setsToWin)
                dataMap.putInt(MatchSetupDataKeys.GAMES_TO_WIN_SET, setup.rules.gamesToWinSet)
                dataMap.putInt(MatchSetupDataKeys.TIE_BREAK_AT, setup.rules.tieBreakAt)
                dataMap.putInt(
                    MatchSetupDataKeys.TIE_BREAK_POINTS_TO_WIN,
                    setup.rules.tieBreakPointsToWin
                )
                dataMap.putInt(MatchSetupDataKeys.MINIMUM_ADVANTAGE, setup.rules.minimumAdvantage)
            }.asPutDataRequest().setUrgent()

            Wearable.getDataClient(context).putDataItem(request)
                .addOnSuccessListener {
                    Log.d("WATCH_SETUP", "Successfully sent match setup to watch")
                    onSuccess()
                }
                .addOnFailureListener { error ->
                    Log.e("WATCH_SETUP", "Failed to send match setup to watch", error)
                    onFailure(error)
                }
        } catch (e: Exception) {
            Log.e("WATCH_SETUP", "Exception while sending match setup", e)
            onFailure(e)
        }
    }
}
