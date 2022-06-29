package com.soywiz.korau.sound

import com.soywiz.korau.format.AudioFormats
import com.soywiz.korau.format.mp3.MP3Decoder
import com.soywiz.korau.format.mp3.minimp3.Minimp3AudioFormat
import com.soywiz.korau.internal.toByteArrayLE
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.krypto.sha1
import doIOTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SoftMp3DecoderTest {
    val formats = AudioFormats(MP3Decoder)

    @Test
    fun testMiniMp3Speed() = suspendTest({ doIOTest }) {
        val bytes = resourcesVfs["mp31.mp3"].readBytes()
        //for (n in 0 until 100) {
        for (n in 0 until 10) {
            val output = Minimp3AudioFormat.decode(bytes)
            //val output = JavaMp3AudioFormat.decode(bytes)
        }
    }

    @Test
    fun testMiniMp31() = suspendTest({ doIOTest }) {
        assertEquals(
            "1,44100,25344,52910cbfb3d8b1b45e462c296a7d8a9538a7b9c4",
            resourcesVfs["mp31.mp3"].readAudioData(Minimp3AudioFormat).toFingerprintString()
        )
    }

    @Test fun mp3_1() = suspendTest({ doIOTest }) {
        assertEquals(
            "1,44100,28800,e5571a38ad5ee655136c753e51e8b462b1e807a0",
            resourcesVfs["circle_ok.mp3"].readAudioData(formats).toFingerprintString()
        )
    }
    @Test fun mp3_2() = suspendTest({ doIOTest }) {
        assertEquals(
            "1,44100,16128,b55cb9a5530bbbf029aede6ec85bb9337d4b3392",
            resourcesVfs["line_missed.mp3"].readAudioData(formats).toFingerprintString()
        )
    }
    @Test fun mp3_3() = suspendTest({ doIOTest }) {
        assertEquals(
            "1,44100,14976,f01d9a73f623923fa62372ffbef9c0a1a376a0bf",
            resourcesVfs["line_ok.mp3"].readAudioData(formats).toFingerprintString(),
        )
    }
    @Test fun monkeyDrama() = suspendTest({ doIOTest }) {
        assertEquals(
            "2,44100,3528576,ab95743bf262309fc5b0ed79506e17c558c1ddac",
            resourcesVfs["monkey_drama.mp3"].readAudioData(formats).toFingerprintString(),
        )
    }
    @Test fun snowland() = suspendTest({ doIOTest }) {
        assertEquals(
            "2,48000,569088,7a2f06a8e55135bfa24bcc17eeaab883f5db81c6",
            resourcesVfs["Snowland.mp3"].readAudioData(formats).toFingerprintString(),
        )
    }

    fun AudioData.toFingerprintString(): String = "$channels,$rate,$totalSamples,${samplesInterleaved.data.toByteArrayLE().sha1().hex}"

}
