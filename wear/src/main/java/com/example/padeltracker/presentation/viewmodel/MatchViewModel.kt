package com.example.padeltracker.presentation.viewmodel

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableDoubleStateOf
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
    private var forehandsCount = 0
    private var backhandsCount = 0
    private var smashesCount = 0
    private var servicesCount = 0
    private var forehandLobsCount = 0
    private var backhandLobsCount = 0
    private val _state = mutableStateOf(createInitialState())
    val state: State<ScoreTrackerState> = _state

    // HEARTBEAT
    private val _heartRate = mutableStateOf(0.0)
    val heartRate: State<Double> = _heartRate

    private val hrHistoryBuilder = java.lang.StringBuilder()
    private var lastSavedTimestamp = 0L
    //private val sensorManager = WearSensorManager(application)

    // 2. Connect manager with heartbeat of ViewModel
    private val sensorManager = WearSensorManager(application) { newRate ->
        _heartRate.value = newRate

        // silent counting every 5 sec
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSavedTimestamp >= 5000 && newRate > 0) {
            if (hrHistoryBuilder.isNotEmpty()) hrHistoryBuilder.append(",")
            hrHistoryBuilder.append(newRate.toInt())
            lastSavedTimestamp = currentTime
            Log.d("VIEW_MODEL_TEST", "Saved HR point: ${newRate.toInt()}")
        }
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

    /**
     * Moves the match status to server selection.
     */
    fun startMatch() {
        matchEndedMessageSent = false

        matchStartTimeMs = System.currentTimeMillis()

        // new, zero for the start of the game
        forehandsCount = 0
        backhandsCount = 0
        smashesCount = 0
        servicesCount = 0
        forehandLobsCount = 0
        backhandLobsCount = 0

        _state.value = engine.startMatch(_state.value)

        updateSensorScoreMarker()
        // start the collection of data from sensors
        sensorManager.startTracking()

        // Sends a message to the phone stating that the match has officially started
        val context = getApplication<Application>()
        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node ->
                Wearable.getMessageClient(context).sendMessage(node.id, WearPaths.MATCH_STARTED, ByteArray(0))
            }
        }
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

        updateSensorScoreMarker()

        broadcastLiveScore()
    }

    /**
     * Undoes the last point recorded.
     */
    fun undo() {
        _state.value = engine.undo(_state.value)
        //simplify labeling
        updateSensorScoreMarker()
        broadcastLiveScore()
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
        updateSensorScoreMarker()
    }

    // NEW: A helper template function for future use.
    // Once the team implements shot recognition, this function should be called
    // passing the shot type in order to increment the correct counter.
    fun logDetectedShot(shotType: String) {
        when (shotType.lowercase()) {
            "forehand" -> forehandsCount++
            "backhand" -> backhandsCount++
            "smash" -> smashesCount++
            "service" -> servicesCount++
            "forehand_lob" -> forehandLobsCount++
            "backhand_lob" -> backhandLobsCount++
        }
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


        // Send the history to the sender
        matchEndedSender.sendMatchEnded(heartRateHistory = historyString,
            avgHeartRate = avgHr,
            teamAPlayers = teamA,
            teamBPlayers = teamB,
            score = finalScore,
            winner = winnerName,
            duration = finalDuration,
            forehands = forehandsCount,
            backhands = backhandsCount,
            smashes = smashesCount,
            services = servicesCount,
            forehandLobs = forehandLobsCount,
            backhandLobs = backhandLobsCount)

        // ... (όλα τα προηγούμενα της συνάρτησης) ...

        // Αυτό το έχει ήδη γράψει η άλλη κοπέλα:
        matchEndedSender.sendMatchEnded(
            heartRateHistory = historyString,
            avgHeartRate = avgHr,
            teamAPlayers = teamA,
            teamBPlayers = teamB,
            score = finalScore,
            winner = winnerName,
            duration = finalDuration,
            forehands = forehandsCount,
            backhands = backhandsCount,
            smashes = smashesCount,
            services = servicesCount,
            forehandLobs = forehandLobsCount,
            backhandLobs = backhandLobsCount
        )


        val info = "$finalScore|$avgHr|$forehandsCount|$backhandsCount|$smashesCount|$servicesCount|$forehandLobsCount|$backhandLobsCount|$teamA|$teamB|$winnerName|$finalDuration|$historyString"
        val wearContext = getApplication<Application>()

        Wearable.getNodeClient(wearContext).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node ->
                Wearable.getMessageClient(wearContext).sendMessage(
                    node.id,
                    WearPaths.MATCH_STATS,
                    info.toByteArray(Charsets.UTF_8)
                )
            }
        }

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
