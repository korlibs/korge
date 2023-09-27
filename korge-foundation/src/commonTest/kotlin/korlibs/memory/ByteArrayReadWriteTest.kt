package korlibs.memory

import korlibs.encoding.*
import kotlin.test.*

class ByteArrayReadWriteTest {
    @Test
    fun test() {
        assertEquals(254, byteArrayOf(-1, -2, -3).getU8(1))
        assertEquals(-2, byteArrayOf(-1, -2, -3).getS8(1))

        assertEquals(0x9145, byteArrayOf(-1, 0x45, 0x91).getU16LE(1))
        assertEquals(0x9145, byteArrayOf(-1, 0x45, 0x91).getU16(1, littleEndian = true))

        assertEquals(0x9145, byteArrayOf(-1, 0x91, 0x45).getU16BE(1))
        assertEquals(0x9145, byteArrayOf(-1, 0x91, 0x45).getU16(1, littleEndian = false))

        assertEquals(0x914533, byteArrayOf(-1, 0x33, 0x45, 0x91).getU24LE(1))
        assertEquals(0x914533, byteArrayOf(-1, 0x33, 0x45, 0x91).getU24(1, littleEndian = true))

        assertEquals(0x914533, byteArrayOf(-1, 0x91, 0x45, 0x33).getU24BE(1))
        assertEquals(0x914533, byteArrayOf(-1, 0x91, 0x45, 0x33).getU24(1, littleEndian = false))
    }

    @Test
    fun test2() {
        assertEquals("000123456789abcdef", ByteArray(9).apply { set64BE(1, 0x0123456789ABCDEFL) }.hex)
        assertEquals("00efcdab8967452301", ByteArray(9).apply { set64LE(1, 0x0123456789ABCDEFL) }.hex)
    }
}
