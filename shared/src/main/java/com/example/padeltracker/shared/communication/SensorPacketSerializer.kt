package com.example.padeltracker.shared.communication

import com.example.padeltracker.shared.experiment.ExperimentMode
import com.example.padeltracker.shared.sensors.ImuVector
import com.example.padeltracker.shared.sensors.PairedImuSample
import com.example.padeltracker.shared.shotrecognition.ShotFeatureVector
import com.example.padeltracker.shared.shotrecognition.ShotWindow
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Serializes and deserializes sensor packets exchanged between watch and phone.
 *
 * Public entry points:
 * - serialize(mode, packet)
 * - deserialize(mode, data)
 *
 * Specific packet formats are kept here so communication logic is centralized.
 */
object SensorPacketSerializer {

    private const val INT_BYTES = 4
    private const val LONG_BYTES = 8
    private const val FLOAT_BYTES = 4
    private const val AXES_PER_SENSOR = 3
    private const val SENSORS_PER_SAMPLE = 2

    private const val SCORE_HEADER_INTS = 4
    private const val SCORE_HEADER_BYTES = SCORE_HEADER_INTS * INT_BYTES

    private const val BYTES_PER_IMU_VECTOR = AXES_PER_SENSOR * FLOAT_BYTES
    private const val BYTES_PER_PAIRED_SAMPLE = SENSORS_PER_SAMPLE * BYTES_PER_IMU_VECTOR

    private const val RAW_SAMPLE_BYTES = INT_BYTES + LONG_BYTES + BYTES_PER_IMU_VECTOR

    /**
     * Main serialization entry point.
     *
     * The caller provides the experiment mode and the packet to serialize.
     */
    fun serialize(
        mode: ExperimentMode,
        packet: SensorPacket
    ): ByteArray {
        return when (mode) {
            ExperimentMode.RAW_TO_PHONE -> {
                serializeRawSensorSample(packet.requireType<SensorPacket.RawSensorSample>())
            }

            ExperimentMode.DATA_COLLECTION -> {
                serializeShotWindowForDataCollection(packet.requireType<SensorPacket.DataCollectionShotWindow>())
            }

            ExperimentMode.SHOT_TO_PHONE -> {
                serializeShotWindowForClassification(packet.requireType<SensorPacket.ShotWindowPacket>())
            }

            ExperimentMode.FEATURES_TO_PHONE -> {
                serializeFeatureVector(packet.requireType<SensorPacket.FeatureVector>())
            }
        }
    }

    /**
     * Main deserialization entry point.
     *
     * The caller provides the experiment mode so the correct packet format is used.
     */
    fun deserialize(
        mode: ExperimentMode,
        data: ByteArray
    ): SensorPacket {
        return when (mode) {
            ExperimentMode.RAW_TO_PHONE -> deserializeRawSensorSample(data)
            ExperimentMode.DATA_COLLECTION -> deserializeShotWindowForDataCollection(data)
            ExperimentMode.SHOT_TO_PHONE -> deserializeShotWindowForClassification(data)
            ExperimentMode.FEATURES_TO_PHONE -> deserializeFeatureVector(data)
        }
    }

    /**
     * Serializes one raw IMU sample.
     *
     * Packet format:
     * - sensorType: Int
     * - timestampNanos: Long
     * - x: Float
     * - y: Float
     * - z: Float
     */
    fun serializeRawSensorSample(
        packet: SensorPacket.RawSensorSample
    ): ByteArray {
        val buffer = ByteBuffer.allocate(RAW_SAMPLE_BYTES)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        buffer.putInt(packet.sensorType)
        buffer.putLong(packet.timestampNanos)
        buffer.putImuVector(packet.value)

        return buffer.array()
    }

    fun deserializeRawSensorSample(data: ByteArray): SensorPacket.RawSensorSample {
        require(data.size == RAW_SAMPLE_BYTES) {
            "Invalid raw sensor packet size: ${data.size} bytes."
        }

        val buffer = ByteBuffer.wrap(data)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        return SensorPacket.RawSensorSample(
            sensorType = buffer.int,
            timestampNanos = buffer.long,
            value = buffer.readImuVector()
        )
    }

