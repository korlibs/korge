package com.soywiz.klock

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
}
