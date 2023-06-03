package korlibs.time

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class DateFormatTest {
    @Test
    fun test() {
        assertEquals("Sat, 03 Dec 2011 10:15:30 GMT+0100", DateFormat.FORMAT1.parse("2011-12-03T10:15:30+01:00").toStringDefault())
        assertEquals("Sat, 03 Dec 2011 10:15:30 GMT+0100", DateFormat.FORMAT1.parse("2011-12-03T10:15:30+0100").toStringDefault())
    }

    @Test
    fun testParseIsoFormat() {
        val iso8601JsonFormat = PatternDateFormat("yyyy-MM-ddTHH:mm:ss.SZ")

        assertEquals("2020-01-01T13:12:30.001Z", iso8601JsonFormat.parse("2020-01-01T13:12:30.1Z").toString(DateFormat.FORMAT2))
        assertEquals("2020-01-01T13:12:30.001Z", iso8601JsonFormat.parse("2020-01-01T13:12:30.01Z").toString(DateFormat.FORMAT2))
        assertEquals("2020-01-01T13:12:30.001Z", iso8601JsonFormat.parse("2020-01-01T13:12:30.001Z").toString(DateFormat.FORMAT2))
        assertEquals("2020-01-01T13:12:30.001Z", iso8601JsonFormat.parse("2020-01-01T13:12:30.0001Z").toString(DateFormat.FORMAT2))
        assertEquals("2020-01-01T13:12:30.001Z", iso8601JsonFormat.parse("2020-01-01T13:12:30.00001Z").toString(DateFormat.FORMAT2))
        assertEquals("2020-01-01T13:12:30.001Z", iso8601JsonFormat.parse("2020-01-01T13:12:30.000001Z").toString(DateFormat.FORMAT2))

        // Fails with "S" -> """(\d{1,6})""", now succeeds with (\d{1,9})
        assertEquals("2020-01-01T13:12:30.001Z", iso8601JsonFormat.parse("2020-01-01T13:12:30.0000001Z").toString(DateFormat.FORMAT2)) //7S
        assertEquals("2020-01-01T13:12:30.001Z", iso8601JsonFormat.parse("2020-01-01T13:12:30.00000001Z").toString(DateFormat.FORMAT2)) //8S
        assertEquals("2020-01-01T13:12:30.001Z", iso8601JsonFormat.parse("2020-01-01T13:12:30.000000001Z").toString(DateFormat.FORMAT2)) //9S

        //assertFailsWith<DateException> { iso8601JsonFormat.parse("2020-01-01T13:12:30.0000000001Z").toString(DateFormat.FORMAT2) } //10S

        // Negative tests
        assertNotEquals("2020-01-01T13:12:30.001Z", iso8601JsonFormat.parse("2020-01-01T13:12:30.10Z").toString(DateFormat.FORMAT2))
        assertNotEquals("2020-01-01T13:12:30.001Z", iso8601JsonFormat.parse("2020-01-01T13:12:30.100Z").toString(DateFormat.FORMAT2))
        assertNotEquals("2020-01-01T13:12:30.001Z", iso8601JsonFormat.parse("2020-01-01T13:12:30.1000Z").toString(DateFormat.FORMAT2))
        assertNotEquals("2020-01-01T13:12:30.001Z", iso8601JsonFormat.parse("2020-01-01T13:12:30.10000Z").toString(DateFormat.FORMAT2))
        assertNotEquals("2020-01-01T13:12:30.001Z", iso8601JsonFormat.parse("2020-01-01T13:12:30.100000Z").toString(DateFormat.FORMAT2))
        assertNotEquals("2020-01-01T13:12:30.001Z", iso8601JsonFormat.parse("2020-01-01T13:12:30.1000000Z").toString(DateFormat.FORMAT2)) //7S
        assertNotEquals("2020-01-01T13:12:30.001Z", iso8601JsonFormat.parse("2020-01-01T13:12:30.10000000Z").toString(DateFormat.FORMAT2)) //8S
        assertNotEquals("2020-01-01T13:12:30.001Z", iso8601JsonFormat.parse("2020-01-01T13:12:30.100000000Z").toString(DateFormat.FORMAT2)) //9S

        //assertFailsWith<DateException> { iso8601JsonFormat.parse("2020-01-01T13:12:30.1000000000Z").toString(DateFormat.FORMAT2) } //10S
    }

    @Test
    fun testTimezoneBug670() {
        val expected = TimezoneOffset(9.hours)
        val actual = DateFormat("z").parse("+09:00").offset
        assertEquals(expected, actual)
    }
}
