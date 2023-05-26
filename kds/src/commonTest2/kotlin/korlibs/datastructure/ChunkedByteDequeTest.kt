package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals

class ChunkedByteDequeTest {
    @Test
    fun test() {
        val log = arrayListOf<String>()
        val deque = ChunkedByteDeque()
        log += "${deque.availableRead}"
        deque.write(byteArrayOf(1, 2, 3, 4))
        log += "${deque.availableRead}"
        deque.write(byteArrayOf(5, 6, 7))
        log += "${deque.availableRead}"
        log += ":" + deque.read(100).joinToString(",")
        log += "${deque.availableRead}"
        log += ":" + deque.read(100).joinToString(",")
        log += "${deque.availableRead}"
        assertEquals(
            """
                0
                4
                7
                :1,2,3,4,5,6,7
                0
                :
                0
            """.trimIndent(),
            log.joinToString("\n")
        )
    }
}
