package com.soywiz.korio.lang

actual val currentThreadId: Long get() = Thread.currentThread().id
actual val currentThreadName: String? get() = Thread.currentThread().name

actual fun Thread_sleep(time: Long) = Thread.sleep(time)
