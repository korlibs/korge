package korlibs.io.concurrent

import korlibs.datastructure.lock.Lock
import korlibs.datastructure.lock.NonRecursiveLock
import kotlin.test.Test
import kotlin.test.assertEquals

class LockTest {
	@Test
	fun test() {
		val lock = Lock()
		var a = 0
		lock {
			a++
		}
		assertEquals(1, a)
	}

    @Test
    fun test2() {
        val lock = NonRecursiveLock()
        var a = 0
        lock {
            a++
        }
        assertEquals(1, a)
    }
}
