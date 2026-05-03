package com.example.padeltracker.ml

import android.content.Context
import android.content.res.AssetManager
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

enum class ShotType {
    FOREHAND,
    BACKHAND,
    FOREHAND_LOB,
    BACKHAND_LOB,
    SMASH,
    SERVICE,
    UNKNOWN
}

class ShotClassifier(private val context: Context) {
    private var interpreter: Interpreter? = null
    private val modelPath = "padel_shot_classifier.tflite"

    init {
        try {
            val modelBuffer = loadModelFile(context.assets, modelPath)
            val options = Interpreter.Options()
            interpreter = Interpreter(modelBuffer, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadModelFile(assetManager: AssetManager, modelPath: String): ByteBuffer {
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Classify the shot basing on the data taken from sensors
     * @param sensorData Array of floats (ex. Accelerometer x,y,z in a temporal window)
     * @return The predicted type of shot
     */

    fun classify(sensorData: FloatArray): ShotType {
        if (interpreter == null) return ShotType.UNKNOWN

        // TO DO: ADAPTATION (FOR INSTANCE NORMALIZATION!!)

        // Esempio: il modello accetta un input di forma (1, 40, 6) float32
        // Dobbiamo convertire il FloatArray in un ByteBuffer o un input compatibile
        
        // Supponiamo che l'output sia un array di probabilità per ogni classe
        val output = Array(1) { FloatArray(6) } // 6 classi: forehand, backhand, etc.

        try {
            // Se il modello accetta direttamente FloatArray multidimensionali:
            // interpreter?.run(input, output)

            // Per ora usiamo un placeholder per l'input basato sul tuo specifico modello
            val inputBuffer = ByteBuffer.allocateDirect(sensorData.size * 4)
            inputBuffer.order(ByteOrder.nativeOrder())
            sensorData.forEach { inputBuffer.putFloat(it) }
            
            interpreter?.run(inputBuffer, output)

            // Trova l'indice con la probabilità più alta
            val probabilities = output[0]
            val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1
            
            return when (maxIndex) {
                0 -> ShotType.FOREHAND
                1 -> ShotType.BACKHAND
                2 -> ShotType.FOREHAND_LOB
                3 -> ShotType.BACKHAND_LOB
                4 -> ShotType.SMASH
                5 -> ShotType.SERVICE
                else -> ShotType.UNKNOWN
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ShotType.UNKNOWN
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
