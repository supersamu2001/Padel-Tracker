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
import com.example.padeltracker.data.MatchRecord
import com.example.padeltracker.shared.MatchSetup
import com.example.padeltracker.shared.WearCommunicationConstants
import com.example.padeltracker.ui.screens.*
import com.example.padeltracker.ui.theme.*
import com.example.padeltracker.wear.PhoneMatchEndedEventBus
import com.example.padeltracker.wear.WearMatchSetupSender
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch

// Navigation Enum (Now includes Analysis)
enum class AppScreen { Home, Setup, History, LiveMatch, Analysis }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PadelTrackerTheme {
                // NAVIGATION STATE
                var currentScreen by remember { mutableStateOf(AppScreen.Home) }

                // CONNECTIVITY STATE
                var isWatchConnected by remember { mutableStateOf(false) }
                var isCheckingWatch by remember { mutableStateOf(false) }

                // DATA STATES
                var activeMatchSetup by remember { mutableStateOf<MatchSetup?>(null) }
                var selectedMatchForAnalysis by remember { mutableStateOf<MatchRecord?>(null) }

                // Temporary list for History (until we add the database back)
                val matchHistory = remember { mutableStateListOf<MatchRecord>() }

                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                val matchSetupSender = remember {
                    WearMatchSetupSender(this@MainActivity)
                }

                // Listen for match-ended messages coming from the Wear app
                LaunchedEffect(Unit) {
                    PhoneMatchEndedEventBus.events.collect { endedAt ->
                        Log.d(
                            "PHONE_MATCH_ENDED",
                            "Match ended event received in MainActivity: $endedAt"
                        )

                        if (currentScreen == AppScreen.LiveMatch) {
                            selectedMatchForAnalysis = null
                            currentScreen = AppScreen.Analysis

                            scope.launch {
                                snackbarHostState.showSnackbar("Match ended from watch")
                            }
                        } else {
                            Log.d(
                                "PHONE_MATCH_ENDED",
                                "Match ended event ignored because currentScreen=$currentScreen"
                            )
                        }
                    }
                }

                // WATCH CHECK FUNCTION (From your old Main)
                fun checkWatchAndOpenSetup() {
                    if (isCheckingWatch) return
                    isCheckingWatch = true

                    Log.d("WATCH_DEBUG", "Starting watch capability check...")

                    Wearable.getCapabilityClient(this@MainActivity)
                        .getCapability(
                            WearCommunicationConstants.WATCH_CAPABILITY,
                            CapabilityClient.FILTER_REACHABLE
                        )
                        .addOnSuccessListener { capabilityInfo ->
                            isCheckingWatch = false
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

                // Keep the HomeScreen watch status updated while the app is open
                val watchCapabilityListener = remember {
                    CapabilityClient.OnCapabilityChangedListener { capabilityInfo ->
                        val hasReachableWatch = capabilityInfo.nodes.isNotEmpty()

                        Log.d(
                            "WATCH_DEBUG",
                            "Capability changed. hasReachableWatch=$hasReachableWatch"
                        )

                        isWatchConnected = hasReachableWatch
                        isCheckingWatch = false
                    }
                }

                DisposableEffect(Unit) {
                    val capabilityClient = Wearable.getCapabilityClient(this@MainActivity)

                    // Initial status check
                    isCheckingWatch = true

                    capabilityClient
                        .getCapability(
                            WearCommunicationConstants.WATCH_CAPABILITY,
                            CapabilityClient.FILTER_REACHABLE
                        )
                        .addOnSuccessListener { capabilityInfo ->
                            isWatchConnected = capabilityInfo.nodes.isNotEmpty()
                            isCheckingWatch = false
                        }
                        .addOnFailureListener { error ->
                            Log.e("WATCH_DEBUG", "Initial capability check failed", error)
                            isWatchConnected = false
                            isCheckingWatch = false
                        }

                    // Live updates while the app is open
                    capabilityClient
                        .addListener(
                            watchCapabilityListener,
                            WearCommunicationConstants.WATCH_CAPABILITY
                        )
                        .addOnSuccessListener {
                            Log.d("WATCH_DEBUG", "Watch capability listener registered")
                        }
                        .addOnFailureListener { error ->
                            Log.e("WATCH_DEBUG", "Failed to register capability listener", error)
                        }

                    onDispose {
                        capabilityClient.removeListener(
                            watchCapabilityListener,
                            WearCommunicationConstants.WATCH_CAPABILITY
                        )

                        Log.d("WATCH_DEBUG", "Watch capability listener removed")
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .background(BackgroundBeige)
                    ) {
                        when (currentScreen) {
                            AppScreen.Home -> {
                                HomeScreen(
                                    isConnected = isWatchConnected, // Now reads the real watch status!
                                    onNewGameClick = { checkWatchAndOpenSetup() }, // Checks before letting you enter
                                    onHistoryClick = { currentScreen = AppScreen.History }
                                )
                            }

                            AppScreen.Setup -> {
                                MatchSetupScreen(
                                    onBackClick = { currentScreen = AppScreen.Home },
                                    onSendToWatch = { setup ->
                                        // Sends data to the watch before going to Live Match
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
                                activeMatchSetup?.let { setup ->
                                    LiveScoreScreen(
                                        setup = setup,
                                        onFinish = {
                                            // Go to Analysis (or History) when the match finishes
                                            currentScreen = AppScreen.Analysis
                                        }
                                    )
                                } ?: run { currentScreen = AppScreen.Home }
                            }

                            AppScreen.Analysis -> {
                                GameAnalysisScreen(
                                    record = selectedMatchForAnalysis,
                                    setup = activeMatchSetup,
                                    onGoHome = {
                                        selectedMatchForAnalysis = null
                                        currentScreen = AppScreen.Home
                                    }
                                )
                            }

                            AppScreen.History -> {
                                HistoryScreen(
                                    matches = matchHistory,
                                    onBackClick = { currentScreen = AppScreen.Home },
                                    onMatchClick = { match ->
                                        selectedMatchForAnalysis = match
                                        currentScreen = AppScreen.Analysis
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}