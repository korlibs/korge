package korlibs.time.hr

import korlibs.time.milliseconds
import kotlin.test.Test
import kotlin.test.assertEquals

class HRTimeSpanTest {
    @Test
    fun test() {
        assertEquals(HRTimeSpan.fromMilliseconds(10), 10.milliseconds.hr)
    }
}
