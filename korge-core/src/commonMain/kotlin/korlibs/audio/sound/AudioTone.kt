package korlibs.audio.sound

import korlibs.audio.internal.*
import korlibs.time.*
import kotlin.math.*

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
