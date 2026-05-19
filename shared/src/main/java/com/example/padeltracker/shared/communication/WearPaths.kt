package com.example.padeltracker.shared.communication

/**
 * Centralized Wear OS Data Layer paths.
 *
 * Keep all phone-watch communication paths here to avoid hardcoded strings
 * spread across the app and wear modules.
 */
object WearPaths {

    // Capability
    const val WATCH_CAPABILITY = "padel_tracker_watch"

    // Match setup and lifecycle
    const val MATCH_SETUP = "/match/setup"
    const val MATCH_STARTED = "/match_started"
    const val LIVE_SCORE = "/live_score"
    const val MATCH_ENDED = "/match/ended"
    const val MATCH_STATS = "/match_stats"
    const val MATCH_RESULT = "/match_result"

    // Sensor data
    const val SENSOR_RAW = "/sensor_data"
    const val SENSOR_SHOT = "/shot_data"
    const val HEART_RATE = "/heart_rate_data"
}