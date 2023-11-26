package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ArrayListTest {
    @Test
    fun testRotated() {
        assertEquals(listOf(1, 2, 3, 4), listOf(1, 2, 3, 4).rotated(0))

        assertEquals(listOf(4, 1, 2, 3), listOf(1, 2, 3, 4).rotated(+1))
        assertEquals(listOf(3, 4, 1, 2), listOf(1, 2, 3, 4).rotated(+2))
        assertEquals(listOf(2, 3, 4, 1), listOf(1, 2, 3, 4).rotated(+3))

        assertEquals(listOf(2, 3, 4, 1), listOf(1, 2, 3, 4).rotated(-1))
        assertEquals(listOf(3, 4, 1, 2), listOf(1, 2, 3, 4).rotated(-2))
        assertEquals(listOf(4, 1, 2, 3), listOf(1, 2, 3, 4).rotated(-3))
    }

    @Test
    fun testInt() {
        val values = IntArrayList(2)
        assertEquals(0, values.size)
        assertEquals(2, values.capacity)
        values.add(1)
        assertEquals(listOf(1), values.toList())
        assertEquals(1, values.size)
        assertEquals(2, values.capacity)
        values.add(2)
        assertEquals(listOf(1, 2), values.toList())
        assertEquals(2, values.size)
        assertEquals(2, values.capacity)
        values.add(3)
        assertEquals(listOf(1, 2, 3), values.toList())
        assertEquals(3, values.size)
        assertEquals(6, values.capacity)

        run {
            val v = IntArrayList()
            v.add(1)
            v.add(2)
            v.add(3)
            assertEquals(listOf(1, 2, 3), v.toList())
            v.removeAt(1)
            assertEquals(listOf(1, 3), v.toList())
            assertEquals(2, v.size)
            v.removeAt(1)
            assertEquals(listOf(1), v.toList())
            v.removeAt(0)
            assertEquals(listOf(), v.toList())
        }
    }

    @Test
    fun testFloat() {
        val values = FloatArrayList(2)
        assertEquals(0, values.size)
        assertEquals(2, values.capacity)
        values.add(1f)
        assertEquals(listOf(1f), values.toList())
        assertEquals(1, values.size)
        assertEquals(2, values.capacity)
        values.add(2f)
        assertEquals(listOf(1f, 2f), values.toList())
        assertEquals(2, values.size)
        assertEquals(2, values.capacity)
        values.add(3f)
        assertEquals(listOf(1f, 2f, 3f), values.toList())
        assertEquals(3, values.size)
        assertEquals(6, values.capacity)

        run {
            val v = FloatArrayList()
            v.add(1f)
            v.add(2f)
            v.add(3f)
            assertEquals(listOf(1f, 2f, 3f), v.toList())
            v.removeAt(1)
            assertEquals(listOf(1f, 3f), v.toList())
            assertEquals(2, v.size)
            v.removeAt(1)
            assertEquals(listOf(1f), v.toList())
            v.removeAt(0)
            assertEquals(listOf(), v.toList())
        }
    }

    @Test
    fun testDouble() {
        val values = DoubleArrayList(2)
        assertEquals(0, values.size)
        assertEquals(2, values.capacity)
        values.add(1.0)
        assertEquals(listOf(1.0), values.toList())
        assertEquals(1, values.size)
        assertEquals(2, values.capacity)
        values.add(2.0)
        assertEquals(listOf(1.0, 2.0), values.toList())
        assertEquals(2, values.size)
        assertEquals(2, values.capacity)
        values.add(3.0)
        assertEquals(listOf(1.0, 2.0, 3.0), values.toList())
        assertEquals(3, values.size)
        assertEquals(6, values.capacity)

        run {
            val v = DoubleArrayList()
            v.add(1.0)
            v.add(2.0)
            v.add(3.0)
            assertEquals(listOf(1.0, 2.0, 3.0), v.toList())
            v.removeAt(1)
            assertEquals(listOf(1.0, 3.0), v.toList())
            assertEquals(2, v.size)
            v.removeAt(1)
            assertEquals(listOf(1.0), v.toList())
            v.removeAt(0)
            assertEquals(listOf(), v.toList())
        }
    }

    @Test
    fun map() {
        assertEquals(intArrayListOf(0, 6, 12, 18, 24), (0 until 10).mapInt { it * 3 }.filter { it % 2 == 0 })
    }

    @Test
    fun list() {
        assertEquals(intArrayListOf(1, 2, 3).toList(), listOf(1, 2, 3))
        assertEquals(listOf(1, 2, 3), intArrayListOf(1, 2, 3).toList())
    }

    @Test
    fun demo() {
        val a1 = intArrayListOf(1, 2, 3, 4)
        val a2 = IntArrayList(a1)
        assertEquals(listOf(1, 2, 3, 4), a1.toList())
        assertEquals(listOf(1, 2, 3, 4), a2.toList())
    }

    @Test
    fun listIterator() {
        assertEquals(listOf(2, 3, 4), intArrayListOf(1, 2, 3, 4).listIterator(1).asSequence().toList())
    }

    @Test
    fun testInsertAt() {
        assertEquals(listOf(4, 3, 2, 1), intArrayListOf().insertAt(0, 1).insertAt(0, 2).insertAt(0, 3).insertAt(0, 4).toList())
        assertEquals(listOf(21, 22, 23, 11, 12, 13, 1, 2, 3), intArrayListOf().insertAt(0, intArrayOf(1, 2, 3)).insertAt(0, intArrayOf(11, 12, 13)).insertAt(0, intArrayOf(21, 22, 23)).toList())
    }

    @Test
    fun testRemoveAt() {
        assertEquals(listOf(1, 5), intArrayListOf(1, 2, 3, 4, 5).apply { removeAt(1, 3) }.toList())
        assertEquals(listOf(1), intArrayListOf(1, 2, 3, 4, 5).apply { removeAt(1, 4) }.toList())
        assertEquals(listOf(5), intArrayListOf(1, 2, 3, 4, 5).apply { removeAt(0, 4) }.toList())
        assertEquals(listOf(), intArrayListOf(1, 2, 3, 4, 5).apply { removeAt(0, 5) }.toList())
    }

    @Test
    fun testHashCodeIntArrayList() {
        val a = IntArrayList(10)
        val hc0 = a.hashCode()
        a.add(10)
        val hc1 = a.hashCode()
        a.add(20)
        val hc2 = a.hashCode()
        a.removeAt(a.size - 1)
        val hc3 = a.hashCode()
        val b = IntArrayList(10).also { it.add(a) }
        assertNotEquals(hc1, hc0)
        assertNotEquals(hc1, hc2)
        assertEquals(hc1, hc3)
        assertEquals(a, b)
    }

    @Test
    fun testHashCodeFloatArrayList() {
        val a = FloatArrayList(10)
        val hc0 = a.hashCode()
        a.add(10f)
        val hc1 = a.hashCode()
        a.add(20f)
        val hc2 = a.hashCode()
        a.removeAt(a.size - 1)
        val hc3 = a.hashCode()
        val b = FloatArrayList(10).also { it.add(a) }
        assertNotEquals(hc1, hc0)
        assertNotEquals(hc1, hc2)
        assertEquals(hc1, hc3)
        assertEquals(a, b)
    }

    @Test
    fun testHashCodeDoubleArrayList() {
        val a = DoubleArrayList(10)
        val hc0 = a.hashCode()
        a.add(10.0)
        val hc1 = a.hashCode()
        a.add(20.0)
        val hc2 = a.hashCode()
        a.removeAt(a.size - 1)
        val hc3 = a.hashCode()
        val b = DoubleArrayList(10).also { it.add(a) }
        assertNotEquals(hc1, hc0)
        assertNotEquals(hc1, hc2)
        assertEquals(hc1, hc3)
        assertEquals(a, b)
    }

    @Test
    fun testHashCodeLongArrayList() {
        val a = ArrayList<Any>(10)
        val hc0 = a.hashCode()
        a.add(Unit)
        val hc1 = a.hashCode()
        a.add(Unit)
        val hc2 = a.hashCode()
        a.removeAt(a.size - 1)
        val hc3 = a.hashCode()
        val b = ArrayList<Any>(10).also { it.addAll(a) }
        assertNotEquals(hc1, hc0)
        assertNotEquals(hc1, hc2)
        assertEquals(hc1, hc3)
        assertEquals(a, b)
    }
}
