package com.soywiz.klock

import kotlin.test.*

class YearMonthTest {
    @Test
    fun testPacking() {
        val y1 = Year(1)
        val y2 = Year(2018)
        val y3 = Year(9999)

        val m1 = Month.January
        val m2 = Month.June
        val m3 = Month.December

        val ym1 = YearMonth(y1, m1)
        val ym2 = YearMonth(y2, m2)
        val ym3 = YearMonth(y3, m3)

        assertEquals(y1, ym1.year)
        assertEquals(y2, ym2.year)
        assertEquals(y3, ym3.year)

        assertEquals(m1, ym1.month)
        assertEquals(m2, ym2.month)
        assertEquals(m3, ym3.month)
    }

    @Test
    fun test2() {
        assertEquals(YearMonth(2019, Month.January), (YearMonth(2018, Month.December) + 1.months))
        assertEquals(YearMonth(2019, Month.December), (YearMonth(2018, Month.December) + 1.years))

        assertEquals(YearMonth(2018, Month.December), (YearMonth(2019, Month.January) - 1.months))
        assertEquals(YearMonth(2018, Month.December), (YearMonth(2018, Month.June) + 6.months))

        assertEquals(YearMonth(2021, Month.February), (YearMonth(2018, Month.June) + 1.years + 20.months)) // 2 years + 8 months
    }
}