    /**
     * Serializes a shot window for dataset collection.
     *
     * Packet format:
     * - score header
     * - accelerometer samples
     * - gyroscope samples
     */
    fun serializeShotWindowForDataCollection(
        packet: SensorPacket.DataCollectionShotWindow
    ): ByteArray {
        return serializeShotWindowInternal(
            shotWindow = packet.shotWindow,
            scoreHeader = packet.scoreHeader
        )
    }

    /**
     * Deserializes a dataset collection shot packet.
     *
     * This packet includes match score context.
     */
    fun deserializeShotWindowForDataCollection(
        data: ByteArray
    ): SensorPacket.DataCollectionShotWindow {
        val deserialized = deserializeShotWindowInternal(
            data = data,
            hasScoreHeader = true
        )

        val scoreHeader = requireNotNull(deserialized.scoreHeader) {
            "Dataset collection shot packet must contain a score header."
        }

        return SensorPacket.DataCollectionShotWindow(
            shotWindow = deserialized.shotWindow,
            scoreHeader = scoreHeader
        )
    }

    /**
     * Serializes a shot window for classification.
     *
     * Packet format:
     * - accelerometer samples
     * - gyroscope samples
     *
     * No score header is included.
     */
    fun serializeShotWindowForClassification(
        packet: SensorPacket.ShotWindowPacket
    ): ByteArray {
        return serializeShotWindowInternal(
            shotWindow = packet.shotWindow,
            scoreHeader = null
        )
    }

    /**
     * Deserializes a classification shot packet.
     *
     * This packet does not include match score context.
     */
    fun deserializeShotWindowForClassification(
        data: ByteArray
    ): SensorPacket.ShotWindowPacket {
        val deserialized = deserializeShotWindowInternal(
            data = data,
            hasScoreHeader = false
        )

        return SensorPacket.ShotWindowPacket(
            shotWindow = deserialized.shotWindow
        )
    }

    /**
     * Serializes a feature vector.
     *
     * Packet format:
     * - feature count: Int
     * - feature values: FloatArray
     */
    fun serializeFeatureVector(
        packet: SensorPacket.FeatureVector
    ): ByteArray {
        val values = packet.values
        val buffer = ByteBuffer.allocate(INT_BYTES + values.size * FLOAT_BYTES)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        buffer.putInt(values.size)
        values.forEach { value ->
            buffer.putFloat(value)
        }

        return buffer.array()
    }

    fun deserializeFeatureVector(data: ByteArray): SensorPacket.FeatureVector {
        require(data.size >= INT_BYTES) {
            "Feature vector packet is too small: ${data.size} bytes."
        }

        val buffer = ByteBuffer.wrap(data)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        val featureCount = buffer.int
        val expectedSize = INT_BYTES + featureCount * FLOAT_BYTES

        require(data.size == expectedSize) {
            "Invalid feature vector packet size: ${data.size} bytes. Expected $expectedSize bytes."
        }

        val values = List(featureCount) {
            buffer.float
        }

        return SensorPacket.FeatureVector(values = values)
    }

    /**
     * Generic internal shot window serializer.
     *
     * If scoreHeader is not null, the score header is written before samples.
     * If scoreHeader is null, only shot samples are written.
     */
    private fun serializeShotWindowInternal(
        shotWindow: ShotWindow,
        scoreHeader: ScoreHeader?
    ): ByteArray {
        val numSamples = shotWindow.totalSamples
        val headerBytes = if (scoreHeader != null) SCORE_HEADER_BYTES else 0

        val buffer = ByteBuffer.allocate(
            headerBytes + numSamples * BYTES_PER_PAIRED_SAMPLE
        )
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        if (scoreHeader != null) {
            buffer.putScoreHeader(scoreHeader)
        }

        writeShotSamples(buffer, shotWindow)

        return buffer.array()
    }

