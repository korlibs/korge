package korlibs.time

import kotlin.test.Test
import kotlin.test.assertTrue

class KlockTest {
    //@Test
    @Test
    fun testTimeAdvances() {
        val time1 = DateTime.nowUnixMillis()
        assertTrue("Time is provided in milliseconds since EPOCH. Expected ($time1 >= 1508887000000)") { time1 >= 1508887000000 }
        while (true) {
            val time2 = DateTime.nowUnixMillis()
            assertTrue("Time advances") { time2 >= time1 }
            if (time2 > time1) break
        }
    }

    @Test
    fun testThatLocalTimezoneOffsetRuns() {
        assertTrue(TimezoneOffset.local(DateTime(0L)).time.seconds != -1.0)
    }
}
