@file:Suppress("PackageDirectoryMismatch")

package korlibs.datastructure.thread

import korlibs.datastructure.*
import korlibs.time.*
import korlibs.concurrent.thread.sleep as sleepConcurrent
import korlibs.concurrent.thread.sleepExact as sleepExactConcurrent
import korlibs.concurrent.thread.sleepWhile as sleepWhileConcurrent

@Deprecated("Use korlibs.concurrent.thread package")
typealias NativeThread = korlibs.concurrent.thread.NativeThread

val korlibs.concurrent.thread.NativeThread.extra: Extra get() {
    if (this.userData == null) {
        this.userData = Extra.Mixin()
    }
    return this.userData as Extra
}

@Deprecated("Use korlibs.concurrent.thread package")
public fun nativeThread(
    start: Boolean = true,
    isDaemon: Boolean = false,
    name: String? = null,
    priority: Int = -1,
    block: (NativeThread) -> Unit
): NativeThread = korlibs.concurrent.thread.nativeThread(start, isDaemon, name, priority, block)

@Deprecated("Use korlibs.concurrent.thread package")
fun korlibs.concurrent.thread.NativeThread.Companion.sleep(time: TimeSpan, exact: Boolean) = sleepConcurrent(time, exact)

// https://stackoverflow.com/questions/13397571/precise-thread-sleep-needed-max-1ms-error#:~:text=Scheduling%20Fundamentals
// https://www.softprayog.in/tutorials/alarm-sleep-and-high-resolution-timers
@Deprecated("Use korlibs.concurrent.thread package")
fun korlibs.concurrent.thread.NativeThread.Companion.sleepExact(time: TimeSpan) = sleepExactConcurrent(time)
@Deprecated("Use korlibs.concurrent.thread package")
inline fun korlibs.concurrent.thread.NativeThread.Companion.sleepWhile(cond: () -> Boolean) = sleepWhileConcurrent(cond)

// Extension from DateTime
fun korlibs.concurrent.thread.NativeThread.Companion.sleepUntil(date: DateTime, exact: Boolean = true) {
    sleep(date - DateTime.now(), exact)
}
