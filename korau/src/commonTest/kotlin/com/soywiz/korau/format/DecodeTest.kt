package com.soywiz.korau.format

import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.checksum.*
import com.soywiz.korio.util.encoding.*
import doIOTest
import kotlin.test.*

class DecodeTest {
    val formats = AudioFormats(WAV)

    @kotlin.test.Test
    fun wav() = suspendTest({ doIOTest }) {
        val wavContents = resourcesVfs["wav1.wav"].read()
        assertEquals(44144, wavContents.size, "wavContents.size")
        assertEquals(0x901751CE.toInt(), wavContents.checksum(CRC32), "wavContents.crc32")

        val wavData = formats.decode(wavContents.openAsync())!!

        assertEquals("AudioData(rate=44100, channels=1, samples=22050)", "$wavData")
        val wavContentsGen = formats.encodeToByteArray(wavData, "out.wav")

        assertEquals(wavContents.size, wavContentsGen.size)
        assertEquals(wavContents.hex, wavContentsGen.hex)
    }

    @kotlin.test.Test
    fun wav24() = suspendTest({ doIOTest }) {
        val wavContents = resourcesVfs["wav24.wav"].read()
        val wavData = formats.decode(wavContents.openAsync())!!

        assertEquals("AudioData(rate=48000, channels=1, samples=4120)", "$wavData")
        val wavContentsGen = formats.encodeToByteArray(wavData, "out.wav")

        //LocalVfs("c:/temp/lol.wav").write(wavContentsGen)
        //Assert.assertArrayEquals(wavContents, wavContentsGen)
    }

    @kotlin.test.Test
    fun wav8bit() = suspendTest({ doIOTest }) {
        val wavContents = resourcesVfs["wav8bit.wav"].read()
        val wavData = formats.decode(wavContents.openAsync())!!

        assertEquals("AudioData(rate=44100, channels=2, samples=22050)", "$wavData")
        val wavContentsGen = formats.encodeToByteArray(wavData, "out.wav")

        //LocalVfs("c:/temp/lol.wav").write(wavContentsGen)
        //Assert.assertArrayEquals(wavContents, wavContentsGen)
    }
}
