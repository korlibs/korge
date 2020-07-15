package com.soywiz.klock

import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinNativeImmutabilityTest {
    @Test
    fun testImmutabilityDateFormat() {
        assertEquals("2019-09-01", myDateFormat1a.let { format -> format.parse("2019-09-01").toString(format) })
        assertEquals("2019-09-01", myDateFormat2a.let { format -> format.parse("2019-09-01").toString(format) })
        assertEquals("2019-09-01", myDateFormat1b.let { format -> format.parse("2019-09-01").toString(format) })
        assertEquals("2019-09-01", myDateFormat2b.let { format -> format.parse("2019-09-01").toString(format) })
        assertEquals("2019-09-01", DateFormat.FORMAT_DATE.let { format -> format.parse("2019-09-01").toString(format) })
    }

    @Test
    fun testImmutabilityDateTimeRange() {
        assertEquals("0.1s", myDateTimeRange1a.span.toString())
        assertEquals("0.1s", myDateTimeRange1b.span.toString())
    }

    @Test
    fun testImmutabilityKlockLocale() {
        assertEquals(12, myKlockLocale1a.monthsShort.size)
        assertEquals(7, myKlockLocale1a.daysOfWeekShort.size)
        assertEquals(12, myKlockLocale1b.monthsShort.size)
        assertEquals(7, myKlockLocale1b.daysOfWeekShort.size)
    }

    companion object {
        private val myDateFormat1b = DateFormat("YYYY-MM-dd")
        private val myDateFormat2b = DateFormat("YYYY[-MM[-dd]]").withOptional()
        private val myDateTimeRange1b = DateTimeRange(DateTime(0L), DateTime(100L))
        private val myKlockLocale1b = KlockLocale.English()
    }
}

private val myDateFormat1a = DateFormat("YYYY-MM-dd")
private val myDateFormat2a = DateFormat("YYYY[-MM[-dd]]").withOptional()

private val myDateTimeRange1a = DateTimeRange(DateTime(0L), DateTime(100L))
private val myKlockLocale1a = KlockLocale.English()
