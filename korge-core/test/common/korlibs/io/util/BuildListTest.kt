package korlibs.io.util

import kotlin.test.Test
import kotlin.test.assertEquals

class BuildListTest {
	@Test
	fun test() {
		assertEquals(listOf("a", "b"), buildList { add("a"); add("b") })
	}
}
