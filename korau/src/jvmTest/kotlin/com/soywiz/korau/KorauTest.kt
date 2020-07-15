package com.soywiz.korau

import com.soywiz.korau.format.*
import com.soywiz.korau.sound.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class KorauTest {
    val formats = standardAudioFormats()

    @Test
    fun name(): Unit = suspendTest {
        val sound = resourcesVfs["wav1.wav"].readAudioData(formats)
        //sleep(0)
        //sound.play()
    }

    //@Test
    //fun decodeMp3() = suspendTest {
    //    println("[a]")
    //    //val data = Mp3DecodeAudioFormat.decode(resourcesVfs["mp31.mp3"].open())
    //    val data = MP3Decoder.decode(resourcesVfs["mp31.mp3"].open())
    //    println("[b]")
    //    localCurrentDirVfs["mp31.mp3.raw"].writeBytes(data!!.samples.toByteArrayLE())
    //    println("[c]")
    //}
}

