package korlibs.memory

import kotlin.test.Test
import kotlin.test.assertEquals

class ArrayCopyTest {
    @Test
    fun testCopy() {
        val array = arrayOf("a", "b", "c", null, null)
        arraycopy(array, 0, array, 1, 4)
        assertEquals(listOf("a", "a", "b", "c", null), array.toList())
        arraycopy(array, 2, array, 1, 3)
        assertEquals(listOf("a", "b", "c", null, null), array.toList())
    }
}
