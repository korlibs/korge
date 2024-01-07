@file:Suppress("PackageDirectoryMismatch")

package korlibs.datastructure.thread

import korlibs.datastructure.*
import korlibs.time.*
import kotlin.time.*

expect class NativeThread(code: (NativeThread) -> Unit) : Extra {
    companion object {
        val isSupported: Boolean
        val currentThreadId: Long
        val currentThreadName: String?

        fun gc(full: Boolean): Unit
        fun sleep(time: TimeSpan): Unit
        inline fun spinWhile(cond: () -> Boolean): Unit
    }
    var threadSuggestRunning: Boolean
    var priority: Int
    var name: String?
    var isDaemon: Boolean
    fun start(): Unit
    fun interrupt(): Unit
}

public fun nativeThread(
    start: Boolean = true,
    isDaemon: Boolean = false,
    name: String? = null,
    priority: Int = -1,
    block: (NativeThread) -> Unit
): NativeThread {
    val thread = NativeThread(block)
    if (isDaemon) thread.isDaemon = true
    if (priority > 0) thread.priority = priority
    if (name != null) thread.name = name
    // if (contextClassLoader != null) thread.contextClassLoader = contextClassLoader
    if (start) thread.start()
    return thread
}

fun NativeThread.Companion.sleep(time: TimeSpan, exact: Boolean) {
    if (exact) sleepExact(time) else sleep(time)
}

// https://stackoverflow.com/questions/13397571/precise-thread-sleep-needed-max-1ms-error#:~:text=Scheduling%20Fundamentals
// https://www.softprayog.in/tutorials/alarm-sleep-and-high-resolution-timers
fun NativeThread.Companion.sleepExact(time: TimeSpan) {
    val start = TimeSource.Monotonic.markNow()
    //val imprecision = 10.milliseconds
    //val imprecision = 1.milliseconds
    val imprecision = 4.milliseconds
    val javaSleep = time - imprecision
    if (javaSleep >= 0.seconds) {
        NativeThread.sleep(javaSleep)
    }
    NativeThread.spinWhile { start.elapsedNow() < time }
}

fun NativeThread.Companion.sleepUntil(date: DateTime, exact: Boolean = true) {
    sleep(date - DateTime.now(), exact)
}

fun NativeThread.Companion.sleepWhile(cond: () -> Boolean) {
    while (cond()) {
        NativeThread.sleep(1.milliseconds)
    }
}
