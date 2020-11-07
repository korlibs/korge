package com.soywiz.klock

import kotlin.test.*

class DateTimeTest {
    val HttpDate by lazy { DateFormat("EEE, dd MMM yyyy HH:mm:ss z") }
    val HttpDate2 by lazy { DateFormat("EEE, dd MMM yyyy H:mm:ss z") }

    @Test
    fun testFromString() {
        assertEquals("Mon, 04 Dec 2017 04:35:37 UTC", DateTime.fromString("2017-12-04T04:35:37Z").toString())
    }

    @Test
    fun testFormattingToCustomDateTimeFormats() {
        val dt = DateTime(2018, 9, 8, 4, 8, 9)
        assertEquals("Sat, 08 Sep 2018 04:08:09 UTC", dt.format("EEE, dd MMM yyyy HH:mm:ss z"))
        assertEquals("Saturday, 08 Sep 2018 04:08:09 UTC", dt.format("EEEE, dd MMM yyyy HH:mm:ss z"))
        // This doesn't follow the Java's rules
        //assertEquals("S, 08 Sep 2018 04:08:09 UTC", dt.format("EEEEE, dd MMM yyyy HH:mm:ss z"))
        //assertEquals("Sa, 08 Sep 2018 04:08:09 UTC", dt.format("EEEEEE, dd MMM yyyy HH:mm:ss z"))

        assertEquals("Sat, 8 Sep 2018 04:08:09 UTC", dt.format("EEE, d MMM yyyy HH:mm:ss z"))

        assertEquals("Sat, 08 9 2018 04:08:09 UTC", dt.format("EEE, dd M yyyy HH:mm:ss z"))
        assertEquals("Sat, 08 09 2018 04:08:09 UTC", dt.format("EEE, dd MM yyyy HH:mm:ss z"))
        assertEquals("Sat, 08 September 2018 04:08:09 UTC", dt.format("EEE, dd MMMM yyyy HH:mm:ss z"))
        assertEquals("Sat, 08 S 2018 04:08:09 UTC", dt.format("EEE, dd MMMMM yyyy HH:mm:ss z"))

        assertEquals("Sat, 08 Sep 2018 04:08:09 UTC", dt.format("EEE, dd MMM y HH:mm:ss z"))
        assertEquals("Sat, 08 Sep 18 04:08:09 UTC", dt.format("EEE, dd MMM yy HH:mm:ss z"))
        assertEquals("Sat, 08 Sep 018 04:08:09 UTC", dt.format("EEE, dd MMM yyy HH:mm:ss z"))
        assertEquals("Sat, 08 Sep 2018 04:08:09 UTC", dt.format("EEE, dd MMM yyyy HH:mm:ss z"))

        assertEquals("Sat, 08 Sep 2018 04:08:09 UTC", dt.format("EEE, dd MMM YYYY HH:mm:ss z"))

        assertEquals("Sat, 08 Sep 2018 4:08:09 UTC", dt.format("EEE, dd MMM yyyy H:mm:ss z"))

        assertEquals("Sat, 08 Sep 2018 4:08:09 am UTC", dt.format("EEE, dd MMM yyyy h:mm:ss a z"))
        assertEquals("Sat, 08 Sep 2018 04:08:09 am UTC", dt.format("EEE, dd MMM yyyy hh:mm:ss a z"))

        assertEquals("Sat, 08 Sep 2018 04:8:09 UTC", dt.format("EEE, dd MMM yyyy HH:m:ss z"))

        assertEquals("Sat, 08 Sep 2018 04:08:9 UTC", dt.format("EEE, dd MMM yyyy HH:mm:s z"))
    }

    @Test
    fun testFormattingToCustomDateTimeFormatsWithMilliseconds999() {
        val dt = DateTime(2018, 9, 8, 4, 8, 9, 999)
        assertEquals("Sat, 08 Sep 2018 04:08:9.9 UTC", dt.format("EEE, dd MMM yyyy HH:mm:s.S z"))
        assertEquals("Sat, 08 Sep 2018 04:08:9.99 UTC", dt.format("EEE, dd MMM yyyy HH:mm:s.SS z"))
        assertEquals("Sat, 08 Sep 2018 04:08:9.999 UTC", dt.format("EEE, dd MMM yyyy HH:mm:s.SSS z"))
        assertEquals("Sat, 08 Sep 2018 04:08:9.9990 UTC", dt.format("EEE, dd MMM yyyy HH:mm:s.SSSS z"))
        assertEquals("Sat, 08 Sep 2018 04:08:9.99900 UTC", dt.format("EEE, dd MMM yyyy HH:mm:s.SSSSS z"))
        assertEquals("Sat, 08 Sep 2018 04:08:9.999000 UTC", dt.format("EEE, dd MMM yyyy HH:mm:s.SSSSSS z"))
    }

