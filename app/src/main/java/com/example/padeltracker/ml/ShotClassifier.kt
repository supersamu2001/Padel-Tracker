package com.example.padeltracker.ml

import android.content.Context
import com.example.padeltracker.shared.shotrecognition.ShotFeatureExtractor
import com.example.padeltracker.shared.shotrecognition.ShotWindow

// Defines all the possible types of shot
enum class ShotType {
    BACKHAND,
    FOREHAND,
    LOB_BACKHAND,
    LOB_FOREHAND,
    SERVICE,
    SMASH,
    UNKNOWN
}

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

    // Maps the ML model indices into the actual shot type
    private val indexToShotType = arrayOf(
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

    // Called directly by the handler that receives feature vector
    fun classify_shot(inputFeature: DoubleArray): ShotType {
        // Security check
        if (inputFeature.size != meansPython.size) {
            throw IllegalArgumentException("The number of features do not correspond to the model!")
        }

        // Standardization
        val standardizedFeatures = DoubleArray(inputFeature.size)
        for (i in inputFeature.indices) {
            standardizedFeatures[i] = (inputFeature[i] - meansPython[i]) / deviationsPython[i]
        }

        // The model returns an array of “scores” (one for each class/shot)
        val predictedScores: DoubleArray = PadelModel.score(standardizedFeatures)

        // Check on the indices
        if (predictedScores.size != indexToShotType.size) {
            // If the dimensions don't match, it means the model was trained with a different number of classes than we expect here.
            return ShotType.UNKNOWN
        }

        // Find the shot with the highest score
        var bestIndex = 0
        var bestScore = predictedScores[0]

        for (i in 1 until predictedScores.size) {
            if (predictedScores[i] > bestScore) {
                bestScore = predictedScores[i]
                bestIndex = i
            }
        }

        // Return directly the enum
        return indexToShotType[bestIndex]
    }
}
