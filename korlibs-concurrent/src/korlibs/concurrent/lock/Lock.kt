@file:Suppress("PackageDirectoryMismatch")

package korlibs.concurrent.lock

import korlibs.concurrent.thread.*
import kotlin.time.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

interface BaseLock {
    fun notify(unit: Unit = Unit)
    fun wait(time: Duration): Boolean
    //fun lock()
    //fun unlock()
}

//typealias Lock = BaseLock
//typealias NonRecursiveLock = BaseLock

//inline operator fun <T> BaseLock.invoke(callback: () -> T): T {
//    lock()
//    try {
//        return callback()
//    } finally {
//        unlock()
//    }
//}

/**
 * Reentrant typical lock.
 */
expect class Lock() : BaseLock {
    override fun notify(unit: Unit)
    override fun wait(time: Duration): Boolean
    inline operator fun <T> invoke(callback: () -> T): T
}

/**
 * Optimized lock that cannot be called inside another lock,
 * don't keep the current thread id, or a list of threads to awake
 * It is lightweight and just requires an atomic.
 * Does busy-waiting instead of sleeping the thread.
 */
expect class NonRecursiveLock() : BaseLock {
    override fun notify(unit: Unit)
    override fun wait(time: Duration): Boolean
    inline operator fun <T> invoke(callback: () -> T): T
}

fun BaseLock.waitPrecise(time: Duration): Boolean {
    val startTime = TimeSource.Monotonic.markNow()
    val doWait = time - 10.milliseconds
    val signaled = if (doWait > 0.seconds) wait(doWait) else false
    if (!signaled && doWait > 0.seconds) {
        val elapsed = startTime.elapsedNow()
        //println(" !!!!! SLEEP EXACT: ${elapsed - time}")
        NativeThread.sleepExact(time - elapsed)
    }
    return signaled
}

fun BaseLock.wait(time: Duration, precise: Boolean): Boolean {
    return if (precise) waitPrecise(time) else wait(time)
}
