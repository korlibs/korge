package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StackTest {
	@Test
	fun name() {
		val s = IntStack()
		assertEquals(0, s.size)
		s.push(3); assertEquals(1, s.size)
		s.push(5); assertEquals(2, s.size)
		s.push(7); assertEquals(3, s.size)
		assertEquals(7, s.pop()); assertEquals(2, s.size)
		assertEquals(5, s.pop()); assertEquals(1, s.size)
		assertEquals(3, s.pop()); assertEquals(0, s.size)
		assertFailsWith<IndexOutOfBoundsException> {
			s.pop()
		}
	}

	@Test
	fun grow() {
		val s = IntStack()
		for (n in 0..999) s.push(n)
		for (n in 999 downTo 0) assertEquals(n, s.pop())
	}

	@Test
	fun collection() {
		val s = IntStack(1, 2, 3)
		assertEquals(listOf(1, 2, 3), s.toList())
	}
}
