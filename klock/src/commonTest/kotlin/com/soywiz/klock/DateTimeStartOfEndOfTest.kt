package com.soywiz.klock

import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeStartOfEndOfTest {
    val date = DateTime(2019, Month.October, 27, 21, 9, 33, 500)

    @Test
    fun testQuarter() {
        assertEquals(1, DateTime(2019, Month.January, 1).quarter)
        assertEquals(1, DateTime(2019, Month.February, 1).quarter)
        assertEquals(1, DateTime(2019, Month.March, 1).quarter)

        assertEquals(2, DateTime(2019, Month.April, 1).quarter)
        assertEquals(2, DateTime(2019, Month.May, 1).quarter)
        assertEquals(2, DateTime(2019, Month.June, 1).quarter)

        assertEquals(3, DateTime(2019, Month.July, 1).quarter)
        assertEquals(3, DateTime(2019, Month.August, 1).quarter)
        assertEquals(3, DateTime(2019, Month.September, 1).quarter)

        assertEquals(4, DateTime(2019, Month.October, 1).quarter)
        assertEquals(4, DateTime(2019, Month.November, 1).quarter)
        assertEquals(4, DateTime(2019, Month.December, 1).quarter)
    }

    @Test
    fun testStartOf() {
        assertEquals("2019-01-01T00:00:00", date.startOfYear.toString(ISO8601.DATETIME_COMPLETE))
        assertEquals("2019-10-01T00:00:00", date.startOfMonth.toString(ISO8601.DATETIME_COMPLETE))
        assertEquals("2019-10-01T00:00:00", date.startOfQuarter.toString(ISO8601.DATETIME_COMPLETE))
        assertEquals("2019-10-21T00:00:00", date.startOfIsoWeek.toString(ISO8601.DATETIME_COMPLETE))
        assertEquals("2019-10-27T00:00:00", date.startOfWeek.toString(ISO8601.DATETIME_COMPLETE))
        assertEquals("2019-10-27T00:00:00", date.startOfDay.toString(ISO8601.DATETIME_COMPLETE))
        assertEquals("2019-10-27T21:00:00", date.startOfHour.toString(ISO8601.DATETIME_COMPLETE))
        assertEquals("2019-10-27T21:09:00", date.startOfMinute.toString(ISO8601.DATETIME_COMPLETE))
        assertEquals("2019-10-27T21:09:33", date.startOfSecond.toString(ISO8601.DATETIME_COMPLETE))
    }

    @Test
    fun testEndOf() {
        assertEquals("2019-12-31T23:59:59", date.endOfYear.toString(ISO8601.DATETIME_COMPLETE))
        assertEquals("2019-10-31T23:59:59", date.endOfMonth.toString(ISO8601.DATETIME_COMPLETE))
        assertEquals("2019-12-31T23:59:59", date.endOfQuarter.toString(ISO8601.DATETIME_COMPLETE))
        assertEquals("2019-10-27T23:59:59", date.endOfIsoWeek.toString(ISO8601.DATETIME_COMPLETE))
        assertEquals("2019-10-28T23:59:59", date.endOfWeek.toString(ISO8601.DATETIME_COMPLETE))
        assertEquals("2019-10-27T23:59:59", date.endOfDay.toString(ISO8601.DATETIME_COMPLETE))
        assertEquals("2019-10-27T21:59:59", date.endOfHour.toString(ISO8601.DATETIME_COMPLETE))
        assertEquals("2019-10-27T21:09:59", date.endOfMinute.toString(ISO8601.DATETIME_COMPLETE))
        assertEquals("2019-10-27T21:09:33", date.endOfSecond.toString(ISO8601.DATETIME_COMPLETE))
    }
}
