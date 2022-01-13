package com.soywiz.klock

import kotlin.test.*

class YearTest {
    @Test
    fun testLeap() {
        assertEquals(true, Year.isLeap(2000))
        assertEquals(false, Year.isLeap(2006))
    }

    @Test
    fun testLeapInstance() {
        assertEquals(true, Year(2000).isLeap)
        assertEquals(false, Year(2006).isLeap)
    }
}
