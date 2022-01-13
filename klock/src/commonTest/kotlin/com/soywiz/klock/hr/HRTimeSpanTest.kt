package com.soywiz.klock.hr

import com.soywiz.klock.milliseconds
import kotlin.test.Test
import kotlin.test.assertEquals

class HRTimeSpanTest {
    @Test
    fun test() {
        assertEquals(HRTimeSpan.fromMilliseconds(10), 10.milliseconds.hr)
    }
}
