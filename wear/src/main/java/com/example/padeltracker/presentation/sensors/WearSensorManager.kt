package com.example.padeltracker.presentation.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.ui.geometry.isEmpty
import com.example.padeltracker.shared.SensorConstants
import com.google.android.gms.wearable.Wearable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayDeque
import kotlin.math.abs

//toast debug
import android.os.Handler
import android.os.Looper
import android.widget.Toast

//heartbeat
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.unregisterMeasureCallback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
class WearSensorManager(
    private val context: Context,
    private val onHeartRateChanged: (Double) -> Unit // <--- ΠΡΟΣΘΕΣΕ ΑΥΤΟ
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    private val measureClientHR = HealthServices.getClient(context).measureClient
    var latestHeartRate: Double = 0.0 // Stores the latest HR value

    private val scope = CoroutineScope(Dispatchers.Main) //heavy staff
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val TAG = "WearSensorManager"

    //toast debug
    private val DEBUG_SHOW_SHOT_TOAST = true
    private val mainHandler = Handler(Looper.getMainLooper())

    // Recognition Thresholds
    private val ACC_THRESHOLD = 2 * 9.81f   // ~29.43 m/s^2
    private val GYRO_THRESHOLD = 5.0f       // rad/s
    // test on emulator
    //private val ACC_THRESHOLD = 0.8f
    //private val GYRO_THRESHOLD = 0.4f
    private val WINDOW_SIZE = 20            // Number of samples before and after the trigger

    // Buffers for recognition
    private val accHistory = ArrayDeque<FloatArray>()
    private val gyroHistory = ArrayDeque<FloatArray>()
    private var lastAccValues: FloatArray? = null
    private var lastGyroValues: FloatArray? = null

    // State for shot recording
    private var isRecordingShot = false     // tell us if a shot has been detected
    private var postShotAccCount = 0        // number of acceleration samples after the detection
    private var postShotGyroCount = 0       // number of gyroscope samples after the detection
    private val shotAccData = mutableListOf<FloatArray>()
    private val shotGyroData = mutableListOf<FloatArray>()

    // 25Hz means 1 sample each 40ms.
    // The parameter of the periodic listener must be in microseconds
    private val SENSOR_DELAY_25HZ = 40000

    private var targetNodeId: String? = null

    // adding score to  simplify labeling
    @Volatile
    private var currentTeamASets: Int = 0

    @Volatile
    private var currentTeamBSets: Int = 0

    @Volatile
    private var currentTeamAGames: Int = 0

    @Volatile
    private var currentTeamBGames: Int = 0

    
    // Variables to track sampling intervals
    private var lastAccTime: Long = 0
    private var lastGyroTime: Long = 0

    // ---> NEW ADDITION FOR HEART RATE: Callback <---
    // This listens for new heart rate values from the sensor

    private val heartRateCallback = object : MeasureCallback {
        override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) {
            Log.d(TAG, "Heart Rate Sensor Availability: $availability")
        }

        override fun onDataReceived(data: DataPointContainer) {
            val hrData = data.getData(DataType.HEART_RATE_BPM)
            if (hrData.isNotEmpty()) {
                latestHeartRate = hrData.last().value

                onHeartRateChanged(latestHeartRate)

                Log.d(TAG, "❤️ Heart Rate: $latestHeartRate BPM")

                // Send the heart rate to the phone using the new path
                targetNodeId?.let { nodeId ->
                    val buffer = ByteBuffer.allocate(8) // 8 bytes for a Double
                    buffer.order(ByteOrder.LITTLE_ENDIAN)
                    buffer.putDouble(latestHeartRate)

                    messageClient.sendMessage(nodeId, SensorConstants.HEART_RATE_PATH, buffer.array())
                        .addOnFailureListener { Log.e(TAG, "FAILED TO SEND HEART RATE") }
                }
            }
        }
    }

    init {
        // Search for connected phone
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            if (nodes.isEmpty()) {
                Log.e(TAG, "ERROR: NO PHONE FOUND!")
            } else {
                targetNodeId = nodes.firstOrNull()?.id
                Log.d(TAG, "PHONE FOUND: $targetNodeId")
            }
        }
    }

    // simplify labeling
    fun updateScoreMarker(
        teamASets: Int,
        teamBSets: Int,
        teamAGames: Int,
        teamBGames: Int
    ) {
        currentTeamASets = teamASets
        currentTeamBSets = teamBSets
        currentTeamAGames = teamAGames
        currentTeamBGames = teamBGames

        Log.d(
            TAG,
            "Score marker updated: S$teamASets-$teamBSets" +
                    "_G$teamAGames-$teamBGames"
        )
    }
    fun startTracking() {
        Log.d(TAG, "Start tracking sensors (25Hz)...")
        
        accelerometer?.let {
            sensorManager.registerListener(this, it, SENSOR_DELAY_25HZ)
            Log.d(TAG, "Accelerometer registered at 25Hz")
        } ?: Log.e(TAG, "Accelerometer not found!")

        gyroscope?.let {
            sensorManager.registerListener(this, it, SENSOR_DELAY_25HZ)
            Log.d(TAG, "Gyroscope registered at 25Hz")
        } ?: Log.e(TAG, "Gyroscope not found!")

        //heartbeat

        scope.launch {
            try {
                measureClientHR.registerMeasureCallback(DataType.HEART_RATE_BPM, heartRateCallback)
                Log.d(TAG, "Heart Rate registered successfully!")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register HR: ${e.message}")
            }
        }

    }

    fun stopTracking() {
        Log.d(TAG, "Interruption tracking sensors")
        sensorManager.unregisterListener(this)

        //measureClientHR.unregisterMeasureCallback(heartRateCallback) //heartbeat

        scope.launch {
            try {
                measureClientHR.unregisterMeasureCallback(DataType.HEART_RATE_BPM,heartRateCallback)
                Log.d(TAG, "Heart Rate unregistered.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister HR: ${e.message}")
            }
        }

    }

    //toast debug
    private fun showShotDetectedToast() {
        if (!DEBUG_SHOW_SHOT_TOAST) return

        mainHandler.post {
            Toast.makeText(
                context.applicationContext,
                "SHOT DETECTED",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // called each time a sensor registers a new value
    override fun onSensorChanged(event: SensorEvent) {
        // values => values obtained by the sensor
        val values = event.values.copyOf()

        // 1. Update buffers for history and tracking
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            lastAccValues = values
            accHistory.addLast(values)

            // each time we only keep the 20 most recent samples (plus the actual one)
            if (accHistory.size > WINDOW_SIZE + 1) accHistory.removeFirst()

            if (isRecordingShot) {
                shotAccData.add(values)
                postShotAccCount++
            }
        } else if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            lastGyroValues = values
            gyroHistory.addLast(values)
            if (gyroHistory.size > WINDOW_SIZE + 1) gyroHistory.removeFirst()

            if (isRecordingShot) {
                shotGyroData.add(values)
                postShotGyroCount++
            }
        }

        // 2. Check for trigger (if not already recording)
        if (!isRecordingShot && lastAccValues != null && lastGyroValues != null) {
            val accTrigger = lastAccValues!!.any { abs(it) > ACC_THRESHOLD }
            val gyroTrigger = lastGyroValues!!.any { abs(it) > GYRO_THRESHOLD }

            if (accTrigger && gyroTrigger) {
                isRecordingShot = true
                postShotAccCount = 0
                postShotGyroCount = 0
                shotAccData.clear()
                shotGyroData.clear()

                // Capture history: 20 previous samples + the trigger sample (which is last in history)
                shotAccData.addAll(accHistory)
                shotGyroData.addAll(gyroHistory)

                Log.d(TAG, "!!! SHOT DETECTED !!! Collecting post-shot data...")
                // toast debug
                showShotDetectedToast()
            }
        }

        // 3. Check if recording is complete (trigger + 20 samples after)
        if (isRecordingShot && postShotAccCount >= WINDOW_SIZE && postShotGyroCount >= WINDOW_SIZE) {
            sendShotBatch()
            isRecordingShot = false
        }

        // TO SEND ALL THE SAMPLES TO THE PHONE
        // Existing streaming logic (optional: can be removed if only shot detection is needed)
        /**
        val streamBuffer = ByteBuffer.allocate(16)
        streamBuffer.order(ByteOrder.LITTLE_ENDIAN)
        streamBuffer.putInt(event.sensor.type)
        streamBuffer.putFloat(event.values[0])
        streamBuffer.putFloat(event.values[1])
        streamBuffer.putFloat(event.values[2])

        val streamData = streamBuffer.array()

        targetNodeId?.let { nodeId ->
            messageClient.sendMessage(nodeId, SensorConstants.SENSOR_DATA_PATH, streamData)
                .addOnFailureListener { e ->
                    Log.e(TAG, "FAILED TO SEND STREAM: ${e.message}")
                }
        }
        */

        /**
        // ... logging code ...
        // Keep local logging for verification
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val currentNano = event.timestamp
                val diffMs = if (lastAccTime != 0L) (currentNano - lastAccTime) / 1_000_000 else 0
                lastAccTime = currentNano
                
                // Log.d(TAG, "ACC -> x: ${event.values[0]}, y: ${event.values[1]}, z: ${event.values[2]}, interval: ${diffMs}ms")
            }

            Sensor.TYPE_GYROSCOPE -> {
                val currentNano = event.timestamp
                val diffMs = if (lastGyroTime != 0L) (currentNano - lastGyroTime) / 1_000_000 else 0
                lastGyroTime = currentNano
                
                // Log.d(TAG, "GYRO -> x: ${event.values[0]}, y: ${event.values[1]}, z: ${event.values[2]}, interval: ${diffMs}ms")
            }
        }
        */
    }

    // SEND THE 41 SAMPLES RELATED TO THE SHOT
    private fun sendShotBatch() {
        // simplify labeling
        /*val numSamples = shotAccData.size.coerceAtMost(shotGyroData.size)
        // Each sample is 3 floats (12 bytes) * 2 sensors
        val buffer = ByteBuffer.allocate(numSamples * 2 * 12)
        buffer.order(ByteOrder.LITTLE_ENDIAN)*/
        val numSamples = shotAccData.size.coerceAtMost(shotGyroData.size)

        // Header: 4 Int values for score marker
        // teamASets, teamBSets, teamAGames, teamBGames
        val scoreHeaderBytes = 4 * 4

        // Each sample is 3 floats (12 bytes) * 2 sensors
        val buffer = ByteBuffer.allocate(scoreHeaderBytes + numSamples * 2 * 12)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        buffer.putInt(currentTeamASets)
        buffer.putInt(currentTeamBSets)
        buffer.putInt(currentTeamAGames)
        buffer.putInt(currentTeamBGames)
        // end simplify labeling (delete new block and uncomment old one)


        // Accelerometer data block
        for (i in 0 until numSamples) {
            val vals = shotAccData[i]
            buffer.putFloat(vals[0])
            buffer.putFloat(vals[1])
            buffer.putFloat(vals[2])
        }

        // Gyroscope data block
        for (i in 0 until numSamples) {
            val vals = shotGyroData[i]
            buffer.putFloat(vals[0])
            buffer.putFloat(vals[1])
            buffer.putFloat(vals[2])
        }

        val data = buffer.array()
        targetNodeId?.let { nodeId ->
            messageClient.sendMessage(nodeId, SensorConstants.SHOT_DATA_PATH, data)
                .addOnSuccessListener {
                    Log.d(TAG, "Shot batch sent successfully! ($numSamples samples)")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "FAILED TO SEND SHOT BATCH: ${e.message}")
                }
        }
    }

    // called when the accuracy of a sensor changes. It's not essential for our purpose
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not necessary up to now
    }
}
