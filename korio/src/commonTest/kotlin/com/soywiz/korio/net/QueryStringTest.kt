package com.soywiz.korio.net

import kotlin.test.*

class QueryStringTest {
	private fun assertIdem(str: String) {
		assertEquals(str, QueryString.encode(QueryString.decode(str)))
	}

	@kotlin.test.Test
	fun name() {
		assertEquals(linkedMapOf("a" to listOf("2"), "b" to listOf("3")), QueryString.decode("a=2&b=3"))
		assertIdem("a=1&b=2")
	}
}