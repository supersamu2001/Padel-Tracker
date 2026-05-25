package com.example.padeltracker.shared.shotrecognition

import com.example.padeltracker.shared.experiment.ExperimentConfig
import com.example.padeltracker.shared.sensors.ImuVector
import com.example.padeltracker.shared.sensors.PairedImuSample
import java.util.ArrayDeque
import kotlin.math.abs

/**
 * Pure shot detection logic based on accelerometer and gyroscope samples.
 *
 * This class does not depend on Android APIs, SensorManager, Wear OS, or the phone.
 * It can be used both by the wear module and by the app module.
 */
class ShotDetector(
    private val config: ExperimentConfig = ExperimentConfig()
) {
    private val accHistory = ArrayDeque<TimedImuVector>()
    private val gyroHistory = ArrayDeque<TimedImuVector>()

    private var lastAccSample: TimedImuVector? = null
    private var lastGyroSample: TimedImuVector? = null

    private var isRecordingShot = false
    private var postShotAccCount = 0
    private var postShotGyroCount = 0

    private val shotAccData = mutableListOf<TimedImuVector>()
    private val shotGyroData = mutableListOf<TimedImuVector>()

    /**
     * Adds one accelerometer sample.
     *
     * Returns a ShotWindow only when a complete shot window is available.
     */
    fun addAccelerometerSample(
        timestampNanos: Long,
        value: ImuVector
    ): ShotWindow? {
        val sample = TimedImuVector(timestampNanos, value)

        lastAccSample = sample
        accHistory.addLast(sample)

        while (accHistory.size > config.preTriggerSamples + 1) {
            accHistory.removeFirst()
        }

        if (isRecordingShot) {
            shotAccData.add(sample)
            postShotAccCount++
        }

        return updateDetectionState()
    }

    /**
     * Adds one gyroscope sample.
     *
     * Returns a ShotWindow only when a complete shot window is available.
     */
    fun addGyroscopeSample(
        timestampNanos: Long,
        value: ImuVector
    ): ShotWindow? {
        val sample = TimedImuVector(timestampNanos, value)

        lastGyroSample = sample
        gyroHistory.addLast(sample)

        while (gyroHistory.size > config.preTriggerSamples + 1) {
            gyroHistory.removeFirst()
        }

        if (isRecordingShot) {
            shotGyroData.add(sample)
            postShotGyroCount++
        }

        return updateDetectionState()
    }

    /**
     * Clears the detector state.
     *
     * Useful when a match starts, ends, or when sensor tracking is restarted.
     */
    fun reset() {
        accHistory.clear()
        gyroHistory.clear()

        lastAccSample = null
        lastGyroSample = null

        isRecordingShot = false
        postShotAccCount = 0
        postShotGyroCount = 0

        shotAccData.clear()
        shotGyroData.clear()
    }

    private fun updateDetectionState(): ShotWindow? {
        val hasEnoughPreTriggerHistory =
            accHistory.size >= config.preTriggerSamples + 1 &&
                    gyroHistory.size >= config.preTriggerSamples + 1

        if (
            !isRecordingShot &&
            hasEnoughPreTriggerHistory &&
            lastAccSample != null &&
            lastGyroSample != null
        ) {
            val accTrigger = hasComponentAboveThreshold(
                vector = lastAccSample!!.value,
                threshold = config.accelerationThresholdMps2
            )

            val gyroTrigger = hasComponentAboveThreshold(
                vector = lastGyroSample!!.value,
                threshold = config.gyroscopeThresholdRadS
            )

            val shotDetected = if (config.requireBothSensorsForShot) {
                accTrigger && gyroTrigger
            } else {
                accTrigger || gyroTrigger
            }

            if (shotDetected) {
                startShotRecording()
            }
        }

        if (
            isRecordingShot &&
            postShotAccCount >= config.postTriggerSamples &&
            postShotGyroCount >= config.postTriggerSamples
        ) {
            val shotWindow = buildShotWindow()
            reset()
            return shotWindow
        }

        return null
    }

    private fun startShotRecording() {
        isRecordingShot = true
        postShotAccCount = 0
        postShotGyroCount = 0

        shotAccData.clear()
        shotGyroData.clear()

        shotAccData.addAll(accHistory)
        shotGyroData.addAll(gyroHistory)
    }

    private fun buildShotWindow(): ShotWindow {
        val numSamples = shotAccData.size.coerceAtMost(shotGyroData.size)

        val pairedSamples = List(numSamples) { index ->
            PairedImuSample(
                timestampNanos = shotAccData[index].timestampNanos,
                accelerometer = shotAccData[index].value,
                gyroscope = shotGyroData[index].value
            )
        }

        return ShotWindow(samples = pairedSamples)
    }

    private fun hasComponentAboveThreshold(
        vector: ImuVector,
        threshold: Float
    ): Boolean {
        return abs(vector.x) > threshold ||
                abs(vector.y) > threshold ||
                abs(vector.z) > threshold
    }

    private data class TimedImuVector(
        val timestampNanos: Long,
        val value: ImuVector
    )
}
