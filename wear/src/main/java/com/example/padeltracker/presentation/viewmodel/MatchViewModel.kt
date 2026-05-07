package com.example.padeltracker.presentation.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.example.padeltracker.presentation.model.ScoreTrackerState
import com.example.padeltracker.presentation.model.TeamId
import com.example.padeltracker.presentation.scoring.PadelScoreEngine
import com.example.padeltracker.presentation.sensors.WearSensorManager

/**
 * ViewModel that manages the padel match state and delegates logic to the PadelScoreEngine.
 */
class MatchViewModel @JvmOverloads constructor(
    application: Application,
    private val engine: PadelScoreEngine = PadelScoreEngine()
) : AndroidViewModel(application) {

    private val _state = mutableStateOf(engine.createDefaultMatch())
    val state: State<ScoreTrackerState> = _state

    private val sensorManager = WearSensorManager(application)

    /**
     * Moves the match status to server selection.
     */
    fun startMatch() {
        _state.value = engine.startMatch(_state.value)

        // start the collection of data from sensors
        sensorManager.startTracking()
    }

    /**
     * Selects the initial serving team and begins match scoring.
     */
    fun selectInitialServer(teamId: TeamId) {
        _state.value = engine.selectInitialServer(_state.value, teamId)
    }

    /**
     * Awards a point to the specified team.
     */
    fun addPoint(teamId: TeamId) {
        _state.value = engine.addPoint(_state.value, teamId)
    }

    /**
     * Undoes the last point recorded.
     */
    fun undo() {
        _state.value = engine.undo(_state.value)
    }

    /**
     * Resets the match to its initial state with placeholder players.
     */
    fun resetMatch() {
        _state.value = engine.createDefaultMatch()

        // stops the collection of data from sensors
        sensorManager.stopTracking()
    }

    /**
     * Ends the match early manually.
     */
    fun endMatchEarly() {
        _state.value = engine.endMatchEarly(_state.value)
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.stopTracking()
    }
}
