package com.example.padeltracker.service

import com.example.padeltracker.ml.ShotDetectionState
import com.example.padeltracker.data.AppDatabase
import com.example.padeltracker.data.HistoryRepository
import com.example.padeltracker.data.MatchRecord
import com.example.padeltracker.wear.PhoneMatchEndedEventBus
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.padeltracker.shared.communication.WearPaths
import com.example.padeltracker.shared.debug.DebugLogger
import org.json.JSONObject

/**
 * Save the match infos into the Room database (with a coroutine) when the match is ended
 */
class MatchEndedListenerService : WearableListenerService() {

    // Dispatchers.IO => coroutine optimized for I/O jobs
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: HistoryRepository

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(this)
        repository = HistoryRepository(database.matchDao())
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        DebugLogger.d(TAG, "Message received. path=${messageEvent.path}")

        if (messageEvent.path == WearPaths.MATCH_STARTED) {
            ShotDetectionState.reset()
            DebugLogger.d(TAG, "Match started: ShotDetectionState reset")
            return
        }

        if (messageEvent.path == WearPaths.MATCH_ENDED) {
            val rawData = messageEvent.data.toString(Charsets.UTF_8)
            DebugLogger.d(TAG, "Received payload from wear: $rawData")

            try {
                val payload = JSONObject(rawData)
                val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

                // Use the counts from the phone's classifier
                val currentShots = ShotDetectionState.shotCounts.value

                val completedMatch = MatchRecord(
                    // Room generates automatically a new incremental ID
                    id = 0,
                    date = currentDate,
                    score = payload.optString("score", "0-0"),
                    avgHeartRate = payload.optInt("avgHeartRate", 0),
                    forehands = currentShots.forehands,
                    backhands = currentShots.backhands,
                    smashes = currentShots.smashes,
                    services = currentShots.services,
                    forehandLobs = currentShots.forehandLobs,
                    backhandLobs = currentShots.backhandLobs,
                    teamAPlayers = payload.optString("teamAPlayers", "Team A"),
                    teamBPlayers = payload.optString("teamBPlayers", "Team B"),
                    winner = payload.optString("winner", "Draw"),
                    duration = payload.optString("duration", "00:00"),
                    heartRateHistory = payload.optString("heartRateHistory", ""),
                    tournamentName = payload.optString("tournamentName", "")
                )

                // save in database of the phone
                serviceScope.launch {
                    repository.insertMatch(completedMatch)
                    DebugLogger.d(TAG, "Match saved to Room database successfully!")

                    // end match
                    PhoneMatchEndedEventBus.notifyMatchEnded(System.currentTimeMillis())
                }

            } catch (e: Exception) {
                DebugLogger.e(TAG, "Error parsing or saving match data", e)
            }
        }
    }

    companion object {
        private const val TAG = "PHONE_MATCH_ENDED"
    }
}
