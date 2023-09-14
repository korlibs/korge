package korlibs.math.interpolation

import kotlin.test.Test
import kotlin.test.assertEquals

class InterpolationTest {
    @Test
    fun test() {
        assertEquals(100, 0.0.toRatio().interpolate(100, 200))
        assertEquals(150, 0.5.toRatio().interpolate(100, 200))
        assertEquals(200, 1.0.toRatio().interpolate(100, 200))

        assertEquals(100L, 0.0.toRatio().interpolate(100L, 200L))
        assertEquals(150L, 0.5.toRatio().interpolate(100L, 200L))
        assertEquals(200L, 1.0.toRatio().interpolate(100L, 200L))

        assertEquals(100f, 0.0.toRatio().interpolate(100f, 200f))
        assertEquals(150f, 0.5.toRatio().interpolate(100f, 200f))
        assertEquals(200f, 1.0.toRatio().interpolate(100f, 200f))

        assertEquals(100.0, 0.0.toRatio().interpolate(100.0, 200.0))
        assertEquals(150.0, 0.5.toRatio().interpolate(100.0, 200.0))
        assertEquals(200.0, 1.0.toRatio().interpolate(100.0, 200.0))
    }
}
