package com.soywiz.korau.sound.effects

import com.soywiz.kmem.*
import com.soywiz.korau.internal.*
import com.soywiz.korau.sound.*
import kotlin.math.*

data class AudioStreamEchoConfig(
    var volume: Double = 1.0
)

suspend fun AudioStreamable.withEcho(bufferLen: Int = 2048, config: AudioStreamEchoConfig = AudioStreamEchoConfig()): Sound {
    val buffer2 = AudioSamples(2, bufferLen)
    val buffer = AudioSamples(2, bufferLen)
    val temp = IntArray(bufferLen)
    return withProcessor { inp, outp ->
        val len = inp.read(buffer)
        for (ch in 0 until inp.channels) {
            val data = buffer.data[ch]
            val data2 = buffer2.data[ch]
            var maxAbs = 0
            val volume = config.volume.clamp01().toFloat()
            for (n in 0 until len) {
                temp[n] = ((data[n] + data2[n] * volume).toInt())
                maxAbs = kotlin.math.max(maxAbs, temp[n].absoluteValue)
            }
            val maxMul = Short.MAX_VALUE.toInt() ushr 4
            val maxDiv = maxAbs ushr 4
            when {
                maxDiv == 0 || maxAbs <= Short.MAX_VALUE -> {
                    for (n in 0 until len) {
                        data2[n] = data[n]
                        data[n] = temp[n].coerceToShort()
                    }
                }

                else -> {
                    for (n in 0 until len) {
                        data2[n] = data[n]
                        data[n] = ((temp[n] * maxMul) / maxDiv).coerceToShort()
                    }
                }
            }
        }
        outp.write(buffer, 0, len)
    }
}
