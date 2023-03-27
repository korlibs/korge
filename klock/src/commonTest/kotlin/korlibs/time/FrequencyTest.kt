package korlibs.time

import kotlin.test.Test
import kotlin.test.assertEquals

class FrequencyTest {
    @Test
    fun test() {
        assertEquals(100.milliseconds, 10.timesPerSecond.timeSpan)
        assertEquals(10.hz, 100.milliseconds.hz)
    }

    @Test
    fun testFrequencyOperatorsWorkAsExpected() {
        assertEquals((-60).hz, -(60.hz))
        assertEquals((+60).hz, +(60.hz))
        assertEquals(80.hz, 60.hz + 20.hz)
        assertEquals(40.hz, 60.hz - 20.hz)
        assertEquals(120.hz, 60.hz * 2)
        assertEquals(90.hz, 60.hz * 1.5)
        assertEquals(90.hz, 60.hz * 1.5f)
        assertEquals(5.hz, 65.hz % 30.hz)
    }
}
