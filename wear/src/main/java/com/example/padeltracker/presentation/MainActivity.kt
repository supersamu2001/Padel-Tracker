package com.example.padeltracker.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.padeltracker.presentation.ui.WearApp
import com.example.padeltracker.presentation.viewmodel.MatchViewModel
import com.example.padeltracker.presentation.data.PendingMatchSetupStore
import com.example.padeltracker.shared.WearCommunicationConstants
import com.google.android.gms.wearable.Wearable
import android.Manifest // heartbeat
import android.content.pm.PackageManager

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        registerWatchCapability()
        logPendingMatchSetup()

        setContent {
            val matchViewModel: MatchViewModel = viewModel()
            WearApp(matchViewModel)
        }
    }

    private fun checkAndRequestPermissions() {
        if (checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            Log.d("PERMISSIONS", "Requesting BODY_SENSORS permission")
            requestPermissions(arrayOf(Manifest.permission.BODY_SENSORS), 100)
        } else {
            Log.d("PERMISSIONS", "BODY_SENSORS permission already granted")
        }
    }

    private fun registerWatchCapability() {
        Wearable.getCapabilityClient(this)
            .addLocalCapability(WearCommunicationConstants.WATCH_CAPABILITY)
            .addOnSuccessListener {
                Log.d(
                    "WATCH_CAPABILITY",
                    "Local capability registered: ${WearCommunicationConstants.WATCH_CAPABILITY}"
                )
            }
            .addOnFailureListener { error ->
                Log.e(
                    "WATCH_CAPABILITY",
                    "Failed to register local capability",
                    error
                )
            }
    }

    private fun logPendingMatchSetup() {
        val setup = PendingMatchSetupStore(this).load()

        if (setup == null) {
            Log.d("PENDING_MATCH_SETUP", "No pending match setup found")
        } else {
            Log.d("PENDING_MATCH_SETUP", "Pending match setup available: ${setup.matchId}")
            Log.d(
                "PENDING_MATCH_SETUP",
                "teamA=${setup.teamA.name}: ${setup.teamA.players.joinToString { it.name }}"
            )
            Log.d(
                "PENDING_MATCH_SETUP",
                "teamB=${setup.teamB.name}: ${setup.teamB.players.joinToString { it.name }}"
            )
        }
    }
}
