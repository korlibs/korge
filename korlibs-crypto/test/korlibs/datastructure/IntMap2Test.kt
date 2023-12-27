package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals

class IntMap2Test {
	@Test
	fun simple() {
		val m = IntMap<String>()
		assertEquals(0, m.size)
		assertEquals(null, m[0])

		m[0] = "test"
		assertEquals(1, m.size)
		assertEquals("test", m[0])
		assertEquals(null, m[1])

		m[0] = "test2"
		assertEquals(1, m.size)
		assertEquals("test2", m[0])
		assertEquals(null, m[1])

		m.remove(0)
		assertEquals(0, m.size)
		assertEquals(null, m[0])
		assertEquals(null, m[1])

		m.remove(0)
	}

	@Test
	fun name2() {
		val m = IntMap<Int>()
		for (n in 0 until 1000) m[n] = n * 1000
		for (n in 0 until 1000) assertEquals(n * 1000, m[n])
		assertEquals(null, m[-1])
		assertEquals(null, m[1001])
	}
}
