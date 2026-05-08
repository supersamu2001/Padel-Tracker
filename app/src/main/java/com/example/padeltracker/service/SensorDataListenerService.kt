package com.example.padeltracker.service

import android.hardware.Sensor
import android.util.Log
import com.example.padeltracker.ml.ShotClassifier
import com.example.padeltracker.ml.ShotDetectionState
import com.example.padeltracker.shared.SensorConstants
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
    private val WINDOW_SIZE = 40

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, ">>> SERVIZIO AVVIATO: onCreate chiamato correttamente! <<<")
        try {
            classifier = ShotClassifier(this)
            Log.d(TAG, "Classifier inizializzato con successo")
        } catch (e: Exception) {
            Log.e(TAG, "ERRORE durante inizializzazione Classifier: ${e.message}")
            e.printStackTrace()
        }
        shotLogger = ShotLogger(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        // Log verbose per vedere OGNI messaggio che arriva al telefono
        // Log.v(TAG, "Ricevuto messaggio sul path: ${messageEvent.path}")

        // RECEPTION OF ALL THE SAMPLES
        if (messageEvent.path == SensorConstants.SENSOR_DATA_PATH) {
            val data = messageEvent.data ?: return

            try {
                val buffer = ByteBuffer.wrap(data)
                buffer.order(ByteOrder.LITTLE_ENDIAN)

                val sensorType = buffer.int
                val x = buffer.float
                val y = buffer.float
                val z = buffer.float

                // Aggiorna lo stato globale per la UI
                SensorStatusState.updateData(sensorType, x, y, z)

                when (sensorType) {
                    Sensor.TYPE_ACCELEROMETER -> accBuffer.add(floatArrayOf(x, y, z))
                    Sensor.TYPE_GYROSCOPE -> gyroBuffer.add(floatArrayOf(x, y, z))
                }

                /**
                if (accBuffer.size >= WINDOW_SIZE && gyroBuffer.size >= WINDOW_SIZE) {
                processInference()
                }
                 **/
            } catch (e: Exception) {
                Log.e(TAG, "Parsing error: ${e.message}")
            }
        }
        
        // RECEPTION OF THE SHOT SAMPLES (Batch)
        if (messageEvent.path == SensorConstants.SHOT_DATA_PATH) {
            val data = messageEvent.data ?: return
            
            try {
                val numSamples = data.size / (2 * 12) // 2 sensors * 3 floats * 4 bytes
                Log.d(TAG, "Received shot: $numSamples samples")

                val buffer = ByteBuffer.wrap(data)
                buffer.order(ByteOrder.LITTLE_ENDIAN)

                // array containing the values of all the distinct samples
                val accBatch = mutableListOf<FloatArray>()
                val gyroBatch = mutableListOf<FloatArray>()

                // Read all the acceleration samples
                for (i in 0 until numSamples) {
                    accBatch.add(floatArrayOf(buffer.float, buffer.float, buffer.float))
                }

                // Read all the gyroscope samples
                for (i in 0 until numSamples) {
                    gyroBatch.add(floatArrayOf(buffer.float, buffer.float, buffer.float))
                }

                // Save in the CSV file
                shotLogger?.logShot(accBatch, gyroBatch)

                // Update the state of UI
                SensorStatusState.recordShot(numSamples)
                
            } catch (e: Exception) {
                Log.e(TAG, "Errore nel parsing del batch colpo: ${e.message}")
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
        Log.d(TAG, "Servizio distrutto")
        super.onDestroy()
    }
}
