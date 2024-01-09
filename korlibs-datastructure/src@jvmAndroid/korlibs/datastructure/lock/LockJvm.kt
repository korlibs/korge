package korlibs.datastructure.lock

import korlibs.time.*
import java.util.concurrent.atomic.*
import kotlin.time.*

private fun TimeSpan.toMillisNanos(): Pair<Long, Int> {
    val nanoSeconds = inWholeNanoseconds
    val millis = (nanoSeconds / 1_000_000L)
    val nanos = (nanoSeconds % 1_000_000L).toInt()
    return millis to nanos
}

actual class Lock actual constructor() : BaseLock {
    private val signaled = AtomicBoolean()

    actual override fun notify(unit: Unit) {
        signaled.set(true)
        (this as java.lang.Object).notifyAll()
    }

    actual override fun wait(time: TimeSpan): Boolean {
        val (millis, nanos) = time.toMillisNanos()
        signaled.set(false)
        //println("MyLock.wait: $time")
        val time = TimeSource.Monotonic.measureTime {
            (this as java.lang.Object).wait(millis, nanos)
        }
        //println("    -> $time")
        return signaled.get()
    }

    actual inline operator fun <T> invoke(callback: () -> T): T = synchronized(this) { callback() }
}

actual class NonRecursiveLock actual constructor() : BaseLock {
    private val signaled = AtomicBoolean()

    actual override fun notify(unit: Unit) {
        signaled.set(true)
        (this as java.lang.Object).notifyAll()
    }

    actual override fun wait(time: TimeSpan): Boolean {
        val (millis, nanos) = time.toMillisNanos()
        signaled.set(false)
        //println("MyLock.wait: $time")
        val time = TimeSource.Monotonic.measureTime {
            (this as java.lang.Object).wait(millis, nanos)
        }
        //println("    -> $time")
        return signaled.get()
    }

    actual inline operator fun <T> invoke(callback: () -> T): T = synchronized(this) { callback() }
}
