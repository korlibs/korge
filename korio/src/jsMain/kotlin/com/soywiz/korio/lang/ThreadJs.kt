package com.soywiz.korio.lang

import com.soywiz.klock.*

actual val currentThreadId: Long get() = 1L

actual fun Thread_sleep(time: Long) {
	val start = PerformanceCounter.milliseconds
	while (PerformanceCounter.milliseconds - start < time) {
		// Spinlock
	}
}
