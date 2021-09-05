package com.soywiz.klock

import kotlin.test.*

class DateFormatTest {
    @Test
    fun test() {
        assertEquals("Sat, 03 Dec 2011 10:15:30 GMT+0100", DateFormat.FORMAT1.parse("2011-12-03T10:15:30+01:00").toStringDefault())
        assertEquals("Sat, 03 Dec 2011 10:15:30 GMT+0100", DateFormat.FORMAT1.parse("2011-12-03T10:15:30+0100").toStringDefault())
    }
}
