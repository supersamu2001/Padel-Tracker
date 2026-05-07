package com.example.padeltracker.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton object to track the status of data arriving from the watch.
 */

object SensorStatusState {
    private val _lastMessageReceived = MutableStateFlow<Long?>(null)
    val lastMessageReceived = _lastMessageReceived.asStateFlow()

    // Aggiungiamo i flussi per i valori dei sensori
    private val _lastAccValues = MutableStateFlow(floatArrayOf(0f, 0f, 0f))
    val lastAccValues = _lastAccValues.asStateFlow()

    private val _lastGyroValues = MutableStateFlow(floatArrayOf(0f, 0f, 0f))
    val lastGyroValues = _lastGyroValues.asStateFlow()

    fun updateData(type: Int, x: Float, y: Float, z: Float) {
        _lastMessageReceived.value = System.currentTimeMillis()
        if (type == android.hardware.Sensor.TYPE_ACCELEROMETER) {
            _lastAccValues.value = floatArrayOf(x, y, z)
        } else {
            _lastGyroValues.value = floatArrayOf(x, y, z)
        }
    }
}
