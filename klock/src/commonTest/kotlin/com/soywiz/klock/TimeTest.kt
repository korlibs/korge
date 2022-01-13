package com.soywiz.klock

import kotlin.test.Test
import kotlin.test.assertEquals

class TimeTest {

    @Test
    fun test() {
        assertEquals("09:30:00.000", (Time(9) + 30.minutes).toString())
        assertEquals("26:00:00.000", (Time(26)).toString())
        assertEquals("26:00:30.256", (Time(26) + 30.seconds + 256.milliseconds).toString())
        assertEquals("16:00:30.256", (Time(16) + 30.seconds + 256.milliseconds).toString())
    }

    @Test
    fun testProperties() {
        val time = Time(hour = 12, minute = 36, second = 28, millisecond = 128)
        assertEquals(12, time.hour)
        assertEquals(36, time.minute)
        assertEquals(28, time.second)
        assertEquals(128, time.millisecond)

        assertEquals(25, Time(25).hour)
        assertEquals(36, Time(36).hour)
        assertEquals(12, Time(36).hourAdjusted)
    }

    @Test
    fun testAdjusting() {
        val time = Time(hour = 25, minute = 62, second = 70, millisecond = 2000)
        assertEquals(0, time.millisecond)
        assertEquals(12, time.second)
        assertEquals(3, time.minute)
        assertEquals(26, time.hour)
        assertEquals(2, time.hourAdjusted)

        val timeAdjusted = time.adjust()
        assertEquals(0, timeAdjusted.millisecond)
        assertEquals(12, timeAdjusted.second)
        assertEquals(3, timeAdjusted.minute)
        assertEquals(2, timeAdjusted.hour)
        assertEquals(2, timeAdjusted.hourAdjusted)
    }
}
