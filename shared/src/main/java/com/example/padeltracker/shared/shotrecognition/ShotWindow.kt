package com.example.padeltracker.shared.shotrecognition

import com.example.padeltracker.shared.sensors.PairedImuSample

/**
 * Represents the complete IMU window associated with a detected shot.
 *
 * With the default experiment configuration, a shot window contains:
 * 25 samples before the trigger,
 * 1 trigger sample,
 * 25 samples after the trigger,
 * for a total of 51 paired IMU samples.
 */
data class ShotWindow(
    val samples: List<PairedImuSample>,

    // Timestamp assigned when the shot window is created.
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val totalSamples: Int
        get() = samples.size
}
