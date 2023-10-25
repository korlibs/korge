package korlibs.io.lang

import korlibs.datastructure.lock.*
import korlibs.datastructure.thread.*
import korlibs.time.*
import kotlin.test.*

class ThreadLocalTest {
    @Test
    fun test() {
        if (!NativeThread.isSupported) return

        var n = 0
        val lock = Lock()
        val log = arrayListOf<String>()
        val tl = threadLocal { n++ }
        log += "main:${tl.value}"
        nativeThread { log += "thread:${tl.value}"; lock { lock.notify() } }
        lock { lock.wait(10.seconds) }
        log += "main:${tl.value}"
        assertEquals(listOf("main:0", "thread:1", "main:0"), log)
    }
}
