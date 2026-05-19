package com.example.padeltracker.presentation.sensors

import android.hardware.Sensor
import com.example.padeltracker.shared.experiment.ExperimentConfig
import com.example.padeltracker.shared.experiment.ExperimentMode
import com.example.padeltracker.shared.sensors.ImuVector
import com.example.padeltracker.shared.shotrecognition.ShotDetector
import com.example.padeltracker.shared.shotrecognition.ShotFeatureExtractor
import com.example.padeltracker.shared.shotrecognition.ShotFeatureVector
import com.example.padeltracker.shared.shotrecognition.ShotWindow
import android.util.Log

/**
 * Coordinates the sensor processing pipeline on the watch.
 *
 * It receives raw IMU samples from WearSensorManager and decides what to do
 * according to the current experiment mode.
 *
 * Current modes:
 * - RAW_TO_PHONE: raw samples will be sent to the phone.
 * - SHOT_TO_PHONE: the watch detects a shot and sends the shot window.
 * - FEATURES_TO_PHONE: the watch detects a shot, extracts features, and sends only features.
 * - DATA_COLLECTION: the watch detects a shot and sends the shot window for CSV logging.
 *
 * This class does not access Android sensors directly.
 * WearSensorManager remains responsible for reading SensorEvent values.
 */
class WearExperimentPipeline(
    private val config: ExperimentConfig = ExperimentConfig(),
    private val shotDetector: ShotDetector = ShotDetector(config),
    private val onRawSample: (
        sensorType: Int,
        timestampNanos: Long,
        value: ImuVector
    ) -> Unit = { _, _, _ -> },
    private val onShotWindow: (
        shotWindow: ShotWindow,
        purpose: ShotWindowPurpose
    ) -> Unit = { _, _ -> },
    private val onFeatureVector: (ShotFeatureVector) -> Unit = {}
) {

    /**
     * Processes one IMU sample coming from WearSensorManager.
     */
    fun onSensorSample(
        sensorType: Int,
        timestampNanos: Long,
        value: ImuVector
    ) {
        when (config.mode) {
            ExperimentMode.RAW_TO_PHONE -> {
                onRawSample(sensorType, timestampNanos, value)
            }

            ExperimentMode.DATA_COLLECTION -> {
                Log.d("WEAR_PIPELINE", "DATA_COLLECTION sample received. sensorType=$sensorType")
                val shotWindow = detectShotIfAvailable(
                    sensorType = sensorType,
                    timestampNanos = timestampNanos,
                    value = value
                )

                if (shotWindow != null) {
                    Log.d("WEAR_PIPELINE", "ShotWindow produced with ${shotWindow.totalSamples} samples")
                    onShotWindow(shotWindow, ShotWindowPurpose.DATA_COLLECTION)
                }
            }

            ExperimentMode.SHOT_TO_PHONE -> {
                val shotWindow = detectShotIfAvailable(
                    sensorType = sensorType,
                    timestampNanos = timestampNanos,
                    value = value
                )

                if (shotWindow != null) {
                    onShotWindow(shotWindow, ShotWindowPurpose.CLASSIFICATION)
                }
            }

            ExperimentMode.FEATURES_TO_PHONE -> {
                val shotWindow = detectShotIfAvailable(
                    sensorType = sensorType,
                    timestampNanos = timestampNanos,
                    value = value
                )

                if (shotWindow != null) {
                    val features = ShotFeatureExtractor.extract(shotWindow)
                    onFeatureVector(features)
                }
            }
        }
    }

    /**
     * Resets the internal shot detector state.
     *
     * This should be called when sensor tracking starts, stops, or restarts.
     */
    fun reset() {
        shotDetector.reset()
    }

    private fun detectShotIfAvailable(
        sensorType: Int,
        timestampNanos: Long,
        value: ImuVector
    ): ShotWindow? {
        return when (sensorType) {
            Sensor.TYPE_ACCELEROMETER -> {
                shotDetector.addAccelerometerSample(
                    timestampNanos = timestampNanos,
                    value = value
                )
            }

            Sensor.TYPE_GYROSCOPE -> {
                shotDetector.addGyroscopeSample(
                    timestampNanos = timestampNanos,
                    value = value
                )
            }

            else -> null
        }
    }
}

/**
 * Describes why a shot window is being sent to the phone.
 */
enum class ShotWindowPurpose {
    /**
     * The shot window is sent for dataset creation.
     * In this mode, match score context can be attached to the packet.
     */
    DATA_COLLECTION,

    /**
     * The shot window is sent for feature extraction and classification.
     * In this mode, match score context is not needed.
     */
    CLASSIFICATION
}