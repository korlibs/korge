package korlibs.datastructure.lock

import korlibs.datastructure.thread.NativeThread
import korlibs.datastructure.thread.nativeThread
import korlibs.platform.Platform
import korlibs.time.milliseconds
import korlibs.time.seconds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

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
