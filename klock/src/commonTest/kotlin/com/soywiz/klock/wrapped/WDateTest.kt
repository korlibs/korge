package com.soywiz.klock.wrapped

import kotlin.test.Test
import kotlin.test.assertEquals

class WDateTest {
    @Test
    fun `ensureEqualityComparisonIsByValue`() {
        assertEquals(WDate(2010, 1, 1), WDate(2010, 1, 1))
    }
}
