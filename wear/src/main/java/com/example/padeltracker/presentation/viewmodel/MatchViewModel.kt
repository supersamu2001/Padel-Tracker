package com.example.padeltracker.presentation.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.wearable.Wearable
import androidx.lifecycle.AndroidViewModel
import com.example.padeltracker.presentation.communication.MatchEndedSender
import com.example.padeltracker.presentation.data.PendingMatchSetupStore
import com.example.padeltracker.presentation.model.MatchStatus
import com.example.padeltracker.presentation.model.ScoreTrackerState
import com.example.padeltracker.presentation.model.TeamId
import com.example.padeltracker.presentation.model.toDomain
import com.example.padeltracker.presentation.scoring.PadelScoreEngine
import com.example.padeltracker.presentation.sensors.WearSensorManager
import com.example.padeltracker.shared.communication.WearPaths
import com.example.padeltracker.shared.debug.DebugLogger

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
    private var matchStartTimeMs: Long = 0L
    private val _state = mutableStateOf(createInitialState())
    val state: State<ScoreTrackerState> = _state

    // HEARTBEAT
    private val _heartRate = mutableStateOf(0.0)
    val heartRate: State<Double> = _heartRate

    private val hrHistoryBuilder = java.lang.StringBuilder()
    private var lastSavedTimestamp = 0L
    //private val sensorManager = WearSensorManager(application)

    // Connect manager with heartbeat of ViewModel
    private val sensorManager = WearSensorManager(application) { newRate ->
        _heartRate.value = newRate

        // silent counting every 5 sec
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSavedTimestamp >= 5000 && newRate > 0) {
            if (hrHistoryBuilder.isNotEmpty()) hrHistoryBuilder.append(",")
            hrHistoryBuilder.append(newRate.toInt())
            lastSavedTimestamp = currentTime
            DebugLogger.d("VIEW_MODEL_TEST", "Saved HR point: ${newRate.toInt()}")
        }
        DebugLogger.d("VIEW_MODEL_TEST", "ViewModel received new heart rate: $newRate")
    }
    private val matchEndedSender = MatchEndedSender(application)

    private val pendingSetupChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (pendingSetupStore.isPendingSetupKey(key)) {
                DebugLogger.d(TAG, "Pending setup change detected.")
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

            DebugLogger.d(
                TAG,
                "Loaded pending match setup: ${pendingSetup.matchId}"
            )
            DebugLogger.d(
                TAG,
                "Team A: ${match.teamA.players.joinToString { it.name }}"
            )
            DebugLogger.d(
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
            DebugLogger.d(TAG, "No pending match setup found. Using default match.")
            currentMatchUsesPhoneSetup = false
            engine.createDefaultMatch()
        }
    }

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
            DebugLogger.d(TAG, "Pending setup not applied because a match is already active.")
            return
        }

        val pendingSetup = pendingSetupStore.consume()

        if (pendingSetup == null) {
            DebugLogger.d(TAG, "No pending setup to apply.")
            return
        }

        val match = pendingSetup.toDomain()

        DebugLogger.d(TAG, "Applied pending match setup: ${pendingSetup.matchId}")
        DebugLogger.d(TAG, "Team A: ${match.teamA.players.joinToString { it.name }}")
        DebugLogger.d(TAG, "Team B: ${match.teamB.players.joinToString { it.name }}")

        currentMatchUsesPhoneSetup = true

        _state.value = ScoreTrackerState(
            initialMatch = match,
            currentMatch = match,
            pointHistory = emptyList()
        )
    }

    // Broadcasts the live score to the phone whenever a point is scored
    private fun broadcastLiveScore() {
        val match = _state.value.currentMatch

        val scoreBuilder = java.lang.StringBuilder()
        match.completedSets.forEach { set ->
            scoreBuilder.append("${set.teamAGames}-${set.teamBGames}   ")
        }
        scoreBuilder.append("${match.currentSet.teamAGames}-${match.currentSet.teamBGames}")
        val currentScore = scoreBuilder.toString().trim()

        val payload = currentScore.toByteArray()
        val context = getApplication<Application>()

        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node ->
                Wearable.getMessageClient(context).sendMessage(node.id, WearPaths.LIVE_SCORE, payload)
            }
        }
    }

    private fun notifyMatchStartedOnPhone() {
        val context = getApplication<Application>()
        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node ->
                Wearable.getMessageClient(context).sendMessage(node.id, WearPaths.MATCH_STARTED, ByteArray(0))
            }
        }
    }

    private fun startSensorTrackingForActiveMatch() {
        matchStartTimeMs = System.currentTimeMillis()
        sensorManager.startTracking()
        notifyMatchStartedOnPhone()
    }

    /**
     * Moves the match status to server selection.
     */
    fun startMatch() {
        matchEndedMessageSent = false
        resetHeartRateHistory()

        _state.value = engine.startMatch(_state.value)

        updateSensorScoreMarker()
    }

    /**
     * Selects the initial serving team and begins match scoring.
     */
    fun selectInitialServer(teamId: TeamId) {
        val previousStatus = _state.value.currentMatch.status
        _state.value = engine.selectInitialServer(_state.value, teamId)
        updateSensorScoreMarker()

        if (previousStatus != MatchStatus.IN_PROGRESS &&
            _state.value.currentMatch.status == MatchStatus.IN_PROGRESS
        ) {
            startSensorTrackingForActiveMatch()
        }
    }

    /**
     * Awards a point to the specified team.
     */
    fun addPoint(teamId: TeamId) {
        _state.value = engine.addPoint(_state.value, teamId)

        updateSensorScoreMarker()

        sensorManager.flushPendingSensorBatches()

        if (_state.value.currentMatch.status == MatchStatus.FINISHED) {
            sensorManager.stopTracking()
        }

        broadcastLiveScore()
    }

    /**
     * Undoes the last point recorded.
     */
    fun undo() {
        val previousStatus = _state.value.currentMatch.status
        _state.value = engine.undo(_state.value)
        updateSensorScoreMarker()

        val newStatus = _state.value.currentMatch.status
        if (previousStatus == MatchStatus.FINISHED && newStatus == MatchStatus.IN_PROGRESS) {
            sensorManager.startTracking()
        } else if (previousStatus == MatchStatus.IN_PROGRESS && newStatus == MatchStatus.SELECTING_SERVER) {
            sensorManager.stopTracking()
        }

        broadcastLiveScore()
    }

    /**
     * Resets the match to the latest pending setup if available, otherwise to the default match.
     */
    fun resetMatch() {
        matchEndedMessageSent = false
        resetHeartRateHistory()
        _state.value = createInitialState()

        updateSensorScoreMarker()

        // stops the collection of data from sensors
        sensorManager.stopTracking()
    }

    /**
     * Ends the match early manually.
     */
    fun endMatchEarly() {
        _state.value = engine.endMatchEarly(_state.value)
        updateSensorScoreMarker()

        if (_state.value.currentMatch.status == MatchStatus.FINISHED) {
            sensorManager.stopTracking()
        }
    }

    /**
     * Confirms the match end, sends notification to the phone and resets state.
     */
    fun confirmEndMatch() {
        val match = _state.value.currentMatch

        if (match.status != MatchStatus.FINISHED) {
            DebugLogger.d(TAG, "End match confirmation ignored because match is not finished.")
            return
        }

        if (!currentMatchUsesPhoneSetup) {
            DebugLogger.d(TAG, "Default Wear match ended locally. No phone notification sent.")
            resetMatch()
            return
        }

        if (matchEndedMessageSent) {
            DebugLogger.d(TAG, "Match ended message already sent.")
            return
        }

        matchEndedMessageSent = true

        DebugLogger.d(TAG, "User confirmed end match. Sending match ended message.")
        // Stop sensor tracking before sending match-end data.
        // This prevents sensor packets, especially RAW_TO_PHONE packets,
        // from delaying the match-ended communication.
        sensorManager.stopTracking()

        // 1. Names
        val teamA = match.teamA.players.joinToString(" & ") { it.name }
        val teamB = match.teamB.players.joinToString(" & ") { it.name }

        // 2. Score
        val scoreBuilder = java.lang.StringBuilder()
        match.completedSets.forEach { set ->
            scoreBuilder.append("${set.teamAGames}-${set.teamBGames} ")
        }
        scoreBuilder.append("${match.currentSet.teamAGames}-${match.currentSet.teamBGames}")
        val finalScore = scoreBuilder.toString().trim()

        // 3. Winner
        val teamASets = match.completedSets.count { it.teamAGames > it.teamBGames }
        val teamBSets = match.completedSets.count { it.teamBGames > it.teamAGames }
        val winnerName = if (teamASets > teamBSets) "Team A" else if (teamBSets > teamASets) "Team B" else "Draw"

        // 4. Av heartbeat
        val historyString = hrHistoryBuilder.toString()
        val hrList = historyString.split(",").filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }
        val avgHr = if (hrList.isNotEmpty()) hrList.average().toInt() else 0

        // 5. timer for game
        val durationMs = if (matchStartTimeMs > 0) System.currentTimeMillis() - matchStartTimeMs else 0L
        val minutes = (durationMs / 1000) / 60
        val seconds = (durationMs / 1000) % 60
        val finalDuration = String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)

        matchEndedSender.sendMatchEnded(
            score = finalScore,
            avgHeartRate = avgHr,
            teamAPlayers = teamA,
            teamBPlayers = teamB,
            winner = winnerName,
            duration = finalDuration,
            heartRateHistory = historyString,
            tournamentName = match.tournamentName
        )

        resetMatch()
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.stopTracking()
        pendingSetupStore.unregisterChangeListener(pendingSetupChangeListener)
    }

    private fun resetHeartRateHistory() {
        _heartRate.value = 0.0
        hrHistoryBuilder.clear()
        lastSavedTimestamp = 0L
    }

    companion object {
        private const val TAG = "MATCH_VIEW_MODEL"
    }
}
