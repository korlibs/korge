package korlibs.time

import kotlin.test.*

class SimplerDateFormatTest {
    // Sun, 06 Nov 1994 08:49:37 GMT
    val format = DateFormat("EEE, dd MMM yyyy HH:mm:ss z")

    @Test
    fun testParse() {
        assertEquals(784111777000, format.parseLong("Sun, 06 Nov 1994 08:49:37 UTC"))
    }

    @Test
    fun testFormat() {
        assertEquals("Sun, 06 Nov 1994 08:49:37 UTC", format.format(784111777000))
    }

    @Test
    fun testParseFormat() {
        val dateStr = "Sun, 06 Nov 1994 08:49:37 UTC"
        assertEquals(dateStr, format.format(format.parseDouble(dateStr)))
    }

    @Test
    fun testBug67() {
        assertEquals(31, DateTime(2000, 12, 31).dayOfMonth)
        //assertEquals(31, DateFormat("yyyy-MM-dd'T'HH:mm:ss").parse("2000-12-31T00:00:00").dayOfMonth)
    }

    @Test
    fun testDateFormatMillisecondPrecisionIssue2197() {
        _testDateFormatMillisecondPrecisionIssue2197(DateFormat("yyyy-MM-dd'T'HH:mm[:ss[.S*]]Z").withOptional())
    }

    @Test
    fun testDateFormatMillisecondPrecisionIssue2197_2() {
        _testDateFormatMillisecondPrecisionIssue2197(DateFormat.ISO_DATE_TIME_OFFSET)
    }

    @Test
    fun testDateFormatMillisecondPrecisionIssue2197_3() {
        _testDateFormatMillisecondPrecisionIssue2197(ISO8601.DATETIME_UTC_COMPLETE_FRACTION)
    }

    private fun _testDateFormatMillisecondPrecisionIssue2197(format: DateFormat) {
        val date = korlibs.time.DateTime(2020, 1, 1, 13, 12, 30, 100)

        assertEquals(1, format.parseUtc("2020-01-01T13:12:30.001Z").milliseconds) //1
        assertEquals(10, format.parseUtc("2020-01-01T13:12:30.010Z").milliseconds) //10
        assertEquals(100, format.parseUtc("2020-01-01T13:12:30.100Z").milliseconds) //100
        assertEquals(100, format.parseUtc("2020-01-01T13:12:30.1Z").milliseconds) // ❌ parsed as 1
        assertEquals(100, format.parseUtc("2020-01-01T13:12:30.10Z").milliseconds) // ❌ parsed as 10
        assertEquals(100, format.parseUtc("2020-01-01T13:12:30.100Z").milliseconds) //100

        assertEquals(date, format.parseUtc("2020-01-01T13:12:30.1Z")) // ❌ parsed as 2020-01-01T13:12:30.001Z
        assertEquals(date, format.parseUtc("2020-01-01T13:12:30.10Z")) // ❌ parsed as 2020-01-01T13:12:30.010Z
        assertEquals(date, format.parseUtc("2020-01-01T13:12:30.100Z"))
        assertEquals(date, format.parseUtc("2020-01-01T13:12:30.1000Z"))
        assertEquals(date, format.parseUtc("2020-01-01T13:12:30.10000Z"))
        assertEquals(date, format.parseUtc("2020-01-01T13:12:30.100000Z"))
        assertEquals(date, format.parseUtc("2020-01-01T13:12:30.1000000Z"))
        assertEquals(date, format.parseUtc("2020-01-01T13:12:30.10000000Z")) // ❌ parsed as 2020-01-01T13:12:30.099Z
        assertEquals(date, format.parseUtc("2020-01-01T13:12:30.100000000Z"))
        // Out of precision
        assertEquals(date, format.parseUtc("2020-01-01T13:12:30.1000000000Z"))

        assertNotEquals(date, format.parseUtc("2020-01-01T13:12:30.01Z"))
        assertNotEquals(date, format.parseUtc("2020-01-01T13:12:30.001Z"))
        assertNotEquals(date, format.parseUtc("2020-01-01T13:12:30.0001Z"))
        assertNotEquals(date, format.parseUtc("2020-01-01T13:12:30.00001Z"))
        assertNotEquals(date, format.parseUtc("2020-01-01T13:12:30.000001Z"))
        assertNotEquals(date, format.parseUtc("2020-01-01T13:12:30.0000001Z"))
        assertNotEquals(date, format.parseUtc("2020-01-01T13:12:30.00000001Z"))
        assertNotEquals(date, format.parseUtc("2020-01-01T13:12:30.000000001Z"))
        // Out of precision
        assertNotEquals(date, format.parseUtc("2020-01-01T13:12:30.0000000001Z"))
    }

