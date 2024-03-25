package korlibs.math.interpolation

import kotlin.test.*

class RatioTest {
    @Test
    fun testSimple() {
        assertEquals(Ratio.ZERO, Ratio(0.0, 10.0))
        assertEquals(Ratio.HALF, Ratio(5.0, 10.0))
        assertEquals(Ratio.ONE, Ratio(10.0, 10.0))
    }
}
