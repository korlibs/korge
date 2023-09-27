package korlibs.image.color

import kotlin.test.Test
import kotlin.test.assertEquals

class ColorSwizzleTest {
    @Test
    fun testSwizzle() {
        assertEquals(RGBA.float(0f, 0f, 1f, 1f), Colors.RED.swizzle("bgr"))
        assertEquals(RGBA.float(1f, 1f, 0f, 1f), Colors.RED.swizzle("rrb"))
        assertEquals(RGBA.float(1f, 1f, 1f, 1f), Colors.RED.swizzle("rrr"))
    }
}
