package com.soywiz.klock.wrapped

import com.soywiz.klock.MonthSpan
import kotlin.test.Test
import kotlin.test.assertEquals

class WMonthSpanTest {
    @Test
    fun `ensureEqualityComparisonIsByValue`() {
        assertEquals(WMonthSpan(MonthSpan(1)), WMonthSpan((MonthSpan(1))))
    }
}
