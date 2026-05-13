package com.example.padeltracker.presentation.viewmodel

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.lifecycle.AndroidViewModel
import com.example.padeltracker.presentation.communication.MatchEndedSender
import com.example.padeltracker.presentation.data.PendingMatchSetupStore
import com.example.padeltracker.presentation.model.MatchStatus
import com.example.padeltracker.presentation.model.ScoreTrackerState
import com.example.padeltracker.presentation.model.TeamId
import com.example.padeltracker.presentation.model.toDomain
import com.example.padeltracker.presentation.scoring.PadelScoreEngine
import com.example.padeltracker.presentation.sensors.WearSensorManager

/**
 * ViewModel that manages the padel match state and delegates logic to the PadelScoreEngine.
 */
class MatchViewModel @JvmOverloads constructor(
    application: Application,
    private val engine: PadelScoreEngine = PadelScoreEngine()
) : AndroidViewModel(application) {

    private val pendingSetupStore = PendingMatchSetupStore(application)
    private var matchEndedMessageSent = false
    private var currentMatchUsesPhoneSetup = false
    private val _state = mutableStateOf(createInitialState())
    val state: State<ScoreTrackerState> = _state

    // HEARTBEAT
    private val _heartRate = mutableStateOf(0.0)
    val heartRate: State<Double> = _heartRate

    //private val sensorManager = WearSensorManager(application)

    // 2. Εδώ συνδέσαμε τον Manager με τον παλμό του ViewModel
    private val sensorManager = WearSensorManager(application) { newRate ->
        _heartRate.value = newRate

        // TESTTTTTTTTTT
        Log.d("VIEW_MODEL_TEST", "🚀 EXOUME ViewModel NEOS PALMOS: $newRate")
    }
    private val matchEndedSender = MatchEndedSender(application)

    private val pendingSetupChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (pendingSetupStore.isPendingSetupKey(key)) {
                Log.d(TAG, "Pending setup change detected.")
                applyPendingSetupIfAvailable()
            }
        }

    init {
        pendingSetupStore.registerChangeListener(pendingSetupChangeListener)
    }

    private fun createInitialState(): ScoreTrackerState {
        val pendingSetup = pendingSetupStore.consume()

        return if (pendingSetup != null) {
            val match = pendingSetup.toDomain()

            Log.d(
                TAG,
                "Loaded pending match setup: ${pendingSetup.matchId}"
            )
            Log.d(
                TAG,
                "Team A: ${match.teamA.players.joinToString { it.name }}"
            )
            Log.d(
                TAG,
                "Team B: ${match.teamB.players.joinToString { it.name }}"
            )

            currentMatchUsesPhoneSetup = true

            ScoreTrackerState(
                initialMatch = match,
                currentMatch = match,
                pointHistory = emptyList()
            )
        } else {
            Log.d(TAG, "No pending match setup found. Using default match.")
            currentMatchUsesPhoneSetup = false
            engine.createDefaultMatch()
        }
    }

    //simplify labeling
    private fun updateSensorScoreMarker() {
        val match = _state.value.currentMatch

        val teamASets = match.completedSets.count { it.teamAGames > it.teamBGames }
        val teamBSets = match.completedSets.count { it.teamBGames > it.teamAGames }

        sensorManager.updateScoreMarker(
            teamASets = teamASets,
            teamBSets = teamBSets,
            teamAGames = match.currentSet.teamAGames,
            teamBGames = match.currentSet.teamBGames
        )
    }
    private fun applyPendingSetupIfAvailable() {
        val currentState = _state.value

        if ((currentState.currentMatch.status != MatchStatus.NOT_STARTED && currentState.currentMatch.status != MatchStatus.WAITING_FOR_SETUP) ||
            currentState.pointHistory.isNotEmpty()
        ) {
            Log.d(TAG, "Pending setup not applied because a match is already active.")
            return
        }

        val pendingSetup = pendingSetupStore.consume()

        if (pendingSetup == null) {
            Log.d(TAG, "No pending setup to apply.")
            return
        }

        val match = pendingSetup.toDomain()

        Log.d(TAG, "Applied pending match setup: ${pendingSetup.matchId}")
        Log.d(TAG, "Team A: ${match.teamA.players.joinToString { it.name }}")
        Log.d(TAG, "Team B: ${match.teamB.players.joinToString { it.name }}")

        currentMatchUsesPhoneSetup = true

        _state.value = ScoreTrackerState(
            initialMatch = match,
            currentMatch = match,
            pointHistory = emptyList()
        )
    }

    /**
     * Moves the match status to server selection.
     */
    fun startMatch() {
        matchEndedMessageSent = false
        _state.value = engine.startMatch(_state.value)

        //simplify labeling
        updateSensorScoreMarker()
        // start the collection of data from sensors
        sensorManager.startTracking()
    }

    /**
     * Selects the initial serving team and begins match scoring.
     */
    fun selectInitialServer(teamId: TeamId) {
        _state.value = engine.selectInitialServer(_state.value, teamId)
        //simplify labeling
        updateSensorScoreMarker()
    }

    /**
     * Awards a point to the specified team.
     */
    fun addPoint(teamId: TeamId) {
        _state.value = engine.addPoint(_state.value, teamId)
        //simplify labeling
        updateSensorScoreMarker()
    }

    /**
     * Undoes the last point recorded.
     */
    fun undo() {
        _state.value = engine.undo(_state.value)
        //simplify labeling
        updateSensorScoreMarker()
    }

    /**
     * Resets the match to the latest pending setup if available, otherwise to the default match.
     */
    fun resetMatch() {
        matchEndedMessageSent = false
        _state.value = createInitialState()

        //simplify labeling
        updateSensorScoreMarker()

        // stops the collection of data from sensors
        sensorManager.stopTracking()
    }

    /**
     * Ends the match early manually.
     */
    fun endMatchEarly() {
        _state.value = engine.endMatchEarly(_state.value)

        //simplify labeling
        updateSensorScoreMarker()
    }

    /**
     * Confirms the match end, sends notification to the phone and resets state.
     */
    fun confirmEndMatch() {
        val match = _state.value.currentMatch

        if (match.status != MatchStatus.FINISHED) {
            Log.d(TAG, "End match confirmation ignored because match is not finished.")
            return
        }

        if (!currentMatchUsesPhoneSetup) {
            Log.d(TAG, "Default Wear match ended locally. No phone notification sent.")
            resetMatch()
            return
        }

        if (matchEndedMessageSent) {
            Log.d(TAG, "Match ended message already sent.")
            return
        }

        matchEndedMessageSent = true

        Log.d(TAG, "User confirmed end match. Sending match ended message.")
        matchEndedSender.sendMatchEnded()

        resetMatch()
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.stopTracking()
        pendingSetupStore.unregisterChangeListener(pendingSetupChangeListener)
    }

    companion object {
        private const val TAG = "MATCH_VIEW_MODEL"
    }
}
