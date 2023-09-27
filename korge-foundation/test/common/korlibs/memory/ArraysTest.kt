package korlibs.memory

import kotlin.test.Test
import kotlin.test.assertEquals

class ArraysTest {
    @Test
    fun floatArrayFromIntArray() {
        val fa = IntArray(16).asFloatArray()
        val ia = fa.asIntArray()
        fa[0] = 1f
        assertEquals(0x3f800000, ia[0])
    }

    @Test
    fun testLastIndexOf() {
        val bytes = byteArrayOf(1, 2, 3, 4, 2, 3, 7)
        assertEquals(1, bytes.indexOf(byteArrayOf(2, 3)))
        assertEquals(4, bytes.lastIndexOf(byteArrayOf(2, 3)))

        assertEquals(1, bytes.indexOf(byteArrayOf(2, 3, 4)))
        assertEquals(1, bytes.lastIndexOf(byteArrayOf(2, 3, 4)))

        assertEquals(-1, bytes.indexOf(byteArrayOf(2, 3, 4, 5)))
        assertEquals(-1, bytes.lastIndexOf(byteArrayOf(2, 3, 4, 5)))
    }
}
