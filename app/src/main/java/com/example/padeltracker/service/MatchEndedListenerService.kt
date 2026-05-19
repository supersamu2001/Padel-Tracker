package com.example.padeltracker.service

import android.util.Log
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

class MatchEndedListenerService : WearableListenerService() {

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

        if (messageEvent.path == "/match_stats") {
            val rawData = messageEvent.data?.toString(Charsets.UTF_8) ?: ""
            Log.d("PHONE_MATCH_ENDED", "Received payload from wear: $rawData")

            try {
                val tokens = rawData.split("|")
                val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

                val completedMatch = MatchRecord(
                    id = 0,
                    date = currentDate,
                    score = tokens.getOrNull(0) ?: "0-0",
                    avgHeartRate = tokens.getOrNull(1)?.toIntOrNull() ?: 0,
                    forehands = tokens.getOrNull(2)?.toIntOrNull() ?: 0,
                    backhands = tokens.getOrNull(3)?.toIntOrNull() ?: 0,
                    smashes = tokens.getOrNull(4)?.toIntOrNull() ?: 0,
                    services = tokens.getOrNull(5)?.toIntOrNull() ?: 0,
                    forehandLobs = tokens.getOrNull(6)?.toIntOrNull() ?: 0,
                    backhandLobs = tokens.getOrNull(7)?.toIntOrNull() ?: 0,
                    teamAPlayers = tokens.getOrNull(8) ?: "Team A",
                    teamBPlayers = tokens.getOrNull(9) ?: "Team B",
                    winner = tokens.getOrNull(10) ?: "Draw",
                    duration = tokens.getOrNull(11) ?: "00:00",
                    heartRateHistory = tokens.getOrNull(12) ?: ""
                )

                // save in data base of the phone
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