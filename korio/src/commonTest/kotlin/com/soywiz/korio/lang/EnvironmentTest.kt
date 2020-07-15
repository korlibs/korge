package com.soywiz.korio.lang

import kotlin.test.Test

class EnvironmentTest {
	@Test
	fun test() {
		println(Environment["path"])
		println(Environment["PATH"])
		println(Environment.getAll())
	}
}