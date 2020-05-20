package com.soywiz.korge.time

import com.soywiz.klock.*
import com.soywiz.korge.internal.*
import kotlin.test.*

class TimeSpanTest {
	private val DELTA = 0.00001

	@Test
	fun name() {
		assertEquals(1000.0, 1.secs.milliseconds)
		assertEquals(1.0, 1.secs.seconds)
		assertEquals(1000.0, 1000.ms.milliseconds)
		assertEquals(1.0, 1000.ms.seconds)
	}
}
