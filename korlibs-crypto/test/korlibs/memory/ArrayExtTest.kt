package korlibs.memory

import kotlin.test.Test
import kotlin.test.assertEquals

class ArrayExtTest {
    @Test
    fun test() {
        assertEquals(1, byteArrayOf(1, 2, 3, 4, 5).indexOf(byteArrayOf(2, 3)))
        assertEquals(1, byteArrayOf(1, 2, 3, 4, 5).indexOf(byteArrayOf(2, 3), 1))
        assertEquals(-1, byteArrayOf(1, 2, 3, 4, 5).indexOf(byteArrayOf(2, 3), 2))
    }
}
