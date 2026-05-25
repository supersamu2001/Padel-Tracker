package com.example.padeltracker.shared.experiment

/**
 * Central configuration for sensor-based experiments.
 *
 * Keep all values that must be shared between watch and phone here,
 * so sampling, shot detection, feature extraction, and logging do not rely on duplicated constants.
 */
data class ExperimentConfig(
    //val mode: ExperimentMode = ExperimentMode.DATA_COLLECTION,
    //val mode: ExperimentMode = ExperimentMode.RAW_TO_PHONE,
    val mode: ExperimentMode = ExperimentMode.FEATURES_TO_PHONE,
    //val mode: ExperimentMode = ExperimentMode.SHOT_TO_PHONE,
    // true -> log and debug feature, false everything disable for power consumption purpose
    val debugMode: Boolean = false,
    /**
     * Sensor sampling frequency.
     *
     * 25 Hz means one sample every 40 ms.
     */
    val samplingHz: Int = 25,

    /**
     * Time kept before and after the trigger sample.
     *
     * The actual number of samples is derived from samplingHz so the
     * shot window covers the same time span at different frequencies.
     */
    val preTriggerDurationMillis: Int = 1_000,
    val postTriggerDurationMillis: Int = 1_000,

    /**
     * Acceleration threshold used for shot detection.
     *
     * 2G = 2 * 9.81 m/s² = 19.62 m/s².
     */
    val accelerationThresholdMps2: Float = 2f * 9.81f,

    /**
     * Gyroscope threshold used for shot detection.
     */
    val gyroscopeThresholdRadS: Float = 5.0f,

    // for test on emulator purpose
    //val accelerationThresholdMps2: Float = 0.8f,
    //val gyroscopeThresholdRadS: Float = 0.4f,

    /**
     * If true, both acceleration and gyroscope thresholds must be exceeded.
     * If false, acceleration OR gyroscope is enough.
     */
    val requireBothSensorsForShot: Boolean = true,

    /**
     * Shows a toast when a shot is detected.
     * Mainly useful during debugging or dataset collection.
     */
    val showShotDetectionToast: Boolean = debugMode,

    /**
     * Number of shot windows accumulated before sending them to the phone.
     *
     * Used only in SHOT_TO_PHONE mode.
     */
    val shotWindowBatchSize: Int = 20,

    /**
     * Number of feature vectors accumulated before sending them to the phone.
     *
     * Used only in FEATURES_TO_PHONE mode.
     */
    val featureVectorBatchSize: Int = 20,

    /**
     * Maximum time a shot/feature batch can remain pending before being sent.
     *
     * This prevents data from staying buffered for too long if the score does not change.
     */
    val sensorBatchMaxDelayMillis: Long = 30_000L,
    /**
     * Time window used to batch raw sensor samples before sending them to the phone.
     *
     * Used only in RAW_TO_PHONE mode.
     */
    val rawBatchDurationMillis: Long = 1_000L
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
     */
    val totalShotSamples: Int
        get() = preTriggerSamples + 1 + postTriggerSamples

    val preTriggerSamples: Int
        get() = samplingHz * preTriggerDurationMillis / 1_000

    val postTriggerSamples: Int
        get() = samplingHz * postTriggerDurationMillis / 1_000
}
