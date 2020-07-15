package com.soywiz.korio.util

import kotlin.math.*
import kotlin.test.*

class NumberParserTest {
	@Test fun test1() = assertEquals(0.0, NumberParser.parseDouble("0"))
	@Test fun test2() = assertEquals(1.0, NumberParser.parseDouble("1"))
	@Test fun test3() = assertEquals(-1.0, NumberParser.parseDouble("-1"))
	@Test fun test4a() = assertEquals(105.0, NumberParser.parseDouble("105"))
	@Test fun test4b() = assertEquals(10.5, NumberParser.parseDouble("10.5"))
	@Test fun test4c() = assertEquals(-10.5, NumberParser.parseDouble("-10.5"))
	@Test fun test5() = assertEquals(1.5, NumberParser.parseDouble("1.5"))
	@Test fun test6() = assertEquals(1e10, NumberParser.parseDouble("1e10"))
	@Test fun test7() = assertEquals(1e-10, NumberParser.parseDouble("1e-10"), 0.0001)
	@Test fun test8() = assertEquals(-1.5e-10, NumberParser.parseDouble("-1.5e-10"), 0.0001)
    @Test fun test9() = assertEquals(123456, NumberParser.parseInt("123456"))
    @Test fun test10() = assertEquals(-123456, NumberParser.parseInt("-123456"))
    @Test fun test11() = assertEquals(123456, NumberParser.parseInt("1e240", radix = 16))
    @Test fun test12() = assertEquals(-123456, NumberParser.parseInt("-1e240", radix = 16))

	fun assertEquals(expected: Double, actual: Double, epsilon: Double) {
		assertTrue("$expected != $actual (epsilon=$epsilon) (diff=${abs(expected - actual)})") { abs(expected - actual) <= epsilon }
	}
}
