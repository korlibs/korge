package korlibs.datastructure.iterators

import kotlin.test.Test
import kotlin.test.assertEquals

class FastIteratorsTest {
	@Test
	fun testFastIterateRemove() {
		assertEquals(listOf(1, 3, 5, 5, 3), arrayListOf(1, 2, 3, 4, 5, 5, 8, 8, 3).fastIterateRemove { it % 2 == 0 })
	}

	@Test
	fun testSupportsAddingItemsWhileIterating() {
		val items = arrayListOf("a", "b")
		val out = arrayListOf<String>()
		items.fastForEachWithIndex { index, value ->
			if (value == "a") items.add("c")
			if (value == "b") items.add("d")
			out += "$index:$value"
		}
		assertEquals("0:a,1:b,2:c,3:d", out.joinToString(","))
	}
}
