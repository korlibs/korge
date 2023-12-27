package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals


private class C {
    val value = 1
}

private var C.prop by WeakProperty { 0 }
private var C.prop2 by WeakPropertyThis<C, String> { "${value * 2}" }

class WeakPropertyTest {
	@Test
	fun name() {
		val c1 = C()
		val c2 = C()
		assertEquals(0, c1.prop)
		assertEquals(0, c2.prop)
		c1.prop = 1
		c2.prop = 2
		assertEquals(1, c1.prop)
		assertEquals(2, c2.prop)

		assertEquals("2", c2.prop2)
		c2.prop2 = "3"
		assertEquals("3", c2.prop2)
	}
}
