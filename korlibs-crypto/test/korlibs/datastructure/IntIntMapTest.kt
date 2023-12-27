package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals

class IntIntMapTest {
	@Test
	fun simple() {
		val m = IntIntMap()
		assertEquals(0, m.size)
		assertEquals(0, m[0])

		m[0] = 98
		assertEquals(1, m.size)
		assertEquals(98, m[0])
		assertEquals(0, m[1])

		m[0] = 99
		assertEquals(1, m.size)
		assertEquals(99, m[0])
		assertEquals(0, m[1])

		m.remove(0)
		assertEquals(0, m.size)
		assertEquals(0, m[0])
		assertEquals(0, m[1])

		m.remove(0)
	}

	@Test
	fun name2() {
		val m = IntIntMap()
		for (n in 0 until 1000) m[n] = n * 1000
		for (n in 0 until 1000) {
			assertEquals(n * 1000, m[n])
			assertEquals(true, m.contains(n))
		}
		assertEquals(0, m[-1])
		assertEquals(0, m[1001])
		assertEquals(false, m.contains(-1))
		assertEquals(false, m.contains(1001))
	}
}
