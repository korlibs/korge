package korlibs.datastructure.lock

import korlibs.datastructure.thread.*
import korlibs.platform.*
import korlibs.time.*
import kotlin.test.*

class LockTest {
    @Test
    fun test() {
        val lock = Lock()
        var a = 0
        lock {
            lock {
                a++
            }
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

    @Test
    fun testWaitNotify() {
        if (Platform.isJsOrWasm) return

        val lock = Lock()
        var log = arrayListOf<String>()
        nativeThread(start = true) {
            NativeThread.sleep(10.milliseconds)
            log += "b"
            lock { lock.notify() }
        }
        lock {
            lock {
                log += "a"
                lock.wait(1.seconds)
                log += "c"
            }
        }
        assertEquals("abc", log.joinToString(""))
    }

    @Test
    fun testNotifyError() {
        val lock = Lock()
        assertFails { lock.notify() }
    }

    @Test
    fun testWaitError() {
        val lock = Lock()
        assertFails { lock.wait(1.seconds) }
    }
}
