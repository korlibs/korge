package com.soywiz.korge.time

import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import kotlin.test.Test
import kotlin.test.assertEquals

class TimeSpanTest {
	private val DELTA = 0.00001

	@Test
	fun name() {
		assertEquals(1000.0, 1.seconds.milliseconds)
		assertEquals(1.0, 1.seconds.seconds)
		assertEquals(1000.0, 1000.milliseconds.milliseconds)
		assertEquals(1.0, 1000.milliseconds.seconds)
	}
}
