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

    inline operator fun <T> invoke(callback: () -> T): T {
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
        try {
            return callback()
        } finally {
            if (initialThread) {
                current = null
                while (!locked.compareAndSet(true, false)) {
                    Unit // Should we try to sleep this thread and awake it later? If the lock is short, might not be needed
                }
            }
        }
    }
}

class NonRecursiveLock actual constructor() {
    @PublishedApi internal var locked = atomic(false)

    actual inline operator fun <T> invoke(callback: () -> T): T {
        while (!locked.compareAndSet(false, true)) {
            Unit // Should we try to sleep this thread and awake it later? If the lock is short, might not be needed
        }
        try {
            return callback()
        } finally {
            while (!locked.compareAndSet(true, false)) {
                Unit // Should we try to sleep this thread and awake it later? If the lock is short, might not be needed
            }
        }
    }
}
