package com.soywiz.korau.sound

import com.soywiz.klock.TimeSpan
import com.soywiz.klock.seconds
import com.soywiz.kmem.arraycopy
import com.soywiz.korau.format.AudioDecodingProps
import com.soywiz.korau.format.AudioFormats
import com.soywiz.korau.format.defaultAudioFormats
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.lang.invalidOp
import kotlin.math.min

class AudioData(
    val rate: Int,
    val samples: AudioSamples
) {
    val samplesInterleaved by lazy { samples.interleaved() }

    companion object {
        val DUMMY by lazy { AudioData(44100, AudioSamples(2, 0)) }
    }

    val stereo get() = channels > 1
    val channels get() = samples.channels
    val totalSamples get() = samples.totalSamples
    val totalTime: TimeSpan get() = timeAtSample(totalSamples)
    fun timeAtSample(sample: Int) = ((sample).toDouble() / rate.toDouble()).seconds

    operator fun get(channel: Int): ShortArray = samples.data[channel]
    operator fun get(channel: Int, sample: Int): Short = samples.data[channel][sample]

    operator fun set(channel: Int, sample: Int, value: Short) { samples.data[channel][sample] = value }

    override fun toString(): String = "AudioData(rate=$rate, channels=$channels, samples=$totalSamples)"
}

enum class AudioConversionQuality { FAST }

/** Change the rate, changing the pitch and the duration of the sound. */
fun AudioData.withRate(rate: Int) = AudioData(rate, samples)

// @TODO: Use FFT
//fun AudioData.withAdjustedPitch(pitch: Double = 1.0): AudioData {
//    val MAX_CHUNK_SIZE = 1024
//    val inp = this
//    val out = AudioData(inp.rate, AudioSamples(inp.samples.channels, inp.totalSamples))
//    for (chunk in 0 until ceil(out.totalSamples.toDouble() / MAX_CHUNK_SIZE).toInt()) {
//        val offset = chunk * MAX_CHUNK_SIZE
//        val available = out.totalSamples - offset
//        val chunkSize = min(MAX_CHUNK_SIZE, available)
//        //println("chunk=$chunk, offset=$offset, available=$available, chunkSize=$chunkSize")
//        for (channel in 0 until channels) {
//            for (n in 0 until chunkSize) {
//                val m = (n * pitch).toInt() % chunkSize
//                //println("n=$n, m=$m")
//                //println(" -> SAMPLE: ${this[channel, m]}")
//                out[channel, offset + n] = this[channel, offset + m]
//            }
//        }
//    }
//    return out
//}

//fun AudioData.convertTo(rate: Int = 44100, channels: Int = 2, quality: AudioConversionQuality = AudioConversionQuality.FAST): AudioData {
//    TODO()
//}

fun AudioData.toStream(): AudioStream = AudioDataStream(this)

class AudioDataStream(val data: AudioData) : AudioStream(data.rate, data.channels) {
    var cursor = 0
    override var finished: Boolean = false

    override suspend fun read(out: AudioSamples, offset: Int, length: Int): Int {
        val available = data.samples.totalSamples - cursor
        val toread = min(available, length)
        if (toread > 0) {
            for (n in 0 until channels) {
                arraycopy(data.samples[n], cursor, out[n], offset, toread)
            }
            cursor += toread
        }
        if (toread <= 0) finished = true
        return toread
    }

    override suspend fun clone(): AudioStream = AudioDataStream(data)
}


suspend fun AudioData.toSound() = nativeSoundProvider.createSound(this)

suspend fun VfsFile.readAudioData(formats: AudioFormats = defaultAudioFormats, props: AudioDecodingProps = AudioDecodingProps.DEFAULT) =
    this.openUse { formats.decode(this, props) ?: invalidOp("Can't decode audio file ${this@readAudioData}") }
