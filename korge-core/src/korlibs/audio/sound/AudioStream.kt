package korlibs.audio.sound

import korlibs.time.TimeSpan
import korlibs.time.seconds
import korlibs.audio.format.AudioDecodingProps
import korlibs.audio.format.AudioEncodingProps
import korlibs.audio.format.AudioFormat
import korlibs.audio.format.defaultAudioFormats
import korlibs.io.file.VfsFile
import korlibs.io.file.VfsOpenMode
import korlibs.io.file.baseName
import korlibs.io.lang.Closeable
import kotlin.math.min

abstract class AudioStream(
    val rate: Int,
    val channels: Int
) : AudioStreamable, Closeable {
    open val finished = false
    open val totalLengthInSamples: Long? = null
    val totalLength get() = ((totalLengthInSamples ?: 0L).toDouble() / rate.toDouble()).seconds
    open var currentPositionInSamples: Long = 0L
    var currentTime: TimeSpan
        set(value) { currentPositionInSamples = estimateSamplesFromTime(value) }
        get() = estimateTimeFromSamples(currentPositionInSamples)

    fun estimateSamplesFromTime(time: TimeSpan): Long = (time.seconds * rate.toDouble()).toLong()
    fun estimateTimeFromSamples(samples: Long): TimeSpan = (samples.toDouble() / rate.toDouble()).seconds

    open suspend fun read(out: AudioSamples, offset: Int = 0, length: Int = out.totalSamples): Int = 0
    override fun close() = Unit

    abstract suspend fun clone(): AudioStream
    override suspend fun toStream(): AudioStream = clone()

    companion object {
        fun generator(rate: Int, channels: Int, generateChunk: suspend AudioSamplesDeque.(step: Int) -> Boolean): AudioStream =
            GeneratorAudioStream(rate, channels, generateChunk)
    }

    internal class GeneratorAudioStream(rate: Int, channels: Int, val generateChunk: suspend AudioSamplesDeque.(step: Int) -> Boolean) : AudioStream(rate, channels) {
        val deque = AudioSamplesDeque(channels)
        val availableRead get() = deque.availableRead
        override var finished: Boolean = false
        private var step: Int = 0

        override suspend fun read(out: AudioSamples, offset: Int, length: Int): Int {
            if (finished && availableRead <= 0) return -1
            while (availableRead <= 0) {
                if (!generateChunk(deque, step++)) {
                    finished = true
                    break
                }
            }
            val read = min(length, availableRead)
            deque.read(out, offset, read)
            return read
        }

        override suspend fun clone(): AudioStream = GeneratorAudioStream(rate, channels, generateChunk)
    }
}

// default maxSamples is 15 minutes of data at 44100hz
val DEFAULT_MAX_SAMPLES = 15 * 60 * 44100

suspend fun AudioStream.toData(maxSamples: Int = DEFAULT_MAX_SAMPLES): AudioData {
    val out = AudioSamplesDeque(channels)
    val buffer = AudioSamples(channels, min(maxSamples, 16 * 4096))
    try {
        while (!finished) {
            val read = read(buffer, 0, min(maxSamples - out.availableRead, buffer.totalSamples))
            if (read <= 0) break
            val mread = min(read, maxSamples - out.availableRead)
            out.write(buffer, 0, mread)
            if (out.availableRead >= maxSamples) break
        }
    } finally {
        close()
    }

    val maxOutSamples = out.availableReadMax

    return AudioData(rate, AudioSamples(channels, maxOutSamples).apply { out.read(this) })
}

suspend fun AudioStream.playAndWait(params: PlaybackParameters = PlaybackParameters.DEFAULT) = nativeSoundProvider.playAndWait(this, params)
suspend fun AudioStream.playAndWait(times: PlaybackTimes = 1.playbackTimes, startTime: TimeSpan = 0.seconds, bufferTime: TimeSpan = 0.1.seconds) = nativeSoundProvider.createStreamingSound(this).playAndWait(PlaybackParameters(times, startTime, bufferTime))

suspend fun AudioStream.toSound(closeStream: Boolean = false, name: String = "Unknown"): Sound =
    nativeSoundProvider.createStreamingSound(this, closeStream, name)

suspend fun AudioStream.toSound(closeStream: Boolean = false, name: String = "Unknown", onComplete: (suspend () -> Unit)? = null): Sound =
    nativeSoundProvider.createStreamingSound(this, closeStream, name, onComplete)

suspend fun VfsFile.readAudioStreamOrNull(formats: AudioFormat = defaultAudioFormats + nativeSoundProvider.audioFormats, props: AudioDecodingProps = AudioDecodingProps.DEFAULT) = formats.decodeStream(this.open(), props)
suspend fun VfsFile.readAudioStream(formats: AudioFormat = defaultAudioFormats + nativeSoundProvider.audioFormats, props: AudioDecodingProps = AudioDecodingProps.DEFAULT) =
    readAudioStreamOrNull(formats, props)
        ?: error("Can't decode audio stream")

suspend fun VfsFile.writeAudio(data: AudioData, formats: AudioFormat = defaultAudioFormats + nativeSoundProvider.audioFormats, props: AudioEncodingProps = AudioEncodingProps.DEFAULT) =
    this.openUse(VfsOpenMode.CREATE_OR_TRUNCATE) {
        formats.encode(data, this, this@writeAudio.baseName, props)
    }
