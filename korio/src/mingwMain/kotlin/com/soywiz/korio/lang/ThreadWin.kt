package com.soywiz.korio.lang

import platform.windows.GetCurrentThreadId

actual val currentThreadId: Long get() = GetCurrentThreadId().toLong()
actual val currentThreadName: String? get() = "Thread-$currentThreadId"
