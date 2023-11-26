package korlibs.io.util

import kotlin.test.Test
import kotlin.test.assertEquals

class GenerateTest {
	fun evens() = sequence {
		for (n in 0 until Int.MAX_VALUE step 2) yield(n)
	}

	@Test
	fun name() {
		assertEquals(listOf(0, 2, 4, 6), evens().take(4).toList())
	}
}
