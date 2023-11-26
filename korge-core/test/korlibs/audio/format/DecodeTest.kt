@file:Suppress("UnusedImport")

package korlibs.audio.format

import korlibs.io.async.suspendTest
import korlibs.io.file.std.resourcesVfs
import korlibs.io.stream.openAsync
import korlibs.io.util.checksum.CRC32
import korlibs.io.util.checksum.checksum
import korlibs.encoding.hex
import doIOTest
import kotlin.test.assertEquals

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