    @Test
    fun testFormattingToCustomDateTimeFormatsWithMilliseconds009() {
        val dt = DateTime(2018, 9, 8, 4, 8, 9, 9)
        assertEquals("Sat, 08 Sep 2018 04:08:9.0 UTC", dt.format("EEE, dd MMM yyyy HH:mm:s.S z"))
        assertEquals("Sat, 08 Sep 2018 04:08:9.00 UTC", dt.format("EEE, dd MMM yyyy HH:mm:s.SS z"))
        assertEquals("Sat, 08 Sep 2018 04:08:9.009 UTC", dt.format("EEE, dd MMM yyyy HH:mm:s.SSS z"))
        assertEquals("Sat, 08 Sep 2018 04:08:9.0090 UTC", dt.format("EEE, dd MMM yyyy HH:mm:s.SSSS z"))
        assertEquals("Sat, 08 Sep 2018 04:08:9.00900 UTC", dt.format("EEE, dd MMM yyyy HH:mm:s.SSSSS z"))
        assertEquals("Sat, 08 Sep 2018 04:08:9.009000 UTC", dt.format("EEE, dd MMM yyyy HH:mm:s.SSSSSS z"))
    }

    @Test
    fun testParsingDateTimesInCustomStringFormats() {
        val dtmilli = 1536379689000L
        assertEquals(dtmilli, DateTime(2018, 9, 8, 4, 8, 9).unixMillisLong)
        assertEquals(
            message = "Sat, 08 Sep 2018 04:08:09 UTC",
            expected = dtmilli,
            actual = DateFormat("EEE, dd MMM yyyy HH:mm:ss z").parseLong("Sat, 08 Sep 2018 04:08:09 UTC")
        )
        assertEquals(
            message = "Saturday, 08 Sep 2018 04:08:09 UTC",
            expected = dtmilli,
            actual = DateFormat("EEEE, dd MMM yyyy HH:mm:ss z").parseLong("Saturday, 08 Sep 2018 04:08:09 UTC")
        )
        assertEquals(
            message = "S, 08 Sep 2018 04:08:09 UTC",
            expected = dtmilli,
            actual = DateFormat("EEEEE, dd MMM yyyy HH:mm:ss z").parseLong("S, 08 Sep 2018 04:08:09 UTC")
        )
        assertEquals(
            message = "Sa, 08 Sep 2018 04:08:09 UTC",
            expected = dtmilli,
            actual = DateFormat("EEEEEE, dd MMM yyyy HH:mm:ss z").parseLong("Sa, 08 Sep 2018 04:08:09 UTC")
        )

        assertEquals(
            message = "Sat, 8 Sep 2018 04:08:09 UTC",
            expected = dtmilli,
            actual = DateFormat("EEE, d MMM yyyy HH:mm:ss z").parseLong("Sat, 8 Sep 2018 04:08:09 UTC")
        )

        assertEquals(
            message = "Sat, 08 9 2018 04:08:09 UTC",
            expected = dtmilli,
            actual = DateFormat("EEE, dd M yyyy HH:mm:ss z").parseLong("Sat, 08 9 2018 04:08:09 UTC")
        )
        assertEquals(
            message = "Sat, 08 09 2018 04:08:09 UTC",
            expected = dtmilli,
            actual = DateFormat("EEE, dd MM yyyy HH:mm:ss z").parseLong("Sat, 08 09 2018 04:08:09 UTC")
        )
        assertEquals(
            message = "Sat, 08 September 2018 04:08:09 UTC",
            expected = dtmilli,
            actual = DateFormat("EEE, dd MMMM yyyy HH:mm:ss z").parseLong("Sat, 08 September 2018 04:08:09 UTC")
        )
        assertEquals(
            message = "Sat, 08 S 2018 04:08:09 UTC",
            expected = null,
            actual = DateFormat("EEE, dd MMMMM yyyy HH:mm:ss z").parseDoubleOrNull("Sat, 08 S 2018 04:08:09 UTC")
        )

        assertEquals(
            message = "Sat, 08 Sep 2018 04:08:09 UTC - y",
            expected = dtmilli,
            actual = DateFormat("EEE, dd MMM y HH:mm:ss z").parseLong("Sat, 08 Sep 2018 04:08:09 UTC")
        )
        assertEquals(
            message = "Sat, 08 Sep 18 04:08:09 UTC - yy",
            expected = null,
            actual = DateFormat("EEE, dd MMM yy HH:mm:ss z").parseDoubleOrNull("Sat, 08 Sep 18 04:08:09 UTC")
        )
        assertEquals(
            message = "Sat, 08 Sep 018 04:08:09 UTC - yyy",
            expected = dtmilli,
            actual = DateFormat("EEE, dd MMM yyy HH:mm:ss z").parseLong("Sat, 08 Sep 018 04:08:09 UTC")
        )


        assertEquals(
            message = "Sat, 08 Sep 2018 04:08:09 UTC - YYYY",
            expected = dtmilli,
            actual = DateFormat("EEE, dd MMM YYYY HH:mm:ss z").parseLong("Sat, 08 Sep 2018 04:08:09 UTC")
        )

        assertEquals(
            message = "Sat, 08 Sep 2018 4:08:09 UTC",
            expected = dtmilli,
            actual = DateFormat("EEE, dd MMM yyyy H:mm:ss z").parseLong("Sat, 08 Sep 2018 4:08:09 UTC")
        )

        assertEquals(
            message = "Sat, 08 Sep 2018 04:08:09 am UTC",
            expected = dtmilli,
            actual = DateFormat("EEE, dd MMM yyyy HH:m:ss z").parseLong("Sat, 08 Sep 2018 04:8:09 UTC")
        )

        assertEquals(
            message = "Sat, 08 Sep 2018 04:08:9 UTC",
            expected = dtmilli,
            actual = DateFormat("EEE, dd MMM yyyy HH:mm:s z").parseLong("Sat, 08 Sep 2018 04:08:9 UTC")
        )
    }

