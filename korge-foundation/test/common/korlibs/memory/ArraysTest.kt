package korlibs.memory

import kotlin.test.*

class ArraysTest {

    @Test
    fun testCopyStrided() {
        val data = ByteArray(6)
        val data0 = byteArrayOf(0, 2, 4)
        val data1 = byteArrayOf(1, 3, 5)
        arraycopyStride(data0, 0, 1, data, 0, 2, 3)
        arraycopyStride(data1, 0, 1, data, 1, 2, 3)
        assertEquals(listOf(0, 1, 2, 3, 4, 5), data.map { it.toInt() })
    }

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

    @Test
    fun testGetSampled() {
        val array = byteArrayOf(0, 127, 64)
        assertEquals(0, array.getSampled(0f))
        assertEquals(15, array.getSampled(.125f))
        assertEquals(63, array.getSampled(.5f))
        assertEquals(111, array.getSampled(.875f))
        assertEquals(127, array.getSampled(1f))
        assertEquals(119, array.getSampled(1.125f))
        assertEquals(95, array.getSampled(1.5f))
        assertEquals(71, array.getSampled(1.875f))
        assertEquals(64, array.getSampled(2f))
    }
}
