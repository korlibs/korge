package com.soywiz.klock

import kotlin.test.Test
import kotlin.test.assertEquals

class ISO8601Test {
    @Test
    fun testDate() {
        val date = DateTime(2019, Month.April, 14)
        assertEquals("2019-04-14", date.format(ISO8601.DATE_CALENDAR_COMPLETE))
        assertEquals("2019-04-14", date.format(ISO8601.DATE_CALENDAR_COMPLETE.extended))
        assertEquals("20190414", date.format(ISO8601.DATE_CALENDAR_COMPLETE.basic))

        assertEquals("2019-04", date.format(ISO8601.DATE_CALENDAR_REDUCED0))
        assertEquals("2019-04", date.format(ISO8601.DATE_CALENDAR_REDUCED0.extended))
        assertEquals("2019-04", date.format(ISO8601.DATE_CALENDAR_REDUCED0.basic))

        assertEquals("2019", date.format(ISO8601.DATE_CALENDAR_REDUCED1))
        assertEquals("2019", date.format(ISO8601.DATE_CALENDAR_REDUCED1.extended))
        assertEquals("2019", date.format(ISO8601.DATE_CALENDAR_REDUCED1.basic))

        assertEquals("19", date.format(ISO8601.DATE_CALENDAR_REDUCED2))
        assertEquals("19", date.format(ISO8601.DATE_CALENDAR_REDUCED2.extended))
        assertEquals("19", date.format(ISO8601.DATE_CALENDAR_REDUCED2.basic))

        assertEquals("+002019-04-14", date.format(ISO8601.DATE_CALENDAR_EXPANDED0))

        assertEquals("2019-W15-7", date.format(ISO8601.DATE_WEEK_COMPLETE))
        assertEquals("+002019-W15-7", date.format(ISO8601.DATE_WEEK_EXPANDED0))

        assertEquals(date, ISO8601.DATE.parse("2019-04-14").utc)
        assertEquals(date, ISO8601.DATE.parse("2019-W15-7").utc)
    }

    @Test
    fun testTime() {
        val time = 15.hours + 30.minutes + 12.seconds
        assertEquals("15:30:12", ISO8601.TIME_LOCAL_COMPLETE.format(time))
        assertEquals("153012", ISO8601.TIME_LOCAL_COMPLETE.basic.format(time))

        assertEquals("15:30:12,00", ISO8601.TIME_LOCAL_FRACTION0.format(time))
        assertEquals("15:30:12,63", ISO8601.TIME_LOCAL_FRACTION0.format(time + 630.milliseconds))

        assertEquals("15:30,20", ISO8601.TIME_LOCAL_FRACTION1.format(time))
        assertEquals("15,50", ISO8601.TIME_LOCAL_FRACTION2.format(time))

        assertEquals("15,50Z", ISO8601.TIME_UTC_FRACTION2.format(time))

        assertEquals(time + 630.milliseconds, ISO8601.TIME_LOCAL_FRACTION0.parse("15:30:12,63"))
    }

    @Test
    fun testInterval() {
        assertEquals(24.hours, 1.days)
        assertEquals((27 * 24).hours, 27.days)
        val time = 1.years + 0.months + 27.days + 11.hours + 9.minutes + 11.seconds
        assertEquals("P1Y0M27DT11H9M11S", ISO8601.INTERVAL_COMPLETE0.format(time))

        assertEquals(time, ISO8601.INTERVAL_COMPLETE0.parse("P1Y0M27DT11H9M11S"))
        assertEquals(time, ISO8601.INTERVAL.parse("P1Y0M27DT11H9M11S"))

    }

    @Test
    fun testWeekOfYear() {
        assertEquals(1, DateTime(2019, Month.January, 1).dayOfYear)

        assertEquals(1, DateTime(2019, Month.January, 1).weekOfYear1)
        assertEquals(1, DateTime(2019, Month.January, 6).weekOfYear1)
        assertEquals(2, DateTime(2019, Month.January, 7).weekOfYear1)
        assertEquals(2, DateTime(2019, Month.January, 13).weekOfYear1)
        assertEquals(3, DateTime(2019, Month.January, 14).weekOfYear1)

        assertEquals(1, DateTime(2018, Month.January, 1).weekOfYear1)
        assertEquals(1, DateTime(2018, Month.January, 7).weekOfYear1)
        assertEquals(2, DateTime(2018, Month.January, 8).weekOfYear1)
        assertEquals(2, DateTime(2018, Month.January, 14).weekOfYear1)
        assertEquals(3, DateTime(2018, Month.January, 15).weekOfYear1)

        assertEquals(1, DateTime(2018, Month.January, 1).weekOfYear1)
        assertEquals(1, DateTime(2018, Month.January, 7).weekOfYear1)
        assertEquals(2, DateTime(2018, Month.January, 8).weekOfYear1)
        assertEquals(2, DateTime(2018, Month.January, 14).weekOfYear1)
        assertEquals(3, DateTime(2018, Month.January, 15).weekOfYear1)

        assertEquals(44, DateTime(2007, Month.November, 3).weekOfYear1)
        assertEquals(6, DateTime(2007, Month.November, 3).dayOfWeek.index1Monday)
    }

