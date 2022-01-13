package com.soywiz.klock

import kotlin.test.Test
import kotlin.test.assertEquals

class FrequencyTest {
    @Test
    fun test() {
        assertEquals(100.milliseconds, 10.timesPerSecond.timeSpan)
        assertEquals(10.hz, 100.milliseconds.hz)
    }
}
