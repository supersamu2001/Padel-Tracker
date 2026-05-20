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
import com.example.padeltracker.shared.shotrecognition.ShotWindow

class SensorDataListenerService : WearableListenerService() {

    private var classifier: ShotClassifier? = null
    private var shotLogger: ShotLogger? = null
    private val TAG = "SensorDataListener"
    private val experimentConfig = ExperimentConfig()
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

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val data = messageEvent.data

        when (messageEvent.path) {
            // Stream di tutti i dati
            WearPaths.SENSOR_RAW -> {
                handleRawSensorPacket(data)
            }

            // Finestra con i 51 samples dello shot detectato con score_marker
            WearPaths.SENSOR_SHOT_DATA_COLLECTION -> {
                handleDataCollectionShotPacket(data)
            }

            // Finestra con i 51 samples dello shot detectato
            WearPaths.SENSOR_SHOT_WINDOW -> {
                handleShotWindowPacket(data)
            }

            // Feature engineering già fatta nello smartwatch
            WearPaths.SENSOR_FEATURES -> {
                handleFeatureVectorPacket(data)
            }

            else -> {
                Log.d(TAG, "Ignored message on path: ${messageEvent.path}")
            }
        }
    }

    private fun handleRawSensorPacket(data: ByteArray) {
        try {
            val packet = SensorPacketSerializer.deserialize(
                mode = ExperimentMode.RAW_TO_PHONE,
                data = data
            ) as SensorPacket.RawSensorSample

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
                ShotDetectionState.recordShot(shotType)

                SensorStatusState.recordShot(shotWindow.totalSamples)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing raw sensor packet: ${e.message}", e)
        }
    }

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

    private fun handleShotWindowPacket(data: ByteArray) {
        try {
            val packet = SensorPacketSerializer.deserialize(
                mode = ExperimentMode.SHOT_TO_PHONE,
                data = data
            ) as SensorPacket.ShotWindowPacket

            val shotWindow = packet.shotWindow

            Log.d(
                TAG,
                "Shot window received. samples=${shotWindow.totalSamples}"
            )

            val shotType = classifier?.classify_shot(shotWindow) ?: ShotType.UNKNOWN
            Log.d(TAG, "Shot classified: $shotType")
            ShotDetectionState.recordShot(shotType)

            SensorStatusState.recordShot(shotWindow.totalSamples)

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing classification shot packet: ${e.message}", e)
        }
    }

    private fun handleFeatureVectorPacket(data: ByteArray) {
        try {
            val packet = SensorPacketSerializer.deserialize(
                mode = ExperimentMode.FEATURES_TO_PHONE,
                data = data
            ) as SensorPacket.FeatureVector

            Log.d(
                TAG,
                "Feature vector received. features=${packet.values.size}"
            )

            val doubleFeatures = packet.values.map { it.toDouble() }.toDoubleArray()
            val shotType = classifier?.classify_shot(doubleFeatures) ?: ShotType.UNKNOWN
            Log.d(TAG, "Feature vector classified: $shotType")
            ShotDetectionState.recordShot(shotType)

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing feature vector packet: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        phoneShotDetector.reset()
        // classifier?.close()
        Log.d(TAG, "Service destroyed")
        super.onDestroy()
    }
}
