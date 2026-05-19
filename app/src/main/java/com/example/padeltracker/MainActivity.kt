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
import com.example.padeltracker.shared.communication.WearPaths
import com.example.padeltracker.ui.screens.*
import com.example.padeltracker.ui.theme.*
import com.example.padeltracker.wear.PhoneMatchEndedEventBus
import com.example.padeltracker.wear.WearMatchSetupSender
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMapItem
import kotlinx.coroutines.launch


// Navigation Enum
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

                // Temporary list for History
                val matchHistory = remember { mutableStateListOf<MatchRecord>() }

                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                val matchSetupSender = remember {
                    WearMatchSetupSender(this@MainActivity)
                }

                // NEW,1. Starts DataClient
                val dataClient = Wearable.getDataClient(this@MainActivity)

                // always "listen" data from the game
                DisposableEffect(Unit) {
                    val dataListener = DataClient.OnDataChangedListener { dataEvents ->
                        dataEvents.forEach { event ->
                            if (event.type == DataEvent.TYPE_CHANGED &&
                                event.dataItem.uri.path == WearPaths.MATCH_RESULT
                            ) {
                                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

                                val match = MatchRecord(
                                    date = dataMap.getString("date") ?: "",
                                    duration = dataMap.getString("duration") ?: "",
                                    score = dataMap.getString("score") ?: "",
                                    avgHeartRate = dataMap.getInt("avgHeartRate"),
                                    heartRateHistory = dataMap.getString("heartRateHistory") ?: "",
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

                                Log.d("DATA_SYNC", "Match received! HeartRate History: ${match.heartRateHistory}")

                                // put data in the screen and history
                                selectedMatchForAnalysis = match
                                matchHistory.add(0, match)
                                currentScreen = AppScreen.Analysis
                            }
                        }
                    }
                    dataClient.addListener(dataListener)
                    onDispose {
                        dataClient.removeListener(dataListener)
                    }
                }

                // 2. Listen for match-ended messages from Wear
                LaunchedEffect(Unit) {
                    PhoneMatchEndedEventBus.events.collect { endedAt ->
                        Log.d("PHONE_MATCH_ENDED", "Match ended event received: $endedAt")

                        if (currentScreen == AppScreen.LiveMatch) {

                            currentScreen = AppScreen.Analysis
                            scope.launch {
                                snackbarHostState.showSnackbar("Match ended from watch! Loading data...")
                            }
                        }
                    }
                }

                // 3. WATCH CHECK FUNCTION
                fun checkWatchAndOpenSetup() {
                    if (isCheckingWatch) return
                    isCheckingWatch = true

                    Log.d("WATCH_DEBUG", "Starting watch capability check...")

                    Wearable.getCapabilityClient(this@MainActivity)
                        .getCapability(
                            WearPaths.WATCH_CAPABILITY,
                            CapabilityClient.FILTER_REACHABLE
                        )
                        .addOnSuccessListener { capabilityInfo ->
                            isCheckingWatch = false
                            if (capabilityInfo.nodes.isNotEmpty()) {
                                Log.d("WATCH_DEBUG", "Watch found. Opening setup.")
                                isWatchConnected = true
                                currentScreen = AppScreen.Setup
                            } else {
                                Log.d("WATCH_DEBUG", "No watch found.")
                                isWatchConnected = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("No Padel Tracker watch connected")
                                }
                            }
                        }
                        .addOnFailureListener { error ->
                            isCheckingWatch = false
                            isWatchConnected = false
                            Log.e("WATCH_DEBUG", "Connection check failed", error)
                            scope.launch {
                                snackbarHostState.showSnackbar("Unable to check watch connection")
                            }
                        }
                }

                // 4. INITIAL CHECK WHEN APP STARTS
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
                        when (currentScreen) {
                            AppScreen.Home -> {
                                HomeScreen(
                                    isConnected = isWatchConnected, // Real status
                                    onNewGameClick = { checkWatchAndOpenSetup() }, // Real check
                                    onHistoryClick = { currentScreen = AppScreen.History }
                                )
                            }

                            AppScreen.Setup -> {
                                MatchSetupScreen(
                                    onBackClick = { currentScreen = AppScreen.Home },
                                    onSendToWatch = { setup ->
                                        // Επαναφορά της αποστολής στο ρολόι
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
                                    },
                                    onDeleteMatch = { match ->
                                        matchHistory.removeIf { it.id == match.id }                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}