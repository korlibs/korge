package com.soywiz.klock.wrapped

import kotlin.test.Test
import kotlin.test.assertEquals

class WTimeTest {
    @Test
    fun `ensureEqualityComparisonIsByValue`() {
        assertEquals(WTime(10), WTime(10))
    }
}
