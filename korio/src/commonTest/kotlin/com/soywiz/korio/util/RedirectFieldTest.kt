package com.soywiz.korio.util

import kotlin.test.*

class RedirectFieldTest {
	class A {
		var z: Int = 10
		val i: Int = 10
	}

	class B(val a: A) {
		var z: Int by a::z.redirected()
		val y: Int by this::z.redirected()
		val i: Int by a::i.redirected()
		val l: Int by { a::i }.redirected()
		val r: Int by { a::z }.redirected()
	}

	@Test
	fun redirect() {
		val b = B(A())
		assertEquals(b.z, 10)
		assertEquals(b.a.z, 10)
		assertEquals(b.y, 10)
		b.z = 20
		assertEquals(b.z, 20)
		assertEquals(b.a.z, 20)
		assertEquals(b.y, 20)

		assertEquals(b.i, 10)
		assertEquals(b.a.i, 10)

		assertEquals(b.l, 10)
		assertEquals(b.r, 20)
	}

	@Test
	fun demo() {
		val a = A()
		val zi2 by a::z.transformed { it * 2 }
		var zm2 by a::z.transformed({ it * 2 }, { it / 2 })
		assertEquals(10, a.z)
		assertEquals(20, zi2)
		a.z = 20
		assertEquals(20, a.z)
		assertEquals(40, zi2)
		zm2 = 60
		assertEquals(30, a.z)
		assertEquals(60, zm2)
	}
}