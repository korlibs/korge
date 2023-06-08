package korlibs.time.wrapped

import korlibs.time.TimeSpan
import kotlin.test.Test
import kotlin.test.assertEquals

class WTimeSpanTest {
    @Test
    fun `ensureEqualityComparisonIsByValue`() {
        assertEquals(WTimeSpan(TimeSpan(10.0)), WTimeSpan(TimeSpan(10.0)))
    }
}
