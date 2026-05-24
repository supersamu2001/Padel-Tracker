package com.example.padeltracker.shared.debug

import android.util.Log
import com.example.padeltracker.shared.experiment.ExperimentConfig

object DebugLogger {
    private val config = ExperimentConfig()

    fun d(tag: String, message: String) {
        if (config.debugMode) {
            Log.d(tag, message)
        }
    }

    fun e(tag: String, message: String, error: Throwable? = null) {
        if (config.debugMode) {
            if (error == null) {
                Log.e(tag, message)
            } else {
                Log.e(tag, message, error)
            }
        }
    }
}
