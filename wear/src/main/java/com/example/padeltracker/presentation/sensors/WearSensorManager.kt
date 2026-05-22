package com.example.padeltracker.presentation.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.google.android.gms.wearable.Wearable
import java.nio.ByteBuffer
import java.nio.ByteOrder


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

import com.example.padeltracker.shared.experiment.ExperimentConfig
import com.example.padeltracker.shared.sensors.ImuVector
import com.example.padeltracker.shared.shotrecognition.ShotWindow
import com.example.padeltracker.shared.communication.SensorPacketSerializer
import com.example.padeltracker.shared.shotrecognition.ShotFeatureVector
import com.example.padeltracker.shared.communication.WearPaths
import com.example.padeltracker.shared.communication.ScoreHeader
import com.example.padeltracker.shared.communication.SensorPacket
import com.example.padeltracker.shared.experiment.ExperimentMode

class WearSensorManager(
    private val context: Context,
    private val onHeartRateChanged: (Double) -> Unit
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

    private val experimentConfig = ExperimentConfig()

    private val experimentPipeline = WearExperimentPipeline(
        config = experimentConfig,
        onRawSample = { sensorType, timestampNanos, value ->
            sendRawSample(
                sensorType = sensorType,
                timestampNanos = timestampNanos,
                value = value
            )
        },
        onShotWindow = { shotWindow, purpose ->
            when (purpose) {
                ShotWindowPurpose.DATA_COLLECTION -> {
                    sendShotWindowForDataCollection(shotWindow)
                }

                ShotWindowPurpose.CLASSIFICATION -> {
                    sendShotWindowForClassification(shotWindow)
                }
            }
        },
        onFeatureVector = { features ->
            sendFeatureVector(features)
        }
    )

    private val mainHandler = Handler(Looper.getMainLooper())

    private var targetNodeId: String? = null
    private val pendingShotWindows = mutableListOf<ShotWindow>()
    private val pendingFeatureVectors = mutableListOf<ShotFeatureVector>()
    private val pendingRawSamples = mutableListOf<SensorPacket.RawSensorSample>()

    private var shotWindowBatchFlushScheduled = false
    private var featureVectorBatchFlushScheduled = false
    private var rawBatchFlushScheduled = false

    // adding score to  simplify labeling
    @Volatile
    private var currentTeamASets: Int = 0

    @Volatile
    private var currentTeamBSets: Int = 0

    @Volatile
    private var currentTeamAGames: Int = 0

    @Volatile
    private var currentTeamBGames: Int = 0


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

                    messageClient.sendMessage(nodeId, WearPaths.HEART_RATE, buffer.array())
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

    /**
     * Sends all pending sensor batches to the phone.
     *
     * This should be called when a point is scored and when the match ends.
     */
    fun flushPendingSensorBatches() {
        flushRawSensorBatch()
        flushShotWindowBatch()
        flushFeatureVectorBatch()
    }

    fun startTracking() {
        Log.d(TAG, "Start tracking sensors (${experimentConfig.samplingHz}Hz)...")

        experimentPipeline.reset()

        accelerometer?.let {
            sensorManager.registerListener(this, it, experimentConfig.sensorDelayMicros)
            Log.d(TAG, "Accelerometer registered at ${experimentConfig.samplingHz}Hz")
        } ?: Log.e(TAG, "Accelerometer not found!")

        gyroscope?.let {
            sensorManager.registerListener(this, it, experimentConfig.sensorDelayMicros)
            Log.d(TAG, "Gyroscope registered at ${experimentConfig.samplingHz}Hz")
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
        flushPendingSensorBatches()
        sensorManager.unregisterListener(this)

        experimentPipeline.reset()

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
        if (!experimentConfig.showShotDetectionToast) return

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
        val values = event.values.copyOf()
        val imuVector = ImuVector.from(values)

        experimentPipeline.onSensorSample(
            sensorType = event.sensor.type,
            timestampNanos = event.timestamp,
            value = imuVector
        )
    }

    private fun sendRawSample(
        sensorType: Int,
        timestampNanos: Long,
        value: ImuVector
    ) {
        pendingRawSamples.add(
            SensorPacket.RawSensorSample(
                sensorType = sensorType,
                timestampNanos = timestampNanos,
                value = value
            )
        )

        scheduleRawSensorBatchFlush()
    }

    private fun scheduleRawSensorBatchFlush() {
        if (rawBatchFlushScheduled) return

        rawBatchFlushScheduled = true

        mainHandler.postDelayed(
            {
                rawBatchFlushScheduled = false
                flushRawSensorBatch()
            },
            experimentConfig.rawBatchDurationMillis
        )
    }

    private fun flushRawSensorBatch() {
        if (pendingRawSamples.isEmpty()) return

        val batch = pendingRawSamples.toList()
        pendingRawSamples.clear()
        rawBatchFlushScheduled = false

        val data = SensorPacketSerializer.serialize(
            mode = ExperimentMode.RAW_TO_PHONE,
            packet = SensorPacket.RawSensorBatch(batch)
        )

        targetNodeId?.let { nodeId ->
            messageClient.sendMessage(nodeId, WearPaths.SENSOR_RAW, data)
                .addOnSuccessListener {
                    Log.d(
                        TAG,
                        "Raw sensor batch sent successfully! path=${WearPaths.SENSOR_RAW}, bytes=${data.size}, samples=${batch.size}"
                    )
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "FAILED TO SEND RAW SENSOR BATCH: ${e.message}")
                }
        }
    }

    private fun sendShotWindowForDataCollection(shotWindow: ShotWindow) {
        val data = SensorPacketSerializer.serialize(
            mode = ExperimentMode.DATA_COLLECTION,
            packet = SensorPacket.DataCollectionShotWindow(
                shotWindow = shotWindow,
                scoreHeader = ScoreHeader(
                    teamASets = currentTeamASets,
                    teamBSets = currentTeamBSets,
                    teamAGames = currentTeamAGames,
                    teamBGames = currentTeamBGames
                )
            )
        )

        targetNodeId?.let { nodeId ->
            messageClient.sendMessage(nodeId, WearPaths.SENSOR_SHOT_DATA_COLLECTION, data)
                .addOnSuccessListener {
                    Log.d(
                        TAG,
                        "Data collection shot sent successfully! (${shotWindow.totalSamples} samples)"
                    )
                    showShotDetectedToast()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "FAILED TO SEND DATA COLLECTION SHOT: ${e.message}")
                }
        }
    }

    private fun sendShotWindowForClassification(shotWindow: ShotWindow) {
        pendingShotWindows.add(shotWindow)

        if (pendingShotWindows.size >= experimentConfig.shotWindowBatchSize) {
            flushShotWindowBatch()
        } else {
            scheduleShotWindowBatchFlush()
        }
    }

    private fun sendFeatureVector(features: ShotFeatureVector) {
        pendingFeatureVectors.add(features)

        if (pendingFeatureVectors.size >= experimentConfig.featureVectorBatchSize) {
            flushFeatureVectorBatch()
        } else {
            scheduleFeatureVectorBatchFlush()
        }
    }

    private fun scheduleShotWindowBatchFlush() {
        if (shotWindowBatchFlushScheduled) return

        shotWindowBatchFlushScheduled = true

        mainHandler.postDelayed(
            {
                shotWindowBatchFlushScheduled = false
                flushShotWindowBatch()
            },
            experimentConfig.sensorBatchMaxDelayMillis
        )
    }

    private fun scheduleFeatureVectorBatchFlush() {
        if (featureVectorBatchFlushScheduled) return

        featureVectorBatchFlushScheduled = true

        mainHandler.postDelayed(
            {
                featureVectorBatchFlushScheduled = false
                flushFeatureVectorBatch()
            },
            experimentConfig.sensorBatchMaxDelayMillis
        )
    }

    private fun flushShotWindowBatch() {
        if (pendingShotWindows.isEmpty()) return

        val batch = pendingShotWindows.toList()
        pendingShotWindows.clear()
        shotWindowBatchFlushScheduled = false

        val data = SensorPacketSerializer.serialize(
            mode = ExperimentMode.SHOT_TO_PHONE,
            packet = SensorPacket.ShotWindowBatch(batch)
        )

        targetNodeId?.let { nodeId ->
            messageClient.sendMessage(nodeId, WearPaths.SENSOR_SHOT_WINDOW, data)
                .addOnSuccessListener {
                    Log.d(
                        TAG,
                        "Shot window batch sent successfully! path=${WearPaths.SENSOR_SHOT_WINDOW}, bytes=${data.size}, windows=${batch.size}"
                    )
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "FAILED TO SEND SHOT WINDOW BATCH: ${e.message}")
                }
        }
    }

    private fun flushFeatureVectorBatch() {
        if (pendingFeatureVectors.isEmpty()) return

        val batch = pendingFeatureVectors.toList()
        pendingFeatureVectors.clear()
        featureVectorBatchFlushScheduled = false

        val data = SensorPacketSerializer.serialize(
            mode = ExperimentMode.FEATURES_TO_PHONE,
            packet = SensorPacket.FeatureVectorBatch(
                featureVectors = batch.map { it.values }
            )
        )

        targetNodeId?.let { nodeId ->
            messageClient.sendMessage(nodeId, WearPaths.SENSOR_FEATURES, data)
                .addOnSuccessListener {
                    Log.d(
                        TAG,
                        "Feature vector batch sent successfully! path=${WearPaths.SENSOR_FEATURES}, bytes=${data.size}, vectors=${batch.size}"
                    )
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "FAILED TO SEND FEATURE VECTOR BATCH: ${e.message}")
                }
        }
    }

    // called when the accuracy of a sensor changes. It's not essential for our purpose
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not necessary up to now
    }
}
