@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korau.format

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.korau.internal.*
import com.soywiz.korau.sound.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import kotlin.coroutines.cancellation.*

open class AudioFormat(vararg exts: String) {
	val extensions = exts.map { it.toLowerCase().trim() }.toSet()

	data class Info(
		var duration: TimeSpan = 0.seconds,
		var channels: Int = 2,
        var decodingTime: TimeSpan? = null
	) : Extra by Extra.Mixin() {
		override fun toString(): String = "Info(duration=${duration.milliseconds.niceStr}ms, channels=$channels)"
	}

	open suspend fun tryReadInfo(data: AsyncStream, props: AudioDecodingProps = AudioDecodingProps.DEFAULT): Info? = null
	open suspend fun decodeStream(data: AsyncStream, props: AudioDecodingProps = AudioDecodingProps.DEFAULT): AudioStream? = null
	suspend fun decode(data: AsyncStream, props: AudioDecodingProps = AudioDecodingProps.DEFAULT): AudioData? = decodeStream(data, props)?.toData()
	suspend fun decode(data: ByteArray, props: AudioDecodingProps = AudioDecodingProps.DEFAULT): AudioData? = decodeStream(data.openAsync(), props)?.toData()
	open suspend fun encode(data: AudioData, out: AsyncOutputStream, filename: String, props: AudioEncodingProps = AudioEncodingProps.DEFAULT): Unit = unsupported()

	suspend fun encodeToByteArray(
		data: AudioData,
		filename: String = "out.wav",
		format: AudioFormat = this,
        props: AudioEncodingProps = AudioEncodingProps.DEFAULT
	): ByteArray = MemorySyncStreamToByteArray { format.encode(data, this.toAsync(), filename, props) }

	override fun toString(): String = "AudioFormat(${extensions.sorted()})"
}

open class AudioDecodingProps(
    val exactTimings: Boolean? = null,
    val readInMemory: Boolean = true,
) {
    //var readInMemory: Boolean = true

    companion object {
        val DEFAULT = AudioDecodingProps()
        val FAST = AudioDecodingProps(false, false)
    }
}

open class AudioEncodingProps(
    val quality: Double = 0.84,
    val filename: String? = null
) {
    companion object {
        val DEFAULT = AudioEncodingProps()
    }
}

open class InvalidAudioFormatException(message: String) : RuntimeException(message)

fun invalidAudioFormat(message: String = "invalid audio format"): Nothing = throw InvalidAudioFormatException(message)

val defaultAudioFormats by lazy { standardAudioFormats() }

class AudioFormats : AudioFormat() {
	val formats = linkedSetOf<AudioFormat>()

    companion object {
        operator fun invoke(vararg formats: AudioFormat) = AudioFormats().register(*formats)
        operator fun invoke(formats: Iterable<AudioFormat>) = AudioFormats().register(formats)
    }

    fun register(formats: AudioFormats): AudioFormats = this.apply { this.formats += formats.formats }
	fun register(vararg formats: AudioFormat): AudioFormats = this.apply { this.formats += formats }
	fun register(formats: Iterable<AudioFormat>): AudioFormats = this.apply { this.formats += formats }

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

	override suspend fun decodeStream(data: AsyncStream, props: AudioDecodingProps): AudioStream? {
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

    suspend fun decodeStreamOrError(data: AsyncStream, props: AudioDecodingProps): AudioStream =
        decodeStream(data, props)
            ?: error("Can't decode audio stream [$formats]")

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

fun standardAudioFormats(): AudioFormats = AudioFormats(WAV, OGG, MP3)
