package com.soywiz.klock

import com.soywiz.klock.Month.*
import kotlin.test.*

class MonthTest {
    @Test
    fun testBasicMonthMetrics() {
        assertEquals(
            listOf(
                0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334,
                0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335
            ),
            listOf(false, true).map { leap -> (1..12).map { Month(it).daysToStart(leap = leap) } }.flatMap { it }
        )

        assertEquals(
            listOf(
                365, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365,
                366, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335, 366
            ),
            listOf(false, true).map { leap -> (0..12).map { Month(it).daysToEnd(leap = leap) } }.flatMap { it }
        )

        assertEquals(
            listOf(
                31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31,
                31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
            ),
            listOf(false, true).map { leap -> (1..12).map { Month(it).days(leap = leap) } }.flatMap { it }
        )

        assertEquals(
            listOf(January, February, March, April, May, June, July, August, September, October, November, December),
            (1..12).map { Month(it) }
        )
    }

    @Test
    fun testFromDayOfYear() {
        fun Month.Companion.fromDayOfYearSlow(day: Int, isLeap: Boolean): Month? {
            val table = MONTH_TABLE(isLeap)
            for ((month, range) in table) if (day in range) return month
            return null
        }

        for (leap in listOf(false, true)) {
            for (day in -100..Year.days(leap) + 100) {
                val month = Month.fromDayOfYear(day, leap = leap)
                val monthSure = Month.fromDayOfYearSlow(day, isLeap = leap)
                assertEquals(monthSure, month, "day=$day, monthSure=$monthSure, month=$month, leap=$leap")
            }
        }
    }

    @Test
    fun testDaysInMonth() {
        for (leap in listOf(false, true)) {
            val table = MONTH_TABLE(leap)
            for ((month, range) in table) {
                assertEquals((range.endInclusive - range.start + 1), month.days(leap))
            }
        }
    }

    val MONTH_TABLE_COMMON = mapOf(
        Month.January to 1..31,
        Month.February to 32..59,
        Month.March to 60..90,
        Month.April to 91..120,
        Month.May to 121..151,
        Month.June to 152..181,
        Month.July to 182..212,
        Month.August to 213..243,
        Month.September to 244..273,
        Month.October to 274..304,
        Month.November to 305..334,
        Month.December to 335..365
    )

    val MONTH_TABLE_LEAP = mapOf(
        Month.January to 1..31,
        Month.February to 32..60,
        Month.March to 61..91,
        Month.April to 92..121,
        Month.May to 122..152,
        Month.June to 153..182,
        Month.July to 183..213,
        Month.August to 214..244,
        Month.September to 245..274,
        Month.October to 275..305,
        Month.November to 306..335,
        Month.December to 336..366
    )

    fun MONTH_TABLE(leap: Boolean) = if (leap) MONTH_TABLE_LEAP else MONTH_TABLE_COMMON
}
