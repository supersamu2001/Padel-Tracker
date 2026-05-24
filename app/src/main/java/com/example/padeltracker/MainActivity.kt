package com.example.padeltracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.padeltracker.data.AppDatabase
import com.example.padeltracker.data.HistoryRepository
import com.example.padeltracker.data.MatchRecord
import com.example.padeltracker.shared.MatchSetup
import com.example.padeltracker.shared.communication.WearPaths
import com.example.padeltracker.shared.experiment.ExperimentConfig
import com.example.padeltracker.shared.debug.DebugLogger
import com.example.padeltracker.ui.screens.*
import com.example.padeltracker.ui.theme.*
import com.example.padeltracker.wear.PhoneMatchEndedEventBus
import com.example.padeltracker.wear.WearMatchSetupSender
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Navigation Enum
enum class AppScreen { Home, Setup, History, LiveMatch, Analysis }

/**
 * Manage all the UI and the navigation among all the screens
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // set variables that keep information about the system
        setContent {
            PadelTrackerTheme {
                // NAVIGATION STATE
                // keep track of in which screen is the user
                var currentScreen by remember { mutableStateOf(AppScreen.Home) }

                // CONNECTIVITY STATE
                // Tell if the watch is connected to the phone
                var isWatchConnected by remember { mutableStateOf(false) }

                var isCheckingWatch by remember { mutableStateOf(false) }

                // DATA STATES
                // contains the infos about the match
                // MatchSetup => set up to initialize the match
                var activeMatchSetup by remember { mutableStateOf<MatchSetup?>(null) }
                // MatchRecord => class representing a single match in the database
                var selectedMatchForAnalysis by remember { mutableStateOf<MatchRecord?>(null) }

                // Database and Repository
                val database = remember { AppDatabase.getDatabase(this@MainActivity) }
                val repository = remember { HistoryRepository(database.matchDao()) }

                // Persistent list from Room
                val matchHistory by repository.getAllMatches().collectAsState(initial = emptyList())

                // SnackBar: small black notification that appears at the bottom of the screen
                // snackbarHostState manages this notifications
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                val matchSetupSender = remember {
                    WearMatchSetupSender(this@MainActivity)
                }
                val experimentConfig = remember { ExperimentConfig() }

                // Listener that when receives the signal that the match is ended, retrieve data just saved from the latest match,
                // wait a bit and then move them into the AnalysisScreen
                LaunchedEffect(Unit) {
                    PhoneMatchEndedEventBus.events.collect { endedAt ->
                        if (experimentConfig.debugMode) {
                            DebugLogger.d("PHONE_MATCH_ENDED", "Match ended event received: $endedAt")
                        }

                        // Wait a bit for the DB to be written by the Service
                        delay(700)
                        
                        // Get the latest match from DB
                        val latestMatch = repository.getAllMatches().first().firstOrNull()
                        
                        if (currentScreen == AppScreen.LiveMatch || currentScreen == AppScreen.Home) {
                            selectedMatchForAnalysis = latestMatch
                            currentScreen = AppScreen.Analysis
                            scope.launch {
                                snackbarHostState.showSnackbar("Match ended! Data saved.")
                            }
                        }
                    }
                }

                // Check if the app is installed and reachable on the watch. Called when the user
                // wants to go to the screen where he set up the match
                fun checkWatchAndOpenSetup() {
                    if (isCheckingWatch) return
                    isCheckingWatch = true

                    Wearable.getCapabilityClient(this@MainActivity)
                        .getCapability(
                            WearPaths.WATCH_CAPABILITY,
                            CapabilityClient.FILTER_REACHABLE
                        )
                        .addOnSuccessListener { capabilityInfo ->
                            isCheckingWatch = false
                            if (capabilityInfo.nodes.isNotEmpty()) {
                                isWatchConnected = true
                                currentScreen = AppScreen.Setup
                            } else {
                                isWatchConnected = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("No Padel Tracker watch connected")
                                }
                            }
                        }
                        .addOnFailureListener { error ->
                            isCheckingWatch = false
                            isWatchConnected = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Unable to check watch connection")
                            }
                        }
                }

                // INITIAL CHECK WHEN APP STARTS
                LaunchedEffect(Unit) {
                    Wearable.getCapabilityClient(this@MainActivity)
                        .getCapability(
                            WearPaths.WATCH_CAPABILITY,
                            CapabilityClient.FILTER_REACHABLE
                        )
                        .addOnSuccessListener { capabilityInfo ->
                            isWatchConnected = capabilityInfo.nodes.isNotEmpty()
                        }
                        .addOnFailureListener {
                            isWatchConnected = false
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
                        // Shows the correct screen based on the value of "currentScreen"
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
                                activeMatchSetup?.let { setup ->
                                    LiveScoreScreen(
                                        setup = setup,
                                        onFinish = {
                                            // Optional: Local finish if watch doesn't send event
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
                                        currentScreen = AppScreen.History
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
                                    },
                                    onDeleteMatch = { match ->
                                        scope.launch {
                                            repository.deleteMatch(match)
                                        }
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