    class StrictOffset {
        val format = DateFormat("yyyy-MM-dd'T'HH:mm:ssxxx")

        @Test
        fun testParseUtc() {
            assertEquals(1462390174000, format.parseLong("2016-05-04T19:29:34+00:00"))
        }

        @Test
        fun testFormatUtc() {
            assertEquals("2016-05-04T19:29:34+00:00", format.format(1462390174000))
        }

        @Test
        fun testFormatWithNegativeOffset() {
            val now = DateTime.fromUnixMillis(1462390174000)
                .toOffsetUnadjusted((-3.5).hours)
            val formatted = now.format(format)
            assertEquals("2016-05-04T19:29:34-03:30", formatted)
        }

        @Test
        fun testFormatWithPositiveOffset() {
            val now = DateTime.fromUnixMillis(1462390174000)
                .toOffsetUnadjusted(4.5.hours)
            val formatted = now.format(format)
            assertEquals("2016-05-04T19:29:34+04:30", formatted)
        }

        @Test
        fun testParseFormatUtc() {
            val dateStr = "2016-05-04T19:29:34+00:00"
            assertEquals(dateStr, format.format(format.parseDouble(dateStr)))
        }

        @Test
        fun testParseWithOffsetAsUtc() {
            val offsetDateStr = "2016-05-04T19:29:34+05:00"
            val utcDateStr = "2016-05-04T14:29:34+00:00"
            assertEquals(format.parse(offsetDateStr).utc, format.parse(utcDateStr).utc)
        }

        @Test
        fun testParseWithOffset() {
            assertEquals(1462390174000, format.parseLong("2016-05-04T19:29:34-07:00"))
        }

        @Test
        fun testParseFormatOffset() {
            val dateStr = "2016-05-04T19:29:34+05:00"
            val date = format.parse(dateStr)
            //println(date.base)
            //println(date.offset)
            assertEquals(dateStr, format.format(date))
        }

        @Test
        fun testParseWithZuluFails() {
            val dateStr = "2016-05-04T19:29:34Z"
            assertFailsWith(RuntimeException::class, "Zulu Time Zone is only accepted with X-XXX formats.") {
                format.parseDouble(dateStr)
            }
        }
    }

    class ZuluCapableOffset {
        val format = DateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")

        @Test
        fun testParseUtc() {
            assertEquals(1462390174000, format.parseLong("2016-05-04T19:29:34+00:00"))
        }

        @Test
        fun testParseZulu() {
            assertEquals(1462390174000, format.parseLong("2016-05-04T19:29:34Z"))
        }

        @Test
        fun testFormatUtc() {
            assertEquals("2016-05-04T19:29:34Z", format.format(1462390174000))
        }

        @Test
        fun testParseWithUtcOffsetFormatsWithZulu() {
            val dateStr = "2016-05-04T19:29:34+00:00"
            val expectedStr = "2016-05-04T19:29:34Z"
            assertEquals(expectedStr, format.format(format.parseDouble(dateStr)))
        }

        @Test
        fun testParseWithZuluFormatsWithZulu() {
            val dateStr = "2016-05-04T19:29:34Z"
            assertEquals(dateStr, format.format(format.parseDouble(dateStr)))
        }
    }
}
