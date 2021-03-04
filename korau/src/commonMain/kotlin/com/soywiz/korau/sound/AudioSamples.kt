package com.soywiz.korau.sound

import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korau.internal.*
import com.soywiz.korio.lang.*
import kotlin.math.*

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
        val inpc = this[c]
        fastShortTransfer.use(out[c]) { outc ->
            for (n in 0 until totalSamples) {
                // @TODO: Increase quality
                outc[n] = inpc[(n * iscale).toInt()]
            }
        }
    }
    return out
}

fun AudioSamples.resample(srcFreq: Int, dstFreq: Int): AudioSamples =
    resample(dstFreq.toDouble() / srcFreq.toDouble())

fun AudioSamples.resampleIfRequired(srcFreq: Int, dstFreq: Int): AudioSamples =
    if (srcFreq == dstFreq) this else resample(srcFreq, dstFreq)

class AudioSamplesProcessor(val channels: Int, val totalSamples: Int, val data: Array<FloatArray> = Array(channels) { FloatArray(totalSamples) })  {
    fun reset(): AudioSamplesProcessor {
        for (ch in 0 until channels) data[ch].fill(0f)
        return this
    }
    fun add(samples: AudioSamples, scale: Float = 1f): AudioSamplesProcessor {
        for (ch in 0 until min2(channels, samples.channels)) {
            val odata = this.data[ch]
            val idata = samples.data[ch]
            for (n in 0 until samples.totalSamples) {
                odata[n] += SampleConvert.shortToFloat(idata[n]) * scale
            }
        }
        return this
    }
    fun normalize(): AudioSamplesProcessor {
        for (ch in 0 until channels) {
            val odata = this.data[ch]
            var maxAbs = 0f
            for (n in 0 until totalSamples) {
                maxAbs = max2(maxAbs, odata[n].absoluteValue)
            }
            if (maxAbs > 1f) {
                val invMaxAbs = 1f / maxAbs
                for (n in 0 until totalSamples) {
                    odata[n] *= invMaxAbs
                }
            }
        }
        return this
    }
    fun copyTo(samples: AudioSamples) {
        for (ch in 0 until min2(channels, samples.channels)) {
            val idata = this.data[ch]
            val odata = samples.data[ch]
            for (n in 0 until samples.totalSamples) {
                odata[n] = SampleConvert.floatToShort(idata[n])
            }
        }
    }
}

class AudioSamples(override val channels: Int, override val totalSamples: Int, val data: Array<ShortArray> = Array(channels) { ShortArray(totalSamples) }) : IAudioSamples {
    //val interleaved by lazy { interleaved() }
    internal val fastShortTransfer = FastShortTransfer()

    operator fun get(channel: Int): ShortArray = data[channel]

    override operator fun get(channel: Int, sample: Int): Short = data[channel][sample]
    override operator fun set(channel: Int, sample: Int, value: Short) = run { data[channel][sample] = value }

    fun scaleVolume(scale: Double): AudioSamples = scaleVolume(scale.toFloat())

    fun scaleVolume(scale: Float): AudioSamples {
        data.fastForEach { channel ->
            for (n in channel.indices) {
                channel[n] = (channel[n] * scale).toInt().coerceToShort()
            }
        }
        return this
    }

    fun setTo(that: AudioSamples): AudioSamples {
        that.copyTo(this)
        return this
    }

    fun copyTo(that: AudioSamples) {
        for (ch in 0 until min2(channels, that.channels)) {
            arraycopy(this.data[ch], 0, that.data[ch], 0, min2(totalSamples, that.totalSamples))
        }
    }

    override fun hashCode(): Int = channels + totalSamples * 32 + data.contentDeepHashCode() * 64
    override fun equals(other: Any?): Boolean = (other is AudioSamples) && this.channels == other.channels && this.totalSamples == other.totalSamples && this.data.contentDeepEquals(other.data)

    override fun toString(): String = "AudioSamples(channels=$channels, totalSamples=$totalSamples)"
}

class AudioSamplesInterleaved(override val channels: Int, override val totalSamples: Int, val data: ShortArray = ShortArray(totalSamples * channels)) : IAudioSamples {
    //val separared by lazy { separated() }
    internal val fastShortTransfer = FastShortTransfer()

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

fun AudioSamples.interleaved(out: AudioSamplesInterleaved = AudioSamplesInterleaved(channels, totalSamples)): AudioSamplesInterleaved {
    assert(out.data.size >= totalSamples * channels)
    when (channels) {
        1 -> arraycopy(this.data[0], 0, out.data, 0, totalSamples)
        2 -> arrayinterleave(
            out.data, 0,
            this.data[0], 0,
            this.data[1], 0,
            totalSamples,
            out.fastShortTransfer
        )
        else -> {
            out.fastShortTransfer.use(out.data) { outData ->
                val channels = channels
                for (c in 0 until channels) {
                    var m = c
                    for (n in 0 until totalSamples) {
                        outData[m] = this[c, n]
                        m += channels
                    }
                }
            }
        }
    }
    return out
}

fun IAudioSamples.interleaved(out: AudioSamplesInterleaved = AudioSamplesInterleaved(channels, totalSamples)): AudioSamplesInterleaved {
    assert(out.data.size >= totalSamples * channels)
    when (this) {
        is AudioSamples -> this.interleaved(out)
        is AudioSamplesInterleaved -> arraycopy(this.data, 0, out.data, 0, totalSamples * channels)
        else -> {
            out.fastShortTransfer.use(out.data) { outData ->
                val channels = channels
                for (c in 0 until channels) {
                    var m = c
                    for (n in 0 until totalSamples) {
                        outData[m] = this[c, n]
                        m += channels
                    }
                }
            }
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

    out.fastShortTransfer.use(out.data) { outData ->
        var m = 0
        if (channels == 2) {
            for (n in 0 until out.totalSamples) {
                outData[m++] = (outData[(n * speedf).toInt() * 2 + 0] * lratio).toInt().toShort()
                outData[m++] = (outData[(n * speedf).toInt() * 2 + 1] * rratio).toInt().toShort()
            }
        } else {
            for (n in out.data.indices) {
                outData[m++] = (outData[(n * speedf).toInt()] * lratio).toInt().toShort()
            }
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
                val ichannels = inp.channels
                val odata = out.data
                val idata = inp.data
                for (n in 0 until out.totalSamples) {
                    val v = idata[n * ichannels]
                    odata[m++] = v
                    odata[m++] = v
                }
            }
        }
    }
}

fun IAudioSamples.separated(out: AudioSamples = AudioSamples(channels, totalSamples)): AudioSamples {
    for (n in 0 until totalSamples) for (c in 0 until channels) out[c, n] = this[c, n]
    return out
}
