package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals

class FastArrayListTest {
    @Test
    fun test() {
        val fal = FastArrayList<Int>()
        fal.add(1)
        fal.add(2)
        fal.add(3)
        fal.add(4)
        assertEquals(0, fal.indexOf(1))
        assertEquals(1, fal.indexOf(2))
        assertEquals(2, fal.indexOf(3))
        assertEquals(3, fal.indexOf(4))

        assertEquals("[1, 2, 3, 4]", fal.toString())
        fal.removeAt(1)
        assertEquals("[1, 3, 4]", fal.toString())
        fal.removeAt(1)
        assertEquals("[1, 4]", fal.toString())
        fal.removeAt(0)
        assertEquals("[4]", fal.toString())
        fal.removeAt(0)
        assertEquals("[]", fal.toString())

        assertEquals(-1, fal.indexOf(1))
        assertEquals(-1, fal.indexOf(2))
        assertEquals(-1, fal.indexOf(3))
        assertEquals(-1, fal.indexOf(4))

        fal.add(0, 4)
        fal.add(0, 3)
        fal.add(0, 1)
        fal.add(1, 2)
        assertEquals("[1, 2, 3, 4]", fal.toString())
    }

    @Test
    fun testAddLast() {
        run {
            val a = ArrayList<Int>(1)
            a.add(0, 0)
            a.add(1, 1)
            assertEquals(listOf(0, 1), a.toList())
        }
        run {
            val a = FastArrayList<Int>(1)
            a.add(0, 0)
            a.add(1, 1)
            assertEquals(listOf(0, 1), a.toList())
        }
    }

    @Test
    fun testToFastList() {
        assertEquals(listOf(1, 2, 3), listOf(1, 2, 3).toFastList(FastArrayList()))
        assertEquals(listOf(1, 2, 3), listOf(1, 2, 3).toFastList(FastArrayList(listOf(-1, -2, -3))))
        assertEquals(listOf(1, 2, 3), listOf(1, 2, 3).toFastList(FastArrayList(listOf(-1))))
        assertEquals(listOf(1, 2, 3), listOf(1, 2, 3).toFastList(FastArrayList(listOf(-1, -2, -3, -4))))
    }

    @Test
    fun testRemove() {
        val list = fastArrayListOf(1, 2, 3)
        assertEquals(true, list.remove(2))
        assertEquals(listOf(1, 3), list)
        assertEquals(false, list.remove(2))
        assertEquals(listOf(1, 3), list)
    }

    @Test
    fun testRemoveAt() {
        val list = fastArrayListOf(1, 2, 3, 4, 5)
        list.removeAt(4)
        assertEquals(listOf(1, 2, 3, 4), list)
        list.removeAt(0)
        assertEquals(listOf(2, 3, 4), list)
        list.removeAt(1)
        assertEquals(listOf(2, 4), list)
    }

    @Test
    fun testClear() {
        val list = fastArrayListOf(1, 2, 3)
        assertEquals(listOf(1, 2, 3), list)
        list.clear()
        assertEquals(listOf(), list)
        list.add(1)
        assertEquals(listOf(1), list)
    }

    @Test
    fun testToArrayIsACopy() {
        val list = fastArrayListOf(1, 2, 3)
        val list2 = list.toTypedArray()
        list[0] = -1
        val list3 = list.toTypedArray()
        assertEquals(listOf(1, 2, 3), list2.toList())
        assertEquals(listOf(-1, 2, 3), list3.toList())
    }

    @Test
    fun testAddAll() {
        assertEquals(
            listOf(1, 2, 3, 4, 5, 6),
            fastArrayListOf(1, 2, 3).also { it.addAll(fastArrayListOf(4, 5, 6)) }
        )
    }
}