	@Test
	fun testNewParserBug1() {
		DateFormat("EEE, dd MMMM yyyy HH:mm:ss z").parseLong("Sat, 08 September 2018 04:08:09 UTC")
	}

    @Test
    fun testParsingDateTimesInCustomStringFormatsWithAmPm() {
        val amDtmilli = 1536379689000L
        assertEquals(amDtmilli, DateTime(2018, 9, 8, 4, 8, 9).unixMillisLong)

        val pmDtmilli = 1536422889000L
        assertEquals(pmDtmilli, DateTime(2018, 9, 8, 16, 8, 9).unixMillisLong)

        assertEquals(
            message = "Sat, 08 Sep 2018 4:08:09 am UTC",
            expected = amDtmilli,
            actual = DateFormat("EEE, dd MMM yyyy h:mm:ss a z").parseLong("Sat, 08 Sep 2018 4:08:09 am UTC")
        )
        assertEquals(
            message = "Sat, 08 Sep 2018 04:08:09 am UTC",
            expected = amDtmilli,
            actual = DateFormat("EEE, dd MMM yyyy hh:mm:ss a z").parseLong("Sat, 08 Sep 2018 04:08:09 am UTC")
        )

        assertEquals(
            message = "Sat, 08 Sep 2018 4:08:09 pm UTC",
            expected = pmDtmilli,
            actual = DateFormat("EEE, dd MMM yyyy h:mm:ss a z").parseLong("Sat, 08 Sep 2018 4:08:09 pm UTC")
        )
        assertEquals(
            message = "Sat, 08 Sep 2018 04:08:09 pm UTC",
            expected = pmDtmilli,
            actual = DateFormat("EEE, dd MMM yyyy hh:mm:ss a z").parseLong("Sat, 08 Sep 2018 04:08:09 pm UTC")
        )
    }

