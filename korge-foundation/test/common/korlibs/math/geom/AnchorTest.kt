package korlibs.math.geom

import korlibs.math.interpolation.*
import kotlin.test.*

class AnchorTest {
    val rect = Rectangle(0, 0, 100, 100)

    @Test
    fun test() {
        assertEquals(Point(0, 0), rect.getAnchoredPoint(Anchor.TOP_LEFT))
        assertEquals(Point(50, 0), rect.getAnchoredPoint(Anchor.TOP_CENTER))
        assertEquals(Point(100, 0), rect.getAnchoredPoint(Anchor.TOP_RIGHT))

        assertEquals(Point(0, 50), rect.getAnchoredPoint(Anchor.MIDDLE_LEFT))
        assertEquals(Point(50, 50), rect.getAnchoredPoint(Anchor.MIDDLE_CENTER))
        assertEquals(Point(100, 50), rect.getAnchoredPoint(Anchor.MIDDLE_RIGHT))

        assertEquals(Point(0, 100), rect.getAnchoredPoint(Anchor.BOTTOM_LEFT))
        assertEquals(Point(50, 100), rect.getAnchoredPoint(Anchor.BOTTOM_CENTER))
        assertEquals(Point(100, 100), rect.getAnchoredPoint(Anchor.BOTTOM_RIGHT))

        assertEquals(Anchor.TOP_LEFT, Ratio.ZERO.interpolate(Anchor.TOP_LEFT, Anchor.BOTTOM_RIGHT))
        assertEquals(Anchor.MIDDLE_CENTER, Ratio.HALF.interpolate(Anchor.TOP_LEFT, Anchor.BOTTOM_RIGHT))
        assertEquals(Anchor.BOTTOM_RIGHT, Ratio.ONE.interpolate(Anchor.TOP_LEFT, Anchor.BOTTOM_RIGHT))

        assertEquals(Anchor.MIDDLE_CENTER, Anchor(0.5, 0.5))
        assertEquals(Anchor(0.6, 0.6), Anchor(0.6, 0.6))
        assertEquals(0.6, Anchor(0.6, 0.7).sx)
        assertEquals(0.7, Anchor(0.6, 0.7).sy)
    }
}
