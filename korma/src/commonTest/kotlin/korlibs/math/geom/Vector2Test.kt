package korlibs.math.geom

import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals

class Vector2Test {
    @Test
    fun name() {
        val v = MPoint(1.0, 1.0)
        //assertEquals(sqrt(2.0), v.length, 0.001)
        assertEquals(sqrt(2.0), v.length)
    }

    @Test
    fun testString() {
        assertEquals("(1, 2)", MPoint(1, 2).toString())

    }
}