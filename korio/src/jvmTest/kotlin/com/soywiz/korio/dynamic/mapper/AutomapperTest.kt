package com.soywiz.korio.dynamic.mapper

import com.soywiz.korio.dynamic.serialization.*
import com.soywiz.korio.serialization.json.*
import com.soywiz.korio.util.*
import kotlin.test.*

class AutomapperTest {
	@Test
	fun test() {
		val mapper = ObjectMapper().jvmFallback()
		val obj = MyObj(10, "world")
		val json = Json.stringifyTyped(obj, mapper)
		assertEquals("""{"num":10,"hello":"world"}""", json)
		assertEquals(obj, Json.parseTyped<MyObj>(json, mapper))
	}

	data class MyObj(val num: Int, val hello: String)
}
