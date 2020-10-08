package com.soywiz.klock.internal

import com.soywiz.klock.hours
import com.soywiz.klock.minutes
import kotlin.test.Test
import kotlin.test.assertEquals

class TimeZoneParserTest {
    @Test
    fun test() {
        assertEquals(null, "TEST".tz)
        assertEquals(0.hours, "Z".tz)
        assertEquals(0.hours, "UTC".tz)
        assertEquals(0.hours, "GMT".tz)
        assertEquals((-7).hours, "PDT".tz)
        assertEquals((-8).hours, "PST".tz)
        assertEquals(2.hours + 30.minutes, "UTC+0230".tz)
        assertEquals(-(2.hours + 30.minutes), "UTC-0230".tz)
    }
    private val String.tz get() = reader.readTimeZoneOffset()
    private val String.reader get() = MicroStrReader(this)
}
