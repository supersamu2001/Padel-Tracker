package com.example.padeltracker.ml

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Singleton object to hold the live counts of detected shots.
 * The SensorDataListenerService updates these values, and the UI observes them.
 */
object ShotDetectionState {
    private val _shotCounts = MutableStateFlow(MapShotCounts())
    val shotCounts: StateFlow<MapShotCounts> = _shotCounts.asStateFlow()

    data class MapShotCounts(
        val forehands: Int = 0,
        val backhands: Int = 0,
        val forehandLobs: Int = 0,
        val backhandLobs: Int = 0,
        val smashes: Int = 0,
        val services: Int = 0
    )

    fun recordShot(type: ShotType) {
        _shotCounts.update { current ->
            when (type) {
                ShotType.FOREHAND -> current.copy(forehands = current.forehands + 1)
                ShotType.BACKHAND -> current.copy(backhands = current.backhands + 1)
                ShotType.FOREHAND_LOB -> current.copy(forehandLobs = current.forehandLobs + 1)
                ShotType.BACKHAND_LOB -> current.copy(backhandLobs = current.backhandLobs + 1)
                ShotType.SMASH -> current.copy(smashes = current.smashes + 1)
                ShotType.SERVICE -> current.copy(services = current.services + 1)
                ShotType.UNKNOWN -> current
            }
        }
    }

    fun reset() {
        _shotCounts.value = MapShotCounts()
    }
}
