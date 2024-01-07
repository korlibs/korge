package korlibs.datastructure

import kotlin.test.assertEquals

private class ExtraTestDemo : Extra by Extra.Mixin() {
    val default = 9
}

private var ExtraTestDemo.demo by Extra.Property { 0 }
private var ExtraTestDemo.demo2 by Extra.PropertyThis<ExtraTestDemo, Int> { default }


class ExtraTest {
	@kotlin.test.Test
    @kotlin.test.Ignore
	fun name() {
		val demo = ExtraTestDemo()
		assertEquals(0, demo.demo)
		assertEquals(9, demo.demo2)
		demo.demo = 7
		assertEquals(7, demo.demo)
		assertEquals(mapOf("demo" to 7, "demo2" to 9), demo.extra?.toMap())
	}
}
