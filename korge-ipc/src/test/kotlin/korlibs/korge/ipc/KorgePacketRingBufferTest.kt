package korlibs.korge.ipc

import java.nio.*
import kotlin.test.*

class KorgePacketRingBufferTest {
    @Test
    fun test() {
        val ring = KorgePacketRingBuffer(
            meta = IntBuffer.allocate(2),
            buffer = ByteBuffer.allocate(1024)
        )
        assertEquals(0, ring.availableRead)
        assertEquals(1024, ring.availableWrite)
        ring.write(IPCPacket(1, byteArrayOf(1, 2, 3)))
        assertEquals(11, ring.availableRead)
        assertEquals(1013, ring.availableWrite)
        ring.write(IPCPacket(2, byteArrayOf(1, 2, 3)))
        assertEquals(22, ring.availableRead)
        assertEquals(1002, ring.availableWrite)
        assertEquals("Packet(type=1, data=bytes[3])", ring.read().toString())
        assertEquals(11, ring.availableRead)
        assertEquals(1013, ring.availableWrite)
        assertEquals("Packet(type=2, data=bytes[3])", ring.read().toString())
        assertEquals(0, ring.availableRead)
        assertEquals(1024, ring.availableWrite)
        assertEquals("null", ring.read().toString())
        assertEquals(0, ring.availableRead)
        assertEquals(1024, ring.availableWrite)
    }
}