    @Test
    fun testParsingDateTimesWithPmMixedWith24Hourformat() {
        val pmDtmilli = 1536422889000L
        assertEquals(pmDtmilli, DateTime(2018, 9, 8, 16, 8, 9).unixMillisLong)

        assertEquals(
            message = "Sat, 08 Sep 2018 4:08:09 pm UTC",
            expected = pmDtmilli,
            actual = DateFormat("EEE, dd MMM yyyy H:mm:ss a z").parseLong("Sat, 08 Sep 2018 16:08:09 pm UTC")
        )
        assertEquals(
            message = "Sat, 08 Sep 2018 04:08:09 pm UTC",
            expected = pmDtmilli,
            actual = DateFormat("EEE, dd MMM yyyy HH:mm:ss a z").parseLong("Sat, 08 Sep 2018 16:08:09 pm UTC")
        )
    }

    @Test
    fun testParsingDateTimesWithDeciSeconds() {
        var dtmilli = 1536379689009L
        assertEquals(dtmilli, DateTime(2018, 9, 8, 4, 8, 9, 9).unixMillisLong)
        assertEquals(
            message = "Sat, 08 Sep 2018 04:08:09.9 UTC",
            expected = dtmilli,
            actual = DateFormat("EEE, dd MMM yyyy HH:mm:ss.S z").parseLong("Sat, 08 Sep 2018 04:08:09.9 UTC")
        )
    }

    @Test
    fun testParsingDateTimesWithCentiSeconds() {
        var dtmilli = 1536379689099L
        assertEquals(dtmilli, DateTime(2018, 9, 8, 4, 8, 9, 99).unixMillisLong)
        assertEquals(
            message = "Sat, 08 Sep 2018 04:08:09.99 UTC",
            expected = dtmilli,
            actual = DateFormat("EEE, dd MMM yyyy HH:mm:ss.SS z").parseLong("Sat, 08 Sep 2018 04:08:09.99 UTC")
        )
    }

    @Test
    fun testParsingDateTimesWithMilliseconds() {
        val dtmilli = 1536379689999L
        assertEquals(dtmilli, DateTime(2018, 9, 8, 4, 8, 9, 999).unixMillisLong)
        assertEquals(
            message = "Sat, 08 Sep 2018 04:08:09.999 UTC",
            expected = dtmilli,
            actual = DateFormat("EEE, dd MMM yyyy HH:mm:ss.SSS z").parseLong("Sat, 08 Sep 2018 04:08:09.999 UTC")
        )
    }

    @Test
    fun testParsingDateTimesWithGreaterPrecisionThanMillisecond() {
        val dtmilli = 1536379689999L
        assertEquals(dtmilli, DateTime(2018, 9, 8, 4, 8, 9, 999).unixMillisLong)
        assertEquals(
            message = "Sat, 08 Sep 2018 04:08:09.9999 UTC",
            expected = dtmilli,
            actual = DateFormat("EEE, dd MMM yyyy HH:mm:ss.SSSS z").parseLong("Sat, 08 Sep 2018 04:08:09.9999 UTC")
        )
        assertEquals(
            message = "Sat, 08 Sep 2018 04:08:09.99999 UTC",
            expected = dtmilli,
            actual = DateFormat("EEE, dd MMM yyyy HH:mm:ss.SSSSS z").parseLong("Sat, 08 Sep 2018 04:08:09.99999 UTC")
        )
        assertEquals(
            message = "Sat, 08 Sep 2018 04:08:09.999999 UTC",
            expected = dtmilli,
            actual = DateFormat("EEE, dd MMM yyyy HH:mm:ss.SSSSSS z").parseLong("Sat, 08 Sep 2018 04:08:09.999999 UTC")
        )
    }


    @Test
    fun testParse() {
        assertEquals("Mon, 18 Sep 2017 04:58:45 UTC", HttpDate.format(1505710725916L))
    }

    @Test
    fun testReverseParse() {
        val STR = "Tue, 19 Sep 2017 00:58:45 UTC"
        assertEquals(STR, HttpDate.format(HttpDate.parse(STR)))
    }

    @Test
    fun testCheckedCreation() {
        assertEquals("Mon, 18 Sep 2017 23:58:45 UTC", HttpDate.format(DateTime(2017, 9, 18, 23, 58, 45)))
    }

