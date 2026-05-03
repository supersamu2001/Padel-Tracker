package com.example.padeltracker

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.padeltracker.ui.theme.*
import com.example.padeltracker.ui.screens.*
import com.example.padeltracker.shared.MatchConfig
import com.google.android.gms.wearable.Wearable

// Enum to define the different screens in the app
enum class AppScreen { Home, Setup, History, LiveMatch }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        println(">>> TEST PRINTLN: L'APP E' PARTITA! <<<")
        Log.d("TEST_LOG", ">>> TEST LOG: L'APP E' PARTITA! <<<")
        Log.d("AIUTOO", "Funzionaaaaaaaaa")


        // Enables edge-to-edge display (status bar and navigation bar transparency)
        enableEdgeToEdge()

        setContent {
            PadelTrackerTheme {
                // NAVIGATION STATE: Tracks which screen is currently visible
                var currentScreen by remember { mutableStateOf(AppScreen.Home) }

                // CONNECTIVITY STATE: Tracks if a Wear OS watch is connected
                var isWatchConnected by remember { mutableStateOf(false) }

                // DATA STATE: Holds the player names and match settings entered in SetupScreen
                // This is shared between SetupScreen and LiveScoreScreen
                var activeMatchConfig by remember { mutableStateOf<MatchConfig?>(null) }

                // SIDE EFFECT: Check for connected Wear OS nodes when the app starts
                LaunchedEffect(Unit) {
                    Wearable.getNodeClient(this@MainActivity).connectedNodes
                        .addOnSuccessListener { nodes ->
                            isWatchConnected = nodes.isNotEmpty()
                        }
                        .addOnFailureListener {
                            isWatchConnected = false
                        }
                }

                // Root layout using Scaffold for basic material design structure
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .background(BackgroundBeige) // Using our custom theme color
                    ) {
                        // NATIVE NAVIGATION LOGIC
                        when (currentScreen) {
                            AppScreen.Home -> {
                                HomeScreen(
                                    isConnected = isWatchConnected,
                                    onNewGameClick = { currentScreen = AppScreen.Setup },
                                    onHistoryClick = { currentScreen = AppScreen.History }
                                )
                            }

                            AppScreen.Setup -> {
                                MatchSetupScreen(
                                    onBackClick = { currentScreen = AppScreen.Home },
                                    onSendToWatch = { config ->
                                        // 1. Capture the configuration from the setup screen
                                        activeMatchConfig = config
                                        // 2. Navigate to the live scoring screen
                                        currentScreen = AppScreen.LiveMatch
                                    }
                                )
                            }

                            AppScreen.LiveMatch -> {
                                // Safety Check: Only show LiveScoreScreen if we have a valid configuration
                                activeMatchConfig?.let { config ->
                                    LiveScoreScreen(
                                        config = config,
                                        onFinish = {
                                            // After saving the match, go straight to History
                                            currentScreen = AppScreen.History
                                        }
                                    )
                                } ?: run {
                                    // Fallback: If config is null, return to Home
                                    currentScreen = AppScreen.Home
                                }
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
}