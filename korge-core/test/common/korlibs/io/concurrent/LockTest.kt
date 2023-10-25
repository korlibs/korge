package korlibs.io.concurrent

import korlibs.datastructure.lock.*
import kotlin.test.*

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
