package korlibs.math.geom

import kotlin.test.Test
import kotlin.test.assertEquals

class PointArrayListTest {
    @Test
    fun reverse() {
        assertEquals("[(1, 1)]", PointIntArrayList().apply { add(1, 1) }.apply { reverse() }.toString())
        assertEquals("[(2, 2), (1, 1)]", PointIntArrayList().apply { add(1, 1).add(2, 2) }.apply { reverse() }.toString())
        assertEquals("[(3, 3), (2, 2), (1, 1)]", PointIntArrayList().apply { add(1, 1).add(2, 2).add(3, 3) }.apply { reverse() }.toString())
    }

    @Test
    fun testClear() {
        val p = PointArrayList()
        p.add(1, 1)
        assertEquals("[(1, 1)]", p.toList().toString())
        p.add(2, 2)
        assertEquals("[(1, 1), (2, 2)]", p.toList().toString())
        assertEquals(2, p.size)
        p.clear()
        assertEquals(0, p.size)
        assertEquals("[]", p.toList().toString())
        p.add(2, 2)
        assertEquals(1, p.size)
        assertEquals("[(2, 2)]", p.toList().toString())
    }

    @Test
    fun testTransform() {
        val list = PointArrayList().add(1, 1).add(2, 2).add(3, 3)
        assertEquals("[(1, 1), (2, 2), (3, 3)]", list.toList().toString())
        assertEquals("[(10, -10), (20, -20), (30, -30)]", list.clone().also { it.transform(Matrix().scaled(10, -10)) }.toList().toString())
        assertEquals("[(1, 1), (2, 2), (3, 3)]", list.toList().toString())
    }

    @Test
    fun testInsert() {
        val list = pointArrayListOf(Point(1, -1), Point(2, -2), Point(3, -3))
        list.insertAt(1, Point(0, -1))
        assertEquals("[(1, -1), (0, -1), (2, -2), (3, -3)]", list.toList().toString())
        list.removeAt(1, 2)
        assertEquals("[(1, -1), (3, -3)]", list.toList().toString())
        list.removeAt(0, 2)
        assertEquals("[]", list.toList().toString())
        list.insertAt(0, Point(0, -1))
        assertEquals("[(0, -1)]", list.toList().toString())
        list.insertAt(1, pointArrayListOf(Point(2, -2), Point(4, -4)))
        assertEquals("[(0, -1), (2, -2), (4, -4)]", list.toList().toString())
    }

    @Test
    fun testCreation() {
        assertEquals("[(1, 2), (3, 4)]", pointArrayListOf(1, 2, 3, 4).toString())
        assertEquals("[(1, 2), (3, 4)]", pointArrayListOf(1.0, 2.0, 3.0, 4.0).toString())
    }

    @Test
    fun testFlatten() {
        assertEquals(
            "[(1, 2), (3, 4), (5, 6), (7, 8), (9, 10)]",
            listOf(pointArrayListOf(1, 2, 3, 4), pointArrayListOf(5, 6, 7, 8), PointArrayList(9, 10)).flatten().toString()
        )
    }
}
