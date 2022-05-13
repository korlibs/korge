package com.soywiz.kmem

import com.soywiz.kmem.internal.byteArrayOf
import kotlin.test.Test
import kotlin.test.assertEquals

class ByteArrayReadWriteTest {
    @Test
    fun test() {
        assertEquals(254, byteArrayOf(-1, -2, -3).readU8(1))
        assertEquals(-2, byteArrayOf(-1, -2, -3).readS8(1))

        assertEquals(0x9145, byteArrayOf(-1, 0x45, 0x91).readU16LE(1))
        assertEquals(0x9145, byteArrayOf(-1, 0x45, 0x91).readU16(1, little = true))

        assertEquals(0x9145, byteArrayOf(-1, 0x91, 0x45).readU16BE(1))
        assertEquals(0x9145, byteArrayOf(-1, 0x91, 0x45).readU16(1, little = false))

        assertEquals(0x914533, byteArrayOf(-1, 0x33, 0x45, 0x91).readU24LE(1))
        assertEquals(0x914533, byteArrayOf(-1, 0x33, 0x45, 0x91).readU24(1, little = true))

        assertEquals(0x914533, byteArrayOf(-1, 0x91, 0x45, 0x33).readU24BE(1))
        assertEquals(0x914533, byteArrayOf(-1, 0x91, 0x45, 0x33).readU24(1, little = false))
    }

    @Test
    fun test2() {
        assertEquals("000123456789abcdef", ByteArray(9).apply { write64BE(1, 0x0123456789ABCDEFL) }.hex)
        assertEquals("00efcdab8967452301", ByteArray(9).apply { write64LE(1, 0x0123456789ABCDEFL) }.hex)
    }
}
