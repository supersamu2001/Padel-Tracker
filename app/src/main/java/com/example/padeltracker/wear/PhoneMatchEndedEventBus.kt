package com.example.padeltracker.wear

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Simple in-app event bus used to notify the phone UI that the Wear match ended.
 */
object PhoneMatchEndedEventBus {
    private val _events = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun notifyMatchEnded(endedAt: Long = System.currentTimeMillis()) {
        _events.tryEmit(endedAt)
    }
}
