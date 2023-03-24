package korlibs.time.wrapped

import korlibs.time.Year
import kotlin.test.Test
import kotlin.test.assertEquals

class WYearTest {
    @Test
    fun `ensureEqualityComparisonIsByValue`() {
        assertEquals(WYear(Year(10)), WYear(Year(10)))
    }
}