    @Test
    fun testDateTimeComplete() {
        assertEquals("20190917T114805", ISO8601.DATETIME_COMPLETE.basic.format(1568720885000))
        assertEquals("2019-09-17T11:48:05", ISO8601.DATETIME_COMPLETE.extended.format(1568720885000))

        assertEquals("Tue, 17 Sep 2019 11:48:05 UTC", ISO8601.DATETIME_COMPLETE.parse("20190917T114805").utc.toStringDefault())
        assertEquals("Tue, 17 Sep 2019 11:48:05 UTC", ISO8601.DATETIME_COMPLETE.parse("2019-09-17T11:48:05").utc.toStringDefault())
    }

    @Test
    fun testDateTimeUtcComplete() {
        assertEquals("20190917T114805Z", ISO8601.DATETIME_UTC_COMPLETE.basic.format(1568720885000))
        assertEquals("2019-09-17T11:48:05Z", ISO8601.DATETIME_UTC_COMPLETE.extended.format(1568720885000))

        assertEquals("Tue, 17 Sep 2019 11:48:05 UTC", ISO8601.DATETIME_UTC_COMPLETE.parse("20190917T114805Z").utc.toStringDefault())
        assertEquals("Tue, 17 Sep 2019 11:48:05 UTC", ISO8601.DATETIME_UTC_COMPLETE.parse("2019-09-17T11:48:05Z").utc.toStringDefault())
    }

    @Test
    fun testDateTimeUtcCompleteFraction() {
        assertEquals("20190917T114805.000Z", ISO8601.DATETIME_UTC_COMPLETE_FRACTION.basic.format(1568720885000))
        assertEquals("2019-09-17T11:48:05.000Z", ISO8601.DATETIME_UTC_COMPLETE_FRACTION.extended.format(1568720885000))

        assertEquals("Tue, 17 Sep 2019 11:48:05 UTC", ISO8601.DATETIME_UTC_COMPLETE_FRACTION.parse("20190917T114805.000Z").utc.toStringDefault())
        assertEquals("Tue, 17 Sep 2019 11:48:05 UTC", ISO8601.DATETIME_UTC_COMPLETE_FRACTION.parse("2019-09-17T11:48:05.000Z").utc.toStringDefault())
    }

    @Test
    fun testIssue84() {
        val badUtc = DateTime(
            date = Date(2020, 1, 4),
            time = Time(2, 42, 55, millisecond = 500)
        )
        assertEquals(
            "2020-01-04T02:42:55,50",
            badUtc.format(ISO8601.IsoDateTimeFormat("YYYYMMDDThhmmss,ss", "YYYY-MM-DDThh:mm:ss,ss"))
        )
    }

    @Test
    fun testIssue102() {
        val time = 15.hours + 30.minutes + 12.seconds + 160.milliseconds

        assertEquals("15:30:12", ISO8601.IsoTimeFormat("hhmmss", "hh:mm:ss").format(time))
        assertEquals("153012", ISO8601.IsoTimeFormat("hhmmss", "hh:mm:ss").basic.format(time))

        assertEquals("15:30:12.2", ISO8601.IsoTimeFormat("hhmmss.s", "hh:mm:ss.s").format(time))
        assertEquals("15:30:12,2", ISO8601.IsoTimeFormat("hhmmss,s", "hh:mm:ss,s").format(time))
        assertEquals("15:30:12.16", ISO8601.IsoTimeFormat("hhmmss.ss", "hh:mm:ss.ss").format(time))
        assertEquals("15:30:12,16", ISO8601.IsoTimeFormat("hhmmss,ss", "hh:mm:ss,ss").format(time))
        assertEquals("15:30:12.160", ISO8601.IsoTimeFormat("hhmmss.sss", "hh:mm:ss.sss").format(time))
        assertEquals("15:30:12,160", ISO8601.IsoTimeFormat("hhmmss,sss", "hh:mm:ss,sss").format(time))

        assertEquals("15:30.2", ISO8601.IsoTimeFormat("hhmm.m", "hh:mm.m").format(time))
        assertEquals("15:30,2", ISO8601.IsoTimeFormat("hhmm,m", "hh:mm,m").format(time))
        assertEquals("15:30.20", ISO8601.IsoTimeFormat("hhmm.mm", "hh:mm.mm").format(time))
        assertEquals("15:30,20", ISO8601.IsoTimeFormat("hhmm,mm", "hh:mm,mm").format(time))

        assertEquals("15.5", ISO8601.IsoTimeFormat("hh.h", "hh.h").format(time))
        assertEquals("15,5", ISO8601.IsoTimeFormat("hh,h", "hh,h").format(time))
        assertEquals("15.50", ISO8601.IsoTimeFormat("hh.hh", "hh.hh").format(time))
        assertEquals("15,50", ISO8601.IsoTimeFormat("hh,hh", "hh,hh").format(time))

        assertEquals("15,5Z", ISO8601.IsoTimeFormat("hh,hZ", null).format(time))

        assertEquals(time, ISO8601.IsoTimeFormat("hhmmss,ss", "hh:mm:ss,ss").parse("15:30:12,16"))
        assertEquals(time, ISO8601.IsoTimeFormat("hhmmss.sss", "hh:mm:ss.sss").parse("15:30:12.160"))
    }
}
