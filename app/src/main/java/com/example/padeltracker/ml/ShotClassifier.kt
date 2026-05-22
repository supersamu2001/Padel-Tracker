package com.example.padeltracker.ml

import android.content.Context
import android.content.res.AssetManager
import com.example.padeltracker.shared.shotrecognition.ShotFeatureExtractor
import com.example.padeltracker.shared.shotrecognition.ShotWindow
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

enum class ShotType {
    BACKHAND,
    FOREHAND,
    LOB_BACKHAND,
    LOB_FOREHAND,
    SERVICE,
    SMASH,
    UNKNOWN
}

/**
class ShotClassifier(private val context: Context) {
    private val modelPath = "PadelModel.java"

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
}*/

class ShotClassifier(private val context: Context) {

    val meansPython = doubleArrayOf( 9.26578691, 6.16099932, 11.6982833, -4.39890836, 47.95986454, -5.24923713, -5.53268105, 6.07140353,
        -20.15572514, 10.18990179, -0.31532567, 1.22416826, 8.20344144, -26.21688755, 13.77001197, 15.79357004, 11.88777592, 11.38624256,
        4.30416378, 54.31407199, -0.08080214, -0.17831126, 2.61440711, -5.2997294, 7.86897869, -0.23923738, 0.08759223, 3.14179614,
        -8.2569865, 4.97106218, -0.29925124, -0.15432833, 2.34919149, -6.0039157, 4.06487991, 3.9420587, 3.13374619, 2.88054331,
        0.81413693, 13.41467084)
    val deviationsPython = doubleArrayOf(3.57854334, 2.82187337, 5.64337699, 4.41362883, 23.92577965,  2.35116629, 2.41267997, 2.33130586,
        7.92421268, 9.78687755, 2.94785457, 2.15507152, 2.82163177, 13.30026272, 6.37708672, 4.12965439, 2.0844946, 5.30362742,
        1.89960233, 23.36507338, 0.47896065, 0.69782331, 1.00508985, 2.84259446, 5.314987, 0.65587988, 0.86148267, 1.06423845, 5.05724417, 3.21789723,
        0.49682259, 0.68346756, 0.83304647, 3.44881048, 2.03285384, 0.96556716, 0.79870687, 1.02141473, 0.38839306, 5.15482384)

    private val indiceToShotType = arrayOf(
        ShotType.BACKHAND,
        ShotType.FOREHAND,
        ShotType.LOB_BACKHAND,
        ShotType.LOB_FOREHAND,
        ShotType.SERVICE,
        ShotType.SMASH
    )

    // Called by handlers that receive raw data
    fun classify_shot(shotWindow: ShotWindow): ShotType {
        val featureVector = ShotFeatureExtractor.extract(shotWindow)
        val inputFeature = featureVector.values.map { it.toDouble() }.toDoubleArray()
        return classify_shot(inputFeature)
    }

    // Called by the handler that receives feature vector
    fun classify_shot(inputFeature: DoubleArray): ShotType {
        // Controllo di sicurezza
        if (inputFeature.size != meansPython.size) {
            throw IllegalArgumentException("Il numero di feature non corrisponde al modello!")
        }

        // STEP 1: Standardizzazione (Sostituisce lo StandardScaler di Python)
        val featureStandardizzate = DoubleArray(inputFeature.size)
        for (i in inputFeature.indices) {
            featureStandardizzate[i] = (inputFeature[i] - meansPython[i]) / deviationsPython[i]
        }

        // Il modello restituisce un array di "score" (uno per ogni classe/colpo)
        val scoresPrevisione: DoubleArray = PadelModel.score(featureStandardizzate)

        // Controllo di sicurezza sugli indici
        if (scoresPrevisione.size != indiceToShotType.size) {
            // Se le dimensioni non combaciano, significa che il modello è stato addestrato
            // con un numero di classi diverso da quello che ci aspettiamo qui.
            return ShotType.UNKNOWN
        }

        // STEP 3: Troviamo l'indice del colpo con lo score più alto (ArgMax)
        var bestIndex = 0
        var bestScore = scoresPrevisione[0]

        for (i in 1 until scoresPrevisione.size) {
            if (scoresPrevisione[i] > bestScore) {
                bestScore = scoresPrevisione[i]
                bestIndex = i
            }
        }

        // Restituiamo direttamente l'Enum!
        return indiceToShotType[bestIndex]
    }
}
