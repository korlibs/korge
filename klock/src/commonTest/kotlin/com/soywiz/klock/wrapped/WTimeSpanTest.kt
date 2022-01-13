package com.soywiz.klock.wrapped

import com.soywiz.klock.TimeSpan
import kotlin.test.Test
import kotlin.test.assertEquals

class WTimeSpanTest {
    @Test
    fun `ensureEqualityComparisonIsByValue`() {
        assertEquals(WTimeSpan(TimeSpan(10.0)), WTimeSpan(TimeSpan(10.0)))
    }
}
