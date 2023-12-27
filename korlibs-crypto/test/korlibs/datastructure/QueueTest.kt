package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class QueueTest {
	@Test
	fun name() {
		val queue = Queue<Boolean>()
		for (n in 0 until 1025) queue.enqueue(true)
		for (n in 0 until 1025) assertEquals(true, queue.dequeue())
	}

	@Test
	fun test1() {
		val queue = Queue<Boolean>()
		assertFailsWith<IndexOutOfBoundsException> {
			queue.dequeue()
		}
		queue.enqueue(true)
		queue.enqueue(true)
		queue.dequeue()
		queue.dequeue()
		assertFailsWith<IndexOutOfBoundsException> {
			queue.dequeue()
		}
	}

	@Test
	fun int() {
		val queue = IntQueue()
		queue.enqueue(10)
		queue.enqueue(20)
		queue.enqueue(15)

		assertEquals(true, queue.isNotEmpty())
		assertEquals(3, queue.size); assertEquals(10, queue.dequeue())
		assertEquals(2, queue.size); assertEquals(20, queue.dequeue())
		assertEquals(1, queue.size); assertEquals(15, queue.dequeue())
		assertEquals(0, queue.size)
		assertEquals(false, queue.isNotEmpty())
	}
}
