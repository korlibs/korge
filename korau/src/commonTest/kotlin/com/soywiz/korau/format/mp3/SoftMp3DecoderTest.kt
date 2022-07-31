package com.soywiz.korau.sound

import com.soywiz.klock.measureTime
import com.soywiz.klock.measureTimeWithResult
import com.soywiz.kmem.divCeil
import com.soywiz.kmem.divRound
import com.soywiz.korau.format.AudioDecodingProps
import com.soywiz.korau.format.AudioFormats
import com.soywiz.korau.format.mp3.FastMP3Decoder
import com.soywiz.korau.format.mp3.MP3Decoder
import com.soywiz.korau.format.mp3.javamp3.JavaMp3AudioFormat
import com.soywiz.korau.format.mp3.minimp3.Minimp3AudioFormat
import com.soywiz.korau.internal.toByteArrayLE
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.krypto.sha1
import doIOTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class SoftMp3DecoderTest {
    val formats = AudioFormats(FastMP3Decoder)

    @Test
    @Ignore
    fun testMiniMp3SpeedFast() = suspendTest({ doIOTest }) {
        val bytes = resourcesVfs["mp31.mp3"].readBytes()
        //for (n in 0 until 100) {
        for (n in 0 until 100) {
            val output = FastMP3Decoder.decode(bytes)
        }
    }

    @Test
    @Ignore
    fun testMiniMp3SpeedJava() = suspendTest({ doIOTest }) {
        val bytes = resourcesVfs["mp31.mp3"].readBytes()
        //for (n in 0 until 100) {
        for (n in 0 until 100) {
            val output = JavaMp3AudioFormat.decode(bytes)
        }
    }

    @Test
    fun testMiniMp31() = suspendTest({ doIOTest }) {
        //resourcesVfs["mp31.mp3"].readAudioData(FastMP3Decoder).toSound()
        assertEquals(
            "1,44100,22050,c82a407c8353c9d47c6f499a5195f85809bbbf8a",
            resourcesVfs["mp31.mp3"].readAudioData(FastMP3Decoder).toFingerprintString()
        )
    }

    @Test fun mp3_1() = suspendTest({ doIOTest }) {
        assertEquals(
            "1,44100,28800,ee797bf9ec5a2b5ed0e3064cc5d091157921be6f",
            resourcesVfs["circle_ok.mp3"].readAudioData(formats).toFingerprintString()
        )
    }
    @Test fun mp3_2() = suspendTest({ doIOTest }) {
        assertEquals(
            "1,44100,16128,e4848a4bd5b3117665dcafc14109fdc677c9ee2f",
            resourcesVfs["line_missed.mp3"].readAudioData(formats).toFingerprintString()
        )
    }
    @Test fun mp3_3() = suspendTest({ doIOTest }) {
        assertEquals(
            "1,44100,14976,f38dc856841ba47afe815d6a64654f29b63b822e",
            resourcesVfs["line_ok.mp3"].readAudioData(formats).toFingerprintString(),
        )
    }
    @Test fun monkeyDramaMiniMp3() = suspendTest({ doIOTest }) {
        val (mp3Bytes, readTime) = measureTimeWithResult { resourcesVfs["monkey_drama.mp3"].readBytes() }
        println("Read in $readTime")
        val (decode, decodeTime) = measureTimeWithResult { formats.decode(mp3Bytes, AudioDecodingProps(maxSamples = 569088)) }
        println("Decoded in $decodeTime")
        val (fingerprint, fingerprintTime) = measureTimeWithResult { decode?.toFingerprintString() }
        println("Fingerprint in in $fingerprintTime")
        assertEquals(
            "2,44100,569088,f43f395b2029b060f9f6ef06a1a96b2e1e6f3860",
            fingerprint,
        )
    }
    @Ignore
    @Test
    fun monkeyDramaJavaMp3() = suspendTest({ doIOTest }) {
        resourcesVfs["monkey_drama.mp3"].readAudioData(JavaMp3AudioFormat, AudioDecodingProps(maxSamples = 569088)).toFingerprintString()
    }
    @Test fun snowland() = suspendTest({ doIOTest }) {
        assertEquals(
            "2,48000,565920,36945a5c28a37e4f860b951fe397f03ba1bd187d",
            resourcesVfs["Snowland.mp3"].readAudioData(formats).toFingerprintString(),
        )
    }

    fun AudioData.toFingerprintString(): String {
        val sdata = samplesInterleaved.data
        val data = ByteArray(sdata.size) { sdata[it].toInt().divRound(256 * 8).toByte() }
        return "$channels,$rate,$totalSamples,${data.sha1().hex}"
    }

}
