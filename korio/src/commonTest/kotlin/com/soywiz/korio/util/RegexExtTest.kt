package com.soywiz.korio.util

import kotlin.test.*

class RegexExtTest {
	@Test
	fun test() {
		assertEquals("""\*\+""", Regex.quote("*+"))
		assertEquals("""\.\?\*\+\^\\\[\]\(\)\{\}\|\-\${'$'}""", Regex.quote(""".?*+^\[](){}|-${'$'}"""))

		assertEquals("""abc\[a\]""", Regex.quote("abc[a]"))


		// @TODO: Inconsistent among targets! JS/JVM
		//assertEquals("""\Q*+\E""", Regex.escape("*+"))
		//assertEquals("""*+""", Regex.escapeReplacement("*+"))
		//assertEquals("""*+""", Regex.fromLiteral("*+").pattern)
	}
}