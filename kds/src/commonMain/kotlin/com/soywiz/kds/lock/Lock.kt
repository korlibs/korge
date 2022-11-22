package com.soywiz.kds.lock

/**
 * Reentrant typical lock.
 */
expect class Lock() {
	inline operator fun <T> invoke(callback: () -> T): T
}

/**
 * Optimized lock that cannot be called inside another lock,
 * don't keep the current thread id, or a list of threads to awake
 * It is lightweight and just requires an atomic.
 * Does busy-waiting instead of sleeping the thread.
 */
expect class NonRecursiveLock() {
    inline operator fun <T> invoke(callback: () -> T): T
}
