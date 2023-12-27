package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals

class ByteArrayDequeTest {
    @Test
    fun test() {
        val data = ByteArrayDeque(1)
        assertEquals(0, data.availableRead)
        assertEquals(2, data.availableWriteWithoutAllocating)
        data.writeByte(0xFF)
        assertEquals(1, data.availableRead)
        assertEquals(1, data.availableWriteWithoutAllocating)
        data.writeByte(0x77)
        assertEquals(2, data.availableRead)
        assertEquals(0, data.availableWriteWithoutAllocating)
        data.writeByte(0x3F)
        assertEquals(3, data.availableRead)
        assertEquals(5, data.availableWriteWithoutAllocating)
        assertEquals(listOf(0xFF, 0x77), data.readBytesUpTo(2).map { it.toInt() and 0xFF })
        data.writeByte(0x7F)
        assertEquals(0x3F, data.readByte())
        assertEquals(1, data.availableRead)
        assertEquals(7, data.availableWriteWithoutAllocating)
        data.write(byteArrayOf(0x99.toByte(), 0x88.toByte(), 0x33, 0x44, 0x55, 0x66, 0xAA.toByte()))
        assertEquals(8, data.availableRead)
        assertEquals(0, data.availableWriteWithoutAllocating)
        assertEquals(listOf(0x7F, 0x99, 0x88, 0x33, 0x44, 0x55, 0x66, 0xAA), data.readBytesUpTo(8).map { it.toInt() and 0xFF })
        assertEquals(-1, data.readByte())
        assertEquals(0, data.readBytesUpTo(10).size)
    }

    @Test
    fun demo() {
        val data = ByteArrayDeque(4)
        data.write(byteArrayOf(4, 5, 6))
        data.writeHead(byteArrayOf(1, 2, 3))
        assertEquals(byteArrayOf(1, 2, 3, 4, 5, 6).toList(), data.readBytesUpTo(6).toList())
    }

    private fun ByteArrayDeque.readBytesUpTo(count: Int): ByteArray = ByteArray(count).let {
        val size = read(it, 0, it.size)
        it.copyOf(size)
    }
}
