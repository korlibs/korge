package com.soywiz.korio.lang

import kotlin.test.*

class StringExtTest {
	@Test
	fun testParseInt() {
		assertEquals(0x10, "0x10".parseInt()) // Hex
		assertEquals(0b11, "0b11".parseInt()) // Binary
		assertEquals(9, "0o11".parseInt()) // Octal
		assertEquals(16, "16".parseInt()) // Decimal
	}

	@Test
	fun testTransform() {
		// @TODO WTF (only on kotlin-jvm?): e: /Users/soywiz/projects/korlibs/korio/korio/src/commonTest/kotlin/com/soywiz/korio/lang/StringExtTest.kt: (14, 50): Operator '==' cannot be applied to 'String!' and 'Char'
		//assertEquals("hEEllo", "hello".transform { if (it == 'e') "EE" else "$it" })
	}

	@Test
	fun testEachBuilder() {
		assertEquals("hEEllo", "hello".eachBuilder { if (it == 'e') append("EE") else append(it) })
	}

	@Test
	fun testSplitInChunks() {
		assertEquals(listOf("ab", "cd"), "abcd".splitInChunks(2))
		assertEquals(listOf("ab", "cd", "e"), "abcde".splitInChunks(2))
	}

	@Test
	fun testSplitKeep() {
		assertEquals(listOf("a", "   ", "c"), "a   c".splitKeep(Regex("\\s+")))
	}

	@Test
	fun testFormat() {
		assertEquals("1 2", "%d %d".format(1, 2))
		assertEquals("01 2", "%02d %d".format(1, 2))
		assertEquals("f", "%x".format(15))
		assertEquals("0f", "%02x".format(15))
		assertEquals("0F", "%02X".format(15))
	}

}
