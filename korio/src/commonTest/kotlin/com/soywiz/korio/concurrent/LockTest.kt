package com.soywiz.korio.concurrent

import com.soywiz.korio.concurrent.lock.*
import kotlin.test.*

class LockTest {
	@Test
	fun test() {
		val lock = Lock()
		var a = 0
		lock {
			a++
		}
		assertEquals(1, a)
	}
}