package com.example.padeltracker.shared.experiment

/**
 * Central configuration for sensor-based experiments.
 *
 * Keep all values that must be shared between watch and phone here,
 * so sampling, shot detection, feature extraction, and logging do not
 * rely on duplicated constants.
 */
data class ExperimentConfig(
    val mode: ExperimentMode = ExperimentMode.DATA_COLLECTION,

    /**
     * Sensor sampling frequency.
     *
     * 25 Hz means one sample every 40 ms.
     */
    val samplingHz: Int = 25,

    /**
     * Number of samples kept before the trigger sample.
     *
     * With the default value, the shot window contains 25 samples
     * before the detected shot.
     */
    val preTriggerSamples: Int = 25,

    /**
     * Number of samples collected after the trigger sample.
     *
     * With the default value, the shot window contains 25 samples
     * after the detected shot.
     */
    val postTriggerSamples: Int = 25,

    /**
     * Acceleration threshold used for shot detection.
     *
     * 2G = 2 * 9.81 m/s² = 19.62 m/s².
     */
    //val accelerationThresholdMps2: Float = 2f * 9.81f,

    /**
     * Gyroscope threshold used for shot detection.
     */
    //val gyroscopeThresholdRadS: Float = 5.0f,

    // for test on emulator purpose
    val accelerationThresholdMps2: Float = 0.8f,
    val gyroscopeThresholdRadS: Float = 0.4f,

    /**
     * If true, both acceleration and gyroscope thresholds must be exceeded.
     * If false, acceleration OR gyroscope is enough.
     */
    val requireBothSensorsForShot: Boolean = true,

    /**
     * Shows a toast when a shot is detected.
     * Mainly useful during debugging or dataset collection.
     */
    val showShotDetectionToast: Boolean = true
) {
    /**
     * Android SensorManager sampling period in microseconds.
     *
     * This value is passed directly to SensorManager.registerListener().
     */
    val sensorDelayMicros: Int
        get() = 1_000_000 / samplingHz

    /**
     * Total number of paired IMU samples in a shot window.
     *
     * Formula:
     * pre-trigger samples + trigger sample + post-trigger samples.
     *
     * Default:
     * 25 + 1 + 25 = 51 samples.
     */
    val totalShotSamples: Int
        get() = preTriggerSamples + 1 + postTriggerSamples

    /**
     * Approximate shot window duration in milliseconds.
     *
     * Default:
     * 51 samples at 25 Hz = 2040 ms.
     */
    val shotWindowDurationMs: Int
        get() = totalShotSamples * 1000 / samplingHz
}