package korlibs.concurrent.lock

import korlibs.concurrent.thread.*
import platform.posix.*
import kotlin.concurrent.*
import kotlin.time.*

actual class Lock actual constructor() : BaseLock {
    @PublishedApi internal var notified = AtomicReference(false)
    @PublishedApi internal var locked = AtomicReference(false)
    @PublishedApi internal var current: platform.posix.pthread_t? = null

    @PublishedApi internal fun lock(): Boolean {
        var initialThread = false
        if (!locked.compareAndSet(false, true)) {
            if (current != pthread_self()) {
                NativeThread.spinWhile { !locked.compareAndSet(false, true) }
            }
        } else {
            current = pthread_self()
            initialThread = true
        }
        return initialThread
    }

    @PublishedApi internal fun unlock(initialThread: Boolean) {
        if (initialThread) {
            current = null
            // Should we try to sleep this thread and awake it later? If the lock is short, might not be needed
            NativeThread.spinWhile { !locked.compareAndSet(true, false) }
        }
    }

    actual inline operator fun <T> invoke(callback: () -> T): T {
        val initialThread = lock()
        try {
            return callback()
        } finally {
            unlock(initialThread)
        }
    }

    actual override fun notify(unit: Unit) {
        check(locked.value) { "Must wait inside a synchronization block" }
        if (current != pthread_self()) error("Must lock the notify thread")
        notified.value = true
    }
    actual override fun wait(time: Duration): Boolean {
        check(locked.value) { "Must wait inside a synchronization block" }
        val start = TimeSource.Monotonic.markNow()
        notified.value = false
        unlock(true)
        try {
            NativeThread.spinWhile { !notified.value && start.elapsedNow() < time }
        } finally {
            lock()
        }
        return notified.value
    }
}

actual class NonRecursiveLock actual constructor() : BaseLock {
    @PublishedApi internal var notified = AtomicReference(false)
    @PublishedApi internal var locked = AtomicReference(false)

    fun lock() {
        // Should we try to sleep this thread and awake it later? If the lock is short, might not be needed
        NativeThread.spinWhile { !locked.compareAndSet(false, true) }
    }

    fun unlock() {
        // Should we try to sleep this thread and awake it later? If the lock is short, might not be needed
        NativeThread.spinWhile { !locked.compareAndSet(true, false) }
    }

    actual inline operator fun <T> invoke(callback: () -> T): T {
        lock()
        try {
            return callback()
        } finally {
            unlock()
        }
    }

    actual override fun notify(unit: Unit) {
        notified.value = true
    }
    actual override fun wait(time: Duration): Boolean {
        check(locked.value) { "Must wait inside a synchronization block" }
        val start = TimeSource.Monotonic.markNow()
        notified.value = false
        unlock()
        try {
            NativeThread.sleepWhile { !notified.value && start.elapsedNow() < time }
        } finally {
            lock()
        }
        return notified.value
    }
}
