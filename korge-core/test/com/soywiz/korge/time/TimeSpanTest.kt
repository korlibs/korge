package com.soywiz.korge.time

import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test

class TimeSpanTest {
	private val DELTA = 0.00001

	@Test
	fun name() {
		Assert.assertEquals(1000, 1.seconds.milliseconds)
		Assert.assertEquals(1.0, 1.seconds.seconds, DELTA)
		Assert.assertEquals(1000, 1000.milliseconds.milliseconds)
		Assert.assertEquals(1.0, 1000.milliseconds.seconds, DELTA)
	}
}
