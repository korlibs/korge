package korlibs.memory

import kotlin.test.Test
import kotlin.test.assertEquals

class ByteArrayBuilderTest {
    @Test
    fun test() {
        assertEquals(
            listOf(1, 2, 3, 4, 5, 6, 0, 0, 0),
            buildByteArray {
                append(1).append(2).append(byteArrayOf(3, 4, 5))
                s32LE(6)
            }.apply {
                assertEquals(9, size)
            }.map { it.toInt() }
        )
    }
}
