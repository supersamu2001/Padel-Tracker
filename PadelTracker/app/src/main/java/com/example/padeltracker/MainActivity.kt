package com.example.padeltracker

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.padeltracker.shared.MatchConfig
import com.example.padeltracker.ui.theme.*
import com.example.padeltracker.ui.screens.* // This links to your new screens package
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

// Navigation state
enum class AppScreen { Home, Setup, History }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PadelTrackerTheme {
                // Tracking current screen
                var currentScreen by remember { mutableStateOf(AppScreen.Home) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .background(BackgroundBeige)
                    ) {
                        // Navigation Logic
                        when (currentScreen) {
                            AppScreen.Home -> {
                                HomeScreen(
                                    onNewGameClick = { currentScreen = AppScreen.Setup },
                                    onHistoryClick = { currentScreen = AppScreen.History }
                                )
                            }
                            AppScreen.Setup -> {
                                MatchSetupScreen(
                                    onBackClick = { currentScreen = AppScreen.Home },
                                    onSendToWatch = { config -> sendConfigToWatch(config) }
                                )
                            }
                            AppScreen.History -> {
                                HistoryScreen(
                                    onBackClick = { currentScreen = AppScreen.Home }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // This function stays here because it needs the Activity's context to talk to the Wearable API
    private fun sendConfigToWatch(config: MatchConfig) {
        val putDataMapReq = PutDataMapRequest.create(MatchConfig.PATH)
        putDataMapReq.dataMap.putString(MatchConfig.KEY_TEAM_A_P1, config.teamAPlayer1)
        putDataMapReq.dataMap.putString(MatchConfig.KEY_TEAM_A_P2, config.teamAPlayer2)
        putDataMapReq.dataMap.putString(MatchConfig.KEY_TEAM_B_P1, config.teamBPlayer1)
        putDataMapReq.dataMap.putString(MatchConfig.KEY_TEAM_B_P2, config.teamBPlayer2)
        putDataMapReq.setUrgent()

        val putDataReq = putDataMapReq.asPutDataRequest()
        Wearable.getDataClient(this).putDataItem(putDataReq)
            .addOnSuccessListener {
                Toast.makeText(this, "Sent to watch!", Toast.LENGTH_SHORT).show()
            }
    }
}