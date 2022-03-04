package com.soywiz.korio.stream

import com.soywiz.korio.experimental.*
import kotlin.test.*

@OptIn(KorioExperimentalApi::class)
class ByteArrayBitReaderTest {
    @Test
    fun test() {
        val bytes = byteArrayOf(0b1000_1111.toByte(), 0b1100_1101.toByte())
        val reader = ByteArrayBitReader(bytes)
        assertEquals(true, reader.hasMoreBits)
        assertEquals(0b1, reader.readIntBits(1))
        assertEquals(0b000, reader.readIntBits(3))
        assertEquals(0b1111, reader.readIntBits(4))
        assertEquals(true, reader.hasMoreBits)
        assertEquals(0b110, reader.readIntBits(3))
        assertEquals(true, reader.hasMoreBits)
        assertEquals(0b01101, reader.readIntBits(5))
        assertEquals(false, reader.hasMoreBits)
    }
}
