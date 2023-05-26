package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals

class IntFloatMapTest {
	@Test
	fun simple() {
		val m = IntFloatMap()
		assertEquals(0, m.size)
		assertEquals(0f, m[0])

		m[0] = 98f
		assertEquals(1, m.size)
		assertEquals(98f, m[0])
		assertEquals(0f, m[1])

		m[0] = 99f
		assertEquals(1, m.size)
		assertEquals(99f, m[0])
		assertEquals(0f, m[1])

		m.remove(0)
		assertEquals(0, m.size)
		assertEquals(0f, m[0])
		assertEquals(0f, m[1])

		m.remove(0)
	}

	@Test
	fun name2() {
		val m = IntFloatMap()
		for (n in 0 until 1000) m[n] = (n * 1000).toFloat()
		for (n in 0 until 1000) {
			assertEquals((n * 1000).toFloat(), m[n])
			assertEquals(true, m.contains(n))
		}
		assertEquals(0f, m[-1])
		assertEquals(0f, m[1001])
		assertEquals(false, m.contains(-1))
		assertEquals(false, m.contains(1001))
	}
}
