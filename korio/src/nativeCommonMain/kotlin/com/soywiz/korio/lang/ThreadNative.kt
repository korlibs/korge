package com.soywiz.korio.lang

import kotlinx.cinterop.*

actual val currentThreadId: Long get() = 1L

actual fun Thread_sleep(time: Long): Unit {
	platform.posix.usleep((time * 1000L).convert())
}
