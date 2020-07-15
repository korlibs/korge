package com.soywiz.korio.dynamic

import org.junit.Test
import kotlin.test.*

class KDynamicTest {
	@Test
	fun test() {
		assertEquals("10", KDynamic { global["java.lang.String"].dynamicInvoke("valueOf", 10) })
		assertEquals("10", KDynamic { global["java"]["lang.String"].dynamicInvoke("valueOf", 10) })
		assertEquals("HELLO", KDynamic { "hello".dynamicInvoke("toUpperCase") })
		assertEquals("a", KDynamic { Demo().dynamicInvoke("demo") })
		assertEquals("b", KDynamic { Demo().dynamicInvoke("demo", "1") })
		assertEquals("c", KDynamic { Demo().dynamicInvoke("demo", 1) })
	}

	@Test
	fun test2() {
		assertEquals("c", KDynamic { Demo().dynamicInvoke("demo", 1) })
	}

	@Suppress("unused", "UNUSED_PARAMETER")
	class Demo {
		fun demo(): String = "a"
		fun demo(a: String): String = "b"
		fun demo(a: Int): String = "c"
	}

	@Test
	fun test3() {
		assertEquals(10, KDynamic(mapOf("a" to 10)) { it["a"].int })
	}

	@Suppress("unused")
	class Demo2 {
		var z = 3
		fun setA(value: Int) {
			z = value * 2
		}
		fun getA() = z * 3
	}

	@Test
	fun test4() {
		assertEquals(20, KDynamic(Demo2()) { it["a"] = 10; it }.z)
		assertEquals(9, KDynamic(Demo2()) { it["a"] })
		assertEquals(3, KDynamic(Demo2()) { it["z"] })
	}

	@Test
	fun test5() {
		assertEquals(java.lang.Integer.TYPE, KDynamic { global["java.lang.Integer"]["TYPE"] })
	}
}
