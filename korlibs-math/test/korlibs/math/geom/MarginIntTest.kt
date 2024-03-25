package korlibs.math.geom

import kotlin.test.*

class MarginIntTest {
    @Test
    fun testBasicProperties() {
        val margin = MarginInt(1, -3, -7, 33)

        assertEquals(1, margin.top)
        assertEquals(-3, margin.right)
        assertEquals(-7, margin.bottom)
        assertEquals(33, margin.left)
        assertEquals(30, margin.leftPlusRight)
        assertEquals(-6, margin.topPlusBottom)
        assertEquals(15, margin.horizontal)
        assertEquals(-3, margin.vertical)
    }

    @Test
    fun testToString() {
        assertEquals(
            "MarginInt(top=1, right=-3, bottom=-7, left=33)",
            MarginInt(1, -3, -7, 33).toString()
        )
    }

    @Test
    fun testPlus() {
        assertEquals(
            MarginInt(11, 22, 33, 44),
            MarginInt(10, 20, 30, 40) + MarginInt(1, 2, 3, 4)
        )
    }

    @Test
    fun testMinus() {
        assertEquals(
            MarginInt(9, 18, 27, 36),
            MarginInt(10, 20, 30, 40) - MarginInt(1, 2, 3, 4)
        )
    }
}
