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

class WearSensorManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)
    
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val TAG = "WearSensorManager"

    // 25Hz means 1 sample each 40ms.
    // The parameter of the periodic listener must be in microseconds
    private val SENSOR_DELAY_25HZ = 40000

    private var targetNodeId: String? = null
    
    // Variables to track sampling intervals
    private var lastAccTime: Long = 0
    private var lastGyroTime: Long = 0

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
    }

    fun stopTracking() {
        Log.d(TAG, "Interruption tracking sensors")
        sensorManager.unregisterListener(this)
    }

    // called each time a sensor registers a new value
    override fun onSensorChanged(event: SensorEvent) {
        // Prepare data packet: [SensorType (Int), X (Float), Y (Float), Z (Float)]
        val buffer = ByteBuffer.allocate(16) 
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(event.sensor.type)
        buffer.putFloat(event.values[0])
        buffer.putFloat(event.values[1])
        buffer.putFloat(event.values[2])

        val data = buffer.array()

        // Send to phone the registered data
        // p0 => smartphone address
        // p1 => object of the message (in order to let the phone identify the content of the packet)
        // p2 => actual data, which is [SensorType (Int), X (Float), Y (Float), Z (Float)]
        targetNodeId?.let { nodeId ->
            messageClient.sendMessage(nodeId, SensorConstants.SENSOR_DATA_PATH, data)
                .addOnFailureListener { e ->
                    Log.e(TAG, "FAILED TO SEND: ${e.message}")
                }
        }


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
    }

    // called when the accuracy of a sensor changes. It's not essential for our purpose
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not necessary up to now
    }
}
