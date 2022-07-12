package com.soywiz.korau.sound

import com.soywiz.korau.format.AudioFormats
import com.soywiz.korau.format.WAV
import com.soywiz.korau.format.mp3.FastMP3Decoder
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import kotlin.test.Test
import kotlin.test.assertEquals

class SoundAudioStreamTest {
    @Test
    fun testPlaySeveralTimes() = suspendTest {
        val soundProvider = LogNativeSoundProvider(AudioFormats(WAV, FastMP3Decoder))

        val sound = soundProvider.createSound(resourcesVfs["click.mp3"], streaming = true)
        val data = sound.toData()
        sound.playAndWait(2.playbackTimes)
        assertEquals(1, soundProvider.streams.size)
        val stream = soundProvider.streams[0]
        val dataOut = stream.toData()
        val dataOut2 = dataOut.toSound().toData()

        //WAV.encodeToByteArray(dataOut).writeToFile("/tmp/demo.wav")
        //dataOut.toSound().toData().toSound().toData().toSound().toData().toSound().playAndWait()

        assertEquals("468/1", "${data.totalSamples}/${data.channels}")
        assertEquals("936/2", "${stream.data.availableRead}/${stream.data.channels}")
        assertEquals("936/2", "${dataOut.totalSamples}/${dataOut.channels}")
        assertEquals("936/2", "${dataOut2.totalSamples}/${dataOut2.channels}")

    }
}
