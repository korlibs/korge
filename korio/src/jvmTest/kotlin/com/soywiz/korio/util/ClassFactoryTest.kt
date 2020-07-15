package com.soywiz.korio.util

import com.soywiz.korio.dynamic.mapper.*
import com.soywiz.korio.dynamic.serialization.*
import com.soywiz.korio.serialization.json.*
import kotlin.test.*

class ClassFactoryTest {
	@Test
	fun name() {
		data class A(val a: Int, val b: String)

		val mapper = ObjectMapper()
		mapper.jvmFallback()
		assertEquals(
			mapOf("a" to 10, "b" to "test"),
			mapper.toUntyped(A(10, "test"))
		)
	}

	@Suppress("unused")
	@Test
	fun name2() {
		class B()
		class A(val a: Array<B>, val b: IntArray)

		val obj = A(arrayOf(B()), intArrayOf(1, 2, 3))

		assertEquals(
			mapOf("a" to listOf(mapOf<Any, Any>()), "b" to listOf(1, 2, 3)),
			JvmTyper.untype(obj)
		)
	}

	@Suppress("unused")
	@Test
	fun name3() {
		val mapper = ObjectMapper().jvmFallback()

		class B()
		class A(val a: Array<B>, val b: IntArray)
		assertEquals(
			"""{"a":[{}],"b":[1,2,3]}""",
			Json.stringifyTyped(A(arrayOf(B()), intArrayOf(1, 2, 3)), mapper)
		)
	}

}