    /**
     * Generic internal shot window deserializer.
     *
     * If hasScoreHeader is true, the first 4 Int values are read as score context.
     * Otherwise, the whole packet is interpreted as shot samples.
     */
    private fun deserializeShotWindowInternal(
        data: ByteArray,
        hasScoreHeader: Boolean
    ): InternalShotWindowPacket {
        val headerBytes = if (hasScoreHeader) SCORE_HEADER_BYTES else 0

        require(data.size >= headerBytes) {
            "Shot packet is too small: ${data.size} bytes."
        }

        val sampleBytes = data.size - headerBytes

        require(sampleBytes > 0) {
            "Shot packet does not contain sample data."
        }

        require(sampleBytes % BYTES_PER_PAIRED_SAMPLE == 0) {
            "Invalid shot packet size: ${data.size} bytes."
        }

        val numSamples = sampleBytes / BYTES_PER_PAIRED_SAMPLE

        val buffer = ByteBuffer.wrap(data)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        val scoreHeader = if (hasScoreHeader) {
            buffer.readScoreHeader()
        } else {
            null
        }

        val shotWindow = readShotWindow(
            buffer = buffer,
            numSamples = numSamples
        )

        return InternalShotWindowPacket(
            shotWindow = shotWindow,
            scoreHeader = scoreHeader
        )
    }


    private fun writeShotSamples(
        buffer: ByteBuffer,
        shotWindow: ShotWindow
    ) {
        // Accelerometer block
        shotWindow.samples.forEach { sample ->
            buffer.putImuVector(sample.accelerometer)
        }

        // Gyroscope block
        shotWindow.samples.forEach { sample ->
            buffer.putImuVector(sample.gyroscope)
        }
    }

    private fun readShotWindow(
        buffer: ByteBuffer,
        numSamples: Int
    ): ShotWindow {
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

        return ShotWindow(samples = pairedSamples)
    }

    private fun ByteBuffer.putScoreHeader(scoreHeader: ScoreHeader) {
        putInt(scoreHeader.teamASets)
        putInt(scoreHeader.teamBSets)
        putInt(scoreHeader.teamAGames)
        putInt(scoreHeader.teamBGames)
    }

    private fun ByteBuffer.readScoreHeader(): ScoreHeader {
        return ScoreHeader(
            teamASets = int,
            teamBSets = int,
            teamAGames = int,
            teamBGames = int
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

    private inline fun <reified T : SensorPacket> SensorPacket.requireType(): T {
        require(this is T) {
            "Invalid packet type. Expected ${T::class.simpleName}, got ${this::class.simpleName}."
        }

        return this
    }

    private data class InternalShotWindowPacket(
        val shotWindow: ShotWindow,
        val scoreHeader: ScoreHeader?
    )
}

/**
 * Score context attached only to dataset collection packets.
 */
data class ScoreHeader(
    val teamASets: Int,
    val teamBSets: Int,
    val teamAGames: Int,
    val teamBGames: Int
) {
    val scoreMarker: String
        get() = "S$teamASets-$teamBSets-G$teamAGames-$teamBGames"
}

/**
 * Sensor packet types supported by the communication layer.
 */
sealed class SensorPacket {

    data class RawSensorSample(
        val sensorType: Int,
        val timestampNanos: Long,
        val value: ImuVector
    ) : SensorPacket()

    data class DataCollectionShotWindow(
        val shotWindow: ShotWindow,
        val scoreHeader: ScoreHeader
    ) : SensorPacket()

    data class ShotWindowPacket(
        val shotWindow: ShotWindow
    ) : SensorPacket()

    data class FeatureVector(
        val values: List<Float>
    ) : SensorPacket() {
        constructor(featureVector: ShotFeatureVector) : this(
            values = featureVector.values
        )

        fun toFloatArray(): FloatArray {
            return values.toFloatArray()
        }
    }
}
