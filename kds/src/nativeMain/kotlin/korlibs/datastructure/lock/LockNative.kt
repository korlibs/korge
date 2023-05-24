package korlibs.datastructure.lock

import platform.posix.pthread_self
import kotlin.native.concurrent.*

actual class Lock actual constructor() {
    @PublishedApi internal var locked = AtomicInt(0)
    @PublishedApi internal var current: platform.posix.pthread_t? = null

    @PublishedApi internal fun lock(): Boolean {
        var initialThread = false
        if (!locked.compareAndSet(0, 1)) {
            if (current != pthread_self()) {
                while (!locked.compareAndSet(0, 1)) {
                    Unit // Should we try to sleep this thread and awake it later? If the lock is short, might not be needed
                }
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
            while (!locked.compareAndSet(1, 0)) {
                Unit // Should we try to sleep this thread and awake it later? If the lock is short, might not be needed
            }
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
}

actual class NonRecursiveLock actual constructor() {
    @PublishedApi internal var locked = AtomicInt(0)

    fun lock() {
        while (!locked.compareAndSet(0, 1)) {
            Unit // Should we try to sleep this thread and awake it later? If the lock is short, might not be needed
        }
    }

    fun unlock() {
        while (!locked.compareAndSet(1, 0)) {
            Unit // Should we try to sleep this thread and awake it later? If the lock is short, might not be needed
        }
    }

    actual inline operator fun <T> invoke(callback: () -> T): T {
        lock()
        try {
            return callback()
        } finally {
            unlock()
        }
    }
}
