package korlibs.io.lang

import korlibs.time.*

// @TODO: Can we have different values when inside a worker? https://developer.mozilla.org/en-US/docs/Web/API/Worker
actual val currentThreadId: Long get() = 1L
actual val currentThreadName: String? get() = "Thread-$currentThreadId"

actual fun Thread_sleep(ms: Double) {
	val start = PerformanceCounter.milliseconds
	while (PerformanceCounter.milliseconds - start < ms) {
		// Spinlock
	}
}