    @Test
    fun testCreatedAdjusted() {
        assertEquals(
            "Thu, 18 Jan 2018 23:58:45 UTC",
            HttpDate.format(DateTime.createAdjusted(2017, 13, 18, 23, 58, 45))
        )
        assertEquals("Mon, 18 Sep 2017 23:58:45 UTC", HttpDate.format(DateTime.createAdjusted(2017, 9, 18, 23, 58, 45)))
        assertEquals(
            "Mon, 01 Jan 2018 00:00:01 UTC",
            HttpDate.format(DateTime.createAdjusted(2017, 12, 31, 23, 59, 61))
        )
        assertEquals(
            "Thu, 21 Mar 2024 19:32:20 UTC",
            HttpDate.format(DateTime.createAdjusted(2017, 12, 31, 23, 59, 200_000_000))
        )
    }

    @Test
    fun testCreatedClamped() {
        assertEquals("Mon, 18 Sep 2017 23:58:45 UTC", HttpDate.format(DateTime.createClamped(2017, 9, 18, 23, 58, 45)))
        assertEquals("Mon, 18 Dec 2017 23:58:45 UTC", HttpDate.format(DateTime.createClamped(2017, 13, 18, 23, 58, 45)))
    }

    @Test
    fun testSpecial() {
        assertEquals("Mon, 01 Jan 0001 00:00:00 UTC", HttpDate.format(DateTime.createClamped(1, 1, 1, 0, 0, 0, 0)))
    }

    @Test
    fun testBaseAdjust() {
        val date = DateTime(Year(2018), Month.November, 4, 5, 54, 30)

        assertEquals("Sun, 04 Nov 2018 05:54:30 GMT+0100", date.toOffsetUnadjusted((+60).minutes).toString())
        assertEquals("Sun, 04 Nov 2018 06:54:30 GMT+0100", date.toOffset((+60).minutes).toString())
    }

    @Test
    fun testMinMaxClamp() {
        val a = DateTime(Year(2018), Month.November, 4, 5, 54, 30)
        val b = DateTime(Year(2018), Month.November, 4, 6, 54, 30)
        val c = DateTime(Year(2018), Month.November, 4, 7, 54, 30)
        assertEquals(a, min(a, b))
        assertEquals(b, max(a, b))
        assertEquals(b, a.clamp(b, c))
        assertEquals(b, b.clamp(a, c))
        assertEquals(a, a.clamp(a, c))
        assertEquals(c, c.clamp(a, c))
    }

	@Test
	fun testStartEndDay() {
		val date = DateTime(1568803601377)
		val start = date.dateDayStart
		val end = date.dateDayEnd
		assertEquals("2019-09-18T00:00:00", ISO8601.DATETIME_COMPLETE.extended.format(start))
		assertEquals("2019-09-18T23:59:59", ISO8601.DATETIME_COMPLETE.extended.format(end))
		assertEquals(1568764800000L, start.unixMillisLong)
		assertEquals(1568851199999L, end.unixMillisLong)
	}

	@Test
	fun testTimeZones() {
		"Tue, 19 Sep 2017 00:58:45 GMT-0800".let { STR -> assertEquals(STR, HttpDate.parse(STR).toString()) }
		"Tue, 19 Sep 2017 00:58:45 GMT+0800".let { STR -> assertEquals(STR, HttpDate.parse(STR).toString()) }
	}

	@Test
	fun testBug37() {
		val format = DateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
		format.parse("2019-04-15T17:28:46.862+0900")
	}

	@Test
	fun testBug33() {
		assertEquals("20190412", DateTime(2019, 4, 12).localUnadjusted.format("yyyyMMdd"))
		assertEquals("2019年04月12日", Date(2019, 4, 12).format("yyyy年MM月dd日"))
		assertEquals("2019年04月12日", Date(2019, 4, 12).format("yyyy'年'MM'月'dd'日'"))
	}

