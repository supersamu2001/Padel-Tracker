package com.example.padeltracker.service

import android.hardware.Sensor
import android.util.Log
import com.example.padeltracker.ml.ShotClassifier
import com.example.padeltracker.ml.ShotDetectionState
import com.example.padeltracker.ml.ShotType
import com.example.padeltracker.shared.communication.WearPaths
import com.example.padeltracker.shared.communication.SensorPacketSerializer
import com.example.padeltracker.shared.communication.SensorPacket
import com.example.padeltracker.shared.experiment.ExperimentMode
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.example.padeltracker.shared.experiment.ExperimentConfig
import com.example.padeltracker.shared.shotrecognition.ShotDetector

/**
 * Receives real-time sensor packets and features from the watch.
 */
class SensorDataListenerService : WearableListenerService() {

    // Shot classifier
    private var classifier: ShotClassifier? = null

    // Create the dataset for the training of the model
    private var shotLogger: ShotLogger? = null

    private val TAG = "SensorDataListener"

    // To set the method of data collection:
    // 1) All raw data, 2) Raw data of the shot with score_marker, 3) Raw data of the shot, 4) Feature vector
    private val experimentConfig = ExperimentConfig()

    // Detect the shot basing on acceleration and gyroscope
    private val phoneShotDetector = ShotDetector(experimentConfig)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, ">>> SERVICE STARTED: onCreate called correctly! <<<")
        try {
            classifier = ShotClassifier(this)
            Log.d(TAG, "Classifier initialized with success")
        } catch (e: Exception) {
            Log.e(TAG, "ERROR in the initialization of the Classifier: ${e.message}")
            e.printStackTrace()
        }
        shotLogger = ShotLogger(this)
    }

    // Called each time a data is received from the watch
    override fun onMessageReceived(messageEvent: MessageEvent) {
        val data = messageEvent.data

        Log.d(
            TAG,
            "Message received. path=${messageEvent.path}, bytes=${data.size}"
        )

        when (messageEvent.path) {
            // Stream of all the raw data
            WearPaths.SENSOR_RAW -> {
                handleRawSensorPacket(data)
            }

            // Window with the 51 raw samples of the detected shot with the score_marker (to populate our dataset)
            WearPaths.SENSOR_SHOT_DATA_COLLECTION -> {
                handleDataCollectionShotPacket(data)
            }

            // Window with the 51 raw samples of the detected shot
            WearPaths.SENSOR_SHOT_WINDOW -> {
                handleShotWindowPacket(data)
            }

            // Feature engineering already done on the watch
            WearPaths.SENSOR_FEATURES -> {
                handleFeatureVectorPacket(data)
            }

            else -> {
                Log.d(TAG, "Ignored message on path: ${messageEvent.path}")
            }
        }
    }

    // Stream of all the raw data
    private fun handleRawSensorPacket(data: ByteArray) {
        try {
            val packet = SensorPacketSerializer.deserialize(
                mode = ExperimentMode.RAW_TO_PHONE,
                data = data
            ) as SensorPacket.RawSensorBatch

            Log.d(
                TAG,
                "Raw sensor batch received. samples=${packet.samples.size}"
            )

            packet.samples.forEach { sample ->
                processRawSensorSample(sample)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing raw sensor batch packet: ${e.message}", e)
        }
    }

    private fun processRawSensorSample(packet: SensorPacket.RawSensorSample) {
        val value = packet.value

        SensorStatusState.updateData(
            type = packet.sensorType,
            x = value.x,
            y = value.y,
            z = value.z
        )

        val shotWindow = when (packet.sensorType) {
            Sensor.TYPE_ACCELEROMETER -> {
                phoneShotDetector.addAccelerometerSample(
                    timestampNanos = packet.timestampNanos,
                    value = value
                )
            }

            Sensor.TYPE_GYROSCOPE -> {
                phoneShotDetector.addGyroscopeSample(
                    timestampNanos = packet.timestampNanos,
                    value = value
                )
            }

            else -> null
        }

        if (shotWindow != null) {
            Log.d(
                TAG,
                "Raw pipeline detected shot. samples=${shotWindow.totalSamples}"
            )

            val shotType = classifier?.classify_shot(shotWindow) ?: ShotType.UNKNOWN
            Log.d(TAG, "Raw pipeline shot classified: $shotType")

            // Record the shot in order to show all of them in the final AnalysisScreen
            ShotDetectionState.recordShot(shotType)

            // Record the shot in order to show it in the HomeScreen as debug
            SensorStatusState.recordShot(shotWindow.totalSamples)
        }
    }

    // Window with the 51 raw samples of the detected shot with the score_marker (to populate our dataset)
    private fun handleDataCollectionShotPacket(data: ByteArray) {
        try {
            val packet = SensorPacketSerializer.deserialize(
                mode = ExperimentMode.DATA_COLLECTION,
                data = data
            ) as SensorPacket.DataCollectionShotWindow

            val shotWindow = packet.shotWindow
            val scoreMarker = packet.scoreHeader.scoreMarker

            val accBatch = shotWindow.samples.map { sample ->
                floatArrayOf(
                    sample.accelerometer.x,
                    sample.accelerometer.y,
                    sample.accelerometer.z
                )
            }

            val gyroBatch = shotWindow.samples.map { sample ->
                floatArrayOf(
                    sample.gyroscope.x,
                    sample.gyroscope.y,
                    sample.gyroscope.z
                )
            }

            Log.d(
                TAG,
                "Received data collection shot: ${shotWindow.totalSamples} samples, scoreMarker=$scoreMarker"
            )

            shotLogger?.logShot(accBatch, gyroBatch, scoreMarker)

            SensorStatusState.recordShot(shotWindow.totalSamples)

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing data collection shot packet: ${e.message}", e)
        }
    }

    // Window with the 51 raw samples of the detected shot
    private fun handleShotWindowPacket(data: ByteArray) {
        try {
            val packet = SensorPacketSerializer.deserialize(
                mode = ExperimentMode.SHOT_TO_PHONE,
                data = data
            ) as SensorPacket.ShotWindowBatch

            Log.d(
                TAG,
                "Shot window batch received. windows=${packet.shotWindows.size}"
            )

            packet.shotWindows.forEach { shotWindow ->
                val shotType = classifier?.classify_shot(shotWindow) ?: ShotType.UNKNOWN
                Log.d(TAG, "Shot classified: $shotType")

                //
                ShotDetectionState.recordShot(shotType)

                SensorStatusState.recordShot(shotWindow.totalSamples)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing shot window batch packet: ${e.message}", e)
        }
    }

    // Feature engineering already done on the watch
    private fun handleFeatureVectorPacket(data: ByteArray) {
        try {
            val packet = SensorPacketSerializer.deserialize(
                mode = ExperimentMode.FEATURES_TO_PHONE,
                data = data
            ) as SensorPacket.FeatureVectorBatch

            Log.d(
                TAG,
                "Feature vector batch received. vectors=${packet.featureVectors.size}"
            )

            packet.featureVectors.forEach { values: List<Float> ->
                val doubleFeatures = values.map { it.toDouble() }.toDoubleArray()
                val shotType = classifier?.classify_shot(doubleFeatures) ?: ShotType.UNKNOWN

                Log.d(TAG, "Feature vector classified: $shotType")
                ShotDetectionState.recordShot(shotType)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing feature vector batch packet: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        phoneShotDetector.reset()
        Log.d(TAG, "Service destroyed")
        super.onDestroy()
    }
}
