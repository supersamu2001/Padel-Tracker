package com.example.padeltracker.service

import android.hardware.Sensor
import android.util.Log
import com.example.padeltracker.ml.ShotClassifier
import com.example.padeltracker.ml.ShotDetectionState
import com.example.padeltracker.shared.communication.WearPaths
import com.example.padeltracker.shared.communication.SensorPacketSerializer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SensorDataListenerService : WearableListenerService() {

    private var classifier: ShotClassifier? = null
    private var shotLogger: ShotLogger? = null
    private val TAG = "SensorDataListener"

    // Buffers for data accumulation
    private val accBuffer = mutableListOf<FloatArray>()
    private val gyroBuffer = mutableListOf<FloatArray>()

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
        // Log verbose to inspect every message received by the phone
        // Log.v(TAG, "Received message on path: ${messageEvent.path}")

        // RECEPTION OF ALL THE SENSOR SAMPLES
        if (messageEvent.path == WearPaths.SENSOR_RAW) {
            val data = messageEvent.data ?: return

            try {
                val buffer = ByteBuffer.wrap(data)
                buffer.order(ByteOrder.LITTLE_ENDIAN)

                val sensorType = buffer.int
                val x = buffer.float
                val y = buffer.float
                val z = buffer.float

                // Update global state for the UI
                SensorStatusState.updateData(sensorType, x, y, z)

                when (sensorType) {
                    Sensor.TYPE_ACCELEROMETER -> accBuffer.add(floatArrayOf(x, y, z))
                    Sensor.TYPE_GYROSCOPE -> gyroBuffer.add(floatArrayOf(x, y, z))
                }

                /**
                if (accBuffer.size >= WINDOW_SIZE && gyroBuffer.size >= WINDOW_SIZE) {
                processInference()
                }
                 */
            } catch (e: Exception) {
                Log.e(TAG, "Parsing error: ${e.message}")
            }
        }

        // RECEPTION OF THE SHOT SAMPLES BATCH
        if (messageEvent.path == WearPaths.SENSOR_SHOT) {
            val data = messageEvent.data ?: return

            try {
                val packet = SensorPacketSerializer.deserializeShotWindow(data)
                val shotWindow = packet.shotWindow

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
                    "Received shot: ${shotWindow.totalSamples} samples, scoreMarker=${packet.scoreMarker}"
                )

                // Save in the CSV file with score marker
                shotLogger?.logShot(accBatch, gyroBatch, packet.scoreMarker)

                // Update the UI state
                SensorStatusState.recordShot(shotWindow.totalSamples)

            } catch (e: Exception) {
                Log.e(TAG, "Error parsing shot batch: ${e.message}", e)
            }
        }
    }

    /**
    private fun processInference() {
    val currentClassifier = classifier ?: return

    val input = FloatArray(WINDOW_SIZE * 6)
    for (i in 0 until WINDOW_SIZE) {
    val acc = accBuffer[i]
    val gyro = gyroBuffer[i]
    input[i * 6 + 0] = acc[0]
    input[i * 6 + 1] = acc[1]
    input[i * 6 + 2] = acc[2]
    input[i * 6 + 3] = gyro[0]
    input[i * 6 + 4] = gyro[1]
    input[i * 6 + 5] = gyro[2]
    }

    val result = currentClassifier.classify(input)
    if (result != com.example.padeltracker.ml.ShotType.UNKNOWN) {
    Log.d(TAG, "Colpo rilevato: $result")
    ShotDetectionState.recordShot(result)
    }

    accBuffer.clear()
    gyroBuffer.clear()
    }
     **/

    override fun onDestroy() {
        classifier?.close()
        Log.d(TAG, "Service destroyed")
        super.onDestroy()
    }
}
