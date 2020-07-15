package com.soywiz.klock

import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeRangeTest {
    val format = ISO8601.DATETIME_COMPLETE
    fun String.date() = format.parseUtc(this)
    fun range(from: Int, to: Int) = (DateTime.EPOCH + from.milliseconds)..(DateTime.EPOCH + to.milliseconds)

    @Test
    fun test() {
        val range1 = "2019-09-17T13:53:31".date() until "2019-10-17T07:00:00".date()
        val range2 = "2019-09-17T14:53:31".date() until "2019-10-17T08:00:00".date()
        val range3 = "2019-10-19T00:00:00".date() until "2019-10-20T00:00:00".date()
        assertEquals("2019-09-17T14:53:31..2019-10-17T07:00:00", range1.intersectionWith(range2)?.toString(format))
        assertEquals(null, range1.intersectionWith(range3)?.toString(format))
    }

    @Test
    fun test2() {
        assertEquals("[]", DateTimeRangeSet.toStringLongs(range(0, 100).without(range(0, 100))))
        assertEquals("[50..100]", DateTimeRangeSet.toStringLongs(range(0, 100).without(range(-50, 50))))
        assertEquals("[0..100]", DateTimeRangeSet.toStringLongs(range(0, 100).without(range(-50, -10))))
        assertEquals("[0..100]", DateTimeRangeSet.toStringLongs(range(0, 100).without(range(-50, 0))))
        assertEquals("[0..20, 70..100]", DateTimeRangeSet.toStringLongs(range(0, 100).without(range(20, 70))))
        assertEquals("[0..50]", DateTimeRangeSet.toStringLongs(range(0, 100).without(range(50, 100))))
        assertEquals("[0..50]", DateTimeRangeSet.toStringLongs(range(0, 100).without(range(50, 120))))
        assertEquals("[0..100]", DateTimeRangeSet.toStringLongs(range(0, 100).without(range(100, 120))))
        assertEquals("[0..100]", DateTimeRangeSet.toStringLongs(range(0, 100).without(range(120, 200))))
    }

	@Test
	fun testContainsRange() {
		assertEquals(true, range(0, 100) in range(0, 100))
		assertEquals(true, range(20, 80) in range(0, 100))
		assertEquals(true, range(80, 100) in range(0, 100))

		assertEquals(false, range(-50, -20) in range(0, 100))
		assertEquals(false, range(-10, 110) in range(0, 100))
		assertEquals(false, range(80, 101) in range(0, 100))
		assertEquals(false, range(-50, 0) in range(0, 100))
	}

	@Test
	fun testTest() {
		val range = DateTimeRange(Date(2019, Month.September, 18), Time(8), Time(13))
		assertEquals("2019-09-18T08:00:00..2019-09-18T13:00:00", range.toString(ISO8601.DATETIME_COMPLETE.extended))
	}

    @Test
    fun testOptionalPatterns() {
        val format = PatternDateFormat("YYYY[-MM[-dd]]").withOptional()
        assertEquals("2019-01-01", format.parse("2019").toString(format))
        assertEquals("2019-09-01", format.parse("2019-09").toString(format))
        assertEquals("2019-09-03", format.parse("2019-09-03").toString(format))
    }
}
