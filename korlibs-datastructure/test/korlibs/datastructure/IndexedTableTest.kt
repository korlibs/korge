package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals

class IndexedTableTest {
	@Test
	fun test() {
		val table = IndexedTable<String>()
		table.add("hello")
		table.add("world")
		assertEquals(0, table["hello"])
		assertEquals(1, table["world"])
		assertEquals(2, table["test"])
		assertEquals(0, table["hello"])
		assertEquals(1, table["world"])
		assertEquals(2, table["test"])
		assertEquals(3, table.size)
	}
}
