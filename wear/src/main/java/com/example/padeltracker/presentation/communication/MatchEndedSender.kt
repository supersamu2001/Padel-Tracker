package com.example.padeltracker.presentation.communication

import android.content.Context
import android.util.Log
import com.example.padeltracker.shared.WearCommunicationConstants
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sends a match-ended event from the Wear app to connected devices.
 */
class MatchEndedSender(
    private val context: Context
) {
    fun sendMatchEnded(heartRateHistory: String,
                       avgHeartRate: Int,
                       teamAPlayers: String,
                       teamBPlayers: String,
                       score: String,
                       winner: String,
                       duration: String)

    {

        val payload = System.currentTimeMillis().toString().toByteArray()

        Log.d(TAG, "Preparing to send match ended message")

        // new, date
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        // send the heartbeat history to the phone (for the graph)
        val putDataReq = PutDataMapRequest.create("/match_result").apply {
            dataMap.putString("heartRateHistory", heartRateHistory)
            dataMap.putLong("timestamp", System.currentTimeMillis())

            dataMap.putString("date", currentDate)
            dataMap.putString("duration", duration)
            dataMap.putString("score", score)
            dataMap.putString("teamAPlayers", teamAPlayers)
            dataMap.putString("teamBPlayers", teamBPlayers)
            dataMap.putString("winner", winner)
            dataMap.putInt("avgHeartRate", avgHeartRate)
        }.asPutDataRequest()

        putDataReq.setUrgent()

        Wearable.getDataClient(context).putDataItem(putDataReq)
            .addOnSuccessListener {
                Log.d(TAG, "Successfully synced Heart Rate History to phone!")
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to sync DataMap", error)
            }

        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    Log.d(TAG, "No connected nodes found for match ended message")
                    return@addOnSuccessListener
                }

                nodes.forEach { node ->
                    Log.d(
                        TAG,
                        "Sending match ended message to ${node.displayName} (${node.id})"
                    )

                    Wearable.getMessageClient(context)
                        .sendMessage(
                            node.id,
                            WearCommunicationConstants.MATCH_ENDED_PATH,
                            payload
                        )
                        .addOnSuccessListener {
                            Log.d(
                                TAG,
                                "Match ended message sent to ${node.displayName} (${node.id})"
                            )
                        }
                        .addOnFailureListener { error ->
                            Log.e(
                                TAG,
                                "Failed to send match ended message to ${node.displayName} (${node.id})",
                                error
                            )
                        }
                }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to get connected nodes", error)
            }
    }

    companion object {
        private const val TAG = "MATCH_ENDED_SENDER"
    }
}
