package com.soywiz.korau.sound

import com.soywiz.klock.TimeSpan
import com.soywiz.korau.internal.SampleConvert
import kotlin.math.PI
import kotlin.math.sin

object AudioTone {
    fun generate(length: TimeSpan, freq: Double, rate: Int = 44100): AudioData {
        val nsamples = (rate * length.seconds).toInt()
        val samples = AudioSamples(1, nsamples)
        val samples0 = samples[0]
        for (n in 0 until nsamples) {
            val ratio = (n.toDouble() * freq) / rate
            val sample = sin(ratio * PI * 2)
            samples0[n] = SampleConvert.floatToShort(sample.toFloat())
        }
        return AudioData(rate, samples)
    }
}
