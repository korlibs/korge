package com.soywiz.korau.module.xm

import com.soywiz.korau.format.WAV
import com.soywiz.korau.internal.coerceToShort
import com.soywiz.korau.internal.toSampleShort
import com.soywiz.korau.sound.AudioData
import com.soywiz.korau.sound.AudioSamples
import com.soywiz.korau.sound.AudioSamplesInterleaved
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.file.writeToFile
import doIOTest
import kotlin.test.Test

class XMTest {
    @Test
    fun test() = suspendTest({ doIOTest }) {
        //val bytes = resourcesVfs["transatlantic.xm"].readBytes()
        val bytes = resourcesVfs["poliamber.xm"].readBytes()
        val xm = XM()
        xm.load(bytes)
        val NSAMPLES = 44100 * 10
        val ev = XM.AudioEvent(
            44100, 0.0, XM.AudioBuffer(arrayOf(FloatArray(NSAMPLES), FloatArray(NSAMPLES)))
        )
        xm.audio_cb(ev)
        val samples = AudioSamples(2, NSAMPLES)
        for (n in 0 until NSAMPLES) {
            val l = ev.outputBuffer.channels[0][n].toSampleShort()
            val r = ev.outputBuffer.channels[1][n].toSampleShort()
            samples.setStereo(n, l, r)
        }

        WAV.encodeToByteArray(AudioData(44100, samples)).writeToFile("/tmp/lol2.wav")
        println(ev.outputBuffer.channels[0].toList())
        println(ev)
    }
}
