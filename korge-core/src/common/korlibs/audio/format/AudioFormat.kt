@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package korlibs.audio.format

import korlibs.audio.internal.*
import korlibs.audio.sound.*
import korlibs.encoding.*
import korlibs.datastructure.*
import korlibs.io.file.*
import korlibs.io.lang.*
import korlibs.io.stream.*
import korlibs.number.*
import korlibs.time.*
import kotlinx.coroutines.*
import kotlin.coroutines.cancellation.CancellationException
import kotlin.native.concurrent.*

open class AudioFormat(vararg exts: String) {
	open val extensions: Set<String> = exts.map { it.lowercase().trim() }.toSet()

	data class Info(
    var duration: TimeSpan? = 0.seconds,
    var channels: Int = 2,
    var decodingTime: TimeSpan? = null
	) : Extra by Extra.Mixin() {
    val durationNotNull: TimeSpan get() = duration ?: 0.seconds
		override fun toString(): String = "Info(duration=${durationNotNull.milliseconds.niceStr}ms, channels=$channels)"
	}

	open suspend fun tryReadInfo(data: AsyncStream, props: AudioDecodingProps = AudioDecodingProps.DEFAULT): Info? = null
	protected open suspend fun decodeStreamInternal(data: AsyncStream, props: AudioDecodingProps = AudioDecodingProps.DEFAULT): AudioStream? = null
    suspend fun decodeStream(data: AsyncStream, props: AudioDecodingProps = AudioDecodingProps.DEFAULT): AudioStream? {
        return if (props.dispatcher != null) {
            withContext(props.dispatcher) { decodeStreamInternal(data, props) }
        } else {
            decodeStreamInternal(data, props)
        }
    }
	suspend fun decode(data: AsyncStream, props: AudioDecodingProps = AudioDecodingProps.DEFAULT): AudioData? = decodeStream(data, props)?.toData(props.maxSamples)
	suspend fun decode(data: ByteArray, props: AudioDecodingProps = AudioDecodingProps.DEFAULT): AudioData? = decodeStream(data.openAsync(), props)?.toData(props.maxSamples)
	open suspend fun encode(data: AudioData, out: AsyncOutputStream, filename: String, props: AudioEncodingProps = AudioEncodingProps.DEFAULT): Unit = unsupported()

    open suspend fun decodeStreamOrError(data: AsyncStream, props: AudioDecodingProps): AudioStream =
        decodeStream(data, props)
            ?: error("Can't decode audio stream [$this] ${data.duplicate().readBytesUpTo(8).hex}")

    suspend fun encodeToByteArray(
		data: AudioData,
		filename: String = "out.wav",
		format: AudioFormat = this,
        props: AudioEncodingProps = AudioEncodingProps.DEFAULT
	): ByteArray = MemorySyncStreamToByteArray { format.encode(data, this.toAsync(), filename, props) }

    open val name: String get() = "AudioFormat"
	override fun toString(): String = "$name(${extensions.sorted()})"
}

data class AudioDecodingProps(
    val exactTimings: Boolean? = null,
    val readInMemory: Boolean = true,
    val formats: AudioFormat? = null,
    val maxSamples: Int = 15 * 60 * 44100,
    val dispatcher: CoroutineDispatcher? = null,
) : Extra by Extra.Mixin() {
    //var readInMemory: Boolean = true

    companion object {
        val DEFAULT = AudioDecodingProps()
        val FAST = AudioDecodingProps(false, false)
    }
}

fun AudioFormat.toProps(): AudioDecodingProps = AudioDecodingProps(formats = this)

data class AudioEncodingProps(
    val quality: Double = 0.84,
    val filename: String? = null
) : Extra by Extra.Mixin() {
    companion object {
        val DEFAULT = AudioEncodingProps()
    }
}

open class InvalidAudioFormatException(message: String) : RuntimeException(message)

fun invalidAudioFormat(message: String = "invalid audio format"): Nothing = throw InvalidAudioFormatException(message)

val defaultAudioFormats by lazy { standardAudioFormats() }

class AudioFormats : AudioFormat() {
	val formats = arrayListOf<AudioFormat>()

    private var _extensions: Set<String>? = null

    override val extensions: Set<String> get() {
        if (_extensions == null) {
            _extensions = formats.flatMap { it.extensions }.toSet()
        }
        return _extensions!!
    }

    companion object {
        operator fun invoke(vararg formats: AudioFormat) = AudioFormats().register(*formats)
        operator fun invoke(formats: Iterable<AudioFormat>) = AudioFormats().register(formats)
    }

    private fun invalidate(): AudioFormats {
        _extensions = null
        return this
    }

    fun register(formats: AudioFormats): AudioFormats = invalidate().apply { this.formats += formats.formats }
	fun register(vararg formats: AudioFormat): AudioFormats = invalidate().apply { this.formats += formats }
	fun register(formats: Iterable<AudioFormat>): AudioFormats = invalidate().apply { this.formats += formats }
    fun registerFirst(vararg formats: AudioFormat): AudioFormats = invalidate().apply { this.formats.addAll(0, formats.toList()) }

	override suspend fun tryReadInfo(data: AsyncStream, props: AudioDecodingProps): Info? {
		//println("formats:$formats")
		for (format in formats) {
			try {
				return format.tryReadInfo(data.duplicate(), props) ?: continue
            } catch (e: CancellationException) {
                throw e
			} catch (e: Throwable) {
				e.printStackTrace()
			}
		}
		return null
	}

	override suspend fun decodeStreamInternal(data: AsyncStream, props: AudioDecodingProps): AudioStream? {
		//println(formats)
		for (format in formats) {
			try {
				if (format.tryReadInfo(data.duplicate(), AudioDecodingProps.FAST) == null) continue
				return format.decodeStream(data.duplicate(), props) ?: continue
			} catch (e: Throwable) {
				e.printStackTrace()
			}
		}
		return null
	}

	override suspend fun encode(data: AudioData, out: AsyncOutputStream, filename: String, props: AudioEncodingProps) {
		val ext = PathInfo(filename).extensionLC
		val format = formats.firstOrNull { ext in it.extensions }
				?: throw UnsupportedOperationException("Don't know how to generate file for extension '$ext'")
		return format.encode(data, out, filename)
	}

    operator fun plus(other: AudioFormat): AudioFormats = AudioFormats(formats + other)
    operator fun plus(other: Iterable<AudioFormat>): AudioFormats = AudioFormats(formats + other)
}

suspend fun VfsFile.readSoundInfo(formats: AudioFormat = defaultAudioFormats, props: AudioDecodingProps = AudioDecodingProps.DEFAULT) =
	this.openUse { formats.tryReadInfo(this, props) }

fun standardAudioFormats(): AudioFormats = AudioFormats(WAV, MP3Decoder)
