package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DequeTest {
	private val Deque<String>.str get() = this.joinToString(",")

	private fun <T> create() = CircularList<T>()

	@Test
	fun simple() {
		val l = create<String>()
		l += listOf("a", "b", "c", "d", "e", "f")
		assertEquals("a,b,c,d,e,f", l.str)
		assertEquals("a", l.first)
		assertEquals("f", l.last)
		assertEquals(6, l.size)
		l.removeAt(1)
		assertEquals(5, l.size)
		l.removeAt(l.size - 2)
		assertEquals(4, l.size)
		assertEquals("a,c,d,f", l.str)
		l.remove("d")
		assertEquals(3, l.size)
		assertEquals("a,c,f", l.str)
		l.retainAll(listOf("a", "f"))
		assertEquals(2, l.size)
		assertEquals("a,f", l.str)
		l.removeAll(listOf("a"))
		assertEquals(1, l.size)
		assertEquals("f", l.str)
	}

	@Test
	fun grow() {
		val l = create<String>()
		for (n in 0 until 1000) l.add("$n")
		for (n in 0 until 495) {
			l.removeFirst()
			l.removeLast()
		}
		assertEquals(10, l.size)
		assertEquals("495,496,497,498,499,500,501,502,503,504", l.str)
	}

	@Test
	fun grow2() {
		val l = create<Boolean>()
		for (n in 0 until 1000) l.addFirst(true)
		for (n in 0 until 1000) l.removeFirst()
		for (n in 0 until 1000) l.addFirst(true)
	}

	@Test
	fun test2() {
		val l = create<String>()
		l.addLast("a")
		l.addLast("b")
		l.addLast("c")
		l.removeAt(1)
		assertEquals("a,c", l.str)
	}

	@Test
	fun exceptions() {
		val l = create<Boolean>()
		assertFailsWith<IndexOutOfBoundsException> {
			l.removeFirst()
		}
		assertFailsWith<IndexOutOfBoundsException> {
			l.removeLast()
		}
		assertFailsWith<IndexOutOfBoundsException> {
			l.removeAt(1)
		}
		l.addFirst(true)
		l.removeAt(0)
		assertFailsWith<IndexOutOfBoundsException> {
			l.removeAt(0)
		}
	}

	@Test
	fun remove() {
		for (n in 0 until 16) {
			assertEquals(
				(0 until 16).toMutableList().apply { remove(n) },
				IntDeque().apply { addAll(0 until 16) }.apply { removeAt(n) }.toList()
			)
		}

		for (n in 0 until 16) {
			assertEquals(
				(0 until 17).toMutableList().apply { removeAt(0) }.apply { removeAt(n) },
				IntDeque().apply { addAll(0 until 17) }.apply { removeFirst() }.apply { removeAt(n) }.toList()
			)
		}

		for (n in 0 until 16) {
			assertEquals(
				(0 until 17).toMutableList().apply { removeAt(size - 1) }.apply { removeAt(n) },
				IntDeque().apply { addAll(0 until 17) }.apply { removeLast() }.apply { removeAt(n) }.toList()
			)
		}
	}

	@Test
	fun removeIterator() {
		val deque = IntDeque()
		deque.addAll(0 until 10)
		val iterator = deque.iterator()
		while (iterator.hasNext()) {
			val item = iterator.next()
			if ((item % 2) == 0 || item < 5) {
				iterator.remove()
			}
		}
		assertEquals(listOf(5, 7, 9), deque.toList())
	}

	@Test
	fun hashCodeEqualsTest() {
		val a = IntDeque().apply { addAll(listOf(1, 2, 3, 4)) }
		val b = IntDeque().apply { addAll(listOf(1, 2, 3, 4)) }
		assertEquals(a.hashCode(), b.hashCode())
		assertEquals(a, b)
	}

    @Test
    fun testAddFirstAll() {
        val deque = IntDeque(4)
        deque.addAll(listOf(5, 6, 7, 8))
        deque.addAllFirst(listOf(1, 2, 3, 4))
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 8), deque.toList())
    }

    @Test
    fun testAddAllArray() {
        val deque = IntDeque(1)
        deque.addAll(intArrayOf(+1, +2))
        deque.addAllFirst(intArrayOf(-2, -1))
        deque.addAll(intArrayOf(+3))
        deque.addAllFirst(intArrayOf(-3))
        assertEquals("-3,-2,-1,1,2,3", deque.joinToString(","))
    }

    @Test
    fun testAddOverflow() {
        val deque = IntDeque(4)
        deque.addAll(IntArray(1000))
        assertEquals(1000, deque.size)
        deque.addAll(IntArray(1000))
        assertEquals(2000, deque.size)
    }
}
