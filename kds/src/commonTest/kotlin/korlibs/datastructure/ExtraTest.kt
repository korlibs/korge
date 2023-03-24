package korlibs.datastructure

import kotlin.test.assertEquals

class ExtraTest {
	class Demo : Extra by Extra.Mixin() {
		val default = 9
	}

	var Demo.demo by Extra.Property { 0 }
	var Demo.demo2 by Extra.PropertyThis<Demo, Int> { default }

	@kotlin.test.Test
	fun name() {
		val demo = Demo()
		assertEquals(0, demo.demo)
		assertEquals(9, demo.demo2)
		demo.demo = 7
		assertEquals(7, demo.demo)
		assertEquals(mapOf("demo" to 7, "demo2" to 9), demo.extra?.toMap())
	}
}