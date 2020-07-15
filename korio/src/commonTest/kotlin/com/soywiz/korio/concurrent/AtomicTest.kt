package com.soywiz.korio.concurrent

import com.soywiz.korio.concurrent.atomic.*
import kotlin.test.*

class AtomicTest {
	@Test
	fun test() {
		val value = KorAtomicInt(0)
		assertEquals(1, value.incrementAndGet())
		assertEquals(1, value.value++)
		assertEquals(2, value.value)
		assertEquals(3, ++value.value)
	}
}