package com.example.padeltracker.shared.sensors

/**
 * Represents a 3-axis IMU sensor value.
 *
 * For the accelerometer, values are expressed in m/s².
 * For the gyroscope, values are expressed in rad/s.
 */
data class ImuVector(
    val x: Float,
    val y: Float,
    val z: Float
) {
    companion object {
        fun from(values: FloatArray): ImuVector {
            require(values.size >= 3) {
                "IMU vector requires at least 3 values."
            }

            return ImuVector(
                x = values[0],
                y = values[1],
                z = values[2]
            )
        }
    }
}

/**
 * Represents one paired IMU sample.
 *
 * A paired sample contains the accelerometer and gyroscope values that are
 * considered part of the same sampling step.
 */
data class PairedImuSample(
    val timestampNanos: Long,
    val accelerometer: ImuVector,
    val gyroscope: ImuVector
)
