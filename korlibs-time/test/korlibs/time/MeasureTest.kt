package korlibs.time

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MeasureTest {
    @Test
    fun test() {
        val result = measureTimeWithResult {
            val start = DateTime.now()
            do {
                val current = DateTime.now()
            } while (current - start < 40.milliseconds)
            "hello"
        }
        assertEquals("hello", result.result)
        assertTrue("Near 40.milliseconds != ${result.time}") { result.time >= 20.milliseconds && result.time <= 1.seconds }
    }
}
