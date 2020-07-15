package com.soywiz.klock.wrapped

import com.soywiz.klock.TimezoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class WTimezoneOffsetTest {
    @Test
    fun `ensureEqualityComparisonIsByValue`() {
        assertEquals(WTimezoneOffset(TimezoneOffset(10.0)), WTimezoneOffset(TimezoneOffset(10.0)))
    }
}
