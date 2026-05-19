package com.example.padeltracker.shared.communication

import com.example.padeltracker.shared.sensors.ImuVector
import com.example.padeltracker.shared.sensors.PairedImuSample
import com.example.padeltracker.shared.shotrecognition.ShotWindow
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Serializes and deserializes sensor packets exchanged between watch and phone.
 *
 * This keeps the binary packet format centralized
 */
object SensorPacketSerializer {

    private const val INT_BYTES = 4
    private const val FLOAT_BYTES = 4
    private const val AXES_PER_SENSOR = 3
    private const val SENSORS_PER_SAMPLE = 2

    private const val SCORE_HEADER_INTS = 4
    private const val SCORE_HEADER_BYTES = SCORE_HEADER_INTS * INT_BYTES

    private const val BYTES_PER_SENSOR_SAMPLE = AXES_PER_SENSOR * FLOAT_BYTES
    private const val BYTES_PER_PAIRED_SAMPLE = SENSORS_PER_SAMPLE * BYTES_PER_SENSOR_SAMPLE

    /**
     * Serializes a detected shot window using the current packet format:
     *
     * Header:
     * - teamASets: Int
     * - teamBSets: Int
     * - teamAGames: Int
     * - teamBGames: Int
     *
     * Payload:
     * - all accelerometer samples first
     * - all gyroscope samples after
     */
    fun serializeShotWindow(
        shotWindow: ShotWindow,
        teamASets: Int,
        teamBSets: Int,
        teamAGames: Int,
        teamBGames: Int
    ): ByteArray {
        val numSamples = shotWindow.totalSamples

        val buffer = ByteBuffer.allocate(
            SCORE_HEADER_BYTES + numSamples * BYTES_PER_PAIRED_SAMPLE
        )
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        buffer.putInt(teamASets)
        buffer.putInt(teamBSets)
        buffer.putInt(teamAGames)
        buffer.putInt(teamBGames)

        shotWindow.samples.forEach { sample ->
            buffer.putImuVector(sample.accelerometer)
        }

        shotWindow.samples.forEach { sample ->
            buffer.putImuVector(sample.gyroscope)
        }

        return buffer.array()
    }

    /**
     * Deserializes a shot packet received from the watch.
     *
     * Note: timestamps are not currently sent in the packet, so deserialized
     * PairedImuSample objects use timestampNanos = 0L.
     */
    fun deserializeShotWindow(data: ByteArray): DeserializedShotWindowPacket {
        require(data.size >= SCORE_HEADER_BYTES) {
            "Shot packet is too small: ${data.size} bytes."
        }

        val sampleBytes = data.size - SCORE_HEADER_BYTES

        require(sampleBytes % BYTES_PER_PAIRED_SAMPLE == 0) {
            "Invalid shot packet size: ${data.size} bytes."
        }

        val numSamples = sampleBytes / BYTES_PER_PAIRED_SAMPLE

        val buffer = ByteBuffer.wrap(data)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        val teamASets = buffer.int
        val teamBSets = buffer.int
        val teamAGames = buffer.int
        val teamBGames = buffer.int

        val accelerometerSamples = MutableList(numSamples) {
            buffer.readImuVector()
        }

        val gyroscopeSamples = MutableList(numSamples) {
            buffer.readImuVector()
        }

        val pairedSamples = List(numSamples) { index ->
            PairedImuSample(
                timestampNanos = 0L,
                accelerometer = accelerometerSamples[index],
                gyroscope = gyroscopeSamples[index]
            )
        }

        return DeserializedShotWindowPacket(
            shotWindow = ShotWindow(samples = pairedSamples),
            teamASets = teamASets,
            teamBSets = teamBSets,
            teamAGames = teamAGames,
            teamBGames = teamBGames
        )
    }

    private fun ByteBuffer.putImuVector(vector: ImuVector) {
        putFloat(vector.x)
        putFloat(vector.y)
        putFloat(vector.z)
    }

    private fun ByteBuffer.readImuVector(): ImuVector {
        return ImuVector(
            x = float,
            y = float,
            z = float
        )
    }
}

/**
 * Deserialized shot packet with both the IMU window and the match score context.
 */
data class DeserializedShotWindowPacket(
    val shotWindow: ShotWindow,
    val teamASets: Int,
    val teamBSets: Int,
    val teamAGames: Int,
    val teamBGames: Int
) {
    val scoreMarker: String
        get() = "S$teamASets-$teamBSets-G$teamAGames-$teamBGames"
}