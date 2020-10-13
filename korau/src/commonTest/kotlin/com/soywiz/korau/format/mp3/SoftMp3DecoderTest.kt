package com.soywiz.korau.sound

import com.soywiz.korau.format.*
import com.soywiz.korau.format.mp3.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import doIOTest
import kotlin.test.*

class SoftMp3DecoderTest {
    val formats = AudioFormats(MP3Decoder)

    @Test
    fun testMiniMp3() = suspendTest({ doIOTest }) {
        //for (n in 0 until 100) {
        for (n in 0 until 10) {
            val output = resourcesVfs["mp31.mp3"].readAudioData(formats)
        }
    }

    @Test fun mp3_1() = suspendTest({ doIOTest }) { resourcesVfs["circle_ok.mp3"].readAudioData(formats) }
    @Test fun mp3_2() = suspendTest({ doIOTest }) { resourcesVfs["line_missed.mp3"].readAudioData(formats) }
    @Test fun mp3_3() = suspendTest({ doIOTest }) { resourcesVfs["line_ok.mp3"].readAudioData(formats) }
}
