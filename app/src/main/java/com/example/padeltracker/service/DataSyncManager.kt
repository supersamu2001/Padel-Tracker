package com.example.padeltracker.service

import android.content.Context
import com.example.padeltracker.data.HistoryRepository
import com.example.padeltracker.data.MatchRecord
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DataSyncManager(
    private val context: Context,
    private val repository: HistoryRepository
) : DataClient.OnDataChangedListener {

    private val dataClient = Wearable.getDataClient(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    // Ξεκινάει να "ακούει" δεδομένα από το ρολόι
    fun startListening() {
        dataClient.addListener(this)
    }

    // Σταματάει να ακούει (π.χ. όταν κλείνει η εφαρμογή)
    fun stopListening() {
        dataClient.removeListener(this)
    }

    // Αυτή καλείται αυτόματα όταν έρθουν νέα δεδομένα από το ρολόι
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == "/match_result"
            ) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

                val match = MatchRecord(
                    date = dataMap.getString("date") ?: "",
                    duration = dataMap.getString("duration") ?: "",
                    score = dataMap.getString("score") ?: "",
                    avgHeartRate = dataMap.getInt("avgHeartRate"),
                    forehands = dataMap.getInt("forehands"),
                    backhands = dataMap.getInt("backhands"),
                    forehandLobs = dataMap.getInt("forehandLobs"),
                    backhandLobs = dataMap.getInt("backhandLobs"),
                    smashes = dataMap.getInt("smashes"),
                    services = dataMap.getInt("services"),
                    teamAPlayers = dataMap.getString("teamAPlayers") ?: "",
                    teamBPlayers = dataMap.getString("teamBPlayers") ?: "",
                    winner = dataMap.getString("winner") ?: ""
                )

                // Αποθήκευση στη βάση δεδομένων
                scope.launch {
                    repository.insertMatch(match)
                }
            }
        }
    }
}