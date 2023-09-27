package korlibs.memory

import kotlin.test.Test
import kotlin.test.assertEquals

class ArrayFillTest {
    @Test
    fun testFill() {
        val array = intArrayOf(1, 1, 1, 1, 1)
        array.fill(2)
        assertEquals(intArrayOf(2, 2, 2, 2, 2).toList(), array.toList())
        array.fill(3, 1, 4)
        assertEquals(intArrayOf(2, 3, 3, 3, 2).toList(), array.toList())
    }
}
