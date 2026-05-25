package com.example.padeltracker.shared.shotrecognition

import kotlin.math.sqrt

/**
 * Feature vector extracted from a detected shot window.
 *
 * The order of the values is defined by featureNames.
 * This is important because the ML model must receive the features
 * in the same order used during training.
 */
data class ShotFeatureVector(
    val values: List<Float>,
    val featureNames: List<String>
) {
    init {
        require(values.size == featureNames.size) {
            "Feature values and names must have the same size."
        }
    }

    fun toFloatArray(): FloatArray {
        return values.toFloatArray()
    }
}

/**
 * Extracts statistical features from a detected shot window.
 *
 * This class is pure Kotlin and can be used both on the watch and on the phone.
 */
object ShotFeatureExtractor {

    private val signalNames = listOf(
        "acc_x",
        "acc_y",
        "acc_z",
        "acc_mag",
        "gyro_x",
        "gyro_y",
        "gyro_z",
        "gyro_mag"
    )

    private val statisticNames = listOf(
        "mean",
        "median",
        "std",
        "min",
        "max",
    )

    /**
     * Extracts a fixed-size feature vector from a shot window.
     *
     * Current output:
     * 8 signals x 5 statistics = 40 features.
     *
     * Signals:
     * - acc_x, acc_y, acc_z, acc_mag
     * - gyro_x, gyro_y, gyro_z, gyro_mag
     *
     * Statistics:
     * - mean
     * - median
     * - standard deviation
     * - minimum
     * - maximum
     */
    fun extract(shotWindow: ShotWindow): ShotFeatureVector {
        require(shotWindow.samples.isNotEmpty()) {
            "Cannot extract features from an empty shot window."
        }

        val signals = listOf(
            shotWindow.samples.map { it.accelerometer.x },
            shotWindow.samples.map { it.accelerometer.y },
            shotWindow.samples.map { it.accelerometer.z },
            shotWindow.samples.map {
                sqrt(it.accelerometer.x * it.accelerometer.x +
                        it.accelerometer.y * it.accelerometer.y +
                        it.accelerometer.z * it.accelerometer.z)
            },
            shotWindow.samples.map { it.gyroscope.x },
            shotWindow.samples.map { it.gyroscope.y },
            shotWindow.samples.map { it.gyroscope.z },
            shotWindow.samples.map {
                sqrt(it.gyroscope.x * it.gyroscope.x +
                        it.gyroscope.y * it.gyroscope.y +
                        it.gyroscope.z * it.gyroscope.z)
            }
        )

        val values = mutableListOf<Float>()
        val names = mutableListOf<String>()

        signals.forEachIndexed { signalIndex, signalValues ->
            val signalName = signalNames[signalIndex]
            val stats = computeStatistics(signalValues)

            values.add(stats.mean)
            names.add("${signalName}_mean")

            values.add(stats.median)
            names.add("${signalName}_median")

            values.add(stats.standardDeviation)
            names.add("${signalName}_std")

            values.add(stats.min)
            names.add("${signalName}_min")

            values.add(stats.max)
            names.add("${signalName}_max")
        }

        return ShotFeatureVector(
            values = values,
            featureNames = names
        )
    }

    private fun computeStatistics(values: List<Float>): SignalStatistics {
        val mean = values.average().toFloat()

        // Median
        val sortedValues = values.sorted()
        val size = sortedValues.size
        val median = if (size % 2 == 0) {
            // Se i campioni sono pari, facciamo la media dei due valori centrali
            (sortedValues[size / 2 - 1] + sortedValues[size / 2]) / 2f
        } else {
            // Se sono dispari, prendiamo il valore esattamente al centro
            sortedValues[size / 2]
        }

        // Variance
        val variance = values
            .map { value ->
                val difference = value - mean
                difference * difference
            }
            .average()
            .toFloat()

        return SignalStatistics(
            mean = mean,
            median = median,
            standardDeviation = sqrt(variance),
            min = values.minOrNull() ?: 0f,
            max = values.maxOrNull() ?: 0f
        )
    }

    private data class SignalStatistics(
        val mean: Float,
        val median: Float,
        val standardDeviation: Float,
        val min: Float,
        val max: Float
    )
}