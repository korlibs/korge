package com.soywiz.korio.util

import kotlin.test.*

class BuildListTest {
	@Test
	fun test() {
		assertEquals(listOf("a", "b"), buildList { add("a"); add("b") })
	}
}