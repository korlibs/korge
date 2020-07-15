package com.soywiz.korio.lang

actual val currentThreadId: Long get() = Thread.currentThread().id

actual fun Thread_sleep(time: Long) = Thread.sleep(time)
