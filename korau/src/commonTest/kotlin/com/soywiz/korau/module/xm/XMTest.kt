package com.soywiz.korau.module.xm

import com.soywiz.korau.format.WAV
import com.soywiz.korau.internal.coerceToShort
import com.soywiz.korau.internal.toSampleShort
import com.soywiz.korau.sound.AudioData
import com.soywiz.korau.sound.AudioSamples
import com.soywiz.korau.sound.AudioSamplesInterleaved
import com.soywiz.korau.sound.toData
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.file.writeToFile
import doIOTest
import kotlin.test.Test

class XMTest {
    @Test
    fun test() = suspendTest({ doIOTest }) {
        val bytes = resourcesVfs["transatlantic.xm"].readBytes()
        //val bytes = resourcesVfs["poliamber.xm"].readBytes()
        val xm = XM()
        xm.load(bytes)

        val NSAMPLES = 16
        //val NSAMPLES = 128
        //val NSAMPLES = 16000
        //val NSAMPLES = 44100 * 24

        val e = XM.AudioEvent(44100, 0.0, XM.AudioBuffer(NSAMPLES))
        xm.audio_cb(e)

        //WAV.encodeToByteArray(xm.createAudioStream().toData(NSAMPLES)).writeToFile("/tmp/lol2.wav")
        //println(ev.outputBuffer.channels[0].toList())
        //println(ev)
    }
}
