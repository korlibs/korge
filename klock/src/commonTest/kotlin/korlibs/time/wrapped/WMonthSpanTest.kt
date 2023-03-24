package korlibs.time.wrapped

import korlibs.time.MonthSpan
import kotlin.test.Test
import kotlin.test.assertEquals

class WMonthSpanTest {
    @Test
    fun `ensureEqualityComparisonIsByValue`() {
        assertEquals(WMonthSpan(MonthSpan(1)), WMonthSpan((MonthSpan(1))))
    }
}
