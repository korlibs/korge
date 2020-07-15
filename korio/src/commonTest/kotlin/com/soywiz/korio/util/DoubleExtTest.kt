package com.soywiz.korio.util

import kotlin.test.*

class DoubleExtTest {
	@Test
	fun test() {
		assertEquals("10", 10.0.toStringDecimal(0))
		assertEquals("10.0", 10.0.toStringDecimal(1))
		assertEquals("10.00", 10.0.toStringDecimal(2))
		assertEquals("10.000", 10.0.toStringDecimal(3))

		assertEquals("10", 10.0.toStringDecimal(0, true))
		assertEquals("10", 10.0.toStringDecimal(1, true))
		assertEquals("10", 10.0.toStringDecimal(2, true))
	}

	@Test
	fun test2() {
		assertEquals("10", 10.123.toStringDecimal(0))
		assertEquals("10.1", 10.123.toStringDecimal(1))
		assertEquals("10.12", 10.123.toStringDecimal(2))
		assertEquals("10.123", 10.123.toStringDecimal(3))
		assertEquals("10.1230", 10.123.toStringDecimal(4))

		assertEquals("10", 10.123.toStringDecimal(0, true))
		assertEquals("10.1", 10.123.toStringDecimal(1, true))
		assertEquals("10.12", 10.123.toStringDecimal(2, true))
		assertEquals("10.123", 10.123.toStringDecimal(3, true))
		assertEquals("10.123", 10.123.toStringDecimal(4, true))
	}

	@Test
	fun test3() {
		//assertEquals("1.0e21", 10e20.toString().toLowerCase())

		assertEquals("100000000000000000000", 1e20.toStringDecimal(0))
		assertEquals("1000000000000000000000", 1e21.toStringDecimal(0))
		assertEquals("10000000000000000000000", 1e22.toStringDecimal(0))
		assertEquals("123000000000000000000", 1.23e20.toStringDecimal(0))
		assertEquals("1230000000000000000000", 1.23e21.toStringDecimal(0))

		assertEquals("0", 1.23e-3.toStringDecimal(0))
		assertEquals("0.0", 1.23e-3.toStringDecimal(1))
		assertEquals("0.00123", 1.23e-3.toStringDecimal(10, true))
		assertEquals("0.000123", 1.23e-4.toStringDecimal(10, true))
		assertEquals("0.0000123", 1.23e-5.toStringDecimal(10, true))
		assertEquals("0.00000123", 1.23e-6.toStringDecimal(10, true))
	}
}