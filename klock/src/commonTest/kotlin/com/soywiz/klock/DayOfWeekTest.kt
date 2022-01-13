package com.soywiz.klock

import com.soywiz.klock.locale.*
import kotlin.test.Test
import kotlin.test.assertEquals

class DayOfWeekTest {
	@Test
	fun testFirstDayOfWeek() {
		assertEquals(DayOfWeek.Sunday, DayOfWeek.firstDayOfWeek(KlockLocale.english))
	}

	@Test
	fun testIsWeekend() {
		assertEquals(
			listOf(true, false, false, false, false, false, true),
			DayOfWeek.values().map { it.isWeekend(KlockLocale.english) }
		)
	}

    @Test
    fun testNext() {
        assertEquals(DayOfWeek.Monday, DayOfWeek.Sunday.next)
        assertEquals(DayOfWeek.Tuesday, DayOfWeek.Monday.next)
        assertEquals(DayOfWeek.Wednesday, DayOfWeek.Tuesday.next)
        assertEquals(DayOfWeek.Thursday, DayOfWeek.Wednesday.next)
        assertEquals(DayOfWeek.Friday, DayOfWeek.Thursday.next)
        assertEquals(DayOfWeek.Saturday, DayOfWeek.Friday.next)
        assertEquals(DayOfWeek.Sunday, DayOfWeek.Saturday.next)
    }

    @Test
    fun testPrev() {
        assertEquals(DayOfWeek.Saturday, DayOfWeek.Sunday.prev)
        assertEquals(DayOfWeek.Sunday, DayOfWeek.Monday.prev)
        assertEquals(DayOfWeek.Monday, DayOfWeek.Tuesday.prev)
        assertEquals(DayOfWeek.Tuesday, DayOfWeek.Wednesday.prev)
        assertEquals(DayOfWeek.Wednesday, DayOfWeek.Thursday.prev)
        assertEquals(DayOfWeek.Thursday, DayOfWeek.Friday.prev)
        assertEquals(DayOfWeek.Friday, DayOfWeek.Saturday.prev)
    }

    @Test
    fun testCompareLocale() {
        assertEquals(true, DayOfWeek.Monday.withLocale(KlockLocale.spanish) < DayOfWeek.Sunday.withLocale(KlockLocale.spanish))
        assertEquals(true, DayOfWeek.Monday.withLocale(KlockLocale.english) > DayOfWeek.Sunday.withLocale(KlockLocale.english))

        assertEquals(
            listOf(0, 1, 2, 3, 4, 5, 6),
            (0 until 7).map { DayOfWeek[it].index0Locale(KlockLocale.english) }
        )

        assertEquals(
            listOf(6, 0, 1, 2, 3, 4, 5),
            (0 until 7).map { DayOfWeek[it].index0Locale(KlockLocale.spanish) }
        )

        assertEquals(
            listOf(
                DayOfWeek.Sunday, DayOfWeek.Monday, DayOfWeek.Tuesday, DayOfWeek.Wednesday,
                DayOfWeek.Thursday, DayOfWeek.Friday, DayOfWeek.Saturday
            ),
            (0 until 7).map { DayOfWeek.get0(it, KlockLocale.english) }
        )
        assertEquals(
            listOf(
                DayOfWeek.Monday, DayOfWeek.Tuesday, DayOfWeek.Wednesday,
                DayOfWeek.Thursday, DayOfWeek.Friday, DayOfWeek.Saturday, DayOfWeek.Sunday
            ),
            (0 until 7).map { DayOfWeek.get0(it, KlockLocale.spanish) }
        )
    }
}
