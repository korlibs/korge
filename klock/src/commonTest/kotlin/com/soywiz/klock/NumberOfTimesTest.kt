package com.soywiz.klock

import kotlin.test.Test
import kotlin.test.assertEquals

class NumberOfTimesTest {
    @Test
    fun testInfinite() {
        assertEquals(infiniteTimes, infiniteTimes.oneLess)
        assertEquals(infiniteTimes, infiniteTimes + 1.times)
        assertEquals(infiniteTimes, infiniteTimes - 1.times)
        assertEquals(0.times, infiniteTimes - infiniteTimes)
    }

    @Test
    fun testOps() {
        assertEquals(0.times, 1.times.oneLess)
        assertEquals((-1).times, 0.times.oneLess)
    }

    @Test
    fun testTimes() {
        assertEquals(true, infiniteTimes.hasMore)
        assertEquals(true, 1.times.hasMore)
        assertEquals(false, 0.times.hasMore)
    }
}
