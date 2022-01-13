package com.soywiz.klock

import kotlin.test.Test
import kotlin.test.assertEquals

class TimeFormatTest {

    @Test
    fun testFormat() {
        val time1 = Time(hour = 48, minute = 23, second = 12, millisecond = 450)
        assertEquals("48:23:12.450", TimeFormat.DEFAULT_FORMAT.format(time1))
        assertEquals("48:23:12", TimeFormat.FORMAT_TIME.format(time1))
        assertEquals("48.23.12", TimeFormat("HH.mm.ss").format(time1))
        assertEquals("12:23:12", TimeFormat("hh:mm:ss").format(time1))
        assertEquals("48:23:12", TimeFormat("kk:mm:ss").format(time1))
        assertEquals("00:23:12", TimeFormat("KK:mm:ss").format(time1))

        val time2 = Time(hour = 50, minute = 2, second = 0, millisecond = 109)
        assertEquals("2:2:0.10", TimeFormat("h:m:s.SS").format(time2))
        assertEquals("2:2:0.1", TimeFormat("h:m:s.S").format(time2))
        assertEquals("2:2:0", TimeFormat("K:m:s").format(time2))
    }

    @Test
    fun testParse() {
        val time1 = TimeFormat.FORMAT_TIME.parseTime("23:59:59")
        assertEquals(23, time1.hour)
        assertEquals(59, time1.minute)
        assertEquals(59, time1.second)
        assertEquals(0, time1.millisecond)

        val time2 = TimeFormat.FORMAT_TIME.parseTime("48:00:00")
        assertEquals(48, time2.hour)
        assertEquals(0, time2.minute)
        assertEquals(0, time2.second)
        assertEquals(0, time2.millisecond)

        val time3 = TimeFormat.DEFAULT_FORMAT.parseTime("23:59:59.999")
        assertEquals(23, time3.hour)
        assertEquals(59, time3.minute)
        assertEquals(59, time3.second)
        assertEquals(999, time3.millisecond)

        val time4 = TimeFormat.DEFAULT_FORMAT.parseTime("48:00:00.000")
        assertEquals(48, time4.hour)
        assertEquals(0, time4.minute)
        assertEquals(0, time4.second)
        assertEquals(0, time4.millisecond)

        val time = TimeFormat("ss.mm.HH").parseTime("59.59.48")
        assertEquals(48, time.hour)
        assertEquals(59, time.minute)
        assertEquals(59, time.second)
        assertEquals(0, time.millisecond)
    }

    @Test
    fun testFromUnix() {
        val now = 1611658981L * 1000
        val diff = TimezoneOffset.local(DateTime(now))

        val dateUnix = DateTimeTz.fromUnix(now)
        val dateUnixLocal = DateTimeTz.fromUnixLocal(now).addOffset(diff)

        assertEquals(dateUnix.hours, dateUnixLocal.hours)
    }
}
