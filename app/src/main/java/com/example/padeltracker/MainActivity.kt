package com.example.padeltracker

import android.os.Bundle
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
import com.google.android.gms.wearable.Wearable

// Navigation states for the application
enum class AppScreen { Home, Setup, History }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PadelTrackerTheme {
                // Navigation state management
                var currentScreen by remember { mutableStateOf(AppScreen.Home) }

                // STATE: Track if a Wear OS device is currently connected via Bluetooth
                var isWatchConnected by remember { mutableStateOf(false) }

                // EFFECT: Check for connected wearable nodes when the app is launched
                LaunchedEffect(Unit) {
                    Wearable.getNodeClient(this@MainActivity).connectedNodes
                        .addOnSuccessListener { nodes ->
                            // If the list of nodes is not empty, at least one watch is connected
                            isWatchConnected = nodes.isNotEmpty()
                        }
                        .addOnFailureListener {
                            // Default to disconnected state if the query fails
                            isWatchConnected = false
                        }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .background(BackgroundBeige)
                    ) {
                        when (currentScreen) {
                            AppScreen.Home -> {
                                HomeScreen(
                                    // Pass the connectivity status to the Home Screen
                                    isConnected = isWatchConnected,
                                    onNewGameClick = { currentScreen = AppScreen.Setup },
                                    onHistoryClick = { currentScreen = AppScreen.History }
                                )
                            }
                            AppScreen.Setup -> {
                                MatchSetupScreen(
                                    onBackClick = { currentScreen = AppScreen.Home },
                                    onSendToWatch = { /* Wearable sync logic goes here */ }
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
}