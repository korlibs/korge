package korlibs.math.geom

import korlibs.number.*
import korlibs.platform.*
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

        assertEquals((1.1), margin.top, 0.001)
        assertEquals((-3.2), margin.right, 0.001)
        assertEquals((-7.3), margin.bottom, 0.001)
        assertEquals((33.4), margin.left, 0.001)
        assertEquals((30.2), margin.leftPlusRight, 0.001)
        assertEquals((-6.2), margin.topPlusBottom, 0.001)
        assertEquals((15.1), margin.horizontal, 0.001)
        assertEquals((-3.1), margin.vertical, 0.001)

    }

    @Test
    fun testToString() {
        if (Platform.isWasm) {
            println("!! WASM: SKIPPING FOR NOW BECAUSE toString differs!")
            return
        }
        assertEquals(
            "Margin(top=1.1, right=-3.2, bottom=-7.3, left=33.4)",
            Margin(1.1, -3.2, -7.3, 33.4).toString()
        )
    }

    @Test
    fun testPlus() {
        assertEquals(
            Margin(1.1, 2.2, 3.3, 4.4),
            Margin(1, 2, 3, 4) + Margin(.1, .2, .3, .4)
        )
    }

    @Test
    fun testMinus() {
        assertEqualsFloat(
            Margin(0.9, 1.8, 2.7, 3.6),
            Margin(1, 2, 3, 4) - Margin(.1, .2, .3, .4),
            0.0001
        )
    }
}
