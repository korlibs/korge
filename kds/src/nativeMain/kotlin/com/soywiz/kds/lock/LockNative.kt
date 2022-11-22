package com.soywiz.kds.lock

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.withLock
import platform.posix.pthread_self

actual class Lock actual constructor() : SynchronizedObject() {
	actual inline operator fun <T> invoke(callback: () -> T): T = withLock {
        callback()
    }
}

class FastReentrantLock constructor() {
    @PublishedApi internal var locked = atomic(false)
    @PublishedApi internal var current: platform.posix.pthread_t? = null

    @PublishedApi internal fun lock(): Boolean {
        var initialThread = false
        if (!locked.compareAndSet(false, true)) {
            if (current != pthread_self()) {
                while (!locked.compareAndSet(false, true)) {
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
            while (!locked.compareAndSet(true, false)) {
                Unit // Should we try to sleep this thread and awake it later? If the lock is short, might not be needed
            }
        }
    }

    inline operator fun <T> invoke(callback: () -> T): T {
        val initialThread = lock()
        try {
            return callback()
        } finally {
            unlock(initialThread)
        }
    }
}

actual class NonRecursiveLock actual constructor() {
    @PublishedApi internal var locked = atomic(false)

    fun lock() {
        while (!locked.compareAndSet(false, true)) {
            Unit // Should we try to sleep this thread and awake it later? If the lock is short, might not be needed
        }
    }

    fun unlock() {
        while (!locked.compareAndSet(true, false)) {
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
