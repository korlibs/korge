package korlibs.io.concurrent

import korlibs.io.concurrent.atomic.KorAtomicInt
import korlibs.io.concurrent.atomic.getAndAdd
import korlibs.io.concurrent.atomic.getAndIncrement
import korlibs.io.concurrent.atomic.incrementAndGet
import kotlin.test.Test
import kotlin.test.assertEquals

class AtomicTest {
	@Test
	fun test() {
		val value = KorAtomicInt(0)
		assertEquals(1, value.incrementAndGet())
		assertEquals(1, value.value++)
		assertEquals(2, value.value)
		assertEquals(3, ++value.value)
	}

    @Test
    fun testSingleton() {
        val value = singleton
        value.value = 0
        assertEquals(1, value.incrementAndGet())
        assertEquals(1, value.value++)
        assertEquals(2, value.value)
        assertEquals(3, ++value.value)
    }

    @Test
    fun testSingleton2() {
        val value = singleton
        value.value = 0
        assertEquals(0, value.getAndIncrement())
        assertEquals(1, value.getAndAdd(10))
        assertEquals(11, value.value)
    }

    companion object {
        val singleton = KorAtomicInt(0)
    }
}
