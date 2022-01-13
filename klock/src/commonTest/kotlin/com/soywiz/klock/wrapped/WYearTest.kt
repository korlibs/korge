package com.soywiz.klock.wrapped

import com.soywiz.klock.TimezoneOffset
import com.soywiz.klock.Year
import kotlin.test.Test
import kotlin.test.assertEquals

class WYearTest {
    @Test
    fun `ensureEqualityComparisonIsByValue`() {
        assertEquals(WYear(Year(10)), WYear(Year(10)))
    }
}
