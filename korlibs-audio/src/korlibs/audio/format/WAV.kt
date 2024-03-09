@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package korlibs.audio.format

import korlibs.audio.internal.*
import korlibs.audio.sound.*
import korlibs.io.annotations.*
import korlibs.io.async.*
import korlibs.io.lang.*
import korlibs.io.stream.*
import korlibs.memory.*
import korlibs.time.*
import kotlin.coroutines.cancellation.*

@Keep
open class WAV : AudioFormat("wav") {
	companion object : WAV()

	data class Chunk(val type: String, val data: AsyncStream)
	data class ProcessedChunk(val type: String, val data: AsyncStream, val extra: Any)

	override suspend fun tryReadInfo(data: AsyncStream, props: AudioDecodingProps): Info? = try {
		parse(data) { }
    } catch (e: CancellationException) {
        throw e
	} catch (e: Throwable) {
        //println("DATA: data.size=${data.size()}")
		//e.printStackTrace()
		null
	}

	override suspend fun decodeStreamInternal(data: AsyncStream, props: AudioDecodingProps): AudioStream? {
		var fmt = Fmt()
		var buffer = MemorySyncStream().toAsync()
		parse(data) {
			val extra = it.extra
			when (extra) {
				is Fmt -> fmt = extra
			}
			if (it.type == "data") buffer = it.data
		}

		return WavAudioStream(fmt, buffer, buffer.getLength(), data, props)
	}

    class WavAudioStream(val fmt: Fmt, val buffer: AsyncStream, val bufferLength: Long, val data: AsyncStream, val props: AudioDecodingProps) : AudioStream(fmt.samplesPerSec, fmt.channels) {
        val bytesPerSample: Int = fmt.bytesPerSample
        override var finished: Boolean = false

        override val totalLengthInSamples: Long? get() = bufferLength / bytesPerSample
        override var currentPositionInSamples: Long
            get() = buffer.position / bytesPerSample
            set(value) {
                finished = false
                buffer.position = value * bytesPerSample
            }

        override suspend fun read(out: AudioSamples, offset: Int, length: Int): Int {
            //println("fmt: $fmt")
            val bytes = buffer.readBytesUpTo(length * bytesPerSample * channels)
            finished = buffer.eof()
            val availableSamples = bytes.size / bytesPerSample / channels
            for (channel in 0 until channels) {
                when (bytesPerSample) {
                    1 -> readBlock(channel, channels, availableSamples, bytesPerSample, out, offset) { ((bytes.getU8(it) - 128) * 255).coerceToShort() }
                    2 -> readBlock(channel, channels, availableSamples, bytesPerSample, out, offset) {
                        (bytes.getS16LE(
                            it
                        ).toShort()) }
                    3 -> readBlock(channel, channels, availableSamples, bytesPerSample, out, offset) {
                        (bytes.getS24LE(
                            it
                        ) ushr 8).toShort() }
                    else -> invalidOp("Unsupported bytesPerSample=$bytesPerSample")
                }
            }
            return availableSamples
        }

        override suspend fun clone(): AudioStream = WAV().decodeStreamInternal(data.duplicate(), props)!!
    }

	internal inline fun readBlock(channel: Int, channels: Int, availableSamples: Int, bytesPerSample: Int, out: AudioSamples, offset: Int, read: (index: Int) -> Short) {
		val increment = channels * bytesPerSample
		var index = channel * bytesPerSample
		val outc = out[channel]
		for (n in 0 until availableSamples) {
			outc[offset + n] = read(index)
			index += increment
		}
	}

	override suspend fun encode(data: AudioData, out: AsyncOutputStream, filename: String, props: AudioEncodingProps) {
        val bytesPerSample = 2

		// HEADER
		out.writeString("RIFF")
		out.write32LE(0x24 + data.samples.totalSamples * bytesPerSample * data.channels) // length
		out.writeString("WAVE")

		// FMT
		out.writeString("fmt ")
		out.write32LE(0x10)
		out.write16LE(1) // PCM
		out.write16LE(data.channels) // Channels
		out.write32LE(data.rate) // SamplesPerSec
		out.write32LE(data.rate * data.channels * bytesPerSample) // AvgBytesPerSec
		out.write16LE(bytesPerSample) // BlockAlign
		out.write16LE(bytesPerSample * 8) // BitsPerSample

		// DATA
		out.writeString("data")
		out.write32LE(data.samples.totalSamples * bytesPerSample * data.channels)
        val array = data.samples.interleaved().data
        out.writeShortArrayLE(array)
	}

	data class Fmt(
		var formatTag: Int = -1, // CM = 1 (i.e. Linear quantization) Values other than 1 indicate some form of compression.
		var channels: Int = 2, // Mono = 1, Stereo = 2, etc.
		var samplesPerSec: Int = 44100, // 8000, 44100, etc.
		var avgBytesPerSec: Long = 0L, // == SampleRate * NumChannels * BitsPerSample/8
		var blockAlign: Int = 0, // == NumChannels * BitsPerSample/8 The number of bytes for one sample including all channels. I wonder what happens when this number isn't an integer?
		var bitsPerSample: Int = 0      // 8 bits = 8, 16 bits = 16, etc.
	) {
        val bytesPerSample get() = bitsPerSample / 8
    }

	suspend fun parse(data: AsyncStream, handle: (ProcessedChunk) -> Unit): Info {
		val fmt = Fmt()
		var dataSize = 0L

		riff(data) {
			val (type, d2) = this
			val d = d2.duplicate()
			var cdata: Any = Unit
			when (type) {
				"fmt " -> {
					fmt.formatTag = d.readS16LE()
					fmt.channels = d.readS16LE()
					fmt.samplesPerSec = d.readS32LE()
					fmt.avgBytesPerSec = d.readU32LE()
					fmt.blockAlign = d.readS16LE()
					fmt.bitsPerSample = d.readS16LE()
					cdata = fmt
				}
				"data" -> {
					dataSize += d.getLength()
					cdata = d
				}
				else -> Unit
			}
			handle(ProcessedChunk(this.type, this.data, cdata))
		}
		if (fmt.formatTag < 0) invalidOp("Couldn't find RIFF 'fmt ' chunk")

		return Info(
			duration = ((dataSize * 1000 * 1000) / fmt.avgBytesPerSec).microseconds,
			channels = fmt.channels
		)
	}

	suspend fun riff(data: AsyncStream, handler: suspend Chunk.() -> Unit) {
		//println("riff.data.size(): ${data.size()}")
		val s2 = data.duplicate()
		//val s2 = data.readAll().openAsync()
        if (s2.getAvailable() < 12) error("Not enough data for a RIFF file")
		val magic = s2.readString(4)
		val length = s2.readS32LE()
		val magic2 = s2.readString(4)
		if (magic != "RIFF") invalidAudioFormat("Not a RIFF file but '$magic'")
		if (magic2 != "WAVE") invalidAudioFormat("Not a RIFF + WAVE file")
		val s = s2.readStream(length - 4)
		while (!s.eof()) {
            if (s.getAvailable() < 8L) break
			val type = s.readString(4)
			val size = s.readS32LE()
            if (s.getAvailable() < size) break
			val d = s.readStream(size)
			handler(Chunk(type, d))
		}
	}
}

fun AudioData.toWav() = runBlockingNoSuspensions { WAV.encodeToByteArray(this) }
