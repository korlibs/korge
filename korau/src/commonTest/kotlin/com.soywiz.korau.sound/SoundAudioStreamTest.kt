package com.soywiz.korau.sound

import com.soywiz.klock.milliseconds
import com.soywiz.korau.format.AudioFormats
import com.soywiz.korau.format.WAV
import com.soywiz.korau.format.mp3.FastMP3Decoder
import com.soywiz.korio.async.delay
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
            val wait = CompletableDeferred<Unit>()
            soundProvider.onAfterAdd.once {
                println("currentThreadId:$currentThreadId")
                wait.complete(Unit)
            }
            println("currentThreadId:$currentThreadId")
            val channel = sound2.play()
            assertEquals("0ms/58.5ms", "${channel.current}/${channel.total}")
            wait.await()
            delay(20.milliseconds) // @TODO: This is a patch to try to avoid or reduce a flaky test. This shouldn't be needed and we should figure out the real reason for this
            assertEquals("58.5ms/58.5ms", "${channel.current}/${channel.total}")
        }
    }
}
