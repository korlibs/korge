package com.soywiz.korau.sound

import com.soywiz.kmem.*
import com.soywiz.korau.internal.*

interface IAudioSamples {
    val channels: Int
    val totalSamples: Int
    fun isEmpty() = totalSamples == 0
    fun isNotEmpty() = totalSamples != 0
    operator fun get(channel: Int, sample: Int): Short
    operator fun set(channel: Int, sample: Int, value: Short): Unit
    fun getFloat(channel: Int, sample: Int): Float = SampleConvert.shortToFloat(this[channel, sample])
    fun setFloat(channel: Int, sample: Int, value: Float) = run { this[channel, sample] = SampleConvert.floatToShort(value) }
}

internal fun AudioSamples.resample(scale: Double, totalSamples: Int = (this.totalSamples * scale).toInt(), out: AudioSamples = AudioSamples(channels, totalSamples)): AudioSamples {
    val iscale = 1.0 / scale
    for (c in 0 until channels) {
        val outc = out[c]
        val inpc = this[c]
        for (n in 0 until totalSamples) {
            // @TODO: Increase quality
            outc[n] = inpc[(n * iscale).toInt()]
        }
    }
    return out
}

class AudioSamples(override val channels: Int, override val totalSamples: Int, val data: Array<ShortArray> = Array(channels) { ShortArray(totalSamples) }) : IAudioSamples {
    //val interleaved by lazy { interleaved() }


    operator fun get(channel: Int): ShortArray = data[channel]

    override operator fun get(channel: Int, sample: Int): Short = data[channel][sample]
    override operator fun set(channel: Int, sample: Int, value: Short) = run { data[channel][sample] = value }

    override fun hashCode(): Int = channels + totalSamples * 32 + data.contentDeepHashCode() * 64
    override fun equals(other: Any?): Boolean = (other is AudioSamples) && this.channels == other.channels && this.totalSamples == other.totalSamples && this.data.contentDeepEquals(other.data)

    override fun toString(): String = "AudioSamples(channels=$channels, totalSamples=$totalSamples)"
}

class AudioSamplesInterleaved(override val channels: Int, override val totalSamples: Int, val data: ShortArray = ShortArray(totalSamples * channels)) : IAudioSamples {
    //val separared by lazy { separated() }


    private fun index(channel: Int, sample: Int) = (sample * channels) + channel
    override operator fun get(channel: Int, sample: Int): Short = data[index(channel, sample)]
    override operator fun set(channel: Int, sample: Int, value: Short) = run { data[index(channel, sample)] = value }

    override fun toString(): String = "AudioSamplesInterleaved(channels=$channels, totalSamples=$totalSamples)"
}

fun AudioSamples.copyOfRange(start: Int, end: Int): AudioSamples {
    val out = AudioSamples(channels, end - start)
    for (n in 0 until channels) {
        arraycopy(this[n], start, out[n], 0, end - start)
    }
    return out
}

fun IAudioSamples.interleaved(out: AudioSamplesInterleaved = AudioSamplesInterleaved(channels, totalSamples)): AudioSamplesInterleaved {
    val channels = channels
    for (c in 0 until channels) {
        var m = c
        for (n in 0 until totalSamples) {
            out.data[m] = this[c, n]
            m += channels
        }
    }
    return out
}

fun AudioSamplesInterleaved.applyProps(speed: Double, panning: Double, volume: Double): AudioSamplesInterleaved {
    if (speed == 1.0 && panning == 0.0 && volume == 1.0) return this
    val speedf = speed.toFloat()
    val ispeedf = (1.0 / speed).toFloat()
    val out = AudioSamplesInterleaved(channels, (totalSamples * ispeedf).toInt())

    val rratio = ((((panning + 1.0) / 2.0).clamp01()) * volume).toFloat()
    val lratio = ((1.0 - rratio) * volume).toFloat()

    if (channels == 2) {
        for (n in 0 until out.totalSamples) {
            out[0, n] = (this[0, (n * speedf).toInt()] * lratio).toShort()
            out[1, n] = (this[1, (n * speedf).toInt()] * rratio).toShort()
        }
    } else {
        for (n in out.data.indices) {
            out.data[n] = (this.data[(n * speedf).toInt()] * lratio).toShort()
        }
    }

    return out
}

fun AudioSamplesInterleaved.ensureTwoChannels(): AudioSamplesInterleaved {
    return when (channels) {
        2 -> this
        else -> {
            AudioSamplesInterleaved(2, this.totalSamples).also { out ->
                val inp = this@ensureTwoChannels
                var m = 0
                for (n in 0 until out.totalSamples) {
                    val v = inp.data[n]
                    out.data[m++] = v
                    out.data[m++] = v
                }
            }
        }
    }
}

fun IAudioSamples.separated(out: AudioSamples = AudioSamples(channels, totalSamples)): AudioSamples {
    for (n in 0 until totalSamples) for (c in 0 until channels) out[c, n] = this[c, n]
    return out
}
