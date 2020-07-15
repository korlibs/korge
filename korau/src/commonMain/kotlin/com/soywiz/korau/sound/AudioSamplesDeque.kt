package com.soywiz.korau.sound

import com.soywiz.kds.*
import com.soywiz.korau.internal.*
import kotlin.math.*

class AudioSamplesDeque(val channels: Int) {
    val buffer = Array(channels) { ShortArrayDeque() }
    val availableRead get() = buffer.getOrNull(0)?.availableRead ?: 0
    val availableReadMax: Int get() = buffer.map { it.availableRead }.max() ?: 0

    private val temp = ShortArray(1)

    // Individual samples
    fun read(channel: Int): Short = buffer[channel].read(temp, 0, 1).let { temp[0] }
    fun write(channel: Int, sample: Short) = run { buffer[channel].write(temp.also { temp[0] = sample }, 0, 1) }

    fun readFloat(channel: Int): Float = read(channel).toFloat() / Short.MAX_VALUE.toFloat()
    fun writeFloat(channel: Int, sample: Float) = write(channel, (sample * Short.MAX_VALUE.toFloat()).toShort())

    // Write samples
    fun write(samples: AudioSamples, offset: Int = 0, len: Int = samples.size - offset) {
        for (channel in 0 until samples.channels) write(channel, samples[channel], offset, len)
    }

    fun write(samples: AudioSamplesInterleaved, offset: Int = 0, len: Int = samples.size - offset) {
        writeInterleaved(samples.data, offset, len, samples.channels)
    }

    fun write(samples: IAudioSamples, offset: Int = 0, len: Int = samples.size - offset) {
        when (samples) {
            is AudioSamples -> write(samples, offset, len)
            is AudioSamplesInterleaved -> write(samples, offset, len)
            else -> for (c in 0 until samples.channels) for (n in 0 until len) write(c, samples[c, offset + n])
        }
    }

    // Write raw
    fun write(channel: Int, data: ShortArray, offset: Int = 0, len: Int = data.size - offset) {
        buffer[channel].write(data, offset, len)
    }

    fun write(channel: Int, data: FloatArray, offset: Int = 0, len: Int = data.size - offset) {
        for (n in 0 until len) write(channel, SampleConvert.floatToShort(data[offset + n]))
    }

    fun writeInterleaved(data: ShortArray, offset: Int, len: Int = data.size - offset, channels: Int = this.channels) {
        when (channels) {
            1 -> {
                buffer[0].write(data, offset, len)
            }
            2 -> {
                for (n in 0 until len / 2) write(0, data[n * 2 + 0])
                for (n in 0 until len / 2) write(1, data[n * 2 + 1])
            }
            else -> {
                for (c in 0 until channels) for (n in 0 until len / channels) write(c, data[n * channels + c])
            }
        }
    }

    fun read(out: AudioSamples, offset: Int = 0, len: Int = out.totalSamples - offset): Int {
        val result = min(len, availableRead)
        for (channel in 0 until out.channels) this.buffer[channel].read(out[channel], offset, len)
        return result
    }

    fun read(out: AudioSamplesInterleaved, offset: Int = 0, len: Int = out.totalSamples - offset): Int {
        val result = min(len, availableRead)
        for (channel in 0 until out.channels) for (n in 0 until len) out[channel, offset + n] = this.read(channel)
        return result
    }

    fun read(out: IAudioSamples, offset: Int = 0, len: Int = out.totalSamples - offset): Int {
        val result = min(len, availableRead)
        when (out) {
            is AudioSamples -> read(out, offset, len)
            is AudioSamplesInterleaved -> read(out, offset, len)
            else -> for (c in 0 until out.channels) for (n in 0 until len) out[c, offset + n] = this.read(c)
        }
        return result
    }

    fun clear() {
        for (c in buffer.indices) buffer[c].clear()
    }

    override fun toString(): String = "AudioSamplesDeque(channels=$channels, availableRead=$availableRead)"
}
