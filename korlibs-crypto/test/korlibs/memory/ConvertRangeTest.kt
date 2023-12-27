package korlibs.memory

import korlibs.math.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ConvertRangeTest {
    @Test
    fun test() {
        // Double
        assertEquals(50.0, 10.0.convertRange(0.0, 20.0, 0.0, 100.0))
        assertEquals(150.0, 10.0.convertRange(5.0, 15.0, 100.0, 200.0))
        assertEquals(300.0, 25.0.convertRange(5.0, 15.0, 100.0, 200.0))
        assertEquals(200.0, 25.0.convertRangeClamped(5.0, 15.0, 100.0, 200.0))

        // Int
        assertEquals(50, 10.convertRange(0, 20, 0, 100))
        assertEquals(150, 10.convertRange(5, 15, 100, 200))
        assertEquals(300, 25.convertRange(5, 15, 100, 200))
        assertEquals(200, 25.convertRangeClamped(5, 15, 100, 200))

        // Long
        assertEquals(50L, 10L.convertRange(0L, 20L, 0L, 100L))
        assertEquals(150L, 10L.convertRange(5L, 15L, 100L, 200L))
        assertEquals(300L, 25L.convertRange(5L, 15L, 100L, 200L))
        assertEquals(200L, 25L.convertRangeClamped(5L, 15L, 100L, 200L))
    }
}
