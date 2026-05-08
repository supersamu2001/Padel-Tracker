package com.example.padeltracker.service

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ShotLogger(private val context: Context) {
    private val TAG = "ShotLogger"
    private val fileName = "padel_shots_dataset.csv"

    init {
        createFileIfNotExists()
    }

    private fun createFileIfNotExists() {
        val file = File(context.getExternalFilesDir(null), fileName)
        if (!file.exists()) {
            try {
                val writer = FileWriter(file)
                writer.append("shot_id,timestamp,sample_index,acc_x,acc_y,acc_z,gyro_x,gyro_y,gyro_z\n")
                writer.flush()
                writer.close()
                Log.d(TAG, "CSV file created at: ${file.absolutePath}")
            } catch (e: IOException) {
                Log.e(TAG, "Error creating CSV file: ${e.message}")
            }
        }
    }

    // Save the shot samples in a CSV file
    /**
     * CSV format:
     * shotId => identifier of the shot
     * timestamp => human-readable date and hour
     * sample_index => sample index within the shot (from 0 to 40)
     * acc_x, acc_y, acc_z => accelerometer values
     * gyro_x, gyro_y, gyro_z => gyroscope values
     */
    fun logShot(accSamples: List<FloatArray>, gyroSamples: List<FloatArray>) {
        val file = File(context.getExternalFilesDir(null), fileName)
        val shotId = System.currentTimeMillis()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date(shotId))
        
        try {
            val writer = FileWriter(file, true) // Append mode

            // coerceAtMost: take the lowest value between accSampes and gyroSamples
            // Avoid, in case of errors, that the two lists have always the same number of values
            val numSamples = accSamples.size.coerceAtMost(gyroSamples.size)
            
            for (i in 0 until numSamples) {
                val acc = accSamples[i]
                val gyro = gyroSamples[i]
                
                writer.append("$shotId,")
                writer.append("$timestamp,")
                writer.append("$i,")
                writer.append("${acc[0]},${acc[1]},${acc[2]},")
                writer.append("${gyro[0]},${gyro[1]},${gyro[2]}\n")
            }
            
            writer.flush()
            writer.close()
            Log.d(TAG, "Shot $shotId saved to CSV (${numSamples} samples)")
        } catch (e: IOException) {
            Log.e(TAG, "Error writing to CSV: ${e.message}")
        }
    }
}
