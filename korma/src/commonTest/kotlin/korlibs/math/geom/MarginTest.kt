package korlibs.math.geom

import korlibs.memory.*
import kotlin.test.*

class MarginTest {
    @Test
    fun testBasicProperties() {
        val margin = Margin(1.1f, -3.2f, -7.3f, 33.4f)

        assertEquals((1.1f).toFixedShort(), margin.topFixed)
        assertEquals((-3.2f).toFixedShort(), margin.rightFixed)
        assertEquals((-7.3f).toFixedShort(), margin.bottomFixed)
        assertEquals((33.4f).toFixedShort(), margin.leftFixed)
        assertEquals((30.2f).toFixedShort(), margin.leftPlusRightFixed)
        assertEquals((-6.2f).toFixedShort(), margin.topPlusBottomFixed)
        assertEquals((15.1f).toFixedShort(), margin.horizontalFixed)
        assertEquals((-3.1f).toFixedShort(), margin.verticalFixed)

        assertEquals((1.1f), margin.top, 0.001f)
        assertEquals((-3.2f), margin.right, 0.001f)
        assertEquals((-7.3f), margin.bottom, 0.001f)
        assertEquals((33.4f), margin.left, 0.001f)
        assertEquals((30.2f), margin.leftPlusRight, 0.001f)
        assertEquals((-6.2f), margin.topPlusBottom, 0.001f)
        assertEquals((15.1f), margin.horizontal, 0.001f)
        assertEquals((-3.1f), margin.vertical, 0.001f)

    }

    @Test
    fun testToString() {
        assertEquals(
            "Margin(top=1.1, right=-3.2, bottom=-7.3, left=33.4)",
            Margin(1.1f, -3.2f, -7.3f, 33.4f).toString()
        )
    }

    @Test
    fun testPlus() {
        assertEquals(
            Margin(1.1f, 2.2f, 3.3f, 4.4f),
            Margin(1f, 2f, 3f, 4f) + Margin(.1f, .2f, .3f, .4f)
        )
    }

    @Test
    fun testMinus() {
        assertEquals(
            Margin(0.9f, 1.8f, 2.7f, 3.6f),
            Margin(1f, 2f, 3f, 4f) - Margin(.1f, .2f, .3f, .4f)
        )
    }
}
