package com.example.padeltracker.presentation.communication

import android.content.Context
import com.example.padeltracker.shared.communication.WearPaths
import com.example.padeltracker.shared.debug.DebugLogger
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject

class MatchEndedSender(
    private val context: Context
) {
    fun sendMatchEnded(
        score: String,
        avgHeartRate: Int,
        teamAPlayers: String,
        teamBPlayers: String,
        winner: String,
        duration: String,
        heartRateHistory: String,
        tournamentName: String
    ) {
        val payload = JSONObject()
            .put("score", score)
            .put("avgHeartRate", avgHeartRate)
            .put("teamAPlayers", teamAPlayers)
            .put("teamBPlayers", teamBPlayers)
            .put("winner", winner)
            .put("duration", duration)
            .put("heartRateHistory", heartRateHistory)
            .put("tournamentName", tournamentName)
            .toString()
            .toByteArray(Charsets.UTF_8)

        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    DebugLogger.d(TAG, "No connected nodes found for match ended message")
                    return@addOnSuccessListener
                }

                nodes.forEach { node ->
                    Wearable.getMessageClient(context)
                        .sendMessage(node.id, WearPaths.MATCH_ENDED, payload)
                        .addOnSuccessListener {
                            DebugLogger.d(TAG, "Match ended message sent to ${node.displayName} (${node.id})")
                        }
                        .addOnFailureListener { error ->
                            DebugLogger.e(
                                TAG,
                                "Failed to send match ended message to ${node.displayName} (${node.id})",
                                error
                            )
                        }
                }
            }
            .addOnFailureListener { error ->
                DebugLogger.e(TAG, "Failed to get connected nodes", error)
            }
    }

    companion object {
        private const val TAG = "MATCH_ENDED_SENDER"
    }
}
