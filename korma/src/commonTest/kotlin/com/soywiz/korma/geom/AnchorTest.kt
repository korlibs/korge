package com.soywiz.korma.geom

import com.soywiz.korma.interpolation.*
import kotlin.test.*

class AnchorTest {
    val rect = Rectangle(0, 0, 100, 100)

    @Test
    fun test() {
        assertEquals(Point(0, 0), rect.getAnchoredPosition(Anchor.TOP_LEFT))
        assertEquals(Point(50, 0), rect.getAnchoredPosition(Anchor.TOP_CENTER))
        assertEquals(Point(100, 0), rect.getAnchoredPosition(Anchor.TOP_RIGHT))

        assertEquals(Point(0, 50), rect.getAnchoredPosition(Anchor.MIDDLE_LEFT))
        assertEquals(Point(50, 50), rect.getAnchoredPosition(Anchor.MIDDLE_CENTER))
        assertEquals(Point(100, 50), rect.getAnchoredPosition(Anchor.MIDDLE_RIGHT))

        assertEquals(Point(0, 100), rect.getAnchoredPosition(Anchor.BOTTOM_LEFT))
        assertEquals(Point(50, 100), rect.getAnchoredPosition(Anchor.BOTTOM_CENTER))
        assertEquals(Point(100, 100), rect.getAnchoredPosition(Anchor.BOTTOM_RIGHT))

        assertEquals(Anchor.TOP_LEFT, 0.0.interpolate(Anchor.TOP_LEFT, Anchor.BOTTOM_RIGHT))
        assertEquals(Anchor.MIDDLE_CENTER, 0.5.interpolate(Anchor.TOP_LEFT, Anchor.BOTTOM_RIGHT))
        assertEquals(Anchor.BOTTOM_RIGHT, 1.0.interpolate(Anchor.TOP_LEFT, Anchor.BOTTOM_RIGHT))

        assertEquals(Anchor.MIDDLE_CENTER, Anchor(0.5, 0.5))
        assertEquals(Anchor(0.6, 0.6), Anchor(0.6, 0.6))
        assertEquals(0.6, Anchor(0.6, 0.7).sx)
        assertEquals(0.7, Anchor(0.6, 0.7).sy)
    }
}
