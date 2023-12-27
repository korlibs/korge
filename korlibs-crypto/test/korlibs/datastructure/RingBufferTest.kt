package korlibs.datastructure

import korlibs.datastructure.internal.KdsInternalApi
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(KdsInternalApi::class)
class RingBufferTest {
    val RingBuffer.info: String get() = "totalSize=$totalSize, WRITE[pos=$internalWritePos, available=$availableWrite, availableBeforeWrap=$availableWriteBeforeWrap], READ[pos=$internalReadPos, available=$availableRead, availableBeforeWrap=$availableReadBeforeWrap]"

    @Test
    fun test() {
        val log = arrayListOf<String>()
        val ring = RingBuffer(4)
        log += ring.info
        ring.write(byteArrayOf(1, 2, 3))
        ring.write(byteArrayOf(4, 5, 6))
        log += ring.info
        log += "${ring.readByte()}"
        log += "${ring.readByte()}"
        log += ring.info
        ring.write(byteArrayOf(7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18))
        log += ring.info
        log += "${ring.readBytes(100).toList()}"
        log += ring.info

        assertEquals(
            """
                totalSize=16, WRITE[pos=0, available=16, availableBeforeWrap=16], READ[pos=0, available=0, availableBeforeWrap=0]
                totalSize=16, WRITE[pos=6, available=10, availableBeforeWrap=10], READ[pos=0, available=6, availableBeforeWrap=6]
                1
                2
                totalSize=16, WRITE[pos=6, available=12, availableBeforeWrap=10], READ[pos=2, available=4, availableBeforeWrap=4]
                totalSize=16, WRITE[pos=2, available=0, availableBeforeWrap=0], READ[pos=2, available=16, availableBeforeWrap=14]
                [3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18]
                totalSize=16, WRITE[pos=2, available=16, availableBeforeWrap=14], READ[pos=2, available=0, availableBeforeWrap=0]
            """.trimIndent(),
            log.joinToString("\n")
        )
    }
}
