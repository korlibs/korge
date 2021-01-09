package com.soywiz.korma.geom

import kotlin.test.Test
import kotlin.test.assertEquals

class RectangleIntTest {
    @Test
    fun name() {
        assertEquals(SizeInt(25, 100), SizeInt(50, 200).fitTo(container = SizeInt(100, 100)))
        assertEquals(SizeInt(50, 200), SizeInt(50, 200).fitTo(container = SizeInt(100, 200)))
    }

    @Test
    fun corners() {
        val rectangle = RectangleInt(1, 20, 300, 4000)
        assertEquals(IPointInt(1, 20), rectangle.topLeft)
        assertEquals(IPointInt(301, 20), rectangle.topRight)
        assertEquals(IPointInt(1, 4020), rectangle.bottomLeft)
        assertEquals(IPointInt(301, 4020), rectangle.bottomRight)

        val iRectangle = IRectangleInt(1000, 200, 30, 4)
        assertEquals(IPointInt(1000, 200), iRectangle.topLeft)
        assertEquals(IPointInt(1030, 200), iRectangle.topRight)
        assertEquals(IPointInt(1000, 204), iRectangle.bottomLeft)
        assertEquals(IPointInt(1030, 204), iRectangle.bottomRight)
    }
}
