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
import com.example.padeltracker.shared.MatchSetup
import com.example.padeltracker.shared.WearCommunicationConstants
import com.example.padeltracker.ui.screens.*
import com.example.padeltracker.ui.theme.*
import com.example.padeltracker.wear.WearMatchSetupSender
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch

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
                var activeMatchSetup by remember { mutableStateOf<MatchSetup?>(null) }

                var isCheckingWatch by remember { mutableStateOf(false) }
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                val matchSetupSender = remember {
                    WearMatchSetupSender(this@MainActivity)
                }

                fun checkWatchAndOpenSetup() {
                    if (isCheckingWatch) return
                    isCheckingWatch = true

                    Log.d("WATCH_DEBUG", "Starting watch capability check...")

                    // Debug 1: check generic connected Wear OS nodes
                    Wearable.getNodeClient(this@MainActivity).connectedNodes
                        .addOnSuccessListener { nodes ->
                            Log.d(
                                "WATCH_DEBUG",
                                "Connected nodes: ${
                                    nodes.joinToString { node ->
                                        "${node.displayName} (${node.id}) nearby=${node.isNearby}"
                                    }
                                }"
                            )
                        }
                        .addOnFailureListener { error ->
                            Log.e("WATCH_DEBUG", "Failed to get connected nodes", error)
                        }

                    // Debug 2: check all nodes that declare the Padel Tracker capability
                    Wearable.getCapabilityClient(this@MainActivity)
                        .getCapability(
                            WearCommunicationConstants.WATCH_CAPABILITY,
                            CapabilityClient.FILTER_ALL
                        )
                        .addOnSuccessListener { capabilityInfo ->
                            Log.d(
                                "WATCH_DEBUG",
                                "All capability nodes: ${
                                    capabilityInfo.nodes.joinToString { node ->
                                        "${node.displayName} (${node.id}) nearby=${node.isNearby}"
                                    }
                                }"
                            )
                        }
                        .addOnFailureListener { error ->
                            Log.e("WATCH_DEBUG", "Failed to get all capability nodes", error)
                        }

                    // Real check: only reachable nodes with the Padel Tracker capability
                    Wearable.getCapabilityClient(this@MainActivity)
                        .getCapability(
                            WearCommunicationConstants.WATCH_CAPABILITY,
                            CapabilityClient.FILTER_REACHABLE
                        )
                        .addOnSuccessListener { capabilityInfo ->
                            isCheckingWatch = false

                            Log.d(
                                "WATCH_DEBUG",
                                "Reachable capability nodes: ${
                                    capabilityInfo.nodes.joinToString { node ->
                                        "${node.displayName} (${node.id}) nearby=${node.isNearby}"
                                    }
                                }"
                            )

                            if (capabilityInfo.nodes.isNotEmpty()) {
                                Log.d("WATCH_DEBUG", "Padel Tracker watch found. Opening setup screen.")

                                isWatchConnected = true
                                currentScreen = AppScreen.Setup
                            } else {
                                Log.d("WATCH_DEBUG", "No reachable Padel Tracker watch found.")

                                isWatchConnected = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("No Padel Tracker watch connected")
                                }
                            }
                        }
                        .addOnFailureListener { error ->
                            isCheckingWatch = false
                            isWatchConnected = false

                            Log.e("WATCH_DEBUG", "Unable to check reachable watch capability", error)

                            scope.launch {
                                snackbarHostState.showSnackbar("Unable to check watch connection")
                            }
                        }
                }

                // SIDE EFFECT: Check if a reachable Padel Tracker watch is available when the app starts
                LaunchedEffect(Unit) {
                    Log.d("WATCH_DEBUG", "Initial watch capability check...")

                    Wearable.getCapabilityClient(this@MainActivity)
                        .getCapability(
                            WearCommunicationConstants.WATCH_CAPABILITY,
                            CapabilityClient.FILTER_REACHABLE
                        )
                        .addOnSuccessListener { capabilityInfo ->
                            Log.d(
                                "WATCH_DEBUG",
                                "Initial reachable capability nodes: ${
                                    capabilityInfo.nodes.joinToString { node ->
                                        "${node.displayName} (${node.id}) nearby=${node.isNearby}"
                                    }
                                }"
                            )

                            isWatchConnected = capabilityInfo.nodes.isNotEmpty()
                        }
                        .addOnFailureListener { error ->
                            Log.e("WATCH_DEBUG", "Initial capability check failed", error)
                            isWatchConnected = false
                        }
                }

                // Root layout using Scaffold for basic material design structure
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
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
                                    onNewGameClick = { checkWatchAndOpenSetup() },
                                    onHistoryClick = { currentScreen = AppScreen.History }
                                )
                            }

                            AppScreen.Setup -> {
                                MatchSetupScreen(
                                    onBackClick = { currentScreen = AppScreen.Home },
                                    onSendToWatch = { setup ->
                                        matchSetupSender.sendMatchSetup(
                                            setup = setup,
                                            onSuccess = {
                                                activeMatchSetup = setup
                                                currentScreen = AppScreen.LiveMatch

                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Match setup sent to watch")
                                                }
                                            },
                                            onFailure = {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Unable to send setup to watch")
                                                }
                                            }
                                        )
                                    }
                                )
                            }

                            AppScreen.LiveMatch -> {
                                // Safety Check: Only show LiveScoreScreen if we have a valid configuration
                                activeMatchSetup?.let { setup ->
                                    LiveScoreScreen(
                                        setup = setup,
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