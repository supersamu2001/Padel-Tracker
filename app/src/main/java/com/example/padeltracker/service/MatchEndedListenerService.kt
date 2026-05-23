package com.example.padeltracker.service

import android.util.Log
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

        Log.d("PHONE_MATCH_ENDED", "Message received. path=${messageEvent.path}")

        if (messageEvent.path == WearPaths.MATCH_STARTED) {
            ShotDetectionState.reset()
            Log.d("PHONE_MATCH_ENDED", "Match started: ShotDetectionState reset")
            return
        }

        if (messageEvent.path == WearPaths.MATCH_STATS) {
            val rawData = messageEvent.data?.toString(Charsets.UTF_8) ?: ""
            Log.d("PHONE_MATCH_ENDED", "Received payload from wear: $rawData")

            try {
                val tokens = rawData.split("|")
                val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

                // Use the counts from the phone's classifier
                val currentShots = ShotDetectionState.shotCounts.value

                val completedMatch = MatchRecord(
                    // Room generates automatically a new incremental ID
                    id = 0,
                    date = currentDate,
                    score = tokens.getOrNull(0) ?: "0-0",
                    avgHeartRate = tokens.getOrNull(1)?.toIntOrNull() ?: 0,
                    forehands = currentShots.forehands,
                    backhands = currentShots.backhands,
                    smashes = currentShots.smashes,
                    services = currentShots.services,
                    forehandLobs = currentShots.forehandLobs,
                    backhandLobs = currentShots.backhandLobs,
                    teamAPlayers = tokens.getOrNull(2) ?: "Team A",
                    teamBPlayers = tokens.getOrNull(3) ?: "Team B",
                    winner = tokens.getOrNull(4) ?: "Draw",
                    duration = tokens.getOrNull(5) ?: "00:00",
                    heartRateHistory = tokens.getOrNull(6) ?: ""
                )

                // save in database of the phone
                serviceScope.launch {
                    repository.insertMatch(completedMatch)
                    Log.d("PHONE_MATCH_ENDED", "Match saved to Room database successfully!")

                    // end match
                    PhoneMatchEndedEventBus.notifyMatchEnded(System.currentTimeMillis())
                }

            } catch (e: Exception) {
                Log.e("PHONE_MATCH_ENDED", "Error parsing or saving match data", e)
            }
        }
    }
}