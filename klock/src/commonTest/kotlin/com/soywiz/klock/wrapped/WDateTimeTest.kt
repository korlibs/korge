package com.soywiz.klock.wrapped

import kotlin.test.Test
import kotlin.test.assertEquals

class WDateTimeTest {
    @Test
    fun `ensureEqualityComparisonIsByValue`() {
        assertEquals(WDateTime(2010, 1, 1), WDateTime(2010, 1, 1))
    }
}
