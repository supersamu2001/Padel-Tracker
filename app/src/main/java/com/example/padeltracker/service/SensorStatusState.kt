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

    // Stato per l'ultimo colpo rilevato
    private val _lastShotTime = MutableStateFlow<Long?>(null)
    val lastShotTime = _lastShotTime.asStateFlow()

    private val _lastShotSamplesCount = MutableStateFlow(0)
    val lastShotSamplesCount = _lastShotSamplesCount.asStateFlow()

    // Called each time a new data sensor is retrieved
    fun updateData(type: Int, x: Float, y: Float, z: Float) {
        _lastMessageReceived.value = System.currentTimeMillis()
        if (type == android.hardware.Sensor.TYPE_ACCELEROMETER) {
            _lastAccValues.value = floatArrayOf(x, y, z)
        } else {
            _lastGyroValues.value = floatArrayOf(x, y, z)
        }
    }

    // Called each time a new shot batch is received by the phone
    fun recordShot(samplesCount: Int) {
        _lastShotTime.value = System.currentTimeMillis()
        _lastShotSamplesCount.value = samplesCount
    }
}
