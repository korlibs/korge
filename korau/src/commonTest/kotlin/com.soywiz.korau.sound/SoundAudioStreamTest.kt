package com.soywiz.korau.sound

import com.soywiz.korau.format.AudioFormats
import com.soywiz.korau.format.WAV
import com.soywiz.korau.format.mp3.FastMP3Decoder
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.lang.currentThreadId
import doIOTest
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.Test
import kotlin.test.assertEquals

class SoundAudioStreamTest {
    @Test
    fun testPlaySeveralTimes() = suspendTest({ doIOTest }) {
        val soundProvider = LogNativeSoundProvider()

        val sound = soundProvider.createSound(resourcesVfs["click.mp3"], streaming = true)
        val data = sound.toAudioData()
        sound.playAndWait(2.playbackTimes)
        assertEquals(1, soundProvider.streams.size)
        val stream = soundProvider.streams[0]
        val dataOut = stream.toData()
        val dataOut2 = dataOut.toSound().toAudioData()

        //WAV.encodeToByteArray(dataOut).writeToFile("/tmp/demo.wav")
        //dataOut.toSound().toData().toSound().toData().toSound().toData().toSound().playAndWait()

        assertEquals("468/1", "${data.totalSamples}/${data.channels}")
        assertEquals("936/2", "${stream.data.availableRead}/${stream.data.channels}")
        assertEquals("936/2", "${dataOut.totalSamples}/${dataOut.channels}")
        assertEquals("936/2", "${dataOut2.totalSamples}/${dataOut2.channels}")
    }

    @Test
    fun testChannelCurrentLength() = suspendTest({ doIOTest }) {
        val soundProvider = LogNativeSoundProvider()
        for (fileName in listOf("click.wav", "click.mp3")) {
            val sound2 = soundProvider.createSound(resourcesVfs[fileName], streaming = true)
            val channel = sound2.play()
            val wait = CompletableDeferred<Unit>()
            println("currentThreadId:$currentThreadId")
            soundProvider.onAfterAdd.once {
                println("currentThreadId:$currentThreadId")
                wait.complete(Unit)
            }
            assertEquals("0ms/58.5ms", "${channel.current}/${channel.total}")
            wait.await()
            assertEquals("58.5ms/58.5ms", "${channel.current}/${channel.total}")
        }
    }
}