    @Test
    fun testBug93() {
        // 2020-03-20 11:13:31.317 +05:00
        val dateTime = DateTime(
            year = 2020, month = 3, day = 20,
            hour = 11, minute = 13, second = 31, milliseconds = 317
        )
        val original = DateTimeTz.local(dateTime, TimezoneOffset(5.hours))

        val formatter1 = DateFormat("yyyy-MM-dd HH:mm:ss.SSS X")
        val string1 = formatter1.format(original)
        assertEquals("2020-03-20 11:13:31.317 +05", string1)
        assertEquals(original, formatter1.parse(string1))

        val formatter2 = DateFormat("yyyy-MM-dd HH:mm:ss.SSS XX")
        val string2 = formatter2.format(original)
        assertEquals("2020-03-20 11:13:31.317 +0500", string2)
        assertEquals(original, formatter2.parse(string2))

        val formatter3 = DateFormat("yyyy-MM-dd HH:mm:ss.SSS XXX")
        val string3 = formatter3.format(original)
        assertEquals("2020-03-20 11:13:31.317 +05:00", string3)
        assertEquals(original, formatter3.parse(string3))

        val formatter4 = DateFormat("yyyy-MM-dd HH:mm:ss.SSS x")
        val string4 = formatter4.format(original)
        assertEquals("2020-03-20 11:13:31.317 +05", string4)
        assertEquals(original, formatter4.parse(string4))

        val formatter5 = DateFormat("yyyy-MM-dd HH:mm:ss.SSS xx")
        val string5 = formatter5.format(original)
        assertEquals("2020-03-20 11:13:31.317 +0500", string5)
        assertEquals(original, formatter5.parse(string5))

        val formatter6 = DateFormat("yyyy-MM-dd HH:mm:ss.SSS xxx")
        val string6 = formatter6.format(original)
        assertEquals("2020-03-20 11:13:31.317 +05:00", string6)
        assertEquals(original, formatter6.parse(string6))
    }

    @Test
    fun testBug103() {
        assertEquals("Fri, 15 Oct -0249 19:33:20 UTC", DateTime.fromUnix(-70000000000000L).toString())
        assertEquals("Mon, 01 Jan 0001 00:00:00 UTC", DateTime.fromUnix(-62135596800000L).toString())
        assertEquals("Sat, 11 Aug -0027 08:00:00 UTC", DateTime.fromUnix(-63000000000000L).toString())
        assertEquals("Sun, 31 Dec 0000 23:59:59 UTC", DateTime.fromUnix(-62135596800000L - 1L).toString())
    }

    @Test
    fun testBug123() {
        val str1 = "1989-01-01T10:00:00Z"
        val str2 = "1989-01-01T10:00:00.000Z"
        assertEquals(str1, DateTime.parse(str1).format(DateFormat.FORMAT1))
        assertEquals(str2, DateTime.parse(str2).format(DateFormat.FORMAT2))
    }

    @Test
    fun testIssue131() {
        assertEquals(
            "2020-07-23T12:30:52.999000000Z",
            DateTime(1595507452999L).format("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSZ")
        )
    }

    @Test
    fun testDaySuffix() {
        // 2020-03-21 11:13:31.317 +05:00
        val dateTime = DateTime(
            year = 2020, month = 3, day = 21,
            hour = 11, minute = 13, second = 31, milliseconds = 317
        )
        val original = DateTimeTz.local(dateTime, TimezoneOffset(5.hours))

        val formatter = DateFormat("yyyy-MM-do HH:mm:ss.SSS xxx")
        val string = formatter.format(original)
        assertEquals("2020-03-21st 11:13:31.317 +05:00", string)
        assertEquals(original, formatter.parse(string))

        // 2020-03-22 11:13:31.317 +05:00
        val dateTime2 = DateTime(
            year = 2020, month = 3, day = 22,
            hour = 11, minute = 13, second = 31, milliseconds = 317
        )
        val original2 = DateTimeTz.local(dateTime2, TimezoneOffset(5.hours))

        val formatter2 = DateFormat("yyyy-MM-do HH:mm:ss.SSS xxx")
        val string2 = formatter2.format(original2)
        assertEquals("2020-03-22nd 11:13:31.317 +05:00", string2)
        assertEquals(original2, formatter2.parse(string2))


        // 2020-03-20 11:13:31.317 +05:00
        val dateTime3 = DateTime(
            year = 2020, month = 3, day = 20,
            hour = 11, minute = 13, second = 31, milliseconds = 317
        )
        val original3 = DateTimeTz.local(dateTime3, TimezoneOffset(5.hours))

        val formatter3 = DateFormat("yyyy-MM-do HH:mm:ss.SSS xxx")
        val string3 = formatter3.format(original3)
        assertEquals("2020-03-20th 11:13:31.317 +05:00", string3)
        assertEquals(original3, formatter3.parse(string3))
    }
}
