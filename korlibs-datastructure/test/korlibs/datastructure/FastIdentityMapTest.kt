package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals

class FastIdentityMapTest {
	@Test
	fun test() {
		class Demo {
			override fun hashCode(): Int = 0
		}

		val map = FastIdentityMap<Demo, String>()
		val i1 = Demo()
		val i2 = Demo()
		val i3 = Demo()
		map[i1] = "i1"
		map[i2] = "i2"

		assertEquals("i1", map[i1])
		assertEquals("i2", map[i2])
		assertEquals(null, map[i3])
		assertEquals(2, map.size)
		assertEquals(listOf("i1", "i2"), map.values.sorted())

		map[i1] = "i1b"
		map[i2] = "i2b"

		assertEquals("i1b", map[i1])
		assertEquals("i2b", map[i2])
		assertEquals(null, map[i3])
		assertEquals(2, map.size)
		assertEquals(listOf("i1b", "i2b"), map.values.sorted())

		map.remove(i1)

		assertEquals(null, map[i1])
		assertEquals("i2b", map[i2])
		assertEquals(null, map[i3])
		assertEquals(1, map.size)
		assertEquals(listOf("i2b"), map.values.sorted())
	}
}
