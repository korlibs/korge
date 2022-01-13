package com.soywiz.klock.wrapped

import com.soywiz.klock.Year
import kotlin.test.Test
import kotlin.test.assertEquals

class WYearMonthTest {
    @Test
    fun `ensureEqualityComparisonIsByValue`() {
        assertEquals(WYearMonth(10, 1), WYearMonth(10, 1))
    }
}
