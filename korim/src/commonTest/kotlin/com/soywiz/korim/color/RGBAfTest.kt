package com.soywiz.korim.color

import kotlin.test.*

class RGBAfTest {
	@Test
	fun name() {
		assertEquals("RGBAf(0.5, 4, 1, 7)", RGBAf(0.5, 4, 1, 7).toString())
	}
}
