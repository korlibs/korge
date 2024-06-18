package korlibs.datastructure.event

import korlibs.concurrent.thread.*
import korlibs.datastructure.closeable.*
import korlibs.datastructure.thread.*
import korlibs.datastructure.thread.NativeThread
import korlibs.datastructure.thread.nativeThread
import korlibs.io.async.*
import korlibs.time.*
import kotlin.test.*
import kotlin.time.*
import kotlin.time.Duration.Companion.seconds

class SyncEventLoopTest {
    // @TODO: Lock.notify is not implemented on JS
    @Test
    fun test() = suspendTest({ NativeThread.isSupported }) {
    //fun test() = suspendTest {
        repeat(2) {
            val ep = SyncEventLoop(precise = true)
            val start = TimeSource.Monotonic.markNow()
            fun log(msg: String) {
                println("${start.elapsedNow().milliseconds}: $msg")
            }
            var times = 0
            var interval: AutoCloseable? = null
            ep.setTimeout(0.5.seconds) { log("timeout after 0.5 seconds") }
            interval = ep.setInterval(0.2.seconds) {
                log("interval after 0.2 seconds")
                times++
                if (times >= 3) interval?.close()
            }
            ep.setImmediate { log("hello") }
            ep.setImmediate { log("world") }
            ep.setImmediateFirst { log("hi, ") }
            if (NativeThread.isSupported) {
                nativeThread {
                    NativeThread.sleepExact(10.milliseconds)
                    println("Enqueuing test after 10ms")
                    ep.setImmediate { log("test from thread") }
                }
                nativeThread {
                    NativeThread.sleepExact(50.milliseconds)
                    println("Enqueuing 150ms test after 50ms")
                    ep.setTimeout(150.milliseconds) { log("after 500 ms from thread") }
                }
            }
            ep.runTasksUntilEmpty()
        }
    }
}
