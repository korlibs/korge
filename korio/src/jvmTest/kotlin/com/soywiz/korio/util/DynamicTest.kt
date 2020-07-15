package com.soywiz.korio.util

import com.soywiz.korio.async.*
import kotlin.test.*

class DynamicTest {
	@Test
	fun eq() {
		assertEquals(true, DynamicJvm.binop(1, 1, "=="))
		assertEquals(true, DynamicJvm.binop(1.0, 1, "=="))
		assertEquals(false, DynamicJvm.binop(1.0, 1.1, "=="))
	}

	@Test
	fun op() {
		assertEquals(true, DynamicJvm.binop(1.0, 3, "<"))
		assertEquals(false, DynamicJvm.binop(1.0, 3, ">"))
		assertEquals(true, DynamicJvm.binop(1, 3.0, "<"))
		assertEquals(false, DynamicJvm.binop(1, 3.0, ">"))
		assertEquals(true, DynamicJvm.binop(1.0, 3.0, "<"))
		assertEquals(false, DynamicJvm.binop(1.0, 3.0, ">"))
		assertEquals(false, DynamicJvm.binop(6.0, 3.0, "<"))
		assertEquals(true, DynamicJvm.binop(6.0, 3.0, ">"))
	}

	@Test
	fun get() = suspendTest {
		class DynamicObj(val obj: Any?) {
			suspend fun get(key: String) = DynamicObj(DynamicJvm.getAny(obj, key))

			fun <T> to(): T = obj as T
		}

		assertEquals(10, DynamicObj(linkedMapOf("a" to 10)).get("a").to<Int>())
	}
}
