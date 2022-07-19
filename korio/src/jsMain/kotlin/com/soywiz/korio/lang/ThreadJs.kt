package com.soywiz.korio.lang

import com.soywiz.klock.*

// @TODO: Can we have different values when inside a worker? https://developer.mozilla.org/en-US/docs/Web/API/Worker
actual val currentThreadId: Long get() = 1L
actual val currentThreadName: String? get() = "Thread-$currentThreadId"

actual fun Thread_sleep(time: Long) {
	val start = PerformanceCounter.milliseconds
	while (PerformanceCounter.milliseconds - start < time) {
		// Spinlock
	}
}
