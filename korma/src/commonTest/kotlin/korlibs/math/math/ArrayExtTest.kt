package korlibs.math.math

import korlibs.math.maxOrElse
import korlibs.math.minOrElse
import kotlin.test.Test
import kotlin.test.assertEquals

class ArrayExtTest {
    @Test
    fun testMinOrElse() {
        assertEquals(1.0, doubleArrayOf().minOrElse(1.0))
        assertEquals(0.0, doubleArrayOf(0.0).minOrElse(1.0))
        assertEquals(-1.0, doubleArrayOf(0.0, 1.0, -1.0).minOrElse(1.0))
    }

    @Test
    fun testMaxOrElse() {
        assertEquals(1.0, doubleArrayOf().maxOrElse(1.0))
        assertEquals(0.0, doubleArrayOf(0.0).maxOrElse(1.0))
        assertEquals(+1.0, doubleArrayOf(0.0, 1.0, -1.0).maxOrElse(1.0))
    }
}
